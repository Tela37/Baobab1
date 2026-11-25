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
import td.teladoum.baobab.repositories.FriendRepository;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.UserRepository;

/**
 * ViewModel pour la gestion du profil utilisateur (ProfileActivity, PageUtilisateurActivity) suivant l'architecture MVVM.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FriendRepository friendRepository;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Post>> userPostsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> postsCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> friendsCountLiveData = new MutableLiveData<>(0);

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.postRepository = new PostRepository(application);
        this.friendRepository = new FriendRepository(application);
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<List<Post>> getUserPostsLiveData() {
        return userPostsLiveData;
    }

    public LiveData<Integer> getPostsCountLiveData() {
        return postsCountLiveData;
    }

    public LiveData<Integer> getFriendsCountLiveData() {
        return friendsCountLiveData;
    }

    public void loadUserProfile(int targetUserId, int currentUserId) {
        if (targetUserId <= 0) return;

        User user = userRepository.getUserById(targetUserId);
        userLiveData.postValue(user);

        List<Post> posts = postRepository.getPostsByUserId(targetUserId, currentUserId);
        userPostsLiveData.postValue(posts != null ? posts : new ArrayList<>());
        postsCountLiveData.postValue(posts != null ? posts.size() : 0);

        List<User> friends = friendRepository.getFriendsList(targetUserId, null);
        friendsCountLiveData.postValue(friends != null ? friends.size() : 0);
    }

    public void updateProfileImage(int userId, String imagePath) {
        userRepository.updateUserProfileImage(userId, imagePath);
        loadUserProfile(userId, userId);
    }
}
