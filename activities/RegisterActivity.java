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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.PasswordUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité d'inscription (Register).
 * Valide le format de l'adresse email, vérifie la confirmation du mot de passe
 * et enregistre le nouveau compte dans la base de données.
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

    private td.teladoum.baobab.viewmodels.RegisterViewModel registerViewModel;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

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

        registerViewModel = new androidx.lifecycle.ViewModelProvider(this).get(td.teladoum.baobab.viewmodels.RegisterViewModel.class);
        registerViewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (btnRegister != null) btnRegister.setEnabled(!isLoading);
        });
        registerViewModel.getErrorMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toasty.error(this, msg, Toasty.LENGTH_SHORT).show();
            }
        });

        userRepository = new UserRepository(this);

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

        if (userRepository.identifierExists(identifier)) {
            etEmail.setError(getString(R.string.error_email_exists));
            etEmail.requestFocus();
            return;
        }

        boolean isPhone = !UserRepository.isEmailInput(identifier);
        String registerEmail = isPhone ? "phone_" + UserRepository.cleanPhone(identifier) + "@baobabtd.app" : identifier;

        if (isPhone) {
            LoginActivity.showPhoneVerificationDialog(this, identifier, () -> proceedRegistration(registerEmail, password, identifier));
        } else {
            proceedRegistration(registerEmail, password, null);
        }
    }

    private void proceedRegistration(String email, String password, String phone) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        String hashedPassword = PasswordUtils.hashPassword(password);

        // Enregistrement cloud dans Firebase Auth avec vérification
        FirebaseHelper.getInstance().registerUser(email, password, task -> {
            if (task.isSuccessful() || (task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().contains("already in use"))) {
                com.google.firebase.auth.FirebaseUser fbUser = FirebaseHelper.getInstance().getAuth().getCurrentUser();
                String firebaseUid = "";
                if (fbUser != null) {
                    firebaseUid = fbUser.getUid();
                }
                if (fbUser != null && !fbUser.isEmailVerified() && phone == null) {
                    try {
                        fbUser.sendEmailVerification();
                    } catch (Exception ignored) {}
                    Toasty.info(RegisterActivity.this, "Un e-mail de vérification a été envoyé à votre adresse 📩", Toasty.LENGTH_LONG).show();
                }
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(RegisterActivity.this, CreateProfileActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("password", hashedPassword);
                intent.putExtra("firebaseUid", firebaseUid);
                if (phone != null) {
                    intent.putExtra("phone", phone);
                }
                startActivity(intent);
                finish();
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Erreur d'inscription Cloud";
                Toasty.error(RegisterActivity.this, errorMsg, Toasty.LENGTH_LONG).show();
            }
        });
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
                                        android.widget.Toast.makeText(RegisterActivity.this, "Erreur de traitement profil Facebook", android.widget.Toast.LENGTH_SHORT).show();
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
                        Toasty.error(RegisterActivity.this, "Erreur Facebook: " + error.getMessage(), Toasty.LENGTH_SHORT).show();
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

            Toasty.success(this, "Bienvenue " + user.getName(), android.widget.Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, AcceuilActivity.class));
            finish();
        } else {
            Toasty.warning(this, "Erreur lors de la connexion sociale", Toasty.LENGTH_SHORT).show();
        }
    }
}

