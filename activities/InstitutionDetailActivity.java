package td.teladoum.baobab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import dagger.hilt.android.AndroidEntryPoint;

import td.teladoum.baobab.R;
import td.teladoum.baobab.models.Institution;
import td.teladoum.baobab.models.InstitutionEvent;
import td.teladoum.baobab.models.InstitutionNews;
import td.teladoum.baobab.models.InstitutionResult;
import td.teladoum.baobab.viewmodels.InstitutionViewModel;

@AndroidEntryPoint
public class InstitutionDetailActivity extends AppCompatActivity {

    private InstitutionViewModel viewModel;

    private Institution institution;
    private int currentTab = 0; // 0: Actualités, 1: Résultats, 2: Événements

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institution_detail);

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(InstitutionViewModel.class);

        institution = (Institution) getIntent().getSerializableExtra("institution");
        if (institution == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(institution.getName());

        ImageView imgLogo = findViewById(R.id.imgDetailLogo);
        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvTypeCity = findViewById(R.id.tvDetailTypeCity);
        TextView tvDesc = findViewById(R.id.tvDetailDescription);
        TextView tvContact = findViewById(R.id.tvDetailContact);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        RecyclerView rvContent = findViewById(R.id.rvDetailContent);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddContent);

        tvName.setText(institution.getName());
        tvTypeCity.setText((institution.getType() != null ? institution.getType() : "Établissement") + " • " + (institution.getCity() != null ? institution.getCity() : ""));
        tvDesc.setText(institution.getDescription());
        tvContact.setText("Tél: " + (institution.getPhone() != null ? institution.getPhone() : "N/A") + " | Email: " + (institution.getEmail() != null ? institution.getEmail() : "N/A"));

        if (institution.getLogoUrl() != null && !institution.getLogoUrl().isEmpty()) {
            Glide.with(this).load(institution.getLogoUrl()).placeholder(R.drawable.ic_baobab_logo).into(imgLogo);
        }

        tabLayout.addTab(tabLayout.newTab().setText("Actualités"));
        tabLayout.addTab(tabLayout.newTab().setText("Résultats"));
        tabLayout.addTab(tabLayout.newTab().setText("Événements"));

        rvContent.setLayoutManager(new LinearLayoutManager(this));
        loadContentForTab(0, rvContent);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadContentForTab(currentTab, rvContent);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateInstitutionContentActivity.class);
            intent.putExtra("institutionId", institution.getId());
            intent.putExtra("defaultTab", currentTab);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RecyclerView rvContent = findViewById(R.id.rvDetailContent);
        loadContentForTab(currentTab, rvContent);
    }

    private void loadContentForTab(int tab, RecyclerView rvContent) {
        if (institution == null) return;
        if (tab == 0) {
            List<InstitutionNews> newsList = viewModel.getNews(institution.getId());
            rvContent.setAdapter(new NewsAdapter(newsList));
        } else if (tab == 1) {
            List<InstitutionResult> resultsList = viewModel.getResults(institution.getId());
            rvContent.setAdapter(new ResultsAdapter(resultsList));
        } else if (tab == 2) {
            List<InstitutionEvent> eventsList = viewModel.getEvents(institution.getId());
            rvContent.setAdapter(new EventsAdapter(eventsList));
        }
    }



    // Adapters
    private static class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {
        private final List<InstitutionNews> list;
        NewsAdapter(List<InstitutionNews> list) { this.list = list; }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_institution_news, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            InstitutionNews item = list.get(pos);
            h.title.setText(item.getTitle());
            h.content.setText(item.getContent());
            h.date.setText(item.getTimestamp() > 0 ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(item.getTimestamp())) : "");
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView title, content, date;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.tvNewsTitle);
                content = v.findViewById(R.id.tvNewsContent);
                date = v.findViewById(R.id.tvNewsDate);
            }
        }
    }

    private static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.VH> {
        private final List<InstitutionResult> list;
        ResultsAdapter(List<InstitutionResult> list) { this.list = list; }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_institution_result, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            InstitutionResult item = list.get(pos);
            h.title.setText(item.getTitle());
            h.details.setText(item.getDetails());
            h.date.setText(item.getTimestamp() > 0 ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(item.getTimestamp())) : "");
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView title, details, date;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.tvResultTitle);
                details = v.findViewById(R.id.tvResultDetails);
                date = v.findViewById(R.id.tvResultDate);
            }
        }
    }

    private static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.VH> {
        private final List<InstitutionEvent> list;
        EventsAdapter(List<InstitutionEvent> list) { this.list = list; }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_institution_event, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            InstitutionEvent item = list.get(pos);
            h.title.setText(item.getTitle());
            h.desc.setText(item.getDescription());
            h.dateLoc.setText((item.getDate() != null ? item.getDate() : "") + (item.getLocation() != null ? " • " + item.getLocation() : ""));
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView title, desc, dateLoc;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.tvEventTitle);
                desc = v.findViewById(R.id.tvEventDescription);
                dateLoc = v.findViewById(R.id.tvEventDateLocation);
            }
        }
    }
}
