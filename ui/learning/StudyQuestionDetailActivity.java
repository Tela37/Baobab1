package td.teladoum.baobab.ui.learning;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.StudyReplyAdapter;
import td.teladoum.baobab.models.StudyReply;
import td.teladoum.baobab.repositories.StudyHelpRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité affichant les détails d'un sujet d'entraide scolaire et permettant de poster, modifier ou signaler des réponses.
 */
public class StudyQuestionDetailActivity extends AppCompatActivity {

    private int questionId;
    private int authorUserId = -1;
    private String authorName, authorAvatar, title, content, category, imagePath, createdAt;

    private StudyHelpRepository repository;
    private SessionManager sessionManager;
    private UserRepository userRepository;

    private StudyReplyAdapter adapter;
    private final List<StudyReply> replyList = new ArrayList<>();

    private String savedReplyFilePath = null;
    private View layoutReplyMediaPreview;
    private ImageView imgReplyMediaPreview;
    private TextView tvReplyMediaName;

    private final androidx.activity.result.ActivityResultLauncher<String> replyMediaPickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    savedReplyFilePath = ImageUtils.saveImageToInternalStorage(this, uri);
                    if (savedReplyFilePath == null) savedReplyFilePath = uri.toString();
                    if (layoutReplyMediaPreview != null) layoutReplyMediaPreview.setVisibility(View.VISIBLE);
                    if (tvReplyMediaName != null) tvReplyMediaName.setText("Média attaché : " + uri.getLastPathSegment());
                    if (imgReplyMediaPreview != null) {
                        boolean isVideo = savedReplyFilePath.toLowerCase().endsWith(".mp4") || savedReplyFilePath.toLowerCase().contains("youtube") || savedReplyFilePath.toLowerCase().endsWith(".mov");
                        if (isVideo) {
                            ImageUtils.loadVideoThumbnail(this, savedReplyFilePath, imgReplyMediaPreview);
                        } else {
                            ImageUtils.loadStoryImage(this, savedReplyFilePath, imgReplyMediaPreview);
                        }
                    }
                    Toasty.success(this, "Média attaché avec succès !", Toasty.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_question_detail);

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

        Toolbar toolbar = findViewById(R.id.toolbarStudyDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détails du Sujet 💬");
        }

        questionId = getIntent().getIntExtra("question_id", -1);
        authorUserId = getIntent().getIntExtra("author_user_id", -1);
        authorName = getIntent().getStringExtra("author_name");
        authorAvatar = getIntent().getStringExtra("author_avatar");
        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        category = getIntent().getStringExtra("category");
        imagePath = getIntent().getStringExtra("image_path");
        createdAt = getIntent().getStringExtra("created_at");

        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);
        repository = new StudyHelpRepository(this);

        ImageView imgAvatar = findViewById(R.id.imgDetailAuthorAvatar);
        TextView tvName = findViewById(R.id.tvDetailAuthorName);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        TextView tvCategory = findViewById(R.id.tvDetailCategory);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvContent = findViewById(R.id.tvDetailContent);
        ImageView imgImage = findViewById(R.id.imgDetailImage);

        tvName.setText(authorName != null ? authorName : "Élève");
        tvTime.setText(formatTime(createdAt));
        tvCategory.setText(category != null ? category : "Général");
        tvTitle.setText(title);
        tvContent.setText(content);

        if (imagePath != null && !imagePath.trim().isEmpty()) {
            boolean isVideo = isVideoPath(imagePath);
            boolean isPdf = isPdfPath(imagePath);
            if (isVideo) {
                if (imgImage != null) imgImage.setVisibility(View.GONE);
                View layoutDetailPdf = findViewById(R.id.layoutDetailPdf);
                if (layoutDetailPdf != null) layoutDetailPdf.setVisibility(View.GONE);
                View layoutDetailVideo = findViewById(R.id.layoutDetailVideo);
                ImageView imgDetailVideoThumbnail = findViewById(R.id.imgDetailVideoThumbnail);
                ImageView ibPlayDetailVideo = findViewById(R.id.ibPlayDetailVideo);
                if (layoutDetailVideo != null) {
                    layoutDetailVideo.setVisibility(View.VISIBLE);
                    ImageUtils.loadVideoThumbnail(this, imagePath, imgDetailVideoThumbnail);
                    View.OnClickListener vClick = v -> {
                        android.content.Intent intent = new android.content.Intent(this, td.teladoum.baobab.activities.VideoPlayerActivity.class);
                        intent.putExtra("video_url", imagePath);
                        intent.putExtra("video_title", title);
                        startActivity(intent);
                    };
                    layoutDetailVideo.setOnClickListener(vClick);
                    if (ibPlayDetailVideo != null) ibPlayDetailVideo.setOnClickListener(vClick);
                }
            } else if (isPdf) {
                if (imgImage != null) imgImage.setVisibility(View.GONE);
                View layoutDetailVideo = findViewById(R.id.layoutDetailVideo);
                if (layoutDetailVideo != null) layoutDetailVideo.setVisibility(View.GONE);
                View layoutDetailPdf = findViewById(R.id.layoutDetailPdf);
                TextView tvDetailPdfFileName = findViewById(R.id.tvDetailPdfFileName);
                android.widget.Button btnOpenDetailPdf = findViewById(R.id.btnOpenDetailPdf);
                if (layoutDetailPdf != null) {
                    layoutDetailPdf.setVisibility(View.VISIBLE);
                    if (tvDetailPdfFileName != null) tvDetailPdfFileName.setText(imagePath);
                    View.OnClickListener pClick = v -> {
                        android.content.Intent intent = new android.content.Intent(this, PdfViewerActivity.class);
                        intent.putExtra("pdf_path", imagePath);
                        intent.putExtra("pdf_title", title);
                        startActivity(intent);
                    };
                    layoutDetailPdf.setOnClickListener(pClick);
                    if (btnOpenDetailPdf != null) btnOpenDetailPdf.setOnClickListener(pClick);
                }
            } else {
                View layoutDetailVideo = findViewById(R.id.layoutDetailVideo);
                View layoutDetailPdf = findViewById(R.id.layoutDetailPdf);
                if (layoutDetailVideo != null) layoutDetailVideo.setVisibility(View.GONE);
                if (layoutDetailPdf != null) layoutDetailPdf.setVisibility(View.GONE);
                imgImage.setVisibility(View.VISIBLE);
                ImageUtils.loadFullImage(this, imagePath, imgImage);
            }
        } else {
            imgImage.setVisibility(View.GONE);
            View layoutDetailVideo = findViewById(R.id.layoutDetailVideo);
            View layoutDetailPdf = findViewById(R.id.layoutDetailPdf);
            if (layoutDetailVideo != null) layoutDetailVideo.setVisibility(View.GONE);
            if (layoutDetailPdf != null) layoutDetailPdf.setVisibility(View.GONE);
        }

        ImageUtils.loadProfileImage(this, authorAvatar, imgAvatar);

        RecyclerView rvReplies = findViewById(R.id.rvStudyReplies);
        rvReplies.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new StudyReplyAdapter(replyList, sessionManager.getUserId(), new StudyReplyAdapter.OnReplyActionListener() {
            @Override
            public void onEditReply(StudyReply reply) {
                showEditReplyDialog(reply);
            }

            @Override
            public void onReportReply(StudyReply reply) {
                showReportReplyDialog(reply);
            }

            @Override
            public void onLikeReply(StudyReply reply) {
                boolean isLiked = repository.toggleLikeReply(questionId, reply.getId(), sessionManager.getUserId(), null);
                if (isLiked) {
                    Toasty.success(StudyQuestionDetailActivity.this, "J'aime ajouté ❤️", Toasty.LENGTH_SHORT).show();
                } else {
                    Toasty.info(StudyQuestionDetailActivity.this, "J'aime retiré 🤍", Toasty.LENGTH_SHORT).show();
                }
                adapter.updateData(repository.getReplies(questionId));
            }

            @Override
            public void onReactionReply(StudyReply reply, String reactionType) {
                repository.setReplyReaction(questionId, reply.getId(), sessionManager.getUserId(), reactionType, null);
                Toasty.success(StudyQuestionDetailActivity.this, "Réaction enregistrée !", Toasty.LENGTH_SHORT).show();
                adapter.updateData(repository.getReplies(questionId));
            }
        });
        rvReplies.setAdapter(adapter);

        EditText etReply = findViewById(R.id.etReplyContent);
        ImageButton btnSend = findViewById(R.id.btnSendReply);
        android.widget.ProgressBar progressSend = findViewById(R.id.progressSendReply);

        layoutReplyMediaPreview = findViewById(R.id.layoutReplyMediaPreview);
        imgReplyMediaPreview = findViewById(R.id.imgReplyMediaPreview);
        tvReplyMediaName = findViewById(R.id.tvReplyMediaName);
        ImageButton btnCloseReplyMedia = findViewById(R.id.btnCloseReplyMedia);
        ImageButton btnAttachReply = findViewById(R.id.btnAttachReply);

        if (btnAttachReply != null) {
            btnAttachReply.setOnClickListener(v -> replyMediaPickerLauncher.launch("*/*"));
        }

        if (btnCloseReplyMedia != null) {
            btnCloseReplyMedia.setOnClickListener(v -> {
                savedReplyFilePath = null;
                if (layoutReplyMediaPreview != null) layoutReplyMediaPreview.setVisibility(View.GONE);
            });
        }

        btnSend.setOnClickListener(v -> {
            String replyText = etReply.getText().toString().trim();
            if (replyText.isEmpty() && (savedReplyFilePath == null || savedReplyFilePath.trim().isEmpty())) {
                Toasty.warning(this, "Veuillez saisir une réponse ou joindre un média", Toasty.LENGTH_SHORT).show();
                return;
            }

            btnSend.setVisibility(View.GONE);
            etReply.setEnabled(false);
            if (progressSend != null) progressSend.setVisibility(View.VISIBLE);

            int currentUserId = sessionManager.getUserId();
            td.teladoum.baobab.models.User currentUser = userRepository.getUserById(currentUserId);
            String name = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : sessionManager.getName();
            String avatar = (currentUser != null && currentUser.getProfileImage() != null) ? currentUser.getProfileImage() : sessionManager.getProfileImage();

            repository.publishReply(questionId, currentUserId, name, avatar, replyText, savedReplyFilePath, () -> {
                btnSend.setVisibility(View.VISIBLE);
                etReply.setEnabled(true);
                if (progressSend != null) progressSend.setVisibility(View.GONE);
                etReply.setText("");
                savedReplyFilePath = null;
                if (layoutReplyMediaPreview != null) layoutReplyMediaPreview.setVisibility(View.GONE);
                Toasty.success(StudyQuestionDetailActivity.this, "Réponse publiée avec succès ! 🚀", Toasty.LENGTH_SHORT).show();
            });
        });

        if (questionId != -1) {
            repository.syncRepliesFromCloud(questionId, () -> {
                List<StudyReply> cloudReplies = repository.getReplies(questionId);
                adapter.updateData(cloudReplies);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Détails (" + cloudReplies.size() + " réponses)");
                }
            });
        }
    }

    private void showEditReplyDialog(StudyReply reply) {
        final EditText etEdit = new EditText(this);
        etEdit.setText(reply.getContent());
        etEdit.setSelection(etEdit.getText().length());
        etEdit.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
                .setTitle("Modifier la réponse ✏️")
                .setView(etEdit)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newText = etEdit.getText().toString().trim();
                    if (!newText.isEmpty()) {
                        repository.editReply(questionId, reply.getId(), newText, () -> {
                            Toasty.success(this, "Réponse modifiée avec succès", Toasty.LENGTH_SHORT).show();
                            List<StudyReply> cloudReplies = repository.getReplies(questionId);
                            adapter.updateData(cloudReplies);
                        });
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showReportReplyDialog(StudyReply reply) {
        new AlertDialog.Builder(this)
                .setTitle("Signaler la réponse 🚩")
                .setMessage("Voulez-vous signaler cette réponse comme inappropriée ou contraire aux règles ?")
                .setPositiveButton("Signaler", (dialog, which) -> {
                    repository.reportReply(questionId, reply.getId(), () -> {
                        Toasty.success(this, "Réponse signalée et masquée", Toasty.LENGTH_SHORT).show();
                        List<StudyReply> cloudReplies = repository.getReplies(questionId);
                        adapter.updateData(cloudReplies);
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private String formatTime(String dateTime) {
        try {
            if (dateTime == null || dateTime.trim().isEmpty()) return "À l'instant";
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            if (date == null) return "À l'instant";
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM à HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "À l'instant";
        }
    }

    private boolean isVideoPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") ||
                lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    private boolean isPdfPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".pdf") || lower.contains("pdf");
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        if (authorUserId != -1 && authorUserId == sessionManager.getUserId()) {
            menu.add(0, 3001, 0, "🗑️ Supprimer le sujet");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 3001) {
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer la publication 🗑️")
                    .setMessage("Voulez-vous vraiment supprimer cette publication ?")
                    .setPositiveButton("Supprimer", (dialog, which) -> {
                        repository.deleteQuestion(questionId, () -> {
                            Toasty.success(this, "Publication supprimée", Toasty.LENGTH_SHORT).show();
                            finish();
                        });
                    })
                    .setNegativeButton("Annuler", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
