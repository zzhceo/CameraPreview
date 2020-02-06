package com.zzh.camerapreview.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * add by zzh 20180726
 * */
public class PermissionCheckUtil {
    private static final String TAG = "PermissionCheckUtil";
    // 此处列出所有需要的权限
    public static final String[] ALL_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    // 此处列出必要的权限
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    static final String PREVIOUS_ACTIVITY_INTENT = "previous_intent";
    public static final String MISSING_PERMISSIONS = "missing_permissions";
    public static int sPermissionsActivityStarted = 0;
    // 请求所有权限时调用
    public static boolean requestAllPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return requestPermissions(activity, ALL_PERMISSIONS);
        else
            return true;
    }
    // 请求必要权限时调用
    public static boolean requestRequiredPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return requestPermissions(activity, REQUIRED_PERMISSIONS);
        else
            return true;
    }
    private static boolean requestPermissions(Activity activity, String[] permissions) {
        ArrayList<String> missingList = getMissingPermissions(activity, permissions);
        return requestPermissions(activity, missingList);
    }
    public static ArrayList<String> getMissingPermissions(
            Activity activity, String[] requiredPermissions) {
        final ArrayList<String> missingList = new ArrayList<String>();
        for (int i = 0; i < requiredPermissions.length; i++) {
            if (!hasPermission(activity, requiredPermissions[i])) {
                missingList.add(requiredPermissions[i]);
            }
        }
        return missingList;
    }
    public static boolean hasNeverGrantedPermissions(Activity activity, ArrayList<String> permissionList) {
        boolean isNeverGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissionList.size(); i++) {
                if (isNeverGrantedPermission(activity, permissionList.get(i))) {
                    isNeverGranted = true;
                    Log.d(TAG, " hasNeverGrantedPermissions "
                            + permissionList.get(i) + " is always denied");
                    break;
                }
            }
        }
        return isNeverGranted;
    }
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isNeverGrantedPermission(Activity activity, String permission) {
        return !activity.shouldShowRequestPermissionRationale(permission);
    }
    private static boolean requestPermissions(Activity activity, ArrayList<String> missingList) {
        if (missingList.size() == 0) {
            Log.d(TAG, " requestPermissions all permissions granted");
            return false;
        }
        final String[] missingArray = new String[missingList.size()];
        missingList.toArray(missingArray);
        Intent intentPermissions = new Intent(activity, PermissionCheckActivity.class);
        intentPermissions.putExtra(PREVIOUS_ACTIVITY_INTENT, activity.getIntent());
        intentPermissions.putExtra(MISSING_PERMISSIONS, missingArray);
        activity.startActivity(intentPermissions);
        activity.finish();
        return true;
    }
    static boolean checkAllPermissions(Context context) {
        return checkPermissions(context, ALL_PERMISSIONS);
    }
    public static boolean checkRequiredPermissions(Context context) {
        return checkPermissions(context, REQUIRED_PERMISSIONS);
    }
    private static boolean checkPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                Log.d(TAG, "checkPermissions false : " + permission);
                return false;
            }
        }
        return true;
    }
    public static boolean onRequestPermissionsResult(
            Activity activity, int requestCode, String[] permissions,
            int[] grantResults, boolean needFinish) {
        for (int i = 0; i < permissions.length; i++) {
            if (!hasPermission(activity, permissions[i])) {
                boolean isNeverGrantedPermission = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    isNeverGrantedPermission = isNeverGrantedPermission(activity, permissions[i]);
                }
                // Show toast
                if (isRequiredPermission(permissions[i]) || isNeverGrantedPermission) {
                    showNoPermissionsToast(activity);
                    if (needFinish) {
                        activity.finish();
                    }
                }
                Log.d(TAG, "onRequestPermissionsResult return false");
                return false;
            }
        }
        String[] requiredPermissions;
        requiredPermissions = REQUIRED_PERMISSIONS;
        for (int i = 0; i < requiredPermissions.length; i++) {
            if (!hasPermission(activity, requiredPermissions[i])) {
                boolean isNeverGrantedPermission = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    isNeverGrantedPermission = isNeverGrantedPermission(activity, permissions[i]);
                }
                // Show toast
                if (isRequiredPermission(requiredPermissions[i]) || isNeverGrantedPermission) {
                    if (!isPermissionChecking()) {
                        showNoPermissionsToast(activity);
                    }
                    if (needFinish) {
                        activity.finish();
                    }
                }
                Log.d(TAG, "onRequestPermissionsResult return false");
                return false;
            }
        }
        Log.d(TAG, "onRequestPermissionsResult return true");
        return true;
    }
    public static boolean hasPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return hasPermissionForM(context, permission);
        else
            return PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
    }
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasPermissionForM(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    public static void showNoPermissionsToast(Context context) {
        Toast.makeText(context, "has some permission not granted", Toast.LENGTH_LONG).show();
    }
    public static boolean isPermissionChecking() {
        Log.d(TAG, " isPermissionChecking Activity Count: " + sPermissionsActivityStarted);
        return sPermissionsActivityStarted > 0;
    }
    /*
     * It means permission activity would be finished if startActivity is false.
     */
    public static void setPermissionActivityCount(boolean startActivity) {
        if (startActivity) {
            if (sPermissionsActivityStarted < 0) {
                sPermissionsActivityStarted = 0;
            }
            sPermissionsActivityStarted++;
        } else {
            sPermissionsActivityStarted--;
            if (sPermissionsActivityStarted < 0) {
                sPermissionsActivityStarted = 0;
            }
        }
        Log.d(TAG, "setPermissionActivityCount: "
                + sPermissionsActivityStarted + ", start: " + startActivity);
    }
    public static boolean isRequiredPermission(String permission) {
        String[] requiredPermissions;
        requiredPermissions = REQUIRED_PERMISSIONS;
        for (int i = 0; i < requiredPermissions.length; i++) {
            if (requiredPermissions[i].equals(permission)) {
                Log.d(TAG, "isRequiredPermission: " + permission);
                return true;
            }
        }
        return false;
    }
}
