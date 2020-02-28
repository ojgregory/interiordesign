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

    // Write out merged HDR result
    return convert_uchar4(clamp(rgb, 0, 255));
}