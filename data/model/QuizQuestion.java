package td.teladoum.baobab.data.model;

import java.util.List;

/**
 * Modèle représentant une question de quiz de révision.
 */
public class QuizQuestion {

    private String id;
    private String question;
    private List<String> options; // A, B, C, D
    private int correctOptionIndex;
    private String explanation;
    private String matiere;
    private String niveau;

    public QuizQuestion() {
    }

    public QuizQuestion(String id, String question, List<String> options, int correctOptionIndex, String explanation, String matiere, String niveau) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.matiere = matiere;
        this.niveau = niveau;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getMatiere() {
        return matiere;
    }

    public void setMatiere(String matiere) {
        this.matiere = matiere;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }
}
