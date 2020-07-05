package com.verifie.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.verifie.android.api.ApiManager;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.api.model.res.Score;
import com.verifie.android.ui.DocumentScannerActivity;
import com.verifie.android.ui.IDCardView;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class OperationsManager {

    private static final String TAG = "OperationsManager";

    private static OperationsManager instance;

    private boolean initialized;

    private String accessToken;
    private VerifieConfig config;
    private ApiManager apiManager;
    private OperationsManagerCallback callback;

    private OperationsManager() {
        this.apiManager = new ApiManager();
    }

    public void init(VerifieConfig config, OperationsManagerCallback callback) {
        this.config = config;
        this.callback = callback;
        this.initialized = true;
    }

    @SuppressLint("CheckResult")
    public void start(String licenseKey, String personID) {
        ensureManagerInitialized();

        apiManager.authorize(licenseKey, personID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        accessTokenModelResponseModel -> {
                            accessToken = accessTokenModelResponseModel.getResult().getAccessToken();
                            notifySessionStarted();
                        },
                        throwable -> Log.e(TAG, "accept: ", throwable));
    }

    public Single<ResponseModel<Document>> uploadDocument(String imageData) {
        return apiManager.sendDocument(accessToken, imageData);
    }

    public Single<ResponseModel<Score>> uploadFace(String imageData) {
        return apiManager.sendFaceImage(accessToken, imageData);
    }

    public void startDocScanner(Context context) {
        DocumentScannerActivity.start(context, config);
    }

    public void onDocumentReceived(Document document) {
        notifyDocumentReceived(document);
    }

    public void onScoreReceived(Score score) {
        notifyScoreReceived(score);
    }

    private void notifySessionStarted() {
        if (callback != null) {
            callback.onAuthorized();
        }
    }

    public void notifySessionFinished() {
        if (callback != null) {
            callback.onDestroy();
        }
    }

    public IDCardView getIdCardView() {
        if (callback == null) {
            return null;
        }
        return callback.getIDCardInfoView();
    }

    private void notifyDocumentReceived(Document document) {
        if (callback != null) {
            callback.onDocumentReceived(document);
        }
    }

    private void notifyScoreReceived(Score score) {
        if (callback != null) {
            callback.onScoreReceived(score);
        }
    }

    private void ensureManagerInitialized() {
        if (!initialized) {
            throw new RuntimeException("Not initialed");
        }
    }

    public static OperationsManager getInstance() {
        if (instance == null) {
            instance = new OperationsManager();
        }
        return instance;
    }

    public VerifieConfig getConfig() {
        return config;
    }

    interface OperationsManagerCallback {

        void onAuthorized();

        void onDestroy();

        void onDocumentReceived(Document document);

        void onScoreReceived(Score score);

        IDCardView getIDCardInfoView();
    }
}
