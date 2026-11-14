package td.teladoum.baobab.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Helper de gestion dynamique de la langue de l'application (Français, Anglais, Arabe).
 * Gère la persistance de la langue dans SharedPreferences et l'application de la Locale.
 */
public class LocaleHelper {

    private static final String PREF_NAME = "baobab_language_pref";
    private static final String KEY_SELECTED_LANGUAGE = "selected_language";

    /**
     * Applique la langue enregistrée au contexte spécifié.
     * @return Le contexte mis à jour avec la Locale configurée.
     */
    public static String getDefaultSystemLanguage() {
        try {
            String sysLang = Locale.getDefault().getLanguage();
            if (sysLang != null) {
                sysLang = sysLang.toLowerCase().trim();
                if (sysLang.startsWith("fr")) return "fr";
                if (sysLang.startsWith("ar")) return "ar";
                if (sysLang.startsWith("en")) return "en";
            }
        } catch (Exception ignored) {}
        return "en";
    }

    /**
     * Applique la langue enregistrée au contexte spécifié.
     * @param context Le contexte d'activité.
     * @return Le contexte mis à jour avec la Locale configurée.
     */
    public static Context onAttach(Context context) {
        String lang = getPersistedLanguage(context, getDefaultSystemLanguage());
        return setLocale(context, lang);
    }

    /**
     * Obtient le code langue actuellement enregistré ("fr", "en", "ar").
     * @param context Le contexte.
     * @param defaultLanguage La langue par défaut ("fr").
     * @return Le code langue enregistré.
     */
    public static String getPersistedLanguage(Context context, String defaultLanguage) {
        if (context == null) return getDefaultSystemLanguage();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_SELECTED_LANGUAGE)) {
            return getDefaultSystemLanguage();
        }
        return prefs.getString(KEY_SELECTED_LANGUAGE, getDefaultSystemLanguage());
    }

    /**
     * Définit et enregistre la nouvelle langue de l'application ("fr", "en", "ar").
     * @param context Le contexte.
     * @param languageCode Le code langue ("fr", "en", "ar").
     * @return Le contexte mis à jour.
     */
    public static Context setLocale(Context context, String languageCode) {
        persistLanguage(context, languageCode);

        // Applique la locale au système AppCompat
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocales);

        return updateResources(context, languageCode);
    }

    private static void persistLanguage(Context context, String languageCode) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, languageCode).apply();
    }

    private static Context updateResources(Context context, String languageCode) {
        if (context == null) return context;

        Locale locale = Locale.forLanguageTag(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            return context.createConfigurationContext(config);
        } else {
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }
}

