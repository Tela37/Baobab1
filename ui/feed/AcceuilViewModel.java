package td.teladoum.baobab.ui.feed;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import td.teladoum.baobab.models.Post;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.models.UserStoryGroup;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.StoryRepository;
import td.teladoum.baobab.repositories.SystemRepository;
import td.teladoum.baobab.repositories.UserRepository;

/**
 * ViewModel pour le fil d'actualités principal (AcceuilActivity) dans le package ui.feed.
 */
public class AcceuilViewModel extends AndroidViewModel {

    private final PostRepository postRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SystemRepository systemRepository;

    private final MutableLiveData<List<Post>> postsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<UserStoryGroup>> storiesLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserStoryGroup> currentUserStoryGroupLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadNotificationsLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> announcementLiveData = new MutableLiveData<>();

    public AcceuilViewModel(@NonNull Application application) {
        super(application);
        this.postRepository = new PostRepository(application);
        this.storyRepository = new StoryRepository(application);
        this.userRepository = new UserRepository(application);
        this.notificationRepository = new NotificationRepository(application);
        this.systemRepository = new SystemRepository(application);
    }

    public LiveData<List<Post>> getPostsLiveData() {
        return postsLiveData;
    }

    public LiveData<List<UserStoryGroup>> getStoriesLiveData() {
        return storiesLiveData;
    }

    public LiveData<UserStoryGroup> getCurrentUserStoryGroupLiveData() {
        return currentUserStoryGroupLiveData;
    }

    public LiveData<Integer> getUnreadNotificationsLiveData() {
        return unreadNotificationsLiveData;
    }

    public LiveData<String> getAnnouncementLiveData() {
        return announcementLiveData;
    }

    public void loadPosts(int currentUserId) {
        List<Post> posts = postRepository.getAllPosts(currentUserId);
        postsLiveData.setValue(posts != null ? posts : new ArrayList<>());
    }

    public void loadStories(int currentUserId) {
        List<UserStoryGroup> allGroups = storyRepository.getGroupedRecentStories();
        List<UserStoryGroup> friendGroups = new ArrayList<>();
        UserStoryGroup currentUserGroup = null;

        for (UserStoryGroup group : allGroups) {
            if (group.getUserId() == currentUserId) {
                currentUserGroup = group;
            } else {
                friendGroups.add(group);
            }
        }

        currentUserStoryGroupLiveData.setValue(currentUserGroup);
        storiesLiveData.setValue(friendGroups);
    }

    public void updateNotificationBadge(int currentUserId) {
        if (currentUserId <= 0) return;
        int unread = notificationRepository.getUnreadCount(currentUserId);
        unreadNotificationsLiveData.setValue(unread);
    }

    public void listenToSystemAnnouncement() {
        systemRepository.listenToAppAnnouncement((text, active) -> {
            if (active && text != null && !text.trim().isEmpty()) {
                announcementLiveData.setValue(text.trim());
            } else {
                announcementLiveData.setValue(null);
            }
        });
    }

    public void syncCloudData(int currentUserId, Runnable onComplete) {
        PostRepository.syncPendingPosts(getApplication());
        PostRepository.loadAllPostsFromCloud(getApplication(), () -> {
            userRepository.syncAllUsersFromCloud(() -> {
                loadPosts(currentUserId);
                loadStories(currentUserId);
                updateNotificationBadge(currentUserId);
                if (onComplete != null) onComplete.run();
            });
        });
    }
}
