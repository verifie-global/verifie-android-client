package com.verifie.android.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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
    private RectF cropFrame;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        operationsManager = OperationsManager.getInstance();
        config = getArguments().getParcelable(ARG_CONFIG);
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

        setupCropperFrame();
    }

    @Override
    public synchronized void onResume() {
        Log.d("onResume ", this + " :class");
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


    private void setupCropperFrame() {
        previewHolder.post(new Runnable() {

            @Override
            public void run() {
                int holderWidth = previewHolder.getWidth();
                int holderHeight = previewHolder.getHeight();

                Rect preview = new Rect(0, 0, holderWidth, holderHeight);
                cropFrame = getCropFrame(preview);

                if (cropFrame == null) {
                    throw new NullPointerException("getCropFrame should not return null");
                }

                onCropFrameSet(preview, cropFrame);
            }
        });
    }


    protected Bitmap processImageByFrameDetectFace(Bitmap image) {
        float cropAreaWidthScale = 1;
        float cropAreaHeightScale = 1;

        if (image.getWidth() != preview.getWidth() || image.getHeight() != preview.getHeight()) {
            cropAreaWidthScale = ((float) image.getWidth()) / ((float) preview.getWidth());
            cropAreaHeightScale = ((float) image.getHeight()) / ((float) preview.getHeight());
        }

        int cropAreaWidth = (int) (cropFrame.right * cropAreaWidthScale);
        int cropAreaHeight = (int) (cropFrame.bottom * cropAreaHeightScale);

        return ImageUtils.cropArea(image, cropAreaWidth, cropAreaHeight);
    }

    protected abstract void onCropFrameSet(Rect preview, RectF cropFrame);

    protected abstract RectF getCropFrame(Rect preview);

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
        if (document.getDocumentType() == null || !config.getDocType().getName().equalsIgnoreCase(document.getDocumentType())) {
            hideCapturedImage();
            new AlertDialog.Builder(getActivity())
                    .setMessage("Invalid document")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (!document.isDocumentValid()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("Please take a document photo again.")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (document.isNextPage()) {
//            onDocumentScanFinished(true);
            operationsManager.onDocumentReceived(document);
            hideCapturedImage();
        } else if (document.isDocumentValid() && document.getDocumentType() == null) {
//            onDocumentScanFinished(true);
        } else {
//            onDocumentScanFinished(false);
            operationsManager.onDocumentReceived(document);
            if (preview != null) {
                preview.stop();
                previewHolder.removeView(preview);
                preview = null;
            }
            handler.postDelayed(() -> {
                preview = new CameraPreview(getActivity(), 1, CameraPreview.LayoutMode.NoBlank);
                preview.setOnPreviewReady(BaseDocumentScannerFragment.this);

                RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                previewHolder.addView(preview, 0, previewLayoutParams);
            }, 500);
        }
    }

    protected void openFaceDetectorActivity() {
        if (preview != null) {
            preview.stop();
            previewHolder.removeView(preview);
            preview = null;
        }
        Intent intent = new Intent(getContext(), FaceDetectorActivity.class);
        intent.putExtra(DocumentScannerActivity.EXTRA_CONFIG, config);
        startActivity(intent);
        getActivity().finish();
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

    public void onDocumentScanError(String errorMessage){
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Please take a document photo again.";
        }
        operationsManager.onDocumentReceived(null);
    }

    public abstract void onDocumentScanFinished(boolean nextPageRequired);
}
