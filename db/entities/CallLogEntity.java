package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "call_logs")
public class CallLogEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int currentUserId;
    private int targetUserId;
    private String targetName;
    private String targetAvatar;
    private boolean isVideoCall;
    private String createdAt;

    public CallLogEntity() {}

    public CallLogEntity(int currentUserId, int targetUserId, String targetName, String targetAvatar, boolean isVideoCall, String createdAt) {
        this.currentUserId = currentUserId;
        this.targetUserId = targetUserId;
        this.targetName = targetName;
        this.targetAvatar = targetAvatar;
        this.isVideoCall = isVideoCall;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCurrentUserId() { return currentUserId; }
    public void setCurrentUserId(int currentUserId) { this.currentUserId = currentUserId; }

    public int getTargetUserId() { return targetUserId; }
    public void setTargetUserId(int targetUserId) { this.targetUserId = targetUserId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getTargetAvatar() { return targetAvatar; }
    public void setTargetAvatar(String targetAvatar) { this.targetAvatar = targetAvatar; }

    public boolean isVideoCall() { return isVideoCall; }
    public void setVideoCall(boolean videoCall) { isVideoCall = videoCall; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
