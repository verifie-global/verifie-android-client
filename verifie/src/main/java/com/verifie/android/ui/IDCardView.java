package com.verifie.android.ui;

import android.view.View;

public interface IDCardView {

    //Return the view you want to ad
    View getViewToShow(ActionHandler actionHandler);


    interface ActionHandler {
        void closeIDCardLayout();
    }
}

