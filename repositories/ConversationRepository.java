package td.teladoum.baobab.repositories;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.ConversationEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.ConversationItem;


/**
 * Repository 100% Cloud-Native pour la gestion des conversations.
 */
public class ConversationRepository {

    private static final Map<String, Integer> convPairCache = new ConcurrentHashMap<>();
    private static final Map<Integer, ConversationData> convCache = new ConcurrentHashMap<>();
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public static class ConversationData {
        public int id;
        public int user1Id;
        public int user2Id;
        public boolean isPrivate;
        public boolean isPinned;
    }

    public ConversationRepository(Context context) {
        this.userRepository = new UserRepository(context);
        this.messageRepository = new MessageRepository(context);
    }

    public ConversationRepository(Object dbHelper, UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository != null ? userRepository : new UserRepository();
        this.messageRepository = messageRepository != null ? messageRepository : new MessageRepository((Context) null);
    }

    public long createConversation(int user1Id, int user2Id) {
        int u1 = Math.min(user1Id, user2Id);
        int u2 = Math.max(user1Id, user2Id);
        String key = u1 + "_" + u2;

        if (convPairCache.containsKey(key)) {
            return convPairCache.get(key);
        }

        int convId = Math.abs(key.hashCode());
        ConversationData data = new ConversationData();
        data.id = convId;
        data.user1Id = u1;
        data.user2Id = u2;

        convPairCache.put(key, convId);
        convCache.put(convId, data);

        FirebaseHelper.getInstance().syncConversationToRealtimeDb(convId, u1, u2);
        return convId;
    }

    public int getConversationId(int user1Id, int user2Id) {
        if (user1Id <= 0 || user2Id <= 0) return -1;
        int u1 = Math.min(user1Id, user2Id);
        int u2 = Math.max(user1Id, user2Id);
        String key = u1 + "_" + u2;
        if (convPairCache.containsKey(key)) {
            return convPairCache.get(key);
        }
        int convId = Math.abs(key.hashCode());
        convPairCache.put(key, convId);
        if (!convCache.containsKey(convId)) {
            ConversationData data = new ConversationData();
            data.id = convId;
            data.user1Id = u1;
            data.user2Id = u2;
            convCache.put(convId, data);
        }
        return convId;
    }

    public boolean hasConversation(int user1Id, int user2Id) {
        int convId = getConversationId(user1Id, user2Id);
        if (convId == -1) return false;
        List<td.teladoum.baobab.models.Message> msgs = messageRepository.getMessages(convId);
        return msgs != null && !msgs.isEmpty();
    }

    public void syncConversationsFromCloud(int currentUserId, Runnable onComplete) {
        if (currentUserId <= 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        com.google.firebase.database.DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb()
                .getReference("conversations");

        ref.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        Integer id = snap.child("id").getValue(Integer.class);
                        Integer u1 = snap.child("user1Id").getValue(Integer.class);
                        Integer u2 = snap.child("user2Id").getValue(Integer.class);

                        if (id != null && u1 != null && u2 != null) {
                            if (u1 == currentUserId || u2 == currentUserId) {
                                String key = u1 + "_" + u2;
                                convPairCache.put(key, id);
                                if (!convCache.containsKey(id)) {
                                    ConversationData data = new ConversationData();
                                    data.id = id;
                                    data.user1Id = u1;
                                    data.user2Id = u2;
                                    convCache.put(id, data);
                                }
                                messageRepository.loadMessagesFromCloud(id, onComplete);
                            }
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

    public int createConversationIfNotExists(int user1Id, int user2Id) {
        int existing = getConversationId(user1Id, user2Id);
        if (existing != -1) return existing;
        return (int) createConversation(user1Id, user2Id);
    }

    public int getOtherUserId(int conversationId, int currentUserId) {
        ConversationData data = convCache.get(conversationId);
        if (data != null) {
            return data.user1Id == currentUserId ? data.user2Id : data.user1Id;
        }
        return -1;
    }

    public static void saveConversationsToRoom(Context context, List<ConversationItem> items) {
        if (context == null || items == null) return;
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    List<ConversationEntity> entities = new ArrayList<>();
                    for (ConversationItem item : items) {
                        ConversationEntity entity = ConversationEntity.fromModel(item);
                        if (entity != null) entities.add(entity);
                    }
                    db.conversationDao().insertConversations(entities);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public List<ConversationItem> getConversations(int currentUserId) {
        List<ConversationItem> result = new ArrayList<>();
        for (ConversationData data : convCache.values()) {
            if (data.user1Id == currentUserId || data.user2Id == currentUserId) {
                int otherUserId = (data.user1Id == currentUserId) ? data.user2Id : data.user1Id;
                List<td.teladoum.baobab.models.Message> msgs = messageRepository.getMessages(data.id);
                if (msgs != null && !msgs.isEmpty()) {
                    result.add(
                            new ConversationItem(
                                    data.id,
                                    otherUserId,
                                    userRepository.getUserName(otherUserId),
                                    messageRepository.getLastMessage(data.id),
                                    userRepository.getUserProfileImage(otherUserId),
                                    messageRepository.getUnreadCount(data.id, currentUserId),
                                    data.isPrivate,
                                    data.isPinned
                            )
                    );
                }
            }
        }
        return result;
    }

    public int getTotalUnreadCount(int currentUserId) {
        int total = 0;
        for (ConversationData data : convCache.values()) {
            if (data.user1Id == currentUserId || data.user2Id == currentUserId) {
                total += messageRepository.getUnreadCount(data.id, currentUserId);
            }
        }
        return total;
    }

    public boolean setConversationPrivate(int conversationId, boolean isPrivate) {
        ConversationData data = convCache.get(conversationId);
        if (data != null) {
            data.isPrivate = isPrivate;
            return true;
        }
        return false;
    }

    public boolean setConversationPinned(int conversationId, boolean isPinned) {
        ConversationData data = convCache.get(conversationId);
        if (data != null) {
            data.isPinned = isPinned;
            return true;
        }
        return false;
    }

    public boolean isConversationPrivate(int conversationId) {
        ConversationData data = convCache.get(conversationId);
        return data != null && data.isPrivate;
    }

    public boolean deleteConversation(int conversationId) {
        convCache.remove(conversationId);
        messageRepository.clearConversationMessages(conversationId);
        return true;
    }
}

