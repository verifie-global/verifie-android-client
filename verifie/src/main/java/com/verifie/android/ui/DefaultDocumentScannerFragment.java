package com.verifie.android.ui;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.verifie.android.R;
import com.verifie.android.VerifieColorConfig;
import com.verifie.android.VerifieConfig;
import com.verifie.android.VerifieTextConfig;

public final class DefaultDocumentScannerFragment extends BaseDocumentScannerFragment {

    private static final String TAG = "DefaultDocumentScannerFragment";

    private TextView txtTitle;
    private TextView txtPageInfo;
    private TextView txtScanInfo;
    private View cropperFrame;

    private View topArea;
    private View bottomArea;
    private View middleLeftArea;
    private View middleRightArea;

    private FrameLayout progressBarHolder;
    private View recommendationsLayout;

    public boolean isLoading() {
        return progressBarHolder.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_document_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_capture).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                scanDocument();
            }
        });

        view.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        txtTitle = view.findViewById(R.id.title);
        txtPageInfo = view.findViewById(R.id.txt_page_info);
        txtScanInfo = view.findViewById(R.id.txt_scan_info);
        cropperFrame = view.findViewById(R.id.cropper_frame);

        topArea = view.findViewById(R.id.top_area);
        bottomArea = view.findViewById(R.id.bottom_area);
        middleLeftArea = view.findViewById(R.id.middle_left_area);
        middleRightArea = view.findViewById(R.id.middle_right_area);

        progressBarHolder = view.findViewById(R.id.progress_bar_holder);
        recommendationsLayout = view.findViewById(R.id.layout_recommendations_container);
    }

    private void showProgress() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBarHolder.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void hideProgress() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBarHolder.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public Rect getCropFrame(Rect preview) {
        return new Rect((int) cropperFrame.getX(), (int) cropperFrame.getY(), cropperFrame.getWidth(), cropperFrame.getHeight());
    }

    @Override
    public void onCropFrameSet(Rect preview, Rect cropFrame) {
        VerifieTextConfig textConfig = config.getTextConfig();
        VerifieColorConfig colorConfig = config.getColorConfig();

        txtTitle.setText(textConfig.getAlignTap());
        txtTitle.setTextColor(colorConfig.getDocCropperFrameColor());
        txtPageInfo.setText(textConfig.getPageInfo());
        txtScanInfo.setText(textConfig.getScanInfo());

        int topAreaHeight = (preview.bottom - cropFrame.bottom) / 2;
        int middleAreaWidth = (preview.right - cropFrame.right) / 2;

        LinearLayout.LayoutParams topAreaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topAreaHeight);
        LinearLayout.LayoutParams bottomAreaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, topAreaHeight);
        RelativeLayout.LayoutParams middleLeftParams = (RelativeLayout.LayoutParams) middleLeftArea.getLayoutParams();
        middleLeftParams.width = middleAreaWidth;
        middleLeftParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        RelativeLayout.LayoutParams middleRightParams = (RelativeLayout.LayoutParams) middleRightArea.getLayoutParams();
        middleRightParams.width = middleAreaWidth;
        middleRightParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        topArea.setLayoutParams(topAreaParams);
        bottomArea.setLayoutParams(bottomAreaParams);
        middleLeftArea.setLayoutParams(middleLeftParams);
        middleRightArea.setLayoutParams(middleRightParams);
    }

    @Override
    public void onDocumentScanStarted() {
        showProgress();
    }

    @Override
    public void onDocumentScanError(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Please take a document photo again.";
        }
        hideProgress();
        hideCapturedImage();
        new AlertDialog.Builder(getActivity())
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDocumentScanFinished(boolean nextPageRequired) {
        if (nextPageRequired) {
            txtPageInfo.setText(config.getTextConfig().getIdBackside());
            txtScanInfo.setText(config.getTextConfig().getIdBacksideInfo());
        } else {
            showRecommendationsLayout();
        }
        hideProgress();
    }

    private void showRecommendationsLayout() {
        if (getView() != null) {
            setRecommendationItemData(getView().findViewById(R.id.recommendation_great), getString(R.string.great), R.drawable.ic_boy_great, R.drawable.ic_success);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_glasses), getString(R.string.no_glasses), R.drawable.ic_boy_glasses, R.drawable.ic_error);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_shadow), getString(R.string.no_shadow), R.drawable.ic_boy_shadow, R.drawable.ic_error);
            setRecommendationItemData(getView().findViewById(R.id.recommendation_no_flash), getString(R.string.no_flash), R.drawable.ic_boy_flash, R.drawable.ic_error);
            ((TextView) getView().findViewById(R.id.title_recommendation)).setText(getString(R.string.recommendations));
            getView().findViewById(R.id.doc_scan_layout).setVisibility(View.INVISIBLE);
            getView().findViewById(R.id.btn_continue).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFaceDetectorActivity();
                }
            });
            getView().findViewById(R.id.btn_back_recommend).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            });
            recommendationsLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setRecommendationItemData(View itemView, String titleRes, int iconRes, int statusIconRes) {
        ((TextView) itemView.findViewById(R.id.txt_recommend_text)).setText(titleRes);
        ((ImageView) itemView.findViewById(R.id.person_icon)).setImageResource(iconRes);
        ((ImageView) itemView.findViewById(R.id.icon_recommendation)).setImageResource(statusIconRes);
    }

    static DefaultDocumentScannerFragment newInstance(VerifieConfig config) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);

        DefaultDocumentScannerFragment fragment = new DefaultDocumentScannerFragment();
        fragment.setArguments(args);

        return fragment;
    }
}
