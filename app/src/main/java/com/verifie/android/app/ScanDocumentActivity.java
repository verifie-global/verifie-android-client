package com.verifie.android.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ScanDocumentActivity extends AppCompatActivity {
    private ImageView imgDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_document);
        imgDoc = findViewById(R.id.img_doc);
        Intent intent = getIntent();
        int docType = Constants.DocTypes.PASSPORT;
        if (intent != null && intent.hasExtra(Constants.DocTypes.KEY)) {
            docType = intent.getIntExtra(Constants.DocTypes.KEY, Constants.DocTypes.PASSPORT);
            switch (docType) {
                case Constants.DocTypes.PASSPORT:
                    setImagePassport();
                    break;
                case Constants.DocTypes.NATIONAL_ID:
                case Constants.DocTypes.RESIDENCE_PERMIT:
                    setImageId();
                    break;
                case Constants.DocTypes.LIVENESS_CHECK:
                    openScannerWithId(Constants.DocTypes.LIVENESS_CHECK);
                    finish();
                    break;
            }
        }
        final int finalDocType = docType;
        findViewById(R.id.btn_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScannerWithId(finalDocType);
            }
        });
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void openScannerWithId(int docType) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.DocTypes.KEY, docType);
        startActivity(intent);
        this.finish();
    }

    private void setImageId() {
        imgDoc.setImageResource(R.drawable.ic_id_card_scan);
    }

    private void setImagePassport() {
        imgDoc.setImageResource(R.drawable.ic_passport_scan);
    }
}