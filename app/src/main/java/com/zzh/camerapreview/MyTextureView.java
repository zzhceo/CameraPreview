package com.zzh.camerapreview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.TextureView;
import android.view.WindowManager;

import com.zzh.camerapreview.utils.ImageUtils;

public class MyTextureView extends TextureView {
    private static final String LOG_TAG = "MyTextureView";

    private int cameraPreviewWidth = 1920;
    private int cameraPreviewHeight = 1080;
    private boolean takePhoto;
    private Context context;
    private MyFaceDetectReceiver receiver;
    private Rect faceRect = new Rect();
    public static final String ACTION_FACE_DETECT_COORDINATE = "action.linktop.facedetect.coordinate";
    // like 1920,1080 : express previewWidth,previewHeight
    public static final String EXTRAS_CAMERA_PREVIEW_SIZE = "extras.camera.previewsize";
    // face detect face rect send like 0,0,100,100 : express face rect's left,top,right,bottom coordinate
    public static final String EXTRAS_FACE_DETECT_RECT = "extras.face.detect.rect";

    public static boolean saveGetBitmap;
    public static boolean saveClipBitmap;

    public MyTextureView(Context context) {
        super(context);
        this.context = context;
        Log.i(LOG_TAG, "MyTextureView(Context context)");
    }

    public MyTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        Log.i(LOG_TAG, "MyTextureView(Context context, AttributeSet attrs)");
    }

    public MyTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        Log.i(LOG_TAG, "MyTextureView(Context context, AttributeSet attrs, int defStyleAttr)");
    }

    public MyTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        Log.i(LOG_TAG, "MyTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)");
    }

//    @Override
//    public void draw(Canvas canvas) {
//        super.draw(canvas);
//        if (needRedrawCanvas())
//            clipAndScaleDrawCanvas(canvas);
//    }

    @Override
    protected void onAttachedToWindow() {
        Log.i(LOG_TAG, "onAttachedToWindow()");
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.i(LOG_TAG, "onDetachedFromWindow");
        unRegisterBroadcastReceiver();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (receiver == null) {
            int[] screenSize = getScreenWidthHeight();
            Log.i(LOG_TAG, "onLayout, screen width=" + screenSize[0] + ", height=" + screenSize[1] + ", view width=" + getWidth() + ", height=" + getHeight());
//            if (getWidth() <= screenSize[0] / 2)
                registerBroadcastReceiver();
        }
    }

    // add by zzh
    public void setCameraPreviewSize(int width, int height) {
        this.cameraPreviewWidth = width;
        this.cameraPreviewHeight = height;
        Log.i(LOG_TAG, " preview width=" + cameraPreviewWidth + ", preview height=" + cameraPreviewHeight);
    }

    // add by zzh
    public void setFaceRect(int left, int top, int right, int bottom) {
        if (faceRect != null) {
            float rate = 1.0f;//((getWidth()*getHeight()) * 1.0f) / (cameraPreviewWidth*cameraPreviewHeight);
            faceRect.left = (int) (left * rate);
            faceRect.top = (int) (top * rate);
            faceRect.right = (int) (right * rate);
            faceRect.bottom = (int) (bottom * rate);
            Log.i(LOG_TAG, " face rect before left=" + faceRect.left + ", top=" + faceRect.top
                    + ", right=" + faceRect.right + ", bottom=" + faceRect.bottom);
            if (faceRect.left != 0 && faceRect.top != 0
                    && faceRect.right != 0 && faceRect.bottom != 0)
                faceRect = faceRectRotate90(faceRect, cameraPreviewHeight);
            Log.i(LOG_TAG, " face rect after left=" + faceRect.left + ", top=" + faceRect.top
                    + ", right=" + faceRect.right + ", bottom=" + faceRect.bottom);
        }
    }

    // add by zzh
    public void clipAndScaleDrawCanvas(Canvas canvas) {
        long s1 = System.currentTimeMillis();
        Bitmap bitmap = getBitmap(cameraPreviewHeight, cameraPreviewWidth);
        if (saveGetBitmap) {
            ImageUtils.saveBitmapToFile(Environment.getExternalStorageDirectory()
                    + "/saveGetBitmap" + System.currentTimeMillis() + ".jpg", bitmap);
            saveGetBitmap = false;
        }
        long s2 = System.currentTimeMillis();
        Log.i(LOG_TAG, "clipAndScaleDrawCanvas cost time 1= " + (s2 - s1) + ", bitmap width=" + bitmap.getWidth() + ", height=" + bitmap.getHeight()
                + ", texture view width=" + getWidth() + ", height=" + getHeight());
        bitmap = ImageUtils.clipAndScaleBitmap(bitmap, getWidth(), getHeight(), faceRect);
        long s3 = System.currentTimeMillis();
        Log.i(LOG_TAG, "clipAndScaleDrawCanvas cost time 2= " + (s3 - s2));
        canvas.drawBitmap(bitmap, 0, 0, null);
        Log.i(LOG_TAG, "clipAndScaleDrawCanvas cost time 3= " + (System.currentTimeMillis() - s3));
        Log.i(LOG_TAG, "clipAndScaleDrawCanvas cost time = " + (System.currentTimeMillis() - s1));
    }

    // add by zzh, param is coordinate or bitmap height before rotate
    // because face coordinate is the raw image's, clockwise rotate
    private Rect faceRectRotate90(Rect srcRect, int srcBitmapHeight) {
        Rect destRect = new Rect();
        destRect.left = srcBitmapHeight - srcRect.bottom;
        destRect.top = srcRect.left;
        destRect.right = srcBitmapHeight - srcRect.top;
        destRect.bottom = srcRect.right;
        return destRect;
    }

    // add by zzh, param is coordinate or bitmap width before rotate
    // because face coordinate is the raw image's, anticlockwise rotate
    private Rect faceRectRotateAnti90(Rect srcRect, int srcBitmapWidth) {
        Rect destRect = new Rect();
        destRect.left = srcRect.top;
        destRect.top = srcBitmapWidth - srcRect.right;
        destRect.right = srcRect.bottom;
        destRect.bottom = srcBitmapWidth - srcRect.left;
        return destRect;
    }

    // add by zzh
    private void registerBroadcastReceiver() {
        Log.i(LOG_TAG, "registerBroadcastReceiver");
        if (receiver == null) {
            receiver = new MyFaceDetectReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_FACE_DETECT_COORDINATE);
            context.registerReceiver(receiver, intentFilter);
        }
    }

    // add by zzh
    private void unRegisterBroadcastReceiver() {
        Log.i(LOG_TAG, "unRegisterBroadcastReceiver");
        if (receiver != null) {
            context.unregisterReceiver(receiver);
            receiver = null;
        }
    }

    // add by zzh
    class MyFaceDetectReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "MyFaceDetectCoordinateReceiver receiver intent=" + intent);
            if (intent.getAction() != null &&
                ACTION_FACE_DETECT_COORDINATE.equals(intent.getAction()) &&
                (intent.hasExtra(EXTRAS_CAMERA_PREVIEW_SIZE) ||
                intent.hasExtra(EXTRAS_FACE_DETECT_RECT))) {
                String previewSize = intent.getStringExtra(EXTRAS_CAMERA_PREVIEW_SIZE);
                String faceCoordinate = intent.getStringExtra(EXTRAS_FACE_DETECT_RECT);
                if (!TextUtils.isEmpty(previewSize)) {
                    String[] previewSizeArray = previewSize.split(",");
                    if (previewSizeArray.length > 1)
                        setCameraPreviewSize(Integer.valueOf(previewSizeArray[0]), Integer.valueOf(previewSizeArray[1]));
                }
                if (!TextUtils.isEmpty(faceCoordinate)) {
                    String[] coordinate = faceCoordinate.split(",");
                    if (coordinate.length > 3) {
                        setFaceRect(Integer.valueOf(coordinate[0]), Integer.valueOf(coordinate[1]),
                                Integer.valueOf(coordinate[2]), Integer.valueOf(coordinate[3]));
                    }
                }
            }
        }
    }

    // add by zzh @{
    private boolean needRedrawCanvas() {
        //TODO FOR TEST
        if (true)
            return true;

        return receiver != null && cameraPreviewWidth != 0
                && cameraPreviewHeight != 0 && faceRect.right > 0
                && faceRect.bottom > 0;
    }
    // @}

    // add by zzh
    private int[] getScreenWidthHeight() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
//        Log.i(LOG_TAG, "getScreenWidthHeight width=" + width + ", height=" + height + ", getWidth=" + getMeasuredWidth() + ", getHeight=" + getMeasuredHeight());
        return new int[]{width, height};
    }
}
