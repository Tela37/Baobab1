package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.StudyQuestion;
import td.teladoum.baobab.utils.ImageUtils;

public class StudyQuestionAdapter extends RecyclerView.Adapter<StudyQuestionAdapter.ViewHolder> {

    private final List<StudyQuestion> questionList;
    private final OnQuestionClickListener listener;

    public interface OnQuestionClickListener {
        void onQuestionClick(StudyQuestion question);
    }

    public StudyQuestionAdapter(List<StudyQuestion> questionList, OnQuestionClickListener listener) {
        this.questionList = questionList;
        this.listener = listener;
    }

    public void updateData(List<StudyQuestion> newList) {
        if (newList == null) return;
        questionList.clear();
        questionList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_study_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudyQuestion q = questionList.get(position);

        holder.tvAuthorName.setText(q.getAuthorName() != null ? q.getAuthorName() : "Élève");
        holder.tvQuestionTime.setText(formatTime(q.getCreatedAt()));
        holder.tvQuestionCategory.setText(q.getCategory() != null ? q.getCategory() : "Général");
        holder.tvQuestionTitle.setText(q.getTitle());
        holder.tvQuestionContent.setText(q.getContent());

        boolean isVideo = isVideoPath(q.getImagePath());
        boolean isPdf = isPdfPath(q.getImagePath());

        if (q.getImagePath() != null && !q.getImagePath().trim().isEmpty()) {
            if (isVideo) {
                holder.imgQuestionImage.setVisibility(View.GONE);
                if (holder.layoutQuestionPdf != null) holder.layoutQuestionPdf.setVisibility(View.GONE);
                if (holder.layoutQuestionVideo != null) {
                    holder.layoutQuestionVideo.setVisibility(View.VISIBLE);
                    ImageUtils.loadVideoThumbnail(holder.itemView.getContext(), q.getImagePath(), holder.imgQuestionVideoThumbnail);
                    View.OnClickListener videoClick = v -> {
                        android.content.Intent intent = new android.content.Intent(v.getContext(), td.teladoum.baobab.activities.VideoPlayerActivity.class);
                        intent.putExtra("video_url", q.getImagePath());
                        intent.putExtra("video_title", q.getTitle());
                        v.getContext().startActivity(intent);
                    };
                    holder.layoutQuestionVideo.setOnClickListener(videoClick);
                    if (holder.ibPlayQuestionVideo != null) holder.ibPlayQuestionVideo.setOnClickListener(videoClick);
                }
            } else if (isPdf) {
                holder.imgQuestionImage.setVisibility(View.GONE);
                if (holder.layoutQuestionVideo != null) holder.layoutQuestionVideo.setVisibility(View.GONE);
                if (holder.layoutQuestionPdf != null) {
                    holder.layoutQuestionPdf.setVisibility(View.VISIBLE);
                    if (holder.tvPdfFileName != null) holder.tvPdfFileName.setText(q.getImagePath());
                    View.OnClickListener pdfClick = v -> {
                        android.content.Intent intent = new android.content.Intent(v.getContext(), td.teladoum.baobab.ui.learning.PdfViewerActivity.class);
                        intent.putExtra("pdf_path", q.getImagePath());
                        intent.putExtra("pdf_title", q.getTitle());
                        v.getContext().startActivity(intent);
                    };
                    holder.layoutQuestionPdf.setOnClickListener(pdfClick);
                    if (holder.btnOpenPdfPreview != null) holder.btnOpenPdfPreview.setOnClickListener(pdfClick);
                }
            } else {
                if (holder.layoutQuestionVideo != null) holder.layoutQuestionVideo.setVisibility(View.GONE);
                if (holder.layoutQuestionPdf != null) holder.layoutQuestionPdf.setVisibility(View.GONE);
                holder.imgQuestionImage.setVisibility(View.VISIBLE);
                ImageUtils.loadFullImage(holder.itemView.getContext(), q.getImagePath(), holder.imgQuestionImage);
            }
        } else {
            holder.imgQuestionImage.setVisibility(View.GONE);
            if (holder.layoutQuestionVideo != null) holder.layoutQuestionVideo.setVisibility(View.GONE);
            if (holder.layoutQuestionPdf != null) holder.layoutQuestionPdf.setVisibility(View.GONE);
        }

        ImageUtils.loadProfileImage(holder.itemView.getContext(), q.getAuthorProfileImage(), holder.imgAuthorAvatar);

        holder.tvRepliesCount.setText("💬 " + q.getRepliesCount() + " réponses d'aide");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuestionClick(q);
            }
        });
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    private boolean isVideoPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") ||
                lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    private boolean isPdfPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".pdf") || lower.contains("pdf");
    }

    private String formatTime(String dateTime) {
        try {
            if (dateTime == null || dateTime.trim().isEmpty()) return "À l'instant";
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            if (date == null) return "À l'instant";
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM à HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "À l'instant";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAuthorAvatar, imgQuestionImage, imgQuestionVideoThumbnail, ibPlayQuestionVideo;
        View layoutQuestionVideo, layoutQuestionPdf;
        TextView tvAuthorName, tvQuestionTime, tvQuestionCategory, tvQuestionTitle, tvQuestionContent, tvRepliesCount, tvPdfFileName;
        android.widget.Button btnOpenPdfPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAuthorAvatar = itemView.findViewById(R.id.imgAuthorAvatar);
            imgQuestionImage = itemView.findViewById(R.id.imgQuestionImage);
            layoutQuestionVideo = itemView.findViewById(R.id.layoutQuestionVideo);
            imgQuestionVideoThumbnail = itemView.findViewById(R.id.imgQuestionVideoThumbnail);
            ibPlayQuestionVideo = itemView.findViewById(R.id.ibPlayQuestionVideo);
            layoutQuestionPdf = itemView.findViewById(R.id.layoutQuestionPdf);
            tvPdfFileName = itemView.findViewById(R.id.tvPdfFileName);
            btnOpenPdfPreview = itemView.findViewById(R.id.btnOpenPdfPreview);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvQuestionTime = itemView.findViewById(R.id.tvQuestionTime);
            tvQuestionCategory = itemView.findViewById(R.id.tvQuestionCategory);
            tvQuestionTitle = itemView.findViewById(R.id.tvQuestionTitle);
            tvQuestionContent = itemView.findViewById(R.id.tvQuestionContent);
            tvRepliesCount = itemView.findViewById(R.id.tvRepliesCount);
        }
    }
}
