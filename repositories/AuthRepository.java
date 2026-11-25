package td.teladoum.baobab.repositories;

import android.content.Context;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import td.teladoum.baobab.firebase.FirebaseHelper;

/**
 * Repository dédié à l'authentification des utilisateurs (Firebase Auth & Session Management).
 */
public class AuthRepository {

    private final FirebaseHelper firebaseHelper;
    private final Context context;

    public AuthRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.firebaseHelper = FirebaseHelper.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseHelper.getAuth().getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        if (email == null || password == null) return;
        firebaseHelper.loginUser(email.trim(), password, listener);
    }

    public void register(String email, String password, OnCompleteListener<AuthResult> listener) {
        if (email == null || password == null) return;
        firebaseHelper.registerUser(email.trim(), password, listener);
    }

    public void sendEmailVerification(OnCompleteListener<Void> listener) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(listener);
        }
    }

    public void reloadUser(OnCompleteListener<Void> listener) {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(listener);
        }
    }

    public void sendPasswordResetEmail(String email, OnCompleteListener<Void> listener) {
        if (email == null || email.trim().isEmpty()) return;
        firebaseHelper.getAuth().sendPasswordResetEmail(email.trim()).addOnCompleteListener(listener);
    }

    public void signOut() {
        try {
            firebaseHelper.getAuth().signOut();
        } catch (Exception ignored) {}
    }
}
