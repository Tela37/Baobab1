package td.teladoum.baobab.viewmodels;

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
 * ViewModel pour le centre de notifications (NotificationActivity) suivant l'architecture MVVM.
 */
public class NotificationViewModel extends AndroidViewModel {

    private final NotificationRepository notificationRepository;

    private final MutableLiveData<List<AppNotification>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> unreadCountLiveData = new MutableLiveData<>(0);

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        this.notificationRepository = new NotificationRepository(application);
    }

    public LiveData<List<AppNotification>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadCountLiveData() {
        return unreadCountLiveData;
    }

    public void loadNotifications(int recipientId) {
        if (recipientId <= 0) return;
        notificationRepository.loadNotificationsFromCloud(recipientId, () -> {
            List<AppNotification> list = notificationRepository.getNotificationsForUser(recipientId);
            notificationsLiveData.postValue(list != null ? list : new ArrayList<>());
            unreadCountLiveData.postValue(notificationRepository.getUnreadCount(recipientId));
        });
    }

    public void markAllAsRead(int recipientId) {
        if (recipientId <= 0) return;
        notificationRepository.markAllAsRead(recipientId);
        loadNotifications(recipientId);
    }
}
