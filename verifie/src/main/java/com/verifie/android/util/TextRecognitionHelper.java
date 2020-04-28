package com.verifie.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.verifie.android.BuildConfig;
import com.verifie.android.ui.mrz.parsing.MrzFormat;
import com.verifie.android.ui.mrz.parsing.MrzParser;
import com.verifie.android.ui.mrz.parsing.MrzRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by jsjem on 17.11.2016.
 */
public class TextRecognitionHelper {

    private static final String TAG = "TextRecognitionHelper";

    private static final String TESSERACT_TRAINED_DATA_FOLDER = "tessdata";
    private static String TESSERACT_PATH = null;

    private final Context applicationContext;
    private final TessBaseAPI tessBaseApi;

    Pattern passportLine1Pattern = Pattern.compile("[A-Z0-9<]{2}[A-Z<]{3}[A-Z0-9<]{39}");
    Pattern passportLine2Pattern = Pattern.compile("[A-Z0-9<]{9}[0-9]{1}[A-Z<]{3}[0-9]{6}[0-9]{1}[FM<]{1}[0-9]{6}[0-9]{1}[A-Z0-9<]{14}[0-9]{1}[0-9]{1}");

    private List<MrzFormat> mrzFormats = new ArrayList<>();

    private static List<MrzFormat> supportedFormats = new ArrayList<>();

    private OnMRZScanned listener;

    /**
     * Constructor.
     *
     * @param context Application context.
     */
    public TextRecognitionHelper(final Context context, final OnMRZScanned listener) {
        this.applicationContext = context.getApplicationContext();
        this.listener = listener;
        this.tessBaseApi = new TessBaseAPI();
        this.tessBaseApi.setDebug(BuildConfig.DEBUG);
        this.tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789><");
        this.tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST,"abcdefghijklmopqrstuvwxyz\'/,.-:");
        TESSERACT_PATH = context.getFilesDir().getAbsolutePath() + "/";
        prepareTesseract("ocrb");

        mrzFormats.add(MrzFormat.PASSPORT);
        mrzFormats.add(MrzFormat.MRTD_TD2);
        mrzFormats.add(MrzFormat.SLOVAK_ID_234);
        mrzFormats.add(MrzFormat.MRTD_TD1);

        supportedFormats.add(MrzFormat.PASSPORT);
        supportedFormats.add(MrzFormat.SLOVAK_ID_234);
        supportedFormats.add(MrzFormat.MRTD_TD2);
        supportedFormats.add(MrzFormat.FRENCH_ID);
        supportedFormats.add(MrzFormat.MRTD_TD1);
    }

    /**
     * Initialize tesseract engine.
     *
     * @param language Language code in ISO-639-3 format.
     */
    public void prepareTesseract(final String language) {
        try {
            prepareDirectory(TESSERACT_PATH + TESSERACT_TRAINED_DATA_FOLDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        copyTessDataFiles(TESSERACT_TRAINED_DATA_FOLDER);
        tessBaseApi.init(TESSERACT_PATH, language);
    }

    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG,
                        "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }

    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = applicationContext.getAssets().list(path);

            for (String fileName : fileList) {
                String pathToDataFile = TESSERACT_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {
                    InputStream in = applicationContext.getAssets().open(path + "/" + fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte[] buf = new byte[1024];
                    int length;
                    while ((length = in.read(buf)) > 0) {
                        out.write(buf, 0, length);
                    }
                    in.close();
                    out.close();
                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.getMessage());
        }
    }

    /**
     * Set image for recognition.
     *
     * @param bitmap Image data.
     */
    public void setBitmap(final Bitmap bitmap) {
        //tessBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT);
        tessBaseApi.setImage(colorToGrayscale(bitmap));
    }

    /**
     * Get recognized text for image.
     *
     * @return Recognized text string.
     */
    public void doOCR() {
        String text = tessBaseApi.getUTF8Text();
        listener.textDetected(text);
        ArrayList<Rect> zones = tessBaseApi.getWords().getBoxRects();
        Log.v(TAG, "OCRED TEXT: " + text);
        checkMRZ(text, zones);
    }

    public void checkMRZ(String txt, ArrayList<Rect> zones) {
        final String mrzText = preProcessText(txt);
        if (mrzText != null) {
            listener.possibleMrzFound(mrzText);
            System.out.println("Found possible MRZ: " + mrzText);
            System.out.println("Found possible MRZformatted: " + mrzText.replaceAll("[^a-zA-Z0-9]+", " "));
            try {
                MrzRecord mrzRecord = MrzParser.parse(mrzText);
                if (mrzRecord != null) {
                    System.out.println(" MRZ ======:::     type: " + mrzRecord.format + "     " + mrzText);
                    if (supportedFormats.contains(mrzRecord.format)) {
                        boolean additionalPassportCheckOK = true;
                        if (mrzRecord.format == MrzFormat.PASSPORT) {
                            if (!passportLine1Pattern.matcher(mrzText).find()
                                    || !passportLine2Pattern.matcher(mrzText).find())
                                additionalPassportCheckOK = false;
                        }

                        if (additionalPassportCheckOK) {
                            new Handler(Looper.getMainLooper()).post(() -> listener.onScanned(mrzText, zones, mrzRecord.format));
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.i("MRZ Parser", "Failed");
            }
        }
    }

    private String preProcessText(String txt) {
        String[] lines = txt.split("\n");
        if (lines == null || lines.length < 1)
            return null;
        for (MrzFormat mrzFormat : mrzFormats) {
            for (int i = lines.length - 1; i >= 0; i--) {
                String line2 = lines[i].replace(" ", "");
                if (line2.length() >= mrzFormat.columns) {
                    if (i == 0)
                        break;
                    String line1 = lines[i - 1].replace(" ", "");
                    if (line1.length() >= mrzFormat.columns)
                        if (mrzFormat.rows == 2)
                            return line1.substring(0, mrzFormat.columns) + "\n" +
                                    line2.substring(0, mrzFormat.columns);
                        else if (mrzFormat.rows == 3) {
                            if (lines.length < 2 || i < 1 || i - 2 < 0)
                                break;
                            String line0 = lines[i - 2].replace(" ", "");
                            if (line0.length() >= mrzFormat.columns)
                                return line0.substring(0, mrzFormat.columns) + "\n" +
                                        line1.substring(0, mrzFormat.columns) + "\n" +
                                        line2.substring(0, mrzFormat.columns);
                            else
                                break;
                        } else
                            break;
                }
            }
        }
        return null;
    }

    public Bitmap colorToGrayscale(Bitmap bm) {
        Bitmap grayScale = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);

        Paint p = new Paint();
        p.setColorFilter(new ColorMatrixColorFilter(cm));

        new Canvas(grayScale).drawBitmap(bm, 0, 0, p);

        return grayScale;
    }

    public Bitmap grayScaleToBin(Bitmap bm, int threshold) {
        Bitmap bin = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);

        ColorMatrix cm = new ColorMatrix(new float[] {
                85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                0f, 0f, 0f, 1f, 0f
        });

        Paint p = new Paint();
        p.setColorFilter(new ColorMatrixColorFilter(cm));

        new Canvas(bin).drawBitmap(bm, 0, 0, p);

        return bin;
    }

    public Bitmap otsuThreshold (Bitmap bm) {

        // Get Histogram
        int[] histogram = new int[256];
        for(int i = 0; i < histogram.length; i++) histogram[i] = 0;

        for(int i = 0; i < bm.getWidth(); i++) {
            for(int j = 0; j < bm.getHeight(); j++) {
                histogram[(bm.getPixel(i, j) & 0xFF0000) >> 16]++;
            }
        }

        // Get binary threshold using Otsu's method

        int total = bm.getHeight() * bm.getWidth();

        float sum = 0;
        for(int i = 0; i < 256; i++) sum += i * histogram[i];

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for(int i = 0 ; i < 256 ; i++) {
            wB += histogram[i];
            if(wB == 0) continue;
            wF = total - wB;

            if(wF == 0) break;

            sumB += (float) (i * histogram[i]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

            if(varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }

        return grayScaleToBin(bm, threshold);
    }

    /**
     * Clear tesseract data.
     */
    public void stop() {
        tessBaseApi.clear();
    }

    public interface OnMRZScanned {
        void onScanned(String mrzText, ArrayList<Rect> bounds, MrzFormat format);

        void textDetected(String text);

        void possibleMrzFound(String text);
    }
}
