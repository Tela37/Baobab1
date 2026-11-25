package td.teladoum.baobab.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import dagger.hilt.android.AndroidEntryPoint;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.viewmodels.InstitutionViewModel;

@AndroidEntryPoint
public class CreateInstitutionContentActivity extends AppCompatActivity {

    private InstitutionViewModel viewModel;

    private String institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_institution_content);

        viewModel = new ViewModelProvider(this).get(InstitutionViewModel.class);

        institutionId = getIntent().getStringExtra("institutionId");
        int defaultTab = getIntent().getIntExtra("defaultTab", 0);

        if (institutionId == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        Spinner spinnerType = findViewById(R.id.spinnerContentType);
        EditText etTitle = findViewById(R.id.etTitle);
        EditText etDetails = findViewById(R.id.etDetailsOrContent);
        EditText etExtra = findViewById(R.id.etExtraField);
        Button btnSubmit = findViewById(R.id.btnSubmitContent);

        String[] types = {"Actualité", "Résultat", "Événement"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);
        spinnerType.setSelection(Math.min(defaultTab, 2));

        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position == 0) {
                    etExtra.setHint("Lien image (Optionnel)");
                    etExtra.setVisibility(android.view.View.VISIBLE);
                } else if (position == 1) {
                    etExtra.setHint("Lien document / PDF (Optionnel)");
                    etExtra.setVisibility(android.view.View.VISIBLE);
                } else {
                    etExtra.setHint("Date et Lieu (ex: Samedi 15h, Salle A)");
                    etExtra.setVisibility(android.view.View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String details = etDetails.getText().toString().trim();
            String extra = etExtra.getText().toString().trim();
            int typePos = spinnerType.getSelectedItemPosition();

            if (title.isEmpty() || details.isEmpty()) {
                Toasty.error(this, "Veuillez remplir le titre et le contenu", Toasty.LENGTH_SHORT).show();
                return;
            }

            if (typePos == 0) {
                viewModel.addNews(institutionId, title, details, extra, () -> {
                    Toasty.success(this, "Actualité publiée avec succès", Toasty.LENGTH_SHORT).show();
                    finish();
                }, () -> Toasty.error(this, "Erreur de publication", Toasty.LENGTH_SHORT).show());
            } else if (typePos == 1) {
                viewModel.addResult(institutionId, title, details, extra, () -> {
                    Toasty.success(this, "Résultat publié avec succès", Toasty.LENGTH_SHORT).show();
                    finish();
                }, () -> Toasty.error(this, "Erreur de publication", Toasty.LENGTH_SHORT).show());
            } else {
                viewModel.addEvent(institutionId, title, details, extra, "Lieu à préciser", extra, () -> {
                    Toasty.success(this, "Événement publié avec succès", Toasty.LENGTH_SHORT).show();
                    finish();
                }, () -> Toasty.error(this, "Erreur de publication", Toasty.LENGTH_SHORT).show());
            }
        });
    }
}
