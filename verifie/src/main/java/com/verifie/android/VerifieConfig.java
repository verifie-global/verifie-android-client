package com.verifie.android;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.fragment.app.Fragment;

import com.verifie.android.ui.BaseDocumentScannerFragment;
import com.verifie.android.ui.DefaultDocumentScannerFragment;
import com.verifie.android.ui.FaceDetectorGmsFragment;
import com.verifie.android.ui.MrzScanFragment;

public class VerifieConfig implements Parcelable {

    private static final String DEFAULT_LANGUAGE_CODE = "ENG";

    private String licenseKey;
    private String personId;
    private String languageCode;
    private VerifieTextConfig textConfig;
    private VerifieColorConfig colorConfig;
    private DocType docType;
    private float faceContainingPercentageInOval = 0.6f; //Default Value

    private Class<? extends BaseDocumentScannerFragment> documentScannerFragment = DefaultDocumentScannerFragment.class;
    private Class<? extends MrzScanFragment> passportScannerFragment = MrzScanFragment.class;
    private Class<? extends FaceDetectorGmsFragment> faceDetectorFragment = FaceDetectorGmsFragment.class;

    public VerifieConfig(Context context, String licenseKey, String personId, DocType docType) {
        this.licenseKey = licenseKey;
        this.personId = personId;
        this.languageCode = DEFAULT_LANGUAGE_CODE;
        this.textConfig = VerifieTextConfig.defaultConfig(context);
        this.colorConfig = VerifieColorConfig.defaultConfig();
        this.docType = docType;
    }

    public VerifieConfig(Context context, String licenseKey, String personId) {
        this(context, licenseKey, personId, DocType.DOC_TYPE_ID_CARD);
    }


    protected VerifieConfig(Parcel in) {
        licenseKey = in.readString();
        personId = in.readString();
        languageCode = in.readString();
        textConfig = in.readParcelable(VerifieTextConfig.class.getClassLoader());
        colorConfig = in.readParcelable(VerifieColorConfig.class.getClassLoader());
        docType = DocType.valueOf(in.readString());
        faceContainingPercentageInOval = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(licenseKey);
        dest.writeString(personId);
        dest.writeString(languageCode);
        dest.writeParcelable(textConfig, flags);
        dest.writeParcelable(colorConfig, flags);
        dest.writeString(docType.name());
        dest.writeFloat(faceContainingPercentageInOval);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VerifieConfig> CREATOR = new Creator<VerifieConfig>() {
        @Override
        public VerifieConfig createFromParcel(Parcel in) {
            return new VerifieConfig(in);
        }

        @Override
        public VerifieConfig[] newArray(int size) {
            return new VerifieConfig[size];
        }
    };

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

    public Class<? extends Fragment> getDocumentScannerFragment() {
        if (docType == DocType.DOC_TYPE_PASSPORT) {
            return passportScannerFragment;
        }
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

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public float getFaceContainingPercentageInOval() {
        return faceContainingPercentageInOval;
    }

    public void setFaceContainingPercentageInOval(float faceContainingPercentageInOval) {
        this.faceContainingPercentageInOval = faceContainingPercentageInOval;
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
        if (docType != null ? !docType.equals(config.docType) : config.docType != null)
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
        result = 31 * result + (docType != null ? docType.hashCode() : 0);
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
                ", docType=" + docType +
                ", faceContainingPercentageInOval=" + faceContainingPercentageInOval +
                '}';
    }
}
