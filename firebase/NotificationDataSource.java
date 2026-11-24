package td.teladoum.baobab.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import td.teladoum.baobab.models.AppNotification;

/**
 * DataSource distante dédiée aux notifications dans Firebase Realtime Database.
 */
public class NotificationDataSource {

    private final FirebaseDatabase realtimeDb;

    public NotificationDataSource() {
        this.realtimeDb = FirebaseHelper.getInstance().getRealtimeDb();
    }

    public void syncNotificationToRealtimeDb(AppNotification notif) {
        if (notif == null) return;
        FirebaseHelper.getInstance().syncNotificationToRealtimeDb(notif);
    }

    public void listenToNotifications(int userId, ValueEventListener listener) {
        if (userId <= 0 || listener == null) return;
        realtimeDb.getReference("notifications").child(String.valueOf(userId)).addValueEventListener(listener);
    }
}
