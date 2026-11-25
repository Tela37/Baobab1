package td.teladoum.baobab.workers;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.OneTimeWorkRequest;

import java.util.concurrent.TimeUnit;

public class BaobabSyncScheduler {

    private static final String UNIQUE_MESSAGE_SYNC = "baobab_message_sync_work";
    private static final String UNIQUE_POST_SYNC = "baobab_post_sync_work";
    private static final String UNIQUE_USER_SYNC = "baobab_user_sync_work";
    private static final String UNIQUE_COMMENT_SYNC = "baobab_comment_sync_work";

    public static void scheduleSync(Context context) {
        if (context == null) return;
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest messageWork = new PeriodicWorkRequest.Builder(
                    MessageSyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            PeriodicWorkRequest postWork = new PeriodicWorkRequest.Builder(
                    PostSyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            PeriodicWorkRequest userWork = new PeriodicWorkRequest.Builder(
                    UserSyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            PeriodicWorkRequest commentWork = new PeriodicWorkRequest.Builder(
                    CommentSyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();

            WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
            workManager.enqueueUniquePeriodicWork(UNIQUE_MESSAGE_SYNC, ExistingPeriodicWorkPolicy.KEEP, messageWork);
            workManager.enqueueUniquePeriodicWork(UNIQUE_POST_SYNC, ExistingPeriodicWorkPolicy.KEEP, postWork);
            workManager.enqueueUniquePeriodicWork(UNIQUE_USER_SYNC, ExistingPeriodicWorkPolicy.KEEP, userWork);
            workManager.enqueueUniquePeriodicWork(UNIQUE_COMMENT_SYNC, ExistingPeriodicWorkPolicy.KEEP, commentWork);
        } catch (Exception ignored) {}
    }

    public static void triggerImmediateSync(Context context) {
        if (context == null) return;
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest messageWork = new OneTimeWorkRequest.Builder(MessageSyncWorker.class)
                    .setConstraints(constraints)
                    .build();

            OneTimeWorkRequest postWork = new OneTimeWorkRequest.Builder(PostSyncWorker.class)
                    .setConstraints(constraints)
                    .build();

            OneTimeWorkRequest userWork = new OneTimeWorkRequest.Builder(UserSyncWorker.class)
                    .setConstraints(constraints)
                    .build();

            OneTimeWorkRequest commentWork = new OneTimeWorkRequest.Builder(CommentSyncWorker.class)
                    .setConstraints(constraints)
                    .build();

            WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
            workManager.enqueue(messageWork);
            workManager.enqueue(postWork);
            workManager.enqueue(userWork);
            workManager.enqueue(commentWork);
        } catch (Exception ignored) {}
    }
}
