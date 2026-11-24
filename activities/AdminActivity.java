package td.teladoum.baobab.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import td.teladoum.baobab.helpers.DialogUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;
import td.teladoum.baobab.utils.TimeUtils;

/**
 * Panneau d'administration pour examiner et modérer les signalements et gérer les utilisateurs.
 */
public class AdminActivity extends AppCompatActivity {

    private RecyclerView rvReports, rvUsers;
    private Button btnTabReports, btnTabUsers;
    private ReportsAdapter reportsAdapter;
    private UsersAdapter usersAdapter;
    private final List<PostRepository.ReportItem> reportsList = new ArrayList<>();
    private final List<User> usersList = new ArrayList<>();
    private PostRepository postRepository;
    private UserRepository userRepository;
    private NotificationRepository notificationRepository;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

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

        Toolbar toolbar = findViewById(R.id.toolbarAdmin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        userRepository = new UserRepository(this);
        userRepository.syncSuperAdminIdFromCloud(null);
        adminViewModel = new androidx.lifecycle.ViewModelProvider(this).get(td.teladoum.baobab.viewmodels.AdminViewModel.class);
        setupViewModelObservers();
        notificationRepository = new NotificationRepository(this);
        SessionManager sessionManager = new SessionManager(this);
        User currentUser = userRepository.getUserById(sessionManager.getUserId());
        if (currentUser == null || !currentUser.isAdmin()) {
            Toasty.error(this, getString(R.string.toast_access_denied), Toasty.LENGTH_SHORT).show();
            finish();
            return;
        }

        postRepository = new PostRepository(this);

        SwitchMaterial switchSuperAdminControls = findViewById(R.id.switchSuperAdminControls);
        if (currentUser != null && (userRepository.isSuperAdmin(currentUser.getId()) || currentUser.getId() == 1)) {
            switchSuperAdminControls.setVisibility(View.VISIBLE);
            switchSuperAdminControls.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (usersAdapter != null) {
                    usersAdapter.setShowAdminAndLimit(isChecked);
                }
            });
        }

        Button btnNominateSuperAdmin = findViewById(R.id.btnNominateSuperAdmin);
        if (btnNominateSuperAdmin != null) {
            if (currentUser != null && userRepository.isSuperAdmin(currentUser.getId())) {
                btnNominateSuperAdmin.setVisibility(View.VISIBLE);
                btnNominateSuperAdmin.setOnClickListener(v -> showNominateSuperAdminDialog());
            } else {
                btnNominateSuperAdmin.setVisibility(View.GONE);
            }
        }

        btnTabReports = findViewById(R.id.btnTabReports);
        btnTabUsers = findViewById(R.id.btnTabUsers);
        Button btnPublishUpdate = findViewById(R.id.btnPublishUpdate);
        if (btnPublishUpdate != null) {
            btnPublishUpdate.setOnClickListener(v -> showPublishUpdateDialog());
        }
        Button publierAnnonce = findViewById(R.id.publierAnnonce);
        if (publierAnnonce != null) {
            publierAnnonce.setOnClickListener(v -> showPublishSimpleAnnouncementDialog());
        }
        Button btnManageBlueBadges = findViewById(R.id.btnManageBlueBadges);
        if (btnManageBlueBadges != null) {
            btnManageBlueBadges.setOnClickListener(v -> showManageBlueBadgesDialog());
        }
        listenToAnnouncementState();
        listenToRealtimeStatistics();
        listenToCertificationRequests();

        adminViewModel = new androidx.lifecycle.ViewModelProvider(this).get(td.teladoum.baobab.viewmodels.AdminViewModel.class);
        setupAdminViewModelObservers();

        rvReports = findViewById(R.id.rvReports);
        rvUsers = findViewById(R.id.rvUsers);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        reportsAdapter = new ReportsAdapter(reportsList, new OnReportActionListener() {
            @Override
            public void onDeletePost(PostRepository.ReportItem item) {
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle("Supprimer la publication")
                        .setMessage("Voulez-vous supprimer ce post signalé ? Cette action est irréversible.")
                        .setPositiveButton("Supprimer", (dialog, which) -> {
                            postRepository.deletePost(item.postId, item.reporterId);
                            postRepository.deleteReport(item.reportId);
                            Toasty.success(AdminActivity.this, "Publication supprimée", Toasty.LENGTH_SHORT).show();
                            loadReports();
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }

            @Override
            public void onDismiss(PostRepository.ReportItem item) {
                postRepository.deleteReport(item.reportId);
                Toasty.info(AdminActivity.this, "Signalement ignoré", Toasty.LENGTH_SHORT).show();
                loadReports();
            }
        });

        usersAdapter = new UsersAdapter(usersList, userRepository, new OnUserActionListener() {
            @Override
            public void onDeleteUser(User user) {
                int primarySuperAdminId = userRepository.getPrimarySuperAdminId();
                if (userRepository.isPrimarySuperAdmin(user.getId())) {
                    if (sessionManager.getUserId() != primarySuperAdminId) {
                        notificationRepository.addNotification(primarySuperAdminId, sessionManager.getUserId(), "ADMIN_SECURITY_ALERT", "USER", primarySuperAdminId, "⚠️ L'administrateur " + currentUser.getName() + " a tenté de supprimer le 1er Super Admin.");
                    }
                    Toasty.error(AdminActivity.this, getString(R.string.toast_cannot_delete_primary_admin), Toasty.LENGTH_SHORT).show();
                    return;
                }
                if (userRepository.isSuperAdmin(user.getId()) && !userRepository.isPrimarySuperAdmin(sessionManager.getUserId())) {
                    Toasty.error(AdminActivity.this, getString(R.string.toast_only_primary_can_delete_super_admin), Toasty.LENGTH_SHORT).show();
                    return;
                }
                if (user.getId() == sessionManager.getUserId() && userRepository.isSuperAdmin(user.getId())) {
                    showDeleteAccountWithSuccessorDialog(user);
                    return;
                }
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle(getString(R.string.dialog_title_delete_account))
                        .setMessage("Voulez-vous vraiment supprimer le compte de " + user.getName() + " ? Cette action supprimera définitivement toutes ses données.")
                        .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                            userRepository.deleteUser(user.getId(), sessionManager.getUserId());
                            Toasty.success(AdminActivity.this, getString(R.string.toast_account_deleted), Toasty.LENGTH_SHORT).show();
                            loadUsers();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            }

            @Override
            public void onSuspendUser(User user) {
                int primarySuperAdminId = userRepository.getPrimarySuperAdminId();
                if (userRepository.isPrimarySuperAdmin(user.getId())) {
                    if (sessionManager.getUserId() != primarySuperAdminId) {
                        notificationRepository.addNotification(primarySuperAdminId, sessionManager.getUserId(), "ADMIN_SECURITY_ALERT", "USER", primarySuperAdminId, "⚠️ L'administrateur " + currentUser.getName() + " a tenté de suspendre le 1er Super Admin.");
                    }
                    Toasty.error(AdminActivity.this, "Impossible de suspendre le 1er Super Admin (Fondateur) 🚫", Toasty.LENGTH_SHORT).show();
                    return;
                }
                if (userRepository.isSuperAdmin(user.getId()) && !userRepository.isPrimarySuperAdmin(sessionManager.getUserId())) {
                    Toasty.error(AdminActivity.this, "Seul le 1er Super Admin peut suspendre un autre Super Admin", Toasty.LENGTH_SHORT).show();
                    return;
                }
                boolean currentlySuspended = user.isSuspended();
                if (currentlySuspended) {
                    userRepository.suspendUser(user.getId(), false, null);
                    Toasty.success(AdminActivity.this, "Compte réactivé", Toasty.LENGTH_SHORT).show();
                    loadUsers();
                } else {
                    final EditText inputReason = new EditText(AdminActivity.this);
                    inputReason.setHint("Raison de la suspension (ex: Spam)");
                    new AlertDialog.Builder(AdminActivity.this)
                            .setTitle("Suspendre l'utilisateur 🚫")
                            .setView(inputReason)
                            .setPositiveButton("Suspendre", (dialog, which) -> {
                                String reason = inputReason.getText().toString().trim();
                                userRepository.suspendUser(user.getId(), true, reason.isEmpty() ? "Violation des règles" : reason);
                                Toasty.warning(AdminActivity.this, "Utilisateur suspendu", Toasty.LENGTH_SHORT).show();
                                loadUsers();
                            })
                            .setNegativeButton("Annuler", null)
                            .show();
                }
            }

            @Override
            public void onLimitShare(User user) {
                int primarySuperAdminId = userRepository.getPrimarySuperAdminId();
                if (userRepository.isPrimarySuperAdmin(user.getId())) {
                    if (sessionManager.getUserId() != primarySuperAdminId) {
                        notificationRepository.addNotification(primarySuperAdminId, sessionManager.getUserId(), "ADMIN_SECURITY_ALERT", "USER", primarySuperAdminId, "⚠️ L'administrateur " + currentUser.getName() + " a tenté de limiter le partage du 1er Super Admin.");
                    }
                    Toasty.error(AdminActivity.this, "Impossible de restreindre le 1er Super Admin (Fondateur) 🚫", Toasty.LENGTH_SHORT).show();
                    return;
                }
                if (userRepository.isSuperAdmin(user.getId()) && !userRepository.isPrimarySuperAdmin(sessionManager.getUserId())) {
                    Toasty.error(AdminActivity.this, "Seul le 1er Super Admin peut restreindre un autre Super Admin", Toasty.LENGTH_SHORT).show();
                    return;
                }
                String[] options = {"1 Heure", "24 Heures", "7 Jours", "30 Jours", "Permanent", "Retirer la limitation"};
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle("Limiter le partage pour " + user.getName())
                        .setItems(options, (dialog, which) -> {
                            Calendar calendar = Calendar.getInstance();
                            String untilDateStr = null;
                            String msg = "Limitation retirée";
                            switch (which) {
                                case 0:
                                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                                    msg = "Partage limité pour 1 heure";
                                    break;
                                case 1:
                                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                                    msg = "Partage limité pour 24 heures";
                                    break;
                                case 2:
                                    calendar.add(Calendar.DAY_OF_YEAR, 7);
                                    msg = "Partage limité pour 7 jours";
                                    break;
                                case 3:
                                    calendar.add(Calendar.DAY_OF_YEAR, 30);
                                    msg = "Partage limité pour 30 jours";
                                    break;
                                case 4:
                                    calendar.add(Calendar.YEAR, 100);
                                    msg = "Partage limité de façon permanente";
                                    break;
                                case 5:
                                    untilDateStr = null;
                                    msg = "Limitation de partage retirée";
                                    break;
                            }
                            if (which < 5) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                untilDateStr = sdf.format(calendar.getTime());
                            }
                            userRepository.limitUserSharing(user.getId(), untilDateStr);
                            Toasty.info(AdminActivity.this, msg, Toasty.LENGTH_SHORT).show();
                            loadUsers();
                        })
                        .show();
            }

            @Override
            public void onToggleAdmin(User user) {
                int primarySuperAdminId = userRepository.getPrimarySuperAdminId();
                if (userRepository.isPrimarySuperAdmin(user.getId())) {
                    if (sessionManager.getUserId() != primarySuperAdminId) {
                        notificationRepository.addNotification(primarySuperAdminId, sessionManager.getUserId(), "ADMIN_SECURITY_ALERT", "USER", primarySuperAdminId, "⚠️ L'administrateur " + currentUser.getName() + " a tenté de retirer les privilèges du 1er Super Admin.");
                    }
                    Toasty.error(AdminActivity.this, "Impossible de rétrograder le 1er Super Admin (Fondateur)", Toasty.LENGTH_SHORT).show();
                    return;
                }

                if (userRepository.isSuperAdmin(user.getId()) && !userRepository.isPrimarySuperAdmin(sessionManager.getUserId())) {
                    Toasty.error(AdminActivity.this, "Seul le 1er Super Admin peut modifier le rôle d'un autre Super Admin", Toasty.LENGTH_SHORT).show();
                    return;
                }

                if (userRepository.isSuperAdmin(user.getId()) && userRepository.isPrimarySuperAdmin(sessionManager.getUserId())) {
                    userRepository.removeSuperAdmin(user.getId(), sessionManager.getUserId());
                    Toasty.success(AdminActivity.this, user.getName() + " n'est plus Super Admin", Toasty.LENGTH_SHORT).show();
                    loadUsers();
                    return;
                }

                if (user.isAdmin() && !userRepository.isSuperAdmin(user.getId())) {
                    userRepository.setSimpleAdmin(user.getId(), false);
                    Toasty.success(AdminActivity.this, user.getName() + " n'est plus Administrateur", Toasty.LENGTH_SHORT).show();
                    loadUsers();
                    return;
                }

                String[] promoteOptions = {"Administrateur Simple 🛡️", "Super Admin 👑"};
                new AlertDialog.Builder(AdminActivity.this)
                        .setTitle("Promouvoir " + user.getName())
                        .setItems(promoteOptions, (dialog, which) -> {
                            if (which == 0) {
                                userRepository.setSimpleAdmin(user.getId(), true);
                                Toasty.success(AdminActivity.this, user.getName() + " est désormais Administrateur Simple 🛡️", Toasty.LENGTH_SHORT).show();
                            } else {
                                userRepository.appointSuperAdmin(user.getId());
                                Toasty.success(AdminActivity.this, user.getName() + " est désormais Super Admin 👑", Toasty.LENGTH_SHORT).show();
                            }
                            loadUsers();
                        })
                        .show();
            }
        });

        rvReports.setAdapter(reportsAdapter);
        rvUsers.setAdapter(usersAdapter);

        btnTabReports.setOnClickListener(v -> {
            btnTabReports.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B71C1C")));
            btnTabReports.setTextColor(Color.WHITE);
            btnTabUsers.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnTabUsers.setTextColor(Color.parseColor("#424242"));
            rvReports.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
            loadReports();
        });

        btnTabUsers.setOnClickListener(v -> {
            btnTabUsers.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B71C1C")));
            btnTabUsers.setTextColor(Color.WHITE);
            btnTabReports.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnTabReports.setTextColor(Color.parseColor("#424242"));
            rvUsers.setVisibility(View.VISIBLE);
            rvReports.setVisibility(View.GONE);
            loadUsers();
        });

        userRepository.syncAllUsersFromCloud(() -> {
            loadReports();
            loadUsers();
        });
    }

    private void loadReports() {
        List<PostRepository.ReportItem> list = postRepository.getReports();
        reportsList.clear();
        reportsList.addAll(list);
        reportsAdapter.notifyDataSetChanged();
    }

    private td.teladoum.baobab.viewmodels.AdminViewModel adminViewModel;

    private void setupAdminViewModelObservers() {
        if (adminViewModel == null) return;

        adminViewModel.getUsersLiveData().observe(this, users -> {
            if (users != null) {
                usersList.clear();
                usersList.addAll(users);
                if (usersAdapter != null) usersAdapter.notifyDataSetChanged();
            }
        });

        adminViewModel.getReportsLiveData().observe(this, reports -> {
            if (reports != null) {
                reportsList.clear();
                reportsList.addAll(reports);
                if (reportsAdapter != null) reportsAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadUsers() {
        List<User> list = userRepository.getAllUsers();
        usersList.clear();
        usersList.addAll(list);
        usersAdapter.notifyDataSetChanged();
        updateUserStatistics();
    }

    private com.google.firebase.database.DataSnapshot dailyLoginsSnapshot = null;
    private final java.util.Map<Integer, User> realtimeUsersMap = new java.util.concurrent.ConcurrentHashMap<>();

    private com.google.firebase.database.DatabaseReference usersRef;
    private com.google.firebase.database.ValueEventListener usersListener;
    private com.google.firebase.database.DatabaseReference dailyLoginsRef;
    private com.google.firebase.database.ValueEventListener dailyLoginsListener;
    private com.google.firebase.database.DatabaseReference certificationRef;
    private com.google.firebase.database.ValueEventListener certificationListener;

    private void listenToRealtimeStatistics() {
        usersRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users");
        usersListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        User u = snap.getValue(User.class);
                        if (u != null && u.getId() > 0) {
                            Boolean isOnlineObj = snap.child("isOnline").getValue(Boolean.class);
                            if (isOnlineObj == null) {
                                isOnlineObj = snap.child("online").getValue(Boolean.class);
                            }
                            if (isOnlineObj != null) {
                                u.setOnline(isOnlineObj);
                            }
                            realtimeUsersMap.put(u.getId(), u);
                            userRepository.cacheUser(u);
                        }
                    } catch (Exception ignored) {}
                }
                updateUserStatistics();
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };
        usersRef.addValueEventListener(usersListener);

        dailyLoginsRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("daily_logins");
        dailyLoginsListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                dailyLoginsSnapshot = snapshot;
                updateUserStatistics();
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };
        dailyLoginsRef.addValueEventListener(dailyLoginsListener);
    }

    private void updateUserStatistics() {
        TextView tvAdminStats = findViewById(R.id.tvAdminStats);
        if (tvAdminStats == null) return;

        List<User> list = userRepository.getAllUsers();
        for (User u : realtimeUsersMap.values()) {
            boolean exists = false;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getId() == u.getId()) {
                    list.set(i, u);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                list.add(u);
            }
        }

        int total = list.size();
        int male = 0;
        int female = 0;

        List<String> onlineUserNames = new ArrayList<>();

        for (User u : list) {
            String g = u.getGender();
            if (g != null) {
                String gl = g.toLowerCase().trim();
                if (gl.contains("masculin") || gl.equals("m") || gl.equals("homme")) {
                    male++;
                } else if (gl.contains("féminin") || gl.contains("feminin") || gl.equals("f") || gl.equals("femme")) {
                    female++;
                }
            }

            if (u.isOnline()) {
                String name = (u.getName() != null && !u.getName().trim().isEmpty()) ? u.getName() : u.getEmail();
                if (name != null && !onlineUserNames.contains(name)) {
                    onlineUserNames.add(name);
                }
            }
        }

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String currentMonthStr = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new java.util.Date());
        String currentYearStr = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new java.util.Date());

        int dailyActive = 0;
        int monthlyActive = 0;
        int yearlyActive = 0;

        if (dailyLoginsSnapshot != null && dailyLoginsSnapshot.exists()) {
            for (com.google.firebase.database.DataSnapshot dateSnap : dailyLoginsSnapshot.getChildren()) {
                String dateKey = dateSnap.getKey();
                if (dateKey != null) {
                    int countOnDate = (int) dateSnap.getChildrenCount();

                    if (dateKey.equals(todayStr)) {
                        dailyActive += countOnDate;
                    }

                    if (dateKey.startsWith(currentMonthStr)) {
                        monthlyActive += countOnDate;
                    }

                    if (dateKey.startsWith(currentYearStr)) {
                        yearlyActive += countOnDate;
                    }
                }
            }
        }

        if (dailyActive < onlineUserNames.size()) {
            dailyActive = onlineUserNames.size();
        }
        if (monthlyActive < dailyActive) {
            monthlyActive = dailyActive;
        }
        if (yearlyActive < monthlyActive) {
            yearlyActive = total > monthlyActive ? total : monthlyActive;
        }

        java.util.Collections.sort(onlineUserNames, String.CASE_INSENSITIVE_ORDER);

        String statsText = "📊 Statistiques de l'application :\n" +
                "• Total des abonnés : " + total + " (👨 " + male + " | 👩 " + female + ")\n" +
                "• 🟢 En ligne actuellement : " + onlineUserNames.size() + "\n (👉 Cliquer pour voir la liste)\n" +
                "• 🟢 Connectés aujourd'hui (Journalier) : " + dailyActive + "\n" +
                "• 📅 Connectés ce mois-ci (Mensuel) : " + monthlyActive + "\n" +
                "• 🗓️ Connectés cette année (Annuel) : " + yearlyActive;

        tvAdminStats.setText(statsText);
        final List<String> currentOnlineNames = new ArrayList<>(onlineUserNames);
        tvAdminStats.setOnClickListener(v -> showOnlineUsersListDialog(currentOnlineNames));
    }

    private void setupViewModelObservers() {
        if (adminViewModel == null) return;

        adminViewModel.getUsersLiveData().observe(this, users -> {
            if (users != null) {
                usersList.clear();
                usersList.addAll(users);
                if (usersAdapter != null) usersAdapter.notifyDataSetChanged();
            }
        });

        adminViewModel.getReportsLiveData().observe(this, reports -> {
            if (reports != null) {
                reportsList.clear();
                reportsList.addAll(reports);
                if (reportsAdapter != null) reportsAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showOnlineUsersListDialog(List<String> names) {
        if (names == null || names.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("🟢 En ligne actuellement (0)")
                    .setMessage("Aucun utilisateur n'est connecté pour le moment.")
                    .setPositiveButton("Fermer", null)
                    .show();
            return;
        }

        java.util.Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append("\n\n");
            sb.append((i + 1)).append(". ").append(names.get(i));
        }

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        scrollView.setPadding(padding, padding, padding, padding);

        TextView tvContent = new TextView(this);
        tvContent.setText(sb.toString());
        tvContent.setTextSize(15f);
        tvContent.setTextColor(Color.parseColor("#2E7D32"));
        tvContent.setTypeface(null, android.graphics.Typeface.BOLD);

        scrollView.addView(tvContent);

        new AlertDialog.Builder(this)
                .setTitle("🟢 En ligne actuellement (" + names.size() + ")")
                .setView(scrollView)
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void listenToCertificationRequests() {
        certificationRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("certification_requests");
        certificationListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                List<java.util.Map<String, Object>> requests = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Integer uId = child.child("userId").getValue(Integer.class);
                        String uName = child.child("userName").getValue(String.class);
                        String just = child.child("justification").getValue(String.class);
                        if (uId != null && uId > 0) {
                            java.util.Map<String, Object> req = new java.util.HashMap<>();
                            req.put("userId", uId);
                            req.put("userName", uName != null ? uName : "Utilisateur");
                            req.put("justification", just != null ? just : "");
                            requests.add(req);
                        }
                    } catch (Exception ignored) {}
                }
                if (!requests.isEmpty() && !isFinishing()) {
                    showCertificationReviewDialog(requests);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };
        certificationRef.addValueEventListener(certificationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        if (dailyLoginsRef != null && dailyLoginsListener != null) {
            dailyLoginsRef.removeEventListener(dailyLoginsListener);
        }
        if (certificationRef != null && certificationListener != null) {
            certificationRef.removeEventListener(certificationListener);
        }
    }

    private void showCertificationReviewDialog(List<java.util.Map<String, Object>> requests) {
        if (requests == null || requests.isEmpty()) return;

        String[] items = new String[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            java.util.Map<String, Object> req = requests.get(i);
            items[i] = "🌟 " + req.get("userName") + " : " + req.get("justification");
        }

        SessionManager sm = new SessionManager(this);
        new AlertDialog.Builder(this)
                .setTitle("🌟 Demandes de Certification (" + requests.size() + ")")
                .setItems(items, (dialog, which) -> {
                    java.util.Map<String, Object> req = requests.get(which);
                    int targetUserId = (int) req.get("userId");
                    String targetUserName = (String) req.get("userName");

                    String[] actions = {"Approuver (Accorder le Badge Bleu 🌟)", "Refuser la demande ❌"};
                    new AlertDialog.Builder(this)
                            .setTitle("Demande de " + targetUserName)
                            .setItems(actions, (dialog2, whichAction) -> {
                                if (whichAction == 0) {
                                    userRepository.setUserVerified(targetUserId, true);
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("certification_requests").child(String.valueOf(targetUserId)).removeValue();
                                    notificationRepository.addNotification(targetUserId, sm.getUserId(), "CERTIFICATION_APPROVED", "USER", targetUserId, "🎉 Félicitations ! Votre compte a été certifié et a obtenu le Badge Bleu 🌟 !");
                                    Toasty.success(AdminActivity.this, "Badge Bleu accordé à " + targetUserName + " 🌟", Toast.LENGTH_SHORT).show();
                                    loadUsers();
                                } else {
                                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("certification_requests").child(String.valueOf(targetUserId)).removeValue();
                                    notificationRepository.addNotification(targetUserId, sm.getUserId(), "CERTIFICATION_REJECTED", "USER", targetUserId, "Votre demande de certification bleue n'a pas été retenue.");
                                    Toasty.info(AdminActivity.this, "Demande refusée", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showNominateSuperAdminDialog() {
        SessionManager sm = new SessionManager(this);
        List<User> allUsers = userRepository.getAllUsers();
        List<User> candidates = new ArrayList<>();
        for (User u : allUsers) {
            if (u.getId() != sm.getUserId()) {
                candidates.add(u);
            }
        }

        if (candidates.isEmpty()) {
            Toasty.info(this, "Aucun autre utilisateur disponible", Toasty.LENGTH_SHORT).show();
            return;
        }

        String[] candidateNames = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            User c = candidates.get(i);
            candidateNames[i] = (c.getName() != null ? c.getName() : c.getEmail()) + " (" + (userRepository.isPrimarySuperAdmin(c.getId()) ? "1er Super Admin" : (userRepository.isSuperAdmin(c.getId()) ? "Super Admin" : (c.isAdmin() ? "Admin" : "Membre"))) + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Nommer / Gérer les Super Admins")
                .setItems(candidateNames, (dialog, which) -> {
                    User chosen = candidates.get(which);
                    boolean isPrimary = userRepository.isPrimarySuperAdmin(sm.getUserId());
                    if (isPrimary) {
                        String[] roles = {"Nommer Super Admin (Intérim)", "Transférer 1er Super Admin (Fondateur)"};
                        new AlertDialog.Builder(this)
                                .setTitle("Sélectionner le rôle pour " + chosen.getName())
                                .setItems(roles, (dialog2, whichRole) -> {
                                    if (whichRole == 0) {
                                        userRepository.appointSuperAdmin(chosen.getId());
                                        Toasty.success(AdminActivity.this, chosen.getName() + " est désormais Super Admin (Intérim) 👑", Toasty.LENGTH_SHORT).show();
                                    } else {
                                        userRepository.transferPrimarySuperAdmin(chosen.getId());
                                        Toasty.success(AdminActivity.this, chosen.getName() + " est désormais le 1er Super Admin 👑", Toasty.LENGTH_SHORT).show();
                                    }
                                    loadUsers();
                                })
                                .show();
                    } else {
                        userRepository.appointSuperAdmin(chosen.getId());
                        Toasty.success(AdminActivity.this, chosen.getName() + " est désormais Super Admin (Intérim) 👑", Toasty.LENGTH_SHORT).show();
                        loadUsers();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showDeleteAccountWithSuccessorDialog(User user) {
        SessionManager sm = new SessionManager(this);
        List<User> allUsers = userRepository.getAllUsers();
        List<User> candidates = new ArrayList<>();
        for (User u : allUsers) {
            if (u.getId() != user.getId()) {
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
            candidateNames[i] = (c.getName() != null ? c.getName() : c.getEmail()) + " (" + (userRepository.isPrimarySuperAdmin(c.getId()) ? "1er Super Admin" : (userRepository.isSuperAdmin(c.getId()) ? "Super Admin" : (c.isAdmin() ? "Admin" : "Membre"))) + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("👑 Nommer un successeur Super Admin")
                .setMessage("Avant de supprimer votre compte de Super Admin, vous devez désigner un successeur :")
                .setItems(candidateNames, (dialog, which) -> {
                    User chosen = candidates.get(which);
                    if (userRepository.isPrimarySuperAdmin(user.getId())) {
                        userRepository.transferPrimarySuperAdmin(chosen.getId());
                        Toasty.success(AdminActivity.this, chosen.getName() + " est désormais le nouveau 1er Super Admin 👑", Toasty.LENGTH_LONG).show();
                    } else {
                        userRepository.appointSuperAdmin(chosen.getId());
                        Toasty.success(AdminActivity.this, chosen.getName() + " est désormais Super Admin 👑", Toasty.LENGTH_LONG).show();
                    }
                    userRepository.deleteUser(user.getId(), sm.getUserId());
                    if (user.getId() != -1) {
                        NotificationRepository notificationRepo = new NotificationRepository(this);
                        notificationRepo.clearNotificationsForUser(user.getId());
                    }
                    sm.logout();
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

                    Toasty.success(AdminActivity.this, "Compte supprimé avec succès", Toasty.LENGTH_SHORT).show();
                    android.content.Intent intent = new android.content.Intent(AdminActivity.this, LoginActivity.class);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String currentAnnouncementText = null;

    private void listenToAnnouncementState() {
        Button publierAnnonce = findViewById(R.id.publierAnnonce);
        View layoutExistingAnnouncement = findViewById(R.id.layoutExistingAnnouncement);
        TextView tvAnnouncementContent = findViewById(R.id.tvAnnouncementContent);
        Button btnEditAnnouncement = findViewById(R.id.btnEditAnnouncement);
        Button btnDeleteAnnouncement = findViewById(R.id.btnDeleteAnnouncement);

        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("app_announcement")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        String note = null;
                        long timestamp = 0;
                        if (snapshot.hasChild("text")) {
                            note = snapshot.child("text").getValue(String.class);
                            Long ts = snapshot.child("timestamp").getValue(Long.class);
                            if (ts != null) timestamp = ts;
                        } else {
                            note = snapshot.getValue(String.class);
                        }

                        long THREE_DAYS_MS = 3 * 24 * 60 * 60 * 1000L;
                        if (timestamp > 0 && (System.currentTimeMillis() - timestamp) > THREE_DAYS_MS) {
                            snapshot.getRef().removeValue();
                            note = null;
                        }

                        currentAnnouncementText = note;

                        if (note != null && !note.trim().isEmpty()) {
                            if (publierAnnonce != null) publierAnnonce.setVisibility(View.GONE);
                            if (layoutExistingAnnouncement != null) layoutExistingAnnouncement.setVisibility(View.VISIBLE);
                            if (tvAnnouncementContent != null) tvAnnouncementContent.setText(note);
                        } else {
                            if (publierAnnonce != null) publierAnnonce.setVisibility(View.VISIBLE);
                            if (layoutExistingAnnouncement != null) layoutExistingAnnouncement.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });

        if (btnEditAnnouncement != null) {
            btnEditAnnouncement.setOnClickListener(v -> showEditAnnouncementDialog());
        }

        if (btnDeleteAnnouncement != null) {
            btnDeleteAnnouncement.setOnClickListener(v -> showDeleteAnnouncementConfirmDialog());
        }
    }

    private void showEditAnnouncementDialog() {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.admin_announcement_hint));
        if (currentAnnouncementText != null) {
            input.setText(currentAnnouncementText);
        }
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_edit_announcement))
                .setView(container)
                .setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    if (!note.isEmpty()) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("text", note);
                        map.put("timestamp", System.currentTimeMillis());

                        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("app_announcement")
                                .setValue(map)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toasty.success(this, getString(R.string.toast_announcement_updated), Toasty.LENGTH_SHORT).show();
                                    } else {
                                        Toasty.error(this, getString(R.string.toast_profile_update_error), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteAnnouncementConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_delete_announcement))
                .setMessage(getString(R.string.dialog_msg_delete_announcement))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("app_announcement")
                            .removeValue()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toasty.success(this, getString(R.string.toast_announcement_deleted), Toasty.LENGTH_SHORT).show();
                                } else {
                                    Toasty.error(this, getString(R.string.toast_publish_error), Toasty.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showPublishSimpleAnnouncementDialog() {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.admin_announcement_hint));
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.publier_une_annonce))
                .setView(container)
                .setPositiveButton(getString(R.string.publish_action), (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    if (!note.isEmpty()) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("text", note);
                        map.put("timestamp", System.currentTimeMillis());

                        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("app_announcement")
                                .setValue(map)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toasty.success(this, getString(R.string.toast_announcement_published), Toasty.LENGTH_SHORT).show();
                                    } else {
                                        Toasty.error(this, getString(R.string.toast_publish_error), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showPublishUpdateDialog() {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.admin_announcement_hint));
        input.setText("Une nouvelle version de BaobabTD est disponible ! Profitez des nouvelles fonctionnalités.");
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.admin_publierUpdate))
                .setView(container)
                .setPositiveButton(getString(R.string.publish_action), (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    if (!note.isEmpty()) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("text", note);
                        map.put("timestamp", System.currentTimeMillis());

                        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("app_announcement")
                                .setValue(map)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toasty.success(this, getString(R.string.toast_update_announcement_published), Toasty.LENGTH_SHORT).show();
                                    } else {
                                        Toasty.error(this, getString(R.string.toast_publish_error), Toasty.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }



    private interface OnReportActionListener {
        void onDeletePost(PostRepository.ReportItem item);
        void onDismiss(PostRepository.ReportItem item);
    }

    private interface OnUserActionListener {
        void onDeleteUser(User user);
        void onSuspendUser(User user);
        void onLimitShare(User user);
        void onToggleAdmin(User user);
    }

    private static class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {
        private final List<PostRepository.ReportItem> list;
        private final OnReportActionListener listener;

        public ReportsAdapter(List<PostRepository.ReportItem> list, OnReportActionListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PostRepository.ReportItem item = list.get(position);
            holder.tvReportReporter.setText("Signalé par : " + (item.reporterName != null ? item.reporterName : "Utilisateur"));
            holder.tvReportReason.setText("Raison : " + (item.reason != null && !item.reason.isEmpty() ? item.reason : "Contenu inapproprié"));
            holder.tvReportContent.setText(item.postContent != null ? item.postContent : "(Média ou contenu vide)");

            if (item.imagePath != null && !item.imagePath.trim().isEmpty()) {
                holder.imgReportImage.setVisibility(View.VISIBLE);
                ImageUtils.loadFullImage(holder.itemView.getContext(), item.imagePath, holder.imgReportImage);
            } else if (item.videoPath != null && !item.videoPath.trim().isEmpty()) {
                holder.imgReportImage.setVisibility(View.VISIBLE);
                ImageUtils.loadVideoThumbnail(holder.itemView.getContext(), item.videoPath, holder.imgReportImage);
            } else {
                holder.imgReportImage.setVisibility(View.GONE);
            }

            holder.btnDeletePost.setOnClickListener(v -> {
                if (listener != null) listener.onDeletePost(item);
            });

            holder.btnDismissReport.setOnClickListener(v -> {
                if (listener != null) listener.onDismiss(item);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvReportReporter, tvReportReason, tvReportContent;
            ImageView imgReportImage;
            Button btnDeletePost, btnDismissReport;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvReportReporter = itemView.findViewById(R.id.tvReportReporter);
                tvReportReason = itemView.findViewById(R.id.tvReportReason);
                tvReportContent = itemView.findViewById(R.id.tvReportContent);
                imgReportImage = itemView.findViewById(R.id.imgReportImage);
                btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
                btnDismissReport = itemView.findViewById(R.id.btnDismissReport);
            }
        }
    }

    private static class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
        private final List<User> list;
        private final UserRepository userRepository;
        private final OnUserActionListener listener;
        private boolean showAdminAndLimit = false;

        public UsersAdapter(List<User> list, UserRepository userRepository, OnUserActionListener listener) {
            this.list = list;
            this.userRepository = userRepository;
            this.listener = listener;
        }

        public void setShowAdminAndLimit(boolean show) {
            this.showAdminAndLimit = show;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = list.get(position);
            holder.tvUserName.setText(user.getName() != null ? user.getName() : "Utilisateur #" + user.getId());
            holder.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            if (user.getId() == 1) {
                holder.tvUserStatus.setText("👑 Super Admin");
                holder.tvUserStatus.setBackgroundColor(Color.parseColor("#FFF8E1"));
                holder.tvUserStatus.setTextColor(Color.parseColor("#F57F17"));
                holder.btnToggleAdmin.setVisibility(View.GONE);
                holder.btnSuspendUser.setVisibility(View.GONE);
                holder.btnDeleteUser.setVisibility(View.GONE);
                holder.btnLimitShare.setVisibility(View.GONE);
            } else {
                holder.btnToggleAdmin.setVisibility(showAdminAndLimit ? View.VISIBLE : View.GONE);
                holder.btnLimitShare.setVisibility(showAdminAndLimit ? View.VISIBLE : View.GONE);
                holder.btnSuspendUser.setVisibility(View.VISIBLE);
                holder.btnDeleteUser.setVisibility(View.VISIBLE);

                if (userRepository != null && userRepository.isSuperAdmin(user.getId())) {
                    holder.tvUserStatus.setText("👑 Super Admin");
                    holder.tvUserStatus.setBackgroundColor(Color.parseColor("#FFF8E1"));
                    holder.tvUserStatus.setTextColor(Color.parseColor("#F57F17"));
                    holder.btnToggleAdmin.setText("Rétrograder");
                } else if (user.isAdmin()) {
                    holder.tvUserStatus.setText("🛡️ Admin Simple");
                    holder.tvUserStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                    holder.tvUserStatus.setTextColor(Color.parseColor("#1565C0"));
                    holder.btnToggleAdmin.setText("Rétrograder");
                } else if (user.isSuspended()) {
                    holder.tvUserStatus.setText("Suspendu");
                    holder.tvUserStatus.setBackgroundColor(Color.parseColor("#FFCDD2"));
                    holder.tvUserStatus.setTextColor(Color.parseColor("#B71C1C"));
                    holder.btnSuspendUser.setText("Réactiver");
                    holder.btnToggleAdmin.setText("Promouvoir");
                } else {
                    holder.tvUserStatus.setText("Actif");
                    holder.tvUserStatus.setBackgroundColor(Color.parseColor("#C8E6C9"));
                    holder.tvUserStatus.setTextColor(Color.parseColor("#2E7D32"));
                    holder.btnSuspendUser.setText("Suspendre");
                    holder.btnToggleAdmin.setText("Promouvoir");
                }
            }

            StringBuilder restrictionText = new StringBuilder();
            if (user.isSuspended() && user.getSuspensionReason() != null) {
                restrictionText.append("Raison suspension: ").append(user.getSuspensionReason());
            }
            if (user.getShareLimitedUntil() != null) {
                if (restrictionText.length() > 0) restrictionText.append(" | ");
                restrictionText.append("Partage limité jusqu'au: ").append(user.getShareLimitedUntil());
            }

            if (restrictionText.length() > 0) {
                holder.tvUserRestriction.setText(restrictionText.toString());
                holder.tvUserRestriction.setVisibility(View.VISIBLE);
            } else {
                holder.tvUserRestriction.setVisibility(View.GONE);
            }

            holder.btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteUser(user);
            });

            holder.btnSuspendUser.setOnClickListener(v -> {
                if (listener != null) listener.onSuspendUser(user);
            });

            holder.btnLimitShare.setOnClickListener(v -> {
                if (listener != null) listener.onLimitShare(user);
            });

            holder.btnToggleAdmin.setOnClickListener(v -> {
                if (listener != null) listener.onToggleAdmin(user);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvUserName, tvUserEmail, tvUserStatus, tvUserRestriction;
            Button btnDeleteUser, btnSuspendUser, btnLimitShare, btnToggleAdmin;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
                tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
                tvUserRestriction = itemView.findViewById(R.id.tvUserRestriction);
                btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
                btnSuspendUser = itemView.findViewById(R.id.btnSuspendUser);
                btnLimitShare = itemView.findViewById(R.id.btnLimitShare);
                btnToggleAdmin = itemView.findViewById(R.id.btnToggleAdmin);
            }
        }
    }

    private void showManageBlueBadgesDialog() {
        String[] choices = {
            "📋 Voir les demandes de certification en attente",
            "🌟 Gérer / Retirer le badge bleu des utilisateurs certifiés"
        };
        new AlertDialog.Builder(this)
                .setTitle("Gestion des Badges Bleus 🛡️")
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        loadCertificationRequestsAndShow();
                    } else if (which == 1) {
                        showVerifiedUsersDialog();
                    }
                })
                .show();
    }

    private void loadCertificationRequestsAndShow() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("certification_requests")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        List<java.util.Map<String, Object>> requests = new ArrayList<>();
                        for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                java.util.Map<String, Object> map = (java.util.Map<String, Object>) snap.getValue();
                                if (map != null) requests.add(map);
                            } catch (Exception ignored) {}
                        }
                        if (requests.isEmpty()) {
                            Toasty.info(AdminActivity.this, "Aucune demande de certification en attente", Toasty.LENGTH_SHORT).show();
                        } else {
                            showCertificationReviewDialog(requests);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        Toasty.error(AdminActivity.this, "Erreur de chargement des demandes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showVerifiedUsersDialog() {
        List<User> allUsers = userRepository.getAllUsers();
        List<User> verifiedUsers = new ArrayList<>();
        for (User u : allUsers) {
            if (u.isVerified()) {
                verifiedUsers.add(u);
            }
        }

        if (verifiedUsers.isEmpty()) {
            Toasty.info(this, "Aucun utilisateur certifié (badge bleu) actuellement", Toasty.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[verifiedUsers.size()];
        for (int i = 0; i < verifiedUsers.size(); i++) {
            User u = verifiedUsers.get(i);
            names[i] = "🌟 " + (u.getName() != null ? u.getName() : "Utilisateur") + " (" + u.getEmail() + ")";
        }

        SessionManager sm = new SessionManager(this);
        new AlertDialog.Builder(this)
                .setTitle("Utilisateurs certifiés (" + verifiedUsers.size() + ")")
                .setItems(names, (dialog, which) -> {
                    User target = verifiedUsers.get(which);
                    new AlertDialog.Builder(this)
                            .setTitle("Retirer le badge bleu ⚠️")
                            .setMessage("Voulez-vous vraiment retirer le badge bleu de certification à " + target.getName() + " ?")
                            .setPositiveButton("Retirer le badge", (d, w) -> {
                                userRepository.setUserVerified(target.getId(), false);
                                notificationRepository.addNotification(target.getId(), sm.getUserId(), "CERTIFICATION_REVOKED", "USER", target.getId(), "Votre badge bleu de certification a été retiré par un administrateur.");
                                Toasty.success(AdminActivity.this, "Badge bleu retiré pour " + target.getName(), Toasty.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Annuler", null)
                            .show();
                })
                .show();
    }
}

