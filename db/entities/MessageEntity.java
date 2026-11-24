package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.Message;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    public int id;
    public int conversationId;
    public int senderId;
    public String message;
    public String imagePath;
    public String videoPath;
    public String audioPath;
    public String createdAt;
    public boolean isRead;
    public boolean isDelivered;
    public boolean isDeleted;
    public String reaction;
    public Integer replyToMessageId;
    public String replyToText;

    public MessageEntity() {}

    public static MessageEntity fromModel(Message m) {
        if (m == null) return null;
        MessageEntity entity = new MessageEntity();
        entity.id = m.getId();
        entity.conversationId = m.getConversationId();
        entity.senderId = m.getSenderId();
        entity.message = m.getMessage();
        entity.imagePath = m.getImagePath();
        entity.videoPath = m.getVideoPath();
        entity.audioPath = m.getAudioPath();
        entity.createdAt = m.getCreatedAt();
        entity.isRead = m.isRead();
        entity.isDelivered = m.isDelivered();
        entity.isDeleted = m.isDeleted();
        entity.reaction = m.getReaction();
        entity.replyToMessageId = m.getReplyToMessageId();
        entity.replyToText = m.getReplyToText();
        return entity;
    }

    public Message toModel() {
        Message m = new Message(id, conversationId, senderId, message, createdAt);
        m.setImagePath(imagePath);
        m.setVideoPath(videoPath);
        m.setAudioPath(audioPath);
        m.setRead(isRead);
        m.setDelivered(isDelivered);
        m.setDeleted(isDeleted);
        m.setReaction(reaction);
        m.setReplyToMessageId(replyToMessageId);
        m.setReplyToText(replyToText);
        return m;
    }
}
