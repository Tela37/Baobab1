package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.Post;

@Entity(tableName = "posts")
public class PostEntity {

    @PrimaryKey
    public int id;
    public int userId;
    public String authorName;
    public String authorProfileImage;
    public String content;
    public String imagePath;
    public String videoPath;
    public int likesCount;
    public boolean likedByCurrentUser;
    public String userReactionType;
    public int sharesCount;
    public int commentsCount;
    public boolean isSavedByCurrentUser;
    public boolean isPinned;
    public boolean isSensitive;
    public String createdAt;

    public PostEntity() {}

    public static PostEntity fromModel(Post post) {
        if (post == null) return null;
        PostEntity entity = new PostEntity();
        entity.id = post.getId();
        entity.userId = post.getUserId();
        entity.authorName = post.getAuthorName();
        entity.authorProfileImage = post.getAuthorProfileImage();
        entity.content = post.getContent();
        entity.imagePath = post.getImagePath();
        entity.videoPath = post.getVideoPath();
        entity.likesCount = post.getLikesCount();
        entity.likedByCurrentUser = post.isLikedByCurrentUser();
        entity.userReactionType = post.getUserReactionType();
        entity.sharesCount = post.getSharesCount();
        entity.commentsCount = post.getCommentsCount();
        entity.isSavedByCurrentUser = post.isSavedByCurrentUser();
        entity.isPinned = post.isPinned();
        entity.isSensitive = post.isSensitive();
        entity.createdAt = post.getCreatedAt();
        return entity;
    }

    public Post toModel() {
        Post p = new Post(id, userId, authorName, authorProfileImage, content, imagePath, videoPath, likesCount, likedByCurrentUser, createdAt);
        p.setUserReactionType(userReactionType);
        p.setSharesCount(sharesCount);
        p.setCommentsCount(commentsCount);
        p.setSavedByCurrentUser(isSavedByCurrentUser);
        p.setPinned(isPinned);
        p.setSensitive(isSensitive);
        return p;
    }
}
