package com.zzh.camerapreview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.zzh.camerapreview.camera.CameraCallbacks;
import com.zzh.camerapreview.exif.Exif;
import com.zzh.camerapreview.exif.ExifInterface;
import com.zzh.camerapreview.manager.AudioRecordManager;
import com.zzh.camerapreview.permission.PermissionCheckUtil;
import com.zzh.camerapreview.utils.DateUtils;
import com.zzh.camerapreview.utils.ImageUtils;
import com.zzh.camerapreview.utils.NV21ToBitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements MediaRecorder.OnErrorListener,
        MediaRecorder.OnInfoListener, View.OnClickListener {
    private static final String TAG = "MainActivity";
    private LinearLayout textureViewParent;
    private MyTextureView textureView1;
    private TextureView textureView2;
    private TextureView textureView3;
    Button btn;
    Button btn1;
    boolean isAllCameraPreviewing = true;
    Button btnPreviewSize1;
    Button btnPreviewSize2;
    Button btnPreviewSize3;
    Button btnPreviewSize4;
    Button btnPreviewSizeMin;
    Button btnPreviewSizeMax;
    boolean isRestartPreview = false;
    Button btnFps1;
    Button btnFps2;
    Button btnFps3;
    Button btnFps4;
    Button btnRestartPreview;

    EditText editTextPreviewWidth;
    EditText editTextPreviewHeight;
    Button btnSetPreviewSize;
    EditText editTextFpsMin;
    EditText editTextFpsMax;
    Button btnSetFps;
    EditText editTextPictureWidth;
    EditText editTextPictureHeight;
    Button btnSetPictureSize;

    View layoutSetting;
    Button btnShowHideSetting;

    Button btnTakePhoto;

    // add by zzh
    private HashMap<Integer, TextureView> textureViewList = new HashMap<>();
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Camera> cameraHashMap = new HashMap<>();
    private AlertDialog mCameraUnavailableDialog;
    private AlertDialog cameraInfoDialog;
    ToneGenerator tone;

    private final int MIN_PREVIEW_SIZE_WIDTH = 0;
    private final int MIN_PREVIEW_SIZE_HEIGHT = 0;
    private final int MAX_PREVIEW_SIZE_WIDTH = 1;
    private final int MAX_PREVIEW_SIZE_HEIGHT = 1;
    // for use preview callback data shown on imageview @{
    private ImageView previewFrameView;
    private NV21ToBitmap nv21ToBitmap;
    private HandlerThread previewFrameThread;
    private final boolean isShowPreviewFrameView = true;
    private Handler previewFrameHandler;
    // @}

    private int previewWidth = 0;
    private int previewHeight = 0;
    private boolean isSavePreviewBitmap;
    private boolean isSaveTextureViewGetBitmap;
    private final int DEFAULT_PREVIEW_WIDTH = 1280;
    private final int DEFAULT_PREVIEW_HEIGHT = 720;

    AudioRecordManager audioRecordManager;
    boolean needAudioRecord = true;

    // for camera zoom test
    int currZoomValue = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        PermissionCheckUtil.requestAllPermissions(this);

        initView();
        if (isShowPreviewFrameView) {
            previewFrameView.setVisibility(View.VISIBLE);
            findViewById(R.id.frame_view_bg).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_top_btn).setVisibility(View.GONE);
            nv21ToBitmap = new NV21ToBitmap(this);
            previewFrameThread = new HandlerThread("FrameHandlerThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
            previewFrameThread.start();
            initPreviewFrameHandler();
        } else {
            previewFrameView.setVisibility(View.GONE);
            findViewById(R.id.frame_view_bg).setVisibility(View.GONE);
        }
        Log.i(TAG, "onCreate Camera.getNumberOfCameras()=" + Camera.getNumberOfCameras());
    }

    private void initView() {
        Log.d(TAG, "initView");
        previewFrameView = findViewById(R.id.img_preview_frame);
        textureViewParent = findViewById(R.id.textureview_parent);
        textureView1 = new MyTextureView(this);
        textureView2 = new TextureView(this);
        textureView3 = new TextureView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        lp.weight = 1;
        textureView1.setLayoutParams(lp);
        textureView2.setLayoutParams(lp);
        textureView3.setLayoutParams(lp);
        textureView1.setVisibility(View.VISIBLE);
        textureView2.setVisibility(View.GONE);
        textureView3.setVisibility(View.GONE);


        btn = findViewById(R.id.btn);
        btn1 = findViewById(R.id.btn1);
        btnPreviewSize1 = findViewById(R.id.btn_previewsize1);
        btnPreviewSize2 = findViewById(R.id.btn_previewsize2);
        btnPreviewSize3 = findViewById(R.id.btn_previewsize3);
        btnPreviewSize4 = findViewById(R.id.btn_previewsize4);
        btnPreviewSizeMin = findViewById(R.id.btn_previewsize_min);
        btnPreviewSizeMax = findViewById(R.id.btn_previewsize_max);

        btnFps1 = findViewById(R.id.btn_fps1);
        btnFps2 = findViewById(R.id.btn_fps2);
        btnFps3 = findViewById(R.id.btn_fps3);
        btnFps4 = findViewById(R.id.btn_fps4);
        btnRestartPreview = findViewById(R.id.btn_restartPreview);
        btnRestartPreview.setText(getString(R.string.restart_preview, getString(R.string.no)));

        editTextPreviewWidth = findViewById(R.id.txt_preview_width);
        editTextPreviewHeight = findViewById(R.id.txt_preview_height);
        btnSetPreviewSize = findViewById(R.id.btn_preview_set);
        editTextPictureWidth = findViewById(R.id.txt_picturesize_width);
        editTextPictureHeight = findViewById(R.id.txt_picturesize_height);
        btnSetPictureSize = findViewById(R.id.btn_picturesize_set);

        editTextFpsMin = findViewById(R.id.txt_fps_min);
        editTextFpsMax = findViewById(R.id.txt_fps_max);
        btnSetFps = findViewById(R.id.btn_fps_set);

        btnShowHideSetting = findViewById(R.id.btn_showhide_setting);
        layoutSetting = findViewById(R.id.layout_setting);
        if (layoutSetting.getVisibility() == View.VISIBLE) {
            btnShowHideSetting.setText(getString(R.string.hide_setting));
        } else {
            btnShowHideSetting.setText(getString(R.string.show_setting));
        }


        btnTakePhoto = findViewById(R.id.btn_take_photo);

        textureView1.setOnClickListener(this);
        textureView2.setOnClickListener(this);
        textureView3.setOnClickListener(this);

        btn.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btnPreviewSize1.setOnClickListener(this);
        btnPreviewSize2.setOnClickListener(this);
        btnPreviewSize3.setOnClickListener(this);
        btnPreviewSize4.setOnClickListener(this);
        btnPreviewSizeMin.setOnClickListener(this);
        btnPreviewSizeMax.setOnClickListener(this);

        btnFps1.setOnClickListener(this);
        btnFps2.setOnClickListener(this);
        btnFps3.setOnClickListener(this);
        btnFps4.setOnClickListener(this);
        btnRestartPreview.setOnClickListener(this);

        btnSetPreviewSize.setOnClickListener(this);
        btnSetPictureSize.setOnClickListener(this);
        btnSetFps.setOnClickListener(this);

        btnShowHideSetting.setOnClickListener(this);

        btnTakePhoto.setOnClickListener(this);
    }

    // add by zzh
    private void startAllCameraPreview() {
        if (isAllCameraPreviewing) {
            Log.i(TAG, "startAllCameraPreview fail, camera is previewing");
            return;
        }

        if (cameraHashMap != null) {
            int startPreviewCount = 0;
            for (Integer integer : cameraHashMap.keySet()) {
                Camera camera = cameraHashMap.get(integer);
                if (camera != null) {
                    Log.i(TAG, "已经开始cameraId=" + integer + "相机预览");
                    camera.startPreview();
                    startPreviewCount++;
//                    Toast.makeText(MainActivity.this, "已经开始cameraId=" + integer + "相机预览", Toast.LENGTH_SHORT).show();
                }
            }
            if (startPreviewCount == cameraHashMap.size())
                isAllCameraPreviewing = true;
        }
    }

    // add by zzh
    private void stopAllCameraPreview() {
        if (!isAllCameraPreviewing) {
            Log.i(TAG, "stopAllCameraPreview fail, camera is stop preview");
            return;
        }

        if (cameraHashMap != null) {
            int stopPreviewCount = 0;
            for (Integer integer : cameraHashMap.keySet()) {
                Camera camera = cameraHashMap.get(integer);
                if (camera != null) {
                    Log.i(TAG, "已经停止cameraId=" + integer + "相机预览");
                    camera.stopPreview();
                    stopPreviewCount++;
//                    Toast.makeText(MainActivity.this, "已经停止cameraId=" + integer + "相机预览", Toast.LENGTH_SHORT).show();
                }
            }
            if (stopPreviewCount == cameraHashMap.size())
                isAllCameraPreviewing = false;
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {

    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {

    }

    private void openCamera(final TextureView textureView, final int cameraId) {
        if (textureView == null || cameraId < 0 /*|| cameraId > Camera.getNumberOfCameras() - 1*/) {
            Log.w(TAG, "openCamera failed textureView=" + textureView
                    + ", cameraId=" + cameraId + ", Camera.getNumberOfCameras()=" + Camera.getNumberOfCameras());
            return;
        }
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                try {
                    Log.i(TAG, "surfaceCreated open camera cameraId=" + cameraId + " start");
                    Camera camera = Camera.open(cameraId);
                    if (!cameraHashMap.containsKey(cameraId))
                        cameraHashMap.put(cameraId, camera);
                    if (!textureViewList.containsKey(cameraId))
                        textureViewList.put(cameraId, textureView);
//                    camera.setDisplayOrientation(180);
                    camera.setDisplayOrientation(getCameraDisplayOrientation(MainActivity.this, cameraId));
                    camera.setPreviewTexture(surfaceTexture);
                    camera.setPreviewCallback(mCameraCallbacks);
                    Camera.Parameters parameters = camera.getParameters();

                    Camera.Size defaultPreviewSize = parameters.getPreviewSize();
                    boolean isSupportZoom = parameters.isZoomSupported();

                    int[] defaultPreviewFps = new int[2];
                    parameters.getPreviewFpsRange(defaultPreviewFps);
                    Log.i(TAG, "surfaceCreated defaultPreviewSize, width="
                            + defaultPreviewSize.width + ", height=" + defaultPreviewSize.height
                            + ", fps min=" + defaultPreviewFps[0] + ", max=" + defaultPreviewFps[1] + ", zoom support=" + isSupportZoom);

                    setCameraPreviewSize(cameraId, camera, DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT);

                    parameters = camera.getParameters();
                    defaultPreviewSize = parameters.getPreviewSize();
                    defaultPreviewFps = new int[2];
                    parameters.getPreviewFpsRange(defaultPreviewFps);
                    Camera.Size pictureSize = parameters.getPictureSize();
                    Log.i(TAG, "surfaceCreated after defaultPreviewSize, width="
                            + defaultPreviewSize.width + ", height=" + defaultPreviewSize.height
                            + ", fps min=" + defaultPreviewFps[0] + ", max=" + defaultPreviewFps[1]);

                    /*previewWidth = defaultPreviewSize.width;
                    previewHeight = defaultPreviewSize.height;
                    if (textureView instanceof MyTextureView) {
                        ((MyTextureView) textureView).setCameraPreviewSize(previewWidth, previewHeight);
                    }*/

                    /*int[] va = new int[2];
                    parameters.getPreviewViewingAngleRange(va);
                    parameters.setPreviewViewingAngleRange(1, 2);
                    List<int[]> valist = parameters.getSupportedPreviewViewingAngleRange();
                    if (valist != null && valist.size() > 0) {
                        for (int i = 0; i < valist.size(); i++)
                            Log.i(TAG, "supported va 0=" + valist.get(i)[0] + ", va 1=" + valist.get(i)[1]);
                    }
                    Log.i(TAG, "va 0=" + va[0] + ", va 1=" + va[1]);*/

                    Log.i(TAG, "surfaceCreated change afterPreviewSize, width="
                            + defaultPreviewSize.width + ", height=" + defaultPreviewSize.height
                            + ", fps min=" + defaultPreviewFps[0] + ", max=" + defaultPreviewFps[1]
                            + ", picture size width = " + pictureSize.width
                            + ", height=" + pictureSize.height);

                    // for face detect
//                    camera.setFaceDetectionListener(new FaceDetectorListener());
//                    startFaceDetection(camera);

//                    Camera.Parameters parameters3 = camera.getParameters();
//                    parameters3.setPreviewFpsRange(15 * 100, 20 * 100);
//                    camera.setParameters(parameters3);


                    camera.startPreview();
                } catch (IOException localIOException) {
                    Log.e(TAG, "surfaceCreated open camera localIOException cameraId=" + cameraId + ", error=" + localIOException.getMessage(), localIOException);
                } catch (Exception e) {
                    Log.e(TAG, "surfaceCreated open camera cameraId=" + cameraId + ", error=" + e.getMessage(), e);
                    textureView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                Log.i(TAG, "onSurfaceTextureSizeChanged cameraId=" + cameraId);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                Log.i(TAG, "onSurfaceTextureDestroyed cameraId=" + cameraId);
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//                Log.i(TAG, "onSurfaceTextureUpdated cameraId=" + cameraId);
                /*long s1 = System.currentTimeMillis();
                Bitmap bitmapDefault = textureView.getBitmap();
                long s2 = System.currentTimeMillis();
                Log.i(TAG, "onSurfaceTextureUpdated cost time 1= " + (s2 - s1));
                Bitmap bitmap = textureView.getBitmap(previewHeight, previewWidth);
                long s3 = System.currentTimeMillis();
                Log.i(TAG, "onSurfaceTextureUpdated cost time2= " + (s3 - s2));

                Log.i(TAG, "onSurfaceTextureUpdated save image, bitmap=" + bitmap
                        + ", previewWidth=" + previewWidth + ", previewHeight=" + previewHeight
                        + ", bitmapDefault width=" + bitmapDefault.getWidth() + ", height=" + bitmapDefault.getHeight()
                        + ", bitmap width=" + bitmap.getWidth() + ", height=" + bitmap.getHeight()
                        );
                if (isSaveTextureViewGetBitmap) {
                    ImageUtils.saveBitmapToFile(Environment.getExternalStorageDirectory()
                            + "/onSurfaceTextureUpdated" + System.currentTimeMillis() + ".jpg", bitmap);
                    isSaveTextureViewGetBitmap = false;
                }
                if (previewFrameView != null && bitmap != null) {
//                    Bitmap bitmap2 = ImageUtils.clipAndScaleBitmap(bitmap, previewFrameView.getWidth(), previewFrameView.getHeight(),
//                            new Rect(previewHeight / 4, previewWidth / 4, previewHeight / 4 + 100, previewWidth / 4 + 150));
                    previewFrameView.setImageBitmap(bitmap);
                }*/
            }


        });
    }

    // add by zzh
    private void setAllCameraPreviewSize(int width, int height) {
        if (cameraHashMap == null || cameraHashMap.size() <= 0) {
            Log.i(TAG, "setAllCameraPreviewSize failed");
            return;
        }
        Log.i(TAG, "setAllCameraPreviewSize width=" + width + ", height=" + height);
        for (Integer cameraId : cameraHashMap.keySet()) {
            Camera camera = cameraHashMap.get(cameraId);
            assert camera != null;
            setCameraPreviewSize(cameraId, camera, width, height);
        }
    }

    // add by zzh
    private void setCameraPreviewSize(int cameraId, Camera camera, int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        // set preview size
        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        Camera.Size expected = sizes.get(sizes.size() - 1);
        boolean gotIt = false;

        for (Camera.Size size : sizes) {
            Log.i(TAG, "all Preview size is w:" + size.width + " h:" + size.height);
        }

        if (width == MIN_PREVIEW_SIZE_WIDTH && height == MIN_PREVIEW_SIZE_HEIGHT) {
            expected = sizes.get(sizes.size() - 1);
            gotIt = true;
        } else if (width == MAX_PREVIEW_SIZE_WIDTH && height == MAX_PREVIEW_SIZE_HEIGHT) {
            expected = sizes.get(0);
            gotIt = true;
        } else {
            for (Camera.Size size : sizes) {
//                    Log.i(TAG, "Preview size is w:" + size.width + " h:" + size.height);
                if (size.width == width && size.height == height) {
                    expected = size;
                    gotIt = true;
                    Log.i(TAG, "setCameraPreviewSize width,height is support");
                    break;
                }
            }
        }

        int resultWidth = expected.width;
        int resultHeight = expected.height;
        if (!gotIt) {
            resultWidth = width;
            resultHeight = height;
            Log.i(TAG, "setCameraPreviewSize width,height is not support");
//            Toast.makeText(MainActivity.this, "不支持的预览宽:" + width + ",高:" + height + ", 设置失败", Toast.LENGTH_SHORT).show();
//            return;
        }
        previewWidth = resultWidth;
        previewHeight = resultHeight;
        if (textureViewList.containsKey(cameraId) && textureViewList.get(cameraId) instanceof MyTextureView)
            ((MyTextureView) textureViewList.get(cameraId)).setCameraPreviewSize(previewWidth, previewHeight);
        parameters.setPreviewSize(resultWidth, resultHeight);

        boolean showError = false;
        try {
            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "setCameraPreviewSize setParameters error=" + e.getMessage(), e);
            if (!isShowPreviewFrameView)
                Toast.makeText(MainActivity.this, "设置预览宽:" + resultWidth + ",高:" + resultHeight + " 出错：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            showError = true;
        }
        Camera.Size afterPreviewSize = parameters.getPreviewSize();
        Log.i(TAG, "setCameraPreviewSize camera id=" + cameraId + ", afterPreviewSize, width=" + afterPreviewSize.width + ", height=" + afterPreviewSize.height);

        if (!showError && !isShowPreviewFrameView)
            Toast.makeText(MainActivity.this, "修改后的预览宽:" + afterPreviewSize.width + ",高:" + afterPreviewSize.height
                    + ", 预设宽:" + width + ", 预设高:" + height, Toast.LENGTH_SHORT).show();
    }


    // add by zzh
    private void setAllCameraPictureSize(int width, int height) {
        if (cameraHashMap == null || cameraHashMap.size() <= 0) {
            Log.i(TAG, "setAllCameraPictureSize failed");
            return;
        }
        Log.i(TAG, "setAllCameraPictureSize width=" + width + ", height=" + height);
        for (Integer cameraId : cameraHashMap.keySet()) {
            Camera camera = cameraHashMap.get(cameraId);
            assert camera != null;
            setCameraPictureSize(cameraId, camera, width, height);
        }
    }

    private void setCameraPictureSize(int cameraId, Camera camera, int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        // set preview size
        List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
        Camera.Size expected = sizes.get(sizes.size() - 1);
        boolean gotIt = false;

        for (Camera.Size size : sizes) {
            Log.i(TAG, "all Picture size is w:" + size.width + " h:" + size.height);
        }
        for (Camera.Size size : sizes) {
            if (size.width == width && size.height == height) {
                expected = size;
                gotIt = true;
                Log.i(TAG, "setCameraPictureSize width,height is support");
                break;
            }
        }

        int resultWidth = expected.width;
        int resultHeight = expected.height;
        if (!gotIt) {
            resultWidth = width;
            resultHeight = height;
            Log.i(TAG, "setAllCameraPictureSize width,height is not support");
//            Toast.makeText(MainActivity.this, "不支持的预览宽:" + width + ",高:" + height + ", 设置失败", Toast.LENGTH_SHORT).show();
//            return;
        }
        parameters.setPictureSize(resultWidth, resultHeight);

        boolean showError = false;
        try {
            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "setAllCameraPictureSize setParameters error=" + e.getMessage(), e);
            Toast.makeText(MainActivity.this, "设置预览宽:" + resultWidth + ",高:" + resultHeight + " 出错：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            showError = true;
        }
        Camera.Size afterPictureSize = parameters.getPictureSize();
        Log.i(TAG, "setAllCameraPictureSize camera id=" + cameraId + ", afterPictureSize, width=" + afterPictureSize.width + ", height=" + afterPictureSize.height);

        if (!showError)
            Toast.makeText(MainActivity.this, "修改后的照片宽:" + afterPictureSize.width + ",高:" + afterPictureSize.height
                    + ", 预设宽:" + width + ", 预设高:" + height, Toast.LENGTH_SHORT).show();
    }

    // add by zzh
    private void setAllCameraPreviewFpsRange(int min, int max) {
        Log.i(TAG, "setAllCameraPreviewFpsRange min=" + min + ", max=" + max);
        for (Integer cameraId : cameraHashMap.keySet()) {
            Camera camera = cameraHashMap.get(cameraId);
            assert camera != null;
            setCameraPreviewFpsRange(cameraId, camera, min, max);
        }
    }

    private void setCameraPreviewFpsRange(int cameraId, Camera camera, int min, int max) {
        Camera.Parameters parameters = camera.getParameters();
        // set preview fps range
        List<int[]> supportFpsRange = camera.getParameters().getSupportedPreviewFpsRange();
        int minTemp = 30000, maxTemp = 30000;
        boolean gotIt = false;
//        for (int j = 0; j < supportFpsRange.size(); j++) {
//            Log.i(TAG, "setCameraPreviewFpsRange camera id=" + cameraId + ", fps, min=" + supportFpsRange.get(j)[0] + ", max=" + supportFpsRange.get(j)[1]);
//        }
        for (int j = 0; j < supportFpsRange.size(); j++) {
            Log.i(TAG, "setCameraPreviewFpsRange camera id=" + cameraId + ", fps, min=" + supportFpsRange.get(j)[0] + ", max=" + supportFpsRange.get(j)[1]);
            if (min == supportFpsRange.get(j)[0] && max == supportFpsRange.get(j)[1]) {
                gotIt = true;
                minTemp = supportFpsRange.get(j)[0];
                maxTemp = supportFpsRange.get(j)[1];
                Log.i(TAG, "setCameraPreviewFpsRange min, max is support");
                break;
            }
        }

        if (!gotIt) {
            minTemp = min;
            maxTemp = max;
            Log.i(TAG, "setCameraPreviewFpsRange min, max is not support");
//            Toast.makeText(MainActivity.this, "不支持的fps min:" + minTemp + ",max:" + maxTemp + ", 设置失败", Toast.LENGTH_SHORT).show();
//            return;
        }
        parameters.setPreviewFpsRange(minTemp, maxTemp);

        boolean showError = false;
        try {
            camera.setParameters(parameters);
        } catch (Exception e) {
            Log.e(TAG, "setCameraPreviewFpsRange setParameters error=" + e.getMessage(), e);
            Toast.makeText(MainActivity.this, "设置fps min:" + (minTemp / 1000) + ",max:" + (maxTemp / 1000) + " 出错：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            showError = true;
        }

        parameters = camera.getParameters();
        int[] afterFpsRange = new int[2];
        parameters.getPreviewFpsRange(afterFpsRange);

        Log.i(TAG, "setCameraPreviewFpsRange camera id=" + cameraId + ", after fps, min=" + afterFpsRange[0] + ", max=" + afterFpsRange[1]);
        if (!showError)
            Toast.makeText(MainActivity.this, "修改后的fps min:" + afterFpsRange[0] + ",max:" + afterFpsRange[1], Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        textureViewList.clear();
        cameraHashMap.clear();
        textureViewParent.addView(textureView1);
        textureViewParent.addView(textureView2);
        textureViewParent.addView(textureView3);
        // open camera id is 0 camera
        openCamera(textureView1, 0);
        // open camera id is 1 camera
//        openCamera(textureView2, 1);
        // open camera id is 2 camerasetSurfaceTextureListener
//        openCamera(textureView3, 2);
        if (needAudioRecord) {
            if (audioRecordManager == null) {
                audioRecordManager = new AudioRecordManager(null);
                audioRecordManager.setSaveFileParam(false);
            }
            audioRecordManager.startRecording();
        }
        hideBottomUIMenu();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
        textureViewParent.removeView(textureView1);
        textureViewParent.removeView(textureView2);
        textureViewParent.removeView(textureView3);
        for (Integer integer : cameraHashMap.keySet()) {
            if (textureViewList.containsKey(integer) && textureViewList.get(integer) != null)
                textureViewList.get(integer).setSurfaceTextureListener(null);
            Camera camera = cameraHashMap.get(integer);
            assert camera != null;
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
        }
        if (needAudioRecord) {
            audioRecordManager.stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    // add by zzh
    public static int getCameraDisplayOrientation(Activity activity, int cameraId) {
        if (activity == null) {
            Log.e(TAG, "setCameraDisplayOrientation failed activity is null");
            return 0;
        }
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.i(TAG, "setCameraDisplayOrientation result=" + result);
        return result;
    }

    // add by zzh
    private CameraCallbacks mCameraCallbacks = new CameraCallbacks() {
        @Override
        public void onCameraUnavailable(int errorCode) {
            Log.i(TAG, "camera unavailable, reason=%d" + errorCode);
            showCameraUnavailableDialog(errorCode);
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (isSavePreviewBitmap) {
                Bitmap bitmap = ImageUtils.nv21ToBitmapByYuvImage(data, previewWidth, previewHeight);
//                    bitmap = ImageUtils.rotateBitmap(bitmap, 90);
                ImageUtils.saveBitmapToFile(Environment.getExternalStorageDirectory()
                        + "/onPreviewFrame" + System.currentTimeMillis() + ".jpg", bitmap);
                isSavePreviewBitmap = false;
            }
//            for (Integer integer : cameraHashMap.keySet()) {
//                Camera temp = cameraHashMap.get(integer);
//                if (temp != null && temp.equals(camera)) {
//                    Log.i(TAG, "onPreviewFrame, camera id=" + integer);
//                    break;
//                }
//            }
            if (isShowPreviewFrameView) {
                previewFrameHandler.removeMessages(1);
                previewFrameHandler.obtainMessage(1, data).sendToTarget();
            }
        }
    };

    // add by zzh
    public void showCameraUnavailableDialog(int errorCode) {
        if (mCameraUnavailableDialog == null) {
            mCameraUnavailableDialog = new AlertDialog.Builder(this)
                    .setTitle("摄像头不可用")
                    .setMessage(getString(R.string.please_restart_device_or_app, errorCode))
                    .setPositiveButton("重试", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    recreate();
                                }
                            });
                        }
                    })
                    .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });
                        }
                    })
                    .create();
        }
        if (!mCameraUnavailableDialog.isShowing()) {
            mCameraUnavailableDialog.show();
        }
    }

    // add by zzh
    private String getSupportedPreviewSizeListStr(Camera camera) {
        List<Camera.Size> previewSizeList = camera.getParameters().getSupportedPreviewSizes();
        String previewSizeListStr = "支持的预览尺寸";
        int index = 1;
        if (previewSizeList != null && previewSizeList.size() > 0) {
            previewSizeListStr += "(" + previewSizeList.size() + "组):" + "\n";
            StringBuilder temp = new StringBuilder();
            for (Camera.Size size : previewSizeList) {
                temp.append(size.width).append("x").append(size.height).append(", ");
                if (index % 3 == 0) {
                    temp.delete(temp.length() - 2, temp.length() - 1);
                    temp.append("\n");
                }
                index++;
            }
            // del last line end comma
            if (temp.length() >= 2) {
                temp.delete(temp.length() - 2, temp.length() - 1);
            }
            previewSizeListStr += temp;
        } else
            previewSizeListStr += "无";
        return previewSizeListStr;
    }

    // add by zzh
    private String getSupportedPictureSizeListStr(Camera camera) {
        List<Camera.Size> pictureSizeList = camera.getParameters().getSupportedPictureSizes();
        String previewSizeListStr = "支持的照片尺寸";
        int index = 1;
        if (pictureSizeList != null && pictureSizeList.size() > 0) {
            previewSizeListStr += "(" + pictureSizeList.size() + "组):" + "\n";
            StringBuilder temp = new StringBuilder();
            for (Camera.Size size : pictureSizeList) {
                temp.append(size.width).append("x").append(size.height).append(", ");
                if (index % 3 == 0) {
                    temp.delete(temp.length() - 2, temp.length() - 1);
                    temp.append("\n");
                }
                index++;
            }
            // del last line end comma
            if (temp.length() >= 2) {
                temp.delete(temp.length() - 2, temp.length() - 1);
            }
            previewSizeListStr += temp;
        } else
            previewSizeListStr += "无";
        return previewSizeListStr;
    }

    // add by zzh
    private String getSupportedPreviewFpsListStr(Camera camera) {
        List<int[]> previewFpsList = camera.getParameters().getSupportedPreviewFpsRange();
        String previewFpsListStr = "支持的预览FPS";
        int index = 1;
        if (previewFpsList != null && previewFpsList.size() > 0) {
            previewFpsListStr += "(" + previewFpsList.size() + "组):" + "\n";
            StringBuilder temp = new StringBuilder();
            for (int[] range : previewFpsList) {
                temp.append(range[0] / 1000).append("x").append(range[1] / 1000).append(", ");
                if (index % 3 == 0) {
                    // del line last comma
                    temp.delete(temp.length() - 2, temp.length() - 1);
                    temp.append("\n");
                }
                index++;
            }
            // del last line end comma
            if (temp.length() >= 2) {
                temp.delete(temp.length() - 2, temp.length() - 1);
            }

            previewFpsListStr += temp;
        } else
            previewFpsListStr += "无";
        return previewFpsListStr;
    }

    // add by zzh
    public void showCameraInfoDialog(int cameraId, Camera camera) {
        if (cameraInfoDialog == null) {
            String previewSizeListStr = getSupportedPreviewSizeListStr(camera);
            String pictureSizeListStr = getSupportedPictureSizeListStr(camera);
            String previewFpsListStr = getSupportedPreviewFpsListStr(camera);

            cameraInfoDialog = new AlertDialog.Builder(this)
                    .setTitle("摄像头ID:" + cameraId + "的摄像头信息")
                    .setMessage(previewSizeListStr + "\n" + pictureSizeListStr + "\n" + previewFpsListStr)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                }
                            });
                        }
                    })
                    .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                }
                            });
                        }
                    })
                    .create();
        }
        if (!cameraInfoDialog.isShowing()) {
            cameraInfoDialog.show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                btnPreviewSize1.performClick();
                return true;
            case KeyEvent.KEYCODE_1:
                btnPreviewSize2.performClick();
                return true;
            case KeyEvent.KEYCODE_2:
                btnPreviewSize3.performClick();
                return true;
            case KeyEvent.KEYCODE_3:
                btnPreviewSize4.performClick();
                return true;
            case KeyEvent.KEYCODE_4:
                btnFps1.performClick();
                return true;
            case KeyEvent.KEYCODE_5:
                btnFps2.performClick();
                return true;
            case KeyEvent.KEYCODE_6:
                btnFps3.performClick();
                return true;
            case KeyEvent.KEYCODE_7:
                btnFps4.performClick();
                return true;
            case KeyEvent.KEYCODE_8:
                btnRestartPreview.performClick();
                return true;
            case KeyEvent.KEYCODE_9:
                btnPreviewSizeMin.performClick();
                return true;
            case KeyEvent.KEYCODE_BUTTON_10:
                btnPreviewSizeMax.performClick();
                return true;
            case KeyEvent.KEYCODE_11:
                textureView1.performClick();
                return true;
            case KeyEvent.KEYCODE_12:
                btnShowHideSetting.performClick();
                return true;
            case KeyEvent.KEYCODE_BUTTON_13:
                btnTakePhoto.performClick();
                return true;
            case KeyEvent.KEYCODE_BUTTON_14:
                Intent intent = new Intent(MyTextureView.ACTION_FACE_DETECT_COORDINATE);
                intent.putExtra(MyTextureView.EXTRAS_CAMERA_PREVIEW_SIZE, previewWidth + "," + previewHeight);
                intent.putExtra(MyTextureView.EXTRAS_FACE_DETECT_RECT, 1080 / 4 + "," + 1920 / 4 + "," + (1080 / 4 + 360) + "," + (1920 / 4 + 640));
                sendBroadcast(intent);
                return true;
            case KeyEvent.KEYCODE_BUTTON_15:
                Intent intent2 = new Intent(MyTextureView.ACTION_FACE_DETECT_COORDINATE);
                intent2.putExtra(MyTextureView.EXTRAS_CAMERA_PREVIEW_SIZE, previewWidth + "," + previewHeight);
                intent2.putExtra(MyTextureView.EXTRAS_FACE_DETECT_RECT, "0,0," + previewHeight + "," + previewWidth);
                sendBroadcast(intent2);
                return true;
            case KeyEvent.KEYCODE_BUTTON_16:
                MyTextureView.saveGetBitmap = MyTextureView.saveClipBitmap = true;
                return true;
//            case KeyEvent.KEYCODE_BUTTON_L1:
//                for (int i = 0; i < cameraHashMap.size(); i++) {
//                    Camera camera = cameraHashMap.get(i);
//                    Camera.Parameters parameters = camera.getParameters();
//                    Log.i(TAG, i + "=id, max zoom value=" + parameters.getMaxZoom());
//                    if (currZoomValue < parameters.getMaxZoom()) {
//                        currZoomValue++;
//                        parameters.setZoom(currZoomValue);
//                        Log.i(TAG, "increase zoom value=" + currZoomValue);
//                        camera.setParameters(parameters);
//                    }
//                }
//                return true;
//            case KeyEvent.KEYCODE_BUTTON_L2:
//                for (int i = 0; i < cameraHashMap.size(); i++) {
//                    Camera camera = cameraHashMap.get(i);
//                    Camera.Parameters parameters = camera.getParameters();
//                    if (currZoomValue > 1) {
//                        currZoomValue--;
//                        parameters.setZoom(currZoomValue);
//                        Log.i(TAG, "decrease zoom value=" + currZoomValue);
//                        camera.setParameters(parameters);
//                    }
//                }
//                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (isShowPreviewFrameView)
            return;

        if (v == textureView1) {
            if (cameraInfoDialog != null && cameraInfoDialog.isShowing()) {
                cameraInfoDialog.dismiss();
            } else {
                if (cameraHashMap != null) {
                    for (Integer integer : cameraHashMap.keySet()) {
                        Camera camera = cameraHashMap.get(integer);
                        if (camera != null) {
                            showCameraInfoDialog(integer, camera);
                            break;
                        }
                    }
                }
            }
            return;
        } else if (v == textureView2) {
            if (cameraInfoDialog != null && cameraInfoDialog.isShowing()) {
                cameraInfoDialog.dismiss();
            } else {
                if (cameraHashMap != null) {
                    int index = 0;
                    for (Integer integer : cameraHashMap.keySet()) {
                        Camera camera = cameraHashMap.get(integer);
                        if (camera != null && index++ == 1) {
                            showCameraInfoDialog(integer, camera);
                            break;
                        }
                    }
                }
            }
            return;
        } else if (v == textureView3) {
            if (cameraInfoDialog != null && cameraInfoDialog.isShowing()) {
                cameraInfoDialog.dismiss();
            } else {
                if (cameraHashMap != null) {
                    int index = 0;
                    for (Integer integer : cameraHashMap.keySet()) {
                        Camera camera = cameraHashMap.get(integer);
                        if (camera != null && index++ == 2) {
                            showCameraInfoDialog(integer, camera);
                            break;
                        }
                    }
                }
            }
            return;
        }
        int viewId = v.getId();
        switch (viewId) {
            case R.id.btn:
                startAllCameraPreview();
                break;
            case R.id.btn1:
                stopAllCameraPreview();
                break;
            case R.id.btn_previewsize1:
            case R.id.btn_previewsize2:
            case R.id.btn_previewsize3:
            case R.id.btn_previewsize4:
                if (isRestartPreview)
                    stopAllCameraPreview();

                Button b = (Button) v;
                String buttonText = b.getText().toString();
                if (!TextUtils.isEmpty(buttonText)) {
                    String[] strings = buttonText.split("x");
                    if (strings.length > 1) {
                        setAllCameraPreviewSize(Integer.valueOf(strings[0]), Integer.valueOf(strings[1]));
                    }
                }
                if (isRestartPreview)
                    startAllCameraPreview();
                break;
            case R.id.btn_previewsize_min:
                setAllCameraPreviewSize(MIN_PREVIEW_SIZE_WIDTH, MIN_PREVIEW_SIZE_HEIGHT);
                break;
            case R.id.btn_previewsize_max:
                setAllCameraPreviewSize(MAX_PREVIEW_SIZE_WIDTH, MAX_PREVIEW_SIZE_HEIGHT);
                break;
            case R.id.btn_fps1:
            case R.id.btn_fps2:
            case R.id.btn_fps3:
            case R.id.btn_fps4:
                if (isRestartPreview)
                    stopAllCameraPreview();

                Button b2 = (Button) v;
                String buttonText2 = b2.getText().toString();
                if (!TextUtils.isEmpty(buttonText2)) {
                    String[] strings = buttonText2.split("x");
                    if (strings.length > 1) {
                        setAllCameraPreviewFpsRange(Integer.valueOf(strings[0]) * 1000, Integer.valueOf(strings[1]) * 1000);
                    }
                }
                if (isRestartPreview)
                    startAllCameraPreview();
                break;
            case R.id.btn_restartPreview:
                isRestartPreview = !isRestartPreview;
                btnRestartPreview.setText(getString(R.string.restart_preview, isRestartPreview ? getString(R.string.yes) : getString(R.string.no)));
                break;

            case R.id.btn_preview_set:
                if (isRestartPreview)
                    stopAllCameraPreview();
                int width = Integer.valueOf(editTextPreviewWidth.getHint().toString());
                int height = Integer.valueOf(editTextPreviewHeight.getHint().toString());
                if (!TextUtils.isEmpty(editTextPreviewWidth.getText().toString()) && !TextUtils.isEmpty(editTextPreviewHeight.getText().toString())) {
                    width = Integer.valueOf(editTextPreviewWidth.getText().toString());
                    height = Integer.valueOf(editTextPreviewHeight.getText().toString());
                }
                setAllCameraPreviewSize(width, height);
                if (isRestartPreview)
                    startAllCameraPreview();
                break;
            case R.id.btn_picturesize_set:
                if (isRestartPreview)
                    stopAllCameraPreview();
                int width2 = Integer.valueOf(editTextPictureWidth.getHint().toString());
                int height2 = Integer.valueOf(editTextPictureHeight.getHint().toString());
                if (!TextUtils.isEmpty(editTextPictureWidth.getText().toString()) && !TextUtils.isEmpty(editTextPictureHeight.getText().toString())) {
                    width2 = Integer.valueOf(editTextPictureWidth.getText().toString());
                    height2 = Integer.valueOf(editTextPictureHeight.getText().toString());
                }
                setAllCameraPictureSize(width2, height2);
                if (isRestartPreview)
                    startAllCameraPreview();
                break;
            case R.id.btn_fps_set:
                if (isRestartPreview)
                    stopAllCameraPreview();
                int min = Integer.valueOf(editTextFpsMin.getHint().toString()) * 1000;
                int max = Integer.valueOf(editTextFpsMax.getHint().toString()) * 1000;
                if (!TextUtils.isEmpty(editTextFpsMin.getText().toString()) && !TextUtils.isEmpty(editTextFpsMax.getText().toString())) {
                    min = Integer.valueOf(editTextFpsMin.getText().toString()) * 1000;
                    max = Integer.valueOf(editTextFpsMax.getText().toString()) * 1000;
                }
                setAllCameraPreviewFpsRange(min, max);
                if (isRestartPreview)
                    startAllCameraPreview();
                break;
            case R.id.btn_showhide_setting:
                if (layoutSetting.getVisibility() == View.VISIBLE) {
                    layoutSetting.setVisibility(View.GONE);
                    btnShowHideSetting.setText(getString(R.string.show_setting));
                } else {
                    layoutSetting.setVisibility(View.VISIBLE);
                    btnShowHideSetting.setText(getString(R.string.hide_setting));
                }
                break;

            case R.id.btn_take_photo:
                MyTextureView.saveGetBitmap = MyTextureView.saveClipBitmap = true;
                isSavePreviewBitmap = true;
                isSaveTextureViewGetBitmap = true;
                takePhoto();
                break;
        }
    }

    // add by zzh
    private void takePhoto() {
        btnTakePhoto.setEnabled(false);
        if (cameraHashMap != null) {
            for (Integer integer : cameraHashMap.keySet()) {
                Camera camera = cameraHashMap.get(integer);
                if (camera != null) {
                    Log.i(TAG, "takePhoto cameraId=" + integer);
                    camera.takePicture(shutterCallback, null, null, new CameraPictureCallback());
                }
            }
        }
    }

    // add by zzh
    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            // 发出提示用户的声音
            if (tone == null) {
                tone = new ToneGenerator(
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
                        ToneGenerator.MIN_VOLUME);
            }
            tone.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
    };


    // add by zzh
    private final class CameraPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] jpegData, Camera camera) {
            Log.i(TAG, "CameraPictureCallback onPictureTaken data=" + jpegData);
            // jpeg data
            if (jpegData != null) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getJpegThumbnailSize();
                ImageSaveTask task = new ImageSaveTask(jpegData, camera, size.width, size.height);
                task.execute();
            }
        }
    }

    public static byte[] addExifTags(byte[] jpeg, int orientationInDegree) {
        ExifInterface exif = new ExifInterface();
        exif.addOrientationTag(orientationInDegree);
        ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
        try {
            exif.writeExif(jpeg, jpegOut);
        } catch (IOException e) {
            Log.e(TAG, "Could not write EXIF", e);
        }
        return jpegOut.toByteArray();
    }

    // add by zzh
    public static String writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            return path;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
        return "";
    }

    private class ImageSaveTask extends AsyncTask<Void, Void, String> {
        private byte[] data;
        Camera camera;
        int width, height;

        ImageSaveTask(byte[] data, Camera camera, int width, int height) {
            this.data = data;
            this.camera = camera;
            this.width = width;
            this.height = height;
        }

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected String doInBackground(Void... v) {
            // resolve preview stopped after take picture
            if (camera != null)
                camera.startPreview();
            if (data == null) {
                return "";
            }
            byte[] dst = new byte[data.length];
            dst = data;

//            YUVUtils.YUV420spRotate90Clockwise(data, dst, width, height);

            ExifInterface exif = Exif.getExif(dst);
            int orientation = Exif.getOrientation(exif);
            Log.i(TAG, "data orientation = " + orientation + ", width=" + width + ", height=" + height
                    + ", data length=" + data.length + ", width x height = " + (width * height * 3 / 2));
            dst = addExifTags(dst, orientation);
            return writeFile(Environment.getExternalStorageDirectory() + File.separator
                    + DateUtils.longToDateString(System.currentTimeMillis(), DateUtils.FORMAT_NO_DIVISION_DATE) + ".jpg", dst);
        }

        @Override
        protected void onPostExecute(String result) {
            btnTakePhoto.setEnabled(true);
            if (!TextUtils.isEmpty(result))
                Toast.makeText(MainActivity.this, "拍照完成，目录:" + result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(MainActivity.this, "拍照失败" + result, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 脸部检测接口
     * add by zzh
     */
    private class FaceDetectorListener implements Camera.FaceDetectionListener {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Camera.Face face = faces[0];
                Rect rect = face.rect;
                Rect androidCoorRect = getDestCoordinate(rect, previewWidth, previewHeight);

                textureView1.setFaceRect(androidCoorRect.left, androidCoorRect.top, androidCoorRect.right, androidCoorRect.bottom);

                Log.d(TAG, "confidence：" + face.score + "face detected: " + faces.length +
                        " Face 1 Location X: " + rect.centerX() +
                        "Y: " + rect.centerY() +
                        "   " + rect.left + " " + rect.top + " " + rect.right + " " + rect.bottom +
                        ", androidCoorRect left=" + androidCoorRect.left + ", top" + androidCoorRect.top +
                        ", right" + androidCoorRect.right + ", bottom=" + androidCoorRect.bottom
                );
            } else {
                // 只会执行一次
                Log.e(TAG, "【FaceDetectorListener】类的方法：【onFaceDetection】: " + "没有脸部");
            }
        }
    }

    public void startFaceDetection(Camera mCamera) {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();
        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // mCamera supports face detection, so can start it:
            try {
                mCamera.startFaceDetection();
            } catch (Exception e) {
                Log.e(TAG, "startFaceDetection error=" + e.getMessage());
                // Invoked this method throw exception on some devices but also can detect.
            }
        } else {
            Toast.makeText(this, "Device not support face detection", Toast.LENGTH_SHORT).show();
        }
    }

    private Rect getDestCoordinate(Rect rect, int previewWidth, int previewHeight) {
        Log.d(TAG, "getDestCoordinate rect left=" + rect.left + ", right=" + rect.top + ", top=" + rect.top + ", bottom=" + rect.bottom);
        Log.d(TAG, "getDestCoordinate rect previewSize width=" + previewWidth + ", height=" + previewHeight);
        RectF result = new RectF();
        float rate = 1.0f;//surfaceView.getHeight() * 1.0f / surfaceView.getWidth();
        result.left = (rect.left * (previewWidth / 2000f) + previewWidth / 2f) * rate;
        result.right = (rect.right * (previewWidth / 2000f) + previewWidth / 2f) * rate;
        result.top = (rect.top * (previewHeight / 2000f) + previewHeight / 2f) * rate;
        result.bottom = (rect.bottom * (previewHeight / 2000f) + previewHeight / 2f) * rate;
        Log.d(TAG, "getDestCoordinate result left=" + result.left + ", right=" + result.top + ", top=" + result.top + ", bottom=" + result.bottom);
        Rect resultRect = new Rect();
        result.round(resultRect);
        return resultRect;
    }

    private void initPreviewFrameHandler() {
        previewFrameHandler = new
                Handler(previewFrameThread.getLooper()) {
                    public void handleMessage(Message msg) {
                        try {
                            if (msg != null) {
                                byte[] data = (byte[]) msg.obj;
                                if (data != null && data.length > 0) {
//                                    long t1 = System.currentTimeMillis();
                                    Bitmap bitmap = nv21ToBitmap.nv21ToBitmap(data, previewWidth, previewHeight);
                                    bitmap = ImageUtils.convert(bitmap, 0, true, true);
//                                    long t2 = System.currentTimeMillis();
//                                    Log.e(TAG, "previewFrameHandler nv 21 to bitmap time= " + (t2 - t1) + " ms");
                                    final Bitmap rotateBitmap = ImageUtils.rotateBitmap(bitmap, 180);
//                                    Log.e(TAG, "previewFrameHandler rotate bitmap time = " + (System.currentTimeMillis() - t2) + " ms");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
//                                            Log.i(TAG, "previewFrameHandler set bitmap, preview width=" + previewWidth
//                                                    + ", preview height=" + previewHeight);
                                            previewFrameView.setImageBitmap(rotateBitmap);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "previewFrameHandler error = " + e.getMessage(), e);
                        }
                    }
                };
    }

    //隐藏虚拟按键，并且全屏
    protected void hideBottomUIMenu() {
        // lower api
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) { //for new api versions.
            View decorView = this.getWindow().getDecorView();
            int uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}