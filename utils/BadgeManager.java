package td.teladoum.baobab.utils;

import java.util.Arrays;
import java.util.List;

public class BadgeManager {

    public static final String BADGE_NOUVEAU = "NOUVEAU_MEMBRE";
    public static final String BADGE_CONTRIBUTEUR = "CONTRIBUTEUR";
    public static final String BADGE_EXPERT = "EXPERT";
    public static final String BADGE_ENSEIGNANT = "ENSEIGNANT";
    public static final String BADGE_ENTREPRENEUR = "ENTREPRENEUR";

    public static class BadgeInfo {
        private final String key;
        private final String displayName;
        private final String description;

        public BadgeInfo(String key, String displayName, String description) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static List<BadgeInfo> getAllBadges() {
        return Arrays.asList(
            new BadgeInfo(BADGE_NOUVEAU, "⭐ Nouveau membre", "Nouveau sur la plateforme BaobabTD"),
            new BadgeInfo(BADGE_CONTRIBUTEUR, "🌟 Contributeur", "Actif avec de nombreuses publications et interactions"),
            new BadgeInfo(BADGE_EXPERT, "👑 Expert", "Membre hautement qualifié ou reconnu"),
            new BadgeInfo(BADGE_ENSEIGNANT, "👨‍🏫 Enseignant", "Professeur, formateur ou éducateur"),
            new BadgeInfo(BADGE_ENTREPRENEUR, "💼 Entrepreneur", "Entrepreneur ou commerçant du marché")
        );
    }

    public static String getBadgeDisplay(String key) {
        if (key == null) return "⭐ Nouveau membre";
        switch (key) {
            case BADGE_CONTRIBUTEUR: return "🌟 Contributeur";
            case BADGE_EXPERT: return "👑 Expert";
            case BADGE_ENSEIGNANT: return "👨‍🏫 Enseignant";
            case BADGE_ENTREPRENEUR: return "💼 Entrepreneur";
            case BADGE_NOUVEAU:
            default:
                return "⭐ Nouveau membre";
        }
    }

    public static String determineAutomaticBadge(String profession, int postsCount) {
        if (profession != null) {
            String profLower = profession.toLowerCase();
            if (profLower.contains("ens") || profLower.contains("prof") || profLower.contains("formateur") || profLower.contains("instituteur") || profLower.contains("enseignant")) {
                return BADGE_ENSEIGNANT;
            }
            if (profLower.contains("ent") || profLower.contains("pdg") || profLower.contains("commerce") || profLower.contains("vendeur") || profLower.contains("business") || profLower.contains("entrepreneur")) {
                return BADGE_ENTREPRENEUR;
            }
            if (profLower.contains("expert") || profLower.contains("ing") || profLower.contains("docteur") || profLower.contains("consultant")) {
                return BADGE_EXPERT;
            }
        }
        if (postsCount >= 5) {
            return BADGE_CONTRIBUTEUR;
        }
        return BADGE_NOUVEAU;
    }
}
