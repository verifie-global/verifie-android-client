package com.verifie.android.api.model.res;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ResponseModel<T> {

    @SerializedName("opCode")
    private int opCode;

    @SerializedName("opDesc")
    private String opDesc;

    @SerializedName("result")
    private T result;

    @SerializedName("validationModel")
    private List<ValidationModel> validationModels;

    public int getOpCode() {
        return opCode;
    }

    public void setOpCode(int opCode) {
        this.opCode = opCode;
    }

    public String getOpDesc() {
        return opDesc;
    }

    public void setOpDesc(String opDesc) {
        this.opDesc = opDesc;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public List<ValidationModel> getValidationModels() {
        return validationModels;
    }

    public void setValidationModels(List<ValidationModel> validationModels) {
        this.validationModels = validationModels;
    }

    @Override
    public String toString() {
        return "ResponseModel{" +
                "opCode=" + opCode +
                ", opDesc='" + opDesc + '\'' +
                ", result=" + result +
                ", validationModels=" + validationModels +
                '}';
    }
}
