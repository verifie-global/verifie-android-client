package com.verifie.android.api.model.req;

import com.google.gson.annotations.SerializedName;

public class SendFaceImageRequestModel {

    @SerializedName("SelfieImage")
    private String selfieImage;

    public String getSelfieImage() {
        return selfieImage;
    }

    public void setSelfieImage(String selfieImage) {
        this.selfieImage = selfieImage;
    }

    @Override
    public String toString() {
        return "SendDocumentRequestModel{" +
                "selfieImage='" + selfieImage + '\'' +
                '}';
    }
}
