package com.verifie.android.api.model.req;

import com.google.gson.annotations.SerializedName;

public class SendDocumentRequestModel {

    @SerializedName("DocumentImage")
    private String documentImage;

    public String getDocumentImage() {
        return documentImage;
    }

    public void setDocumentImage(String documentImage) {
        this.documentImage = documentImage;
    }

    @Override
    public String toString() {
        return "SendDocumentRequestModel{" +
                "documentImage='" + documentImage + '\'' +
                '}';
    }
}
