package com.verifie.android;

import android.os.Parcel;
import android.os.Parcelable;

public class VerifieTextConfig implements Parcelable {

    private String movePhoneCloser;
    private String movePhoneAway;
    private String blinkEyes;
    private String alignTap;
    private String pageInfo;
    private String scanInfo;
    private String idBackside;
    private String idBacksideInfo;
    private String faceFailed;
    private String eyesFailed;

    public VerifieTextConfig() {
    }


    protected VerifieTextConfig(Parcel in) {
        movePhoneCloser = in.readString();
        movePhoneAway = in.readString();
        blinkEyes = in.readString();
        alignTap = in.readString();
        pageInfo = in.readString();
        scanInfo = in.readString();
        idBackside = in.readString();
        idBacksideInfo = in.readString();
        faceFailed = in.readString();
        eyesFailed = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(movePhoneCloser);
        dest.writeString(movePhoneAway);
        dest.writeString(blinkEyes);
        dest.writeString(alignTap);
        dest.writeString(pageInfo);
        dest.writeString(scanInfo);
        dest.writeString(idBackside);
        dest.writeString(idBacksideInfo);
        dest.writeString(faceFailed);
        dest.writeString(eyesFailed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VerifieTextConfig> CREATOR = new Creator<VerifieTextConfig>() {
        @Override
        public VerifieTextConfig createFromParcel(Parcel in) {
            return new VerifieTextConfig(in);
        }

        @Override
        public VerifieTextConfig[] newArray(int size) {
            return new VerifieTextConfig[size];
        }
    };

    public String getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(String pageInfo) {
        this.pageInfo = pageInfo;
    }

    public String getScanInfo() {
        return scanInfo;
    }

    public void setScanInfo(String scanInfo) {
        this.scanInfo = scanInfo;
    }

    public String getMovePhoneCloser() {
        return movePhoneCloser;
    }

    public void setMovePhoneCloser(String movePhoneCloser) {
        this.movePhoneCloser = movePhoneCloser;
    }

    public String getMovePhoneAway() {
        return movePhoneAway;
    }

    public void setMovePhoneAway(String movePhoneAway) {
        this.movePhoneAway = movePhoneAway;
    }

    public String getBlinkEyes() {
        return blinkEyes;
    }

    public void setBlinkEyes(String blinkEyes) {
        this.blinkEyes = blinkEyes;
    }

    public String getAlignTap() {
        return alignTap;
    }

    public void setAlignTap(String alignTap) {
        this.alignTap = alignTap;
    }

    public String getIdBackside() {
        return idBackside;
    }

    public void setIdBackside(String idBackside) {
        this.idBackside = idBackside;
    }

    public String getFaceFailed() {
        return faceFailed;
    }

    public void setFaceFailed(String faceFailed) {
        this.faceFailed = faceFailed;
    }

    public String getEyesFailed() {
        return eyesFailed;
    }

    public void setEyesFailed(String eyesFailed) {
        this.eyesFailed = eyesFailed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerifieTextConfig that = (VerifieTextConfig) o;

        if (movePhoneCloser != null ? !movePhoneCloser.equals(that.movePhoneCloser) : that.movePhoneCloser != null)
            return false;
        if (movePhoneAway != null ? !movePhoneAway.equals(that.movePhoneAway) : that.movePhoneAway != null)
            return false;
        if (blinkEyes != null ? !blinkEyes.equals(that.blinkEyes) : that.blinkEyes != null)
            return false;
        if (alignTap != null ? !alignTap.equals(that.alignTap) : that.alignTap != null)
            return false;
        if (idBackside != null ? !idBackside.equals(that.idBackside) : that.idBackside != null)
            return false;
        if (faceFailed != null ? !faceFailed.equals(that.faceFailed) : that.faceFailed != null)
            return false;
        return eyesFailed != null ? eyesFailed.equals(that.eyesFailed) : that.eyesFailed == null;
    }

    @Override
    public int hashCode() {
        int result = movePhoneCloser != null ? movePhoneCloser.hashCode() : 0;
        result = 31 * result + (movePhoneAway != null ? movePhoneAway.hashCode() : 0);
        result = 31 * result + (blinkEyes != null ? blinkEyes.hashCode() : 0);
        result = 31 * result + (alignTap != null ? alignTap.hashCode() : 0);
        result = 31 * result + (idBackside != null ? idBackside.hashCode() : 0);
        result = 31 * result + (faceFailed != null ? faceFailed.hashCode() : 0);
        result = 31 * result + (eyesFailed != null ? eyesFailed.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "VerifieTextConfig{" +
                "movePhoneCloser='" + movePhoneCloser + '\'' +
                ", movePhoneAway='" + movePhoneAway + '\'' +
                ", blinkEyes='" + blinkEyes + '\'' +
                ", alignTap='" + alignTap + '\'' +
                ", idBackside='" + idBackside + '\'' +
                ", faceFailed='" + faceFailed + '\'' +
                ", eyesFailed='" + eyesFailed + '\'' +
                '}';
    }

    public static VerifieTextConfig defaultConfig() {
        VerifieTextConfig config = new VerifieTextConfig();

        config.movePhoneCloser = "Move phone closer";
        config.movePhoneAway = "Move phone away";
        config.blinkEyes = "Blink eyes";
        config.alignTap = "Align and Tap";
        config.idBackside = "Backside of ID";
        config.faceFailed = "Face failed!!!";
        config.eyesFailed = "Eyes failed!!!";

        return config;
    }

    public void setIdBacksideInfo(String string) {
        this.idBacksideInfo = string;
    }

    public String getIdBacksideInfo() {
        return idBacksideInfo;
    }
}
