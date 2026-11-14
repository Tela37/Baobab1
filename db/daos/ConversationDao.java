package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.ConversationEntity;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY conversationId DESC")
    List<ConversationEntity> getAllConversations();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversation(ConversationEntity conversation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversations(List<ConversationEntity> conversations);

    @Query("DELETE FROM conversations WHERE conversationId = :conversationId")
    void deleteConversationById(int conversationId);

    @Query("DELETE FROM conversations")
    void deleteAll();
}
