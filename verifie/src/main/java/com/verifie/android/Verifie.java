package com.verifie.android;

import android.content.Context;

import androidx.annotation.NonNull;

import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.Score;
import com.verifie.android.ui.IDCardView;

public class Verifie implements OperationsManager.OperationsManagerCallback {

    private Context context;
    private VerifieConfig config;
    private VerifieCallback callback;
    private OperationsManager operationsManager;
    private IDCardView idCardView;

    public Verifie(@NonNull Context context, @NonNull VerifieConfig config, @NonNull VerifieCallback callback) {
        this.context = context;
        this.config = config;
        this.callback = callback;
        this.operationsManager = OperationsManager.getInstance();
        this.operationsManager.init(config, this);
    }

    public void setIdCardView(IDCardView idCardView) {
        this.idCardView = idCardView;
    }

    public void setFaceContainingPercentageInOval(float percentage) {
        if (percentage <= 0) {
            percentage = 0.2f;
        }
        this.config.setFaceContainingPercentageInOval(percentage);
    }

    public void start() {
        operationsManager.start(config.getLicenseKey(), config.getPersonId());
    }

    @Override
    public void onAuthorized() {
        notifySessionStarted();
        operationsManager.startDocScanner(context);
    }

    @Override
    public void onDestroy() {
        if (callback != null) {
            callback.onSessionEnded();
        }
    }

    @Override
    public void onDocumentReceived(Document document) {
        notifyDocumentReceived(document);
    }

    @Override
    public void onScoreReceived(Score score) {
        notifyScoreReceived(score);
    }

    @Override
    public IDCardView getIDCardInfoView() {
        return idCardView;
    }

    private void notifySessionStarted() {
        if (callback != null) {
            callback.onSessionStarted();
        }
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
}
