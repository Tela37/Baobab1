package td.teladoum.baobab.data.model;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Modèle Notification dans data.model.
 */
@IgnoreExtraProperties
public class AppNotification {

    private int id;
    private int recipientId;
    private int senderId;
    private String senderName;
    private String senderAvatar;
    private String type;
    private String targetType;
    private int targetId;
    private String message;
    private boolean isRead;
    private String createdAt;

    public AppNotification() {
    }

    public AppNotification(int id, int recipientId, int senderId, String senderName, String senderAvatar, String type, String targetType, int targetId, String message, boolean isRead, String createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
