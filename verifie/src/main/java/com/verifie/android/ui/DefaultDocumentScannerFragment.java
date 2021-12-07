package com.verifie.android.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.verifie.android.R;
import com.verifie.android.VerifieColorConfig;
import com.verifie.android.VerifieConfig;
import com.verifie.android.VerifieTextConfig;
import com.verifie.android.tflite.cardDetector.Classifier;
import com.verifie.android.tflite.cardDetector.ImageUtils;
import com.verifie.android.tflite.cardDetector.Size;
import com.verifie.android.tflite.cardDetector.TFLiteObjectDetectionAPIModel;
import com.verifie.android.ui.widget.FrameOverlay;
import com.verifie.android.util.ConvolutionMatrix;
import com.verifie.android.util.VibrationHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class DefaultDocumentScannerFragment extends BaseDocumentScannerFragment {


    private static final boolean MAINTAIN_ASPECT = false;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    private static final String TAG = "DefaultDocumentScanFr";

    private Classifier detector;
    private int cropSize;
    private Bitmap croppedBitmap = null;
    private Bitmap rgbFrameBitmap = null;
    private Integer sensorOrientation;
    private boolean computingDetection = false;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private FaceDetector faceDetector;
    private Bitmap bitmapInRightOrientation;
    private Long res;
    private Bitmap bmp;

    //    private ImageView croppedImage;
    private volatile boolean stop = false;

    private FrameOverlay cropperFrameHolder;

    public DefaultDocumentScannerFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_document_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                ((DocumentScannerActivity) getActivity()).finish(true);
            }
        });

        TextView txtTitle = view.findViewById(R.id.title);
        TextView txtPageInfo = view.findViewById(R.id.txt_page_info);
        TextView txtScanInfo = view.findViewById(R.id.txt_scan_info);

        VerifieTextConfig textConfig = config.getTextConfig();
        VerifieColorConfig colorConfig = config.getColorConfig();

        txtTitle.setText(textConfig.getPageTitle());
        txtTitle.setTextColor(colorConfig.getDocCropperFrameColor());
        txtPageInfo.setText(textConfig.getPageInfo());
        txtScanInfo.setText(textConfig.getScanInfo());
        cropperFrameHolder = view.findViewById(R.id.cropper_frame_holder_id_card);
//        croppedImage = view.findViewById(R.id.cropped_image);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        ((DocumentScannerActivity) getActivity()).openIdCardBacksideScanner();
    }

    private Bitmap processImageOrientation() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        rgbFrameBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        byte[] imageBytes = out.toByteArray();

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

            rotatedImage = Bitmap.createBitmap(rgbFrameBitmap, 0, 0, rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            rotatedImage = rgbFrameBitmap;
        }
        return rotatedImage;
    }


    @Override
    protected void processImage() {
        if (computingDetection) {
            readyForNextImage();
            return;
        }


        if (!rgbFrameBitmap.isRecycled()) {
            rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
            bitmapInRightOrientation = processImageOrientation();
            final Canvas canvas = new Canvas(croppedBitmap);
            canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        }
        if (bmp != null && stop && !bitmapInRightOrientation.isRecycled()) {
            Bitmap bitmap = getViewFinderArea(bitmapInRightOrientation);
//            croppedImage.setVisibility(View.GONE);
            long res = new ConvolutionMatrix(3).variance(bitmap);
            findFaceOnImage(bitmap, res);
            return;
        }
        if (!stop) {
            runInBackground(
                    () -> {
                        if (stop) {
                            return;
                        }
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                Bitmap bitmap = getViewFinderArea(bitmapInRightOrientation);
                                if (bitmap != null) {
                                    long res = new ConvolutionMatrix(3).variance(bitmap);
                                    findFaceOnImage(bitmap, res);
                                }
                                break;
                            }
                        }
                    });
        }
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }


    private void findFaceOnImage(Bitmap imageBitmap, long res) {
        if (imageBitmap != null && !imageBitmap.isRecycled()) {
            if (faceDetector == null) {
                faceDetector = new FaceDetector.Builder(getContext())
                        .setTrackingEnabled(false)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setMode(FaceDetector.ACCURATE_MODE)
                        .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                        .build();
                if (!faceDetector.isOperational()) {
                    Log.w(TAG, "Face detector dependencies are not yet available.");
                }
            }

            Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
            SparseArray<Face> faces = faceDetector.detect(frame);
            if (faces.size() > 0 && faces.get(faces.keyAt(0)).getLandmarks().size() > 11) {
                if (bmp == null) {
                    stop = true;
                    this.res = res;
                    this.bmp = imageBitmap;
                } else {
                    if (this.res >= res) {
                        processImageOnRemoteServer(this.bmp);
                    } else {
                        processImageOnRemoteServer(imageBitmap);
                    }
                    recycleBitmap(bmp);
                    recycleBitmap(bitmapInRightOrientation);
                    recycleBitmap(rgbFrameBitmap);
                    recycleBitmap(croppedBitmap);
                    recycleBitmap(imageBitmap);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            VibrationHelper.vibrate(getActivity());
                            ((DocumentScannerActivity) getActivity()).openIdCardBacksideScanner();
                        });
                    }
                }
            }
        }
    }

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        cropSize = TF_OD_API_INPUT_SIZE;
        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getActivity().getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            ((DocumentScannerActivity) getActivity()).finish(true);
        }

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        previewWidth = size.width;
        previewHeight = size.height;

        sensorOrientation = rotation - getScreenOrientation();

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }

    @Override
    public void onCropFrameSet(Rect preview) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faceDetector != null) {
            faceDetector.release();
        }
        if (detector != null) {
            detector.close();
        }
    }

    @Override
    public void onDocumentScanStarted() {
    }


    @Override
    public RectF getCropFrame() {
        return cropperFrameHolder.getCropRecF();
    }


    @Override
    protected void hideCapturedImage() {
        super.hideCapturedImage();
    }

    @Override
    public void onDocumentScanFinished(boolean nextPageRequired) {
    }

    static DefaultDocumentScannerFragment newInstance(VerifieConfig config) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);

        DefaultDocumentScannerFragment fragment = new DefaultDocumentScannerFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
