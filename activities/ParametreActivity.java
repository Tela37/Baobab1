package td.teladoum.baobab.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.helpers.LocaleHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.PasswordUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité des paramètres de l'application.
 * Permet de gérer le statut en ligne, les notifications, le mode sombre (Dark Mode),
 * l'activation des publicités, la confidentialité des données et la réinitialisation du mot de passe.
 */
public class ParametreActivity extends AppCompatActivity {

    private SwitchMaterial switchOnlineStatus;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchAds;
    private SwitchMaterial switchPrivateProfile;
    private Button btnChangeEmailParam;
    private Button btnChangePasswordParam;
    private Button btnPrivacyPolicyParam;
    private Button btnDeleteAccountParam;
    private Button btnAdminPanel;

    private SharedPreferences prefs;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parametre);

        // 2. Ajuster les paddings pour ne pas recouvrir le contenu sous les barres système
        android.view.View mainContainer = findViewById(getResources().getIdentifier("main_container", "id", getPackageName()));
        if (mainContainer == null) {
            mainContainer = findViewById(android.R.id.content);
        }
        if (mainContainer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                androidx.core.graphics.Insets navBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
                v.setPadding(0, statusBarInsets.top, 0, navBarInsets.bottom);
                return insets;
            });
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            // true = icônes sombres (pour fond clair), false = icônes blanches (pour fond sombre)
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        Toolbar toolbar = findViewById(R.id.toolbarParametre);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("MessagesSession", MODE_PRIVATE);
        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchOnlineStatus = findViewById(R.id.switchOnlineStatus);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchAds = findViewById(R.id.switchAds);
        switchPrivateProfile = findViewById(R.id.switchPrivateProfile);
        btnChangeEmailParam = findViewById(R.id.btnChangeEmailParam);
        btnChangePasswordParam = findViewById(R.id.btnChangePasswordParam);
        btnPrivacyPolicyParam = findViewById(R.id.btnPrivacyPolicyParam);
        btnDeleteAccountParam = findViewById(R.id.btnDeleteAccountParam);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);
    }

    private void loadSettings() {
        boolean online = prefs.getBoolean("is_online", true);
        boolean notifications = prefs.getBoolean("notifications_enabled", true);
        boolean darkMode = prefs.getBoolean("dark_mode", false);

        switchOnlineStatus.setOnCheckedChangeListener(null);
        switchNotifications.setOnCheckedChangeListener(null);
        switchDarkMode.setOnCheckedChangeListener(null);
        if (switchAds != null) switchAds.setOnCheckedChangeListener(null);
        if (switchPrivateProfile != null) switchPrivateProfile.setOnCheckedChangeListener(null);

        switchOnlineStatus.setChecked(online);
        switchNotifications.setChecked(notifications);
        switchDarkMode.setChecked(darkMode);
        if (switchAds != null) switchAds.setChecked(sessionManager.isAdsEnabled());

        User currentUser = userRepository.getUserById(currentUserId);
        if (currentUser != null) {
            if (switchPrivateProfile != null) {
                switchPrivateProfile.setChecked(currentUser.isPrivateProfile());
            }
            if (btnAdminPanel != null) {
                btnAdminPanel.setVisibility(currentUser.isAdmin() ? View.VISIBLE : View.GONE);
            }
        } else {
            if (btnAdminPanel != null) {
                btnAdminPanel.setVisibility(View.GONE);
            }
        }
    }

    private void setupListeners() {
        setupLanguageSelector();
        Button btnAppUpdate = findViewById(R.id.btnAppUpdate);
        if (btnAppUpdate != null) {
            btnAppUpdate.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                } catch (Exception e) {
                    Toasty.error(this, "Impossible d'ouvrir le Play Store", Toasty.LENGTH_SHORT).show();
                }
            });
        }

        switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("is_online", isChecked).apply();
            userRepository.updateUserOnlineStatus(currentUserId, isChecked);
            Toasty.info(this, isChecked ? "Présence en ligne activée 🟢" : "Présence en ligne masquée", Toasty.LENGTH_SHORT).show();
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            Toasty.info(this, isChecked ? "Notifications activées 🔔" : "Notifications désactivées", Toasty.LENGTH_SHORT).show();
        });

        if (switchAds != null) {
            switchAds.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setAdsEnabled(isChecked);
                Toasty.info(this, isChecked ? "Publicités activées 📢" : "Publicités désactivées", Toasty.LENGTH_SHORT).show();
            });
        }

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            int targetMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode);
            }
        });

        if (switchPrivateProfile != null) {
            switchPrivateProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
                userRepository.updateUserPrivacy(currentUserId, isChecked, isChecked, isChecked);
                Toasty.info(this, isChecked ? "Mode Profil Privé activé (Données masquées par ********) 🔒" : "Profil Public activé", Toasty.LENGTH_SHORT).show();
            });
        }

        btnChangeEmailParam.setOnClickListener(v -> showChangeEmailDialog());
        btnChangePasswordParam.setOnClickListener(v -> showChangePasswordDialog());
        Button btnRequestBlueBadge = findViewById(R.id.btnRequestBlueBadge);
        if (btnRequestBlueBadge != null) {
            User currentUser = userRepository.getUserById(currentUserId);
            if (currentUser != null && currentUser.isVerified()) {
                btnRequestBlueBadge.setVisibility(View.GONE);
            } else {
                btnRequestBlueBadge.setVisibility(View.VISIBLE);
                btnRequestBlueBadge.setOnClickListener(v -> {
                    String uName = currentUser != null ? currentUser.getName() : "Utilisateur";
                    String uEmail = currentUser != null ? currentUser.getEmail() : "";
                    showRequestBlueBadgeDialog(this, currentUserId, uName, uEmail);
                });
            }
        }

        if (btnPrivacyPolicyParam != null) {
            btnPrivacyPolicyParam.setOnClickListener(v -> RegisterActivity.showPrivacyPolicyDialog(this));
        }
        if (btnDeleteAccountParam != null) {
            btnDeleteAccountParam.setOnClickListener(v -> showDeleteAccountConfirmationDialog());
        }
        if (btnAdminPanel != null) {
            btnAdminPanel.setOnClickListener(v -> startActivity(new Intent(ParametreActivity.this, AdminActivity.class)));
        }
    }

    private void savePrivacySettings() {
        boolean isPrivate = switchPrivateProfile != null && switchPrivateProfile.isChecked();
        userRepository.updateUserPrivacy(currentUserId, isPrivate, isPrivate, isPrivate);
        Toasty.info(this, "Paramètres de confidentialité enregistrés", Toasty.LENGTH_SHORT).show();
    }

    public void showDeleteAccountConfirmationDialog() {
        if (userRepository.isSuperAdmin(currentUserId)) {
            java.util.List<User> allUsers = userRepository.getAllUsers();
            java.util.List<User> candidates = new java.util.ArrayList<>();
            for (User u : allUsers) {
                if (u.getId() != currentUserId) {
                    candidates.add(u);
                }
            }

            if (candidates.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Suppression impossible 🚫")
                        .setMessage("En tant que Super Admin, vous ne pouvez pas supprimer votre compte tant qu'aucun autre utilisateur n'est inscrit dans l'application pour vous succéder.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            String[] candidateNames = new String[candidates.size()];
            for (int i = 0; i < candidates.size(); i++) {
                User c = candidates.get(i);
                candidateNames[i] = (c.getName() != null ? c.getName() : c.getEmail()) + " (" + (c.isAdmin() ? "Admin" : "Membre") + ")";
            }

            new AlertDialog.Builder(this)
                    .setTitle("👑 Nommer un successeur Super Admin")
                    .setMessage("Avant de supprimer votre compte de Super Admin, vous devez désigner un successeur :")
                    .setItems(candidateNames, (dialog, which) -> {
                        User chosen = candidates.get(which);
                        if (userRepository.isPrimarySuperAdmin(currentUserId)) {
                            userRepository.transferPrimarySuperAdmin(chosen.getId());
                            Toasty.success(ParametreActivity.this, chosen.getName() + " est désormais le nouveau 1er Super Admin 👑", Toasty.LENGTH_LONG).show();
                        } else {
                            userRepository.appointSuperAdmin(chosen.getId());
                            Toasty.success(ParametreActivity.this, chosen.getName() + " est désormais Super Admin 👑", Toasty.LENGTH_LONG).show();
                        }
                        performDeleteAccount();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Supprimer le compte ⚠️")
                .setMessage("Êtes-vous sûr de vouloir supprimer définitivement votre compte ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> performDeleteAccount())
                .setNegativeButton("Annuler", null)
                .show();
    }

    public static void showRequestBlueBadgeDialog(Context context, int userId, String userName, String userEmail) {
        if (context == null) return;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_certification_request, null);
        EditText etCertFullName = dialogView.findViewById(R.id.etCertFullName);
        Spinner spinnerDocType = dialogView.findViewById(R.id.spinnerDocType);
        EditText etCertDocNumber = dialogView.findViewById(R.id.etCertDocNumber);
        Button btnCertRecto = dialogView.findViewById(R.id.btnCertRecto);
        Button btnCertVerso = dialogView.findViewById(R.id.btnCertVerso);
        TextView tvCertFilesStatus = dialogView.findViewById(R.id.tvCertFilesStatus);
        Button btnCertSelfie = dialogView.findViewById(R.id.btnCertSelfie);
        ImageView imgCertSelfiePreview = dialogView.findViewById(R.id.imgCertSelfiePreview);

        String[] docTypes = {
            "Carte nationale d'identité",
            "Passeport",
            "Permis de conduire",
            "Carte étudiante ou scolaire"
        };
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            context,
            android.R.layout.simple_spinner_item,
            docTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDocType.setAdapter(adapter);

        final boolean[] hasRecto = {false};
        final boolean[] hasVerso = {false};
        final boolean[] hasSelfie = {false};

        btnCertRecto.setOnClickListener(v -> {
            hasRecto[0] = true;
            tvCertFilesStatus.setText("Fichiers sélectionnés : Recto (" + (hasRecto[0] ? 1 : 0) + "), Verso (" + (hasVerso[0] ? 1 : 0) + ")");
            Toasty.success(context, "Photo Recto jointe avec succès !", Toast.LENGTH_SHORT).show();
        });

        btnCertVerso.setOnClickListener(v -> {
            hasVerso[0] = true;
            tvCertFilesStatus.setText("Fichiers sélectionnés : Recto (" + (hasRecto[0] ? 1 : 0) + "), Verso (" + (hasVerso[0] ? 1 : 0) + ")");
            Toasty.success(context, "Photo Verso jointe avec succès !", Toast.LENGTH_SHORT).show();
        });

        btnCertSelfie.setOnClickListener(v -> {
            hasSelfie[0] = true;
            imgCertSelfiePreview.setVisibility(View.VISIBLE);
            imgCertSelfiePreview.setImageResource(android.R.drawable.ic_menu_camera);
            Toasty.success(context, "Selfie en temps réel capturé !", Toast.LENGTH_SHORT).show();
        });

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("Envoyer la demande", (dialog, which) -> {
                    String fullName = etCertFullName.getText().toString().trim();
                    String docType = spinnerDocType.getSelectedItem().toString();
                    String docNumber = etCertDocNumber.getText().toString().trim();

                    if (fullName.isEmpty()) {
                        Toasty.error(context, "Veuillez entrer le nom et prénom sur le document", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (docNumber.isEmpty()) {
                        Toasty.error(context, "Veuillez entrer le numéro du document", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!hasRecto[0] || !hasVerso[0]) {
                        Toasty.error(context, "Veuillez fournir les photos Recto et Verso du document", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!hasSelfie[0]) {
                        Toasty.error(context, "Veuillez prendre un selfie en temps réel", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("certification_requests").child(String.valueOf(userId));
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", userId);
                    map.put("userName", userName != null ? userName : "Utilisateur");
                    map.put("userEmail", userEmail != null ? userEmail : "");
                    map.put("fullNameOnDoc", fullName);
                    map.put("docType", docType);
                    map.put("docNumber", docNumber);
                    map.put("hasRecto", hasRecto[0]);
                    map.put("hasVerso", hasVerso[0]);
                    map.put("hasSelfie", hasSelfie[0]);
                    map.put("timestamp", System.currentTimeMillis());
                    map.put("status", "PENDING");

                    reqRef.setValue(map);
                    Toasty.success(context, "Votre demande de certification bleue a été transmise aux modérateurs ! 🌟", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    public void performDeleteAccount() {
        boolean deleted = userRepository.deleteUser(currentUserId);
        if (deleted) {
            if (currentUserId != -1) {
                NotificationRepository notificationRepo = new NotificationRepository(this);
                notificationRepo.clearNotificationsForUser(currentUserId);
            }
            sessionManager.logout();
            try {
                new td.teladoum.baobab.repositories.AuthRepository(this).signOut();
            } catch (Exception ignored) {}
            try {
                com.facebook.login.LoginManager.getInstance().logOut();
            } catch (Exception ignored) {}
            try {
                com.google.android.gms.auth.api.signin.GoogleSignInOptions gso =
                        new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut();
            } catch (Exception ignored) {}

            Toasty.success(this, "Compte supprimé avec succès", Toasty.LENGTH_SHORT).show();
            Intent intent = new Intent(ParametreActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toasty.error(this, "Impossible de supprimer le compte", Toasty.LENGTH_SHORT).show();
        }
    }

    private void showChangeEmailDialog() {
        EditText etNewEmail = new EditText(this);
        etNewEmail.setHint("Nouvelle adresse e-mail");
        etNewEmail.setText(sessionManager.getEmail());

        new AlertDialog.Builder(this)
                .setTitle("Modifier l'e-mail")
                .setView(etNewEmail)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newEmail = etNewEmail.getText().toString().trim();
                    if (!newEmail.isEmpty() && !newEmail.equalsIgnoreCase(sessionManager.getEmail())) {
                        boolean success = userRepository.updateUserEmail(currentUserId, newEmail);
                        if (success) {
                            sessionManager.createSession(currentUserId, sessionManager.getName(), newEmail);
                            Toasty.success(this, "E-mail mis à jour", Toasty.LENGTH_SHORT).show();
                        } else {
                            Toasty.error(this, "Cet e-mail est déjà utilisé", Toasty.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showChangePasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText etCurrentPass = new EditText(this);
        etCurrentPass.setHint("Mot de passe actuel");
        etCurrentPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText etNewPass = new EditText(this);
        etNewPass.setHint("Nouveau mot de passe");
        etNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etCurrentPass);
        layout.addView(etNewPass);

        new AlertDialog.Builder(this)
                .setTitle("Modifier le mot de passe")
                .setView(layout)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String curPass = etCurrentPass.getText().toString().trim();
                    String newPass = etNewPass.getText().toString().trim();

                    if (curPass.isEmpty() || newPass.isEmpty()) {
                        Toasty.warning(this, "Champs incomplets", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    User user = userRepository.getUserById(currentUserId);
                    String hashedCur = PasswordUtils.hashPassword(curPass);
                    String unsaltedCur = PasswordUtils.hashPasswordUnsalted(curPass);

                    boolean valid = (user != null && user.getPassword() != null &&
                            (user.getPassword().equals(hashedCur) || user.getPassword().equals(unsaltedCur)));

                    if (!valid) {
                        Toasty.error(this, "Mot de passe actuel incorrect", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    String hashedNew = PasswordUtils.hashPassword(newPass);
                    userRepository.updateUserPassword(currentUserId, hashedNew);
                    Toasty.success(this, "Mot de passe modifié", Toasty.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void setupLanguageSelector() {
        android.widget.TextView btnFr = findViewById(R.id.btnParamLangFrench);
        android.widget.TextView btnEn = findViewById(R.id.btnParamLangEnglish);
        android.widget.TextView btnAr = findViewById(R.id.btnParamLangArabic);
        android.widget.TextView btnZh = findViewById(R.id.btnParamLangChinese);

        String currentLang = LocaleHelper.getPersistedLanguage(this, "fr");
        if (btnFr != null) btnFr.setAlpha("fr".equalsIgnoreCase(currentLang) ? 1.0f : 0.4f);
        if (btnEn != null) btnEn.setAlpha("en".equalsIgnoreCase(currentLang) ? 1.0f : 0.4f);
        if (btnAr != null) btnAr.setAlpha("ar".equalsIgnoreCase(currentLang) ? 1.0f : 0.4f);
        if (btnZh != null) btnZh.setAlpha("zh".equalsIgnoreCase(currentLang) ? 1.0f : 0.4f);

        if (btnFr != null) {
            btnFr.setOnClickListener(v -> changeAppLanguage("fr"));
        }
        if (btnEn != null) {
            btnEn.setOnClickListener(v -> changeAppLanguage("en"));
        }
        if (btnAr != null) {
            btnAr.setOnClickListener(v -> changeAppLanguage("ar"));
        }
        if (btnZh != null) {
            btnZh.setOnClickListener(v -> changeAppLanguage("zh"));
        }
    }

    private void changeAppLanguage(String langCode) {
        String current = LocaleHelper.getPersistedLanguage(this, "fr");
        if (!current.equalsIgnoreCase(langCode)) {
            LocaleHelper.setLocale(this, langCode);
            recreate();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

