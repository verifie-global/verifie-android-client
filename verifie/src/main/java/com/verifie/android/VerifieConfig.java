package com.verifie.android;

import android.os.Parcel;
import android.os.Parcelable;

import com.verifie.android.ui.BaseDocumentScannerFragment;
import com.verifie.android.ui.DefaultDocumentScannerFragment;
import com.verifie.android.ui.FaceDetectorGmsFragment;

public class VerifieConfig implements Parcelable {

    private static final String DEFAULT_LANGUAGE_CODE = "ENG";

    private String licenseKey;
    private String personId;
    private String languageCode;
    private VerifieTextConfig textConfig;
    private VerifieColorConfig colorConfig;

    private Class<? extends BaseDocumentScannerFragment> documentScannerFragment = DefaultDocumentScannerFragment.class;
    private Class<? extends FaceDetectorGmsFragment> faceDetectorFragment = FaceDetectorGmsFragment.class;

    public VerifieConfig(String licenseKey, String personId) {
        this.licenseKey = licenseKey;
        this.personId = personId;
        this.languageCode = DEFAULT_LANGUAGE_CODE;
        this.textConfig = VerifieTextConfig.defaultConfig();
        this.colorConfig = VerifieColorConfig.defaultConfig();
    }

    protected VerifieConfig(Parcel in) {
        this.licenseKey = in.readString();
        this.personId = in.readString();
        this.languageCode = in.readString();
        this.textConfig = in.readParcelable(VerifieTextConfig.class.getClassLoader());
        this.colorConfig = in.readParcelable(VerifieColorConfig.class.getClassLoader());
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public VerifieConfig setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        return this;
    }

    public String getPersonId() {
        return personId;
    }

    public VerifieConfig setPersonId(String personId) {
        this.personId = personId;
        return this;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public VerifieConfig setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
        return this;
    }

    public VerifieTextConfig getTextConfig() {
        return textConfig;
    }

    public VerifieConfig setTextConfig(VerifieTextConfig textConfig) {
        this.textConfig = textConfig;
        return this;
    }

    public VerifieColorConfig getColorConfig() {
        return colorConfig;
    }

    public VerifieConfig setColorConfig(VerifieColorConfig colorConfig) {
        this.colorConfig = colorConfig;
        return this;
    }

    public Class<? extends BaseDocumentScannerFragment> getDocumentScannerFragment() {
        return documentScannerFragment;
    }

    public void setDocumentScannerFragment(Class<? extends BaseDocumentScannerFragment> documentScannerFragment) {
        this.documentScannerFragment = documentScannerFragment;
    }

    public Class<? extends FaceDetectorGmsFragment> getFaceDetectorFragment() {
        return faceDetectorFragment;
    }

    public void setFaceDetectorFragment(Class<? extends FaceDetectorGmsFragment> faceDetectorFragment) {
        this.faceDetectorFragment = faceDetectorFragment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerifieConfig config = (VerifieConfig) o;

        if (licenseKey != null ? !licenseKey.equals(config.licenseKey) : config.licenseKey != null)
            return false;
        if (personId != null ? !personId.equals(config.personId) : config.personId != null)
            return false;
        if (languageCode != null ? !languageCode.equals(config.languageCode) : config.languageCode != null)
            return false;
        if (textConfig != null ? !textConfig.equals(config.textConfig) : config.textConfig != null)
            return false;
        return colorConfig != null ? colorConfig.equals(config.colorConfig) : config.colorConfig == null;
    }

    @Override
    public int hashCode() {
        int result = licenseKey != null ? licenseKey.hashCode() : 0;
        result = 31 * result + (personId != null ? personId.hashCode() : 0);
        result = 31 * result + (languageCode != null ? languageCode.hashCode() : 0);
        result = 31 * result + (textConfig != null ? textConfig.hashCode() : 0);
        result = 31 * result + (colorConfig != null ? colorConfig.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "VerifieConfig{" +
                "licenseKey='" + licenseKey + '\'' +
                ", personId='" + personId + '\'' +
                ", languageCode='" + languageCode + '\'' +
                ", textConfig=" + textConfig +
                ", colorConfig=" + colorConfig +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.licenseKey);
        dest.writeString(this.personId);
        dest.writeString(this.languageCode);
        dest.writeParcelable(this.textConfig, flags);
        dest.writeParcelable(this.colorConfig, flags);
    }

    public static final Parcelable.Creator<VerifieConfig> CREATOR = new Parcelable.Creator<VerifieConfig>() {
        @Override
        public VerifieConfig createFromParcel(Parcel source) {
            return new VerifieConfig(source);
        }

        @Override
        public VerifieConfig[] newArray(int size) {
            return new VerifieConfig[size];
        }
    };
}
