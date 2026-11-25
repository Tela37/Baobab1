package td.teladoum.baobab.utils;

/**
 * Classe de constantes globales pour l'application BaobabTD.
 */
public class Constants {

    public static final String PREF_NAME = "BaobabSession";
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int STORY_EXPIRATION_HOURS = 24;
    public static final int POST_EDIT_LIMIT_HOURS = 1;

    // Firebase DB References
    public static final String REF_USERS = "users";
    public static final String REF_POSTS = "posts";
    public static final String REF_STORIES = "stories";
    public static final String REF_MESSAGES = "messages";
    public static final String REF_CONVERSATIONS = "conversations";
    public static final String REF_MARKETPLACE = "marketplace_items";
    public static final String REF_NOTIFICATIONS = "notifications";
    public static final String REF_ANNOUNCEMENT = "app_announcement";
    public static final String REF_UPDATE_VERSION = "app_update_version";

    private Constants() {
        // Constructeur privé pour empêcher l'instanciation
    }
}
