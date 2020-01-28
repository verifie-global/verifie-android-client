package com.verifie.android.api.model.res;

import com.google.gson.annotations.SerializedName;

public class AccessTokenModel {

    @SerializedName("accessToken")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "AccessTokenModel{" +
                "accessToken='" + accessToken + '\'' +
                '}';
    }
}
