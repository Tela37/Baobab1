package td.teladoum.baobab.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthResult;

import td.teladoum.baobab.repositories.AuthRepository;

/**
 * ViewModel pour l'inscription (RegisterActivity) dans le package ui.auth.
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

    public void register(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Saisissez un e-mail et un mot de passe valide");
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
