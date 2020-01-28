package com.verifie.android.util;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static Bitmap cropArea(Bitmap source, int cropWidth, int cropHeight) {
        Log.d(TAG, "cropArea: " + source.getWidth());
        Log.d(TAG, "cropArea: " + source.getHeight());
        Log.d(TAG, "cropArea: " + cropWidth);
        Log.d(TAG, "cropArea: " + cropHeight);

        if (source.getWidth() < cropWidth || source.getHeight() < cropHeight) {
            return null;
        }

        return Bitmap.createBitmap(source, (source.getWidth() - cropWidth) / 2, (source.getHeight() - cropHeight) / 2, cropWidth, cropHeight);
    }

    public static String getImageBase64(Bitmap source) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        source.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();

//        source.recycle();

        return Base64.encodeToString(b, Base64.DEFAULT);
    }
}
