package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.AppNotification;

@Entity(tableName = "notifications")
public class NotificationEntity {

    @PrimaryKey
    public int id;
    public int recipientId;
    public int senderId;
    public String senderName;
    public String senderAvatar;
    public String type;
    public String targetType;
    public int targetId;
    public String message;
    public boolean isRead;
    public String createdAt;

    public NotificationEntity() {}

    public static NotificationEntity fromModel(AppNotification notif) {
        if (notif == null) return null;
        NotificationEntity entity = new NotificationEntity();
        entity.id = notif.getId();
        entity.recipientId = notif.getRecipientId();
        entity.senderId = notif.getSenderId();
        entity.senderName = notif.getSenderName();
        entity.senderAvatar = notif.getSenderAvatar();
        entity.type = notif.getType();
        entity.targetType = notif.getTargetType();
        entity.targetId = notif.getTargetId();
        entity.message = notif.getMessage();
        entity.isRead = notif.isRead();
        entity.createdAt = notif.getCreatedAt();
        return entity;
    }

    public AppNotification toModel() {
        return new AppNotification(id, recipientId, senderId, senderName, senderAvatar, type, targetType, targetId, message, isRead, createdAt);
    }
}
