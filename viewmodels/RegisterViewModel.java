package td.teladoum.baobab.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;

import td.teladoum.baobab.repositories.AuthRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.PasswordUtils;

/**
 * ViewModel pour l'écran d'inscription (RegisterActivity) suivant l'architecture MVVM.
 */
public class RegisterViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<RegistrationSuccessEvent> registerSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<PhoneVerificationEvent> phoneVerificationNeededLiveData = new MutableLiveData<>();

    public static class RegistrationSuccessEvent {
        public final String email;
        public final String hashedPassword;
        public final String firebaseUid;
        public final String phone;

        public RegistrationSuccessEvent(String email, String hashedPassword, String firebaseUid, String phone) {
            this.email = email;
            this.hashedPassword = hashedPassword;
            this.firebaseUid = firebaseUid;
            this.phone = phone;
        }
    }

    public static class PhoneVerificationEvent {
        public final String phone;
        public final Runnable onVerified;

        public PhoneVerificationEvent(String phone, Runnable onVerified) {
            this.phone = phone;
            this.onVerified = onVerified;
        }
    }

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.userRepository = new UserRepository(application);
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<RegistrationSuccessEvent> getRegisterSuccessLiveData() {
        return registerSuccessLiveData;
    }

    public LiveData<PhoneVerificationEvent> getPhoneVerificationNeededLiveData() {
        return phoneVerificationNeededLiveData;
    }

    public void register(String identifier, String password, String confirmPassword) {
        if (identifier == null || identifier.trim().isEmpty()) {
            errorMessageLiveData.setValue("Veuillez saisir un e-mail ou un numéro de téléphone");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Le mot de passe est requis");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorMessageLiveData.setValue("Les mots de passe ne correspondent pas");
            return;
        }

        if (userRepository.identifierExists(identifier.trim())) {
            errorMessageLiveData.setValue("Cet identifiant existe déjà");
            return;
        }

        boolean isPhone = !UserRepository.isEmailInput(identifier.trim());
        String registerEmail = isPhone ? "phone_" + UserRepository.cleanPhone(identifier.trim()) + "@baobabtd.app" : identifier.trim();

        if (isPhone) {
            phoneVerificationNeededLiveData.setValue(new PhoneVerificationEvent(identifier.trim(), () -> proceedRegistration(registerEmail, password, identifier.trim())));
        } else {
            proceedRegistration(registerEmail, password, null);
        }
    }

    public void proceedRegistration(String email, String password, String phone) {
        isLoadingLiveData.setValue(true);
        String hashedPassword = PasswordUtils.hashPassword(password);

        authRepository.register(email, password, task -> {
            isLoadingLiveData.setValue(false);
            if (task.isSuccessful() || (task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().contains("already in use"))) {
                FirebaseUser fbUser = authRepository.getCurrentUser();
                String firebaseUid = fbUser != null ? fbUser.getUid() : "";
                if (fbUser != null && !fbUser.isEmailVerified() && phone == null) {
                    try {
                        fbUser.sendEmailVerification();
                    } catch (Exception ignored) {}
                }
                registerSuccessLiveData.setValue(new RegistrationSuccessEvent(email, hashedPassword, firebaseUid, phone));
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Erreur d'inscription Cloud";
                errorMessageLiveData.setValue(errorMsg);
            }
        });
    }
}
