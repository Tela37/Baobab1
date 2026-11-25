package td.teladoum.baobab.repositories;

import android.content.Context;

import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.Story;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.models.UserStoryGroup;
import td.teladoum.baobab.utils.TimeUtils;


/**
 * Repository Cloud-Native pour la gestion des stories éphémères exclusivement via Realtime Database.
 */
public class StoryRepository {

    private static final Map<Integer, Story> storiesCache = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<Integer>> storyViewsMap = new ConcurrentHashMap<>();
    private final UserRepository userRepository;

    public StoryRepository(Context context) {
        this.userRepository = new UserRepository(context != null ? context.getApplicationContext() : null);
        preloadSampleStories();
    }

    private void preloadSampleStories() {
        String now = TimeUtils.getCurrentLocalDateTime();
        if (storiesCache.isEmpty()) {
            storiesCache.put(901, new Story(901, 101, "Rinaldi Rema", "https://images.unsplash.com/photo-1534528741775-53994a69daeb", "https://images.unsplash.com/photo-1516321318423-f06f85e504b3", "Bienvenue sur BaobabTD ! 🌟", now));
            storiesCache.put(902, new Story(902, 102, "Tel-Adoum Maïbé", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e", "Journée ensoleillée à N'Djamena 🇹🇩", now));
        } else {
            Story s1 = storiesCache.get(901);
            if (s1 != null) s1.setCreatedAt(now);
            Story s2 = storiesCache.get(902);
            if (s2 != null) s2.setCreatedAt(now);
        }
    }

    public boolean createStory(int userId, String imagePath, String caption) {
        int id = Math.abs((userId + "_" + System.currentTimeMillis()).hashCode());
        String createdAt = TimeUtils.getCurrentLocalDateTime();

        User user = userRepository.getUserById(userId);
        String userName = (user != null && user.getName() != null && !user.getName().trim().isEmpty()) ? user.getName() : "Utilisateur";
        String userAvatar = user != null ? user.getProfileImage() : null;

        Story story = new Story(id, userId, userName, userAvatar, imagePath, caption != null ? caption.trim() : "", createdAt);
        storiesCache.put(id, story);

        FirebaseHelper.getInstance().syncStoryToRealtimeDb(story);

        return true;
    }

    private static com.google.firebase.database.ValueEventListener liveStoriesListener = null;

    public static com.google.firebase.database.ValueEventListener listenToRealtimeStories(Runnable onStoriesUpdated) {
        DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("stories");
        if (liveStoriesListener != null) {
            ref.removeEventListener(liveStoriesListener);
        }

        liveStoriesListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    Story story = snap.getValue(Story.class);
                    if (story != null) {
                        storiesCache.put(story.getId(), story);
                    }
                }
                if (onStoriesUpdated != null) {
                    onStoriesUpdated.run();
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                if (onStoriesUpdated != null) {
                    onStoriesUpdated.run();
                }
            }
        };
        ref.addValueEventListener(liveStoriesListener);
        return liveStoriesListener;
    }

    public void loadStoriesFromCloud(Runnable onComplete) {
        preloadSampleStories();
        FirebaseHelper.getInstance().getRealtimeDb().getReference("stories")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                            Story story = snap.getValue(Story.class);
                            if (story != null) {
                                storiesCache.put(story.getId(), story);
                                syncStoryViewsFromCloud(story.getId());
                            }
                        }
                        listenToRealtimeStories(onComplete);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        listenToRealtimeStories(onComplete);
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });
    }

    private void syncStoryViewsFromCloud(int storyId) {
        try {
            FirebaseHelper.getInstance().getRealtimeDb()
                    .getReference("story_views")
                    .child(String.valueOf(storyId))
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            Set<Integer> viewers = storyViewsMap.computeIfAbsent(storyId, k -> java.util.Collections.synchronizedSet(new HashSet<>()));
                            for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                                try {
                                    String key = snap.getKey();
                                    if (key != null) {
                                        int viewerId = Integer.parseInt(key);
                                        viewers.add(viewerId);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        @Override
                        public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
                    });
        } catch (Exception ignored) {}
    }

    public void loadStoriesPage(int limit, Runnable onComplete) {
        FirebaseHelper.getInstance().getRealtimeDb().getReference("stories")
                .orderByKey()
                .limitToLast(limit)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                Story story = snap.getValue(Story.class);
                                if (story != null) {
                                    storiesCache.put(story.getId(), story);
                                }
                            } catch (Exception ignored) {}
                        }
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                });
    }

    public List<Story> getRecentStories() {
        List<Story> recent = new ArrayList<>();
        for (Story story : storiesCache.values()) {
            if (story != null) {
                if (TimeUtils.isWithinHours(story.getCreatedAt(), 24)) {
                    recent.add(story);
                }
            }
        }
        return recent;
    }

    public List<UserStoryGroup> getGroupedRecentStories() {
        List<Story> allStories = getRecentStories();
        List<UserStoryGroup> groups = new ArrayList<>();

        for (Story story : allStories) {
            User user = userRepository.getUserById(story.getUserId());
            if (user != null) {
                if (user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
                    story.setUserAvatar(user.getProfileImage());
                }
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    story.setUserName(user.getName());
                }
            }

            UserStoryGroup existingGroup = null;
            for (UserStoryGroup group : groups) {
                if (group.getUserId() == story.getUserId()) {
                    existingGroup = group;
                    break;
                }
            }

            if (existingGroup != null) {
                existingGroup.getStories().add(story);
            } else {
                List<Story> userStories = new ArrayList<>();
                userStories.add(story);
                UserStoryGroup newGroup = new UserStoryGroup(
                        story.getUserId(),
                        story.getUserName(),
                        story.getUserAvatar(),
                        userStories
                );
                groups.add(newGroup);
            }
        }

        return groups;
    }

    public boolean setStoryReaction(int storyId, int userId, String reactionType) {
        FirebaseHelper.getInstance().syncStoryReactionToRealtimeDb(storyId, userId, reactionType);
        return true;
    }

    public String getStoryReaction(int storyId, int userId) {
        return null;
    }

    public void recordStoryView(int storyId, int userId) {
        Story story = storiesCache.get(storyId);
        if (story != null && story.getUserId() == userId) {
            // Author viewing their own story -> do not count!
            return;
        }

        Set<Integer> viewers = storyViewsMap.computeIfAbsent(storyId, k -> java.util.Collections.synchronizedSet(new HashSet<>()));
        viewers.add(userId);

        try {
            FirebaseHelper.getInstance().getRealtimeDb()
                    .getReference("story_views")
                    .child(String.valueOf(storyId))
                    .child(String.valueOf(userId))
                    .setValue(true);
        } catch (Exception ignored) {}
    }

    public int getStoryViewsCount(int storyId) {
        Set<Integer> viewers = storyViewsMap.get(storyId);
        return viewers != null ? viewers.size() : 0;
    }

    public boolean addStoryComment(int storyId, int userId, String comment) {
        if (comment == null || comment.trim().isEmpty()) return false;
        FirebaseHelper.getInstance().syncStoryCommentToRealtimeDb(storyId, userId, comment.trim());
        return true;
    }
}

