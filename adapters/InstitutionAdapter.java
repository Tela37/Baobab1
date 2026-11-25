package td.teladoum.baobab.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.Institution;

public class InstitutionAdapter extends RecyclerView.Adapter<InstitutionAdapter.ViewHolder> {

    private final List<Institution> institutionList;
    private final OnInstitutionClickListener listener;

    public interface OnInstitutionClickListener {
        void onInstitutionClick(Institution institution);
    }

    public InstitutionAdapter(List<Institution> institutionList, OnInstitutionClickListener listener) {
        this.institutionList = institutionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_institution, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Institution inst = institutionList.get(position);
        holder.tvName.setText(inst.getName());
        holder.tvType.setText((inst.getType() != null ? inst.getType() : "Établissement") + " • " + (inst.getCity() != null ? inst.getCity() : ""));
        holder.tvDesc.setText(inst.getDescription());

        if (inst.getLogoUrl() != null && !inst.getLogoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(inst.getLogoUrl())
                    .placeholder(R.drawable.ic_baobab_logo)
                    .into(holder.imgLogo);
        } else {
            holder.imgLogo.setImageResource(R.drawable.ic_baobab_logo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onInstitutionClick(inst);
        });
    }

    @Override
    public int getItemCount() {
        return institutionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLogo;
        TextView tvName, tvType, tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgLogo = itemView.findViewById(R.id.imgInstitutionLogo);
            tvName = itemView.findViewById(R.id.tvInstitutionName);
            tvType = itemView.findViewById(R.id.tvInstitutionType);
            tvDesc = itemView.findViewById(R.id.tvInstitutionDescription);
        }
    }
}
