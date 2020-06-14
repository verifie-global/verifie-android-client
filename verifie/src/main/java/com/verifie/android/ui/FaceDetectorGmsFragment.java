package com.verifie.android.ui;


import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.verifie.android.OperationsManager;
import com.verifie.android.R;
import com.verifie.android.api.model.res.Score;
import com.verifie.android.gms.CameraSourcePreview;
import com.verifie.android.gms.TensorFaceDetector;
import com.verifie.android.tflite.cardDetector.ImageUtils;
import com.verifie.android.ui.widget.OvalOverlayView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 */
public class FaceDetectorGmsFragment extends Fragment {
    //    public static final float FaceSolidityMin = 0.135F;
//    public static final float FaceSolidityMax = 0.275F;
    public static final float INVALID = -1f;
    public static final float FaceSolidityMinPercentage = 0.01F;
    public static final float FaceSolidityMaxPercentage = 0.0277F;
    public static float FaceSolidityMin = INVALID;
    public static float FaceSolidityMax = INVALID;

    public static final int IMAGE_SIZE = 96;
    public static final int SECONDS_TO_HOLD = 5;
    public static final int REALS_MIN_PERCENTAGE = 40;
    private static final String TAG = "FaceTracker";
    private static final int RC_HANDLE_GMS = 9001;

    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final Float FAKE_MIN_RES = 0.9f;
    private TensorFaceDetector tensorFaceDetector;
    private int realCount = 0;
    private int fakeCount = 0;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private TextView tvInfo;

    private boolean faceDetected = false;
    private volatile int seconds = 0;
    private volatile long startTime = -1;
    private OvalOverlayView oval_overlay_animation;
    private ImageView imgPreview;
    private boolean isRequestSent = false;
    private boolean isAnimationStarted = false;
    private Model model;
    private View recommendationsLayout;
    private boolean stopped = true;

    private boolean areRealsGreather() {
        int total = realCount + fakeCount;
        float realsPercentage = ((float) realCount / (float) total) * 100;
        return realsPercentage > REALS_MIN_PERCENTAGE;
    }

    public FaceDetectorGmsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_face_detector_gms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI(view);
        oval_overlay_animation.setAnimatorListener(new Animator.AnimatorListener() {
            private boolean canceled = false;

            @Override
            public void onAnimationStart(Animator animation) {
                canceled = false;
            }

            @SuppressLint("CheckResult")
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!canceled && areRealsGreather()) {
                    if (!isLoading()) {
                        oval_overlay_animation.startAnimationAsLoading();
                        isRequestSent = true;
                        imgPreview.setVisibility(View.VISIBLE);
                        tvInfo.setVisibility(View.GONE);
                        imgPreview.setImageBitmap(model.bitmapOriginal);
                        model.bitmapOvalShape = ImageUtils.cropArea(model.bitmapOriginal, model.ovalRect);
                        compressBitmap(model.bitmapOvalShape, "selfie.png");
                        final String imageBase64 = ImageUtils.getImageBase64(model.bitmapOvalShape);
                        OperationsManager.getInstance().uploadFace(imageBase64)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(faceScannedModelResponse -> {
                                    faceScannedModelResponse.getResult().setBase64Image(imageBase64);
                                    OperationsManager.getInstance().onScoreReceived(faceScannedModelResponse.getResult());
                                    oval_overlay_animation.drawDone();
                                    new Handler().postDelayed(() -> {
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    }, 1000);
                                }, throwable -> {
                                    OperationsManager.getInstance().onScoreReceived(new Score());
                                    oval_overlay_animation.drawDone();
                                    new Handler().postDelayed(() -> {
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    }, 1000);
                                });
                    }
                } else {
                    isAnimationStarted = false;
                    oval_overlay_animation.stopAnim();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
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

    private void initUI(View view) {
        mPreview = view.findViewById(R.id.preview);
        tvInfo = view.findViewById(R.id.tv_info);
        oval_overlay_animation = view.findViewById(R.id.oval_overlay_animation);
        imgPreview = view.findViewById(R.id.img_preview);
        recommendationsLayout = view.findViewById(R.id.layout_recommendation_passport_page);
        showRecommendationsLayout();
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        if (getContext() != null) {
            int rc = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
            } else {
                requestCameraPermission();
            }
        }
    }

    private void setText(String text) {
        new Handler().post(() -> {
            tvInfo.setText(text);
        });
    }


    private void createCameraSource() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .build();

        MyFaceDetector myFaceDetector = new MyFaceDetector(detector);
        myFaceDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());


        if (!myFaceDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
            setText("Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, myFaceDetector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(100.0f)
//                .setRequestedPreviewSize(640, 480)
                .setRequestedPreviewSize(1920, 1080)
                .build();
        tensorFaceDetector = new TensorFaceDetector(getActivity());
    }

    private void showRecommendationsLayout() {
        if (getView() != null) {
            setRecommendationItemData(getView().findViewById(R.id.recommendation_great), getString(R.string.great), R.drawable.ic_boy_great, R.drawable.ic_success);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_glasses), getString(R.string.no_glasses), R.drawable.ic_boy_glasses, R.drawable.ic_error);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_shadow), getString(R.string.no_shadow), R.drawable.ic_boy_shadow, R.drawable.ic_error);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_flash), getString(R.string.no_flash), R.drawable.ic_boy_flash, R.drawable.ic_error);
            ((TextView) getView().findViewById(R.id.title_recommendation)).setText(getString(R.string.recommendations));
            getView().findViewById(R.id.btn_continue).setOnClickListener(v -> {
                recommendationsLayout.setVisibility(View.INVISIBLE);
                stopped = false;
            });
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


    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(this::startCameraSource, 200);
    }


    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getContext());
        if (code != ConnectionResult.SUCCESS) {
            setText("GMS error code : " + code);
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
                setText("Unable to start camera source: " + e.getMessage());
            }
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        final Activity thisActivity = getActivity();
        if (!ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        DialogInterface.OnClickListener listener = (dialog, which) -> ActivityCompat.requestPermissions(thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM);
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.permission_camera_rationale)
                .setNeutralButton(android.R.string.ok, listener);
    }

    private void moveAway() {
        if (!isLoading()) {
            tvInfo.setText(getString(R.string.move_away));
            oval_overlay_animation.stopAnim();
            isAnimationStarted = false;
        }

    }

    private void moveClose() {
        if (!isLoading()) {
            tvInfo.setText(getString(R.string.move_close));
            oval_overlay_animation.stopAnim();
            isAnimationStarted = false;
        }
    }


    private void positionFaceInOval() {
        if (!isLoading()) {
            tvInfo.setText(getString(R.string.position_your_face_in_the_oval));
            oval_overlay_animation.stopAnim();
            isAnimationStarted = false;
        }
    }


    private void onFaceScanError(String s, final boolean moveToPreviousPage) {
        if (!isLoading()) {
            oval_overlay_animation.stopAnim();
        }
        new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setMessage(s)
                .setPositiveButton("OK", (dialog, which) -> resetData())
                .show();
    }

    public boolean isLoading() {
        return isRequestSent;
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker();
        }
    }

    private void resetData() {
        seconds = 0;
        realCount = 0;
        fakeCount = 0;
        startTime = -1;
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void onNewItem(int faceId, Face face) {
        }

        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            if (stopped) return;
            if (FaceSolidityMin == INVALID) {
                FaceSolidityMin = mPreview.getWidth() * FaceSolidityMinPercentage / 100;
            }
            if (FaceSolidityMax == INVALID) {
                FaceSolidityMax = mPreview.getWidth() * FaceSolidityMaxPercentage / 100;
            }
            float fullArea = mPreview.getWidth() * mPreview.getHeight();
            float faceWidth = face.getWidth();
            float faceHeight = face.getHeight();
            PointF pointF = face.getPosition();
            float faceArea = faceWidth * faceHeight;
            float solidity = faceArea / fullArea;
            RectF faceRectF = new RectF(pointF.x, pointF.y, pointF.x + faceWidth, pointF.y + faceHeight);
            if (oval_overlay_animation.getVisibleBounds().contains(faceRectF)) {
                if (solidity > FaceSolidityMin) {
                    if (solidity < FaceSolidityMax) {
                        mainHandler.post(() -> {
                            tvInfo.setText(R.string.hold_still);
                            if (!isLoading() && !isAnimationStarted && !oval_overlay_animation.isRunningAnimation()) {
                                isAnimationStarted = true;
                                oval_overlay_animation.startAnimation(5);
                            }
                        });
                        faceDetected = true;
                    } else {
                        faceDetected = false;
                        mainHandler.post(FaceDetectorGmsFragment.this::moveAway);
                        if (seconds < SECONDS_TO_HOLD) {
                            mainHandler.post(FaceDetectorGmsFragment.this::resetData);
                        }
                    }
                } else {
                    faceDetected = false;
                    mainHandler.post(FaceDetectorGmsFragment.this::moveClose);
                    if (seconds < SECONDS_TO_HOLD) {
                        mainHandler.post(FaceDetectorGmsFragment.this::resetData);
                    }
                }
            } else {
                faceDetected = false;
                mainHandler.post(FaceDetectorGmsFragment.this::positionFaceInOval);
                if (seconds < SECONDS_TO_HOLD) {
                    mainHandler.post(FaceDetectorGmsFragment.this::resetData);
                }
            }
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            if (stopped) return;
            faceDetected = false;
            isAnimationStarted = false;
            mainHandler.post(() -> {
                tvInfo.setText("");
                if (!isLoading()) {
                    oval_overlay_animation.stopAnim();
                }
                resetData();
            });
        }

        @Override
        public void onDone() {
            mainHandler.post(() -> {
                tvInfo.setText("");
                if (!isLoading()) {
                    oval_overlay_animation.stopAnim();
                }
                resetData();
            });
            isAnimationStarted = false;
            faceDetected = false;
        }
    }

    private class MyFaceDetector extends Detector<Face> {
        private Detector<Face> mDelegate;

        MyFaceDetector(Detector<Face> delegate) {
            mDelegate = delegate;
        }

        public SparseArray<Face> detect(Frame frame) {
            SparseArray<Face> faceSparseArray = mDelegate.detect(frame);
            if (stopped) return faceSparseArray;
            if (faceDetected && seconds < SECONDS_TO_HOLD) {
                if (startTime == -1) {
                    startTime = System.currentTimeMillis();
                }
                if (faceSparseArray.size() > 0) {
                    Face face = faceSparseArray.get(faceSparseArray.keyAt(0));
                    PointF XY = face.getPosition();
                    Model model = new Model();
                    model.bitmap = processImageOrientation(frame);
                    model.bitmapOriginal = model.bitmap;
                    model.ovalRect = oval_overlay_animation.getVisibleBounds();
                    if (XY.x > 0 && XY.y > 0) {
                        model.rect = new Rect(((int) XY.x), ((int) XY.y), ((int) (face.getWidth())), ((int) (face.getHeight())));
                        new CropBitmapTask().execute(model);
                    }
                }
            }
            return faceSparseArray;
        }

        public boolean isOperational() {
            return mDelegate.isOperational();
        }

        public boolean setFocus(int id) {
            return mDelegate.setFocus(id);
        }
    }

    class CropBitmapTask extends AsyncTask<Model, Void, Model> {

        @Override
        protected Model doInBackground(Model... models) {
            Model model = models[0];
            if (model.rect.left + model.rect.right <= model.bitmap.getWidth() && model.rect.top + model.rect.bottom <= model.bitmap.getHeight()) {
                model.bitmap = Bitmap.createBitmap(model.bitmap, model.rect.left, model.rect.top, model.rect.right, model.rect.bottom);
            }
            return model;
        }

        @SuppressLint("CheckResult")
        @Override
        protected void onPostExecute(Model model) {
            super.onPostExecute(model);
            FaceDetectorGmsFragment.this.model = model;
            HashMap<String, Float> result = (HashMap<String, Float>) tensorFaceDetector.classifyImage(Bitmap.createScaledBitmap(model.bitmap, IMAGE_SIZE, IMAGE_SIZE, true));
            Float resFake = result.get(TensorFaceDetector.FAKE);
            if (resFake != null) {
                if (resFake > FAKE_MIN_RES) {
                    fakeCount++;
                } else {
                    realCount++;
                }
            }
        }
    }

    private Bitmap processImageOrientation(Frame frame) {

        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), frame.getMetadata().getFormat(), frame.getMetadata().getWidth(), frame.getMetadata().getHeight(), null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, frame.getMetadata().getWidth(), frame.getMetadata().getHeight()), 60, byteArrayOutputStream);
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
                case ExifInterface.ORIENTATION_UNDEFINED:
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

    class Model {
        Bitmap bitmap;
        Bitmap bitmapOriginal;
        Bitmap bitmapOvalShape;
        Rect rect;
        RectF ovalRect;
    }


}
