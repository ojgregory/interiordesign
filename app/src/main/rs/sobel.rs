#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;

int gImageW;
int gImageH;

// Host version of the convolution mask 2D array.
const int maskSize = 3;
static float convXMask[maskSize][maskSize] = {
                                            { -1, 0, 1 },
										    { -2, 0, 2 },
										    { -1, 0, 1 }
										};
static float convYMask[maskSize][maskSize] = {
                                            { -1, -2, -1 },
										    { 0, 0, 0 },
									        { 1, 2, 1 }
									    };

////
// CUDA kernel to convolve an image using the convolution kernel
// stored in d_convMask.
// inPixels: device array holding the original image data.
// outPixels: device array where the modified image should be written.
// imageW, imageH: width & height of the image.
////
uchar4 __attribute__((kernel)) convolveKernel(uint32_t x, uint32_t y) {
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
                //rsDebug("pixel", pixel);
                newValX = newValX + (float) (pixel * convXMask[maskX][maskY]);
                newValY = newValY + (float) (pixel * convYMask[maskX][maskY]);
            }
        }
    }

    newPixel = (uchar) sqrt(pow(newValX, 2) + pow(newValY, 2));


    int4 rgb;
    newPixel -= 16;
    rgb.r = newPixel;
    rgb.g = newPixel;
    rgb.b = newPixel;
    rgb.a = 255;

    // Write out merged HDR result
    return convert_uchar4(clamp(rgb, 0, 255));
}

////
// CUDA kernel to convolve an image using the convolution kernel
// stored in d_convMask.
// inPixels: device array holding the original image data.
// outPixels: device array where the modified image should be written.
// imageW, imageH: width & height of the image.
////
uchar4 __attribute__((kernel)) greyscale(uint32_t x, uint32_t y) {
    uchar curPixel;
    curPixel = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);

    int4 rgb;
    curPixel -= 16;
    rgb.r = curPixel;
    rgb.g = curPixel;
    rgb.b = curPixel;
    rgb.a = 255;

    // Write out merged HDR result
    return convert_uchar4(clamp(rgb, 0, 255));
}

