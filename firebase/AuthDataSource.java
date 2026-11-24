package td.teladoum.baobab.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * DataSource distante dédiée à l'authentification Firebase.
 */
public class AuthDataSource {

    private final FirebaseAuth auth;

    public AuthDataSource() {
        this.auth = FirebaseHelper.getInstance().getAuth();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void loginUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        if (email == null || password == null) return;
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void registerUser(String email, String password, OnCompleteListener<AuthResult> listener) {
        if (email == null || password == null) return;
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signOut() {
        try {
            auth.signOut();
        } catch (Exception ignored) {}
    }
}
