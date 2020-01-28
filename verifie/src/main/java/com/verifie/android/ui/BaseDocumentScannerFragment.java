package com.verifie.android.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.verifie.android.OperationsManager;
import com.verifie.android.R;
import com.verifie.android.VerifieConfig;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.ui.widget.CameraPreview;
import com.verifie.android.util.ImageUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseDocumentScannerFragment extends Fragment implements Camera.PreviewCallback, CameraPreview.PreviewReadyCallback {

    private static final String TAG = BaseDocumentScannerFragment.class.getSimpleName();

    public static final String ARG_CONFIG = "config";

    private OperationsManager operationsManager;

    private boolean captureDeliveredFrame;

    private CameraPreview preview;
    private RelativeLayout previewHolder;

    private Rect cropFrame;

    private Handler handler = new Handler();
    protected VerifieConfig config;
    private ImageView capturedDocImage;


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
    public void onResume() {
        super.onResume();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                preview = new CameraPreview(getActivity(), 0, CameraPreview.LayoutMode.NoBlank);
                preview.setOnPreviewReady(BaseDocumentScannerFragment.this);

                RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                previewHolder.addView(preview, 0, previewLayoutParams);
            }
        }, 500);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (preview != null) {
            preview.stop();
            previewHolder.removeView(preview);
            preview = null;
        }
    }

    public final void scanDocument() {
        captureDeliveredFrame = true;
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

    @Override
    public final void onPreviewReady() {
        preview.setPreviewCallback(this);
    }

    @Override
    public final void onPreviewFrame(byte[] data, Camera camera) {
        if (captureDeliveredFrame) {
            captureDeliveredFrame = false;

            YuvImage img = new YuvImage(data, ImageFormat.NV21, preview.getPreviewSize().width, preview.getPreviewSize().height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            img.compressToJpeg(new android.graphics.Rect(0, 0, img.getWidth(), img.getHeight()), 50, out);
            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            Bitmap rotatedImage;

            try {
                ExifInterface exif = new ExifInterface(new ByteArrayInputStream(imageBytes));
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                Matrix matrix = new Matrix();
                matrix.postRotate(90);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.setScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(0);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.setRotate(180);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(-90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(-90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(90);
                        break;
                }

                rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } catch (IOException e) {
                rotatedImage = bitmap;
            }
            processImage(rotatedImage);
        }
    }

    protected void showCapturedDocument(Bitmap rotatedImage) {
        capturedDocImage.setVisibility(View.VISIBLE);
        capturedDocImage.setImageBitmap(rotatedImage);
    }

    protected void hideCapturedImage() {
        capturedDocImage.setVisibility(View.GONE);
    }

    @SuppressLint("CheckResult")
    private void processImage(Bitmap image) {
        float cropAreaWidthScale = 1;
        float cropAreaHeightScale = 1;

        if (image.getWidth() != preview.getWidth() || image.getHeight() != preview.getHeight()) {
            cropAreaWidthScale = ((float) image.getWidth()) / ((float) preview.getWidth());
            cropAreaHeightScale = ((float) image.getHeight()) / ((float) preview.getHeight());
        }

        int cropAreaWidth = (int) (cropFrame.right * cropAreaWidthScale);
        int cropAreaHeight = (int) (cropFrame.bottom * cropAreaHeightScale);

        Bitmap result = ImageUtils.cropArea(image, cropAreaWidth, cropAreaHeight);

        showCapturedDocument(result);
        if (result == null) {
            return;
        }

        onDocumentScanStarted();

        String base64Image = ImageUtils.getImageBase64(result);

        operationsManager.uploadDocument(base64Image)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResponseModel<Document>>() {

                    @Override
                    public void accept(ResponseModel<Document> documentScanResponseModelResponseModel) throws Exception {
                        handleDocumentScanResult(documentScanResponseModelResponseModel);
                    }
                }, new Consumer<Throwable>() {

                    @Override
                    public void accept(Throwable throwable) {
                        Log.e(TAG, "accept: ", throwable);
                        onDocumentScanError("Please take a document photo again");
                    }
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
        if (!document.isDocumentValid()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage("Please take a document photo again.")
                    .setPositiveButton("OK", null)
                    .show();
        } else if (document.isNextPage()) {
            onDocumentScanFinished(true);
            operationsManager.onDocumentReceived(document);
            hideCapturedImage();
        } else if (document.isDocumentValid() && document.getDocumentType() == null) {
            onDocumentScanFinished(true);
        } else {
            onDocumentScanFinished(false);
            operationsManager.onDocumentReceived(document);
            if (preview != null) {
                preview.stop();
                previewHolder.removeView(preview);
                preview = null;
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    preview = new CameraPreview(getActivity(), 1, CameraPreview.LayoutMode.NoBlank);
                    preview.setOnPreviewReady(BaseDocumentScannerFragment.this);

                    RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    previewHolder.addView(preview, 0, previewLayoutParams);
                }
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

    public abstract Rect getCropFrame(Rect preview);

    public abstract void onCropFrameSet(Rect preview, Rect cropFrame);

    public abstract void onDocumentScanStarted();

    public abstract void onDocumentScanError(String errorMessage);

    public abstract void onDocumentScanFinished(boolean nextPageRequired);
}
