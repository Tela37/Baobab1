package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Objects;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.MarketplaceItem;

/**
 * Adaptateur RecyclerView pour l'affichage des annonces Marketplace (Acheter, Vendre, Services, Location, Emploi).
 */
public class MarketplaceAdapter extends RecyclerView.Adapter<MarketplaceAdapter.MarketplaceViewHolder> {

    private final List<MarketplaceItem> itemList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MarketplaceItem item);
    }

    public MarketplaceAdapter(List<MarketplaceItem> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    public void updateItems(List<MarketplaceItem> newItems) {
        if (newItems == null) return;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return itemList.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return itemList.get(oldItemPosition).getId() == newItems.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                MarketplaceItem o = itemList.get(oldItemPosition);
                MarketplaceItem n = newItems.get(newItemPosition);
                return o.getPrice() == n.getPrice()
                        && Objects.equals(o.getTitle(), n.getTitle())
                        && Objects.equals(o.getCity(), n.getCity())
                        && Objects.equals(o.getCategory(), n.getCategory())
                        && Objects.equals(o.getImagePath(), n.getImagePath());
            }
        });

        this.itemList.clear();
        this.itemList.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public MarketplaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_marketplace, parent, false);
        return new MarketplaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarketplaceViewHolder holder, int position) {
        MarketplaceItem item = itemList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    public static class MarketplaceViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgPhoto;
        private final TextView tvCategoryBadge;
        private final TextView tvCityBadge;
        private final TextView tvPrice;
        private final TextView tvTitle;
        private final TextView tvSellerInfo;

        public MarketplaceViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgMarketplacePhoto);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvCityBadge = itemView.findViewById(R.id.tvCityBadge);
            tvPrice = itemView.findViewById(R.id.tvMarketplacePrice);
            tvTitle = itemView.findViewById(R.id.tvMarketplaceTitle);
            tvSellerInfo = itemView.findViewById(R.id.tvSellerInfo);
        }

        public void bind(MarketplaceItem item, OnItemClickListener listener) {
            if (item == null) return;

            tvTitle.setText(item.getTitle());
            tvPrice.setText(item.getFormattedPrice());
            tvCategoryBadge.setText(item.getCategory());
            tvCityBadge.setText(item.getCity());
            tvSellerInfo.setText("👤 " + item.getSellerName());

            if (item.getImagePath() != null && !item.getImagePath().trim().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImagePath())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(imgPhoto);
            } else {
                imgPhoto.setImageResource(R.drawable.ic_launcher_background);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}
