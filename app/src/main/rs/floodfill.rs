#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

typedef struct {
    int bag_size;
    int bag_length;
    rs_allocation array;

    int counter;
    int nedges;
} Bag;

typedef struct {
    rs_allocation array;
    int size;
    int q_length;
    int front;
    int rear;
} Queue;

static void create_queue(Queue queue) {
    queue.array = rsCreateAllocation_uchar2(queue.q_length);
}

static void resize(Queue queue) {
    rs_allocation newArray = rsCreateAllocation_uchar(queue.q_length * 2);

    if (queue.front > queue.rear) {
        for (int i = 0; i < queue.front; ++i) {
            rsSetElementAt_uchar(newArray, rsGetElementAt_uchar(queue.array, i), i + queue.q_length);
        }
        queue.rear = queue.rear + queue.q_length;
    }
    else {
        for (int i = queue.front; i < queue.rear; ++i) {
            rsSetElementAt_uchar(newArray, rsGetElementAt_uchar(queue.array, i), i);
        }
    }
    queue.q_length = queue.q_length * 2;
    queue.array = newArray;
}

static void push(Queue queue, uchar2 vertex) {
    if (queue.size == queue.q_length - 1) {
        resize(queue);
    }

    rsSetElementAt_uchar2(queue.array, vertex, queue.rear++);

    if (queue.rear == queue.q_length && queue.front != 0)
        queue.rear = 0;
    else
        resize(queue);

    queue.size++;
}

static uchar2 pop(Queue queue) {
    if (queue.size == 0)
        return 0;

    uchar return_value = rsGetElementAt_uchar(queue.array, queue.front);
    if (queue.front == queue.q_length && queue.rear != 0)
            queue.front = 0;
        else
            resize(queue);
    queue.size--;
    return return_value;
}

static  bool isEmpty(Queue queue) {
    if (queue.front == queue.rear)
        return true;
    return false;
}


void serial_implementation(rs_allocation input, rs_allocation output, int target_x, int target_y, int replacement_colour) {
    uchar target_colour = rsGetElementAtYuv_uchar_Y(input, target_x, target_y);
    Queue q;
    create_queue(q);
    uchar2 n;
    rsSetElementAt_uchar(output, replacement_colour, target_x, target_y);
    push(q, (uchar2){target_x, target_y});
    while (!isEmpty(q)) {
        n = pop(q);
        if (rsGetElementAtYuv_uchar_Y(input, n.x-1, n.y) == target_colour) {
            push(q, (uchar2){n.x-1, n.y});
            rsSetElementAt_uchar(output, replacement_colour, n.x-1, n.y);
        }
        if (rsGetElementAtYuv_uchar_Y(input, n.x+1, n.y) == target_colour) {
            push(q, (uchar2){n.x+1, n.y});
            rsSetElementAt_uchar(output, replacement_colour, n.x+1, n.y);
        }
        if (rsGetElementAtYuv_uchar_Y(input, n.x, n.y-1) == target_colour) {
            push(q, (uchar2){n.x, n.y-1});
            rsSetElementAt_uchar(output, replacement_colour, n.x, n.y-1);
        }
        if (rsGetElementAtYuv_uchar_Y(input, n.x, n.y+1) == target_colour) {
            push(q, (uchar2){n.x, n.y+1});
            rsSetElementAt_uchar(output, replacement_colour, n.x, n.y+1);
        }
    }
}