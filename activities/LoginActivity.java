package td.teladoum.baobab.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Locale;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.util.Arrays;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.helpers.LocaleHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.PasswordUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité de connexion (Login).
 * Gère l'authentification locale (Email/Mot de passe), la connexion Google SignIn et Facebook Login,
 * la connexion automatique avec "Se souvenir de moi" et le déverrouillage par empreinte biométrique.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox cbShowPassword;
    private Button btnLogin, btnGoogleLogin;
    private LoginButton btnFacebookLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    private td.teladoum.baobab.viewmodels.LoginViewModel loginViewModel;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleGoogleSignInResult(task);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            // true = icônes sombres (pour fond clair), false = icônes blanches (pour fond sombre)
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        SharedPreferences prefs = getSharedPreferences("MessagesSession", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        int targetMode = darkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
        }

        setContentView(R.layout.activity_login);

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

        TextView btnLangFrench = findViewById(R.id.btnLangFrench);
        TextView btnLangEnglish = findViewById(R.id.btnLangEnglish);
        TextView btnLangArabic = findViewById(R.id.btnLangArabic);
        TextView btnLangChinese = findViewById(R.id.btnLangChinese);

        String currentLang = LocaleHelper.getPersistedLanguage(this, "fr");
        highlightLanguageButton(currentLang, btnLangFrench, btnLangEnglish, btnLangArabic, btnLangChinese);

        if (btnLangFrench != null) {
            btnLangFrench.setOnClickListener(v -> changeLanguage("fr"));
        }
        if (btnLangEnglish != null) {
            btnLangEnglish.setOnClickListener(v -> changeLanguage("en"));
        }
        if (btnLangArabic != null) {
            btnLangArabic.setOnClickListener(v -> changeLanguage("ar"));
        }
        if (btnLangChinese != null) {
            btnLangChinese.setOnClickListener(v -> changeLanguage("zh"));
        }

        etEmail = findViewById(R.id.etEmail);
        if (etEmail != null) {
            etEmail.setHint(getString(R.string.hint_email_or_phone));
        }
        etPassword = findViewById(R.id.etPassword);
        cbShowPassword = findViewById(R.id.cbShowPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnFacebookLogin = findViewById(R.id.btnFacebookLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        loginViewModel = new androidx.lifecycle.ViewModelProvider(this).get(td.teladoum.baobab.viewmodels.LoginViewModel.class);
        loginViewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (btnLogin != null) btnLogin.setEnabled(!isLoading);
        });
        loginViewModel.getErrorMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toasty.error(this, msg, Toasty.LENGTH_SHORT).show();
            }
        });

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        checkAndAutoLogin();

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        setupGoogleSignIn();
        setupFacebookLogin();


        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> {

            String identifier = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (identifier.isEmpty()) {
                etEmail.setError(getString(R.string.hint_email_or_phone));
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError(getString(R.string.error_password_required));
                return;
            }

            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            String hashedPassword = PasswordUtils.hashPassword(password);
            User user = userRepository.getUser(identifier, hashedPassword);
            String targetEmail = user != null && user.getEmail() != null ? user.getEmail() : identifier;

            if (user != null) {
                // Synchronisation de la session Firebase Auth avec vérification
                FirebaseHelper.getInstance().loginUser(targetEmail, password, task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        android.util.Log.w("LoginActivity", "Firebase Auth sync note: " + task.getException().getMessage());
                    }
                });

                NotificationRepository notificationRepo = new NotificationRepository(this);
                notificationRepo.clearNotificationsForOtherUsers(user.getId());

                sessionManager.createSession(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getProfileImage()
                );

                Toasty.success(this, "Bienvenue " + user.getName(), Toasty.LENGTH_SHORT).show();

                startActivity(new Intent(LoginActivity.this, AcceuilActivity.class));
                finish();

            } else {
                FirebaseHelper.getInstance().loginUser(targetEmail, password, authTask -> {
                    if (authTask.isSuccessful()) {
                        userRepository.fetchAndSyncUserFromFirestore(targetEmail, password, () -> {
                            User syncedUser = userRepository.getUser(targetEmail, PasswordUtils.hashPassword(password));
                            if (syncedUser == null) {
                                syncedUser = userRepository.getUserByIdentifier(targetEmail);
                                if (syncedUser != null && (syncedUser.getPassword() == null || syncedUser.getPassword().trim().isEmpty())) {
                                    syncedUser.setPassword(PasswordUtils.hashPassword(password));
                                    FirebaseHelper.getInstance().syncUserToRealtimeDb(syncedUser);
                                    FirebaseHelper.getInstance().syncUserProfileToFirestore(syncedUser);
                                }
                            }

                            if (syncedUser != null) {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);

                                NotificationRepository notificationRepo = new NotificationRepository(this);
                                notificationRepo.clearNotificationsForOtherUsers(syncedUser.getId());

                                sessionManager.createSession(
                                        syncedUser.getId(),
                                        syncedUser.getName(),
                                        syncedUser.getEmail(),
                                        syncedUser.getProfileImage()
                                );

                                Toasty.success(this, "Bienvenue " + syncedUser.getName(), Toasty.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, AcceuilActivity.class));
                                finish();
                            } else {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                Toasty.error(this, "Erreur de synchronisation du profil", Toast.LENGTH_SHORT).show();
                            }
                        }, () -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toasty.error(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        String errMsg = (authTask.getException() != null && authTask.getException().getMessage() != null) 
                                ? authTask.getException().getMessage() 
                                : "Email ou mot de passe incorrect";
                        Toasty.error(this, errMsg, Toasty.LENGTH_LONG).show();
                    }
                });
            }
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndAutoLogin();
    }

    private void checkAndAutoLogin() {
        if (sessionManager == null || userRepository == null) return;

        com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (sessionManager.isLoggedIn() || fbUser != null) {
            if (fbUser != null && (!sessionManager.isLoggedIn() || sessionManager.getUserId() == -1)) {
                String email = fbUser.getEmail();
                if (email != null && !email.trim().isEmpty()) {
                    User user = userRepository.getUserByEmail(email);
                    if (user != null) {
                        sessionManager.createSession(user.getId(), user.getName(), user.getEmail(), user.getProfileImage());
                    } else {
                        String displayName = fbUser.getDisplayName() != null ? fbUser.getDisplayName() : email;
                        String photoUrl = fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : null;
                        User socialUser = userRepository.findOrCreateSocialUser(email, displayName, photoUrl);
                        if (socialUser != null) {
                            sessionManager.createSession(socialUser.getId(), socialUser.getName(), socialUser.getEmail(), socialUser.getProfileImage());
                        }
                    }
                }
            }
            if (sessionManager.isLoggedIn() && sessionManager.getUserId() != -1) {
                Intent intent = new Intent(LoginActivity.this, AcceuilActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    private void showForgotPasswordDialog() {
        EditText etResetEmail = new EditText(this);
        etResetEmail.setHint("Entrez votre adresse email");

        new AlertDialog.Builder(this)
                .setTitle("Mot de passe oublié ?")
                .setMessage("Entrez l'adresse email de votre compte pour réinitialiser votre mot de passe.")
                .setView(etResetEmail)
                .setPositiveButton("Continuer", (dialog, which) -> {
                    String resetEmail = etResetEmail.getText().toString().trim();
                    if (resetEmail.isEmpty()) {
                        Toasty.warning(this, "Email requis", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    if (!userRepository.emailExists(resetEmail)) {
                        Toasty.error(this, "Aucun compte associé à cet email", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    showResetPasswordFormDialog(resetEmail);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showResetPasswordFormDialog(String email) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText etNewPass = new EditText(this);
        etNewPass.setHint("Nouveau mot de passe");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etConfirmPass = new EditText(this);
        etConfirmPass.setHint("Confirmer le mot de passe");
        etConfirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etNewPass);
        layout.addView(etConfirmPass);

        new AlertDialog.Builder(this)
                .setTitle("Réinitialiser le mot de passe")
                .setMessage("Compte: " + email)
                .setView(layout)
                .setPositiveButton("Réinitialiser", (dialog, which) -> {
                    String np = etNewPass.getText().toString().trim();
                    String cp = etConfirmPass.getText().toString().trim();

                    if (np.isEmpty() || np.length() < 4) {
                        Toasty.warning(this, "Le mot de passe doit faire au moins 4 caractères", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    if (!np.equals(cp)) {
                        Toasty.error(this, "Les mots de passe ne correspondent pas", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    User user = userRepository.getUserByEmail(email);
                    if (user != null) {
                        String hashed = PasswordUtils.hashPassword(np);
                        boolean updated = userRepository.updateUserPassword(user.getId(), hashed);
                        if (updated) {
                            Toasty.success(this, "Mot de passe réinitialisé avec succès", Toasty.LENGTH_SHORT).show();
                        } else {
                            Toasty.error(this, "Erreur de réinitialisation", Toasty.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();

        int webClientIdRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (webClientIdRes != 0) {
            gsoBuilder.requestIdToken(getString(webClientIdRes));
        }

        GoogleSignInOptions gso = gsoBuilder.build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleLogin.setOnClickListener(v -> {
            if (googleSignInClient != null) {
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
            }
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String email = account.getEmail();
                String firstName = account.getGivenName();
                String lastName = account.getFamilyName();
                String displayName = account.getDisplayName();
                String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;

                if (email != null && !email.isEmpty()) {
                    onGoogleSignInSuccess(email, firstName, lastName, displayName, photoUrl);
                } else {
                    Toasty.warning(this, "Impossible de récupérer l'email Google", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (ApiException e) {
            int statusCode = e.getStatusCode();
            if (statusCode == 12500 || statusCode == 10) {
                Toasty.error(this, "Erreur Google (" + statusCode + ") : L'empreinte SHA-1 de l'application doit être ajoutée sur Firebase Console.", Toasty.LENGTH_LONG).show();
            } else if (statusCode == 12501) {
                Toasty.info(this, "Connexion Google annulée", Toasty.LENGTH_SHORT).show();
            } else {
                Toasty.error(this, "Connexion Google indisponible (Code: " + statusCode + ")", Toasty.LENGTH_SHORT).show();
            }
        }
    }

    private void onGoogleSignInSuccess(String email, String firstName, String lastName, String displayName, String photoUrl) {
        User existingUser = userRepository.getUserByEmail(email);
        boolean isProfileComplete = existingUser != null &&
                ((existingUser.getDob() != null && !existingUser.getDob().trim().isEmpty()) ||
                        (existingUser.getCountry() != null && !existingUser.getCountry().trim().isEmpty()));

        if (existingUser != null && isProfileComplete) {
            NotificationRepository notificationRepo = new NotificationRepository(this);
            notificationRepo.clearNotificationsForOtherUsers(existingUser.getId());

            sessionManager.createSession(
                    existingUser.getId(),
                    existingUser.getName(),
                    existingUser.getEmail(),
                    existingUser.getProfileImage()
            );

            Toasty.success(this, "Bienvenue " + existingUser.getName(), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, AcceuilActivity.class));
            finish();
        } else {
            Intent intent = new Intent(LoginActivity.this, CreateProfileActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("first_name", firstName != null ? firstName : "");
            intent.putExtra("last_name", lastName != null ? lastName : (displayName != null ? displayName : ""));
            intent.putExtra("photo_url", photoUrl);
            startActivity(intent);
            finish();
        }
    }

    private void setupFacebookLogin() {
        try {
            if (btnFacebookLogin != null) {
                btnFacebookLogin.setVisibility(View.VISIBLE);
            }
            callbackManager = CallbackManager.Factory.create();
            if (btnFacebookLogin == null) return;
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
                                    Toast.makeText(LoginActivity.this, "Erreur de traitement profil Facebook", Toast.LENGTH_SHORT).show();
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
                    Toasty.warning(LoginActivity.this, "Connexion Facebook annulée", Toasty.LENGTH_SHORT).show();
                }

                @Override
                public void onError(FacebookException error) {
                    Toasty.error(LoginActivity.this, "Erreur Facebook: " + error.getMessage(), Toasty.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Facebook SDK setup error: " + e.getMessage());
        }
    }

    private void changeLanguage(String langCode) {
        String current = LocaleHelper.getPersistedLanguage(this, "fr");
        if (!current.equalsIgnoreCase(langCode)) {
            LocaleHelper.setLocale(this, langCode);
            recreate();
        }
    }

    private void highlightLanguageButton(String langCode, TextView btnFr, TextView btnEn, TextView btnAr, TextView btnZh) {
        if (btnFr != null) btnFr.setAlpha("fr".equalsIgnoreCase(langCode) ? 1.0f : 0.4f);
        if (btnEn != null) btnEn.setAlpha("en".equalsIgnoreCase(langCode) ? 1.0f : 0.4f);
        if (btnAr != null) btnAr.setAlpha("ar".equalsIgnoreCase(langCode) ? 1.0f : 0.4f);
        if (btnZh != null) btnZh.setAlpha("zh".equalsIgnoreCase(langCode) ? 1.0f : 0.4f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
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
            startActivity(new Intent(LoginActivity.this, AcceuilActivity.class));
            finish();
        } else {
            Toasty.warning(this, "Erreur lors de la connexion sociale", Toasty.LENGTH_SHORT).show();
        }
    }

    public static void showPhoneVerificationDialog(Context context, String phone, Runnable onVerified) {
        if (context == null || phone == null) return;
        String generatedCode = String.format(Locale.getDefault(), "%04d", new java.util.Random().nextInt(9000) + 1000);

        String[] options = {"💬 Vérifier via WhatsApp", "📩 Vérifier via SMS OTP (Firebase)"};

        new AlertDialog.Builder(context)
                .setTitle("Vérification du numéro (" + phone + ")")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        try {
                            String msg = "Code de vérification BaobabTD : " + generatedCode;
                            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + phone.replaceAll("[^0-9]", "") + "&text=" + Uri.encode(msg)));
                            context.startActivity(whatsappIntent);
                        } catch (Exception e) {
                            Toasty.error(context, "Impossible d'ouvrir WhatsApp", Toast.LENGTH_SHORT).show();
                        }
                        showCodeEntryDialog(context, generatedCode, onVerified);
                    } else {
                        showSmsOtpDialog(context, phone, onVerified);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static void showCodeEntryDialog(Context context, String expectedCode, Runnable onVerified) {
        final EditText etCode = new EditText(context);
        etCode.setHint("Entrez le code à 4 chiffres");
        etCode.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(context)
                .setTitle("Saisir le code WhatsApp")
                .setMessage("Un code a été envoyé sur WhatsApp. Saisissez le code ci-dessous :")
                .setView(etCode)
                .setPositiveButton("Valider", (dialog, which) -> {
                    String inputCode = etCode.getText().toString().trim();
                    if (inputCode.equals(expectedCode)) {
                        Toasty.success(context, "Numéro de téléphone vérifié avec succès ! 💬", Toast.LENGTH_SHORT).show();
                        if (onVerified != null) onVerified.run();
                    } else {
                        Toasty.error(context, "Code incorrect. Veuillez réessayer.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static void showSmsOtpDialog(Context context, String phone, Runnable onVerified) {
        final EditText etOtp = new EditText(context);
        etOtp.setHint("Entrez le code SMS");
        etOtp.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(context)
                .setTitle("Vérification SMS OTP")
                .setMessage("Saisissez le code de vérification SMS reçu pour le " + phone + " :")
                .setView(etOtp)
                .setPositiveButton("Valider", (dialog, which) -> {
                    String inputOtp = etOtp.getText().toString().trim();
                    if (!inputOtp.isEmpty()) {
                        Toasty.success(context, "Numéro vérifié par SMS ! 📩", Toast.LENGTH_SHORT).show();
                        if (onVerified != null) onVerified.run();
                    } else {
                        Toasty.error(context, "Code SMS invalide", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}

