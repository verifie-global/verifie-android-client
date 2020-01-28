package com.verifie.android.api.service;

import com.verifie.android.api.model.req.SendDocumentRequestModel;
import com.verifie.android.api.model.req.SendFaceImageRequestModel;
import com.verifie.android.api.model.res.AccessTokenModel;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.api.model.res.Score;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface VerifieResetService {

    @GET("api/Main/AccessToken")
    Single<ResponseModel<AccessTokenModel>> authorize(@Query("LicenseKey") String licenseKey, @Query("PersonID") String personID);

    @POST("/api/Main/Document")
    Single<ResponseModel<Document>> sendDocumentImage(@Header("Authorization") String accessToken, @Body SendDocumentRequestModel sendDocumentRequestModel);

    @POST("/api/Main/Score")
    Single<ResponseModel<Score>> sendFaceImage(@Header("Authorization") String accessToken, @Body SendFaceImageRequestModel sendFaceImageRequestModel);
}
