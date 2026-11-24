package td.teladoum.baobab.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;

/**
 * Activité de lecture vidéo intégrée pour les flux directs (MP4) et YouTube.
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private WebView webViewYouTube;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

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

        Toolbar toolbar = findViewById(R.id.toolbarVideo);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lecteur Vidéo 🎬");
        }

        playerView = findViewById(R.id.playerView);
        webViewYouTube = findViewById(R.id.webViewYouTube);

        String videoUrl = getIntent().getStringExtra("video_url");
        String title = getIntent().getStringExtra("video_title");
        if (title != null && !title.isEmpty() && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Toasty.error(this, "URL vidéo introuvable", Toasty.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
            playYouTubeVideo(videoUrl);
        } else {
            playDirectVideo(videoUrl);
        }
    }

    private void playDirectVideo(String url) {
        if (playerView != null) playerView.setVisibility(View.VISIBLE);
        if (webViewYouTube != null) webViewYouTube.setVisibility(View.GONE);

        try {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toasty.error(this, "Erreur de lecture vidéo : " + e.getMessage(), Toasty.LENGTH_LONG).show();
        }
    }

    private void playYouTubeVideo(String url) {
        if (playerView != null) playerView.setVisibility(View.GONE);
        if (webViewYouTube != null) webViewYouTube.setVisibility(View.VISIBLE);

        String videoId = "";
        if (url.contains("youtu.be/")) {
            videoId = url.substring(url.lastIndexOf("/") + 1);
        } else if (url.contains("watch?v=")) {
            int idx = url.indexOf("watch?v=");
            videoId = url.substring(idx + 8);
            int amp = videoId.indexOf("&");
            if (amp != -1) videoId = videoId.substring(0, amp);
        }

        String embedHtml = "<html><body style='margin:0;background:black;display:flex;justify-content:center;align-items:center;height:100vh;'>" +
                "<iframe width='100%' height='100%' src='https://www.youtube.com/embed/" + videoId + "?autoplay=1' frameborder='0' allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' allowfullscreen></iframe>" +
                "</body></html>";

        WebSettings webSettings = webViewYouTube.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewYouTube.setWebViewClient(new WebViewClient());
        webViewYouTube.loadData(embedHtml, "text/html", "utf-8");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null && !isInPictureInPictureMode()) {
            player.pause();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (player != null && player.isPlaying() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(new android.app.PictureInPictureParams.Builder().build());
        }
    }

    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        Toolbar toolbar = findViewById(R.id.toolbarVideo);
        if (isInPictureInPictureMode) {
            if (toolbar != null) toolbar.setVisibility(View.GONE);
            if (playerView != null) {
                playerView.hideController();
            }
        } else {
            if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
            if (playerView != null) {
                playerView.showController();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            menu.add(0, 4001, 0, "🖼️ PiP").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 4001) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                enterPictureInPictureMode(new android.app.PictureInPictureParams.Builder().build());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
