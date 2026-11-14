package td.teladoum.baobab.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.UserRepository;

/**
 * ViewModel pour la gestion du panneau d'administration (AdminActivity) suivant l'architecture MVVM.
 * Encapsule la logique de chargement des utilisateurs, des signalements et du calcul des statistiques.
 */
public class AdminViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    private final MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PostRepository.ReportItem>> reportsLiveData = new MutableLiveData<>();

    public AdminViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.postRepository = new PostRepository(application);
    }

    public LiveData<List<User>> getUsersLiveData() {
        return usersLiveData;
    }

    public LiveData<List<PostRepository.ReportItem>> getReportsLiveData() {
        return reportsLiveData;
    }

    public void loadReports() {
        List<PostRepository.ReportItem> list = postRepository.getReports();
        reportsLiveData.setValue(list);
    }

    public void loadUsers() {
        List<User> list = userRepository.getAllUsers();
        usersLiveData.setValue(list);
    }

    public void syncCloudData() {
        userRepository.syncAllUsersFromCloud(() -> {
            loadReports();
            loadUsers();
        });
    }

    public boolean deleteUser(int targetUserId, int operatorUserId) {
        boolean deleted = userRepository.deleteUser(targetUserId, operatorUserId);
        if (deleted) {
            loadUsers();
        }
        return deleted;
    }

    public boolean setAdmin(int targetUserId, boolean isAdmin) {
        boolean success = userRepository.setAdmin(targetUserId, isAdmin);
        if (success) {
            loadUsers();
        }
        return success;
    }
}
