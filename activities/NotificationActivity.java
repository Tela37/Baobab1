package td.teladoum.baobab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.NotificationAdapter;
import td.teladoum.baobab.models.AppNotification;
import td.teladoum.baobab.repositories.NotificationRepository;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité affichant la liste des notifications reçues pour les commentaires et réactions.
 * Permet d'ouvrir le post concerné et de marquer les notifications comme lues.
 */
public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private View layoutEmptyNotifications;
    private SwipeRefreshLayout swipeRefreshNotifications;

    private td.teladoum.baobab.viewmodels.NotificationViewModel viewModel;
    private NotificationRepository notificationRepository;
    private SessionManager sessionManager;

    private NotificationAdapter adapter;
    private final List<AppNotification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

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

        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        notificationRepository = new NotificationRepository(this);

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(td.teladoum.baobab.viewmodels.NotificationViewModel.class);
        viewModel.getNotificationsLiveData().observe(this, newNotifications -> {
            notificationList.clear();
            if (newNotifications != null) {
                notificationList.addAll(newNotifications);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            boolean isEmpty = notificationList.isEmpty();
            if (layoutEmptyNotifications != null) {
                layoutEmptyNotifications.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
            if (rvNotifications != null) {
                rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmptyNotifications = findViewById(R.id.layoutEmptyNotifications);
        swipeRefreshNotifications = findViewById(R.id.swipeRefreshNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setHasFixedSize(true);

        adapter = new NotificationAdapter(notificationList, notification -> {
            // Clic sur une notification : Redirection explicite selon le targetType et le type
            notificationRepository.markAllAsRead(sessionManager.getUserId());
            String targetType = notification.getTargetType();
            String type = notification.getType();

            // Vérification du type d'interaction (Post, Like, Reaction, Commentaire ou Mention)
            if ("POST".equalsIgnoreCase(targetType) ||
                    "POST_LIKE".equals(type) ||
                    "POST_REACTION".equals(type) ||
                    "POST_COMMENT".equals(type) ||
                    "COMMENT_MENTION".equals(type)) {
                // Redirection vers l'activité des commentaires de la publication concernée
                Intent intent = new Intent(NotificationActivity.this, CommenterActivity.class);
                intent.putExtra("post_id", notification.getTargetId());
                startActivity(intent);
            } else if ("USER".equalsIgnoreCase(targetType) || "FRIEND_REQUEST".equals(type) || "FRIEND_ACCEPT".equals(type)) {
                Intent intent = new Intent(NotificationActivity.this, AmiActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(NotificationActivity.this, AcceuilActivity.class);
                startActivity(intent);
            }
        });

        rvNotifications.setAdapter(adapter);

        if (swipeRefreshNotifications != null) {
            swipeRefreshNotifications.setOnRefreshListener(() -> {
                loadNotifications();
                swipeRefreshNotifications.setRefreshing(false);
            });
        }

        loadNotifications();
        td.teladoum.baobab.helpers.BottomNavigationHelper.setupBottomNavigation(this, td.teladoum.baobab.helpers.BottomNavigationHelper.TAB_NOTIFICATIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        td.teladoum.baobab.helpers.BottomNavigationHelper.setupBottomNavigation(this, td.teladoum.baobab.helpers.BottomNavigationHelper.TAB_NOTIFICATIONS);
        loadNotifications();
    }

    private void loadNotifications() {
        if (notificationRepository == null) return;

        List<AppNotification> newNotifications = notificationRepository.getNotificationsForUser(sessionManager.getUserId());
        notificationList.clear();
        notificationList.addAll(newNotifications);
        adapter.notifyDataSetChanged();

        boolean isEmpty = notificationList.isEmpty();
        if (layoutEmptyNotifications != null) {
            layoutEmptyNotifications.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvNotifications != null) {
            rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }

        notificationRepository.loadNotificationsFromCloud(sessionManager.getUserId(), () -> {
            List<AppNotification> cloudNotifications = notificationRepository.getNotificationsForUser(sessionManager.getUserId());
            notificationList.clear();
            notificationList.addAll(cloudNotifications);
            adapter.notifyDataSetChanged();

            boolean empty = notificationList.isEmpty();
            if (layoutEmptyNotifications != null) {
                layoutEmptyNotifications.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            if (rvNotifications != null) {
                rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 2001, 0, "Tout marquer comme lu");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 2001) {
            notificationRepository.markAllAsRead(sessionManager.getUserId());
            loadNotifications();
            Toasty.success(this, "Toutes les notifications ont été marquées comme lues", Toasty.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
