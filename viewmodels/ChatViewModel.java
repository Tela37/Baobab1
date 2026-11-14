package td.teladoum.baobab.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import td.teladoum.baobab.models.Message;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.ConversationRepository;
import td.teladoum.baobab.repositories.MessageRepository;
import td.teladoum.baobab.repositories.UserRepository;

/**
 * ViewModel pour la gestion de l'écran de discussion (ChatActivity) suivant l'architecture MVVM.
 * Gère l'état UI des messages, le statut de saisie et la présence de l'interlocuteur.
 */
public class ChatViewModel extends AndroidViewModel {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOtherTypingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<UserPresence> receiverPresenceLiveData = new MutableLiveData<>();

    public static class UserPresence {
        public boolean isOnline;
        public String lastSeen;

        public UserPresence(boolean isOnline, String lastSeen) {
            this.isOnline = isOnline;
            this.lastSeen = lastSeen;
        }
    }

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.messageRepository = new MessageRepository(application);
        this.conversationRepository = new ConversationRepository(application);
        this.userRepository = new UserRepository(application);
    }

    public LiveData<List<Message>> getMessagesLiveData() {
        return messagesLiveData;
    }

    public LiveData<Boolean> getIsOtherTypingLiveData() {
        return isOtherTypingLiveData;
    }

    public LiveData<UserPresence> getReceiverPresenceLiveData() {
        return receiverPresenceLiveData;
    }

    public void init(android.content.Context context, int conversationId, int receiverId, int currentUserId) {
        loadMessagesPage(conversationId, currentUserId);
        listenToCloudMessages(conversationId, currentUserId);
        observeReceiverPresence(receiverId);
    }

    public void loadMessagesPage(int conversationId, int currentUserId) {
        if (conversationId <= 0) return;
        messageRepository.loadMessagesPage(conversationId, 20, () -> {
            loadMessages(conversationId, currentUserId);
        });
    }

    public void loadMessages(int conversationId, int currentUserId) {
        if (conversationId <= 0) return;

        if (messageRepository.getUnreadCount(conversationId, currentUserId) > 0) {
            messageRepository.markMessagesAsRead(conversationId, currentUserId);
        }

        List<Message> list = messageRepository.getMessages(conversationId);
        messagesLiveData.setValue(list);
    }

    public void listenToCloudMessages(int conversationId, int currentUserId) {
        if (conversationId <= 0) return;
        messageRepository.loadMessagesFromCloud(conversationId, () -> {
            loadMessages(conversationId, currentUserId);
        });
    }

    public void observeReceiverPresence(int receiverId) {
        if (receiverId <= 0) return;
        userRepository.listenToUserPresence(receiverId, (isOnline, lastSeen) -> {
            receiverPresenceLiveData.postValue(new UserPresence(isOnline, lastSeen));
        });

        User u = userRepository.getUserById(receiverId);
        if (u != null) {
            receiverPresenceLiveData.setValue(new UserPresence(u.isOnline(), u.getLastSeen()));
        }
    }

    public boolean sendMessage(int conversationId, int senderId, String text, String imagePath, Integer replyId, String replyText) {
        boolean sent = messageRepository.sendMessage(conversationId, senderId, text, imagePath, replyId, replyText);
        if (sent) {
            loadMessages(conversationId, senderId);
        }
        return sent;
    }

    public boolean deleteMessage(int messageId, int conversationId, int currentUserId) {
        boolean deleted = messageRepository.deleteMessage(messageId);
        if (deleted) {
            loadMessages(conversationId, currentUserId);
        }
        return deleted;
    }
}
