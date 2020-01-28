package com.verifie.android;

import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.Score;

public interface VerifieCallback {

    void onSessionStarted();

    void onDocumentReceived(Document document);

    void onScoreReceived(Score score);
}
