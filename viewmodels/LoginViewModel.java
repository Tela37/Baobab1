package td.teladoum.baobab.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.AuthRepository;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.PasswordUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * ViewModel pour l'écran de connexion (LoginActivity) suivant l'architecture MVVM.
 * Gère la logique de connexion locale et cloud (Room + Firebase Auth + Firestore).
 */
public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> loginSuccessLiveData = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.userRepository = new UserRepository(application);
        this.sessionManager = new SessionManager(application);
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<User> getLoginSuccessLiveData() {
        return loginSuccessLiveData;
    }

    public void login(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessageLiveData.setValue("Veuillez remplir tous les champs");
            return;
        }

        isLoadingLiveData.setValue(true);
        String hashedPassword = PasswordUtils.hashPassword(password);
        User user = userRepository.getUser(identifier.trim(), hashedPassword);
        String targetEmail = user != null && user.getEmail() != null ? user.getEmail() : identifier.trim();

        if (user != null) {
            authRepository.login(targetEmail, password, task -> {
                if (!task.isSuccessful() && task.getException() != null) {
                    android.util.Log.w("LoginViewModel", "Firebase Auth sync note: " + task.getException().getMessage());
                }
            });

            NotificationRepository notificationRepo = new NotificationRepository(getApplication());
            notificationRepo.clearNotificationsForOtherUsers(user.getId());

            sessionManager.createSession(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getProfileImage()
            );

            isLoadingLiveData.setValue(false);
            loginSuccessLiveData.setValue(user);

        } else {
            authRepository.login(targetEmail, password, authTask -> {
                if (authTask.isSuccessful()) {
                    userRepository.fetchAndSyncUserFromFirestore(targetEmail, password, () -> {
                        User syncedUser = userRepository.getUser(targetEmail, PasswordUtils.hashPassword(password));
                        if (syncedUser == null) {
                            syncedUser = userRepository.getUserByIdentifier(targetEmail);
                            if (syncedUser != null && (syncedUser.getPassword() == null || syncedUser.getPassword().trim().isEmpty())) {
                                syncedUser.setPassword(PasswordUtils.hashPassword(password));
                                FirebaseHelper.getInstance().syncUserToRealtimeDb(syncedUser);
                                FirebaseHelper.getInstance().syncUserProfileToFirestore(syncedUser);
                            }
                        }

                        isLoadingLiveData.setValue(false);
                        if (syncedUser != null) {
                            NotificationRepository notificationRepo = new NotificationRepository(getApplication());
                            notificationRepo.clearNotificationsForOtherUsers(syncedUser.getId());

                            sessionManager.createSession(
                                    syncedUser.getId(),
                                    syncedUser.getName(),
                                    syncedUser.getEmail(),
                                    syncedUser.getProfileImage()
                            );
                            loginSuccessLiveData.setValue(syncedUser);
                        } else {
                            errorMessageLiveData.setValue("Erreur de synchronisation du profil");
                        }
                    }, () -> {
                        isLoadingLiveData.setValue(false);
                        errorMessageLiveData.setValue("Erreur de synchronisation du profil");
                    });
                } else {
                    isLoadingLiveData.setValue(false);
                    String msg = authTask.getException() != null ? authTask.getException().getMessage() : "Échec de connexion";
                    errorMessageLiveData.setValue(msg);
                }
            });
        }
    }

    public void processSocialLogin(String email, String name, String photoUrl) {
        if (email == null || email.trim().isEmpty()) {
            errorMessageLiveData.setValue("Impossible de récupérer l'e-mail social");
            return;
        }

        User user = userRepository.findOrCreateSocialUser(email.trim(), name, photoUrl);
        if (user != null) {
            NotificationRepository notificationRepo = new NotificationRepository(getApplication());
            notificationRepo.clearNotificationsForOtherUsers(user.getId());

            sessionManager.createSession(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getProfileImage()
            );
            loginSuccessLiveData.setValue(user);
        } else {
            errorMessageLiveData.setValue("Erreur lors de la connexion sociale");
        }
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
