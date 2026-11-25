package td.teladoum.baobab.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthResult;

import td.teladoum.baobab.repositories.AuthRepository;

/**
 * ViewModel pour l'écran d'inscription (RegisterActivity) suivant l'architecture MVVM.
 */
public class RegisterViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> registerSuccessLiveData = new MutableLiveData<>();

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<AuthResult> getRegisterSuccessLiveData() {
        return registerSuccessLiveData;
    }

    public void register(String email, String password, String confirmPassword) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Veuillez remplir tous les champs");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorMessageLiveData.setValue("Les mots de passe ne correspondent pas");
            return;
        }

        isLoadingLiveData.setValue(true);
        authRepository.register(email.trim(), password, task -> {
            isLoadingLiveData.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                registerSuccessLiveData.setValue(task.getResult());
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Échec de l'inscription";
                errorMessageLiveData.setValue(msg);
            }
        });
    }
}
