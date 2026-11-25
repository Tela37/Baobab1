package td.teladoum.baobab.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.PostEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;

public class PostSyncWorker extends Worker {

    public PostSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            if (db == null) return Result.success();

            List<PostEntity> unsyncedPosts = db.postDao().getUnsyncedPosts();
            if (unsyncedPosts != null && !unsyncedPosts.isEmpty()) {
                for (PostEntity entity : unsyncedPosts) {
                    try {
                        FirebaseHelper.getInstance().syncPostToRealtimeDb(entity.toModel());
                        db.postDao().markPostSynced(entity.id);
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
