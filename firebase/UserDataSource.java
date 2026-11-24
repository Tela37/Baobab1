package td.teladoum.baobab.firebase;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import td.teladoum.baobab.models.User;

/**
 * DataSource distante dédiée aux données utilisateurs dans Firebase (Realtime DB + Firestore).
 */
public class UserDataSource {

    private final FirebaseDatabase realtimeDb;
    private final FirebaseFirestore firestore;

    public UserDataSource() {
        this.realtimeDb = FirebaseHelper.getInstance().getRealtimeDb();
        this.firestore = FirebaseHelper.getInstance().getFirestore();
    }

    public void syncUserToRealtimeDb(User user) {
        if (user == null) return;
        DatabaseReference ref = realtimeDb.getReference("users").child(String.valueOf(user.getId()));
        ref.setValue(user);
    }

    public void syncUserProfileToFirestore(User user) {
        if (user == null) return;
        FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
    }
}
