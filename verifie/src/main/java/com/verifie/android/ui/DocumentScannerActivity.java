package com.verifie.android.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.verifie.android.OperationsManager;
import com.verifie.android.R;
import com.verifie.android.VerifieConfig;

import java.util.Locale;

public final class DocumentScannerActivity extends AppCompatActivity {

    public static final String EXTRA_CONFIG = "config";

    private static final int REQUEST_ENABLE_CAMERA = 1;

    private VerifieConfig config;
    private Fragment fragment;


    private Locale createLocale(String languageCode) {
        if (languageCode.equals("hy")) {
            return new Locale("hy", "AM");
        } else if (languageCode.equals("ru")) {
            return new Locale("ru");
        } else {
            return new Locale(languageCode);
        }
    }

    private void changeLanguage(String language) {
        Locale.setDefault(createLocale(language));
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        Configuration configuration = getResources().getConfiguration();
        configuration.locale = createLocale(language);
        getResources().updateConfiguration(configuration, displayMetrics);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = getIntent().getParcelableExtra(EXTRA_CONFIG);
        changeLanguage(config.getLanguageCode());


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_document_scanner);
        if (checkPermissions()) {
            openDocumentScanner();
//            openFaceDetectorActivity();
        } else {
            requestCameraAccess();
        }
    }

    @Override
    public void onBackPressed() {
//        if (!((DefaultDocumentScannerFragment) fragment).isLoading()) {
        super.onBackPressed();
        OperationsManager.getInstance().notifySessionFinished();
//        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    public void finish(boolean notify) {
        if (notify) {
            OperationsManager.getInstance().notifySessionFinished();
        }
        finish();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ENABLE_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openDocumentScanner();
        } else {
            finish();
        }
    }

    private void openDocumentScanner() {
        try {
            fragment = config.getDocumentScannerFragment().newInstance();
            Bundle args = new Bundle();
            args.putParcelable(BaseDocumentScannerFragment.ARG_CONFIG, config);
            fragment.setArguments(args);


            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate fragment " + config.getDocumentScannerFragment());
        }
    }

    private void openFaceDetectorActivity() {
        Intent intent = new Intent(this, FaceDetectorActivity.class);
        intent.putExtra(DocumentScannerActivity.EXTRA_CONFIG, config);
        startActivity(intent);
        finish(false);
    }


    public void openIdCardBacksideScanner() {
        try {
            MrzScanFragment fragment = new MrzScanFragment();
            Bundle args = new Bundle();
            args.putParcelable(BaseDocumentScannerFragment.ARG_CONFIG, config);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraAccess() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setMessage("Camera permission is required.")
                    .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(DocumentScannerActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_ENABLE_CAMERA))
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    REQUEST_ENABLE_CAMERA);
        }
    }

    public static void start(Context context, VerifieConfig config) {
        Intent intent = new Intent(context, DocumentScannerActivity.class);
        intent.putExtra(EXTRA_CONFIG, config);
        context.startActivity(intent);
    }
}
