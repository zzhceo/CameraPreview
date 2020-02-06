package com.zzh.camerapreview.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

public class YUVUtils {

    //顺时针旋转270度
    public static void YUV420spRotate270(byte[] des, byte[] src, int width, int height) {
        int n = 0;
        int uvHeight = height >> 1;
        int wh = width * height;
        //copy y
        for (int j = width - 1; j >= 0; j--) {
            for (int i = 0; i < height; i++) {
                des[n++] = src[width * i + j];
            }
        }

        for (int j = width - 1; j > 0; j -= 2) {
            for (int i = 0; i < uvHeight; i++) {
                des[n++] = src[wh + width * i + j - 1];
                des[n++] = src[wh + width * i + j];
            }
        }
    }



    //旋转180度（顺时逆时结果是一样的）
    public static void YUV420spRotate180(byte[] src, byte[] des, int width, int height) {
        int n = 0;
        int uh = height >> 1;
        int wh = width * height;
        //copy y
        for (int j = height - 1; j >= 0; j--) {
            for (int i = width - 1; i >= 0; i--) {
                des[n++] = src[width * j + i];
            }
        }
        for (int j = uh - 1; j >= 0; j--) {
            for (int i = width - 1; i > 0; i -= 2) {
                des[n] = src[wh + width * j + i - 1];
                des[n + 1] = src[wh + width * j + i];
                n += 2;
            }
        }
    }

    //顺时针旋转90
    public static void YUV420spRotate90Clockwise(byte[] src, byte[] dst, int srcWidth, int srcHeight) {
        int wh = srcWidth * srcHeight;
        int uvHeight = srcHeight >> 1;
        //旋转Y
        int k = 0;
        for (int i = 0; i < srcWidth; i++) {
            int nPos = 0;
            for (int j = 0; j < srcHeight; j++) {
                dst[k] = src[nPos + i];
                k++;
                nPos += srcWidth;
            }
        }
        for (int i = 0; i < srcWidth; i += 2) {
            int nPos = wh;
            for (int j = 0; j < uvHeight; j++) {
                dst[k] = src[nPos + i];
                dst[k + 1] = src[nPos + i + 1];
                k += 2;
                nPos += srcWidth;
            }
        }
    }



    //逆时针旋转90

    private void YUV420spRotate90Anticlockwise(byte[] src, byte[] dst, int width, int height) {

        int wh = width * height;

        int uvHeight = height >> 1;



        //旋转Y

        int k = 0;

        for (int i = 0; i < width; i++) {

            int nPos = width - 1;

            for (int j = 0; j < height; j++) {

                dst[k] = src[nPos - i];

                k++;

                nPos += width;

            }

        }



        for (int i = 0; i < width; i += 2) {

            int nPos = wh + width - 1;

            for (int j = 0; j < uvHeight; j++) {

                dst[k] = src[nPos - i - 1];

                dst[k + 1] = src[nPos - i];

                k += 2;

                nPos += width;

            }

        }



        //不进行镜像翻转

//        for (int i = 0; i < width; i++) {

//            int nPos = width - 1;

//            for (int j = 0; j < height; j++) {

//                dst[k] = src[nPos - i];

//                k++;

//                nPos += width;

//            }

//        }

//        for (int i = 0; i < width; i += 2) {

//            int nPos = wh + width - 2;

//            for (int j = 0; j < uvHeight; j++) {

//                dst[k] = src[nPos - i];

//                dst[k + 1] = src[nPos - i + 1];

//                k += 2;

//                nPos += width;

//            }

//        }



    }



    //镜像

    private void Mirror(byte[] yuv_temp, int w, int h) {

        int i, j;



        int a, b;

        byte temp;

        //mirror y

        for (i = 0; i < h; i++) {

            a = i * w;

            b = (i + 1) * w - 1;

            while (a < b) {

                temp = yuv_temp[a];

                yuv_temp[a] = yuv_temp[b];

                yuv_temp[b] = temp;

                a++;

                b--;

            }

        }

        //mirror u

        int uindex = w * h;

        for (i = 0; i < h / 2; i++) {

            a = i * w / 2;

            b = (i + 1) * w / 2 - 1;

            while (a < b) {

                temp = yuv_temp[a + uindex];

                yuv_temp[a + uindex] = yuv_temp[b + uindex];

                yuv_temp[b + uindex] = temp;

                a++;

                b--;

            }

        }

        //mirror v

        uindex = w * h / 4 * 5;

        for (i = 0; i < h / 2; i++) {

            a = i * w / 2;

            b = (i + 1) * w / 2 - 1;

            while (a < b) {

                temp = yuv_temp[a + uindex];

                yuv_temp[a + uindex] = yuv_temp[b + uindex];

                yuv_temp[b + uindex] = temp;

                a++;

                b--;

            }

        }

    }

    /**
     * 将bitmap里得到的argb数据转成yuv420sp格式
     * 这个yuv420sp数据就可以直接传给MediaCodec,通过AvcEncoder间接进行编码
     *
     * @param yuv420sp 用来存放yuv429sp数据，长度=argb长度 * 3 / 2
     * @param argb     传入argb数据
     * @param width    图片width
     * @param height   图片height
     */
    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
    }

    // add by zzh @{
    private static RenderScript rs;
    private static ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private static Type.Builder yuvType, rgbaType;
    private static Allocation in, out;
    public static Bitmap yuvToBitmap(Context context, byte[] data, int prevSizeW, int prevSizeH) {
        if (rs == null)
            rs = RenderScript.create(context);
        if (yuvToRgbIntrinsic == null)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        if (yuvType == null) {
            int dataLength = data.length;
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(dataLength);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(prevSizeW).setY(prevSizeH);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(data);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(prevSizeW, prevSizeH, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        return bmpout;
        /*Bitmap clipBmp = Bitmap.createBitmap(bmpout, 100, 100, prevSizeW - 100, prevSizeH - 100);
        return clipBmp;*/
    }
    // @}

}
