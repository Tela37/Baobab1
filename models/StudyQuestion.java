package td.teladoum.baobab.models;

/**
 * Modèle représentant un sujet d'entraide ou une question scolaire posée par un élève.
 */
public class StudyQuestion {
    private int id;
    private int userId;
    private String authorName;
    private String authorProfileImage;
    private String title;
    private String content;
    private String category;
    private String imagePath;
    private String createdAt;
    private int repliesCount;

    public StudyQuestion() {}

    public StudyQuestion(int id, int userId, String authorName, String authorProfileImage, String title, String content, String category, String imagePath, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.authorName = authorName;
        this.authorProfileImage = authorProfileImage;
        this.title = title;
        this.content = content;
        this.category = category;
        this.imagePath = imagePath;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorProfileImage() { return authorProfileImage; }
    public void setAuthorProfileImage(String authorProfileImage) { this.authorProfileImage = authorProfileImage; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getRepliesCount() { return repliesCount; }
    public void setRepliesCount(int repliesCount) { this.repliesCount = repliesCount; }
}
