package td.teladoum.baobab.viewmodels;

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
 * ViewModel pour l'écran d'accueil (AcceuilActivity) suivant l'architecture MVVM.
 * Gère l'état UI réactif pour les publications, stories, notifications et annonces système.
 */
public class AcceuilViewModel extends AndroidViewModel {

    private final PostRepository postRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SystemRepository systemRepository;

    private final MutableLiveData<List<Post>> postsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<UserStoryGroup>> storiesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<UserStoryGroup> currentUserStoryGroupLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadNotificationsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<String> systemAnnouncementLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserAvatarLiveData = new MutableLiveData<>();

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

    public LiveData<Integer> getUnreadNotificationsCountLiveData() {
        return unreadNotificationsCountLiveData;
    }

    public LiveData<String> getSystemAnnouncementLiveData() {
        return systemAnnouncementLiveData;
    }

    public LiveData<String> getCurrentUserAvatarLiveData() {
        return currentUserAvatarLiveData;
    }

    public void loadFeed(int currentUserId) {
        loadPosts(currentUserId);
        loadStories(currentUserId);
        updateNotificationCount(currentUserId);
        loadUserAvatar(currentUserId);
        listenToSystemAnnouncements();

        PostRepository.loadAllPostsFromCloud(getApplication(), () -> loadPosts(currentUserId));
        storyRepository.loadStoriesPage(20, () -> loadStories(currentUserId));
    }

    public void loadPosts(int currentUserId) {
        List<Post> posts = postRepository.getAllPosts(currentUserId);
        postsLiveData.postValue(posts != null ? posts : new ArrayList<>());
    }

    public void loadStories(int currentUserId) {
        List<UserStoryGroup> allGroups = storyRepository.getGroupedRecentStories();
        List<UserStoryGroup> friendGroups = new ArrayList<>();
        UserStoryGroup ownGroup = null;

        if (allGroups != null) {
            for (UserStoryGroup group : allGroups) {
                if (group.getUserId() == currentUserId) {
                    ownGroup = group;
                } else {
                    friendGroups.add(group);
                }
            }
        }

        currentUserStoryGroupLiveData.postValue(ownGroup);
        storiesLiveData.postValue(friendGroups);
    }

    public void updateNotificationCount(int currentUserId) {
        if (currentUserId <= 0) return;
        int count = notificationRepository.getUnreadCount(currentUserId);
        unreadNotificationsCountLiveData.postValue(count);
    }

    public void loadUserAvatar(int currentUserId) {
        if (currentUserId <= 0) return;
        User user = userRepository.getUserById(currentUserId);
        if (user != null && user.getProfileImage() != null) {
            currentUserAvatarLiveData.postValue(user.getProfileImage());
        }
    }

    public void listenToSystemAnnouncements() {
        systemRepository.listenToAppAnnouncement((text, active) -> {
            if (active && text != null && !text.trim().isEmpty()) {
                systemAnnouncementLiveData.postValue(text.trim());
            } else {
                systemAnnouncementLiveData.postValue(null);
            }
        });
    }

    public void toggleLikePost(int postId, int currentUserId) {
        postRepository.toggleLikePost(postId, currentUserId);
        loadPosts(currentUserId);
    }

    public void setPostReaction(int postId, int currentUserId, String reactionType) {
        postRepository.setPostReaction(postId, currentUserId, reactionType);
        loadPosts(currentUserId);
    }

    public boolean publishPost(int userId, String content, String imagePath, String videoPath, boolean isSensitive) {
        boolean success = postRepository.createPost(userId, content, imagePath, videoPath, isSensitive);
        if (success) {
            loadPosts(userId);
        }
        return success;
    }
}
