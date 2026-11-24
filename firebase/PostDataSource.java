package td.teladoum.baobab.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import td.teladoum.baobab.models.Post;

/**
 * DataSource distante dédiée aux publications (Posts) dans Firebase Realtime Database.
 */
public class PostDataSource {

    private final FirebaseDatabase realtimeDb;

    public PostDataSource() {
        this.realtimeDb = FirebaseHelper.getInstance().getRealtimeDb();
    }

    public void syncPostToRealtimeDb(Post post) {
        if (post == null) return;
        FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
    }

    public void deletePostFromCloud(int postId) {
        FirebaseHelper.getInstance().deletePostFromCloud(postId);
    }

    public void syncPostReactionToRealtimeDb(int postId, int userId, String reactionType) {
        FirebaseHelper.getInstance().syncPostReactionToRealtimeDb(postId, userId, reactionType);
    }

    public void listenToRealtimePosts(ValueEventListener listener) {
        if (listener == null) return;
        realtimeDb.getReference("posts").addValueEventListener(listener);
    }
}
