package td.teladoum.baobab.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import td.teladoum.baobab.helpers.DialogUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.PostAdapter;
import td.teladoum.baobab.models.Post;
import td.teladoum.baobab.models.Story;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.models.UserStoryGroup;
import td.teladoum.baobab.repositories.ConversationRepository;
import td.teladoum.baobab.repositories.FriendRepository;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.StoryRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité de page de profil d'un utilisateur (Style Facebook complet).
 * Affiche la photo de couverture, l'avatar avec cadran story & indicateur de présence,
 * nom, bio, statistiques d'amis et publications, boutons Message et Ajouter,
 * ainsi que les informations de profil avec gestion de la confidentialité.
 */
public class User2PageActivity extends AppCompatActivity {

    private ImageView imgUser2Cover, imgProfileUserAvatar;
    private View viewStoryRing;
    private TextView tvUser2LastSeen, tvProfileUserName, tvUser2Badge, tvUser2Bio, tvUser2StatsSummary;
    private View cardQuickInfo;
    private TextView tvUser2Location, tvUser2Profession, tvUser2Dob, tvUser2Phone;
    private Button btnUser2AddFriend, btnUser2Message;

    private EditText etPostContent;
    private View layoutSelectedImage;
    private ImageView imgSelectedPostImage;
    private ImageButton btnRemoveSelectedImage;
    private ImageButton ibAttachImage;
    private Button btnPublishPost;
    private RecyclerView rvUserPosts;

    private UserRepository userRepository;
    private PostRepository postRepository;
    private FriendRepository friendRepository;
    private StoryRepository storyRepository;
    private ConversationRepository conversationRepository;

    private int targetUserId = -1;
    private int currentUserId;
    private String savedPostImagePath;

    private PostAdapter postAdapter;
    private final List<Post> userPostsList = new ArrayList<>();
    private AlertDialog activeImageDialog;
    private AlertDialog activeVideoDialog;
    private AlertDialog activeStoryDialog;
    private android.os.Handler storyTimerHandler;
    private Runnable storyTimerRunnable;

    private final ActivityResultLauncher<CropImageContractOptions> imagePickerLauncher =
            registerForActivityResult(new CropImageContract(), (CropImageView.CropResult result) -> {
                if (result.isSuccessful()) {
                    Uri uri = result.getUriContent();
                    if (uri != null) {
                        savedPostImagePath = ImageUtils.saveImageToInternalStorage(this, uri);
                        if (savedPostImagePath != null) {
                            imgSelectedPostImage.setImageURI(uri);
                            layoutSelectedImage.setVisibility(View.VISIBLE);
                        } else {
                            Toasty.error(this, "Erreur lors du chargement de l'image", Toasty.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user2_page);

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
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        Toolbar toolbar = findViewById(R.id.toolbarUser2Page);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        userRepository = new UserRepository(this);
        postRepository = new PostRepository(this);
        friendRepository = new FriendRepository(this);
        storyRepository = new StoryRepository(this);
        conversationRepository = new ConversationRepository(this);
        SessionManager sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        targetUserId = getIntent().getIntExtra("user_id", -1);
        if (targetUserId == currentUserId && targetUserId != -1) {
            startActivity(new Intent(this, PagePersonnelleActivity.class));
            finish();
            return;
        }

        String targetName = getIntent().getStringExtra("user_name");
        String targetImg = getIntent().getStringExtra("user_profile_image");

        if (targetUserId == -1) {
            String targetEmail = getIntent().getStringExtra("user_email");
            if (targetEmail != null && !targetEmail.isEmpty()) {
                User u = userRepository.getUserByEmail(targetEmail);
                if (u != null) targetUserId = u.getId();
            }
        }

        initViews();
        setupRecyclerView();
        loadUserInfo(targetName, targetImg);
        userRepository.syncAllUsersFromCloud(() -> {
            loadUserInfo(targetName, targetImg);
        });
        loadUserPosts();

        btnPublishPost.setOnClickListener(v -> publishPostOnProfile());
        ibAttachImage.setOnClickListener(v -> launchImageCropper());
        btnRemoveSelectedImage.setOnClickListener(v -> {
            savedPostImagePath = null;
            layoutSelectedImage.setVisibility(View.GONE);
        });

        imgProfileUserAvatar.setOnClickListener(v -> {
            List<UserStoryGroup> groups = storyRepository.getGroupedRecentStories();
            UserStoryGroup targetGroup = null;
            for (UserStoryGroup g : groups) {
                if (g.getUserId() == targetUserId) {
                    targetGroup = g;
                    break;
                }
            }
            if (targetGroup != null && !targetGroup.getStories().isEmpty()) {
                showStoryGroupViewerDialog(targetGroup);
            } else {
                User user = userRepository.getUserById(targetUserId);
                String imgPath = user != null ? user.getProfileImage() : targetImg;
                if (imgPath != null && !imgPath.isEmpty()) {
                    showImagePreviewDialog(imgPath);
                }
            }
        });

        tvProfileUserName.setOnClickListener(v -> openUser2Activity());
    }

    private void initViews() {
        imgUser2Cover = findViewById(R.id.imgUser2Cover);
        imgProfileUserAvatar = findViewById(R.id.imgProfileUserAvatar);
        viewStoryRing = findViewById(R.id.viewStoryRing);
        tvUser2LastSeen = findViewById(R.id.tvUser2LastSeen);
        tvProfileUserName = findViewById(R.id.tvProfileUserName);
        tvUser2Badge = findViewById(R.id.tvUser2Badge);
        tvUser2Bio = findViewById(R.id.tvUser2Bio);
        tvUser2StatsSummary = findViewById(R.id.tvUser2StatsSummary);
        cardQuickInfo = findViewById(R.id.cardQuickInfo);
        tvUser2Location = findViewById(R.id.tvUser2Location);
        tvUser2Profession = findViewById(R.id.tvUser2Profession);
        tvUser2Dob = findViewById(R.id.tvUser2Dob);
        tvUser2Phone = findViewById(R.id.tvUser2Phone);
        btnUser2AddFriend = findViewById(R.id.btnUser2AddFriend);
        btnUser2Message = findViewById(R.id.btnUser2Message);

        etPostContent = findViewById(R.id.etPostContent);
        layoutSelectedImage = findViewById(R.id.layoutSelectedImage);
        imgSelectedPostImage = findViewById(R.id.imgSelectedPostImage);
        btnRemoveSelectedImage = findViewById(R.id.btnRemoveSelectedImage);
        ibAttachImage = findViewById(R.id.ibAttachImage);
        btnPublishPost = findViewById(R.id.btnPublishPost);
        rvUserPosts = findViewById(R.id.rvUserPosts);
    }

    private void setupRecyclerView() {
        rvUserPosts.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(userPostsList, currentUserId, new PostAdapter.OnPostClickListener() {
            @Override
            public void onLikeClick(Post post) {
                postRepository.toggleLikePost(post.getId(), currentUserId);
                loadUserPosts();
            }

            @Override
            public void onReactionClick(Post post, String reactionType) {
                postRepository.setPostReaction(post.getId(), currentUserId, reactionType);
                loadUserPosts();
            }

            @Override
            public void onCommentClick(Post post) {
                Intent intent = new Intent(User2PageActivity.this, CommenterActivity.class);
                intent.putExtra("post_id", post.getId());
                startActivity(intent);
            }

            @Override
            public void onShareClick(Post post) {
                postRepository.recordPostShare(post.getId(), currentUserId);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Publication de " + post.getAuthorName() + " sur BaobabTD :\n" + (post.getContent() != null ? post.getContent() : ""));
                startActivity(Intent.createChooser(shareIntent, "Partager via"));
                loadUserPosts();
            }

            @Override
            public void onReportClick(Post post) {
                final EditText etReason = new EditText(User2PageActivity.this);
                etReason.setHint("Raison du signalement (ex: contenu inadapté)");
                new AlertDialog.Builder(User2PageActivity.this)
                        .setTitle("Signaler la publication 🚩")
                        .setMessage("Voulez-vous signaler cette publication aux modérateurs ?")
                        .setView(etReason)
                        .setPositiveButton("Signaler", (dialog, which) -> {
                            String reason = etReason.getText().toString().trim();
                            boolean reported = postRepository.reportPost(post.getId(), currentUserId, reason);
                            if (reported) {
                                Toasty.success(User2PageActivity.this, "Publication signalée aux modérateurs", Toasty.LENGTH_SHORT).show();
                            } else {
                                Toasty.error(User2PageActivity.this, "Erreur lors du signalement", Toasty.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }

            @Override
            public void onEditClick(Post post) {
            }

            @Override
            public void onDeleteClick(Post post) {
                if (post.getUserId() == currentUserId || targetUserId == currentUserId) {
                    postRepository.deletePost(post.getId(), currentUserId);
                    loadUserPosts();
                }
            }

            @Override
            public void onImageClick(String imagePath) {
                showImagePreviewDialog(imagePath);
            }

            @Override
            public void onImageClick(Post post) {
                showImagePreviewDialog(post);
            }

            @Override
            public void onVideoClick(String videoPath) {
                showVideoPlayerDialog(videoPath);
            }
        });
        rvUserPosts.setAdapter(postAdapter);
    }

    private void loadUserInfo(String fallbackName, String fallbackImg) {
        User user = userRepository.getUserById(targetUserId);
        String name = (user != null && user.getName() != null && !user.getName().trim().isEmpty()) ? user.getName() : fallbackName;
        String imgPath = (user != null && user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) ? user.getProfileImage() : fallbackImg;
        String coverPath = (user != null && user.getCoverImage() != null && !user.getCoverImage().trim().isEmpty()) ? user.getCoverImage() : null;

        tvProfileUserName.setText(name != null && !name.trim().isEmpty() ? name : "Utilisateur");
        if (imgPath != null && !imgPath.trim().isEmpty()) {
            ImageUtils.loadProfileImage(this, imgPath, imgProfileUserAvatar);
        } else {
            imgProfileUserAvatar.setImageResource(R.drawable.ic_profile);
        }

        if (imgUser2Cover != null) {
            if (coverPath != null && !coverPath.trim().isEmpty()) {
                ImageUtils.loadStoryImage(this, coverPath, imgUser2Cover);
            } else {
                imgUser2Cover.setImageResource(R.drawable.ic_profile);
            }
        }

        // Verified badge
        if (tvUser2Badge != null) {
            tvUser2Badge.setVisibility(user != null && user.isVerified() ? View.VISIBLE : View.GONE);
        }

        // Bio
        if (tvUser2Bio != null) {
            String bio = user != null && user.getBio() != null && !user.getBio().trim().isEmpty() ? user.getBio() : "Membre de BaobabTD";
            tvUser2Bio.setText(bio);
        }

        // Stats (Friends & Posts)
        int friendsCount = friendRepository != null ? friendRepository.getFriendsList(targetUserId, null).size() : 0;
        int postsCount = userPostsList.size();
        if (tvUser2StatsSummary != null) {
            tvUser2StatsSummary.setText(friendsCount + " ami(e)s • " + postsCount + " publications");
        }

        // Last seen / Active status (e.g. "4 heures" or online)
        if (tvUser2LastSeen != null) {
            if (user != null) {
                if (user.isOnline()) {
                    tvUser2LastSeen.setText("En ligne");
                } else {
                    tvUser2LastSeen.setText(formatLastSeenTime(user.getLastSeen()));
                }
            } else {
                tvUser2LastSeen.setText("Hors ligne");
            }
        }

        // Story ring
        if (viewStoryRing != null) {
            List<Story> stories = storyRepository.getRecentStories();
            boolean hasStory = false;
            for (Story s : stories) {
                if (s.getUserId() == targetUserId) {
                    hasStory = true;
                    break;
                }
            }
            viewStoryRing.setVisibility(hasStory ? View.VISIBLE : View.GONE);
        }

        // Quick info / En un coup d'œil with Privacy checks
        boolean isPrivate = user != null && user.isPrivateProfile();
        if (cardQuickInfo != null) {
            cardQuickInfo.setVisibility(isPrivate ? View.GONE : View.VISIBLE);
        }

        if (user != null) {
            if (tvUser2Location != null) {
                if (user.isHideLocation() || isPrivate) {
                    tvUser2Location.setVisibility(View.GONE);
                } else {
                    tvUser2Location.setVisibility(View.VISIBLE);
                    String loc = user.getDisplayLocation(currentUserId);
                    tvUser2Location.setText("Habite à " + (loc != null && !loc.isEmpty() ? loc : "Non renseigné"));
                }
            }

            if (tvUser2Profession != null) {
                if (user.getProfession() == null || user.getProfession().trim().isEmpty() || isPrivate) {
                    tvUser2Profession.setVisibility(View.GONE);
                } else {
                    tvUser2Profession.setVisibility(View.VISIBLE);
                    tvUser2Profession.setText("Profession : " + user.getProfession());
                }
            }

            if (tvUser2Dob != null) {
                if (user.isHideDob() || isPrivate || user.getDob() == null || user.getDob().trim().isEmpty()) {
                    tvUser2Dob.setVisibility(View.GONE);
                } else {
                    tvUser2Dob.setVisibility(View.VISIBLE);
                    tvUser2Dob.setText("Né(e) le " + user.getDob());
                }
            }

            if (tvUser2Phone != null) {
                if (user.isHideEmail() || isPrivate || user.getPhone() == null || user.getPhone().trim().isEmpty()) {
                    tvUser2Phone.setVisibility(View.GONE);
                } else {
                    tvUser2Phone.setVisibility(View.VISIBLE);
                    tvUser2Phone.setText("Téléphone : " + user.getPhone());
                }
            }
        }

        setupActionButtons(name, imgPath);
    }

    private String formatLastSeenTime(String lastSeenStr) {
        if (lastSeenStr == null || lastSeenStr.trim().isEmpty()) {
            return "Hors ligne";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(lastSeenStr);
            if (date == null) return "Hors ligne";
            long diff = System.currentTimeMillis() - date.getTime();
            long hours = diff / (3600 * 1000L);
            if (hours < 1) {
                long minutes = diff / (60 * 1000L);
                return "Il y a " + Math.max(1, minutes) + " min";
            } else if (hours < 24) {
                return "Il y a " + hours + " heure" + (hours > 1 ? "s" : "");
            } else {
                long days = hours / 24;
                return "Il y a " + days + " jour" + (days > 1 ? "s" : "");
            }
        } catch (Exception e) {
            return "Hors ligne";
        }
    }

    private void setupActionButtons(String name, String img) {
        if (targetUserId == currentUserId || targetUserId == -1) {
            if (btnUser2AddFriend != null) btnUser2AddFriend.setVisibility(View.GONE);
            if (btnUser2Message != null) btnUser2Message.setVisibility(View.GONE);
            return;
        }

        if (btnUser2AddFriend != null) {
            btnUser2AddFriend.setVisibility(View.VISIBLE);
            FriendRepository.FriendRequestItem incomingReq = friendRepository.getIncomingRequestFromUser(currentUserId, targetUserId);
            boolean isFriendsList = friendRepository.getFriendsList(currentUserId, null)
                    .stream().anyMatch(usr -> usr != null && usr.getId() == targetUserId);

            if (isFriendsList) {
                btnUser2AddFriend.setText("✅ Amis");
                btnUser2AddFriend.setEnabled(false);
            } else if (incomingReq != null) {
                btnUser2AddFriend.setText("Accepter");
                btnUser2AddFriend.setEnabled(true);
                btnUser2AddFriend.setOnClickListener(v -> {
                    friendRepository.acceptFriendRequest(incomingReq.requestId, incomingReq.senderId, incomingReq.receiverId);
                    Toasty.success(this, "Demande acceptée !", Toast.LENGTH_SHORT).show();
                    loadUserInfo(name, img);
                });
            } else {
                btnUser2AddFriend.setText("👤 Ajouter");
                btnUser2AddFriend.setEnabled(true);
                btnUser2AddFriend.setOnClickListener(v -> {
                    boolean sent = friendRepository.sendFriendRequest(currentUserId, targetUserId);
                    if (sent) {
                        Toasty.success(this, "Invitation envoyée !", Toast.LENGTH_SHORT).show();
                        btnUser2AddFriend.setText("Invitation envoyée");
                        btnUser2AddFriend.setEnabled(false);
                    }
                });
            }
        }

        if (btnUser2Message != null) {
            btnUser2Message.setVisibility(View.VISIBLE);
            btnUser2Message.setOnClickListener(v -> {
                int conversationId = conversationRepository.createConversationIfNotExists(currentUserId, targetUserId);
                Intent intent = new Intent(User2PageActivity.this, ChatActivity.class);
                intent.putExtra("conversation_id", conversationId);
                intent.putExtra("receiver_id", targetUserId);
                intent.putExtra("receiver_name", name);
                intent.putExtra("receiver_profile_image", img);
                startActivity(intent);
            });
        }
    }

    private void openUser2Activity() {
        User user = userRepository.getUserById(targetUserId);
        String name = user != null ? user.getName() : getIntent().getStringExtra("user_name");
        String img = user != null ? user.getProfileImage() : getIntent().getStringExtra("user_profile_image");

        Intent intent = new Intent(User2PageActivity.this, User2ProfileActivity.class);
        intent.putExtra("user_id", targetUserId);
        intent.putExtra("user_name", name);
        intent.putExtra("user_profile_image", img);
        startActivity(intent);
    }

    private void loadUserPosts() {
        if (targetUserId != -1) {
            List<Post> posts = postRepository.getPostsByUserId(targetUserId, currentUserId);
            Collections.sort(posts, (p1, p2) -> Boolean.compare(p2.isPinned(), p1.isPinned()));
            userPostsList.clear();
            userPostsList.addAll(posts);
            postAdapter.notifyDataSetChanged();

            // Refresh stats summary for post count
            User user = userRepository.getUserById(targetUserId);
            loadUserInfo(user != null ? user.getName() : null, user != null ? user.getProfileImage() : null);
        }
    }

    private void launchImageCropper() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeGallery = true;
        options.imageSourceIncludeCamera = true;
        options.guidelines = CropImageView.Guidelines.ON;
        options.activityTitle = "Recadrer l'image";
        imagePickerLauncher.launch(new CropImageContractOptions(null, options));
    }

    private void publishPostOnProfile() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty() && savedPostImagePath == null) {
            Toasty.warning(this, "Écrivez un texte ou ajoutez une photo", Toasty.LENGTH_SHORT).show();
            return;
        }

        int targetWallId = (targetUserId != -1 && targetUserId != currentUserId) ? targetUserId : 0;
        boolean success = postRepository.createPost(currentUserId, targetWallId, content, savedPostImagePath, null, false);
        if (success) {
            etPostContent.setText("");
            savedPostImagePath = null;
            layoutSelectedImage.setVisibility(View.GONE);
            Toasty.success(this, "Publication réussie !", Toasty.LENGTH_SHORT).show();
            loadUserPosts();
        } else {
            Toasty.error(this, "Erreur lors de la publication", Toasty.LENGTH_SHORT).show();
        }
    }

    private void showStoryGroupViewerDialog(UserStoryGroup group) {
        final List<Story> stories = group.getStories();
        if (stories == null || stories.isEmpty()) return;
        final int[] currentStoryIndex = {0};
        final int[] storyProgress = {0};

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_story_viewer, null);
        ImageView imgStoryViewerPhoto = dialogView.findViewById(R.id.imgStoryViewerPhoto);
        ImageView imgStoryViewerAvatar = dialogView.findViewById(R.id.imgStoryViewerAvatar);
        TextView tvAuthorName = dialogView.findViewById(R.id.tvStoryViewerAuthorName);
        TextView tvStoryTime = dialogView.findViewById(R.id.tvStoryViewerTime);
        TextView tvStoryViewsCount = dialogView.findViewById(R.id.tvStoryViewsCount);
        TextView tvCaption = dialogView.findViewById(R.id.tvStoryViewerCaption);
        android.widget.ProgressBar progressStoryTimer = dialogView.findViewById(R.id.progressStoryTimer);
        ImageButton ibClose = dialogView.findViewById(R.id.ibCloseStoryViewer);

        activeStoryDialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        final boolean[] isStoryPaused = {false};
        if (imgStoryViewerPhoto != null) {
            imgStoryViewerPhoto.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isStoryPaused[0] = true;
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isStoryPaused[0] = false;
                        v.performClick();
                        return true;
                }
                return false;
            });
        }

        storyTimerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        storyTimerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isStoryPaused[0]) {
                    if (storyTimerHandler != null) storyTimerHandler.postDelayed(this, 100);
                    return;
                }
                storyProgress[0] += 2;
                if (progressStoryTimer != null) {
                    progressStoryTimer.setProgress(storyProgress[0]);
                }
                if (storyProgress[0] >= 100) {
                    if (currentStoryIndex[0] + 1 < stories.size()) {
                        currentStoryIndex[0]++;
                        storyProgress[0] = 0;
                        renderCurrentStory(stories.get(currentStoryIndex[0]), tvAuthorName, tvStoryTime, tvStoryViewsCount, tvCaption, imgStoryViewerPhoto, imgStoryViewerAvatar);
                        if (storyTimerHandler != null) storyTimerHandler.postDelayed(this, 100);
                    } else {
                        DialogUtils.safeDismiss(activeStoryDialog);
                    }
                } else {
                    if (storyTimerHandler != null) storyTimerHandler.postDelayed(this, 100);
                }
            }
        };

        renderCurrentStory(stories.get(0), tvAuthorName, tvStoryTime, tvStoryViewsCount, tvCaption, imgStoryViewerPhoto, imgStoryViewerAvatar);
        storyTimerHandler.postDelayed(storyTimerRunnable, 100);

        activeStoryDialog.setOnDismissListener(d -> {
            if (storyTimerHandler != null && storyTimerRunnable != null) {
                storyTimerHandler.removeCallbacks(storyTimerRunnable);
            }
            activeStoryDialog = null;
        });

        if (ibClose != null) {
            ibClose.bringToFront();
            ibClose.setOnClickListener(v -> DialogUtils.safeDismiss(activeStoryDialog));
        }

        activeStoryDialog.show();
    }

    private void renderCurrentStory(Story story, TextView tvName, TextView tvTime, TextView tvViews, TextView tvCap, ImageView imgPhoto, ImageView imgAvatar) {
        if (story == null) return;
        if (tvName != null) tvName.setText(story.getUserName() != null ? story.getUserName() : "Utilisateur");
        if (tvTime != null) tvTime.setText(td.teladoum.baobab.utils.TimeUtils.getRemainingTimeText(story.getCreatedAt()));
        if (tvCap != null) {
            if (story.getCaption() != null && !story.getCaption().trim().isEmpty()) {
                tvCap.setVisibility(View.VISIBLE);
                tvCap.setText(story.getCaption());
            } else {
                tvCap.setVisibility(View.GONE);
            }
        }
        if (imgPhoto != null && story.getImagePath() != null) {
            ImageUtils.loadStoryImage(this, story.getImagePath(), imgPhoto);
        }
        if (imgAvatar != null && story.getUserAvatar() != null) {
            ImageUtils.loadProfileImage(this, story.getUserAvatar(), imgAvatar);
        }
        storyRepository.recordStoryView(story.getId(), currentUserId);
        if (tvViews != null) {
            tvViews.setText("👁 " + storyRepository.getStoryViewsCount(story.getId()));
        }
    }

    private void showImagePreviewDialog(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_preview, null);
        ImageView imgEnlarged = dialogView.findViewById(R.id.imgEnlarged);
        ImageButton ibClose = dialogView.findViewById(R.id.ibClosePreview);
        View layoutPreviewBottom = dialogView.findViewById(R.id.layoutPreviewBottom);
        if (layoutPreviewBottom != null) {
            layoutPreviewBottom.setVisibility(View.GONE);
        }

        ImageUtils.loadFullImage(this, imagePath, imgEnlarged);
        if (imgEnlarged != null) {
            ImageUtils.enablePinchToZoom(imgEnlarged);
        }

        activeImageDialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (ibClose != null) {
            ibClose.bringToFront();
            ibClose.setOnClickListener(v -> DialogUtils.safeDismiss(activeImageDialog));
        }

        activeImageDialog.setOnDismissListener(d -> activeImageDialog = null);
        activeImageDialog.show();
    }

    private void showImagePreviewDialog(Post post) {
        if (post == null || post.getImagePath() == null || post.getImagePath().trim().isEmpty())
            return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_preview, null);
        ImageView imgEnlarged = dialogView.findViewById(R.id.imgEnlarged);
        ImageButton ibClose = dialogView.findViewById(R.id.ibClosePreview);
        View layoutPreviewBottom = dialogView.findViewById(R.id.layoutPreviewBottom);
        TextView tvPreviewAuthorName = dialogView.findViewById(R.id.tvPreviewAuthorName);
        TextView tvPreviewStats = dialogView.findViewById(R.id.tvPreviewStats);
        TextView tvPreviewReaction = dialogView.findViewById(R.id.tvPreviewReaction);

        ImageUtils.loadFullImage(this, post.getImagePath(), imgEnlarged);
        if (imgEnlarged != null) {
            ImageUtils.enablePinchToZoom(imgEnlarged);
        }

        if (layoutPreviewBottom != null) {
            layoutPreviewBottom.setVisibility(View.VISIBLE);

            if (tvPreviewAuthorName != null) {
                if (post.getAuthorName() != null && !post.getAuthorName().isEmpty()) {
                    tvPreviewAuthorName.setVisibility(View.VISIBLE);
                    tvPreviewAuthorName.setText(post.getAuthorName());
                } else {
                    tvPreviewAuthorName.setVisibility(View.GONE);
                }
            }

            if (tvPreviewStats != null) {
                String stats = "👍 " + post.getLikesCount() + "    💬 " + post.getCommentsCount() + "    ↗ " + post.getSharesCount();
                tvPreviewStats.setText(stats);
            }

            if (tvPreviewReaction != null) {
                String reaction = post.getUserReactionType();
                String emoji = null;
                int tintColor = android.graphics.Color.parseColor("#2196F3");

                if ("LOVE".equalsIgnoreCase(reaction)) {
                    emoji = "❤️";
                    tintColor = android.graphics.Color.RED;
                } else if ("HAHA".equalsIgnoreCase(reaction)) {
                    emoji = "😆";
                    tintColor = android.graphics.Color.parseColor("#FF9800");
                } else if ("WOW".equalsIgnoreCase(reaction)) {
                    emoji = "😮";
                    tintColor = android.graphics.Color.parseColor("#2196F3");
                } else if ("SAD".equalsIgnoreCase(reaction)) {
                    emoji = "😢";
                    tintColor = android.graphics.Color.parseColor("#9C27B0");
                } else if ("ANGRY".equalsIgnoreCase(reaction)) {
                    emoji = "😡";
                    tintColor = android.graphics.Color.RED;
                } else if ("LIKE".equalsIgnoreCase(reaction) || post.isLikedByCurrentUser()) {
                    emoji = "👍";
                    tintColor = android.graphics.Color.parseColor("#2196F3");
                }

                if (emoji != null) {
                    tvPreviewReaction.setVisibility(View.VISIBLE);
                    tvPreviewReaction.setText(emoji);
                    tvPreviewReaction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(tintColor));
                } else {
                    tvPreviewReaction.setVisibility(View.GONE);
                }
            }
        }

        activeImageDialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (ibClose != null) {
            ibClose.bringToFront();
            ibClose.setOnClickListener(v -> DialogUtils.safeDismiss(activeImageDialog));
        }

        activeImageDialog.setOnDismissListener(d -> activeImageDialog = null);
        activeImageDialog.show();
    }

    private void showVideoPlayerDialog(String videoPath) {
        if (videoPath == null || videoPath.trim().isEmpty()) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_video_player, null);
        android.widget.VideoView videoViewPlayer = dialogView.findViewById(R.id.videoViewPlayer);
        ImageButton ibClose = dialogView.findViewById(R.id.ibCloseVideoPlayer);

        android.widget.MediaController mediaController = new android.widget.MediaController(this);
        mediaController.setAnchorView(videoViewPlayer);
        videoViewPlayer.setMediaController(mediaController);
        videoViewPlayer.setVideoPath(videoPath);

        activeVideoDialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        if (ibClose != null) {
            ibClose.bringToFront();
            ibClose.setOnClickListener(v -> {
                videoViewPlayer.stopPlayback();
                DialogUtils.safeDismiss(activeVideoDialog);
            });
        }

        activeVideoDialog.setOnDismissListener(d -> {
            videoViewPlayer.stopPlayback();
            activeVideoDialog = null;
        });

        activeVideoDialog.show();
        videoViewPlayer.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
