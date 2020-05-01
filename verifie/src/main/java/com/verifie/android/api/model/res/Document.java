package com.verifie.android.api.model.res;

import com.google.gson.annotations.SerializedName;

public class Document {

    @SerializedName("documentType")
    private String documentType;

    @SerializedName("documentNumber")
    private String documentNumber;

    @SerializedName("birthDate")
    private String birthDate;

    @SerializedName("expiryDate")
    private String expiryDate;

    @SerializedName("firstname")
    private String firstname;

    @SerializedName("lastname")
    private String lastname;

    @SerializedName("gender")
    private String gender;

    @SerializedName("nationality")
    private String nationality;

    @SerializedName("country")
    private String country;

    @SerializedName("documentImage")
    private String documentImage;

    @SerializedName("documentFaceImage")
    private String documentFaceImage;

    @SerializedName("documentValid")
    private boolean documentValid;

    @SerializedName("nextPage")
    private boolean nextPage;

    private String error;

    public String getDocumentType() {
        return capitalize(documentType);
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return capitalize(documentNumber);
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getBirthDate() {
        return getFormattedDate(birthDate);
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getExpiryDate() {
        return getFormattedDate(expiryDate);
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getGender() {
        if (gender == null) return "";
        return gender.toLowerCase().equals("f") ? "Female" : "Male";
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDocumentImage() {
        return documentImage;
    }

    public void setDocumentImage(String documentImage) {
        this.documentImage = documentImage;
    }

    public String getDocumentFaceImage() {
        return documentFaceImage;
    }

    public void setDocumentFaceImage(String documentFaceImage) {
        this.documentFaceImage = documentFaceImage;
    }

    public boolean isDocumentValid() {
        return documentValid;
    }

    public void setDocumentValid(boolean documentValid) {
        this.documentValid = documentValid;
    }

    public boolean isNextPage() {
        return nextPage;
    }

    public void setNextPage(boolean nextPage) {
        this.nextPage = nextPage;
    }

    @Override
    public String toString() {
        return "Document{" +
                "documentType='" + documentType + '\'' +
                ", documentNumber='" + documentNumber + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", gender='" + gender + '\'' +
                ", nationality='" + nationality + '\'' +
                ", country='" + country + '\'' +
                ", documentImage='" + documentImage + '\'' +
                ", documentFaceImage='" + documentFaceImage + '\'' +
                ", documentValid=" + documentValid +
                ", nextPage=" + nextPage +
                '}';
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getFormattedDate(String unformatted) {
        if (unformatted == null) {
            return "";
        }
        if (unformatted.contains("-")) {
            return unformatted.replace("-", "/");
        }
        return unformatted;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
