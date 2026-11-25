package td.teladoum.baobab.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utilitaire de gestion des préférences partagées (SharedPreferences).
 */
public class PreferenceManager {

    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context, String prefName) {
        Context ctx = context != null ? context.getApplicationContext() : null;
        this.sharedPreferences = ctx != null ? ctx.getSharedPreferences(prefName, Context.MODE_PRIVATE) : null;
    }

    public void putString(String key, String value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        if (sharedPreferences == null) return defaultValue;
        return sharedPreferences.getString(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (sharedPreferences == null) return defaultValue;
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void putInt(String key, int value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        if (sharedPreferences == null) return defaultValue;
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void remove(String key) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().remove(key).apply();
    }

    public void clear() {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().clear().apply();
    }
}
