package td.teladoum.baobab.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthResult;

import td.teladoum.baobab.repositories.AuthRepository;

/**
 * ViewModel pour l'écran de connexion (LoginActivity) suivant l'architecture MVVM.
 */
public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> loginSuccessLiveData = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<AuthResult> getLoginSuccessLiveData() {
        return loginSuccessLiveData;
    }

    public void login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Veuillez remplir tous les champs");
            return;
        }

        isLoadingLiveData.setValue(true);
        authRepository.login(email.trim(), password, task -> {
            isLoadingLiveData.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                loginSuccessLiveData.setValue(task.getResult());
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Échec de connexion";
                errorMessageLiveData.setValue(msg);
            }
        });
    }

    public void sendPasswordResetEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            errorMessageLiveData.setValue("Veuillez saisir votre adresse e-mail");
            return;
        }

        isLoadingLiveData.setValue(true);
        authRepository.sendPasswordResetEmail(email.trim(), task -> {
            isLoadingLiveData.setValue(false);
            if (!task.isSuccessful()) {
                String msg = task.getException() != null ? task.getException().getMessage() : "Erreur réinitialisation";
                errorMessageLiveData.setValue(msg);
            }
        });
    }
}
