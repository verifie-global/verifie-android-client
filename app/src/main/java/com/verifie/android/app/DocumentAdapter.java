package com.verifie.android.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.verifie.android.api.model.res.Document;
import com.verifie.android.api.model.res.Score;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ITEM_TYPE_SCORE = 1;
    private static final int ITEM_TYPE_DOCUMENT = 2;

    private LayoutInflater inflater;

    private Score score;
    private List<Document> documents;

    public DocumentAdapter(Context context, Score score, List<Document> documents) {
        this.score = score;
        this.documents = documents;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && score != null) {
            return ITEM_TYPE_SCORE;
        }

        return ITEM_TYPE_DOCUMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return viewType == ITEM_TYPE_SCORE
                ? new ScoreViewHolder(inflater.inflate(R.layout.score_item, viewGroup, false))
                : new DocumentViewHolder(inflater.inflate(R.layout.document_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if (getItemViewType(i) == ITEM_TYPE_SCORE) {
            ((ScoreViewHolder) viewHolder).bind(score);
        } else {
            ((DocumentViewHolder) viewHolder).bind(documents.get(i - 1));
        }
    }

    @Override
    public int getItemCount() {
        return score == null ? documents.size() : documents.size() + 1;
    }

    public class ScoreViewHolder extends RecyclerView.ViewHolder {

        private TextView faceScore;
        private TextView facialLiveness;

        public ScoreViewHolder(@NonNull View itemView) {
            super(itemView);

            faceScore = itemView.findViewById(R.id.face_score);
            facialLiveness = itemView.findViewById(R.id.facial_liveness);
        }

        public void bind(Score score) {
            faceScore.setText("Face Score: " + score.getFacialScore());
            facialLiveness.setText("Liveness: " + score.isFacialLiveness());
        }
    }

    public class DocumentViewHolder extends RecyclerView.ViewHolder {

        private ImageView documentImage;
        private TextView documenttType;
        private TextView documentNumber;
        private TextView birthDate;
        private TextView expiryDate;
        private TextView firstName;
        private TextView lastName;
        private TextView gender;
        private TextView nationality;
        private TextView country;
        private TextView documentValid;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);

            documentImage = itemView.findViewById(R.id.document_image);
            documenttType = itemView.findViewById(R.id.document_type);
            documentNumber = itemView.findViewById(R.id.document_number);
            birthDate = itemView.findViewById(R.id.birth_day);
            expiryDate = itemView.findViewById(R.id.expiry_day);
            firstName = itemView.findViewById(R.id.firstname);
            lastName = itemView.findViewById(R.id.lastname);
            gender = itemView.findViewById(R.id.gender);
            nationality = itemView.findViewById(R.id.nationality);
            country = itemView.findViewById(R.id.country);
            documentValid = itemView.findViewById(R.id.document_valid);
        }

        public void bind(Document document) {
            if (document.getDocumentFaceImage() != null && !document.getDocumentFaceImage().isEmpty()) {
                byte[] decodedImage = Base64.decode(document.getDocumentFaceImage(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);

                documentImage.setImageBitmap(bitmap);
                documentImage.setVisibility(View.VISIBLE);
            } else {
                documentImage.setVisibility(View.GONE);
            }

            documenttType.setText("Document type: " + document.getDocumentType());
            documentNumber.setText("Document number: " + document.getDocumentNumber());
            birthDate.setText("Birth date: " + document.getBirthDate());
            expiryDate.setText("Expiry date: " + document.getExpiryDate());
            firstName.setText("First name: " + document.getFirstname());
            lastName.setText("Last name: " + document.getLastname());
            gender.setText("Gender: " + document.getGender());
            nationality.setText("Nationality: " + document.getNationality());
            country.setText("Country: " + document.getCountry());
            documentValid.setText("Document valid: " + String.valueOf(document.isDocumentValid()));
        }
    }
}
