package com.verifie.android.api.model.res;

import com.google.gson.annotations.SerializedName;

public class Score {

    @SerializedName("facialScore")
    private float facialScore;

    @SerializedName("facialLiveness")
    private boolean facialLiveness;

    @SerializedName("predictedGender")
    private String predictedGender;

    @SerializedName("predictedAge")
    private String predictedAge;

    @SerializedName("isMatched")
    private boolean isMatched;

    @SerializedName("livenessScore")
    private float livenessScore;

    private String base64Image;

    public float getFacialScore() {
        return facialScore;
    }

    public void setFacialScore(float facialScore) {
        this.facialScore = facialScore;
    }

    public boolean isFacialLiveness() {
        return facialLiveness;
    }

    public void setFacialLiveness(boolean facialLiveness) {
        this.facialLiveness = facialLiveness;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getPredictedGender() {
        return predictedGender;
    }

    public void setPredictedGender(String predictedGender) {
        this.predictedGender = predictedGender;
    }

    public String getPredictedAge() {
        return predictedAge;
    }

    public void setPredictedAge(String predictedAge) {
        this.predictedAge = predictedAge;
    }

    public boolean isMatched() {
        return isMatched;
    }

    public void setMatched(boolean matched) {
        isMatched = matched;
    }

    public float getLivenessScore() {
        return livenessScore;
    }

    public void setLivenessScore(float livenessScore) {
        this.livenessScore = livenessScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Score score = (Score) o;

        if (Float.compare(score.facialScore, facialScore) != 0) return false;
        return facialLiveness == score.facialLiveness;
    }

    @Override
    public int hashCode() {
        int result = (facialScore != +0.0f ? Float.floatToIntBits(facialScore) : 0);
        result = 31 * result + (facialLiveness ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Score{" +
                "facialScore=" + facialScore +
                ", facialLiveness=" + facialLiveness +
                ", predictedGender='" + predictedGender + '\'' +
                ", predictedAge='" + predictedAge + '\'' +
                ", isMatched=" + isMatched +
                ", base64Image='" + base64Image + '\'' +
                '}';
    }
}
