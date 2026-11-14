package td.teladoum.baobab.notifications;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Service FCM pour la réception des notifications ciblées (Tokens d'appareil)
 * et par canal/sujet (Topics), évitant la diffusion globale à tous les utilisateurs.
 */
public class BaobabFirebaseMessagingService extends FirebaseMessagingService {

    private static final String PREFS_FCM = "BaobabFcmPrefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SharedPreferences prefs = getSharedPreferences(PREFS_FCM, MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            FirebaseHelper.getInstance().syncFcmToken(sessionManager.getUserId(), token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "BaobabTD";
        String body = "";
        int conversationId = -1;
        String senderName = "Correspondant";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            if (data.containsKey("title") && data.get("title") != null) {
                title = data.get("title");
            }
            if (data.containsKey("body") && data.get("body") != null) {
                body = data.get("body");
            }
            if (data.containsKey("conversation_id")) {
                String convIdStr = data.get("conversation_id");
                if (convIdStr != null) {
                    try {
                        conversationId = Integer.parseInt(convIdStr);
                    } catch (Exception ignored) {}
                }
            }
            if (data.containsKey("sender_name") && data.get("sender_name") != null) {
                senderName = data.get("sender_name");
            }
        }

        if (body != null && !body.trim().isEmpty()) {
            NotificationHelper.showNotification(this, title, body, conversationId, senderName);
        }
    }
}
