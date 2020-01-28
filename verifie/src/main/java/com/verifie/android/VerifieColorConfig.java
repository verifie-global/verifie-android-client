package com.verifie.android;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class VerifieColorConfig implements Parcelable {

    private int docCropperFrameColor;

    public VerifieColorConfig() {
    }

    protected VerifieColorConfig(Parcel in) {
        this.docCropperFrameColor = in.readInt();
    }

    public int getDocCropperFrameColor() {
        return docCropperFrameColor;
    }

    public void setDocCropperFrameColor(int docCropperFrameColor) {
        this.docCropperFrameColor = docCropperFrameColor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerifieColorConfig that = (VerifieColorConfig) o;

        return docCropperFrameColor == that.docCropperFrameColor;
    }

    @Override
    public int hashCode() {
        return docCropperFrameColor;
    }

    @Override
    public String toString() {
        return "VerifieColorConfig{" +
                "docCropperFrameColor=" + docCropperFrameColor +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.docCropperFrameColor);
    }

    public static VerifieColorConfig defaultConfig() {
        VerifieColorConfig colorConfig = new VerifieColorConfig();
        colorConfig.docCropperFrameColor = Color.WHITE;

        return colorConfig;
    }

    public static final Parcelable.Creator<VerifieColorConfig> CREATOR = new Parcelable.Creator<VerifieColorConfig>() {
        @Override
        public VerifieColorConfig createFromParcel(Parcel source) {
            return new VerifieColorConfig(source);
        }

        @Override
        public VerifieColorConfig[] newArray(int size) {
            return new VerifieColorConfig[size];
        }
    };
}
