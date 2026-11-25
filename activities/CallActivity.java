package td.teladoum.baobab.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.models.CallSignal;
import td.teladoum.baobab.repositories.CallLogRepository;
import td.teladoum.baobab.repositories.ConversationRepository;
import td.teladoum.baobab.repositories.MessageRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité gérant les appels vocaux, appels vidéo et partage d'écran en temps réel via WebRTC et Firebase Signaling.
 */
public class CallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 101;
    private static final int SCREEN_CAPTURE_REQ_CODE = 102;

    private LinearLayout voiceCallContainer;
    private View videoContainer;
    private ShapeableImageView imgCallAvatar;
    private TextView tvCallName, tvCallStatus;
    private ImageButton btnToggleMute, btnEndCall, btnToggleVideo, btnToggleScreenShare;

    private SurfaceViewRenderer localVideoView, remoteVideoView;

    private boolean isVideoCall = false;
    private boolean isMuted = false;
    private boolean isVideoEnabled = true;
    private boolean isIncoming = false;
    private boolean isScreenSharing = false;

    private int receiverId = -1;
    private int currentUserId = -1;
    private String targetName = "";
    private String receiverProfileImage = "";
    private DatabaseReference callRef;
    private ValueEventListener callListener;

    // WebRTC fields
    private EglBase rootEglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

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
        btnToggleScreenShare = findViewById(R.id.btnToggleScreenShare);
        localVideoView = findViewById(R.id.localVideoView);
        remoteVideoView = findViewById(R.id.remoteVideoView);

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
        }

        checkPermissionsAndStart();

        btnToggleMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(!isMuted);
            }
            btnToggleMute.setAlpha(isMuted ? 0.5f : 1.0f);
            Toasty.info(this, isMuted ? "Microphone désactivé" : "Microphone activé", Toast.LENGTH_SHORT).show();
        });

        btnEndCall.setOnClickListener(v -> endCallAndExit());

        btnToggleVideo.setOnClickListener(v -> {
            if (!isVideoCall) {
                Toasty.warning(this, "Impossible d'activer la caméra en mode appel vocal", Toast.LENGTH_SHORT).show();
                return;
            }
            isVideoEnabled = !isVideoEnabled;
            if (localVideoTrack != null) {
                localVideoTrack.setEnabled(isVideoEnabled);
            }
            btnToggleVideo.setAlpha(isVideoEnabled ? 1.0f : 0.5f);
            Toasty.info(this, isVideoEnabled ? "Caméra activée" : "Caméra désactivée", Toast.LENGTH_SHORT).show();
        });

        btnToggleScreenShare.setOnClickListener(v -> {
            if (!isScreenSharing) {
                startScreenShare();
            } else {
                stopScreenShare();
            }
        });
    }

    private void checkPermissionsAndStart() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (isVideoCall) {
            perms.add(Manifest.permission.CAMERA);
        }
        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERMISSION_REQ_CODE);
        } else {
            initializeWebRTCAndStartCall();
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
                initializeWebRTCAndStartCall();
            } else {
                Toasty.error(this, getString(R.string.toast_call_permissions_required), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initializeWebRTCAndStartCall() {
        if (isVideoCall) {
            videoContainer.setVisibility(View.VISIBLE);
            voiceCallContainer.setVisibility(View.GONE);
            tvCallStatus.setText("Appel vidéo connecté 📹");
        } else {
            videoContainer.setVisibility(View.GONE);
            voiceCallContainer.setVisibility(View.VISIBLE);
            tvCallStatus.setText("Appel vocal connecté 📞");
        }

        // Initialize WebRTC
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions()
        );

        rootEglBase = EglBase.create();

        if (isVideoCall) {
            localVideoView.init(rootEglBase.getEglBaseContext(), null);
            localVideoView.setEnableHardwareScaler(true);
            localVideoView.setMirror(true);

            remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
            remoteVideoView.setEnableHardwareScaler(true);
            remoteVideoView.setMirror(false);
        }

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        createPeerConnection();

        if (isVideoCall) {
            startCamera();
        }

        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAudio", audioSource);
        if (peerConnection != null) {
            peerConnection.addTrack(localAudioTrack, java.util.Collections.singletonList("ARDMS"));
        }

        setupSignaling();
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                runOnUiThread(() -> {
                    if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                        tvCallStatus.setText("Appel connecté WebRTC ✅");
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                            iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                        tvCallStatus.setText("Connexion perdue...");
                    }
                });
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                sendIceCandidate(iceCandidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(org.webrtc.MediaStream mediaStream) {
                if (mediaStream.videoTracks.size() > 0 && isVideoCall) {
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                    runOnUiThread(() -> remoteVideoTrack.addSink(remoteVideoView));
                }
            }

            @Override
            public void onRemoveStream(org.webrtc.MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, org.webrtc.MediaStream[] mediaStreams) {
                org.webrtc.MediaStreamTrack track = rtpReceiver.track();
                if (track instanceof VideoTrack && isVideoCall) {
                    runOnUiThread(() -> ((VideoTrack) track).addSink(remoteVideoView));
                }
            }
        });
    }

    private void startCamera() {
        videoCapturer = createCameraCapturer();
        if (videoCapturer != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);

            localVideoTrack = peerConnectionFactory.createVideoTrack("ARDVS0", videoSource);
            localVideoTrack.addSink(localVideoView);
            if (peerConnection != null) {
                peerConnection.addTrack(localVideoTrack, java.util.Collections.singletonList("ARDMS"));
            }
        }
    }

    private VideoCapturer createCameraCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }
        for (String deviceName : enumerator.getDeviceNames()) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return capturer;
            }
        }
        return null;
    }

    private void startScreenShare() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQ_CODE);
    }

    private void stopScreenShare() {
        isScreenSharing = false;
        btnToggleScreenShare.setAlpha(1.0f);
        Toasty.info(this, "Partage d'écran arrêté", Toast.LENGTH_SHORT).show();
        if (isVideoCall) {
            if (videoCapturer != null) {
                try {
                    videoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            startCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCREEN_CAPTURE_REQ_CODE && resultCode == RESULT_OK && data != null) {
            isScreenSharing = true;
            btnToggleScreenShare.setAlpha(0.5f);
            Toasty.info(this, "Partage d'écran actif 🖥️", Toast.LENGTH_SHORT).show();

            if (videoCapturer != null) {
                try {
                    videoCapturer.stopCapture();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    stopScreenShare();
                }
            });

            surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", rootEglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);
        }
    }

    private void setupSignaling() {
        callRef = FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(receiverId));
        if (!isIncoming) {
            // Caller creates offer
            MediaConstraints constraints = new MediaConstraints();
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    peerConnection.setLocalDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sd) {}
                        @Override
                        public void onSetSuccess() {
                            SessionManager sm = new SessionManager(CallActivity.this);
                            CallSignal signal = new CallSignal(currentUserId, sm.getName(), sm.getProfileImage(), isVideoCall, System.currentTimeMillis());
                            signal.setType("offer");
                            signal.setSdp(sessionDescription.description);
                            callRef.setValue(signal);
                        }
                        @Override
                        public void onCreateFailure(String s) {}
                        @Override
                        public void onSetFailure(String s) {}
                    }, sessionDescription);
                }

                @Override
                public void onSetSuccess() {}
                @Override
                public void onCreateFailure(String s) {}
                @Override
                public void onSetFailure(String s) {}
            }, constraints);
        }

        // Listen for signals
        DatabaseReference myCallRef = FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(currentUserId));
        callListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                CallSignal signal = snapshot.getValue(CallSignal.class);
                if (signal == null) return;

                if ("offer".equals(signal.getType()) && isIncoming) {
                    SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, signal.getSdp());
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sd) {}
                        @Override
                        public void onSetSuccess() {
                            peerConnection.createAnswer(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription answerSdp) {
                                    peerConnection.setLocalDescription(new SdpObserver() {
                                        @Override
                                        public void onCreateSuccess(SessionDescription sd) {}
                                        @Override
                                        public void onSetSuccess() {
                                            CallSignal ansSignal = new CallSignal(currentUserId, targetName, receiverProfileImage, isVideoCall, System.currentTimeMillis());
                                            ansSignal.setType("answer");
                                            ansSignal.setSdp(answerSdp.description);
                                            FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(signal.getCallerId())).setValue(ansSignal);
                                        }
                                        @Override
                                        public void onCreateFailure(String s) {}
                                        @Override
                                        public void onSetFailure(String s) {}
                                    }, answerSdp);
                                }
                                @Override
                                public void onSetSuccess() {}
                                @Override
                                public void onCreateFailure(String s) {}
                                @Override
                                public void onSetFailure(String s) {}
                            }, new MediaConstraints());
                        }
                        @Override
                        public void onCreateFailure(String s) {}
                        @Override
                        public void onSetFailure(String s) {}
                    }, sdp);
                } else if ("answer".equals(signal.getType())) {
                    SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, signal.getSdp());
                    peerConnection.setRemoteDescription(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sd) {}
                        @Override
                        public void onSetSuccess() {}
                        @Override
                        public void onCreateFailure(String s) {}
                        @Override
                        public void onSetFailure(String s) {}
                    }, sdp);
                } else if ("ice".equals(signal.getType())) {
                    IceCandidate candidate = new IceCandidate(signal.getIceSdpMid(), signal.getIceSdpMLineIndex(), signal.getIceCandidate());
                    peerConnection.addIceCandidate(candidate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        myCallRef.addValueEventListener(callListener);
    }

    private void sendIceCandidate(IceCandidate candidate) {
        CallSignal signal = new CallSignal(currentUserId, targetName, receiverProfileImage, isVideoCall, System.currentTimeMillis());
        signal.setType("ice");
        signal.setIceCandidate(candidate.sdp);
        signal.setIceSdpMLineIndex(candidate.sdpMLineIndex);
        signal.setIceSdpMid(candidate.sdpMid);
        FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(receiverId)).setValue(signal);
    }

    private void endCallAndExit() {
        if (callRef != null) {
            callRef.removeValue();
        }
        FirebaseDatabase.getInstance().getReference("calls").child(String.valueOf(currentUserId)).removeValue();

        if (!isIncoming && receiverId != -1) {
            MessageRepository mRepo = new MessageRepository(this);
            ConversationRepository cRepo = new ConversationRepository(this);
            int convId = cRepo.createConversationIfNotExists(currentUserId, receiverId);
            String missedText = isVideoCall ? "Appel vidéo terminé" : "Appel vocal terminé";
            mRepo.sendMessage(convId, currentUserId, missedText, null);
        }

        cleanupWebRTC();
        Toasty.info(this, getString(R.string.toast_call_ended), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void cleanupWebRTC() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }
        if (peerConnection != null) {
            peerConnection.dispose();
            peerConnection = null;
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        if (localVideoView != null) {
            localVideoView.release();
        }
        if (remoteVideoView != null) {
            remoteVideoView.release();
        }
        if (rootEglBase != null) {
            rootEglBase.release();
            rootEglBase = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupWebRTC();
        if (callRef != null && callListener != null) {
            callRef.removeEventListener(callListener);
        }
    }
}
