package td.teladoum.baobab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


import dagger.hilt.android.AndroidEntryPoint;

import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.InstitutionAdapter;
import td.teladoum.baobab.models.Institution;
import td.teladoum.baobab.viewmodels.InstitutionViewModel;

@AndroidEntryPoint
public class InstitutionsActivity extends AppCompatActivity {

    private InstitutionViewModel viewModel;

    private InstitutionAdapter adapter;
    private final List<Institution> institutionList = new ArrayList<>();
    private final List<Institution> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institutions);

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(InstitutionViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        EditText etSearch = findViewById(R.id.etSearchInstitution);
        Button btnCreate = findViewById(R.id.btnCreateInstitution);
        RecyclerView rvInstitutions = findViewById(R.id.rvInstitutions);

        rvInstitutions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InstitutionAdapter(filteredList, institution -> {
            Intent intent = new Intent(this, InstitutionDetailActivity.class);
            intent.putExtra("institution", institution);
            startActivity(intent);
        });
        rvInstitutions.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateInstitutionActivity.class));
        });

        viewModel.getInstitutionsLiveData().observe(this, institutions -> {
            institutionList.clear();
            if (institutions != null) {
                institutionList.addAll(institutions);
            }
            filterList(etSearch.getText().toString());
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(institutionList);
        } else {
            String lower = query.trim().toLowerCase();
            for (Institution inst : institutionList) {
                if ((inst.getName() != null && inst.getName().toLowerCase().contains(lower)) ||
                    (inst.getCity() != null && inst.getCity().toLowerCase().contains(lower)) ||
                    (inst.getType() != null && inst.getType().toLowerCase().contains(lower))) {
                    filteredList.add(inst);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
