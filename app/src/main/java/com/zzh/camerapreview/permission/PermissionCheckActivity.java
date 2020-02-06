package com.zzh.camerapreview.permission;
// import ...
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * add by zzh 20180726
 * */
public class PermissionCheckActivity extends Activity {
    private static final String TAG = "PermissionCheckActivity";
    private static final int REQUIRED_PERMISSIONS_REQUEST_CODE = 1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, " onCreate" + savedInstanceState);
        if (savedInstanceState == null) {
            final String[] missingArray
                    = getIntent().getStringArrayExtra(PermissionCheckUtil.MISSING_PERMISSIONS);
            PermissionCheckUtil.setPermissionActivityCount(true);
            if (missingArray!= null && missingArray.length > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissionForM(missingArray);
            else {
                finish();
                returnOriginationActivity();
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissionForM(String[] missingArray) {
        requestPermissions(missingArray, REQUIRED_PERMISSIONS_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        finish();
        PermissionCheckUtil.setPermissionActivityCount(false);
        Log.d(TAG, " onRequestPermissionsResult Activity Count: "
                + PermissionCheckUtil.sPermissionsActivityStarted);
        if (PermissionCheckUtil.onRequestPermissionsResult(
                this, requestCode, permissions, grantResults, true)) {
            returnOriginationActivity();
        }
    }
    private void returnOriginationActivity() {
        try {
            Intent previousActivityIntent
                    = (Intent) getIntent().getExtras().get(
                    PermissionCheckUtil.PREVIOUS_ACTIVITY_INTENT);
            startActivity(previousActivityIntent);
        } catch (SecurityException e) {
            Log.e(TAG, " SecurityException happened: " + e);
            Toast.makeText(this, "SecurityException happened", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void finish() {
        super.finish();
        //关闭窗体动画显示
        this.overridePendingTransition(0,0);
    }
}