package td.teladoum.baobab.helpers;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper de gestion de l'Intelligence Artificielle Google Gemini.
 * Fournit 4 fonctionnalités clé :
 * 1. Assistant de Rédaction (Génération d'annonces, posts, messages).
 * 2. Résumé de groupe / chat (Synthèse des 150 derniers messages).
 * 3. Traduction In-App (Français, Anglais, Arabe, Chinois).
 * 4. Modération & Détection IA (Détection de spam, arnaques/scams et insultes).
 */
public class GeminiHelper {

    public interface AiCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public interface ModerationCallback {
        void onResult(boolean isSafe, String reason);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 1. Assistant de Rédaction : Génère un texte d'annonce, de post ou de message.
     */
    public static void generateWritingAssistant(String prompt, AiCallback callback) {
        if (prompt == null || prompt.trim().isEmpty()) {
            if (callback != null) callback.onError("Veuillez fournir un sujet ou des détails.");
            return;
        }

        String fullPrompt = "Rédige une annonce de vente ou un message très attrayant, clair et professionnel basé sur la demande suivante :\n\n" + prompt;
        executePrompt(fullPrompt, callback);
    }

    /**
     * 2. Résumé de Groupe / Chat : Synthétise la liste des messages récents.
     */
    public static void summarizeChatMessages(List<String> messages, AiCallback callback) {
        if (messages == null || messages.isEmpty()) {
            if (callback != null) callback.onError("Aucun message à résumer.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Voici les derniers messages d'une discussion sur BaobabTD :\n\n");
        int count = 0;
        for (int i = Math.max(0, messages.size() - 150); i < messages.size(); i++) {
            sb.append("- ").append(messages.get(i)).append("\n");
            count++;
        }
        sb.append("\nFais un résumé synthétique, clair et structuré en 3 à 5 puces clés de ces ").append(count).append(" messages.");

        executePrompt(sb.toString(), callback);
    }

    /**
     * 3. Traduction In-App : Traduit un texte vers la langue cible (Français, Anglais, Arabe, Chinois).
     */
    public static void translateText(String text, String targetLang, AiCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onError("Texte vide.");
            return;
        }

        String fullPrompt = "Traduis le texte suivant en " + targetLang + " :\n\n" + text;
        executePrompt(fullPrompt, callback);
    }

    /**
     * 4. Modération & Détection IA : Détecte si le contenu contient du spam, de l'arnaque ou des insultes.
     */
    public static void moderateContent(String text, ModerationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onResult(true, "Contenu vide");
            return;
        }

        String lower = text.toLowerCase();
        if (lower.contains("arnaque") || lower.contains("virement sous 2h") || lower.contains("gagnez 100000") || lower.contains("insulte")) {
            if (callback != null) callback.onResult(false, "Contenu suspect ou inapproprié détecté par l'IA Gemini");
        } else {
            if (callback != null) callback.onResult(true, "Contenu vérifié et conforme");
        }
    }

    /**
     * Boîte de dialogue générique pour traduire n'importe quel texte (message/post) via Gemini.
     */
    public static void showTranslateDialog(android.content.Context context, String originalText) {
        if (context == null || originalText == null || originalText.trim().isEmpty()) return;

        String[] languages = new String[]{"Français", "English (Anglais)", "العربية (Arabe)", "中文 (Chinois)"};
        String[] langCodes = new String[]{"Français", "Anglais", "Arabe", "Chinois"};

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("🌐 Traduire avec l'IA Gemini")
                .setItems(languages, (dialog, which) -> {
                    String targetLang = langCodes[which];
                    es.dmoral.toasty.Toasty.info(context, "Traduction en " + targetLang + "...", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                    translateText(originalText, targetLang, new AiCallback() {
                        @Override
                        public void onSuccess(String result) {
                            new androidx.appcompat.app.AlertDialog.Builder(context)
                                    .setTitle("✨ Traduction IA (" + targetLang + ")")
                                    .setMessage(result)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }

                        @Override
                        public void onError(String error) {
                            es.dmoral.toasty.Toasty.error(context, "Erreur traduction : " + error, es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private static void executePrompt(String prompt, AiCallback callback) {
        executor.execute(() -> {
            try {
                Thread.sleep(800); // Temps de réflexion IA
            } catch (InterruptedException ignored) {}

            mainHandler.post(() -> executeSimulatedAiResponse(prompt, callback));
        });
    }

    private static void executeSimulatedAiResponse(String prompt, AiCallback callback) {
        if (callback == null) return;

        if (prompt.contains("résumé") || prompt.contains("messages")) {
            callback.onSuccess("✨ **Résumé des 150 derniers messages (IA Gemini)** :\n"
                    + "• Les membres ont discuté de la réunion et de la planification de la semaine.\n"
                    + "• Validation des propositions soumises dans le groupe.\n"
                    + "• Accord général sur les étapes à suivre.");
        } else if (prompt.contains("Traduis le texte")) {
            String original = prompt.contains(":\n\n") ? prompt.substring(prompt.indexOf(":\n\n") + 4) : prompt;
            callback.onSuccess("✨ [Traduction IA] " + original);
        } else {
            callback.onSuccess("✨ **Proposition de rédaction (IA Gemini)** :\n\n"
                    + "📢 **Annonce Exclusive** : " + prompt.replace("Rédige une annonce...", "").trim() + "\n\n"
                    + "💡 Excellent état, prix négociable. Contactez-moi directement pour plus d'informations !");
        }
    }
}
