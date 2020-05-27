#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;

int gImageW;
int gImageH;

int maskSize;

int *convXMask;
int *convYMask;

// kernel to convolve an image using the convolution kernels
// stored in convXMask and convYMask.
uchar __attribute__((kernel)) convolveKernel(uint32_t x, uint32_t y) {
    uchar newPixel;

    ulong pixelX;
    ulong pixelY;
    float newValX = 0.0f;
    float newValY = 0.0f;
    float newVal = 0.0f;
    uchar pixel = 0;

    for (int maskX = 0; maskX < maskSize; ++maskX) {
        for (int maskY = 0; maskY < maskSize; ++maskY) {
            pixelX = (x - (maskSize / 2)) + maskX;
            pixelY = (y - (maskSize / 2)) + maskY;
            if (pixelX < gImageW && pixelY < gImageH) {
                pixel = rsGetElementAtYuv_uchar_Y(gCurrentFrame, pixelX, pixelY);
                newValX = newValX + (float) (pixel * convXMask[maskX * maskSize + maskY]);
                newValY = newValY + (float) (pixel * convYMask[maskX * maskSize + maskY]);
            }
        }
    }

    newPixel = (uchar) sqrt(pow(newValX, 2) + pow(newValY, 2));

    if (newPixel < 50)
        newPixel = 0;


    return newPixel;
}