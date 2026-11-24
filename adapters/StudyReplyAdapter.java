package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.StudyReply;
import td.teladoum.baobab.utils.ImageUtils;

public class StudyReplyAdapter extends RecyclerView.Adapter<StudyReplyAdapter.ViewHolder> {

    private final List<StudyReply> replyList;
    private final int currentUserId;
    private final OnReplyActionListener listener;

    public interface OnReplyActionListener {
        void onEditReply(StudyReply reply);
        void onReportReply(StudyReply reply);
        void onLikeReply(StudyReply reply);
        void onReactionReply(StudyReply reply, String reactionType);
    }

    public StudyReplyAdapter(List<StudyReply> replyList, int currentUserId, OnReplyActionListener listener) {
        this.replyList = replyList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateData(List<StudyReply> newList) {
        if (newList == null) return;
        replyList.clear();
        replyList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudyReply r = replyList.get(position);

        holder.tvCommentAuthorName.setText(r.getAuthorName() != null ? r.getAuthorName() : "Élève / Prof");
        holder.tvCommentContent.setText(r.getContent());
        holder.tvCommentTime.setText(formatTime(r.getCreatedAt()));

        ImageUtils.loadProfileImage(holder.itemView.getContext(), r.getAuthorProfileImage(), holder.imgCommentAuthorAvatar);

        if (r.getImagePath() != null && !r.getImagePath().trim().isEmpty()) {
            if (holder.imgCommentImage != null) {
                holder.imgCommentImage.setVisibility(View.VISIBLE);
                boolean isVideo = r.getImagePath().toLowerCase().endsWith(".mp4") || r.getImagePath().toLowerCase().contains("youtube") || r.getImagePath().toLowerCase().endsWith(".mov") || r.getImagePath().toLowerCase().endsWith(".avi");
                if (isVideo) {
                    ImageUtils.loadVideoThumbnail(holder.itemView.getContext(), r.getImagePath(), holder.imgCommentImage);
                } else {
                    ImageUtils.loadStoryImage(holder.itemView.getContext(), r.getImagePath(), holder.imgCommentImage);
                }
                holder.imgCommentImage.setOnClickListener(v -> {
                    if (isVideo) {
                        android.content.Intent intent = new android.content.Intent(v.getContext(), td.teladoum.baobab.activities.VideoPlayerActivity.class);
                        intent.putExtra("video_url", r.getImagePath());
                        intent.putExtra("video_title", "Tutoriel vidéo");
                        v.getContext().startActivity(intent);
                    } else {
                        ImageUtils.loadFullImage(v.getContext(), r.getImagePath(), holder.imgCommentImage);
                    }
                });
            }
        } else {
            if (holder.imgCommentImage != null) holder.imgCommentImage.setVisibility(View.GONE);
        }

        if (holder.tvReplyComment != null) holder.tvReplyComment.setVisibility(View.GONE);

        if (holder.tvLikeComment != null) {
            holder.tvLikeComment.setVisibility(View.VISIBLE);
            String reaction = r.getUserReactionType();
            if ("LOVE".equalsIgnoreCase(reaction)) {
                holder.tvLikeComment.setText("❤️ J'adore");
                holder.tvLikeComment.setTextColor(android.graphics.Color.RED);
            } else if ("HAHA".equalsIgnoreCase(reaction)) {
                holder.tvLikeComment.setText("😆 Haha");
                holder.tvLikeComment.setTextColor(android.graphics.Color.parseColor("#FF9800"));
            } else if ("WOW".equalsIgnoreCase(reaction)) {
                holder.tvLikeComment.setText("😮 Wouah");
                holder.tvLikeComment.setTextColor(android.graphics.Color.parseColor("#2196F3"));
            } else if ("SAD".equalsIgnoreCase(reaction)) {
                holder.tvLikeComment.setText("😢 Triste");
                holder.tvLikeComment.setTextColor(android.graphics.Color.parseColor("#9C27B0"));
            } else if ("ANGRY".equalsIgnoreCase(reaction)) {
                holder.tvLikeComment.setText("😡 En colère");
                holder.tvLikeComment.setTextColor(android.graphics.Color.RED);
            } else if ("LIKE".equalsIgnoreCase(reaction) || r.isLikedByCurrentUser()) {
                holder.tvLikeComment.setText("❤️ J'aime");
                holder.tvLikeComment.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_like_red));
            } else {
                holder.tvLikeComment.setText("🤍 J'aime");
                holder.tvLikeComment.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.grey));
            }

            holder.tvLikeComment.setOnClickListener(v -> {
                if (listener != null) listener.onLikeReply(r);
            });
            holder.tvLikeComment.setOnLongClickListener(v -> {
                showReactionPopup(holder.itemView.getContext(), r);
                return true;
            });
        }

        if (holder.tvCommentLikesCount != null) {
            if (r.getLikesCount() > 0) {
                holder.tvCommentLikesCount.setVisibility(View.VISIBLE);
                holder.tvCommentLikesCount.setText(r.getLikesCount() + " j'aime");
            } else {
                holder.tvCommentLikesCount.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            boolean isAuthor = (r.getUserId() == currentUserId);
            String[] options = isAuthor ? new String[]{"✏️ Modifier la réponse", "🚩 Signaler (hors sujet / inapproprié)", "❌ Annuler"} : new String[]{"🚩 Signaler cette réponse (hors sujet / inappropriée)", "❌ Annuler"};

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Options de la réponse")
                    .setItems(options, (dialog, which) -> {
                        if (isAuthor) {
                            if (which == 0) {
                                if (listener != null) listener.onEditReply(r);
                            } else if (which == 1) {
                                if (listener != null) listener.onReportReply(r);
                            }
                        } else {
                            if (which == 0) {
                                if (listener != null) listener.onReportReply(r);
                            }
                        }
                    })
                    .show();
            return true;
        });
    }

    private void showReactionPopup(android.content.Context context, StudyReply reply) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnReactLike).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "LIKE");
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnReactLove).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "LOVE");
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnReactCare).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "CARE");
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnReactHaha).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "HAHA");
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnReactWow).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "WOW");
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnReactSad).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "SAD");
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnReactAngry).setOnClickListener(v -> {
            if (listener != null) listener.onReactionReply(reply, "ANGRY");
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return replyList.size();
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
        ImageView imgCommentAuthorAvatar, imgCommentImage;
        TextView tvCommentAuthorName, tvCommentContent, tvCommentTime, tvReplyComment, tvLikeComment, tvCommentLikesCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCommentAuthorAvatar = itemView.findViewById(R.id.imgCommentAuthorAvatar);
            imgCommentImage = itemView.findViewById(R.id.imgCommentImage);
            tvCommentAuthorName = itemView.findViewById(R.id.tvCommentAuthorName);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvReplyComment = itemView.findViewById(R.id.tvReplyComment);
            tvLikeComment = itemView.findViewById(R.id.tvLikeComment);
            tvCommentLikesCount = itemView.findViewById(R.id.tvCommentLikesCount);
        }
    }
}
