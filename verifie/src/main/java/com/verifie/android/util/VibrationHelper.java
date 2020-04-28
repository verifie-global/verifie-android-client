package com.verifie.android.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import com.verifie.android.R;

public class VibrationHelper {
    public static void vibrate(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(200, 255));
            } else {
                v.vibrate(200);
            }
        } else {
            Toast.makeText(context, context.getString(R.string.scanned), Toast.LENGTH_SHORT).show();
        }

    }
}
