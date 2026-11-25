package td.teladoum.baobab.repositories;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.daos.CallLogDao;
import td.teladoum.baobab.db.entities.CallLogEntity;
import td.teladoum.baobab.utils.TimeUtils;

public class CallLogRepository {
    private final CallLogDao callLogDao;

    public CallLogRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.callLogDao = db != null ? db.callLogDao() : null;
    }

    public void logCall(int currentUserId, int targetUserId, String targetName, String targetAvatar, boolean isVideoCall) {
        if (callLogDao == null) return;
        String now = TimeUtils.getCurrentLocalDateTime();
        CallLogEntity entity = new CallLogEntity(currentUserId, targetUserId, targetName, targetAvatar, isVideoCall, now);
        try {
            callLogDao.insertCallLog(entity);
        } catch (Exception ignored) {}
    }

    public List<CallLogEntity> getCallLogs(int currentUserId) {
        if (callLogDao == null) return new ArrayList<>();
        try {
            return callLogDao.getCallLogsForUser(currentUserId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void deleteCallLog(int id) {
        if (callLogDao == null) return;
        try {
            callLogDao.deleteCallLog(id);
        } catch (Exception ignored) {}
    }

    public void deleteAllCallLogs(int currentUserId) {
        if (callLogDao == null) return;
        try {
            callLogDao.deleteAllCallLogsForUser(currentUserId);
        } catch (Exception ignored) {}
    }
}
