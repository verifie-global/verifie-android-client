package com.verifie.android;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

public class VerifieTextConfig implements Parcelable {

    //Current page title - Passport, ID Card
    private String pageTitle;

    //Page Info - Id card face page or Passport Face Page - (Bold text under the rectangle)
    private String pageInfo;

    //Scan instruction - Place your id card in the rectangle
    private String scanInfo;


    // Id card backside page info - Backside of ID - (Bold text under the rectangle)
    private String idBackside;

    //Scan instruction - Place your id card in the rectangle
    private String idBacksideInfo;

    //Face scaning
    private String movePhoneCloser; // When user face is too far
    private String movePhoneAway; // When user face is too close
    private String holdStillText; // When user should freeze head until scan will be completed
    private String positionFaceInOval; //When user face is out of the oval borders-
    private String scannedText; //When scan is completed

    //Recommendations layout (face scan recommendations)
    private String recommendationsTitleText; //Page title - Recommendations
    private String lightUpFaceText; //Bold text - Info - Light up face evenly
    private String greatText; // First Item - Everything is ok
    private String noGlassesText; // Second Item - Should not wear glasses
    private String noShadowText; // Third Item - Try to keep your face in front of light power(To suffer shadows)
    private String noFlashLightText; // Forth Item - Try not to use flash light at back, left or right side
    private String continueText; // Continue - skips recommendations layout

    private String cameraPermissionRational; // When user denies camera permission for the first time,
    // Second time when requesting a camera permission show this test to describe it is necessary


    public VerifieTextConfig(Context context) {
        Resources resources = context.getResources();
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
        pageTitle = in.readString();
        pageInfo = in.readString();
        scanInfo = in.readString();
        idBackside = in.readString();
        idBacksideInfo = in.readString();
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
        dest.writeString(pageTitle);
        dest.writeString(pageInfo);
        dest.writeString(scanInfo);
        dest.writeString(idBackside);
        dest.writeString(idBacksideInfo);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerifieTextConfig that = (VerifieTextConfig) o;

        if (movePhoneCloser != null ? !movePhoneCloser.equals(that.movePhoneCloser) : that.movePhoneCloser != null)
            return false;
        if (movePhoneAway != null ? !movePhoneAway.equals(that.movePhoneAway) : that.movePhoneAway != null)
            return false;
        if (pageTitle != null ? !pageTitle.equals(that.pageTitle) : that.pageTitle != null)
            return false;
        return idBackside != null ? !idBackside.equals(that.idBackside) : that.idBackside != null;
    }

    @Override
    public int hashCode() {
        int result = movePhoneCloser != null ? movePhoneCloser.hashCode() : 0;
        result = 31 * result + (movePhoneAway != null ? movePhoneAway.hashCode() : 0);
        result = 31 * result + (pageTitle != null ? pageTitle.hashCode() : 0);
        result = 31 * result + (idBackside != null ? idBackside.hashCode() : 0);
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
                ", pageTitle='" + pageTitle + '\'' +
                ", pageInfo='" + pageInfo + '\'' +
                ", scanInfo='" + scanInfo + '\'' +
                ", idBackside='" + idBackside + '\'' +
                ", idBacksideInfo='" + idBacksideInfo + '\'' +
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

    public static VerifieTextConfig defaultConfig(Context context) {
        VerifieTextConfig config = new VerifieTextConfig(context);
        config.pageTitle = "Align and Tap";
        config.idBackside = "Backside of ID";

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
