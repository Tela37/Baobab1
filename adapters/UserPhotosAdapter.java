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
import td.teladoum.baobab.utils.ImageUtils;

public class UserPhotosAdapter extends RecyclerView.Adapter<UserPhotosAdapter.PhotoViewHolder> {

    public static class PhotoItem {
        private final String imagePath;
        private final String title;

        public PhotoItem(String imagePath, String title) {
            this.imagePath = imagePath;
            this.title = title;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getTitle() {
            return title;
        }
    }

    public interface OnPhotoClickListener {
        void onPhotoClick(PhotoItem photoItem);
    }

    private final List<PhotoItem> photoList;
    private final OnPhotoClickListener listener;

    public UserPhotosAdapter(List<PhotoItem> photoList, OnPhotoClickListener listener) {
        this.photoList = photoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PhotoItem item = photoList.get(position);
        holder.tvPhotoLabel.setText(item.getTitle());
        ImageUtils.loadPostImage(holder.itemView.getContext(), item.getImagePath(), holder.imgItemPhoto);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoList != null ? photoList.size() : 0;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgItemPhoto;
        TextView tvPhotoLabel;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgItemPhoto = itemView.findViewById(R.id.imgItemPhoto);
            tvPhotoLabel = itemView.findViewById(R.id.tvPhotoLabel);
        }
    }
}
