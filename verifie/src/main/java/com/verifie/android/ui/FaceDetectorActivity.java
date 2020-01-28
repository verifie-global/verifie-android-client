package com.verifie.android.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import com.verifie.android.R;
import com.verifie.android.VerifieConfig;

import java.util.Locale;

public class FaceDetectorActivity extends AppCompatActivity {
    private FaceDetectorGmsFragment faceDetectorGmsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(DocumentScannerActivity.EXTRA_CONFIG)) {
            VerifieConfig config = intent.getParcelableExtra(DocumentScannerActivity.EXTRA_CONFIG);
            changeLanguage(config.getLanguageCode());
        }
        setContentView(R.layout.activity_face_detector);
        faceDetectorGmsFragment = new FaceDetectorGmsFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, faceDetectorGmsFragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if(!faceDetectorGmsFragment.isLoading()){
            super.onBackPressed();
        }
    }

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
}
