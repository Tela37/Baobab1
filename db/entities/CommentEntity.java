package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.Comment;

@Entity(tableName = "comments")
public class CommentEntity {

    @PrimaryKey
    public int id;
    public int postId;
    public Integer parentId;
    public int userId;
    public String authorName;
    public String authorProfileImage;
    public String content;
    public String imagePath;
    public int likesCount;
    public boolean isLikedByCurrentUser;
    public String userReactionType;
    public String createdAt;
    public boolean isSynced = true;

    public CommentEntity() {}

    public static CommentEntity fromModel(Comment c) {
        return fromModel(c, true);
    }

    public static CommentEntity fromModel(Comment c, boolean isSynced) {
        if (c == null) return null;
        CommentEntity entity = new CommentEntity();
        entity.id = c.getId();
        entity.postId = c.getPostId();
        entity.parentId = c.getParentId();
        entity.userId = c.getUserId();
        entity.authorName = c.getAuthorName();
        entity.authorProfileImage = c.getAuthorProfileImage();
        entity.content = c.getContent();
        entity.imagePath = c.getImagePath();
        entity.likesCount = c.getLikesCount();
        entity.isLikedByCurrentUser = c.isLikedByCurrentUser();
        entity.userReactionType = c.getUserReactionType();
        entity.createdAt = c.getCreatedAt();
        entity.isSynced = isSynced;
        return entity;
    }

    public Comment toModel() {
        Comment c = new Comment(id, postId, parentId, userId, authorName, authorProfileImage, content, likesCount, isLikedByCurrentUser, createdAt);
        c.setImagePath(imagePath);
        c.setUserReactionType(userReactionType);
        return c;
    }
}
