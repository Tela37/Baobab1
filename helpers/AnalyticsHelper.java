package td.teladoum.baobab.helpers;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Helper centralisé pour le suivi en production avec Firebase Analytics
 * et le suivi des erreurs/crashs avec Firebase Crashlytics.
 */
public class AnalyticsHelper {

    public static void setCrashlyticsUserContext(int userId, String email) {
        try {
            if (userId > 0) {
                FirebaseCrashlytics.getInstance().setUserId(String.valueOf(userId));
            }
            if (email != null && !email.isEmpty()) {
                FirebaseCrashlytics.getInstance().setCustomKey("user_email", email);
            }
        } catch (Exception ignored) {}
    }

    public static void logException(Throwable throwable) {
        if (throwable == null) return;
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable);
        } catch (Exception ignored) {}
    }

    public static void logAppOpen(Context context) {
        if (context == null) return;
        try {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            bundle.putString("timestamp", String.valueOf(System.currentTimeMillis()));
            analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
        } catch (Exception ignored) {}
    }

    public static void logChatUse(Context context, int conversationId) {
        if (context == null) return;
        try {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            bundle.putInt("conversation_id", conversationId);
            bundle.putString("content_type", "chat_message");
            analytics.logEvent("chat_use", bundle);
        } catch (Exception ignored) {}
    }

    public static void logPostCreated(Context context, boolean hasImage, boolean hasVideo) {
        if (context == null) return;
        try {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            bundle.putBoolean("has_image", hasImage);
            bundle.putBoolean("has_video", hasVideo);
            bundle.putString("content_type", "publication");
            analytics.logEvent("post_create", bundle);
        } catch (Exception ignored) {}
    }

    public static void logScreenView(Context context, String screenName) {
        if (context == null || screenName == null) return;
        try {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName);
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        } catch (Exception ignored) {}
    }
}
