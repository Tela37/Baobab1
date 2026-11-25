package td.teladoum.baobab.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.MessageEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;

public class MessageSyncWorker extends Worker {

    public MessageSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            if (db == null) return Result.success();

            List<MessageEntity> unsyncedMessages = db.messageDao().getUnsyncedMessages();
            if (unsyncedMessages != null && !unsyncedMessages.isEmpty()) {
                for (MessageEntity entity : unsyncedMessages) {
                    try {
                        FirebaseHelper.getInstance().syncMessageToRealtimeDb(entity.conversationId, entity.toModel());
                        db.messageDao().markMessageSynced(entity.id);
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
