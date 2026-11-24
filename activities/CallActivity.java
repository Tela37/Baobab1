package td.teladoum.baobab.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.models.CallSignal;
import td.teladoum.baobab.repositories.CallLogRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité gérant les appels vocaux et appels vidéo en temps réel entre utilisateurs (Signalisation Firebase).
 */
public class CallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 101;

    private LinearLayout voiceCallContainer;
    private View videoContainer;
    private ShapeableImageView imgCallAvatar;
    private TextView tvCallName, tvCallStatus;
    private ImageButton btnToggleMute, btnEndCall, btnToggleVideo;

    private boolean isVideoCall = false;
    private boolean isMuted = false;
    private boolean isVideoEnabled = true;
    private boolean isIncoming = false;

    private int receiverId = -1;
    private int currentUserId = -1;
    private String targetName = "";
    private String receiverProfileImage = "";
    private DatabaseReference callRef;
    private ValueEventListener callListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

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

        voiceCallContainer = findViewById(R.id.voiceCallContainer);
        videoContainer = findViewById(R.id.videoContainer);
        imgCallAvatar = findViewById(R.id.imgCallAvatar);
        tvCallName = findViewById(R.id.tvCallName);
        tvCallStatus = findViewById(R.id.tvCallStatus);
        btnToggleMute = findViewById(R.id.btnToggleMute);
        btnEndCall = findViewById(R.id.btnEndCall);
        btnToggleVideo = findViewById(R.id.btnToggleVideo);

        SessionManager sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        targetName = getIntent().getStringExtra("target_name");
        receiverId = getIntent().getIntExtra("receiver_id", -1);
        receiverProfileImage = getIntent().getStringExtra("receiver_profile_image");
        isVideoCall = getIntent().getBooleanExtra("is_video_call", false);
        isIncoming = getIntent().getBooleanExtra("is_incoming", false);

        if (targetName != null) {
            tvCallName.setText(targetName);
        }
        ImageUtils.loadProfileImage(this, receiverProfileImage, imgCallAvatar);

        CallLogRepository callLogRepository = new CallLogRepository(this);
        if (!isIncoming && receiverId != -1) {
            callLogRepository.logCall(currentUserId, receiverId, targetName != null ? targetName : "Correspondant", receiverProfileImage, isVideoCall);
            sendCallOffer();
        } else if (isIncoming) {
            tvCallStatus.setText("Appel entrant...");
        }

        checkPermissionsAndStart();

        btnToggleMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            btnToggleMute.setAlpha(isMuted ? 0.5f : 1.0f);
            Toasty.info(this, isMuted ? "Microphone désactivé" : "Microphone activé", Toast.LENGTH_SHORT).show();
        });

        btnEndCall.setOnClickListener(v -> {
            endCallAndExit();
        });

        btnToggleVideo.setOnClickListener(v -> {
            isVideoEnabled = !isVideoEnabled;
            btnToggleVideo.setAlpha(isVideoEnabled ? 1.0f : 0.5f);
            if (isVideoCall) {
                videoContainer.setVisibility(isVideoEnabled ? View.VISIBLE : View.GONE);
                voiceCallContainer.setVisibility(isVideoEnabled ? View.GONE : View.VISIBLE);
            } else {
                Toasty.info(this, isVideoEnabled ? "Caméra activée" : "Caméra désactivée", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCallOffer() {
        if (receiverId == -1) return;
        SessionManager sm = new SessionManager(this);
        CallSignal signal = new CallSignal(currentUserId, sm.getName(), sm.getProfileImage(), isVideoCall, System.currentTimeMillis());
        callRef = FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(receiverId));
        callRef.setValue(signal);

        tvCallStatus.setText("Appel en cours (Sonnerie)...");

        callListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toasty.info(CallActivity.this, "Appel terminé par le correspondant", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        callRef.addValueEventListener(callListener);
    }

    private void endCallAndExit() {
        if (callRef != null) {
            callRef.removeValue();
        }
        if (isIncoming && receiverId != -1) {
            FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(currentUserId)).removeValue();
        } else if (!isIncoming && receiverId != -1) {
            td.teladoum.baobab.repositories.MessageRepository mRepo = new td.teladoum.baobab.repositories.MessageRepository(this);
            td.teladoum.baobab.repositories.ConversationRepository cRepo = new td.teladoum.baobab.repositories.ConversationRepository(this);
            int convId = cRepo.createConversationIfNotExists(currentUserId, receiverId);
            String missedText = isVideoCall ? "Appel vidéo manqué" : "Appel vocal manqué";
            mRepo.sendMessage(convId, currentUserId, missedText, null);
        }
        Toasty.info(this, getString(R.string.toast_call_ended), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callRef != null && callListener != null) {
            callRef.removeEventListener(callListener);
        }
    }

    private void checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                (isVideoCall && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            }, PERMISSION_REQ_CODE);
        } else {
            startCallSetup();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            boolean granted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                startCallSetup();
            } else {
                Toasty.error(this, getString(R.string.toast_call_permissions_required), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCallSetup() {
        if (isVideoCall) {
            videoContainer.setVisibility(View.VISIBLE);
            voiceCallContainer.setVisibility(View.GONE);
            tvCallStatus.setText("Appel vidéo connecté 📹");
        } else {
            videoContainer.setVisibility(View.GONE);
            voiceCallContainer.setVisibility(View.VISIBLE);
            tvCallStatus.setText("Appel vocal connecté 📞");
        }
    }
}
