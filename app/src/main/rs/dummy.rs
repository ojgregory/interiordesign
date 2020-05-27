#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;

uchar __attribute__((kernel)) dummy_preprocess(uint32_t x, uint32_t y) {
    return rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);
}

uchar4 __attribute__((kernel)) convertYToRGB(uint32_t x, uint32_t y) {
    int4 rgb;
    uchar newPixel = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);
    rgb.r = newPixel;
    rgb.g = newPixel;
    rgb.b = newPixel;
    rgb.a = 255;

    return convert_uchar4(clamp(rgb, 0, 255));
}

uchar4 __attribute__((kernel)) convertYToRGB_colour(uint32_t x, uint32_t y) {
    int4 rgb;
    uchar yValue = rsGetElementAtYuv_uchar_Y(gCurrentFrame, x, y);
    uchar uValue = rsGetElementAtYuv_uchar_U(gCurrentFrame, x, y);
    uchar vValue = rsGetElementAtYuv_uchar_V(gCurrentFrame, x, y);
    int rTmp = yValue + (1.370705 * (vValue-128));
    int gTmp = yValue - (0.698001 * (vValue-128)) - (0.337633 * (uValue-128));
    int bTmp = yValue + (1.732446 * (uValue-128));

    rgb.r = clamp(rTmp, 0, 255);
    rgb.g = clamp(gTmp, 0, 255);
    rgb.b = clamp(bTmp, 0, 255);
    rgb.a = 255;

    return convert_uchar4(clamp(rgb, 0, 255));
}