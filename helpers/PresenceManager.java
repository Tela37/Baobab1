package td.teladoum.baobab.helpers;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.SessionManager;
import td.teladoum.baobab.utils.TimeUtils;

/**
 * Gestionnaire de présence utilisateur en arrière-plan / premier plan.
 * Aligné sur le comportement Messenger : passe 'isOnline' à false et met à jour 'lastSeen'
 * dès que l'application passe en arrière-plan ou est quittée.
 */
public class PresenceManager implements Application.ActivityLifecycleCallbacks {

    private static PresenceManager instance;
    private int startedActivities = 0;
    private final Context appContext;
    private final SessionManager sessionManager;
    private final UserRepository userRepository;

    private PresenceManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
        this.userRepository = new UserRepository(appContext);
    }

    public static synchronized void init(Application application) {
        if (instance == null && application != null) {
            instance = new PresenceManager(application);
            application.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static PresenceManager getInstance() {
        return instance;
    }

    public static void updatePresence(Context context, boolean isOnline) {
        if (context == null) return;
        SessionManager sm = new SessionManager(context);
        int userId = sm.getUserId();
        if (userId <= 0) return;

        String now = TimeUtils.getCurrentLocalDateTime();
        UserRepository repo = new UserRepository(context);
        User user = repo.getUserById(userId);
        if (user != null) {
            user.setOnline(isOnline);
            user.setLastSeen(now);
            repo.cacheUser(user);
        }

        try {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(String.valueOf(userId));
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", isOnline);
            updates.put("online", isOnline);
            updates.put("lastSeen", now);

            userRef.updateChildren(updates);

            if (isOnline) {
                userRef.child("isOnline").onDisconnect().setValue(false);
                userRef.child("online").onDisconnect().setValue(false);
                userRef.child("lastSeen").onDisconnect().setValue(now);

                try {
                    String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
                    FirebaseDatabase.getInstance().getReference("daily_logins")
                            .child(todayStr)
                            .child(String.valueOf(userId))
                            .setValue(System.currentTimeMillis());
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (startedActivities == 0) {
            // L'application passe au premier plan
            int userId = sessionManager.getUserId();
            if (userId > 0) {
                updatePresence(activity, true);
            }
        }
        startedActivities++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        startedActivities--;
        if (startedActivities <= 0) {
            startedActivities = 0;
            // L'application passe en arrière-plan / est quittée
            int userId = sessionManager.getUserId();
            if (userId > 0) {
                updatePresence(activity, false);
            }
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}
