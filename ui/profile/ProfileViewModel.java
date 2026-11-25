package td.teladoum.baobab.ui.profile;

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
 * ViewModel pour le profil utilisateur (ProfileActivity) dans le package ui.profile.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FriendRepository friendRepository;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Post>> userPostsLiveData = new MutableLiveData<>();
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

    public LiveData<Integer> getFriendsCountLiveData() {
        return friendsCountLiveData;
    }

    public void loadProfile(int userId, int currentUserId) {
        if (userId <= 0) return;

        User user = userRepository.getUserById(userId);
        userLiveData.setValue(user);

        List<Post> posts = postRepository.getPostsByUserId(userId, currentUserId);
        userPostsLiveData.setValue(posts != null ? posts : new ArrayList<>());

        List<User> friends = friendRepository.getFriendsList(userId, null);
        friendsCountLiveData.setValue(friends != null ? friends.size() : 0);
    }
}
