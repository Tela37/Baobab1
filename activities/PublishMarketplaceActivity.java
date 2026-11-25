package td.teladoum.baobab.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.helpers.GeminiHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.MarketplaceRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité pour publier une nouvelle annonce sur la Marketplace avec Hilt DI.
 */
@AndroidEntryPoint
public class PublishMarketplaceActivity extends AppCompatActivity {

    @Inject
    MarketplaceRepository repository;

    @Inject
    SessionManager sessionManager;

    @Inject
    UserRepository userRepository;

    private ImageView imgPublishPhoto;
    private View layoutPhotoPlaceholder;
    private EditText etItemTitle, etItemPrice, etSellerPhone, etItemDescription;
    private Spinner spinnerCategory, spinnerCity;

    private Uri selectedImageUri;
    private String savedImagePath;

    private final ActivityResultLauncher<CropImageContractOptions> imageCropperLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null) {
                    selectedImageUri = result.getUriContent();
                    savedImagePath = ImageUtils.saveImageToInternalStorage(this, selectedImageUri);
                    if (imgPublishPhoto != null) {
                        imgPublishPhoto.setImageURI(selectedImageUri);
                        if (layoutPhotoPlaceholder != null) layoutPhotoPlaceholder.setVisibility(View.GONE);
                    }
                } else if (result.getError() != null) {
                    Toasty.error(this, "Erreur de sélection d'image", Toasty.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_marketplace);

        // 2. Ajuster les paddings pour ne pas recouvrir le contenu sous les barres système
        android.view.View mainContainer = findViewById(getResources().getIdentifier("main_container", "id", getPackageName()));
        if (mainContainer == null) {
            mainContainer = findViewById(android.R.id.content);
        }
        if (mainContainer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                androidx.core.graphics.Insets navBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
                androidx.core.graphics.Insets imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
                int bottomInset = Math.max(navBarInsets.bottom, imeInsets.bottom);
                v.setPadding(0, statusBarInsets.top, 0, bottomInset);
                return insets;
            });
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            // true = icônes sombres (pour fond clair), false = icônes blanches (pour fond sombre)
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        Toolbar toolbar = findViewById(R.id.toolbarPublishMarketplace);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        imgPublishPhoto = findViewById(R.id.imgPublishPhoto);
        layoutPhotoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder);
        View cardPhoto = findViewById(R.id.cardSelectPhoto);
        if (cardPhoto != null) {
            cardPhoto.setOnClickListener(v -> launchImageCropper());
        }

        etItemTitle = findViewById(R.id.etItemTitle);
        etItemPrice = findViewById(R.id.etItemPrice);
        etSellerPhone = findViewById(R.id.etSellerPhone);
        etItemDescription = findViewById(R.id.etItemDescription);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        setupCategorySpinner();

        spinnerCity = findViewById(R.id.spinnerCity);
        setupCitySpinner();

        User currentUser = userRepository.getUserById(sessionManager.getUserId());
        if (currentUser != null && currentUser.getPhone() != null && etSellerPhone != null) {
            etSellerPhone.setText(currentUser.getPhone());
        }

        Button btnSubmit = findViewById(R.id.btnSubmitPublish);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submitPublish());
        }

        Button btnAi = findViewById(R.id.btnGenerateDescriptionAi);
        if (btnAi != null) {
            btnAi.setOnClickListener(v -> generateDescriptionWithAi());
        }
    }

    private void generateDescriptionWithAi() {
        String title = etItemTitle.getText().toString().trim();
        String price = etItemPrice.getText().toString().trim();
        String city = (String) spinnerCity.getSelectedItem();

        String prompt = "Rédige une description complète et professionnelle pour la vente de : " + (title.isEmpty() ? "un article" : title)
                + " à " + (city != null ? city : "N'Djamena") + (price.isEmpty() ? "" : " pour un prix de " + price + " FCFA") + ".";

        Toasty.info(this, "Génération de la description par l'IA Gemini...", Toasty.LENGTH_SHORT).show();
        GeminiHelper.generateWritingAssistant(this, prompt, new GeminiHelper.AiCallback() {
            @Override
            public void onSuccess(String result) {
                if (etItemDescription != null) etItemDescription.setText(result);
                Toasty.success(PublishMarketplaceActivity.this, "Description générée par l'IA ! ✨", Toasty.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toasty.error(PublishMarketplaceActivity.this, "Erreur IA : " + error, Toasty.LENGTH_SHORT).show();
            }
        });
    }

    private void launchImageCropper() {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.guidelines = CropImageView.Guidelines.ON;
        cropImageOptions.aspectRatioX = 4;
        cropImageOptions.aspectRatioY = 3;
        cropImageOptions.fixAspectRatio = true;
        imageCropperLauncher.launch(new CropImageContractOptions(null, cropImageOptions));
    }

    private void setupCategorySpinner() {
        String[] categories = new String[]{
            "VENTE (Articles & Biens)",
            "ACHAT (Recherche / Besoins)",
            "SERVICES (Prestations & Freelance)",
            "IMMOBILIER (Maisons, Terrains, Appartements)",
            "EMPLOI (Offres d'emploi & Recrutement)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupCitySpinner() {
        String[] cities = new String[]{"N'Djamena", "Moundou", "Abéché", "Sarh", "Koumra", "Kelo", "Am Timan", "Bongor", "Pala", "Doba", "Ati", "Mongo", "Faya-Largeau", "Autre ville Tchad"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities);
        spinnerCity.setAdapter(adapter);
    }

    private void submitPublish() {
        String title = etItemTitle.getText().toString().trim();
        String priceStr = etItemPrice.getText().toString().trim();
        String phone = etSellerPhone.getText().toString().trim();
        String description = etItemDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toasty.warning(this, "Veuillez saisir le titre de l'annonce", Toasty.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty()) {
            Toasty.warning(this, "Veuillez saisir un numéro de téléphone de contact", Toasty.LENGTH_SHORT).show();
            return;
        }

        double price = 0;
        if (!priceStr.isEmpty()) {
            try {
                price = Double.parseDouble(priceStr);
            } catch (Exception ignored) {}
        }

        int catPos = spinnerCategory.getSelectedItemPosition();
        String category = "VENTE";
        if (catPos == 1) category = "ACHAT";
        else if (catPos == 2) category = "SERVICES";
        else if (catPos == 3) category = "IMMOBILIER";
        else if (catPos == 4) category = "EMPLOI";

        String city = (String) spinnerCity.getSelectedItem();

        User currentUser = userRepository.getUserById(sessionManager.getUserId());
        String sellerName = currentUser != null && currentUser.getName() != null ? currentUser.getName() : "Utilisateur";

        repository.publishItem(sessionManager.getUserId(), sellerName, phone, title, description, price, category, city, savedImagePath, () -> {
            Toasty.success(this, "Annonce publiée avec succès ! 🎉", Toasty.LENGTH_LONG).show();
            finish();
        }, () -> {
            Toasty.error(this, "Erreur lors de la publication de l'annonce", Toasty.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.color.background_dark || item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
