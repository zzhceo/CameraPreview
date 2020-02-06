package com.zzh.camerapreview.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.text.TextUtils;
import android.util.Log;

import com.zzh.camerapreview.MyTextureView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    // add by zzh
    public static Bitmap nv21ToBitmapByYuvImage(byte[] nv21, int width, int height) {
        long s1 = System.currentTimeMillis();
        Log.e("time",">>>>>>nv21ToBitmapByYuvImage   start>>>");
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 90, stream);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size(), options);
            stream.close();
            Log.e("time",">>>>>>nv21ToBitmapByYuvImage   start 2>>>");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("time",">>>>>>nv21ToBitmapByYuvImage   end>>>, cost time=" + (System.currentTimeMillis() - s1) + " ms");
        return bitmap;
    }

    private static RenderScript rs;
    private static ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private static Type.Builder yuvType, rgbaType;
    private static Allocation in, out;
    /**
     *     将nv21转换成bitmap
     */
    public static Bitmap nv21ToBitmap(Context mContext, byte[] nv21, int width, int height){
        if(rs==null){
            rs = RenderScript.create(mContext);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_3(rs));
        }
        if (yuvType == null){
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        return bmpout;
    }

    /**
     *   对图片进行缩放
     * 未处理的预览数据转化的bitmap
     * destWidth,h预览尺寸宽和高
     *  add by zzh
     */
    public static Bitmap clipAndScaleBitmap(Bitmap srcBm, int destWidth, int destHeight, Rect clipRect) {
        //[2.1]创建一个模板  相当于 创建了一个大小和原图一样的 空白的白纸
        Bitmap copybiBitmap = Bitmap.createBitmap(destWidth, destHeight, srcBm.getConfig());
        //[2.3]创建一个画布  把白纸铺到画布上
        Canvas canvas = new Canvas(copybiBitmap);
        Rect destRect = new Rect(0, 0, destWidth, destHeight);
        if (clipRect == null || (clipRect.left == 0 && clipRect.top == 0 && clipRect.right == 0 && clipRect.bottom == 0)) {
            // 人脸坐标为0，则不作剪裁放大
            canvas.drawBitmap(srcBm, new Rect(0, 0, srcBm.getWidth(), srcBm.getHeight()), destRect, null);
            return copybiBitmap;
        }
        // 将人脸矩形放大到预览大小画到画布上，// faceSrcRect 为将人脸位置矩形坐标转化成以安卓坐标系坐标
        canvas.drawBitmap(srcBm, clipRect, destRect, null);
        // 画人脸框
		 /*Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(clipRect, paint);
		canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();*/
		Log.e("time", "faceSrcRect left, draw face rectangle");
        if (MyTextureView.saveClipBitmap) {
            ImageUtils.saveBitmapToFile(Environment.getExternalStorageDirectory()
                    + "/saveClipBitmap" + System.currentTimeMillis() + ".jpg", copybiBitmap);
            MyTextureView.saveClipBitmap = false;
        }

        return copybiBitmap;
    }

    /**
     * @param fileName need saved image full path
     * @param bitmap bitmap
     * */
    public static boolean saveBitmapToFile(String fileName, Bitmap bitmap) {
        if (bitmap == null || TextUtils.isEmpty(fileName))
            return false;
        File f = new File(fileName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Log.i(TAG, fileName + " had saved!");
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Bitmap doBrightness(Bitmap src, int value) {
        // image size
        int width = src.getWidth();
        int height = src.getHeight();
        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        // color information
        int A, R, G, B;
        int pixel;

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get pixel color
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                // increase/decrease each channel
                R += value;
                if (R > 255) {
                    R = 255;
                } else if (R < 0) {
                    R = 0;
                }

                G += value;
                if (G > 255) {
                    G = 255;
                } else if (G < 0) {
                    G = 0;
                }

                B += value;
                if (B > 255) {
                    B = 255;
                } else if (B < 0) {
                    B = 0;
                }
                // apply new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        // return final image
        return bmOut;
    }

    /**
     * 选择变换
     *
     * @param origin 原图
     * @param rotateAngle  旋转角度，可正可负，正为顺时针，负数为逆时针
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmap(Bitmap origin, float rotateAngle) {
        if (origin == null) {
            return null;
        }
        //创建一个与bitmap一样大小的result
        int destWidth = origin.getWidth();
        int destHeight = origin.getHeight();

        Bitmap result = Bitmap.createBitmap(destWidth, destHeight, origin.getConfig());
        Canvas canvas = new Canvas(result);
        //主要以这个对象调用旋转方法
        Matrix matrix = new Matrix();
        //以图片中心作为旋转中心，旋转rotateAngle
        matrix.setRotate(rotateAngle, result.getWidth() / 2, result.getHeight() / 2);
        Paint paint = new Paint();
        //设置抗锯齿,防止过多的失真
        paint.setAntiAlias(true);
        canvas.drawBitmap(origin, matrix, paint);

        return result;
    }
}
