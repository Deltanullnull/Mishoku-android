package com.deltanullnull.kenshoku;

import android.graphics.Matrix;

public class ImageUtils
{
    static final int kMaxChannelValue = 262143;
    private static boolean useNativeConversion = true;

    public static void convertYUV420ToARGB8888(
        byte [] yData,
        byte [] uData,
        byte [] vData,
        int width,
        int height,
        int yRowStride,
        int uvRowStride,
        int uvPixelStride,
        int [] out
    )
    {
        // use native conversion

        int yp = 0;
        for (int j = 0; j < height; j++)
        {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++)
            {
                int uvOffset = pUV * (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(
                        0xFF & yData[pY + i],
                        0xFF & uData[uvOffset],
                        0xFF & vData[uvOffset]);
            }
        }
    }

    public static void convertYUV420SPToARGB8888(
            byte [] input,
            int width,
            int height,
            int [] output
    )
    {
        if (useNativeConversion)
        {
            try
            {
                convertYUV420SPToARGB8888(input, output, width, height, false);
                return;
            }
            catch (final Exception e)
            {
                useNativeConversion = false;
            }
        }

        // Native conversion not possible. Use Java methods
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++)
        {
            int uvp = frameSize + (j >> 1) * width;
            int u = 0;
            int v = 0;

            for (int i = 0; i < width; i++, yp++)
            {
                int y = 0xff & input[yp];
                if ((i & 1) == 0)
                {
                    v = 0xff & input[uvp++];
                    u = 0xff & input[uvp++];
                }

                output[yp] = YUV2RGB(y, u, v);
            }
        }
    }

    private static int YUV2RGB(int y, int u, int v)
    {
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v  - 400 * u);
        int b = (y1192 + 2066 * u);

        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g << 2) & 0xff00) | ((b << 10) & 0xff);
    }

    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation,
            final boolean maintainAspectRatio
    )
    {
        final Matrix matrix = new Matrix();

        if (applyRotation != 0)
        {
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);
            matrix.postRotate(applyRotation);
        }

        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

        final int inWidth = transpose ? srcWidth : dstWidth;
        final int inHeight = transpose ? srcHeight : dstHeight;

        if (inWidth != dstWidth || inHeight != dstHeight)
        {
            final float scaleFactorX = dstWidth / (float) inWidth;
            final float scaleFactorY = dstHeight / (float) inHeight;

            if (maintainAspectRatio)
            {
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            }
            else
            {
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        if (applyRotation != 0)
        {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;
    }

    private static native void convertYUV420SPToARGB8888(
            byte [] input,
            int [] output,
            int width,
            int height,
            boolean halfSize
    );
}
