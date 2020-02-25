#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;

int gImageW;
int gImageH;

// Host version of the convolution mask 2D array.
int gMaskSize;
float *gConvMask1d;

////
// CUDA kernel to convolve rows of an image using the convolution kernel
// stored in d_convMask1D.
// in_pixels: device array holding the original image data.
// out_pixels: device array where the modified image should be written.
// image_w, image_h: width & height of the image.
////
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

        int4 rgb;
        new_val -= 16;
        rgb.r = new_val;
        rgb.g = new_val;
        rgb.b = new_val;
        rgb.a = 255;

        // Write out merged HDR result
        return new_val;
}


////
// CUDA kernel to convolve columns of an image using the convolution kernel
// stored in d_conv_kernel.
// in_pixels: device array holding the original image data.
// out_pixels: device array where the modified image should be written.
// image_w, image_h: width & height of the image.
////
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

        // Write out merged HDR result
        return new_val;
}