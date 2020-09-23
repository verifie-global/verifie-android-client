package com.verifie.android.ui;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.preview.CameraPreview;
import com.otaliastudios.cameraview.size.Size;
import com.verifie.android.DocType;
import com.verifie.android.OperationsManager;
import com.verifie.android.R;
import com.verifie.android.VerifieColorConfig;
import com.verifie.android.VerifieConfig;
import com.verifie.android.VerifieTextConfig;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.tflite.cardDetector.ImageUtils;
import com.verifie.android.ui.mrz.parsing.MrzFormat;
import com.verifie.android.ui.widget.FrameOverlay;
import com.verifie.android.util.TextRecognitionHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.verifie.android.ui.BaseDocumentScannerFragment.ARG_CONFIG;

/**
 * A simple {@link Fragment} subclass.
 */
public class MrzScanFragment extends Fragment implements IDCardView.ActionHandler {

    private static final String TAG = MrzScanFragment.class.getName();

    private CameraView camera;
    private TextRecognitionHelper textRecognitionHelper;

    private AtomicBoolean processing = new AtomicBoolean(false);

    private ProcessOCR processOCR;

    private Bitmap originalBitmap = null;
    private Bitmap scannable = null;

    private OperationsManager operationsManager;
    private VerifieConfig config;
    private Bitmap bitmap;
    //    private ImageView croppedImage;
    private boolean isSentRequest = false;
    //    private TextView debugTxt;
    private TextView txtTitle;
    private FrameOverlay viewFinder;
    private Size previewSize;
    private Point screenSizes;
    private IDCardView idCardView;
    private View viewToAdd;
    private FrameLayout container;
    private String documentImageStr = "";

    public MrzScanFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = getArguments().getParcelable(ARG_CONFIG);
        operationsManager = OperationsManager.getInstance();
        idCardView = operationsManager.getIdCardView();
        if (isScanningIDCard() && idCardView != null) {
            viewToAdd = idCardView.getViewToShow(this);
        }
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mrz_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        camera = view.findViewById(R.id.camera);
        camera.setLifecycleOwner(this);

        container = view.findViewById(R.id.info_layout_container);

//        croppedImage = view.findViewById(R.id.cropped_image);
        viewFinder = view.findViewById(R.id.cropper_frame_holder);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(@NonNull CameraOptions options) {
//                viewFinder = new FrameOverlay(view.getContext());
//                camera.addView(viewFinder);
                if (!isThereInfoLayout()) {
                    container.setVisibility(View.INVISIBLE);
                    camera.addFrameProcessor(frameProcessor);
                }
            }
        });
        initTextHelper();
        initPageTexts(view);
//        debugTxt = view.findViewById(R.id.txt_debug);
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                ((DocumentScannerActivity) getActivity()).finish(true);
            }
        });

        camera.setPreviewStreamSize(source -> {
            for (int i = 0; i < source.size(); i++) {
                if (source.get(i).getWidth() == screenSizes.x) {
                    int finalI = i;
                    return new ArrayList<Size>() {{
                        add(source.get(finalI));
                    }};
                }
            }
            return source;
        });

        if (isThereInfoLayout()) {
            container.setVisibility(View.VISIBLE);
            container.addView(viewToAdd);
        }

    }

    private boolean isThereInfoLayout() {
        return isScanningIDCard() && idCardView != null && viewToAdd != null;
    }

    private boolean isScanningIDCard() {
        return config.getDocType() == DocType.DOC_TYPE_ID_CARD;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isScanningIDCard()) {
            viewFinder.setCoeficent(1.42f);
        }
    }

    private void initPageTexts(View view) {
        txtTitle = view.findViewById(R.id.title);
        TextView txtPageInfo = view.findViewById(R.id.txt_page_info);
        TextView txtScanInfo = view.findViewById(R.id.txt_scan_info);

        VerifieTextConfig textConfig = config.getTextConfig();
        VerifieColorConfig colorConfig = config.getColorConfig();

        txtTitle.setTextColor(colorConfig.getDocCropperFrameColor());
        txtTitle.setText(textConfig.getPageTitle());

        if (isScanningIDCard()) {
            txtPageInfo.setText(textConfig.getIdBackside());
            txtScanInfo.setText(textConfig.getIdBacksideInfo());
        } else {
            txtPageInfo.setText(textConfig.getPageInfo());
            txtScanInfo.setText(textConfig.getScanInfo());
        }
    }

    private FrameProcessor frameProcessor = new FrameProcessor() {
        @Override
        public void process(@NonNull Frame frame) {
            if (frame.getData() != null && !processing.get()) {
                processing.set(true);
//                bitmap = processImageOrientation(frame);
                YuvImage yuvImage = new YuvImage(frame.getData(), ImageFormat.NV21, frame.getSize().getWidth(), frame.getSize().getHeight(), null);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 100, os);
                byte[] jpegByteArray = os.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
                if (bitmap != null) {
                    bitmap = rotateImage(bitmap, frame.getRotation());
                    Bitmap bitmap = processImageByFrameDetectFace(MrzScanFragment.this.bitmap);
                    originalBitmap = bitmap;
                    scannable = getScannableArea(bitmap);
                    processOCR = new ProcessOCR();
                    processOCR.setBitmap(scannable);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> processOCR.execute());
//                        croppedImage.setImageBitmap(originalBitmap);
                    }
                }
            }
        }
    };


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

    protected Bitmap processImageByFrameDetectFace(Bitmap image) {
        float cropAreaWidthScale = 1;

        if (previewSize == null) {
            tryToGetPreviewSize();
        }

        int sizeInPixel = getResources().getDimensionPixelSize(R.dimen._12sdp);
        if (screenSizes.x != previewSize.getWidth()) {
            cropAreaWidthScale = ((float) screenSizes.x) / ((float) previewSize.getWidth());
        }

        float coeffiecent = 1.54f;
        if (!isScanningIDCard()) {
            coeffiecent = 1.42f;
        }

        int cropAreaWidth = (int) ((viewFinder.getCropRecF().right - viewFinder.getCropRecF().left) * cropAreaWidthScale);
        int cropAreaHeight = (int) (cropAreaWidth / coeffiecent);// Passport's size (ISO/IEC 7810 ID-3) is 125mm × 88mm
        cropAreaHeight -= sizeInPixel;
        return ImageUtils.cropArea(image, cropAreaWidth, cropAreaHeight);
    }

    private void tryToGetPreviewSize() {
        try {
            Field cameraPreview = camera.getClass().getDeclaredField("mCameraPreview");
            cameraPreview.setAccessible(true);
            CameraPreview preview = (CameraPreview) cameraPreview.get(camera);
            if (preview != null) {
                previewSize = preview.getSurfaceSize();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private Bitmap getScannableArea(Bitmap bitmap) {
        int top = bitmap.getHeight() * 4 / 10;

        bitmap = Bitmap.createBitmap(bitmap, 0, top,
                bitmap.getWidth(), bitmap.getHeight() - top);

        return bitmap;
    }

    @Override
    public void closeIDCardLayout() {
        container.setVisibility(View.GONE);
        container.removeAllViews();
        camera.addFrameProcessor(frameProcessor);
    }

    @SuppressLint("StaticFieldLeak")
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
            Document document = new Document();
            document.setError(documentScanResponseModelResponseModel.getOpDesc());
            operationsManager.onDocumentReceived(document);
        }
    }


    private void handleDocument(Document document) {
        if (document == null || document.getDocumentType() == null || !config.getDocType().getName().equalsIgnoreCase(document.getDocumentType()) || !document.isDocumentValid()) {
            if (document == null) {
                document = new Document();
            }
            document.setError("Invalid Document Type");
        }
        document.setDocumentImage(this.documentImageStr);
        operationsManager.onDocumentReceived(document);
    }

    private Bitmap processImageOrientation(Frame frame) {

        YuvImage yuvImage = new YuvImage(frame.getData(), ImageFormat.NV21, frame.getSize().getWidth(), frame.getSize().getHeight(), null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 50, byteArrayOutputStream);
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
    void processImageOnRemoteServer(Bitmap imageBitmap, int bottom) {
//        imageBitmap = processImageByFrameDetectFace(imageBitmap, bottom);
        if (imageBitmap == null) {
            return;
        }
//        croppedImage.setImageBitmap(imageBitmap);
        String base64Image = ImageUtils.getImageBase64(imageBitmap);
        this.documentImageStr = base64Image;

        isSentRequest = true;
        camera.removeFrameProcessor(frameProcessor);
        operationsManager.uploadDocument(base64Image)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleDocumentScanResult,
                        throwable -> {
                            Log.e(TAG, "accept: ", throwable);
                            Document document = new Document();
                            document.setError(throwable.getLocalizedMessage());
                            operationsManager.onDocumentReceived(document);
                        });
    }

    private void openFaceDetectorActivity() {
        Intent intent = new Intent(getContext(), FaceDetectorActivity.class);
        intent.putExtra(DocumentScannerActivity.EXTRA_CONFIG, config);
        startActivity(intent);
        if (getActivity() != null) {
            ((DocumentScannerActivity) getActivity()).finish(false);
        }
    }

    private boolean isConfigIdCardAndScannedTD(MrzFormat format) {
        return isScanningIDCard() && format == MrzFormat.PASSPORT;
    }

    private boolean isScannedPassportAndConfigIsPassport(MrzFormat format) {
        return config.getDocType() == DocType.DOC_TYPE_PASSPORT && format != MrzFormat.PASSPORT;
    }

    private void initTextHelper() {
        Activity activity = getActivity();
        if (activity != null) {
            textRecognitionHelper = new TextRecognitionHelper(activity, new TextRecognitionHelper.OnMRZScanned() {
                @Override
                public void onScanned(String mrzText, ArrayList<Rect> zones, MrzFormat format) {
//                    activity.runOnUiThread(() -> {
//                        debugTxt.setText(mrzText);
//                    });
                    if (isConfigIdCardAndScannedTD(format) || isScannedPassportAndConfigIsPassport(format)) {
//                        Toast.makeText(activity, "DocType: " + config.getDocType().getName() + ", Scanned: " + format, Toast.LENGTH_LONG).show();
//                        Toast.makeText(activity, "Wrong document type. Please scan " + config.getDocType().getName(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        camera.stopVideo();
                        camera.close();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    if (!isSentRequest) {
                        compressBitmap(originalBitmap, "mrzimage.png");
                        compressBitmap(scannable, "scannable.png");
                        Log.e("Found MRZ", mrzText);
                        int bottom = -1;
                        if (!zones.isEmpty()) {
                            if (zones.get(0) != null) {
                                bottom = zones.get(0).bottom;
                                for (int i = 1; i < zones.size(); i++) {
                                    Rect rect = zones.get(i);
                                    if (rect != null) {
                                        bottom = Math.max(bottom, rect.bottom);
                                    }
                                }
                            }
                        }
                        processImageOnRemoteServer(originalBitmap, bottom);
                        new Handler().postDelayed(MrzScanFragment.this::openFaceDetectorActivity, 0);
                    }
                }

                @Override
                public void textDetected(String text) {
//                    activity.runOnUiThread(() -> {
//                        txtTitle.setText(text);
//                    });
                }

                @Override
                public void possibleMrzFound(String text) {
//                    activity.runOnUiThread(() -> {
//                        debugTxt.setText(text);
//                    });
                }
            });
        }

    }

    private void compressBitmap(Bitmap bitmap, String name) {
        Activity activity = getActivity();
        if (activity != null) {
            try {
                FileOutputStream fos = new FileOutputStream(activity.getFilesDir().getAbsolutePath() + "/" + name);
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
