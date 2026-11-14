package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.ConversationItem;

@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey
    public int conversationId;
    public int otherUserId;
    public String userName;
    public String lastMessage;
    public String profileImage;
    public int unreadCount;
    public boolean isPrivate;
    public boolean isPinned;

    public ConversationEntity() {}

    public static ConversationEntity fromModel(ConversationItem item) {
        if (item == null) return null;
        ConversationEntity entity = new ConversationEntity();
        entity.conversationId = item.getConversationId();
        entity.otherUserId = item.getOtherUserId();
        entity.userName = item.getUserName();
        entity.lastMessage = item.getLastMessage();
        entity.profileImage = item.getProfileImage();
        entity.unreadCount = item.getUnreadCount();
        entity.isPrivate = item.isPrivate();
        entity.isPinned = item.isPinned();
        return entity;
    }

    public ConversationItem toModel() {
        return new ConversationItem(conversationId, otherUserId, userName, lastMessage, profileImage, unreadCount, isPrivate, isPinned);
    }
}
