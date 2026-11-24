package td.teladoum.baobab.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import td.teladoum.baobab.models.Story;

/**
 * DataSource distante dédiée aux Stories dans Firebase Realtime Database.
 */
public class StoryDataSource {

    private final FirebaseDatabase realtimeDb;

    public StoryDataSource() {
        this.realtimeDb = FirebaseHelper.getInstance().getRealtimeDb();
    }

    public void syncStoryToRealtimeDb(Story story) {
        if (story == null) return;
        FirebaseHelper.getInstance().syncStoryToRealtimeDb(story);
    }

    public void syncStoryReactionToRealtimeDb(int storyId, int userId, String reactionType) {
        FirebaseHelper.getInstance().syncStoryReactionToRealtimeDb(storyId, userId, reactionType);
    }

    public void listenToStories(ValueEventListener listener) {
        if (listener == null) return;
        realtimeDb.getReference("stories").addValueEventListener(listener);
    }
}
