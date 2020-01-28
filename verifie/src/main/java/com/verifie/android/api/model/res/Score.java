package com.verifie.android.api.model.res;

import com.google.gson.annotations.SerializedName;

public class Score {

    @SerializedName("facialScore")
    private float facialScore;

    @SerializedName("facialLiveness")
    private boolean facialLiveness;

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
                '}';
    }
}
