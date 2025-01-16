package com.verifie.android.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Objects;

public class IdentityVerificationActivity extends BaseActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_verification);
        setViewsData();
    }

    private void setViewsData() {
        setItemData(findViewById(R.id.passport_layout_item), getString(R.string.passport), R.drawable.ic_passport);
//        setItemData(findViewById(R.id.driving_license_layout_item), getString(R.string.driving_license), R.drawable.ic_driving_license);
        setItemData(findViewById(R.id.national_id_layout_item), getString(R.string.national_identity_card), R.drawable.ic_national_identity_card);
//        setItemData(findViewById(R.id.residence_permit_layout_item), getString(R.string.residence_permit_card), R.drawable.ic_residence_permit_card);
        setItemData(findViewById(R.id.liveness_check_layout_item), getString(R.string.check_liveness), R.drawable.ic_liveness);
    }

    private void setItemData(View docItem, String title, @DrawableRes int resourceId) {
        docItem.setOnClickListener(this);
        ((TextView) docItem.findViewById(R.id.txt_doc_name)).setText(title);
        ((ImageView) docItem.findViewById(R.id.img_doc_icon)).setImageResource(resourceId);
        if (docItem.getId() != R.id.passport_layout_item) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) docItem.findViewById(R.id.divider_line).getLayoutParams();
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
        }
    }

    @Override
    public void onClick(View v) {
        int docType = -1;

        if (Objects.equals(v.getId(), R.id.passport_layout_item)) {
            docType = Constants.DocTypes.PASSPORT;
        } else if (Objects.equals(v.getId(), R.id.national_id_layout_item)) {
            docType = Constants.DocTypes.NATIONAL_ID;
        } else if (Objects.equals(v.getId(), R.id.liveness_check_layout_item)) {
            docType = Constants.DocTypes.LIVENESS_CHECK;
        }

        if (docType != -1) {
            openScannerWithId(docType);
        }
    }

    private void openScannerWithId(int docType) {
        Intent intent = new Intent(this, ScanDocumentActivity.class);
        intent.putExtra(Constants.DocTypes.KEY, docType);
        startActivity(intent);
    }
}
