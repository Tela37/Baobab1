package td.teladoum.baobab.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.CommentEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;

public class CommentSyncWorker extends Worker {

    public CommentSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            if (db == null) return Result.success();

            List<CommentEntity> unsyncedComments = db.commentDao().getUnsyncedComments();
            if (unsyncedComments != null && !unsyncedComments.isEmpty()) {
                for (CommentEntity entity : unsyncedComments) {
                    try {
                        FirebaseHelper.getInstance().syncCommentToRealtimeDb(entity.postId, entity.toModel());
                        db.commentDao().markCommentSynced(entity.id);
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
