package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.Story;

@Entity(tableName = "stories")
public class StoryEntity {

    @PrimaryKey
    public int id;
    public int userId;
    public String userName;
    public String userAvatar;
    public String imagePath;
    public String caption;
    public String createdAt;

    public StoryEntity() {}

    public static StoryEntity fromModel(Story story) {
        if (story == null) return null;
        StoryEntity entity = new StoryEntity();
        entity.id = story.getId();
        entity.userId = story.getUserId();
        entity.userName = story.getUserName();
        entity.userAvatar = story.getUserAvatar();
        entity.imagePath = story.getImagePath();
        entity.caption = story.getCaption();
        entity.createdAt = story.getCreatedAt();
        return entity;
    }

    public Story toModel() {
        return new Story(id, userId, userName, userAvatar, imagePath, caption, createdAt);
    }
}
