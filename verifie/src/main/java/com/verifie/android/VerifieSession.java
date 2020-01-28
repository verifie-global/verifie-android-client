package com.verifie.android;

import com.verifie.android.api.model.res.AccessTokenModel;
import com.verifie.android.api.model.res.ResponseModel;
import com.verifie.android.api.service.VerifieResetService;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class VerifieSession {

    private VerifieConfig config;
    private AppSessionCallback callback;
    private VerifieResetService verifieResetService;

    private boolean authorized = false;
    private String accesstoken;

    private VerifieSession(AppSessionCallback callback, VerifieConfig config) {
        this.callback = callback;
        this.config = config;
        initSession();
    }

    private void initSession() {
        initApiService();
    }

    private void initApiService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        verifieResetService = retrofit.create(VerifieResetService.class);
    }

    private void handleAuthResponse(ResponseModel<AccessTokenModel> response) {
        String token = response.getResult().getAccessToken();

        authorized = true;
        this.accesstoken = token;
    }



    static VerifieSession startSession(AppSessionCallback callback, VerifieConfig config) {
        return new VerifieSession(callback, config);
    }

    interface AppSessionCallback {

        void onSessionStarted();

        void onSessionFailed();
    }
}
