package td.teladoum.baobab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONObject;

import java.util.Arrays;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.SessionManager;
import td.teladoum.baobab.viewmodels.RegisterViewModel;

/**
 * Activité d'inscription (Register) suivant l'architecture MVVM.
 * Délègue la logique métier à RegisterViewModel.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private CheckBox cbShowPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private LoginButton btnFacebookLogin;
    private CallbackManager callbackManager;
    private SessionManager sessionManager;

    private RegisterViewModel registerViewModel;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        View mainContainer = findViewById(getResources().getIdentifier("main_container", "id", getPackageName()));
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
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        etEmail = findViewById(R.id.etEmail);
        if (etEmail != null) {
            etEmail.setHint(getString(R.string.hint_email_or_phone));
        }
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbShowPassword = findViewById(R.id.cbShowPasswordRegister);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        sessionManager = new SessionManager(this);
        btnFacebookLogin = findViewById(R.id.btnFacebookLogin);
        setupFacebookLogin();

        userRepository = new UserRepository(this);

        registerViewModel = new androidx.lifecycle.ViewModelProvider(this).get(RegisterViewModel.class);

        registerViewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (btnRegister != null) btnRegister.setEnabled(!isLoading);
        });

        registerViewModel.getErrorMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toasty.error(this, msg, Toasty.LENGTH_SHORT).show();
            }
        });

        registerViewModel.getRegisterSuccessLiveData().observe(this, event -> {
            if (event != null) {
                if (event.phone == null) {
                    Toasty.info(RegisterActivity.this, "Un e-mail de vérification a été envoyé à votre adresse 📩", Toasty.LENGTH_LONG).show();
                }
                Intent intent = new Intent(RegisterActivity.this, CreateProfileActivity.class);
                intent.putExtra("email", event.email);
                intent.putExtra("password", event.hashedPassword);
                intent.putExtra("firebaseUid", event.firebaseUid);
                if (event.phone != null) {
                    intent.putExtra("phone", event.phone);
                }
                startActivity(intent);
                finish();
            }
        });

        registerViewModel.getPhoneVerificationNeededLiveData().observe(this, event -> {
            if (event != null) {
                LoginActivity.showPhoneVerificationDialog(this, event.phone, event.onVerified);
            }
        });

        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int inputType = isChecked
                    ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    : (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            etPassword.setInputType(inputType);
            etConfirmPassword.setInputType(inputType);

            etPassword.setSelection(etPassword.getText().length());
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnRegister.setOnClickListener(v -> registerUser());

        TextView tvPrivacyPolicyRegister = findViewById(R.id.tvPrivacyPolicyRegister);
        if (tvPrivacyPolicyRegister != null) {
            tvPrivacyPolicyRegister.setOnClickListener(v -> showPrivacyPolicyDialog(this));
        }

        if (tvLogin != null) {
            tvLogin.setOnClickListener(v -> {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    public static void showPrivacyPolicyDialog(android.content.Context context) {
        if (context == null) return;
        String privacyText = "POLITIQUE DE CONFIDENTIALITÉ DE BAOBABTD\n\n" +
                "1. Collecte des données :\n" +
                "Nous collectons les données fournies lors de votre inscription (Email, Nom, Prénom, Photo de profil, Localisation optionnelle).\n\n" +
                "2. Utilisation des données :\n" +
                "Vos informations sont utilisées pour gérer votre compte, afficher votre profil et vous permettre de communiquer avec vos amis.\n\n" +
                "3. Publicités & Partenaires (AdMob & Meta) :\n" +
                "L'application utilise les SDK Google AdMob et Facebook Login. Des identifiants publicitaires anonymes peuvent être utilisés pour diffuser des annonces pertinentes.\n\n" +
                "4. Protection & Sécurité :\n" +
                "Les mots de passe sont hachés de manière sécurisée (PBKDF2/SHA-256). Vos données ne sont jamais vendues à des tiers.\n\n" +
                "5. Vos Droits :\n" +
                "Vous pouvez modifier vos paramètres de confidentialité ou supprimer votre compte à tout moment dans les Paramètres.";

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Politique de Confidentialité 📄")
                .setMessage(privacyText)
                .setPositiveButton("J'ai compris", null)
                .show();
    }

    private void registerUser() {
        String identifier = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (identifier.isEmpty()) {
            etEmail.setError(getString(R.string.hint_email_or_phone));
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            etConfirmPassword.requestFocus();
            return;
        }

        registerViewModel.register(identifier, password, confirmPassword);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setupFacebookLogin() {
        try {
            if (btnFacebookLogin != null) {
                btnFacebookLogin.setVisibility(View.VISIBLE);
                callbackManager = CallbackManager.Factory.create();
                btnFacebookLogin.setPermissions(Arrays.asList("email", "public_profile"));

                btnFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                (object, response) -> {
                                    try {
                                        String email = null;
                                        String name = "Utilisateur Facebook";
                                        String photoUrl = null;

                                        if (object != null) {
                                            if (object.has("email")) {
                                                email = object.getString("email");
                                            }
                                            if (object.has("name")) {
                                                name = object.getString("name");
                                            }
                                            if (object.has("picture")) {
                                                JSONObject pictureData = object.getJSONObject("picture").optJSONObject("data");
                                                if (pictureData != null && pictureData.has("url")) {
                                                    photoUrl = pictureData.getString("url");
                                                }
                                            }
                                        }

                                        if (email == null || email.isEmpty()) {
                                            email = "fb_" + loginResult.getAccessToken().getUserId() + "@facebook.com";
                                        }

                                        onSocialLoginSuccess(email, name, photoUrl);

                                    } catch (Exception e) {
                                        Toast.makeText(RegisterActivity.this, "Erreur de traitement profil Facebook", Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );

                        Bundle parameters = new Bundle();
                        parameters.putString("fields", "id,name,email,picture.type(large)");
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        Toasty.warning(RegisterActivity.this, "Connexion Facebook annulée", Toasty.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toasty.error(RegisterActivity.this, "Erreur Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("RegisterActivity", "Facebook SDK setup error: " + e.getMessage());
        }
    }

    private void onSocialLoginSuccess(String email, String name, String photoUrl) {
        User user = userRepository.findOrCreateSocialUser(email, name, photoUrl);
        if (user != null) {
            NotificationRepository notificationRepo = new NotificationRepository(this);
            notificationRepo.clearNotificationsForOtherUsers(user.getId());

            sessionManager.createSession(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getProfileImage()
            );

            Toasty.success(this, "Bienvenue " + user.getName(), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, AcceuilActivity.class));
            finish();
        } else {
            Toasty.warning(this, "Erreur lors de la connexion sociale", Toast.LENGTH_SHORT).show();
        }
    }
}
