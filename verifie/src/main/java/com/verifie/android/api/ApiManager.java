package com.verifie.android.api;

import com.verifie.android.BuildConfig;
import com.verifie.android.api.model.req.SendDocumentRequestModel;
import com.verifie.android.api.model.req.SendFaceImageRequestModel;
import com.verifie.android.api.model.res.AccessTokenModel;
import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.api.model.res.Score;
import com.verifie.android.api.service.VerifieResetService;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiManager {

    private VerifieResetService service;

    public ApiManager() {
        service = getVerifieResetService();
    }

    public Single<ResponseModel<AccessTokenModel>> authorize(String licenseKey, String personID) {
        return service.authorize(licenseKey, personID);
    }

    public Single<ResponseModel<Document>> sendDocument(String accessToken, String imageData) {
        SendDocumentRequestModel sendDocumentRequestModel = new SendDocumentRequestModel();
        sendDocumentRequestModel.setDocumentImage(imageData);

        return service.sendDocumentImage(accessToken, sendDocumentRequestModel);
    }

    public Single<ResponseModel<Score>> sendFaceImage(String accessToken, String imageData) {
        SendFaceImageRequestModel sendFaceImageRequestModel = new SendFaceImageRequestModel();
        sendFaceImageRequestModel.setSelfieImage(imageData);

        return service.sendFaceImage(accessToken, sendFaceImageRequestModel);
    }

    private VerifieResetService getVerifieResetService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .readTimeout(40, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();

        return retrofit.create(VerifieResetService.class);
    }
}
