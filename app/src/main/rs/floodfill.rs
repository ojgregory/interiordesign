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
uchar target_colour;

static void create_queue(Queue* queue) {
    queue->size = 0;
    queue->q_length = 10000;
    queue->front = 0;
    queue->rear = -1;
    queue->array = rsCreateAllocation_uchar2(queue->q_length);
}

static void resize(Queue* queue) {
    rs_allocation newArray = rsCreateAllocation_uchar2(queue->q_length * 2);

    if (queue->front > queue->rear) {
        for (int i = 0; i < queue->front; ++i) {
            rsSetElementAt_uchar(newArray, rsGetElementAt_uchar(queue->array, i), i + queue->q_length);
        }
        queue->rear = queue->rear + queue->q_length;
    }
    else {
        for (int i = queue->front; i < queue->rear; ++i) {
            rsSetElementAt_uchar(newArray, rsGetElementAt_uchar(queue->array, i), i);
        }
    }
    queue->q_length = queue->q_length * 2;
    queue->array = newArray;
}

static void push(Queue* queue, uchar2 vertex) {
    if (queue->size == queue->q_length - 1) {
        //resize(queue);
    }

    queue->rear++;
    rsSetElementAt_uchar2(queue->array, vertex, queue->rear);

//    if (queue->rear == queue->q_length && queue->front != 0)
        //queue->rear = 0;
   // else if (queue->rear == queue->q_length)
        //resize(queue);

    queue->size++;
}

static uchar2 pop(Queue* queue) {
    if (queue->size == 0)
        return 0;

    queue->size--;
    queue->front++;
    uchar return_value = rsGetElementAt_uchar(queue->array, queue->front);
//    if (queue->front == queue->q_length && queue->rear != 0)
            //queue->front = 0;
  //  else if (queue->front == queue->q_length)
        //resize(queue);
    return return_value;
}

static bool isEmpty(Queue queue) {
    if (queue.size == 0) {
        return true;
    }
    return false;
}

static void resetQueue(Queue *queue) {
    rsDebug("reset top", 1);
    queue->size = 0;
    queue->q_length = 10000;
    queue->front = 0;
    queue->rear = -1;
    rsDebug("reset bot", 1);
}

void RS_KERNEL processNextQ(Queue nextQ, Queue currentQ, rs_kernel_context context) {
    uchar4 red = (uchar4) {255, 0, 0, 255};
    uchar2 n;
    rsDebug("array0", rsGetArray0(context));
    rsDebug("isEmpty pre", 1);
    //if (isEmpty(currentQ)) {
    //    rsDebug("return", 1);
    //    return;
    //}
    rsDebug("isEmpty post", 1);
    rsDebug("pop pre", 1);
    n = pop(&currentQ);
    rsDebug("pop post", 1);
    //n = (uchar2){100,100};
    rsDebug("n.x", n.x);
    rsDebug("n.y", n.y);
    if (n.x < imageW && n.x > 0 && n.y < imageH && n.y > 0) {
        rsDebug("red pre", 1);
        rsSetElementAt_uchar4(output, red, n.x, n.y);
        rsDebug("red post", 1);
        if (rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour) {
            push(&nextQ, (uchar2){n.x-1, n.y});
            //rsSetElementAt_uchar4(output, red, n.x-1, n.y);
        }
        if (rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour) {
            push(&nextQ, (uchar2){n.x+1, n.y});
            //rsSetElementAt_uchar4(output, red, n.x+1, n.y);
        }
        if (rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour) {
            push(&nextQ, (uchar2){n.x, n.y-1});
            //rsSetElementAt_uchar4(output, red, n.x, n.y-1);
        }
        if (rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour) {
            push(&nextQ, (uchar2){n.x, n.y+1});
            //rsSetElementAt_uchar4(output, red, n.x, n.y+1);
        }
    }

}

void parallel_implementation(rs_allocation output, int target_x, int target_y, int replacement_colour) {
    target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    uchar4 red = (uchar4) {255, 0, 0, 255};
    int counter = 0;
    Queue temp;
    create_queue(&currentQ);
    create_queue(&nextQ);
    rsDebug("target_x", target_x);
    rsDebug("target_y", target_y);
    push(&currentQ, (uchar2){target_x, target_y});
    rsDebug("x - input", rsAllocationGetDimX(input));
    rsDebug("y - input", rsAllocationGetDimY(input));
    rsDebug("z - input", rsAllocationGetDimZ(input));
    rsDebug("x - output", rsAllocationGetDimX(output));
    rsDebug("y - output", rsAllocationGetDimY(output));
    rsDebug("z - output", rsAllocationGetDimZ(output));

    while(!isEmpty(currentQ)) {
        rs_script_call_t opts = {0};
        rsDebug("currentQ.front", currentQ.front);
        rsDebug("currentQ.rear", currentQ.rear);
        opts.arrayStart = currentQ.front;
        opts.arrayEnd = currentQ.rear + 1;
        opts.xStart = currentQ.front;
        opts.xEnd = currentQ.rear + 1;

        rsForEachWithOptions(processNextQ, &opts, nextQ, currentQ);
        rsDebug("current = next top", 1);
        currentQ = nextQ;
        rsDebug("current = next bot", 1);
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
    uchar2 n;
    rsSetElementAt_uchar4(output, red, target_x, target_y);
    push(&q, (uchar2){target_x, target_y});
    while (!isEmpty(q)) {
        n = pop(&q);
        if (n.x != 0 && rsGetElementAt_uchar4(output, n.x-1, n.y).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour) {
            push(&q, (uchar2){n.x-1, n.y});
            rsSetElementAt_uchar4(output, red, n.x-1, n.y);
        }
        if (n.x != imageW && rsGetElementAt_uchar4(output, n.x+1, n.y).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour) {
            push(&q, (uchar2){n.x+1, n.y});
            rsSetElementAt_uchar4(output, red, n.x+1, n.y);
        }
        if (n.y != 0 && rsGetElementAt_uchar4(output, n.x, n.y-1).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour) {
            push(&q, (uchar2){n.x, n.y-1});
            rsSetElementAt_uchar4(output, red, n.x, n.y-1);
        }
        if (n.y != imageH && rsGetElementAt_uchar4(output, n.x, n.y+1).r != red.r && rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour) {
            push(&q, (uchar2){n.x, n.y+1});
            rsSetElementAt_uchar4(output, red, n.x, n.y+1);
        }
        counter--;
    }
    //rsDebug("return", 1);
}