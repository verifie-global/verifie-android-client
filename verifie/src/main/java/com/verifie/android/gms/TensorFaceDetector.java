package com.verifie.android.gms;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TensorFaceDetector {

    public static final String REAL = "real";
    public static final String FAKE = "fake";

    private static final String MODEL_PATH = "converted_model_96.tflite";
    private static final String LABEL_PATH = "labels.txt";
    private static final int RESULTS_TO_SHOW = 2;
    private static final int DIM_BATCH_SIZE = 1;
    private static final int BUFFER_SIZE = 110592;
    private final String TAG = this.getClass().getSimpleName();
    private Interpreter tflite;
    private List<String> labelList;
    private ByteBuffer inputBuffer = null;
    private float[][] mnistOutput = null;
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));


    public TensorFaceDetector(Activity activity) {
        try {
            MappedByteBuffer mappedByteBuffer = loadModelFile(activity);
            Interpreter.Options options = new Interpreter.Options();
//            options.setUseNNAPI(true);
            options.setNumThreads(4);
            options.setAllowBufferHandleOutput(true);
            options.setAllowFp16PrecisionForFp32(true);
            tflite = new Interpreter(mappedByteBuffer, options);
            labelList = loadLabelList(activity);
            inputBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            inputBuffer.order(ByteOrder.nativeOrder());
            mnistOutput = new float[DIM_BATCH_SIZE][labelList.size()];
            Log.d(TAG, "Created a Tensorflow Lite MNIST Classifier.");
        } catch (IOException e) {
            Log.e(TAG, "IOException loading the tflite file");
            System.err.println(e.getLocalizedMessage());
        }
    }
    protected void runInference() {
        if (tflite != null) {
            tflite.run(inputBuffer, mnistOutput);
        }
    }

    public String classify(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
        }
        preprocess(bitmap);
        runInference();
        return printTopKLabels();
    }

    private String printTopKLabels() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), mnistOutput[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }

        StringBuilder textToShow = new StringBuilder();
        final int size = sortedLabels.size();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            textToShow.append(label.getKey()).append(" : ").append(label.getValue()).append(", ");
        }
        return textToShow.toString();
    }

    public Map<String, Float> classifyImage(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
        }
        preprocess(bitmap);
        runInference();
        return getResults();
    }

    private Map<String, Float> getResults() {
        for (int i = 0; i < labelList.size(); ++i) {
            sortedLabels.add(
                    new AbstractMap.SimpleEntry<>(labelList.get(i), mnistOutput[0][i]));
            if (sortedLabels.size() > RESULTS_TO_SHOW) {
                sortedLabels.poll();
            }
        }
        final int size = sortedLabels.size();
        ArrayList<Float> result = new ArrayList<>();
        Map<String, Float> res = new HashMap<>();
        for (int i = 0; i < size; ++i) {
            Map.Entry<String, Float> label = sortedLabels.poll();
            res.put(label.getKey(), label.getValue());
            if (label.getKey().equals(REAL)) {
                result.add(0, label.getValue());
            } else {
                result.add(label.getValue());
            }

        }

        return res;
    }

    /**
     * Reads label list from Assets.
     */
    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void preprocess(Bitmap bitmap) {
        if (bitmap == null || inputBuffer == null) {
            return;
        }

        // Reset the image data
        inputBuffer.rewind();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        long startTime = SystemClock.uptimeMillis();

        // The bitmap shape should be 36 x 36
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; ++i) {
            int pixel = pixels[i];
            float channelValRed = (Color.red(pixel) - 0.f) / 256.f;
            float channelValGreen = (Color.green(pixel) - 0.f) / 256.f;
            float channelValBlue = (Color.blue(pixel) - 0.f) / 256.f;

            inputBuffer.putFloat((channelValRed));
            inputBuffer.putFloat(channelValGreen);
            inputBuffer.putFloat(channelValBlue);
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Time cost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }
}