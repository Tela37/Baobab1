package td.teladoum.baobab.repositories;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import td.teladoum.baobab.firebase.FirebaseHelper;

/**
 * Repository pour les fonctionnalités système (mises à jour d'application, annonces globales, versioning).
 */
public class SystemRepository {

    private final FirebaseHelper firebaseHelper;

    public SystemRepository(Context context) {
        this.firebaseHelper = FirebaseHelper.getInstance();
    }

    public interface AnnouncementCallback {
        void onAnnouncementReceived(String text, boolean active);
    }

    public interface VersionCheckCallback {
        void onVersionChecked(int latestVersionCode, boolean isRequired);
    }

    public void listenToAppAnnouncement(AnnouncementCallback callback) {
        firebaseHelper.getRealtimeDb().getReference("app_announcement")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String text = snapshot.child("text").getValue(String.class);
                            Boolean active = snapshot.child("active").getValue(Boolean.class);
                            if (callback != null) {
                                callback.onAnnouncementReceived(text, Boolean.TRUE.equals(active));
                            }
                        } else if (callback != null) {
                            callback.onAnnouncementReceived(null, false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (callback != null) {
                            callback.onAnnouncementReceived(null, false);
                        }
                    }
                });
    }

    public void checkAppVersion(VersionCheckCallback callback) {
        firebaseHelper.getRealtimeDb().getReference("app_update_version")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long version = snapshot.child("versionCode").getValue(Long.class);
                            Boolean required = snapshot.child("isRequired").getValue(Boolean.class);
                            if (callback != null && version != null) {
                                callback.onVersionChecked(version.intValue(), Boolean.TRUE.equals(required));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }
}
