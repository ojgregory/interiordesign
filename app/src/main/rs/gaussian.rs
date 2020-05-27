#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;

int gImageW;
int gImageH;

// Host version of the convolution mask 2D array.
int gMaskSize;
float *gConvMask1d;

// Kernel to convolve rows of an image using the convolution kernel
// stored in gConvMask1d.
uchar __attribute__((kernel)) convolve_kernel_row(uint32_t x, uint32_t y)
{
        ulong pixelX;
        uchar new_val = 0;

        // Loop over mask
        for (int mask_x = 0; mask_x < gMaskSize; ++mask_x) {
            pixelX = (x - (gMaskSize / 2)) + mask_x;
            if (pixelX < gImageW) {
                new_val += rsGetElementAtYuv_uchar_Y(gCurrentFrame, pixelX, y) * gConvMask1d[mask_x];
            }
        }

        return new_val;
}


// Kernel to convolve columns of an image using the convolution kernel
// stored in gConvMask1d.
uchar __attribute__((kernel)) convolve_kernel_col(uint32_t x, uint32_t y)
{
        ulong pixelY;
        uchar new_val = 0;

        // Loop over mask
        for (int mask_y = 0; mask_y < gMaskSize; ++mask_y) {
            pixelY = (y - (gMaskSize / 2)) + mask_y;
            if (pixelY < gImageH) {
                new_val +=  rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, pixelY) * gConvMask1d[mask_y];
            }
        }

        return new_val;
}