package com.verifie.android.ui;


import android.Manifest;
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
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.api.model.res.Score;
import com.verifie.android.gms.CameraSourcePreview;
import com.verifie.android.gms.TensorFaceDetector;
import com.verifie.android.util.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class FaceDetectorGmsFragment extends Fragment {
    public static final float FaceSolidityMin = 0.040F;
    public static final float FaceSolidityMax = 0.090F;
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
    private View loading;
    private ImageView imgPreview;

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
        View view = inflater.inflate(R.layout.fragment_face_detector_gms, container, false);

        mPreview = view.findViewById(R.id.preview);
        tvInfo = view.findViewById(R.id.tv_info);
        loading = view.findViewById(R.id.progress_bar_holder);
        imgPreview = view.findViewById(R.id.img_preview);

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
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        }

        mCameraSource = new CameraSource.Builder(context, myFaceDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(60.0f)
                .build();
        tensorFaceDetector = new TensorFaceDetector(getActivity());
    }


    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
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

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.permission_camera_rationale)
                .setNeutralButton(android.R.string.ok, listener);
    }

    private void moveAway() {
        if (loading.getVisibility() != View.VISIBLE) {
            tvInfo.setText(getString(R.string.move_away));
        }

    }

    private void moveClose() {
        if (loading.getVisibility() != View.VISIBLE) {
            tvInfo.setText(getString(R.string.move_close));
        }
    }


    private void onFaceScanError(String s, final boolean moveToPreviousPage) {
        loading.setVisibility(View.GONE);
        new android.support.v7.app.AlertDialog.Builder(getActivity())
                .setMessage(s)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetData();
                    }
                })
                .show();
    }

    public boolean isLoading() {
        return loading.getVisibility() == View.VISIBLE;
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
            float fullArea = mPreview.getWidth() * mPreview.getHeight();
            float faceWidth = face.getWidth();
            float faceHeight = face.getHeight();
            float faceArea = faceWidth * faceHeight;
            float solidity = faceArea / fullArea;
            if (solidity > FaceSolidityMin) {
                if (solidity < FaceSolidityMax) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            tvInfo.setText(R.string.hold_still);
                        }
                    });
                    faceDetected = true;
                } else {
                    faceDetected = false;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            moveAway();
                        }
                    });
                    if (seconds < SECONDS_TO_HOLD) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                resetData();
                            }
                        });
                    }
                }
            } else {
                faceDetected = false;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        moveClose();
                    }
                });
                if (seconds < SECONDS_TO_HOLD) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            resetData();
                        }
                    });
                }
            }
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            faceDetected = false;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    tvInfo.setText("");
                    resetData();
                }
            });
        }

        @Override
        public void onDone() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    tvInfo.setText("");
                    resetData();
                }
            });
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
            if (faceDetected && seconds < SECONDS_TO_HOLD) {
                if (startTime == -1) {
                    startTime = System.currentTimeMillis();
                }
                if (faceSparseArray.size() > 0) {
                    YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), frame.getMetadata().getFormat(), frame.getMetadata().getWidth(), frame.getMetadata().getHeight(), null);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0, 0, frame.getMetadata().getWidth(), frame.getMetadata().getHeight()), 100, byteArrayOutputStream);
                    byte[] jpegArray = byteArrayOutputStream.toByteArray();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
                    Face face = faceSparseArray.get(faceSparseArray.keyAt(0));
                    Model model = new Model();
                    model.bitmap = bitmap;
                    model.bitmapOriginal = bitmap;

                    if (face != null) {
                        PointF pointF = face.getPosition();
                        if (pointF != null) {
                            if (pointF.x > 0 && pointF.y > 0) {
                                model.rect = new Rect(((int) pointF.x), ((int) pointF.y), ((int) (face.getWidth())), ((int) (face.getHeight())));
                                new CropBitmapTask().execute(model);
                            }
                        }
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
            Matrix matrix = new Matrix();
            matrix.postRotate(-90);
            matrix.postScale(-1, 1);
            model.bitmap = Bitmap.createBitmap(model.bitmap, 0, 0, model.bitmap.getWidth(), model.bitmap.getHeight(), matrix, false);
            model.bitmapOriginal = model.bitmap;
            if (model.rect.left + model.rect.right <= model.bitmap.getWidth() && model.rect.top + model.rect.bottom <= model.bitmap.getHeight()) {
                model.bitmap = Bitmap.createBitmap(model.bitmap, model.rect.left, model.rect.top, model.rect.right, model.rect.bottom);
            }
            return model;
        }

        @SuppressLint("CheckResult")
        @Override
        protected void onPostExecute(Model model) {
            super.onPostExecute(model);
            HashMap<String, Float> result = (HashMap<String, Float>) tensorFaceDetector.classifyImage(Bitmap.createScaledBitmap(model.bitmap, IMAGE_SIZE, IMAGE_SIZE, true));
            if (result.get(TensorFaceDetector.FAKE) != null) {
                if (result.get(TensorFaceDetector.FAKE) > FAKE_MIN_RES) {
                    fakeCount++;
                } else {
                    realCount++;
                }
            }
            long diffTime = System.currentTimeMillis() - startTime;
            if (startTime != -1 && diffTime >= SECONDS_TO_HOLD * 1000) {
                seconds = SECONDS_TO_HOLD;
                if (areRealsGreather()) {
                    if (loading.getVisibility() != View.VISIBLE) {
                        loading.setVisibility(View.VISIBLE);
                        imgPreview.setVisibility(View.VISIBLE);
                        tvInfo.setVisibility(View.GONE);
                        imgPreview.setImageBitmap(model.bitmapOriginal);
                        final String imageBase64 = ImageUtils.getImageBase64(model.bitmapOriginal);
                        OperationsManager.getInstance().uploadFace(imageBase64)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<ResponseModel<Score>>() {

                                    @Override
                                    public void accept(ResponseModel<Score> faceScannedModelResponse) throws Exception {
                                        getActivity().finish();
                                        faceScannedModelResponse.getResult().setBase64Image(imageBase64);
                                        OperationsManager.getInstance().onScoreReceived(faceScannedModelResponse.getResult());
                                    }
                                }, new Consumer<Throwable>() {

                                    @Override
                                    public void accept(Throwable throwable) {
                                        Log.e(TAG, "accept: ", throwable);
                                        onFaceScanError("Please take a face photo again", true);
                                        tvInfo.setVisibility(View.VISIBLE);
                                        imgPreview.setVisibility(View.GONE);
                                    }
                                });
                    }
                } else {
                    onFaceScanError("Light up face evenly", false);
                }
            }
        }
    }

    class Model {
        Bitmap bitmap;
        Bitmap bitmapOriginal;
        Rect rect;
    }


}
