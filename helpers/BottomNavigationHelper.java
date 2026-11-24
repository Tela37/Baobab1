package td.teladoum.baobab.helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import td.teladoum.baobab.R;
import td.teladoum.baobab.activities.AcceuilActivity;
import td.teladoum.baobab.activities.AmiActivity;
import td.teladoum.baobab.activities.MarketplaceActivity;
import td.teladoum.baobab.activities.NotificationActivity;
import td.teladoum.baobab.activities.ProfileActivity;
import td.teladoum.baobab.ui.learning.LibraryActivity;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Assistant de gestion centralisée pour la barre de navigation inférieure (Bottom Navigation Bar).
 */
public class BottomNavigationHelper {

    public static final String TAB_HOME = "HOME";
    public static final String TAB_LIBRARY = "LIBRARY";
    public static final String TAB_FRIENDS = "FRIENDS";
    public static final String TAB_MARKETPLACE = "MARKETPLACE";
    public static final String TAB_NOTIFICATIONS = "NOTIFICATIONS";
    public static final String TAB_PROFILE = "PROFILE";

    public static void setupBottomNavigation(Activity activity, String activeTab) {
        if (activity == null) return;

        View btnHome = activity.findViewById(R.id.btnNavHome);
        View btnLibrary = activity.findViewById(R.id.btnNavLibrary);
        View btnFriends = activity.findViewById(R.id.btnNavFriends);
        View btnMarketplace = activity.findViewById(R.id.btnNavMarketplace);
        View btnNotifications = activity.findViewById(R.id.btnNavNotifications);
        View btnProfile = activity.findViewById(R.id.btnNavProfile);

        ImageView imgHome = activity.findViewById(R.id.imgNavHome);
        ImageView imgLibrary = activity.findViewById(R.id.imgNavLibrary);
        ImageView imgFriends = activity.findViewById(R.id.imgNavFriends);
        ImageView imgMarketplace = activity.findViewById(R.id.imgNavMarketplace);
        ImageView imgNotifications = activity.findViewById(R.id.imgNavNotifications);
        ImageView imgProfileAvatar = activity.findViewById(R.id.imgNavProfileAvatar);
        TextView tvNotificationBadge = activity.findViewById(R.id.tvNavNotificationBadge);

        int activeColor = Color.parseColor("#2196F3");
        int inactiveColor = Color.parseColor("#757575");

        if (imgHome != null) imgHome.setColorFilter(TAB_HOME.equals(activeTab) ? activeColor : inactiveColor);
        if (imgLibrary != null) imgLibrary.setColorFilter(TAB_LIBRARY.equals(activeTab) ? activeColor : inactiveColor);
        if (imgFriends != null) imgFriends.setColorFilter(TAB_FRIENDS.equals(activeTab) ? activeColor : inactiveColor);
        if (imgMarketplace != null) imgMarketplace.setColorFilter(TAB_MARKETPLACE.equals(activeTab) ? activeColor : inactiveColor);
        if (imgNotifications != null) imgNotifications.setColorFilter(TAB_NOTIFICATIONS.equals(activeTab) ? activeColor : inactiveColor);

        // Chargement de la photo de profil de l'utilisateur sur l'icône Profil
        SessionManager sessionManager = new SessionManager(activity);
        int userId = sessionManager.getUserId();

        if (imgProfileAvatar != null && userId > 0) {
            UserRepository userRepo = new UserRepository(activity);
            td.teladoum.baobab.models.User user = userRepo.getUserById(userId);
            String avatar = (user != null && user.getProfileImage() != null) ? user.getProfileImage() : sessionManager.getProfileImage();
            ImageUtils.loadProfileImage(activity, avatar, imgProfileAvatar);
        }

        // Chargement du badge réactif de notifications
        if (tvNotificationBadge != null && userId > 0) {
            NotificationRepository notifRepo = new NotificationRepository(activity);
            int unread = notifRepo.getUnreadCount(userId);
            if (unread > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }

        // Écouteurs de clics pour la navigation directe
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                if (!TAB_HOME.equals(activeTab)) {
                    Intent intent = new Intent(activity, AcceuilActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnLibrary != null) {
            btnLibrary.setOnClickListener(v -> {
                if (!TAB_LIBRARY.equals(activeTab)) {
                    Intent intent = new Intent(activity, LibraryActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnFriends != null) {
            btnFriends.setOnClickListener(v -> {
                if (!TAB_FRIENDS.equals(activeTab)) {
                    Intent intent = new Intent(activity, AmiActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnMarketplace != null) {
            btnMarketplace.setOnClickListener(v -> {
                if (!TAB_MARKETPLACE.equals(activeTab)) {
                    Intent intent = new Intent(activity, MarketplaceActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                if (!TAB_NOTIFICATIONS.equals(activeTab)) {
                    Intent intent = new Intent(activity, NotificationActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (!TAB_PROFILE.equals(activeTab)) {
                    Intent intent = new Intent(activity, ProfileActivity.class);
                    activity.startActivity(intent);
                }
            });
        }
    }
}
