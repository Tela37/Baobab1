package td.teladoum.baobab.models;

public class CallSignal {
    private int callerId;
    private String callerName;
    private String callerAvatar;
    private boolean isVideoCall;
    private long timestamp;

    public CallSignal() {}

    public CallSignal(int callerId, String callerName, String callerAvatar, boolean isVideoCall, long timestamp) {
        this.callerId = callerId;
        this.callerName = callerName;
        this.callerAvatar = callerAvatar;
        this.isVideoCall = isVideoCall;
        this.timestamp = timestamp;
    }

    public int getCallerId() { return callerId; }
    public void setCallerId(int callerId) { this.callerId = callerId; }

    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }

    public String getCallerAvatar() { return callerAvatar; }
    public void setCallerAvatar(String callerAvatar) { this.callerAvatar = callerAvatar; }

    public boolean isVideoCall() { return isVideoCall; }
    public void setVideoCall(boolean videoCall) { isVideoCall = videoCall; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
