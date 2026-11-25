package td.teladoum.baobab;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.database.FirebaseDatabase;

import td.teladoum.baobab.helpers.AnalyticsHelper;
import td.teladoum.baobab.helpers.PresenceManager;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Classe Application globale pour initialiser les préférences (ex: Mode Sombre global).
 */
public class BaobabApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {}

        try {
            SharedPreferences prefs = getSharedPreferences("MessagesSession", MODE_PRIVATE);
            boolean darkMode = prefs.getBoolean("dark_mode", false);
            int targetMode = darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
            }
        } catch (Exception ignored) {}

        try {
            PresenceManager.init(this);
            AnalyticsHelper.logAppOpen(this);
            new Thread(() -> {
                try {
                    SessionManager sessionManager = new SessionManager(this);
                    if (sessionManager.isLoggedIn()) {
                        AnalyticsHelper.setCrashlyticsUserContext(sessionManager.getUserId(), sessionManager.getEmail());
                        UserRepository userRepo = new UserRepository(this);
                        userRepo.getUserById(sessionManager.getUserId());
                        userRepo.migrateFirebaseUid();
                        td.teladoum.baobab.firebase.FirebaseHelper.getInstance().subscribeToFcmTopics();
                        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        td.teladoum.baobab.firebase.FirebaseHelper.getInstance().syncFcmToken(sessionManager.getUserId(), task.getResult());
                                    }
                                });
                    }
                } catch (Exception ignored) {}
            }).start();
        } catch (Exception ignored) {}
    }
}
