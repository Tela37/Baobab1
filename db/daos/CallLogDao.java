package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;
import td.teladoum.baobab.db.entities.CallLogEntity;

@Dao
public interface CallLogDao {
    @Insert
    void insertCallLog(CallLogEntity callLog);

    @Query("SELECT * FROM call_logs WHERE currentUserId = :userId ORDER BY id DESC")
    List<CallLogEntity> getCallLogsForUser(int userId);

    @Query("DELETE FROM call_logs WHERE id = :id")
    void deleteCallLog(int id);

    @Query("DELETE FROM call_logs WHERE currentUserId = :userId")
    void deleteAllCallLogsForUser(int userId);
}
