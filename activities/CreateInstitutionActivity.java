package td.teladoum.baobab.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.models.Institution;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;
import td.teladoum.baobab.viewmodels.InstitutionViewModel;

@AndroidEntryPoint
public class CreateInstitutionActivity extends AppCompatActivity {

    private InstitutionViewModel viewModel;

    @Inject
    SessionManager sessionManager;

    private ImageView imgLogo;
    private EditText etName, etCity, etAddress, etPhone, etEmail, etDescription;
    private Spinner spinnerType;

    private String logoPath = "";

    private final ActivityResultLauncher<CropImageContractOptions> imageCropperLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    Uri uri = result.getUriContent();
                    logoPath = ImageUtils.saveImageToInternalStorage(this, uri);
                    if (imgLogo != null) imgLogo.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_institution);

        viewModel = new ViewModelProvider(this).get(InstitutionViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgLogo = findViewById(R.id.imgInstitutionLogo);
        Button btnSelectLogo = findViewById(R.id.btnSelectLogo);
        etName = findViewById(R.id.etName);
        etCity = findViewById(R.id.etCity);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType);
        Button btnSubmit = findViewById(R.id.btnSubmitInstitution);

        String[] types = {"École", "Collège", "Lycée", "Université", "Institut Supérieur", "Centre de Formation"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);

        btnSelectLogo.setOnClickListener(v -> {
            CropImageOptions options = new CropImageOptions();
            options.guidelines = CropImageView.Guidelines.ON;
            options.aspectRatioX = 1;
            options.aspectRatioY = 1;
            options.fixAspectRatio = true;
            imageCropperLauncher.launch(new CropImageContractOptions(null, options));
        });

        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String city = etCity.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String type = spinnerType.getSelectedItem().toString();

            if (name.isEmpty() || city.isEmpty()) {
                Toasty.error(this, "Veuillez remplir le nom et la ville", Toasty.LENGTH_SHORT).show();
                return;
            }

            String userId = sessionManager != null ? String.valueOf(sessionManager.getUserId()) : "admin";

            Institution institution = new Institution(
                    null, name, type, city, desc, logoPath, "", address, phone, email, userId, System.currentTimeMillis()
            );

            viewModel.createInstitution(institution, () -> {
                Toasty.success(this, "Page officielle créée avec succès !", Toasty.LENGTH_SHORT).show();
                finish();
            }, () -> Toasty.error(this, "Erreur lors de la création", Toasty.LENGTH_SHORT).show());
        });
    }
}
