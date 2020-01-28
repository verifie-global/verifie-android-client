package com.verifie.android.api.model.res;

import com.google.gson.annotations.SerializedName;

public class ValidationModel {

    @SerializedName("errorKey")
    private String errorKey;

    @SerializedName("errorDesc")
    private String errorDesc;

    public String getErrorKey() {
        return errorKey;
    }

    public void setErrorKey(String errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public String toString() {
        return "ValidationModel{" +
                "errorKey='" + errorKey + '\'' +
                ", errorDesc='" + errorDesc + '\'' +
                '}';
    }
}
