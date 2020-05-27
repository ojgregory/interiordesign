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
    queue->q_length = 500;
    queue->front = 0;
    queue->rear = 0;
    queue->array = rsCreateAllocation_uint2(queue->q_length);
}

static void resize(Queue* queue) {
    queue->q_length = queue->q_length * 2;
    rs_allocation newArray = rsCreateAllocation_uint2(queue->q_length);

    for (int i = queue->front; i < queue->rear; ++i) {
        rsSetElementAt_uint2(newArray, rsGetElementAt_uint2(queue->array, i), i);
    }
    queue->array = newArray;
}

static void resize_length(Queue* queue, int q_length) {
    queue->q_length = q_length;
    rs_allocation newArray = rsCreateAllocation_uint2(q_length);
    queue->array = newArray;
}

static void push(Queue* queue, uint2 vertex) {
    if (queue->size >= queue->q_length - 1 || queue->rear >= queue->q_length - 1) {
        resize(queue);
    }

    if (queue->rear >= 0 && queue->rear < queue->q_length) {
        rsSetElementAt_uint2(queue->array, vertex, queue->rear);
        queue->rear++;
        queue->size++;
    }
}

static uint2 pop(Queue* queue) {
    queue->size--;
    queue->front++;
    if (queue->front-1 >= 0 && queue->front-1 < queue->q_length)
        return rsGetElementAt_uint2(queue->array, queue->front-1);
}

static bool isEmpty(Queue* queue) {
    if (queue->size == 0) {
        return true;
    }
    return false;
}

static void resetQueue(Queue *queue) {
    queue->size = 0;
    queue->front = 0;
    queue->rear = 0;
}

//Set result queue to match origin queue for all fields
static void copyQueue(Queue *result, Queue *origin) {
    result->size = origin->size;
    if (result->q_length != origin->q_length) {
        resize_length(result, origin->q_length);
    }
    result->front = origin->front;
    result->rear = origin->rear;
    for (int i = result->front; i < result->rear; ++i) {
        rsSetElementAt_uint2(result->array, rsGetElementAt_uint2(origin->array, i), i);
    }
}

void RS_KERNEL processNextQ() {
    if (isEmpty(&currentQ))
        return;
    uint2 n = pop(&currentQ);
    if (n.x <= 0 || n.x >= imageW-1 || n.y <= 0 || n.y >= imageH-1) {
        return;
    }

    if (rsGetElementAt_uchar(isProcessed,n.x, n.y) == 1) {
        return;
    }
    
    rsSetElementAt_uchar4(output, colour, n.x, n.y);
    rsSetElementAt_uchar(isProcessed, 1, n.x, n.y);

    if (
        rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour
    )
    {
        push(&nextQ, (uint2){n.x-1, n.y});
    }
    
    if (
    rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour
    )
    {
        push(&nextQ, (uint2){n.x+1, n.y});
    }

    if (
    rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour
    )
    {
        push(&nextQ, (uint2){n.x, n.y-1});
    }

    if (
    rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour
    )
    {
        push(&nextQ, (uint2){n.x, n.y+1});
    }
}

void parallel_implementation(int target_x, int target_y, int replacement_colour) {
    target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    int counter = 0;
    create_queue(&currentQ);
    create_queue(&nextQ);
    push(&currentQ, (uint2){target_x, target_y});
    isProcessed = rsCreateAllocation_uchar(imageW, imageH);
    upperBound = target_colour + fuzzy;
    lowerBound = target_colour - fuzzy;

    rs_script_call_t opts = {0};
    while(!isEmpty(&currentQ)) {
        opts.xStart = currentQ.front;
        opts.xEnd = currentQ.rear + 1;
        rsDebug("xStart", opts.xStart);
        rsDebug("xEnd", opts.xEnd);

        rsForEachWithOptions(processNextQ, &opts);
        copyQueue(&currentQ, &nextQ);
        resetQueue(&nextQ);
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

void serial_implementation_while(int target_x, int target_y, int replacement_colour) {
    uchar target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    uint2 n;
    upperBound = target_colour + fuzzy;
    lowerBound = target_colour - fuzzy;
    isProcessed = rsCreateAllocation_uchar(imageW, imageH);
    create_queue(&currentQ);
    push(&currentQ, (uint2){target_x, target_y});
    while (!isEmpty(&currentQ)) {
        n = pop(&currentQ);
        if (rsGetElementAt_uchar(isProcessed,n.x, n.y) == 1 || n.x <= 0 || n.x >= imageW-1 || n.y <= 0 || n.y >= imageH-1)
            continue;
        rsSetElementAt_uchar4(output, colour, n.x, n.y);
        rsSetElementAt_uchar(isProcessed, 1, n.x, n.y);

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour
        )
        {
            push(&currentQ, (uint2){n.x-1, n.y});
        }

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour
        )
        {
            push(&currentQ, (uint2){n.x+1, n.y});
        }

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour
        )
        {
            push(&currentQ, (uint2){n.x, n.y-1});
        }

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour
        )
        {
            push(&currentQ, (uint2){n.x, n.y+1});
        }
    };
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
    while (!isEmpty(&currentQ)) {
    while (!isEmpty(&currentQ)) {
        n = pop(&currentQ);
        if (rsGetElementAt_uchar(isProcessed,n.x, n.y) == 1 || n.x <= 0 || n.x >= imageW-1 || n.y <= 0 || n.y >= imageH-1)
            continue;
        rsSetElementAt_uchar4(output, colour, n.x, n.y);
        rsSetElementAt_uchar(isProcessed, 1, n.x, n.y);

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour
        )
        {
            push(&nextQ, (uint2){n.x-1, n.y});
        }

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour
        )
        {
            push(&nextQ, (uint2){n.x+1, n.y});
        }

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour
        )
        {
            push(&nextQ, (uint2){n.x, n.y-1});
        }

        if (
        rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour
        )
        {
            push(&nextQ, (uint2){n.x, n.y+1});
        }
    };
        copyQueue(&currentQ, &nextQ);
        resetQueue(&nextQ);
    };
}