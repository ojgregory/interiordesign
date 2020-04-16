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
    int size;
    int q_length;
    int front;
    int rear;
} Queue;

Queue currentQ, nextQ;
rs_allocation input;
rs_allocation output;
rs_allocation isProcessed;
uchar target_colour;

static void create_queue(Queue* queue) {
    queue->size = 0;
    queue->q_length = 1000;
    queue->front = 0;
    queue->rear = -1;
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

    queue->rear++;
    queue->size++;
    rsSetElementAt_uint2(queue->array, vertex, queue->rear);
}

static uint2 pop(Queue* queue) {
    if (queue->size == 0)
        return 0;

    queue->size--;
    queue->front++;
    rsDebug("pop front", queue->front);
    rsDebug("pop size", queue->size);
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
    queue->rear = -1;
}

static void copyQueue(Queue *result, Queue *origin) {
    result->size = origin->size;
    result->q_length = origin->q_length;
    result->front = origin->front;
    result->rear = origin->rear;
    result->array = origin->array;
}

void RS_KERNEL processNextQ() {
    uchar4 red = (uchar4) {255, 0, 0, 255};
    uint2 n;
    if (isEmpty(currentQ))
        return;
    rsDebug("pop pre", currentQ.size);
    n = pop(&currentQ);
    rsDebug("pop post", currentQ.size);
    //n = (uint2){100,100};
    rsDebug("n.x", n.x);
    rsDebug("n.y", n.y);
    if (n.x < imageW && n.x > 0 && n.y < imageH && n.y > 0) {
        rsDebug("red pre", rsGetElementAt_uchar4(output, n.x, n.y).b);
        rsSetElementAt_uchar4(output, red, n.x, n.y);
        rsDebug("red post", rsGetElementAt_uchar4(output, n.x, n.y).b);
        if (n.x != 0 && (rsGetElementAt_uchar(isProcessed,n.x-1, n.y) != 1 || rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour)) {
            push(&nextQ, (uint2){n.x-1, n.y});
            rsSetElementAt_uchar(isProcessed, 1, n.x-1, n.y);
        }
        if (n.x != imageW && (rsGetElementAt_uchar(isProcessed,n.x+1, n.y) != 1 || rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour)) {
            push(&nextQ, (uint2){n.x+1, n.y});
            rsSetElementAt_uchar(isProcessed, 1, n.x+1, n.y);
        }
        if (n.y != 0 && (rsGetElementAt_uchar(isProcessed,n.x, n.y-1) != 1 || rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour)) {
            push(&nextQ, (uint2){n.x, n.y-1});
            rsSetElementAt_uchar(isProcessed, 1, n.x, n.y-1);
        }
        if (n.y != imageH && (rsGetElementAt_uchar(isProcessed,n.x, n.y+1) != 1 || rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour)) {
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
    rsDebug("target_x", target_x);
    rsDebug("target_y", target_y);
    rsSetElementAt_uchar4(output, red, target_x, target_y);
    push(&currentQ, (uint2){target_x, target_y});
    rsDebug("x - input", rsAllocationGetDimX(input));
    rsDebug("y - input", rsAllocationGetDimY(input));
    rsDebug("z - input", rsAllocationGetDimZ(input));
    rsDebug("x - output", rsAllocationGetDimX(output));
    rsDebug("y - output", rsAllocationGetDimY(output));
    rsDebug("z - output", rsAllocationGetDimZ(output));
    isProcessed = rsCreateAllocation_uchar(imageW, imageH);

    while(currentQ.size < 800 && counter < 256 && !isEmpty(currentQ)) {
        rs_script_call_t opts = {0};
        rsDebug("currentQ.front", currentQ.front);
        rsDebug("currentQ.rear", currentQ.rear);
        opts.arrayStart = currentQ.front;
        opts.arrayEnd = currentQ.rear + 1;
        opts.xStart = currentQ.front;
        opts.xEnd = currentQ.rear + 1;

        rsForEachWithOptions(processNextQ, &opts);
        rsDebug("current = next top", nextQ.front);
        copyQueue(&currentQ, &nextQ);
        rsDebug("current = next bot", currentQ.front);
        resetQueue(&nextQ);
        counter++;
        rsDebug("counter", counter);
    }
}

void serial_implementation(rs_allocation input, rs_allocation output, int target_x, int target_y, int replacement_colour) {
    uchar target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    uchar4 red = (uchar4) {255, 0, 0, 255};
    int counter = 10000;
    Queue q;
    create_queue(&q);
    uint2 n;
    rsSetElementAt_uchar4(output, red, target_x, target_y);
    push(&q, (uint2){target_x, target_y});
    while (!isEmpty(q)) {
        n = pop(&q);
        if (n.x != 0 && rsGetElementAt_uchar4(output, n.x-1, n.y).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour) {
            push(&q, (uint2){n.x-1, n.y});
            rsSetElementAt_uchar4(output, red, n.x-1, n.y);
        }
        if (n.x != imageW && rsGetElementAt_uchar4(output, n.x+1, n.y).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour) {
            push(&q, (uint2){n.x+1, n.y});
            rsSetElementAt_uchar4(output, red, n.x+1, n.y);
        }
        if (n.y != 0 && rsGetElementAt_uchar4(output, n.x, n.y-1).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour) {
            push(&q, (uint2){n.x, n.y-1});
            rsSetElementAt_uchar4(output, red, n.x, n.y-1);
        }
        if (n.y != imageH && rsGetElementAt_uchar4(output, n.x, n.y+1).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour) {
            push(&q, (uint2){n.x, n.y+1});
            rsSetElementAt_uchar4(output, red, n.x, n.y+1);
        }
        counter--;
    }
    //rsDebug("return", 1);
}