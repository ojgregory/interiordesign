#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

float sigmaRange;
float sigmaSpatial;
int diameter;
int gImageW;
int gImageH;
float *nc;
float *kindex;
float *kfract;
float *spatial;
float *range;
int minval;
int maxval;
int ncomps;
int spatial_size;
float spatial_stdev;
float range_stdev;
float denom;

rs_allocation gCurrentFrame;

float __attribute__((kernel)) generateKValues(int x){
    if (x < ncomps)
        return minval + x * (maxval - minval) / (ncomps - 1);
}

void generateKIndex(){
    float fval2;
    int i, k;
    for (i = minval, k = 0; i <= maxval && k < ncomps - 1; k++) {
        fval2 = nc[k + 1];
        while (i < fval2 && i < 255) {
            kindex[i] = k;
            i++;
        }
    }
}

void generateKFract(){
    float fval1;
    float fval2;
    int i, k;
    for (i = minval, k = 0; i <= maxval && k < ncomps - 1; k++) {
            fval1 = nc[k];
            fval2 = nc[k + 1];
            while (i < fval2 && i < 255) {
                kfract[i] = (float)(i - fval1) / (float)(fval2 - fval1);
                i++;
            }
        }

}

float __attribute__((kernel)) calculateSpatialKernel(int x) {
    if (x < spatial_size)
        return native_exp(-(float)(x * x) / denom);
}

float __attribute__((kernel)) calculateRangeKernel(int x) {
    if (x < spatial_size)
        return native_exp(-(float)(x * x) / denom);
}

int __attribute__((kernel)) horizontalConvolution(int x, int y, int z) {
    if (z < ncomps) {
        int width = (int)(2.0 * spatial_stdev);
        float sum = 0.0f;
        float norm = 0.0f;
        float kval = nc[z];
        float kern;
        uchar nval;
        int pixelX;
        int rangeAccess;
        int spatialAccess;
        for (int k = 0; k <= width; k++) {
            pixelX = (x - (width / 2)) + k;
            if (pixelX < gImageW) {
                nval = rsGetElementAtYuv_uchar_Y(gCurrentFrame, pixelX, y);
                spatialAccess = abs(k);
                rangeAccess = abs((int) (kval - nval));
                if (rangeAccess < 256 && spatialAccess < spatial_size) {
                    kern = spatial[spatialAccess] * range[rangeAccess];
                    sum += kern * nval;
                    norm += kern;
                }
            }
        }
        return (int)((sum / norm) + 0.5);
    }
}

int __attribute__((kernel)) verticalConvolution(int x, int y, int z) {
    if (z < ncomps) {
        int width = (int)(2.0 * spatial_stdev);
        float sum = 0.0f;
        float norm = 0.0f;
        float kval = nc[z];
        float kern;
        uchar nval;
        int rangeAccess;
        int spatialAccess;
        int pixelY;
        for (int k = 0; k <= width; k++) {
            pixelY = (y - (width / 2)) + k;
            if (pixelY < gImageH) {
                nval = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, pixelY);
                spatialAccess = abs(k);
                rangeAccess = abs((int) (kval - nval));
                if (rangeAccess < 256 && spatialAccess < spatial_size) {
                    kern = spatial[spatialAccess] * range[rangeAccess];
                    sum += kern * nval;
                    norm += kern;
                }
            }
        }
        return (int)((sum / norm) + 0.5);
    }
}

static float spatial_distance(int x, int y, int i, int j) {
    return sqrt(native_powr((float) x - i, 2.0f) + native_powr((float) y - j, 2.0f));
}

static float gaussian(float x, double sigma) {
    return exp(-(native_powr((float) x, 2.0f)))/(2 * native_powr((float) sigma, 2.0f)) / (2 * M_PI * native_powr((float) sigma, 2.0f));
}

uchar __attribute__((kernel)) applyBilateralFilter(uint32_t x, uint32_t y) {
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

    return newPixel;
}