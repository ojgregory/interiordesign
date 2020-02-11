#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;
rs_allocation gPrevFrame;

const uchar4 *gPixels;
int gCutPointX = 0;
int gDoMerge = 0;
int gFrameCounter = 0;

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
uchar4 __attribute__((kernel)) convolveKernel(uchar4 in, uint32_t x, uint32_t y) {
    uchar curPixel;
    curPixel = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);
    uchar newPixel;


		int pixelX;
		int pixelY;
		float newValX = 0.0f;
		float newValY = 0.0f;
		float newVal = 0.0f;

		for (int maskX = 0; maskX < maskSize; ++maskX) {
			for (int maskY = 0; maskY < maskSize; ++maskY) {
				pixelX = (x - (maskSize / 2)) + maskX;
				pixelY = (y - (maskSize / 2)) + maskY;
				newValX += rsGetElementAtYuv_uchar_Y(gCurrentFrame, pixelX, pixelY) * convXMask[maskX][maskY];
				newValY += rsGetElementAtYuv_uchar_Y(gCurrentFrame, pixelX, pixelY) * convYMask[maskX][maskY];
			}
		}

		newPixel = sqrt(pow(newValX, 2) + pow(newValY, 2));


		int4 rgb;
            rgb.r = newPixel +
                    newPixel * 1436 / 1024 - 179;
            rgb.g = newPixel -
                    newPixel * 46549 / 131072 + 44 -
                    newPixel * 93604 / 131072 + 91;
            rgb.b = newPixel +
                    newPixel * 1814 / 1024 - 227;
            rgb.a = 255;

            // Store current pixel for next frame
            rsSetElementAt_uchar4(gPrevFrame, curPixel, x, y);

            // Write out merged HDR result
            uchar4 out = convert_uchar4(clamp(rgb, 0, 255));

		return fmax(0.0f, fmin(1.0f, newVal));
	}

