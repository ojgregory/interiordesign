#pragma version(1)
#pragma rs java_package_name(uk.ac.plymouth.interiordesign)
#pragma rs_fp_relaxed

rs_allocation gCurrentFrame;
rs_allocation input;
rs_allocation temp_allocation;
rs_allocation processing_allocation;
rs_allocation Gx;
rs_allocation Gy;

#define STRONG 255
#define WEAK 128

int gImageW;
int gImageH;

int maskSize;

int *convXMask;
int *convYMask;

int highThreshold;
int lowThreshold;

float2 __attribute__((kernel)) convolveKernel(uint32_t x, uint32_t y) {
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
        // rsDebug("pixel", convXMask[maskX * maskSize + maskY]);
        newValX =
            newValX + (float)(pixel * convXMask[maskX * maskSize + maskY]);
        newValY =
            newValY + (float)(pixel * convYMask[maskX * maskSize + maskY]);
      }
    }
  }

  newPixel = (uchar)sqrt(pow(newValX, 2) + pow(newValY, 2));
  rsSetElementAt_float(Gx, (float)newValX, x, y);
  rsSetElementAt_float(Gy, (float)newValY, x, y);

  float2 outputPixel;
  outputPixel.x = (float)newPixel;
  outputPixel.y = native_atan2(newValY, newValX);

  return outputPixel;
}

float2 __attribute__((kernel)) supression(uint32_t x, uint32_t y) {
  uchar pixel = 0;
  float theta;
  float2 inputPixel = rsGetElementAt_float2(input, x, y);
  pixel = inputPixel.x;
  theta = inputPixel.y;
  float2 outputPixel;
  float yBot[2];
  float yTop[2];
  float x_est;

  if(x > 1 && y > 1 && x < gImageW - 2 && y < gImageH - 2){
  if ((theta >= 0 && theta <= 45) || (theta < -135 && theta >= -180)) {
    yBot[0] = rsGetElementAt_float2(input, x, y + 1).x;
    yBot[1] = rsGetElementAt_float2(input, x + 1, y + 1).x;

    yTop[0] = rsGetElementAt_float2(input, x, y - 1).x;
    yTop[1] = rsGetElementAt_float2(input, x - 1, y - 1).x;
    x_est = fabs(rsGetElementAt_float(Gy, x, y) / pixel);

    if ((pixel >= ((yBot[1] - yBot[0]) * x_est + yBot[0]) &&
         pixel >= ((yTop[1] - yTop[0]) * x_est + yTop[0]))) {
      outputPixel.x = pixel;
      outputPixel.y = theta;
    } else {
      outputPixel.x = 0;
      outputPixel.y = theta;
    }
  } else if ((theta > 45 && theta <= 90) || (theta < -90 && theta >= -135)) {
    yBot[0] = rsGetElementAt_float2(input, x + 1, y).x;
    yBot[1] = rsGetElementAt_float2(input, x + 1, y - 1).x;
    yTop[0] = rsGetElementAt_float2(input, x - 1, y).x;
    yTop[1] = rsGetElementAt_float2(input, x - 1, y - 1).x;
    x_est = fabs(rsGetElementAt_float(Gx, x, y) / pixel);

    if (pixel >= ((yBot[1] - yBot[0]) * x_est + yBot[0]) &&
        pixel >= ((yTop[1] - yTop[0]) * x_est + yTop[0])) {
      outputPixel.x = pixel;
      outputPixel.y = theta;
    } else {
      outputPixel.x = 0;
      outputPixel.y = theta;
    }
  } else if ((theta > 90 && theta <= 135) || (theta < -45 && theta >= -90)) {
    yBot[0] = rsGetElementAt_float2(input, x + 1, y).x;
    yBot[1] = rsGetElementAt_float2(input, x + 1, y - 1).x;
    yTop[0] = rsGetElementAt_float2(input, x - 1, y).x;
    yTop[1] = rsGetElementAt_float2(input, x - 1, y + 1).x;
    x_est = fabs(rsGetElementAt_float(Gx, x, y) / pixel);

    if (pixel >= ((yBot[1] - yBot[0]) * x_est + yBot[0]) &&
        pixel >= ((yTop[1] - yTop[0]) * x_est + yTop[0])) {
      outputPixel.x = pixel;
      outputPixel.y = theta;
    } else {
      outputPixel.x = 0;
      outputPixel.y = theta;
    }
  } else if ((theta > 135 && theta <= 180) || (theta < 0 && theta >= -45)) {
    yBot[0] = rsGetElementAt_float2(input, x, y - 1).x;
    yBot[1] = rsGetElementAt_float2(input, x + 1, y - 1).x;
    yTop[0] = rsGetElementAt_float2(input, x, y + 1).x;
    yTop[1] = rsGetElementAt_float2(input, x - 1, y + 1).x;
    x_est = fabs(rsGetElementAt_float(Gx, x, y) / pixel);
    if (pixel >= ((yBot[1] - yBot[0]) * x_est + yBot[0]) &&
        pixel >= ((yTop[1] - yTop[0]) * x_est + yTop[0])) {
      outputPixel.x = pixel;
      outputPixel.y = theta;
    } else {
      outputPixel.x = 0;
      outputPixel.y = theta;
    }
  }
  } else {
    outputPixel.x = pixel;
    outputPixel.y = theta;
  }

  return outputPixel;
}

float2 __attribute__((kernel)) doubleThreshold(uint32_t x, uint32_t y) {
  uchar pixel = 0;
  float theta;
  float2 inputPixel = rsGetElementAt_float2(input, x, y);
  pixel = inputPixel.x;
  theta = inputPixel.y;
  float2 outputPixel;
  outputPixel.y = theta;

  if (pixel > highThreshold)
    outputPixel.x = STRONG;
  else if (pixel > lowThreshold)
    outputPixel.x = WEAK;
  else
    outputPixel.x = 0;

  return outputPixel;
}

uchar __attribute__((kernel)) hystersis(uint32_t x, uint32_t y) {
  uchar pixel = 0;
  float theta;
  float2 inputPixel = rsGetElementAt_float2(input, x, y);
  pixel = inputPixel.x;
  theta = inputPixel.y;
  uchar4 outputPixel;

  if (pixel == 0)
    return 0;
  else if (pixel == 255)
    return 255;
  else if (pixel == 128) {
  if(x > 1 && y > 1 && x < gImageW - 2 && y < gImageH - 2){
    if (fabs(rsGetElementAt_float2(input, x - 1, y - 1).x) ==
        255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x, y - 1).x) == 255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x + 1, y - 1).x) == 255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x - 1, y).x) == 255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x + 1, y).x) == 255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x - 1, y + 1).x) == 255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x, y + 1).x) == 255) {
      return 255;
    } else if (fabs(rsGetElementAt_float2(input, x + 1, y + 1).x) == 255) {
      return 255;
    }
    }
    else {
        return 255;
    }
  }
  return 0;
}

void calculateCanny(rs_allocation output_image) {
    rsForEach(convolveKernel, temp_allocation);
    input = temp_allocation;
    rsForEach(supression, temp_allocation);
    input = temp_allocation;
    rsForEach(doubleThreshold, temp_allocation);
    input = temp_allocation;
    rsForEach(hystersis, output_image);
}