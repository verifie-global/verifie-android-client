package com.verifie.android.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.verifie.android.DocType;
import com.verifie.android.Verifie;
import com.verifie.android.VerifieCallback;
import com.verifie.android.VerifieColorConfig;
import com.verifie.android.VerifieConfig;
import com.verifie.android.VerifieTextConfig;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.Score;
import com.verifie.android.ui.IDCardView;


public class MainActivity extends AppCompatActivity implements VerifieCallback {

    private ProgressDialog progressDialog;

    private Score score;
    private Document document;
    private String documentImageBase64;

    private Verifie verifie;

    private ImageView documentImage;
    private ImageView faceImage;
    private TextView documenttType;
    private TextView documentNumber;
    private TextView birthDate;
    private TextView expiryDate;
    private TextView firstName;
    private TextView lastName;
    private TextView gender;
    private TextView nationality;
    private TextView country;
    private TextView documentValid;
    private TextView faceScore;
    private TextView facialLiveness;
    private TextView nextPage;
    private TextView predictedGender;
    private TextView predictedAge;
    private int docType;
    private boolean isVerifyStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getArguments();
        documentImage = findViewById(R.id.document_image);
        faceImage = findViewById(R.id.face_image);
        documenttType = findViewById(R.id.document_type);
        documentNumber = findViewById(R.id.document_number);
        birthDate = findViewById(R.id.birth_day);
        expiryDate = findViewById(R.id.expiry_day);
        firstName = findViewById(R.id.firstname);
        lastName = findViewById(R.id.lastname);
        gender = findViewById(R.id.gender);
        nationality = findViewById(R.id.nationality);
        country = findViewById(R.id.country);
        documentValid = findViewById(R.id.document_valid);
        faceScore = findViewById(R.id.face_score);
        facialLiveness = findViewById(R.id.facial_liveness);
        nextPage = findViewById(R.id.next_page);
        findViewById(R.id.content_layout).setVisibility(View.INVISIBLE);
    }

    private void getArguments() {
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null && bundle.get(Constants.DocTypes.KEY) != null) {
                docType = bundle.getInt(Constants.DocTypes.KEY, Constants.DocTypes.PASSPORT);
            } else {
//                docType = Constants.DocTypes.PASSPORT;
                docType = Constants.DocTypes.NATIONAL_ID;
            }
        } else {
//            docType = Constants.DocTypes.PASSPORT;
            docType = Constants.DocTypes.NATIONAL_ID;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showDocuments();
        startVerifie();
//        startActivity(new Intent(this, FaceDetectorActivity.class));
    }

    private void startVerifie() {
        if (verifie != null) {
            return;
        }
        isVerifyStarted = true;

        showLoading();

        VerifieColorConfig colorConfig = new VerifieColorConfig();
        colorConfig.setDocCropperFrameColor(Color.WHITE);

        VerifieConfig config = new VerifieConfig("licenseKey", "personId");

        config.setColorConfig(colorConfig);
        VerifieTextConfig textConfig = new VerifieTextConfig();
        config.setDocType(DocType.DOC_TYPE_PASSPORT);
        String text = getString(R.string.passport);
        String pageInfo = getString(R.string.page_info_passport);
        String scanInfo = getString(R.string.scan_info_passport);
        switch (docType) {
            case Constants.DocTypes.NATIONAL_ID:
                config.setDocType(DocType.DOC_TYPE_ID_CARD);
                text = getString(R.string.national_identity_card);
                pageInfo = getString(R.string.page_info_id_card);
                scanInfo = getString(R.string.scan_info_id_card);
                break;
            case Constants.DocTypes.RESIDENCE_PERMIT:
                config.setDocType(DocType.DOC_TYPE_RESIDENCE_PERMIT);
                text = getString(R.string.residence_permit_card);
                pageInfo = getString(R.string.page_info_id_card);
                scanInfo = getString(R.string.scan_info_id_card);
                break;
        }
        textConfig.setPageTitle(text);
        textConfig.setPageInfo(pageInfo);
        textConfig.setScanInfo(scanInfo);
        textConfig.setIdBackside(getString(R.string.scan_backside_of_doc));
        textConfig.setIdBacksideInfo(getString(R.string.scan_backside_of_doc_info));
        config.setTextConfig(textConfig);

        verifie = new Verifie(this, config, this);
        verifie.setIdCardView(new IDCardView() {
            @Override
            public View getViewToShow(ActionHandler actionHandler) {
//          Return the view you want to add after ID card first page scanning, use action handler to close the layout and remove the view you have added
//
//                someBtnOnYourView.setOnClickListener(v -> actionHandler.closeIDCardLayout());
//                return yourView;
                return null;
            }
        });
        verifie.setFaceContainingPercentageInOval(0.2f);
        verifie.start();
    }

    private void showLoading() {
        progressDialog = ProgressDialog.show(this, null, "Loading...", true, false);
    }

    private void hideLoading() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onSessionStarted() {
        hideLoading();
    }

    private void showDocuments() {
        if (document == null || score == null) {
            if (isVerifyStarted) {
                finish();
            }
            return;
        }

        if (document.getError() != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(document.getError())
                    .setPositiveButton("OK", null)
                    .show();
        }

        if (documentImageBase64 != null && !documentImageBase64.isEmpty()) {
            byte[] decodedImage = Base64.decode(documentImageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

            documentImage.setImageBitmap(bitmap);
            documentImage.setVisibility(View.VISIBLE);
        } else {
            documentImage.setVisibility(View.GONE);
        }

        if (score.getBase64Image() != null && !score.getBase64Image().isEmpty()) {
            byte[] decodedImage = Base64.decode(score.getBase64Image(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

            faceImage.setImageBitmap(bitmap);
            faceImage.setVisibility(View.VISIBLE);
        } else {
            faceImage.setVisibility(View.GONE);
        }

        findViewById(R.id.content_layout).setVisibility(View.VISIBLE);
        setSpannableText(faceScore, "Score", String.valueOf(score.getFacialScore()));
        setSpannableText(facialLiveness, "Liveness", String.valueOf(score.isFacialLiveness()));
        setSpannableText(documenttType, "Document type", document.getDocumentType());
        setSpannableText(documentNumber, "Document number", document.getDocumentNumber());
        setSpannableText(birthDate, "Birth date", document.getBirthDate());
        setSpannableText(expiryDate, "Expiry date", document.getExpiryDate());
        setSpannableText(firstName, "First Name", document.getFirstname());
        setSpannableText(lastName, "Last Name", document.getLastname());
        setSpannableText(gender, "Gender", document.getGender());
        setSpannableText(nationality, "Nationality", document.getNationality());
        setSpannableText(country, "Country", document.getCountry());
        setSpannableText(documentValid, "Document valid", String.valueOf(document.isDocumentValid()));
        setSpannableText(nextPage, "Next Page", String.valueOf(document.isNextPage()));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setSpannableText(TextView textView, String prefix, String suffix) {
        if (suffix == null || suffix.isEmpty()) return;
        String wholeText = prefix + "\n" + suffix;
        SpannableString spannableString = new SpannableString(wholeText);
        int startIndex = wholeText.indexOf(suffix);
        int endIndex = startIndex + suffix.length();
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#757373")), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
    }

    @Override
    public void onDocumentReceived(Document document) {
        if (document == null) {
            return;
        }
        if (document.getDocumentFaceImage() != null && !document.getDocumentFaceImage().isEmpty()) {
            documentImageBase64 = document.getDocumentFaceImage();
        }

        if (document.getError() != null || document.getDocumentType() != null && !document.getDocumentType().isEmpty()) {
            this.document = document;
        }
    }

    @Override
    public void onScoreReceived(Score score) {
        this.score = score;
    }

    @Override
    public void onSessionEnded() {
        System.out.println("-=-=-=-=-=-=  On session ended");
    }
}
