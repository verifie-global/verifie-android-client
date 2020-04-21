package com.verifie.android.ui;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.google.android.gms.vision.Frame;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.verifie.android.OperationsManager;
import com.verifie.android.R;
import com.verifie.android.VerifieColorConfig;
import com.verifie.android.VerifieConfig;
import com.verifie.android.VerifieTextConfig;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.tflite.cardDetector.ImageUtils;
import com.verifie.android.ui.widget.FrameOverlay;
import com.verifie.android.util.TextRecognitionHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.verifie.android.ui.BaseDocumentScannerFragment.ARG_CONFIG;

/**
 * A simple {@link Fragment} subclass.
 */
public class PassportScanFragment extends Fragment {

    private static final String TAG = PassportScanFragment.class.getName();
    private CameraView camera;

    private FrameOverlay viewFinder;

    private TextRecognitionHelper textRecognitionHelper;

    private AtomicBoolean processing = new AtomicBoolean(false);

    private ProcessOCR processOCR;

    private Bitmap originalBitmap = null;
    private Bitmap scannable = null;

    private OperationsManager operationsManager;
    private VerifieConfig config;
    private View recommendationsLayout;
    private TextView txtTitle;
    private TextView txtPageInfo;
    private TextView txtScanInfo;


    public PassportScanFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = getArguments().getParcelable(ARG_CONFIG);
        operationsManager = OperationsManager.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_passport_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        camera = view.findViewById(R.id.camera);
        camera.setLifecycleOwner(this);
        recommendationsLayout = view.findViewById(R.id.layout_recommendation_passport_page);


        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
                viewFinder = new FrameOverlay(view.getContext());
                camera.addView(viewFinder);
                camera.addFrameProcessor(frameProcessor);
            }
        });
        initTextHelper();
        initPageTexts(view);
    }

    private void initPageTexts(View view) {
        txtTitle = view.findViewById(R.id.title);
        txtPageInfo = view.findViewById(R.id.txt_page_info);
        txtScanInfo = view.findViewById(R.id.txt_scan_info);
        VerifieTextConfig textConfig = config.getTextConfig();
        VerifieColorConfig colorConfig = config.getColorConfig();

        txtTitle.setText(textConfig.getAlignTap());
        txtTitle.setTextColor(colorConfig.getDocCropperFrameColor());
        txtPageInfo.setText(textConfig.getPageInfo());
        txtScanInfo.setText(textConfig.getScanInfo());
    }

    private FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull com.otaliastudios.cameraview.frame.Frame frame) {
            if (frame.getData() != null && !processing.get()) {
                processing.set(true);

                YuvImage yuvImage = new YuvImage(frame.getData(), ImageFormat.NV21, frame.getSize().getWidth(), frame.getSize().getHeight(), null);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 100, os);
                byte[] jpegByteArray = os.toByteArray();

                Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);

                if (bitmap != null) {
                    bitmap = rotateImage(bitmap, frame.getRotation());

                    bitmap = getViewFinderArea(bitmap);

                    originalBitmap = bitmap;

                    scannable = getScannableArea(bitmap);

                    processOCR = new ProcessOCR();
                    processOCR.setBitmap(scannable);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> processOCR.execute());
                    }
                }
            }
        }
    };

    private Bitmap getViewFinderArea(Bitmap bitmap) {
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

    private Bitmap getScannableArea(Bitmap bitmap) {
        int top = bitmap.getHeight() * 4 / 10;

        bitmap = Bitmap.createBitmap(bitmap, 0, top,
                bitmap.getWidth(), bitmap.getHeight() - top);

        return bitmap;
    }

    private Bitmap rotateImage(Bitmap bitmap, int rotate) {
        Log.v(TAG, "Rotation: " + rotate);

        if (rotate != 0) {

            // Getting width & height of the given image.
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            // Setting pre rotate
            Matrix mtx = new Matrix();
            mtx.preRotate(rotate);

            // Rotating Bitmap
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        }

        // Convert to ARGB_8888, required by tess
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        return bitmap;
    }

    /**
     * reduces the size of the image
     *
     * @param image
     * @param maxSize
     * @return
     */
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
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
            processing.set(false);
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    private void handleDocumentScanResult(ResponseModel<Document> documentScanResponseModelResponseModel) {
        if (documentScanResponseModelResponseModel.getOpCode() == 0) {
            handleDocument(documentScanResponseModelResponseModel.getResult());
        } else {
            operationsManager.onDocumentReceived(null);
        }
    }


    private void handleDocument(Document document) {
        if (document.getDocumentType() == null || !config.getDocType().getName().equalsIgnoreCase(document.getDocumentType()) || !document.isDocumentValid()) {
            operationsManager.onDocumentReceived(null);
        } else {
            operationsManager.onDocumentReceived(document);
        }
    }

    private Bitmap processImageOrientation(Frame frame) {

        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), frame.getMetadata().getFormat(), frame.getMetadata().getWidth(), frame.getMetadata().getHeight(), null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, frame.getMetadata().getWidth(), frame.getMetadata().getHeight()), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);

        Bitmap rotatedImage;

        try {
            ExifInterface exif = new ExifInterface(new ByteArrayInputStream(jpegArray));
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
        return rotatedImage;
    }

    @SuppressLint("CheckResult")
    void processImageOnRemoteServer(Bitmap imageBitmap) {

        if (imageBitmap == null) {
            return;
        }

        String base64Image = ImageUtils.getImageBase64(imageBitmap);

        operationsManager.uploadDocument(base64Image)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleDocumentScanResult,
                        throwable -> {
                            Log.e(TAG, "accept: ", throwable);
                            operationsManager.onDocumentReceived(null);
                        });
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

    private void openFaceDetectorActivity() {
        Intent intent = new Intent(getContext(), FaceDetectorActivity.class);
        intent.putExtra(DocumentScannerActivity.EXTRA_CONFIG, config);
        startActivity(intent);
        getActivity().finish();
    }

    private void setRecommendationItemData(View itemView, String titleRes, int iconRes, int statusIconRes) {
        ((TextView) itemView.findViewById(R.id.txt_recommend_text)).setText(titleRes);
        ((ImageView) itemView.findViewById(R.id.person_icon)).setImageResource(iconRes);
        ((ImageView) itemView.findViewById(R.id.icon_recommendation)).setImageResource(statusIconRes);
    }


    private void initTextHelper() {
        Activity activity = getActivity();
        if (activity != null) {
            textRecognitionHelper = new TextRecognitionHelper(activity, mrzText -> {
                compressBitmap(originalBitmap, "mrzimage.png");
                compressBitmap(scannable, "scannable.png");
                Log.e("Found MRZ", mrzText);
                processImageOnRemoteServer(scannable);
                activity.runOnUiThread(this::showRecommendationsLayout);
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
}
