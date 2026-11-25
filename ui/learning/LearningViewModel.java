package td.teladoum.baobab.ui.learning;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import td.teladoum.baobab.data.model.Livre;
import td.teladoum.baobab.data.model.QuizQuestion;
import td.teladoum.baobab.data.model.StudentProgress;
import td.teladoum.baobab.data.repository.LearningRepository;

/**
 * ViewModel pour Baobab Learning (Bibliothèque, Annales, Quiz, Tableau de bord).
 */
public class LearningViewModel extends AndroidViewModel {

    private final LearningRepository learningRepository;

    private final MutableLiveData<List<Livre>> livresLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<QuizQuestion>> quizLiveData = new MutableLiveData<>();
    private final MutableLiveData<StudentProgress> progressLiveData = new MutableLiveData<>();

    public LearningViewModel(@NonNull Application application) {
        super(application);
        this.learningRepository = new LearningRepository(application);
    }

    public LiveData<List<Livre>> getLivresLiveData() {
        return livresLiveData;
    }

    public LiveData<List<QuizQuestion>> getQuizLiveData() {
        return quizLiveData;
    }

    public LiveData<StudentProgress> getProgressLiveData() {
        return progressLiveData;
    }

    public void loadLivres(String niveau, String matiere) {
        loadLivres(niveau, matiere, "TOUT");
    }

    public void loadLivres(String niveau, String matiere, String type) {
        List<Livre> list = learningRepository.getAllLivres();
        List<Livre> filtered = new ArrayList<>();
        for (Livre l : list) {
            boolean matchNiveau = (niveau == null || niveau.equalsIgnoreCase("TOUT") || niveau.equalsIgnoreCase(l.getNiveau()));
            boolean matchMatiere = (matiere == null || matiere.equalsIgnoreCase("TOUT") || matiere.equalsIgnoreCase(l.getMatiere()));
            boolean matchType = (type == null || type.equalsIgnoreCase("TOUT") || type.equalsIgnoreCase(l.getType()));
            if (matchNiveau && matchMatiere && matchType) {
                filtered.add(l);
            }
        }
        livresLiveData.setValue(filtered);
    }

    public void loadFavorites() {
        List<Livre> favs = learningRepository.getFavoriteLivres();
        livresLiveData.setValue(favs != null ? favs : new ArrayList<>());
    }

    public void toggleFavorite(String livreId, String currentNiveau, String currentMatiere) {
        learningRepository.toggleFavorite(livreId);
        loadLivres(currentNiveau, currentMatiere);
    }

    public void loadQuizzes(String matiere) {
        List<QuizQuestion> quizzes = learningRepository.getSampleQuizzes(matiere);
        quizLiveData.setValue(quizzes != null ? quizzes : new ArrayList<>());
    }

    public void loadStudentProgress(int userId) {
        StudentProgress progress = learningRepository.getStudentProgress(userId);
        progressLiveData.setValue(progress);
    }
}
