package td.teladoum.baobab.ui.notifications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import td.teladoum.baobab.models.AppNotification;
import td.teladoum.baobab.repositories.NotificationRepository;

/**
 * ViewModel pour l'écran de notifications (NotificationActivity) dans le package ui.notifications.
 */
public class NotificationViewModel extends AndroidViewModel {

    private final NotificationRepository notificationRepository;
    private final MutableLiveData<List<AppNotification>> notificationsLiveData = new MutableLiveData<>();

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        this.notificationRepository = new NotificationRepository(application);
    }

    public LiveData<List<AppNotification>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public void loadNotifications(int userId) {
        if (userId <= 0) return;
        List<AppNotification> notifs = notificationRepository.getNotificationsForUser(userId);
        notificationsLiveData.setValue(notifs != null ? notifs : new ArrayList<>());

        notificationRepository.loadNotificationsFromCloud(userId, () -> {
            List<AppNotification> updated = notificationRepository.getNotificationsForUser(userId);
            notificationsLiveData.postValue(updated != null ? updated : new ArrayList<>());
        });
    }

    public void markAllAsRead(int userId) {
        if (userId <= 0) return;
        notificationRepository.markAllAsRead(userId);
        loadNotifications(userId);
    }
}
