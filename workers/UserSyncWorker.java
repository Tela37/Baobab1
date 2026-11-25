package td.teladoum.baobab.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.UserEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;

public class UserSyncWorker extends Worker {

    public UserSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            if (db == null) return Result.success();

            List<UserEntity> unsyncedUsers = db.userDao().getUnsyncedUsers();
            if (unsyncedUsers != null && !unsyncedUsers.isEmpty()) {
                for (UserEntity entity : unsyncedUsers) {
                    try {
                        td.teladoum.baobab.models.User model = entity.toModel();
                        FirebaseHelper.getInstance().syncUserProfileToFirestore(model);
                        FirebaseHelper.getInstance().syncUserToRealtimeDb(model);
                        db.userDao().markUserSynced(entity.id);
                    } catch (Exception e) {
                        // Keep unsynced for retry
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
