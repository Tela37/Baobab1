package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.NotificationEntity;

@Dao
public interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE recipientId = :recipientId ORDER BY id DESC")
    List<NotificationEntity> getNotificationsForUser(int recipientId);

    @Query("SELECT COUNT(*) FROM notifications WHERE recipientId = :recipientId AND isRead = 0")
    int getUnreadCount(int recipientId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotifications(List<NotificationEntity> notifications);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotification(NotificationEntity notification);

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientId = :recipientId")
    void markAllAsRead(int recipientId);

    @Query("DELETE FROM notifications WHERE recipientId = :recipientId")
    void deleteNotificationsForUser(int recipientId);
}
