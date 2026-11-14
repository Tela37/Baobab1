package td.teladoum.baobab.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import td.teladoum.baobab.R;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Écran de démarrage (Splash Screen) affichant le logo 'baoba1' en plein écran.
 * Redirige automatiquement après 1,5 seconde vers AcceuilActivity (si déjà connecté)
 * ou vers LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        td.teladoum.baobab.helpers.PresenceManager.init(getApplication());

        // Appliquer le thème clair/sombre sauvegardé
        SharedPreferences prefs = getSharedPreferences("MessagesSession", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = darkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
        }

        setContentView(R.layout.activity_splash);

        // Configuration Plein Écran
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        SessionManager sessionManager = new SessionManager(this);
        td.teladoum.baobab.helpers.AnalyticsHelper.logAppOpen(this);
        if (sessionManager.isLoggedIn()) {
            td.teladoum.baobab.helpers.AnalyticsHelper.setCrashlyticsUserContext(sessionManager.getUserId(), sessionManager.getEmail());
            td.teladoum.baobab.repositories.UserRepository userRepo = new td.teladoum.baobab.repositories.UserRepository(this);
            userRepo.getUserById(sessionManager.getUserId());
            td.teladoum.baobab.firebase.FirebaseHelper.getInstance().subscribeToFcmTopics();
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                td.teladoum.baobab.firebase.FirebaseHelper.getInstance().syncFcmToken(sessionManager.getUserId(), task.getResult());
                            }
                        });
            } catch (Exception ignored) {}
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            Intent intent;
            if (sessionManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, AcceuilActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}

