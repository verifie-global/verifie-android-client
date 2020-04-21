package com.verifie.android.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.verifie.android.DocType;
import com.verifie.android.R;
import com.verifie.android.VerifieColorConfig;
import com.verifie.android.VerifieConfig;
import com.verifie.android.VerifieTextConfig;
import com.verifie.android.tflite.cardDetector.Classifier;
import com.verifie.android.tflite.cardDetector.ImageUtils;
import com.verifie.android.tflite.cardDetector.TFLiteObjectDetectionAPIModel;
import com.verifie.android.ui.tensorFlowIdCard.tracking.MultiBoxTracker;
import com.verifie.android.ui.widget.FrameOverlay;
import com.verifie.android.util.TextRecognitionHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public final class DefaultDocumentScannerFragment extends BaseDocumentScannerFragment {


    private static final boolean MAINTAIN_ASPECT = false;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    private static final String TAG = "DefaultDocumentScanFr";
    private FrameLayout progressBarHolder;
    private View recommendationsLayout;


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
    private MultiBoxTracker tracker;
    private ImageView croppedImage;
    private volatile boolean stop = false;
    private boolean isSecondPage = false;
    private TextRecognitionHelper textRecognitionHelper;
    private Bitmap scannable = null;

    private TextView txtTitle;
    private TextView txtPageInfo;
    private TextView txtScanInfo;
    private FrameOverlay cropperFrameHolder;

    public DefaultDocumentScannerFragment() {
    }


    public boolean isLoading() {
        return progressBarHolder.getVisibility() == View.VISIBLE;
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
                getActivity().finish();
            }
        });
        progressBarHolder = view.findViewById(R.id.progress_bar_holder);
        recommendationsLayout = view.findViewById(R.id.layout_recommendations_container);
        croppedImage = view.findViewById(R.id.cropped_image);

        txtTitle = view.findViewById(R.id.title);
        txtPageInfo = view.findViewById(R.id.txt_page_info);
        txtScanInfo = view.findViewById(R.id.txt_scan_info);
        cropperFrameHolder = view.findViewById(R.id.cropper_frame_holder_id_card);

        initTextHelper();
    }

    private void initTextHelper() {
        Activity activity = getActivity();
        if (activity != null) {
            textRecognitionHelper = new TextRecognitionHelper(activity, mrzText -> {
                Log.e("Found MRZ", mrzText);
                findFaceOnImage(scannable);
            });
        }
    }

    private void compressBitmap(Bitmap bitmap, String name) {
        Activity activity = getActivity();
        if (activity != null) {
            try {
                FileOutputStream fos = new FileOutputStream(activity.getFilesDir().getAbsolutePath() + "/" + name);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        if (stop) {
            croppedImage.setVisibility(View.GONE);
            return;
        }
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        bitmapInRightOrientation = processImageOrientation();
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        runInBackground(
                () -> {
                    if (stop) {
                        return;
                    }
                    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

                    final List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();
                    for (final Classifier.Recognition result : results) {
                        final RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                            cropToFrameTransform.mapRect(location);
                            result.setLocation(location);
                            mappedRecognitions.add(result);
                            Bitmap bitmap = processImageByFrameDetectFace(bitmapInRightOrientation);
                            if (bitmap != null) {
                                if (isIdCardSecondPage()) {
                                    scannable = bitmap;
                                    ProcessOCR processOCR = new ProcessOCR();
                                    processOCR.setBitmap(scannable);
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(processOCR::execute);
                                    }
                                } else {
                                    findFaceOnImage(bitmap);
                                }
                            }
                            break;
                        }
                    }
                    if (stop) {
                        return;
                    }
                });
    }


    private boolean isIdCardFirstPage() {
        return isScanningIdCard() && !isSecondPage;
    }

    private boolean isIdCardSecondPage() {
        return isScanningIdCard() && isSecondPage;
    }

    private boolean isScanningIdCard() {
        return config.getDocType() == DocType.DOC_TYPE_ID_CARD;
    }

    private boolean isPassportScanning() {
        return config.getDocType() == DocType.DOC_TYPE_PASSPORT;
    }

    private void findFaceOnImage(Bitmap imageBitmap) {
        if (imageBitmap != null && !imageBitmap.isRecycled()) {
            faceDetector = new FaceDetector.Builder(getContext())
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build();
            if (!faceDetector.isOperational()) {
                Log.w(TAG, "Face detector dependencies are not yet available.");
            }

            Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
            SparseArray<Face> faces = faceDetector.detect(frame);
            if (faces.size() > 0) {
                stop = true;
                processImageOnRemoteServer(imageBitmap);
                if (isIdCardFirstPage()) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage("Please scan backside of card")
                            .setPositiveButton("OK", (dialog, which) -> {
                                stop = false;
                                isSecondPage = true;
                                setupIdBacksideTexts();
                            })
                            .show();
                }
            } else {
                if (isIdCardSecondPage()) {
                    processImageOnRemoteServer(imageBitmap);
                    stop = true;
                    showRecommendationsLayout();
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
            getActivity().finish();
        }

        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        tracker = new MultiBoxTracker(getContext());
        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    public void onCropFrameSet(Rect preview, RectF cropFrame) {
        VerifieTextConfig textConfig = config.getTextConfig();
        VerifieColorConfig colorConfig = config.getColorConfig();

        txtTitle.setText(textConfig.getAlignTap());
        txtTitle.setTextColor(colorConfig.getDocCropperFrameColor());
        txtPageInfo.setText(textConfig.getPageInfo());
        txtScanInfo.setText(textConfig.getScanInfo());
    }

    protected void setupIdBacksideTexts() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                VerifieTextConfig textConfig = config.getTextConfig();
                txtPageInfo.setText(textConfig.getIdBackside());
                txtScanInfo.setText(textConfig.getIdBacksideInfo());
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faceDetector != null) {
            faceDetector.release();
        }
    }

    @Override
    public void onDocumentScanStarted() {
    }


    @Override
    public RectF getCropFrame(Rect preview) {
        return cropperFrameHolder.getCropRecF();
    }


    @Override
    protected void hideCapturedImage() {
        super.hideCapturedImage();
    }

    @Override
    public void onDocumentScanFinished(boolean nextPageRequired) {
    }


    private class ProcessOCR extends AsyncTask {

        Bitmap bitmap = null;

        @Override
        protected Object doInBackground(Object[] objects) {
            if (bitmap != null) {

                textRecognitionHelper.setBitmap(bitmap);

                textRecognitionHelper.doOCR();

                textRecognitionHelper.stop();

            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    private void showRecommendationsLayout() {
        if (getView() != null) {
            setRecommendationItemData(getView().findViewById(R.id.recommendation_great), getString(R.string.great), R.drawable.ic_boy_great, R.drawable.ic_success);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_glasses), getString(R.string.no_glasses), R.drawable.ic_boy_glasses, R.drawable.ic_error);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_shadow), getString(R.string.no_shadow), R.drawable.ic_boy_shadow, R.drawable.ic_error);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_flash), getString(R.string.no_flash), R.drawable.ic_boy_flash, R.drawable.ic_error);
            ((TextView) getView().findViewById(R.id.title_recommendation)).setText(getString(R.string.recommendations));
            getView().findViewById(R.id.btn_continue).setOnClickListener(v -> openFaceDetectorActivity());
            getView().findViewById(R.id.btn_back_recommend).setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
            recommendationsLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setRecommendationItemData(View itemView, String titleRes, int iconRes, int statusIconRes) {
        ((TextView) itemView.findViewById(R.id.txt_recommend_text)).setText(titleRes);
        ((ImageView) itemView.findViewById(R.id.person_icon)).setImageResource(iconRes);
        ((ImageView) itemView.findViewById(R.id.icon_recommendation)).setImageResource(statusIconRes);
    }

    static DefaultDocumentScannerFragment newInstance(VerifieConfig config) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);

        DefaultDocumentScannerFragment fragment = new DefaultDocumentScannerFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
