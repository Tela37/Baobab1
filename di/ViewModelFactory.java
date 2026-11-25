package td.teladoum.baobab.di;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import td.teladoum.baobab.viewmodels.AcceuilViewModel;
import td.teladoum.baobab.viewmodels.AdminViewModel;
import td.teladoum.baobab.viewmodels.ChatViewModel;
import td.teladoum.baobab.viewmodels.LoginViewModel;
import td.teladoum.baobab.viewmodels.NotificationViewModel;
import td.teladoum.baobab.viewmodels.ProfileViewModel;
import td.teladoum.baobab.viewmodels.RegisterViewModel;

/**
 * Fabrique centralisée pour l'instanciation des ViewModel avec le contexte Application.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;

    public ViewModelFactory(@NonNull Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AcceuilViewModel.class)) {
            return (T) new AcceuilViewModel(application);
        } else if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(application);
        } else if (modelClass.isAssignableFrom(RegisterViewModel.class)) {
            return (T) new RegisterViewModel(application);
        } else if (modelClass.isAssignableFrom(ProfileViewModel.class)) {
            return (T) new ProfileViewModel(application);
        } else if (modelClass.isAssignableFrom(NotificationViewModel.class)) {
            return (T) new NotificationViewModel(application);
        } else if (modelClass.isAssignableFrom(AdminViewModel.class)) {
            return (T) new AdminViewModel(application);
        } else if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(application);
        }
        throw new IllegalArgumentException("ViewModel inconnu: " + modelClass.getName());
    }
}
