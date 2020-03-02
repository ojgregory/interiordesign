#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

float sigmaRange;
float sigmaSpatial;
float diameter;
int gImageW;
int gImageH;

rs_allocation gCurrentFrame;

static float spatial_distance(int x, int y, int i, int j) {
    return sqrt(native_powr((float) x - i, 2.0f) + native_powr((float) y - j, 2.0f));
}

static float gaussian(float x, double sigma) {
    return exp(-(native_powr((float) x, 2.0f)))/(2 * native_powr((float) sigma, 2.0f)) / (2 * M_PI * native_powr((float) sigma, 2.0f));
}

uchar4 __attribute__((kernel)) applyBilateralFilter(uint32_t x, uint32_t y) {
    uchar newPixel;
    uchar currentPixel = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);

    ulong pixelX;
    ulong pixelY;
    float newValSpatial = 0.0f;
    float newValRange = 0.0f;
    float newVal = 0.0f;
    float wp = 0.0f;
    float w = 0.0f;
    uchar pixel = 0;

    for (int diameterX = 0; diameterX < diameter; ++diameterX) {
        for (int diameterY = 0; diameterY < diameter; ++diameterY) {
            pixelX = (x - (diameter / 2)) + diameterX;
            pixelY = (y - (diameter / 2)) + diameterY;
            if (pixelX < gImageW && pixelY < gImageH) {
                pixel = rsGetElementAtYuv_uchar_Y(gCurrentFrame, pixelX, pixelY);
                newValRange = gaussian(pixel - currentPixel, sigmaRange);
                newValSpatial = gaussian(spatial_distance(x, y, pixelX, pixelY), sigmaSpatial);
                w = newValRange * newValSpatial;
                newVal = newVal + pixel * w;
                wp += w;
            }
        }
    }

    newPixel = newVal / wp;

    int4 rgb;
    rgb.r = newPixel;
    rgb.g = newPixel;
    rgb.b = newPixel;
    rgb.a = 255;

    // Write out merged HDR result
    return convert_uchar4(clamp(rgb, 0, 255));
}