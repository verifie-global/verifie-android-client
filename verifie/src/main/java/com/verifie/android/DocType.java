package com.verifie.android;

public enum DocType {
    DOC_TYPE_ID_CARD("idCard"),
    DOC_TYPE_PASSPORT("passport"),
    DOC_TYPE_RESIDENCE_PERMIT("permitCard");

    private String name;

    public String getName() {
        return name;
    }

    DocType(String name) {
        this.name = name;
    }
}
