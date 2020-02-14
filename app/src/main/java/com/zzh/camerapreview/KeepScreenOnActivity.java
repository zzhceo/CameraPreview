package com.zzh.camerapreview;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class KeepScreenOnActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
