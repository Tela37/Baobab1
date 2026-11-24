package td.teladoum.baobab.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import td.teladoum.baobab.helpers.DialogUtils;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import td.teladoum.baobab.R;
import td.teladoum.baobab.activities.User2PageActivity;
import td.teladoum.baobab.models.Comment;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> commentList;
    private final OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onReplyClick(Comment comment);
        void onLikeClick(Comment comment);
        void onReactionClick(Comment comment, String reactionType);
        void onUserClick(Comment comment);
        void onReportClick(Comment comment);
    }

    public CommentAdapter(List<Comment> commentList, OnCommentClickListener listener) {
        this.commentList = commentList;
        this.listener = listener;
    }

    public void updateComments(List<Comment> newComments) {
        if (newComments == null) return;
        if (newComments == this.commentList) {
            notifyDataSetChanged();
            return;
        }
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new CommentDiffCallback(this.commentList, newComments));
        this.commentList.clear();
        this.commentList.addAll(newComments);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class CommentDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<Comment> oldList;
        private final List<Comment> newList;

        public CommentDiffCallback(List<Comment> oldList, List<Comment> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Comment oldItem = oldList.get(oldItemPosition);
            Comment newItem = newList.get(newItemPosition);
            return oldItem != null && newItem != null && oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Comment oldItem = oldList.get(oldItemPosition);
            Comment newItem = newList.get(newItemPosition);
            if (oldItem == null || newItem == null) return false;

            return oldItem.getLikesCount() == newItem.getLikesCount()
                    && java.util.Objects.equals(oldItem.getContent(), newItem.getContent())
                    && java.util.Objects.equals(oldItem.getUserReactionType(), newItem.getUserReactionType());
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        UserRepository uRepo = new UserRepository(holder.itemView.getContext());
        User commentAuthor = uRepo.getUserById(comment.getUserId());
        String authorName = comment.getAuthorName() != null ? comment.getAuthorName() : "Utilisateur";
        if (commentAuthor != null && commentAuthor.isVerified()) {
            authorName = authorName + " 🔵";
        }
        holder.tvCommentAuthorName.setText(authorName);
        if (comment.getContent() != null && !comment.getContent().trim().isEmpty()) {
            holder.tvCommentContent.setVisibility(View.VISIBLE);
            holder.tvCommentContent.setText(formatCommentContent(comment.getContent(), holder.itemView.getContext()));
            holder.tvCommentContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        } else {
            holder.tvCommentContent.setVisibility(View.GONE);
        }

        if (comment.getImagePath() != null && !comment.getImagePath().trim().isEmpty()) {
            holder.imgCommentImage.setVisibility(View.VISIBLE);
            ImageUtils.loadFullImage(holder.itemView.getContext(), comment.getImagePath(), holder.imgCommentImage);
            holder.imgCommentImage.setOnClickListener(v -> showImagePreviewDialog(holder.itemView.getContext(), comment.getImagePath()));
        } else {
            holder.imgCommentImage.setVisibility(View.GONE);
            holder.imgCommentImage.setOnClickListener(null);
        }

        holder.tvCommentTime.setText(formatTime(comment.getCreatedAt()));

        ImageUtils.loadProfileImage(holder.itemView.getContext(), comment.getAuthorProfileImage(), holder.imgCommentAuthorAvatar);

        // Sub-comment indentation
        if (comment.getParentId() != null) {
            holder.layoutCommentContainer.setPadding(100, 8, 16, 8);
        } else {
            holder.layoutCommentContainer.setPadding(16, 8, 16, 8);
        }

        holder.tvCommentLikesCount.setText(String.valueOf(comment.getLikesCount()));

        String reaction = comment.getUserReactionType();
        if ("LOVE".equalsIgnoreCase(reaction)) {
            holder.tvLikeComment.setText("❤️ J'adore");
            holder.tvLikeComment.setTextColor(Color.RED);
        } else if ("HAHA".equalsIgnoreCase(reaction)) {
            holder.tvLikeComment.setText("😆 Haha");
            holder.tvLikeComment.setTextColor(Color.parseColor("#FF9800"));
        } else if ("WOW".equalsIgnoreCase(reaction)) {
            holder.tvLikeComment.setText("😮 Wouah");
            holder.tvLikeComment.setTextColor(Color.parseColor("#2196F3"));
        } else if ("SAD".equalsIgnoreCase(reaction)) {
            holder.tvLikeComment.setText("😢 Triste");
            holder.tvLikeComment.setTextColor(Color.parseColor("#9C27B0"));
        } else if ("ANGRY".equalsIgnoreCase(reaction)) {
            holder.tvLikeComment.setText("😡 En colère");
            holder.tvLikeComment.setTextColor(Color.RED);
        } else if ("LIKE".equalsIgnoreCase(reaction) || comment.isLikedByCurrentUser()) {
            holder.tvLikeComment.setText("👍 J'aime");
            holder.tvLikeComment.setTextColor(Color.parseColor("#2196F3"));
        } else {
            holder.tvLikeComment.setText("🤍 J'aime");
            holder.tvLikeComment.setTextColor(Color.GRAY);
        }

        holder.tvLikeComment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(comment);
            }
        });

        holder.tvLikeComment.setOnLongClickListener(v -> {
            showReactionPopup(holder.itemView.getContext(), comment);
            return true;
        });

        if (holder.tvReportComment != null) {
            holder.tvReportComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(comment);
                }
            });
        }

        holder.tvReplyComment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyClick(comment);
            }
        });

        View.OnClickListener userClickListener = v -> {
            if (listener != null) {
                listener.onUserClick(comment);
            } else {
                td.teladoum.baobab.utils.NavigationHelper.openUserProfile(v.getContext(), comment.getUserId());
            }
        };

        holder.imgCommentAuthorAvatar.setOnClickListener(userClickListener);
        holder.tvCommentAuthorName.setOnClickListener(userClickListener);
        holder.tvCommentLikesCount.setOnClickListener(userClickListener);
    }

    private void showReactionPopup(android.content.Context context, Comment comment) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_reaction_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnReactLike).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "LIKE");
            DialogUtils.safeDismiss(dialog);
        });
        dialogView.findViewById(R.id.btnReactLove).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "LOVE");
            DialogUtils.safeDismiss(dialog);
        });
        dialogView.findViewById(R.id.btnReactCare).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "CARE");
            DialogUtils.safeDismiss(dialog);
        });
        dialogView.findViewById(R.id.btnReactHaha).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "HAHA");
            DialogUtils.safeDismiss(dialog);
        });
        dialogView.findViewById(R.id.btnReactWow).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "WOW");
            DialogUtils.safeDismiss(dialog);
        });
        dialogView.findViewById(R.id.btnReactSad).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "SAD");
            DialogUtils.safeDismiss(dialog);
        });
        dialogView.findViewById(R.id.btnReactAngry).setOnClickListener(v -> {
            if (listener != null) listener.onReactionClick(comment, "ANGRY");
            DialogUtils.safeDismiss(dialog);
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    private String formatTime(String dateTime) {
        try {
            if (dateTime == null || dateTime.trim().isEmpty()) {
                return "À l'instant";
            }
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            if (date == null) return "À l'instant";

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM à HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "À l'instant";
        }
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        LinearLayout layoutCommentContainer;
        ImageView imgCommentAuthorAvatar;
        ImageView imgCommentImage;
        TextView tvCommentAuthorName;
        TextView tvCommentContent;
        TextView tvCommentTime;
        TextView tvReplyComment;
        TextView tvLikeComment;
        TextView tvCommentLikesCount;
        TextView tvReportComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutCommentContainer = itemView.findViewById(R.id.layoutCommentContainer);
            imgCommentAuthorAvatar = itemView.findViewById(R.id.imgCommentAuthorAvatar);
            imgCommentImage = itemView.findViewById(R.id.imgCommentImage);
            tvCommentAuthorName = itemView.findViewById(R.id.tvCommentAuthorName);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvReplyComment = itemView.findViewById(R.id.tvReplyComment);
            tvLikeComment = itemView.findViewById(R.id.tvLikeComment);
            tvCommentLikesCount = itemView.findViewById(R.id.tvCommentLikesCount);
            tvReportComment = itemView.findViewById(R.id.tvReportComment);
        }
    }

    private void showImagePreviewDialog(android.content.Context context, String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null);
        ImageView imgEnlarged = dialogView.findViewById(R.id.imgEnlarged);
        android.widget.ImageButton ibClose = dialogView.findViewById(R.id.ibClosePreview);
        android.widget.ImageButton ibDownload = dialogView.findViewById(R.id.ibDownloadPreview);
        if (ibDownload != null) {
            ibDownload.bringToFront();
            ibDownload.setOnClickListener(v -> ImageUtils.downloadImageToGallery(context, imagePath));
        }
        View layoutPreviewBottom = dialogView.findViewById(R.id.layoutPreviewBottom);
        if (layoutPreviewBottom != null) {
            layoutPreviewBottom.setVisibility(View.GONE);
        }

        ImageUtils.loadFullImage(context, imagePath, imgEnlarged);
        if (imgEnlarged != null) {
            ImageUtils.enablePinchToZoom(imgEnlarged);
        }

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (ibClose != null) {
            ibClose.bringToFront();
            ibClose.setOnClickListener(v -> DialogUtils.safeDismiss(dialog));
        }

        dialog.show();
    }

    private android.text.SpannableString formatCommentContent(String content, android.content.Context context) {
        if (content == null) return new android.text.SpannableString("");
        android.text.SpannableString spannable = new android.text.SpannableString(content);

        UserRepository uRepo = new UserRepository(context);
        List<User> allUsers = uRepo.getAllUsers();

        for (User u : allUsers) {
            String name = u.getName();
            if (name != null && !name.trim().isEmpty()) {
                int index = content.indexOf(name);
                while (index != -1) {
                    int end = index + name.length();
                    spannable.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#2196F3")), index, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), index, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            td.teladoum.baobab.utils.NavigationHelper.openUserProfile(context, u.getId());
                        }
                        @Override
                        public void updateDrawState(@NonNull android.text.TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setColor(Color.parseColor("#2196F3"));
                            ds.setUnderlineText(false);
                        }
                    }, index, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    index = content.indexOf(name, end);
                }
            }
        }
        return spannable;
    }
}
