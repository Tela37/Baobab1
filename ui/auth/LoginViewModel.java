package td.teladoum.baobab.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthResult;

import td.teladoum.baobab.repositories.AuthRepository;

/**
 * ViewModel pour l'écran de connexion (LoginActivity) dans le package ui.auth.
 */
public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> authSuccessLiveData = new MutableLiveData<>();

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

    public LiveData<AuthResult> getAuthSuccessLiveData() {
        return authSuccessLiveData;
    }

    public void login(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Saisissez un e-mail et un mot de passe valide");
            return;
        }

        isLoadingLiveData.setValue(true);
        authRepository.login(email.trim(), password, task -> {
            isLoadingLiveData.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                authSuccessLiveData.setValue(task.getResult());
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Échec de l'authentification";
                errorMessageLiveData.setValue(msg);
            }
        });
    }

    public void resetPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            errorMessageLiveData.setValue("Veuillez saisir votre adresse e-mail");
            return;
        }

        isLoadingLiveData.setValue(true);
        authRepository.sendPasswordResetEmail(email.trim(), task -> {
            isLoadingLiveData.setValue(false);
            if (task.isSuccessful()) {
                errorMessageLiveData.setValue("SUCCESS: Un e-mail de réinitialisation a été envoyé");
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Erreur de réinitialisation";
                errorMessageLiveData.setValue(msg);
            }
        });
    }
}
