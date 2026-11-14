package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.MessageEntity;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id ASC")
    List<MessageEntity> getMessagesForConversation(int conversationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    @Query("DELETE FROM messages WHERE id = :messageId")
    void deleteMessageById(int messageId);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteMessagesByConversationId(int conversationId);
}
