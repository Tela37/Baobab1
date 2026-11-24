package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.UserStoryGroup;
import td.teladoum.baobab.utils.ImageUtils;

/**
 * Adaptateur pour la barre de stories style Facebook.
 * Gère deux types de cartes :
 * 1. "Créer une story" (Card 0) : Photo de profil + bouton (+) bleu + fond blanc.
 * 2. Stories publiées ("Votre story" et Amis) : Image en fond + badge avatar + nom.
 */
public class StoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_CREATE = 0;
    private static final int VIEW_TYPE_STORY = 1;

    private final List<UserStoryGroup> friendStoryGroupList;
    private UserStoryGroup currentUserStoryGroup;
    private String currentUserAvatar;
    private final OnAddStoryClickListener addStoryClickListener;
    private final OnStoryGroupClickListener storyGroupClickListener;

    public interface OnAddStoryClickListener {
        void onAddStoryClick();
    }

    public interface OnStoryGroupClickListener {
        void onStoryGroupClick(UserStoryGroup group);
    }

    public StoryAdapter(
            List<UserStoryGroup> friendStoryGroupList,
            UserStoryGroup currentUserStoryGroup,
            String currentUserAvatar,
            OnAddStoryClickListener addStoryClickListener,
            OnStoryGroupClickListener storyGroupClickListener) {
        this.friendStoryGroupList = friendStoryGroupList;
        this.currentUserStoryGroup = currentUserStoryGroup;
        this.currentUserAvatar = currentUserAvatar;
        this.addStoryClickListener = addStoryClickListener;
        this.storyGroupClickListener = storyGroupClickListener;
    }

    public void setCurrentUserAvatar(String avatar) {
        this.currentUserAvatar = avatar;
    }

    public void updateData(List<UserStoryGroup> newFriendGroups, UserStoryGroup newCurrentUserGroup, String newCurrentUserAvatar) {
        if (newFriendGroups != null) {
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new StoryDiffCallback(this.friendStoryGroupList, newFriendGroups));
            this.friendStoryGroupList.clear();
            this.friendStoryGroupList.addAll(newFriendGroups);
            diffResult.dispatchUpdatesTo(this);
        }
        this.currentUserStoryGroup = newCurrentUserGroup;
        if (newCurrentUserAvatar != null && !newCurrentUserAvatar.isEmpty()) {
            this.currentUserAvatar = newCurrentUserAvatar;
        }
    }

    private static class StoryDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<UserStoryGroup> oldList;
        private final List<UserStoryGroup> newList;

        public StoryDiffCallback(List<UserStoryGroup> oldList, List<UserStoryGroup> newList) {
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
            UserStoryGroup oldItem = oldList.get(oldItemPosition);
            UserStoryGroup newItem = newList.get(newItemPosition);
            return oldItem != null && newItem != null && oldItem.getUserId() == newItem.getUserId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UserStoryGroup oldItem = oldList.get(oldItemPosition);
            UserStoryGroup newItem = newList.get(newItemPosition);
            if (oldItem == null || newItem == null) return false;

            return oldItem.getStoriesCount() == newItem.getStoriesCount()
                    && java.util.Objects.equals(oldItem.getUserName(), newItem.getUserName());
        }
    }

    public void updateData(List<UserStoryGroup> newFriendGroups, UserStoryGroup newCurrentUserGroup) {
        updateData(newFriendGroups, newCurrentUserGroup, null);
    }

    private boolean hasCurrentUserStory() {
        return currentUserStoryGroup != null && currentUserStoryGroup.getStoriesCount() > 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_CREATE;
        }
        return VIEW_TYPE_STORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_CREATE) {
            View view = inflater.inflate(R.layout.item_story_create, parent, false);
            return new CreateStoryViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_story, parent, false);
            return new PublishedStoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CreateStoryViewHolder) {
            // Carte 0 : "Créer une story"
            CreateStoryViewHolder createHolder = (CreateStoryViewHolder) holder;
            ImageUtils.loadProfileImage(createHolder.itemView.getContext(), currentUserAvatar, createHolder.imgUserAvatar);

            View.OnClickListener clickListener = v -> {
                if (addStoryClickListener != null) {
                    addStoryClickListener.onAddStoryClick();
                }
            };

            createHolder.itemView.setOnClickListener(clickListener);
            if (createHolder.cardContainerCreate != null) createHolder.cardContainerCreate.setOnClickListener(clickListener);
            if (createHolder.imgUserAvatar != null) createHolder.imgUserAvatar.setOnClickListener(clickListener);
            if (createHolder.imgCreatePlus != null) createHolder.imgCreatePlus.setOnClickListener(clickListener);

        } else if (holder instanceof PublishedStoryViewHolder) {
            PublishedStoryViewHolder storyHolder = (PublishedStoryViewHolder) holder;

            UserStoryGroup group;
            boolean isOwnStory = false;

            if (hasCurrentUserStory() && position == 1) {
                group = currentUserStoryGroup;
                isOwnStory = true;
            } else {
                int friendIndex = position - 1 - (hasCurrentUserStory() ? 1 : 0);
                group = friendStoryGroupList.get(friendIndex);
            }

            storyHolder.tvStoryAuthorName.setText(isOwnStory ? "Votre story" : group.getUserName());

            if (storyHolder.tvStoryRemainingTime != null) {
                String remaining = td.teladoum.baobab.utils.TimeUtils.getRemainingTimeText(group.getLatestCreatedAt());
                storyHolder.tvStoryRemainingTime.setText(remaining);
            }

            if (group.getStoriesCount() > 1) {
                storyHolder.tvStoryCountBadge.setVisibility(View.VISIBLE);
                storyHolder.tvStoryCountBadge.setText(String.valueOf(group.getStoriesCount()));
            } else {
                storyHolder.tvStoryCountBadge.setVisibility(View.GONE);
            }

            String cover = group.getCoverImage();
            if (cover == null || cover.trim().isEmpty()) {
                cover = group.getUserAvatar();
            }
            if (isOwnStory && (cover == null || cover.trim().isEmpty())) {
                cover = currentUserAvatar;
            }
            ImageUtils.loadStoryImage(storyHolder.itemView.getContext(), cover, storyHolder.imgStoryBackground);
            ImageUtils.loadProfileImage(storyHolder.itemView.getContext(), isOwnStory && currentUserAvatar != null ? currentUserAvatar : group.getUserAvatar(), storyHolder.imgStoryAuthorAvatar);

            View.OnClickListener clickListener = v -> {
                if (storyGroupClickListener != null) {
                    storyGroupClickListener.onStoryGroupClick(group);
                }
            };

            storyHolder.itemView.setOnClickListener(clickListener);
            if (storyHolder.cardContainer != null) storyHolder.cardContainer.setOnClickListener(clickListener);
            if (storyHolder.imgStoryBackground != null) storyHolder.imgStoryBackground.setOnClickListener(clickListener);
            if (storyHolder.imgStoryAuthorAvatar != null) storyHolder.imgStoryAuthorAvatar.setOnClickListener(clickListener);
            if (storyHolder.tvStoryAuthorName != null) storyHolder.tvStoryAuthorName.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return 1 + (hasCurrentUserStory() ? 1 : 0) + friendStoryGroupList.size();
    }

    // ViewHolder pour la carte "Créer une story"
    public static class CreateStoryViewHolder extends RecyclerView.ViewHolder {
        View cardContainerCreate;
        ImageView imgUserAvatar;
        ImageView imgCreatePlus;

        public CreateStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainerCreate = itemView.findViewById(R.id.cardContainerCreate);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            imgCreatePlus = itemView.findViewById(R.id.imgCreatePlus);
        }
    }

    // ViewHolder pour les cartes de stories publiées
    public static class PublishedStoryViewHolder extends RecyclerView.ViewHolder {
        View cardContainer;
        ImageView imgStoryBackground;
        ImageView imgStoryAuthorAvatar;
        TextView tvStoryAuthorName;
        TextView tvStoryCountBadge;
        TextView tvStoryRemainingTime;

        public PublishedStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            imgStoryBackground = itemView.findViewById(R.id.imgStoryBackground);
            imgStoryAuthorAvatar = itemView.findViewById(R.id.imgStoryAuthorAvatar);
            tvStoryAuthorName = itemView.findViewById(R.id.tvStoryAuthorName);
            tvStoryCountBadge = itemView.findViewById(R.id.tvStoryCountBadge);
            tvStoryRemainingTime = itemView.findViewById(R.id.tvStoryRemainingTime);
        }
    }
}
