package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import td.teladoum.baobab.R;
import td.teladoum.baobab.data.model.Livre;
import td.teladoum.baobab.utils.ImageUtils;

/**
 * Adaptateur pour afficher la liste des livres et cours de la bibliothèque numérique.
 */
public class LivreAdapter extends RecyclerView.Adapter<LivreAdapter.LivreViewHolder> {

    private final List<Livre> livreList;
    private final OnBookClickListener bookClickListener;

    public interface OnBookClickListener {
        void onBookClick(Livre livre);
        void onFavoriteClick(Livre livre);
        default void onBookLongClick(Livre livre) {}
    }

    public LivreAdapter(List<Livre> livreList, OnBookClickListener bookClickListener) {
        this.livreList = livreList;
        this.bookClickListener = bookClickListener;
    }

    public void updateData(List<Livre> newList) {
        if (newList != null) {
            livreList.clear();
            livreList.addAll(newList);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public LivreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_livre, parent, false);
        return new LivreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LivreViewHolder holder, int position) {
        Livre livre = livreList.get(position);
        if (livre == null) return;

        holder.tvBookTitle.setText(livre.getTitre());
        holder.tvBookAuthor.setText(livre.getAuteur() != null ? livre.getAuteur() : "Éditions Baobab");
        holder.tvBookNiveau.setText(livre.getNiveau() != null ? livre.getNiveau() : "Général");
        holder.tvBookMatiere.setText(livre.getMatiere() != null ? livre.getMatiere() : "Cours");

        if (holder.tvBookProtected != null) {
            if (!livre.isDownloadable()) {
                holder.tvBookProtected.setVisibility(View.VISIBLE);
            } else {
                holder.tvBookProtected.setVisibility(View.GONE);
            }
        }

        ImageUtils.loadStoryImage(holder.itemView.getContext(), livre.getCouverture(), holder.imgBookCover);

        View.OnClickListener clickListener = v -> {
            if (bookClickListener != null) {
                bookClickListener.onBookClick(livre);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.itemView.setOnLongClickListener(v -> {
            if (bookClickListener != null) {
                bookClickListener.onBookLongClick(livre);
                return true;
            }
            return false;
        });

        if (holder.btnReadBook != null) {
            holder.btnReadBook.setOnClickListener(clickListener);
        }

        if (holder.btnFavorite != null) {
            holder.btnFavorite.setImageResource(livre.isFavorite() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            holder.btnFavorite.setOnClickListener(v -> {
                if (bookClickListener != null) {
                    bookClickListener.onFavoriteClick(livre);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return livreList != null ? livreList.size() : 0;
    }

    public static class LivreViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBookCover;
        TextView tvBookTitle, tvBookAuthor, tvBookNiveau, tvBookMatiere, tvBookProtected;
        Button btnReadBook;
        ImageButton btnFavorite;

        public LivreViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBookCover = itemView.findViewById(R.id.imgBookCover);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvBookAuthor = itemView.findViewById(R.id.tvBookAuthor);
            tvBookNiveau = itemView.findViewById(R.id.tvBookNiveau);
            tvBookMatiere = itemView.findViewById(R.id.tvBookMatiere);
            tvBookProtected = itemView.findViewById(R.id.tvBookProtected);
            btnReadBook = itemView.findViewById(R.id.btnReadBook);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
