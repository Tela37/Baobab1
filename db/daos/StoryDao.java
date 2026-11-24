package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.StoryEntity;

@Dao
public interface StoryDao {

    @Query("SELECT * FROM stories ORDER BY id DESC")
    List<StoryEntity> getAllStories();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStories(List<StoryEntity> stories);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStory(StoryEntity story);

    @Query("DELETE FROM stories WHERE id = :id")
    void deleteStory(int id);

    @Query("DELETE FROM stories")
    void deleteAll();
}
