package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.PostEntity;

@Dao
public interface PostDao {

    @Query("SELECT * FROM posts ORDER BY id DESC")
    List<PostEntity> getAllPosts();

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY id DESC")
    List<PostEntity> getPostsByUserId(int userId);

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    PostEntity getPostById(int postId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPost(PostEntity post);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPosts(List<PostEntity> posts);

    @Query("DELETE FROM posts WHERE id = :postId")
    void deletePostById(int postId);

    @Query("DELETE FROM posts")
    void deleteAll();
}
