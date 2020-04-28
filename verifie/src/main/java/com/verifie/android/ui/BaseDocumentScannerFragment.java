package com.verifie.android.ui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.verifie.android.DocType;
import com.verifie.android.OperationsManager;
import com.verifie.android.R;
import com.verifie.android.VerifieConfig;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.tflite.cardDetector.ImageUtils;
import com.verifie.android.ui.widget.CameraPreview;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseDocumentScannerFragment extends Fragment implements Camera.PreviewCallback, CameraPreview.PreviewReadyCallback {

    private static final String TAG = BaseDocumentScannerFragment.class.getSimpleName();

    public static final String ARG_CONFIG = "config";

    private OperationsManager operationsManager;

    private CameraPreview preview;
    private RelativeLayout previewHolder;

    private Handler handler;
    private HandlerThread handlerThread;
    protected VerifieConfig config;
    private ImageView capturedDocImage;


    private int[] rgbBytes = null;
    private Runnable imageConverter;
    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private Runnable postInferenceCallback;

    private boolean isProcessingFrame = false;
    private Handler mainHandler = new Handler();
    private Point screenSizes;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operationsManager = OperationsManager.getInstance();
        config = getArguments().getParcelable(ARG_CONFIG);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() != null) {
            screenSizes = new Point();
            getActivity().getWindowManager().getDefaultDisplay().getSize(screenSizes);
        }
    }

    @Override
    @CallSuper
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewHolder = view.findViewById(R.id.preview_holder);
        capturedDocImage = view.findViewById(R.id.img_captured_document);

        if (previewHolder == null) {
            throw new RuntimeException("preview not found with id R.id.preview_holder");
        }
        Log.d(TAG, "onViewCreated, " + this + " :class");
    }

    @Override
    public synchronized void onResume() {
        Log.d(TAG, "onResume, " + this + " :class");
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        mainHandler.postDelayed(() -> {
            preview = new CameraPreview(getActivity(), 0, CameraPreview.LayoutMode.NoBlank);
            preview.setOnPreviewReady(BaseDocumentScannerFragment.this);

            RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            previewHolder.addView(preview, 0, previewLayoutParams);
        }, 500);
    }

    @Override
    public synchronized void onPause() {
        Log.d("onPause ", this + " :class");

        preview.stop();

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            Log.e(TAG, "Exception!");
        }
        super.onPause();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public final void onPreviewReady() {
        preview.setPreviewCallback(this);
    }

    @Override
    public final void onPreviewFrame(byte[] bytes, Camera camera) {
        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Exception!");
            return;
        }

        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                () -> com.verifie.android.tflite.cardDetector.ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

        postInferenceCallback =
                () -> {
                    camera.addCallbackBuffer(bytes);
                    isProcessingFrame = false;
                };
        processImage();
    }

    protected byte[] getBytesArray() {
        return yuvBytes[0];
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(Size size, int i);

    protected void showCapturedDocument(Bitmap rotatedImage) {
        capturedDocImage.setVisibility(View.VISIBLE);
        capturedDocImage.setImageBitmap(rotatedImage);
    }

    protected void hideCapturedImage() {
        if (capturedDocImage != null) {
            capturedDocImage.setVisibility(View.GONE);
        }
    }

    protected Bitmap getViewFinderArea(Bitmap bitmap) {
        int sizeInPixel = getResources().getDimensionPixelSize(R.dimen.frame_margin);
        int center = bitmap.getHeight() / 2;

        int left = sizeInPixel;
        int right = bitmap.getWidth() - sizeInPixel;
        int width = right - left;
        int frameHeight = (int) (width / 1.42f); // Passport's size (ISO/IEC 7810 ID-3) is 125mm × 88mm

        int top = center - (frameHeight / 2);

        bitmap = Bitmap.createBitmap(bitmap, left, top,
                width, frameHeight);

        return bitmap;
    }

    protected abstract void onCropFrameSet(Rect preview);

    protected abstract RectF getCropFrame();

    @SuppressLint("CheckResult")
    void processImageOnRemoteServer(Bitmap imageBitmap) {

        if (imageBitmap == null) {
            return;
        }
        onDocumentScanStarted();

        String base64Image = ImageUtils.getImageBase64(imageBitmap);

        operationsManager.uploadDocument(base64Image)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleDocumentScanResult,
                        throwable -> {
                            Log.e(TAG, "accept: ", throwable);
                            onDocumentScanError("Please take a document photo again");
                        });
    }

    private void handleDocumentScanResult(ResponseModel<Document> documentScanResponseModelResponseModel) {
        if (documentScanResponseModelResponseModel.getOpCode() == 0) {
            handleDocument(documentScanResponseModelResponseModel.getResult());
        } else {
            onDocumentScanError(documentScanResponseModelResponseModel.getOpDesc());
        }
    }

    private void handleDocument(Document document) {
        if (document == null || document.getDocumentType() == null || !config.getDocType().getName().equalsIgnoreCase(document.getDocumentType()) || !document.isDocumentValid()) {
            operationsManager.onDocumentReceived(null);
        } else {
            operationsManager.onDocumentReceived(document);
        }
    }

    protected int getScreenOrientation() {
        switch (getActivity().getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    public abstract void onDocumentScanStarted();

    public void onDocumentScanError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Please take a document photo again.";
        }
        operationsManager.onDocumentReceived(null);
    }

    public abstract void onDocumentScanFinished(boolean nextPageRequired);

}
