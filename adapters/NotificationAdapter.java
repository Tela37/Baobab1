package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.AppNotification;
import td.teladoum.baobab.utils.ImageUtils;

/**
 * Adaptateur RecyclerView pour l'affichage de la liste des notifications reçues.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<AppNotification> notificationList;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    public NotificationAdapter(List<AppNotification> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public void updateNotifications(List<AppNotification> newNotifications) {
        if (newNotifications == null) return;
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new NotificationDiffCallback(this.notificationList, newNotifications));
        this.notificationList.clear();
        this.notificationList.addAll(newNotifications);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class NotificationDiffCallback extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<AppNotification> oldList;
        private final List<AppNotification> newList;

        public NotificationDiffCallback(List<AppNotification> oldList, List<AppNotification> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            AppNotification oldItem = oldList.get(oldItemPosition);
            AppNotification newItem = newList.get(newItemPosition);
            return oldItem != null && newItem != null && oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AppNotification oldItem = oldList.get(oldItemPosition);
            AppNotification newItem = newList.get(newItemPosition);
            if (oldItem == null || newItem == null) return false;

            return oldItem.isRead() == newItem.isRead()
                    && java.util.Objects.equals(oldItem.getMessage(), newItem.getMessage());
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notificationList.get(position);

        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(notification.getCreatedAt());
        holder.viewUnreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        ImageUtils.loadProfileImage(holder.itemView.getContext(), notification.getSenderAvatar(), holder.imgAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvMessage;
        TextView tvTime;
        View viewUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgNotificationAvatar);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}
