package td.teladoum.baobab.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import td.teladoum.baobab.R;

/**
 * Helper de gestion de l'Intelligence Artificielle Google Gemini (SDK officiel).
 * Fournit 4 fonctionnalités clé :
 * 1. Assistant de Rédaction (Génération d'annonces, posts, messages).
 * 2. Résumé de groupe / chat (Synthèse des messages).
 * 3. Traduction In-App (Français, Anglais, Arabe, Chinois).
 * 4. Modération & Détection IA.
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
     * 1. Assistant de Rédaction : Génère un texte d'annonce, de post ou de message via Gemini.
     */
    public static void generateWritingAssistant(Context context, String prompt, AiCallback callback) {
        if (prompt == null || prompt.trim().isEmpty()) {
            if (callback != null) callback.onError("Veuillez fournir un sujet ou des détails.");
            return;
        }

        String fullPrompt = "Rédige une annonce de vente ou un message très attrayant, clair et professionnel basé sur la demande suivante :\n\n" + prompt;
        executePrompt(context, fullPrompt, callback);
    }

    /**
     * 2. Résumé de Groupe / Chat : Synthétise la liste des messages récents via Gemini.
     */
    public static void summarizeChatMessages(Context context, List<String> messages, AiCallback callback) {
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

        executePrompt(context, sb.toString(), callback);
    }

    /**
     * 3. Traduction In-App : Traduit un texte vers la langue cible via Gemini.
     */
    public static void translateText(Context context, String text, String targetLang, AiCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) callback.onError("Texte vide.");
            return;
        }

        String fullPrompt = "Traduis fidèlement et naturellement le texte suivant en " + targetLang + " (réponds uniquement avec la traduction) :\n\n" + text;
        executePrompt(context, fullPrompt, callback);
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
    public static void showTranslateDialog(Context context, String originalText) {
        if (context == null || originalText == null || originalText.trim().isEmpty()) return;

        String[] languages = new String[]{"Français", "English (Anglais)", "العربية (Arabe)", "中文 (Chinois)"};
        String[] langCodes = new String[]{"Français", "Anglais", "Arabe", "Chinois"};

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("🌐 Traduire avec l'IA Gemini")
                .setItems(languages, (dialog, which) -> {
                    String targetLang = langCodes[which];
                    es.dmoral.toasty.Toasty.info(context, "Traduction en " + targetLang + "...", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                    translateText(context, originalText, targetLang, new AiCallback() {
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

    /**
     * Assistant d'Exercice IA : Résout un problème ou un exercice scolaire étape par étape avec pédagogie.
     */
    public static void solveExercise(Context context, String exercisePrompt, AiCallback callback) {
        if (exercisePrompt == null || exercisePrompt.trim().isEmpty()) {
            if (callback != null) callback.onError("Veuillez saisir l'énoncé de l'exercice.");
            return;
        }

        String fullPrompt = "Agis en tant que professeur particulier expert et bienveillant. Résous l'exercice ou réponds à la question suivante de manière claire, pédagogique et structurée étape par étape :\n\n" + exercisePrompt;
        executePrompt(context, fullPrompt, callback);
    }

    public static void showExerciseSolverDialog(Context context) {
        if (context == null) return;

        android.widget.EditText etInput = new android.widget.EditText(context);
        etInput.setHint("Collez l'énoncé de votre exercice ou question de cours...");
        etInput.setMinLines(4);
        etInput.setPadding(32, 24, 32, 24);

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("🤖 Assistant d'Exercice IA Gemini")
                .setView(etInput)
                .setPositiveButton("Résoudre", (dialog, which) -> {
                    String query = etInput.getText().toString().trim();
                    if (query.isEmpty()) {
                        es.dmoral.toasty.Toasty.warning(context, "Veuillez saisir un énoncé", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                        return;
                    }
                    es.dmoral.toasty.Toasty.info(context, "L'IA résout l'exercice...", es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                    solveExercise(context, query, new AiCallback() {
                        @Override
                        public void onSuccess(String result) {
                            new androidx.appcompat.app.AlertDialog.Builder(context)
                                    .setTitle("💡 Solution détaillée (IA)")
                                    .setMessage(result)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }

                        @Override
                        public void onError(String error) {
                            es.dmoral.toasty.Toasty.error(context, "Erreur IA : " + error, es.dmoral.toasty.Toasty.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static void executePrompt(Context context, String prompt, AiCallback callback) {
        if (context == null || callback == null) return;

        String apiKey = "";
        try {
            apiKey = context.getString(R.string.google_api_key);
        } catch (Exception ignored) {}

        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = "AIzaSyC9pfXWrJAsHeBV6z5SrttHB_rsFebc_rY";
        }

        final String finalApiKey = apiKey;
        executor.execute(() -> {
            try {
                GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", finalApiKey);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);
                Content content = new Content.Builder().addText(prompt).build();
                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String text = result.getText();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(text != null ? text.trim() : "Pas de réponse.");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError(t.getMessage() != null ? t.getMessage() : "Erreur Gemini API");
                            }
                        });
                    }
                }, Executors.newSingleThreadExecutor());
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Erreur initialisation Gemini");
                    }
                });
            }
        });
    }
}
