package td.teladoum.baobab.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import td.teladoum.baobab.activities.AcceuilActivity;
import td.teladoum.baobab.activities.CommenterActivity;
import td.teladoum.baobab.activities.User2PageActivity;
import td.teladoum.baobab.helpers.GeminiHelper;
import td.teladoum.baobab.models.Post;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.TimeUtils;

/**
 * Adaptateur RecyclerView pour l'affichage des publications (posts) sur le fil d'actualité.
 * Gère les photos, vidéos, textes, j'aime, réactions émojis (❤️, 😂, 👍, etc.), commentaires, partages,
 * l'édition/suppression pour l'auteur, le zoom d'image et la lecture vidéo.
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<Post> postList;
    private final int currentUserId;
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onLikeClick(Post post);
        void onReactionClick(Post post, String reactionType);
        void onCommentClick(Post post);
        void onShareClick(Post post);
        void onReportClick(Post post);
        void onEditClick(Post post);
        void onDeleteClick(Post post);
        void onImageClick(String imagePath);
        void onImageClick(Post post);
        void onVideoClick(String videoPath);
    }

    public PostAdapter(List<Post> postList, int currentUserId, OnPostClickListener listener) {
        this.postList = postList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updatePosts(List<Post> newPosts) {
        if (newPosts == null) return;
        if (newPosts == this.postList) {
            notifyDataSetChanged();
            return;
        }
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new PostDiffCallback(this.postList, newPosts));
        this.postList.clear();
        this.postList.addAll(newPosts);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class PostDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<Post> oldList;
        private final List<Post> newList;

        public PostDiffCallback(List<Post> oldList, List<Post> newList) {
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
            Post oldPost = oldList.get(oldItemPosition);
            Post newPost = newList.get(newItemPosition);
            return oldPost != null && newPost != null && oldPost.getId() == newPost.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Post oldPost = oldList.get(oldItemPosition);
            Post newPost = newList.get(newItemPosition);
            if (oldPost == null || newPost == null) return false;

            return oldPost.getLikesCount() == newPost.getLikesCount()
                    && oldPost.isLikedByCurrentUser() == newPost.isLikedByCurrentUser()
                    && java.util.Objects.equals(oldPost.getContent(), newPost.getContent())
                    && java.util.Objects.equals(oldPost.getUserReactionType(), newPost.getUserReactionType());
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        String displayName = (post.getAuthorName() != null ? post.getAuthorName() : "Utilisateur");
        if (post.isPinned()) {
            displayName = "📌 " + displayName;
        }
        UserRepository userRepo = new UserRepository(holder.itemView.getContext());
        if (userRepo.getActiveRewardBadgesCount(post.getUserId()) > 0) {
            displayName += " 🏆";
        }
        User postAuthor = userRepo.getUserById(post.getUserId());
        if (postAuthor != null) {
            String badgeDisplay = td.teladoum.baobab.utils.BadgeManager.getBadgeDisplay(postAuthor.getBadge());
            if (badgeDisplay != null && !badgeDisplay.isEmpty()) {
                displayName += " (" + badgeDisplay + ")";
            }
            if (postAuthor.isVerified()) {
                displayName = displayName + " 🔵";
            }
        }
        holder.tvPostAuthorName.setText(displayName);
        holder.tvPostTime.setText(formatTime(post.getCreatedAt()));

        ImageUtils.loadProfileImage(holder.itemView.getContext(), post.getAuthorProfileImage(), holder.imgPostAuthorAvatar);
        View.OnClickListener authorClickListener = v -> {
            td.teladoum.baobab.utils.NavigationHelper.openUserProfile(v.getContext(), post.getUserId());
        };
        holder.imgPostAuthorAvatar.setOnClickListener(authorClickListener);
        holder.tvPostAuthorName.setOnClickListener(authorClickListener);

        View.OnClickListener postClickListener = v -> {
            Intent intent = new Intent(v.getContext(), CommenterActivity.class);
            intent.putExtra("post_id", post.getId());
            v.getContext().startActivity(intent);
        };

        if (post.getContent() != null && !post.getContent().trim().isEmpty()) {
            holder.tvPostContent.setVisibility(View.VISIBLE);
            setupClickableSpans(holder.tvPostContent, post.getContent(), holder.itemView.getContext());
            holder.tvPostContent.setOnClickListener(postClickListener);
        } else {
            holder.tvPostContent.setVisibility(View.GONE);
        }

        // Image du post
        if (post.getImagePath() != null && !post.getImagePath().trim().isEmpty()) {
            if (holder.layoutPostImageContainer != null) {
                holder.layoutPostImageContainer.setVisibility(View.VISIBLE);
            }
            holder.imgPostImage.setVisibility(View.VISIBLE);
            ImageUtils.loadPostImage(holder.itemView.getContext(), post.getImagePath(), holder.imgPostImage);

            if (post.isSensitive()) {
                if (holder.layoutSensitiveOverlay != null) {
                    holder.layoutSensitiveOverlay.setVisibility(View.VISIBLE);
                }
                holder.imgPostImage.setAlpha(0.2f);
                if (holder.btnRevealSensitive != null) {
                    holder.btnRevealSensitive.setOnClickListener(v -> {
                        if (holder.layoutSensitiveOverlay != null) {
                            holder.layoutSensitiveOverlay.setVisibility(View.GONE);
                        }
                        holder.imgPostImage.setAlpha(1.0f);
                    });
                }
            } else {
                if (holder.layoutSensitiveOverlay != null) {
                    holder.layoutSensitiveOverlay.setVisibility(View.GONE);
                }
                holder.imgPostImage.setAlpha(1.0f);
            }

            holder.imgPostImage.setOnClickListener(v -> {
                if (!post.isSensitive() || (holder.layoutSensitiveOverlay != null && holder.layoutSensitiveOverlay.getVisibility() == View.GONE)) {
                    if (listener != null) {
                        listener.onImageClick(post);
                    }
                }
            });

            if (post.getUserId() == currentUserId) {
                holder.imgPostImage.setOnLongClickListener(v -> {
                    showPostActionDialog(holder.itemView.getContext(), post);
                    return true;
                });
            } else {
                holder.imgPostImage.setOnLongClickListener(null);
            }
        } else {
            if (holder.layoutPostImageContainer != null) {
                holder.layoutPostImageContainer.setVisibility(View.GONE);
            } else {
                holder.imgPostImage.setVisibility(View.GONE);
            }
            holder.imgPostImage.setOnClickListener(null);
            holder.imgPostImage.setOnLongClickListener(null);
        }

        // Vidéo du post
        if (post.getVideoPath() != null && !post.getVideoPath().trim().isEmpty()) {
            if (holder.layoutPostVideo != null) {
                holder.layoutPostVideo.setVisibility(View.VISIBLE);
                if (holder.imgPostVideoThumbnail != null) {
                    ImageUtils.loadVideoThumbnail(holder.itemView.getContext(), post.getVideoPath(), holder.imgPostVideoThumbnail);
                }
                View.OnClickListener videoClickListener = v -> {
                    if (listener != null) {
                        listener.onVideoClick(post.getVideoPath());
                    }
                };
                holder.layoutPostVideo.setOnClickListener(videoClickListener);
                if (holder.ibPlayPostVideo != null) {
                    holder.ibPlayPostVideo.setOnClickListener(videoClickListener);
                }
            }
        } else {
            if (holder.layoutPostVideo != null) {
                holder.layoutPostVideo.setVisibility(View.GONE);
            }
        }

        String reaction = post.getUserReactionType();
        String reactionEmoji = "👍";
        if ("LOVE".equalsIgnoreCase(reaction)) {
            holder.btnLikePost.setText("❤️ J'adore");
            holder.btnLikePost.setTextColor(Color.RED);
            reactionEmoji = "❤️";
        } else if ("HAHA".equalsIgnoreCase(reaction)) {
            holder.btnLikePost.setText("😂 Haha");
            holder.btnLikePost.setTextColor(Color.parseColor("#FF9800"));
            reactionEmoji = "😂";
        } else if ("WOW".equalsIgnoreCase(reaction)) {
            holder.btnLikePost.setText("😮 Wouah");
            holder.btnLikePost.setTextColor(Color.parseColor("#2196F3"));
            reactionEmoji = "😮";
        } else if ("SAD".equalsIgnoreCase(reaction)) {
            holder.btnLikePost.setText("😢 Triste");
            holder.btnLikePost.setTextColor(Color.parseColor("#9C27B0"));
            reactionEmoji = "😢";
        } else if ("ANGRY".equalsIgnoreCase(reaction)) {
            holder.btnLikePost.setText("😡 En colère");
            holder.btnLikePost.setTextColor(Color.RED);
            reactionEmoji = "😡";
        } else if ("LIKE".equalsIgnoreCase(reaction) || post.isLikedByCurrentUser()) {
            holder.btnLikePost.setText("👍 J'aime");
            holder.btnLikePost.setTextColor(Color.parseColor("#2196F3"));
            reactionEmoji = "👍";
        } else {
            holder.btnLikePost.setText("👍 J'aime");
            holder.btnLikePost.setTextColor(Color.GRAY);
            reactionEmoji = "👍";
        }

        String stats = reactionEmoji + " " + post.getLikesCount() + " réactions  •  " + post.getCommentsCount() + " commentaires  •  " + post.getSharesCount() + " partages";
        holder.tvLikesCount.setText(stats);

        holder.btnLikePost.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(post);
            }
        });
        holder.btnLikePost.setOnLongClickListener(v -> {
            showReactionPopup(holder.itemView.getContext(), post);
            return true;
        });

        if (holder.btnCommentPost != null) {
            holder.btnCommentPost.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(post);
                }
            });
        }

        if (holder.btnSharePost != null) {
            holder.btnSharePost.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShareClick(post);
                }
            });
        }

        if (holder.btnReportPost != null) {
            holder.btnReportPost.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(post);
                }
            });
        }

        if (holder.ibEditPost != null) {
            holder.ibEditPost.setVisibility(View.GONE);
        }
        if (holder.ibDeletePost != null) {
            holder.ibDeletePost.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            showPostActionDialog(holder.itemView.getContext(), post);
            return true;
        });
    }

    private void showPostActionDialog(android.content.Context context, Post post) {
        boolean isEditable = (post.getUserId() == currentUserId) && TimeUtils.isWithinHours(post.getCreatedAt(), 1);
        List<String> options = new java.util.ArrayList<>();

        if (post.getUserId() == currentUserId) {
            options.add(post.isPinned() ? "📌 Désépingler du profil" : "📌 Épingler sur mon profil");
            if (isEditable) {
                options.add("✏️ Modifier la publication");
            }
            options.add("🗑️ Supprimer la publication");
        }

        List<String> extractedUrls = new java.util.ArrayList<>();
        if (post.getContent() != null) {
            java.util.regex.Matcher m = android.util.Patterns.WEB_URL.matcher(post.getContent());
            while (m.find()) {
                String url = m.group();
                if (!extractedUrls.contains(url)) {
                    extractedUrls.add(url);
                }
            }
        }

        if (!extractedUrls.isEmpty()) {
            options.add("🔗 Ouvrir le lien (" + extractedUrls.get(0) + ")");
            options.add("📋 Copier le lien");
        }

        options.add(post.isSavedByCurrentUser() ? "🔖 Retirer des enregistrés" : "🔖 Enregistrer la publication");

        boolean hasTextContent = post.getContent() != null && !post.getContent().trim().isEmpty();
        if (hasTextContent) {
            options.add("🌐 Traduire la publication (IA Gemini)");
        }

        options.add("❌ Annuler");

        String[] items = options.toArray(new String[0]);

        new AlertDialog.Builder(context)
                .setTitle("Options de la publication")
                .setItems(items, (dialog, which) -> {
                    String selected = items[which];
                    PostRepository repo = new PostRepository(context);

                    if (selected.contains("Épingler")) {
                        boolean pinned = repo.togglePinPost(post.getId(), currentUserId);
                        es.dmoral.toasty.Toasty.info(context, pinned ? "Publication épinglée 📌" : "Publication désépinglée", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                    } else if (selected.contains("Modifier")) {
                        if (listener != null) {
                            listener.onEditClick(post);
                        }
                    } else if (selected.contains("Supprimer")) {
                        if (listener != null) {
                            listener.onDeleteClick(post);
                        }
                    } else if (selected.contains("Ouvrir le lien")) {
                        if (!extractedUrls.isEmpty()) {
                            String urlToOpen = extractedUrls.get(0);
                            if (!urlToOpen.startsWith("http://") && !urlToOpen.startsWith("https://")) {
                                urlToOpen = "https://" + urlToOpen;
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen));
                            context.startActivity(intent);
                        }
                    } else if (selected.contains("Copier le lien")) {
                        if (!extractedUrls.isEmpty()) {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Lien du post", extractedUrls.get(0));
                            if (clipboard != null) clipboard.setPrimaryClip(clip);
                            es.dmoral.toasty.Toasty.success(context, "Lien copié dans le presse-papier", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                        }
                    } else if (selected.contains("Enregistrer")) {
                        boolean saved = repo.toggleSavePost(post.getId(), currentUserId);
                        es.dmoral.toasty.Toasty.info(context, saved ? "Publication enregistrée 🔖" : "Retirée des enregistrés", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                    } else if (selected.contains("Traduire")) {
                        GeminiHelper.showTranslateDialog(context, post.getContent());
                    }
                })
                .show();
    }

    private void showReactionPopup(android.content.Context context, Post post) {
        String[] reactions = new String[]{
                "👍 J'aime",
                "❤️ J'adore",
                "😂 Haha",
                "😮 Wouah",
                "😢 Triste",
                "😡 En colère",
                "❌ Retirer ma réaction"
        };

        new AlertDialog.Builder(context)
                .setTitle("Réagir à la publication")
                .setItems(reactions, (dialog, which) -> {
                    String selectedType = null;
                    switch (which) {
                        case 0: selectedType = "LIKE"; break;
                        case 1: selectedType = "LOVE"; break;
                        case 2: selectedType = "HAHA"; break;
                        case 3: selectedType = "WOW"; break;
                        case 4: selectedType = "SAD"; break;
                        case 5: selectedType = "ANGRY"; break;
                        case 6: selectedType = null; break;
                    }
                    if (listener != null) {
                        listener.onReactionClick(post, selectedType);
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    /**
     * Formate et configure les hashtags (#) et mentions (@) cliquables dans un TextView.
     * Les hashtags ouvrent le filtre de recherche et les mentions redirigent vers le profil de l'utilisateur.
     *
     * @param textView Le TextView à formater.
     * @param content  Le contenu texte original.
     * @param context  Le contexte d'activité.
     */
    public static void setupClickableSpans(TextView textView, String content, android.content.Context context) {
        if (textView == null || content == null || content.trim().isEmpty()) return;

        android.text.SpannableString spannableString = new android.text.SpannableString(content);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(#\\w+|@\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            final String match = matcher.group();
            int start = matcher.start();
            int end = matcher.end();

            android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (match.startsWith("#")) {
                        if (context instanceof AcceuilActivity) {
                            ((AcceuilActivity) context).filterPostsByTag(match);
                        } else {
                            Intent intent = new Intent(context, AcceuilActivity.class);
                            intent.putExtra("search_query", match);
                            context.startActivity(intent);
                        }
                    } else if (match.startsWith("@")) {
                        String username = match.substring(1);
                        Intent intent = new Intent(context, User2PageActivity.class);
                        intent.putExtra("user_name", username);
                        context.startActivity(intent);
                    }
                }

                @Override
                public void updateDrawState(@NonNull android.text.TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    if (match.startsWith("#")) {
                        ds.setColor(Color.parseColor("#2196F3"));
                    } else {
                        ds.setColor(Color.parseColor("#4CAF50"));
                    }
                }
            };

            spannableString.setSpan(clickableSpan, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannableString);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
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

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPostAuthorAvatar;
        TextView tvPostAuthorName;
        TextView tvPostTime;
        ImageButton ibEditPost;
        ImageButton ibDeletePost;
        TextView tvPostContent;
        ImageView imgPostImage;
        View layoutPostImageContainer;
        View layoutSensitiveOverlay;
        Button btnRevealSensitive;

        View layoutPostVideo;
        ImageView imgPostVideoThumbnail;
        ImageView ibPlayPostVideo;

        Button btnLikePost;
        Button btnCommentPost;
        Button btnSharePost;
        Button btnReportPost;
        TextView tvLikesCount;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPostAuthorAvatar = itemView.findViewById(R.id.imgPostAuthorAvatar);
            tvPostAuthorName = itemView.findViewById(R.id.tvPostAuthorName);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            ibEditPost = itemView.findViewById(R.id.ibEditPost);
            ibDeletePost = itemView.findViewById(R.id.ibDeletePost);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            imgPostImage = itemView.findViewById(R.id.imgPostImage);
            layoutPostImageContainer = itemView.findViewById(R.id.layoutPostImageContainer);
            layoutSensitiveOverlay = itemView.findViewById(R.id.layoutSensitiveOverlay);
            btnRevealSensitive = itemView.findViewById(R.id.btnRevealSensitive);

            layoutPostVideo = itemView.findViewById(R.id.layoutPostVideo);
            imgPostVideoThumbnail = itemView.findViewById(R.id.imgPostVideoThumbnail);
            ibPlayPostVideo = itemView.findViewById(R.id.ibPlayPostVideo);

            btnLikePost = itemView.findViewById(R.id.btnLikePost);
            btnCommentPost = itemView.findViewById(R.id.btnCommentPost);
            btnSharePost = itemView.findViewById(R.id.btnSharePost);
            btnReportPost = itemView.findViewById(R.id.btnReportPost);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
        }
    }
}
