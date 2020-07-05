package com.verifie.android;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

public class VerifieTextConfig implements Parcelable {

    private String movePhoneCloser;
    private String movePhoneAway;
    private String blinkEyes;
    private String pageTitle;
    private String pageInfo;
    private String scanInfo;
    private String idBackside;
    private String idBacksideInfo;
    private String faceFailed;
    private String eyesFailed;
    private String holdStillText;
    private String positionFaceInOval;
    private String scannedText;
    private String recommendationsTitleText;
    private String lightUpFaceText;
    private String greatText;
    private String noGlassesText;
    private String noShadowText;
    private String noFlashLightText;
    private String continueText;
    private String cameraPermissionRational;


    public VerifieTextConfig() {
        Resources resources = App.getInstance().getResources();
        this.movePhoneCloser = resources.getString(R.string.move_close);
        this.movePhoneAway = resources.getString(R.string.move_away);
        this.holdStillText = resources.getString(R.string.hold_still);
        this.positionFaceInOval = resources.getString(R.string.position_your_face_in_the_oval);
        this.scannedText = resources.getString(R.string.scanned);
        this.recommendationsTitleText = resources.getString(R.string.recommendations);
        this.lightUpFaceText = resources.getString(R.string.light_face_evenly);
        this.greatText = resources.getString(R.string.great);
        this.noGlassesText = resources.getString(R.string.no_glasses);
        this.noShadowText = resources.getString(R.string.no_shadow);
        this.noFlashLightText = resources.getString(R.string.no_flash);
        this.continueText = resources.getString(R.string.continue_key);
        this.cameraPermissionRational = resources.getString(R.string.permission_camera_rationale);
    }


    protected VerifieTextConfig(Parcel in) {
        movePhoneCloser = in.readString();
        movePhoneAway = in.readString();
        blinkEyes = in.readString();
        pageTitle = in.readString();
        pageInfo = in.readString();
        scanInfo = in.readString();
        idBackside = in.readString();
        idBacksideInfo = in.readString();
        faceFailed = in.readString();
        eyesFailed = in.readString();
        holdStillText = in.readString();
        positionFaceInOval = in.readString();
        scannedText = in.readString();
        recommendationsTitleText = in.readString();
        lightUpFaceText = in.readString();
        greatText = in.readString();
        noGlassesText = in.readString();
        noShadowText = in.readString();
        noFlashLightText = in.readString();
        continueText = in.readString();
        cameraPermissionRational = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(movePhoneCloser);
        dest.writeString(movePhoneAway);
        dest.writeString(blinkEyes);
        dest.writeString(pageTitle);
        dest.writeString(pageInfo);
        dest.writeString(scanInfo);
        dest.writeString(idBackside);
        dest.writeString(idBacksideInfo);
        dest.writeString(faceFailed);
        dest.writeString(eyesFailed);
        dest.writeString(holdStillText);
        dest.writeString(positionFaceInOval);
        dest.writeString(scannedText);
        dest.writeString(recommendationsTitleText);
        dest.writeString(lightUpFaceText);
        dest.writeString(greatText);
        dest.writeString(noGlassesText);
        dest.writeString(noShadowText);
        dest.writeString(noFlashLightText);
        dest.writeString(continueText);
        dest.writeString(cameraPermissionRational);
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

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
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
        if (pageTitle != null ? !pageTitle.equals(that.pageTitle) : that.pageTitle != null)
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
        result = 31 * result + (pageTitle != null ? pageTitle.hashCode() : 0);
        result = 31 * result + (idBackside != null ? idBackside.hashCode() : 0);
        result = 31 * result + (faceFailed != null ? faceFailed.hashCode() : 0);
        result = 31 * result + (eyesFailed != null ? eyesFailed.hashCode() : 0);
        return result;
    }

    public String getHoldStillText() {
        return holdStillText;
    }

    public void setHoldStillText(String holdStillText) {
        this.holdStillText = holdStillText;
    }

    public String getPositionFaceInOval() {
        return positionFaceInOval;
    }

    public void setPositionFaceInOval(String positionFaceInOval) {
        this.positionFaceInOval = positionFaceInOval;
    }

    public String getScannedText() {
        return scannedText;
    }

    public void setScannedText(String scannedText) {
        this.scannedText = scannedText;
    }

    public String getRecommendationsTitleText() {
        return recommendationsTitleText;
    }

    public void setRecommendationsTitleText(String recommendationsTitleText) {
        this.recommendationsTitleText = recommendationsTitleText;
    }

    public String getLightUpFaceText() {
        return lightUpFaceText;
    }

    public void setLightUpFaceText(String lightUpFaceText) {
        this.lightUpFaceText = lightUpFaceText;
    }

    public String getGreatText() {
        return greatText;
    }

    public void setGreatText(String greatText) {
        this.greatText = greatText;
    }

    public String getNoGlassesText() {
        return noGlassesText;
    }

    public void setNoGlassesText(String noGlassesText) {
        this.noGlassesText = noGlassesText;
    }

    public String getNoShadowText() {
        return noShadowText;
    }

    public void setNoShadowText(String noShadowText) {
        this.noShadowText = noShadowText;
    }

    public String getNoFlashLightText() {
        return noFlashLightText;
    }

    public void setNoFlashLightText(String noFlashLightText) {
        this.noFlashLightText = noFlashLightText;
    }

    public String getContinueText() {
        return continueText;
    }

    public void setContinueText(String continueText) {
        this.continueText = continueText;
    }

    @Override
    public String toString() {
        return "VerifieTextConfig{" +
                "movePhoneCloser='" + movePhoneCloser + '\'' +
                ", movePhoneAway='" + movePhoneAway + '\'' +
                ", blinkEyes='" + blinkEyes + '\'' +
                ", pageTitle='" + pageTitle + '\'' +
                ", pageInfo='" + pageInfo + '\'' +
                ", scanInfo='" + scanInfo + '\'' +
                ", idBackside='" + idBackside + '\'' +
                ", idBacksideInfo='" + idBacksideInfo + '\'' +
                ", faceFailed='" + faceFailed + '\'' +
                ", eyesFailed='" + eyesFailed + '\'' +
                ", holdStillText='" + holdStillText + '\'' +
                ", positionFaceInOval='" + positionFaceInOval + '\'' +
                ", scannedText='" + scannedText + '\'' +
                ", recommendationsTitleText='" + recommendationsTitleText + '\'' +
                ", lightUpFaceText='" + lightUpFaceText + '\'' +
                ", greatText='" + greatText + '\'' +
                ", noGlassesText='" + noGlassesText + '\'' +
                ", noShadowText='" + noShadowText + '\'' +
                ", noFlashLightText='" + noFlashLightText + '\'' +
                ", continueText='" + continueText + '\'' +
                '}';
    }

    public static VerifieTextConfig defaultConfig() {
        VerifieTextConfig config = new VerifieTextConfig();
        config.blinkEyes = "Blink eyes";
        config.pageTitle = "Align and Tap";
        config.idBackside = "Backside of ID";
        config.faceFailed = "Face failed!!!";
        config.eyesFailed = "Eyes failed!!!";

        return config;
    }

    public String getCameraPermissionRational() {
        return cameraPermissionRational;
    }

    public void setCameraPermissionRational(String cameraPermissionRational) {
        this.cameraPermissionRational = cameraPermissionRational;
    }

    public void setIdBacksideInfo(String string) {
        this.idBacksideInfo = string;
    }

    public String getIdBacksideInfo() {
        return idBacksideInfo;
    }
}
