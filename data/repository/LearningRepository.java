package td.teladoum.baobab.data.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import td.teladoum.baobab.data.model.Livre;
import td.teladoum.baobab.data.model.QuizQuestion;
import td.teladoum.baobab.data.model.StudentProgress;
import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.LivreEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;

/**
 * Repository pour "Baobab Learning" (Bibliothèque numérique, Annales, Favoris, Quiz, Progression).
 */
public class LearningRepository {

    private static final Map<String, Livre> livreCache = new ConcurrentHashMap<>();
    private final Context context;

    public LearningRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        preloadSampleData();
    }

    private void preloadSampleData() {
        if (livreCache.isEmpty()) {
            // Livres, cours et annales avec liens de lecture PDF open source disponibles
            String defaultPdf = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";
            livreCache.put("livre_1", new Livre("livre_1", "Algèbre & Analyse Terminale C & D", "Prof. Kouloum", "Lycée", "Mathématiques", "LIVRE", defaultPdf, "https://images.unsplash.com/photo-1543002588-bfa74002ed7e", ""));
            livreCache.put("livre_2", new Livre("livre_2", "Grammaire Française 3ème", "Ministère de l'Éducation Nationale", "Collège", "Français", "COURS", defaultPdf, "https://images.unsplash.com/photo-1497633762265-9d179a990aa6", ""));
            livreCache.put("livre_3", new Livre("livre_3", "Annales BAC D - Mathématiques 2024", "Office des Examens du Tchad", "Annale", "Mathématiques", "ANNALE", defaultPdf, "https://images.unsplash.com/photo-1532012197267-da84d127e765", ""));
            livreCache.put("livre_4", new Livre("livre_4", "Physique-Chimie Terminale C", "Dr. Mahamat", "Lycée", "Physique", "LIVRE", defaultPdf, "https://images.unsplash.com/photo-1507842217343-583bb7270b66", ""));
            livreCache.put("livre_5", new Livre("livre_5", "Concours ENA Tchad - Culture Générale", "Jury ENA", "Concours", "Histoire", "ANNALE", defaultPdf, "https://images.unsplash.com/photo-1457369804613-52c61a468e7d", ""));
        }
    }

    public List<Livre> getAllLivres() {
        return new ArrayList<>(livreCache.values());
    }

    public List<Livre> getLivresByNiveau(String niveau) {
        List<Livre> result = new ArrayList<>();
        for (Livre l : livreCache.values()) {
            if (niveau == null || niveau.equalsIgnoreCase("TOUT") || niveau.equalsIgnoreCase(l.getNiveau())) {
                result.add(l);
            }
        }
        return result;
    }

    public List<Livre> getLivresByMatiere(String matiere) {
        List<Livre> result = new ArrayList<>();
        for (Livre l : livreCache.values()) {
            if (matiere == null || matiere.equalsIgnoreCase("TOUT") || matiere.equalsIgnoreCase(l.getMatiere())) {
                result.add(l);
            }
        }
        return result;
    }

    public List<Livre> getFavoriteLivres() {
        List<Livre> result = new ArrayList<>();
        for (Livre l : livreCache.values()) {
            if (l.isFavorite()) result.add(l);
        }
        return result;
    }

    public void toggleFavorite(String livreId) {
        Livre l = livreCache.get(livreId);
        if (l != null) {
            l.setFavorite(!l.isFavorite());
        }
    }

    public void addLivre(Livre livre, Runnable onSuccess) {
        if (livre == null) return;
        livreCache.put(livre.getId(), livre);
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null) {
                LivreEntity entity = LivreEntity.fromModel(livre);
                if (entity != null) {
                    db.livreDao().insertLivre(entity);
                }
            }
        } catch (Exception ignored) {}

        try {
            FirebaseHelper.getInstance().getRealtimeDb().getReference("livres").child(livre.getId())
                    .setValue(livre)
                    .addOnSuccessListener(aVoid -> {
                        if (onSuccess != null) onSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        if (onSuccess != null) onSuccess.run();
                    });
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public List<QuizQuestion> getSampleQuizzes(String matiere) {
        List<QuizQuestion> questions = new ArrayList<>();
        questions.add(new QuizQuestion("q1", "Quelle est la formule du discriminant (delta) en mathématiques ?", List.of("b² - 4ac", "2ab + c", "a² + b²", "b + 4ac"), 0, "Le discriminant est Δ = b² - 4ac.", "Mathématiques", "Lycée"));
        questions.add(new QuizQuestion("q2", "Quelle est la capitale politique du Tchad ?", List.of("Abéché", "N'Djamena", "Moundou", "Sarh"), 1, "N'Djamena est la capitale politique du Tchad.", "Histoire", "Collège"));
        questions.add(new QuizQuestion("q3", "Quel est l'élément chimique représenté par le symbole 'O' ?", List.of("Or", "Oxygène", "Osmium", "Ozone"), 1, "O correspond à l'oxygène.", "Physique", "Lycée"));
        return questions;
    }

    public StudentProgress getStudentProgress(int userId) {
        // Simulation du tableau de bord étudiant
        return new StudentProgress(userId, 25, 12, 1080, 35);
    }
}
