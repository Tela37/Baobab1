package td.teladoum.baobab.utils;

import android.content.Context;
import android.content.Intent;

import td.teladoum.baobab.activities.PagePersonnelleActivity;
import td.teladoum.baobab.activities.User2PageActivity;

/**
 * Utilitaire de navigation pour rediriger l'utilisateur vers :
 * - PagePersonnelleActivity si c'est l'utilisateur connecté lui-même.
 * - User2PageActivity si c'est un autre utilisateur.
 */
public class NavigationHelper {

    public static void openUserProfile(Context context, int targetUserId) {
        if (context == null) return;
        SessionManager sessionManager = new SessionManager(context);
        int currentUserId = sessionManager.getUserId();

        if (targetUserId == currentUserId && targetUserId != -1) {
            Intent intent = new Intent(context, PagePersonnelleActivity.class);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(context, User2PageActivity.class);
            intent.putExtra("user_id", targetUserId);
            context.startActivity(intent);
        }
    }
}
