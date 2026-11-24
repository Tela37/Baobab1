package td.teladoum.baobab.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.db.entities.CallLogEntity;
import td.teladoum.baobab.repositories.CallLogRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité affichant l'historique de tous les appels émis/reçus par ordre du plus récent au plus ancien,
 * avec options de suppression individuelle ou totale.
 */
public class CallHistoryActivity extends AppCompatActivity {

    private RecyclerView rvCallHistory;
    private CallLogRepository callLogRepository;
    private SessionManager sessionManager;
    private int currentUserId;
    private CallHistoryAdapter adapter;
    private final List<CallLogEntity> callLogList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_history);

        View mainContainer = findViewById(android.R.id.content);
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

        Toolbar toolbar = findViewById(R.id.toolbarCallHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        callLogRepository = new CallLogRepository(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        rvCallHistory = findViewById(R.id.rvCallHistory);
        rvCallHistory.setLayoutManager(new LinearLayoutManager(this));
        rvCallHistory.setHasFixedSize(true);

        adapter = new CallHistoryAdapter(callLogList, new CallHistoryAdapter.OnCallLogActionListener() {
            @Override
            public void onDelete(CallLogEntity callLog) {
                callLogRepository.deleteCallLog(callLog.getId());
                Toasty.info(CallHistoryActivity.this, "Appel supprimé de l'historique", Toasty.LENGTH_SHORT).show();
                loadCallLogs();
            }
        });
        rvCallHistory.setAdapter(adapter);

        loadCallLogs();
    }

    private void loadCallLogs() {
        List<CallLogEntity> logs = callLogRepository.getCallLogs(currentUserId);
        callLogList.clear();
        callLogList.addAll(logs);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 4001, 0, "🗑️ Supprimer tout l'historique");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 4001) {
            new AlertDialog.Builder(this)
                    .setTitle("Effacer l'historique 🗑️")
                    .setMessage("Voulez-vous vraiment effacer tout l'historique des appels ?")
                    .setPositiveButton("Tout effacer", (dialog, which) -> {
                        callLogRepository.deleteAllCallLogs(currentUserId);
                        loadCallLogs();
                        Toasty.success(this, "Historique vidé", Toasty.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.ViewHolder> {
        private final List<CallLogEntity> list;
        private final OnCallLogActionListener listener;

        public interface OnCallLogActionListener {
            void onDelete(CallLogEntity callLog);
        }

        public CallHistoryAdapter(List<CallLogEntity> list, OnCallLogActionListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_call_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CallLogEntity log = list.get(position);
            holder.tvCallLogName.setText(log.getTargetName() != null ? log.getTargetName() : "Correspondant");
            holder.tvCallLogTime.setText(log.getCreatedAt() != null ? log.getCreatedAt() : "Récemment");

            if (log.isVideoCall()) {
                holder.imgCallTypeIcon.setImageResource(android.R.drawable.ic_menu_camera);
            } else {
                holder.imgCallTypeIcon.setImageResource(android.R.drawable.ic_menu_call);
            }

            ImageUtils.loadProfileImage(holder.itemView.getContext(), log.getTargetAvatar(), holder.imgCallLogAvatar);

            holder.ibDeleteCallLog.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(log);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgCallLogAvatar, imgCallTypeIcon;
            TextView tvCallLogName, tvCallLogTime;
            ImageButton ibDeleteCallLog;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgCallLogAvatar = itemView.findViewById(R.id.imgCallLogAvatar);
                imgCallTypeIcon = itemView.findViewById(R.id.imgCallTypeIcon);
                tvCallLogName = itemView.findViewById(R.id.tvCallLogName);
                tvCallLogTime = itemView.findViewById(R.id.tvCallLogTime);
                ibDeleteCallLog = itemView.findViewById(R.id.ibDeleteCallLog);
            }
        }
    }
}
