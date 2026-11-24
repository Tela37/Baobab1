package td.teladoum.baobab.firebase;

import com.google.firebase.database.FirebaseDatabase;

import td.teladoum.baobab.models.Message;

/**
 * DataSource distante dédiée aux messages et conversations dans Firebase Realtime Database.
 */
public class MessageDataSource {

    private final FirebaseDatabase realtimeDb;

    public MessageDataSource() {
        this.realtimeDb = FirebaseHelper.getInstance().getRealtimeDb();
    }

    public void syncMessageToRealtimeDb(int conversationId, Message msg) {
        if (msg == null) return;
        FirebaseHelper.getInstance().syncMessageToRealtimeDb(conversationId, msg);
    }

    public void syncMessageReactionToRealtimeDb(int messageId, int userId, String reaction) {
        FirebaseHelper.getInstance().syncMessageReactionToRealtimeDb(messageId, userId, reaction);
    }

    public void syncConversationToRealtimeDb(int convId, int user1Id, int user2Id) {
        FirebaseHelper.getInstance().syncConversationToRealtimeDb(convId, user1Id, user2Id);
    }
}
