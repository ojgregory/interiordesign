#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

int imageW;
int imageH;

typedef struct {
    int bag_size;
    int bag_length;
    rs_allocation array;

    int counter;
    int nedges;
} Bag;

typedef struct Queue {
    rs_allocation array;
    uint size;
    uint q_length;
    int front;
    int rear;
} Queue;

Queue currentQ, nextQ;
rs_allocation input;
rs_allocation output;
rs_allocation isProcessed;
uchar target_colour;
uchar4 colour;
int fuzzy = 10;
int upperBound;
int lowerBound;

static void create_queue(Queue* queue) {
    queue->size = 0;
    queue->q_length = 100;
    queue->front = 0;
    queue->rear = 0;
    queue->array = rsCreateAllocation_uint2(queue->q_length);
}

static void resize(Queue* queue) {
    rs_allocation newArray = rsCreateAllocation_uint2(queue->q_length * 2);

    if (queue->front > queue->rear) {
        for (int i = 0; i < queue->front; ++i) {
            rsSetElementAt_uint2(newArray, rsGetElementAt_uint2(queue->array, i), i + queue->q_length);
        }
        queue->rear = queue->rear + queue->q_length;
    }
    else {
        for (int i = queue->front; i < queue->rear; ++i) {
            rsSetElementAt_uint2(newArray, rsGetElementAt_uint2(queue->array, i), i);
        }
    }
    queue->q_length = queue->q_length * 2;
    queue->array = newArray;
}

static void push(Queue* queue, uint2 vertex) {
    if (queue->size == queue->q_length - 1) {
        resize(queue);
    }

    if (queue->rear >= 0 && queue->rear < queue->q_length)
        rsSetElementAt_uint2(queue->array, vertex, queue->rear);
    queue->rear++;
    queue->size++;
}

static uint2 pop(Queue* queue) {
    queue->size--;
    queue->front++;
    if (queue->front-1 >= 0 && queue->front-1 < queue->q_length)
        return rsGetElementAt_uint2(queue->array, queue->front-1);
//    if (queue->front == queue->q_length && queue->rear != 0)
            //queue->front = 0;
  //  else if (queue->front == queue->q_length)
        //resize(queue);
}

static bool isEmpty(Queue queue) {
    if (queue.size == 0) {
        return true;
    }
    return false;
}

static void resetQueue(Queue *queue) {
    queue->size = 0;
    queue->front = 0;
    queue->rear = 0;
}

static void copyQueue(Queue *result, Queue *origin) {
    result->size = origin->size;
    result->q_length = origin->q_length;
    result->front = origin->front;
    result->rear = origin->rear;
    result->array = origin->array;
}

void RS_KERNEL processNextQ() {
    uint2 n;
    if (isEmpty(currentQ))
        return;
    n = pop(&currentQ);
    //n = (uint2){100,100};
    if (n.x < imageW && n.x > 0 && n.y < imageH && n.y > 0) {
        rsSetElementAt_uchar4(output, colour, n.x, n.y);
        if (
        n.x != 0 &&
        rsGetElementAt_uchar(isProcessed,n.x-1, n.y) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) < upperBound &&
        rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) > lowerBound
        )
        {
            push(&nextQ, (uint2){n.x-1, n.y});
            rsSetElementAt_uchar(isProcessed, 1, n.x-1, n.y);
        }
        if (
        n.x != imageW &&
        rsGetElementAt_uchar(isProcessed,n.x+1, n.y) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) < upperBound &&
        rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) > lowerBound
        )
        {
            push(&nextQ, (uint2){n.x+1, n.y});
            rsSetElementAt_uchar(isProcessed, 1, n.x+1, n.y);
        }
        if (
        n.y != 0 &&
        rsGetElementAt_uchar(isProcessed,n.x, n.y-1) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) < upperBound &&
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) > lowerBound
        )
        {
            push(&nextQ, (uint2){n.x, n.y-1});
            rsSetElementAt_uchar(isProcessed, 1, n.x, n.y-1);
        }
        if (
        n.y != imageH &&
        rsGetElementAt_uchar(isProcessed,n.x, n.y+1) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) < upperBound &&
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) > lowerBound
        )
        {
            push(&nextQ, (uint2){n.x, n.y+1});
            rsSetElementAt_uchar(isProcessed, 1, n.x, n.y+1);
        }
    }
    rsDebug("return processNextQ()", 1);
}

void parallel_implementation(int target_x, int target_y, int replacement_colour) {
    target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    uchar4 red = (uchar4) {255, 0, 0, 255};
    int counter = 0;
    Queue temp;
    create_queue(&currentQ);
    create_queue(&nextQ);
    rsSetElementAt_uchar4(output, red, target_x, target_y);
    push(&currentQ, (uint2){target_x, target_y});
    isProcessed = rsCreateAllocation_uchar(imageW, imageH);
    upperBound = target_colour + fuzzy;
    lowerBound = target_colour - fuzzy;

    while(!isEmpty(currentQ)) {
        rs_script_call_t opts = {0};
        opts.xStart = currentQ.front;
        opts.xEnd = currentQ.rear + 1;

        rsForEachWithOptions(processNextQ, &opts);
        copyQueue(&currentQ, &nextQ);
        resetQueue(&nextQ);
        counter++;
    }
}

void printQueue(Queue q) {
    for (int i = q.front; i < q.rear; ++i) {
        rsDebug("x", rsGetElementAt_uint2(q.array, i).x);
        rsDebug("y", rsGetElementAt_uint2(q.array, i).y);
    }

}

void checkQueue(Queue q, Queue otherQ) {
    for (int i = q.front; i < q.rear; ++i) {
        if (rsGetElementAt_uint2(q.array, i).x != rsGetElementAt_uint2(otherQ.array, i).x)
            rsDebug("wrong x", i);
        if (rsGetElementAt_uint2(q.array, i).y != rsGetElementAt_uint2(otherQ.array, i).y)
            rsDebug("wrong y", i);
    }

}

void serial_implementation(int target_x, int target_y, int replacement_colour) {
    uchar target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    rsDebug("target", target_colour);
    uint2 n;
    isProcessed = rsCreateAllocation_uchar(imageW, imageH);
    create_queue(&currentQ);
    create_queue(&nextQ);
    push(&currentQ, (uint2){target_x, target_y});
    upperBound = target_colour + fuzzy;
    lowerBound = target_colour - fuzzy;
    while (!isEmpty(currentQ)) {
    while (!isEmpty(currentQ)) {
        n = pop(&currentQ);
        if (rsGetElementAt_uchar(isProcessed,n.x, n.y) == 1)
            continue;
        rsSetElementAt_uchar4(output, colour, n.x, n.y);
        rsSetElementAt_uchar(isProcessed, 1, n.x, n.y);

        if (
        n.x != 0 &&
        //rsGetElementAt_uchar(isProcessed,n.x-1, n.y) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour
        )
        {
            //rsDebug("west", rsGetElementAt_uchar(isProcessed,target_x-1, target_y));
            push(&nextQ, (uint2){n.x-1, n.y});
            //rsSetElementAt_uchar(isProcessed, 1, n.x-1, n.y);
        }

        if (
        n.x != imageW &&
        //rsGetElementAt_uchar(isProcessed,n.x+1, n.y) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour
        )
        {
            //rsDebug("east", rsGetElementAt_uchar(isProcessed,target_x+1, target_y));
            push(&nextQ, (uint2){n.x+1, n.y});
            //rsSetElementAt_uchar(isProcessed, 1, n.x+1, n.y);
        }

        if (
        n.y != 0 &&
        //rsGetElementAt_uchar(isProcessed,n.x, n.y-1) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour
        )
        {
            //rsDebug("north", rsGetElementAt_uchar(isProcessed,target_x, target_y-1));
            push(&nextQ, (uint2){n.x, n.y-1});
            //rsSetElementAt_uchar(isProcessed, 1, n.x, n.y-1);
        }

        if (
        n.y != imageH &&
        //rsGetElementAt_uchar(isProcessed,n.x, n.y+1) != 1 &&
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour
        )
        {
            //rsDebug("south", rsGetElementAt_uchar(isProcessed,target_x, target_y+1));
            push(&nextQ, (uint2){n.x, n.y+1});
            //rsSetElementAt_uchar(isProcessed, 1, n.x, n.y+1);
        }
    };
        //printQueue(nextQ);
        copyQueue(&currentQ, &nextQ);
        //checkQueue(nextQ,currentQ);
        resetQueue(&nextQ);
        //rsDebug("size", currentQ.size);
    };
    //rsDebug("size", currentQ.size);
    //rsDebug("north", rsGetElementAt_uchar(isProcessed,target_x, target_y-1));
    //rsDebug("south", rsGetElementAt_uchar(isProcessed,target_x, target_y+1));
    //rsDebug("west", rsGetElementAt_uchar(isProcessed,target_x-1, target_y));
    //rsDebug("east", rsGetElementAt_uchar(isProcessed,target_x+1, target_y));
    //rsDebug("rear", currentQ.rear);
    //rsDebug("return", 1);
}