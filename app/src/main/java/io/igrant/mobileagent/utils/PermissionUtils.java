package io.igrant.mobileagent.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;


public class PermissionUtils {

    public static boolean hasPermissions(Activity context, boolean askForPermission, String[] permissions) {
        int i = 0;
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    i++;
                }
            }
        }
        if (i == 0) {
            return true;
        } else {
            if (askForPermission)
                ActivityCompat.requestPermissions(context, permissions, 99);
            return false;

        }
    }

    public static boolean hasPermissions(Activity context, boolean askForPermission, int requestCode, String[] permissions) {
        int i = 0;
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    i++;
                }
            }
        }
        if (i == 0) {
            return true;
        } else {
            if (askForPermission)
                ActivityCompat.requestPermissions(context, permissions, requestCode);
            return false;

        }
    }

    public static boolean hasPermissions(Context context, String[] permissions) {
        int i = 0;
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    i++;
                }
            }
        }
        if (i == 0) {
            return true;
        } else {
            return false;

        }
    }
}
