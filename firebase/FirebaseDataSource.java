package td.teladoum.baobab.firebase;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import td.teladoum.baobab.models.User;

/**
 * Couche unique centralisée (FirebaseDataSource) pour :
 * 1. Authentication (FirebaseAuth)
 * 2. Firestore (FirebaseFirestore)
 * 3. Realtime Database (FirebaseDatabase)
 * 4. FCM (FirebaseMessaging)
 */
public class FirebaseDataSource {

    private static final String TAG = "FirebaseDataSource";
    private static volatile FirebaseDataSource instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseDatabase realtimeDb;
    private final FirebaseMessaging messaging;

    private FirebaseDataSource() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.realtimeDb = FirebaseDatabase.getInstance();
        this.messaging = FirebaseMessaging.getInstance();
        try {
            realtimeDb.setPersistenceEnabled(true);
        } catch (Exception ignored) {}
    }

    public static synchronized FirebaseDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseDataSource();
        }
        return instance;
    }

    // --- 1. Authentication ---

    public FirebaseAuth getAuth() {
        return auth;
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

    // --- 2. Firestore ---

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public void syncUserProfileToFirestore(User user) {
        if (user == null) return;
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("first_name", user.getFirstName());
        map.put("last_name", user.getLastName());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("dob", user.getDob());
        map.put("neighborhood", user.getNeighborhood());
        map.put("city", user.getCity());
        map.put("country", user.getCountry());
        map.put("profile_image", user.getProfileImage());
        map.put("profession", user.getProfession());
        map.put("relationship_status", user.getRelationshipStatus());
        map.put("gender", user.getGender());
        map.put("bio", user.getBio());
        map.put("is_admin", user.isAdmin());
        map.put("is_verified", user.isVerified());
        map.put("firebaseUid", user.getFirebaseUid());

        if (user.getEmail() != null) {
            firestore.collection("users").document(String.valueOf(user.getId())).set(map)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful() && task.getException() != null) {
                            Log.e(TAG, "Firestore user sync failed: " + task.getException().getMessage());
                        }
                    });
        }
    }

    public void saveRewardBadgeToFirestore(int userId, int activeBadges, long timestamp) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("activeBadges", activeBadges);
        map.put("timestamp", timestamp);
        firestore.collection("rewards").document(userId + "_" + timestamp).set(map);
    }

    // --- 3. Realtime Database ---

    public FirebaseDatabase getRealtimeDb() {
        return realtimeDb;
    }

    // --- 4. FCM (Firebase Cloud Messaging) ---

    public FirebaseMessaging getMessaging() {
        return messaging;
    }

    public void syncFcmToken(int userId, String token) {
        if (userId <= 0 || token == null || token.isEmpty()) return;
        try {
            realtimeDb.getReference("users").child(String.valueOf(userId)).child("fcmToken").setValue(token);
            Map<String, Object> map = new HashMap<>();
            map.put("fcmToken", token);
            firestore.collection("users").document(String.valueOf(userId)).set(map, com.google.firebase.firestore.SetOptions.merge());
        } catch (Exception ignored) {}
    }

    public void subscribeToFcmTopics() {
        try {
            messaging.subscribeToTopic("announcements");
            messaging.subscribeToTopic("updates");
        } catch (Exception ignored) {}
    }
}
