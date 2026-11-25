package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.CommentEntity;

@Dao
public interface CommentDao {

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY id ASC")
    List<CommentEntity> getCommentsForPost(int postId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(CommentEntity comment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComments(List<CommentEntity> comments);

    @Query("DELETE FROM comments WHERE id = :commentId")
    void deleteCommentById(int commentId);

    @Query("DELETE FROM comments WHERE postId = :postId")
    void deleteCommentsByPostId(int postId);

    @Query("SELECT * FROM comments WHERE isSynced = 0")
    List<CommentEntity> getUnsyncedComments();

    @Query("UPDATE comments SET isSynced = 1 WHERE id = :commentId")
    void markCommentSynced(int commentId);
}
