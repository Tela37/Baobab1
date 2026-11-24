package td.teladoum.baobab.models;

/**
 * Modèle représentant une réponse d'aide apportée à un sujet d'entraide scolaire.
 */
public class StudyReply {
    private int id;
    private int questionId;
    private int userId;
    private String authorName;
    private String authorProfileImage;
    private String content;
    private String createdAt;
    private int likesCount;
    private boolean isReported;
    private String imagePath;
    private boolean isLikedByCurrentUser;
    private String userReactionType;

    public StudyReply() {}

    public StudyReply(int id, int questionId, int userId, String authorName, String authorProfileImage, String content, String createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorProfileImage = authorProfileImage;
        this.content = content;
        this.createdAt = createdAt;
    }

    public StudyReply(int id, int questionId, int userId, String authorName, String authorProfileImage, String content, String createdAt, String imagePath) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorProfileImage = authorProfileImage;
        this.content = content;
        this.createdAt = createdAt;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorProfileImage() { return authorProfileImage; }
    public void setAuthorProfileImage(String authorProfileImage) { this.authorProfileImage = authorProfileImage; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public boolean isReported() { return isReported; }
    public void setReported(boolean reported) { isReported = reported; }

    public boolean isLikedByCurrentUser() { return isLikedByCurrentUser; }
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { isLikedByCurrentUser = likedByCurrentUser; }

    public String getUserReactionType() { return userReactionType; }
    public void setUserReactionType(String userReactionType) { this.userReactionType = userReactionType; }
}
