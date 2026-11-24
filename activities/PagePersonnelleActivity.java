package td.teladoum.baobab.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.PostAdapter;
import td.teladoum.baobab.adapters.UserPhotosAdapter;
import td.teladoum.baobab.helpers.DialogUtils;
import td.teladoum.baobab.models.Post;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.FriendRepository;
import td.teladoum.baobab.repositories.PostRepository;
import td.teladoum.baobab.repositories.StoryRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * PagePersonnelleActivity : Page personnelle réservée à l'utilisateur connecté.
 * Fait office de récapitulatif complet (style profil Facebook) de toutes ses publications,
 * statistiques, informations de profil, tableau de bord et galerie photos.
 */
public class PagePersonnelleActivity extends AppCompatActivity {

    private ImageView imgPersonalCover, imgPersonalAvatar, imgPersonalPostAvatar, imgPersonalPostImageIcon;
    private TextView tvProfileUserName, tvPersonalBadge, tvPersonalBio, tvPersonalStatsSummary;
    private TextView tvPersonalLocation, tvPersonalProfession, tvPersonalEmail, tvPersonalPhone;
    private TextView tvPersonalQuoiDeNeuf;
    private TextView tabAll, tabReels, tabPhotos, tabEvents;
    private RecyclerView rvPersonalPosts;
    private NestedScrollView scrollPagePersonnelle;

    private UserRepository userRepository;
    private PostRepository postRepository;
    private FriendRepository friendRepository;
    private SessionManager sessionManager;

    private PostAdapter postAdapter;
    private final List<Post> personalPostsList = new ArrayList<>();
    private int currentUserId;
    private int friendsCountGlobal = 0;
    private int postsCountGlobal = 0;

    private Uri pendingSelectedImageUri;
    private AlertDialog activeCreatePostDialog;
    private AlertDialog activeImageDialog;
    private ImageView imgCreatePostPreviewDialog;
    private FrameLayout framePostImagePreviewDialog;

    private final ActivityResultLauncher<CropImageContractOptions> postImagePickerLauncher =
            registerForActivityResult(new CropImageContract(), (CropImageView.CropResult result) -> {
                if (result.isSuccessful()) {
                    Uri uri = result.getUriContent();
                    if (uri != null) {
                        pendingSelectedImageUri = uri;
                        if (activeCreatePostDialog != null && activeCreatePostDialog.isShowing()) {
                            if (imgCreatePostPreviewDialog != null && framePostImagePreviewDialog != null) {
                                imgCreatePostPreviewDialog.setImageURI(uri);
                                framePostImagePreviewDialog.setVisibility(View.VISIBLE);
                            }
                        } else {
                            showCreatePostDialog(uri);
                        }
                    }
                } else if (result.getError() != null) {
                    Toasty.error(this, getString(R.string.toast_crop_error), Toasty.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<CropImageContractOptions> coverImagePickerLauncher =
            registerForActivityResult(new CropImageContract(), (CropImageView.CropResult result) -> {
                if (result.isSuccessful()) {
                    Uri uri = result.getUriContent();
                    if (uri != null) {
                        String savedPath = ImageUtils.saveImageToInternalStorage(this, uri);
                        if (savedPath != null) {
                            userRepository.updateUserCoverImage(currentUserId, savedPath);
                            Toasty.success(this, "Photo de couverture mise à jour!", Toasty.LENGTH_SHORT).show();
                            loadUserDataAndPosts();
                        }
                    }
                } else if (result.getError() != null) {
                    Toasty.error(this, getString(R.string.toast_crop_error), Toasty.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<CropImageContractOptions> profileImagePickerLauncher =
            registerForActivityResult(new CropImageContract(), (CropImageView.CropResult result) -> {
                if (result.isSuccessful()) {
                    Uri uri = result.getUriContent();
                    if (uri != null) {
                        String savedPath = ImageUtils.saveImageToInternalStorage(this, uri);
                        if (savedPath != null) {
                            userRepository.updateUserProfileImage(currentUserId, savedPath);
                            Toasty.success(this, "Photo de profil mise à jour", Toasty.LENGTH_SHORT).show();
                            loadUserDataAndPosts();
                        }
                    }
                } else if (result.getError() != null) {
                    Toasty.error(this, getString(R.string.toast_crop_error), Toasty.LENGTH_SHORT).show();
                }
            });

    private void launchPostImageCropper() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeGallery = true;
        options.imageSourceIncludeCamera = true;
        options.guidelines = CropImageView.Guidelines.ON;
        options.activityTitle = "Recadrer l'image";
        options.cropMenuCropButtonTitle = "Valider";
        postImagePickerLauncher.launch(new CropImageContractOptions(null, options));
    }

    private void launchProfileImageCropper() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeGallery = true;
        options.imageSourceIncludeCamera = true;
        options.guidelines = CropImageView.Guidelines.ON;
        options.activityTitle = "Recadrer la photo";
        options.cropMenuCropButtonTitle = "Valider";
        profileImagePickerLauncher.launch(new CropImageContractOptions(null, options));
    }

    private void launchCoverImageCropper() {
        CropImageOptions options = new CropImageOptions();
        options.imageSourceIncludeGallery = true;
        options.imageSourceIncludeCamera = true;
        options.guidelines = CropImageView.Guidelines.ON;
        options.activityTitle = "Recadrer la photo";
        options.cropMenuCropButtonTitle = "Valider";
        coverImagePickerLauncher.launch(new CropImageContractOptions(null, options));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_personnelle);

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

        Toolbar toolbar = findViewById(R.id.toolbarPagePersonnelle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ma Page Personnelle");
        }

        userRepository = new UserRepository(this);
        postRepository = new PostRepository(this);
        friendRepository = new FriendRepository(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        initViews();
        loadUserDataAndPosts();
    }

    private void initViews() {
        imgPersonalCover = findViewById(R.id.imgPersonalCover);
        imgPersonalAvatar = findViewById(R.id.imgPersonalAvatar);
        tvProfileUserName = findViewById(R.id.tvProfileUserName);
        if (tvProfileUserName != null) {
            tvProfileUserName.setOnClickListener(v -> {
                User user = userRepository != null ? userRepository.getUserById(currentUserId) : null;
                String name = user != null && user.getName() != null ? user.getName() : sessionManager.getName();
                String img = user != null ? user.getProfileImage() : null;

                Intent intent = new Intent(PagePersonnelleActivity.this, User2ProfileActivity.class);
                intent.putExtra("user_id", currentUserId);
                intent.putExtra("user_name", name);
                intent.putExtra("user_profile_image", img);
                startActivity(intent);
            });
        }
        tvPersonalBio = findViewById(R.id.tvPersonalBio);
        tvPersonalBadge = findViewById(R.id.tvPersonalBadge);
        tvPersonalStatsSummary = findViewById(R.id.tvPersonalStatsSummary);
        tvPersonalLocation = findViewById(R.id.tvPersonalLocation);
        tvPersonalProfession = findViewById(R.id.tvPersonalProfession);
        tvPersonalEmail = findViewById(R.id.tvPersonalEmail);
        tvPersonalPhone = findViewById(R.id.tvPersonalPhone);
        scrollPagePersonnelle = findViewById(R.id.scrollPagePersonnelle);

        imgPersonalPostAvatar = findViewById(R.id.imgPersonalPostAvatar);
        tvPersonalQuoiDeNeuf = findViewById(R.id.tvPersonalQuoiDeNeuf);
        imgPersonalPostImageIcon = findViewById(R.id.imgPersonalPostImageIcon);

        tabAll = findViewById(R.id.tabAll);
        tabReels = findViewById(R.id.tabReels);
        tabPhotos = findViewById(R.id.tabPhotos);
        tabEvents = findViewById(R.id.tabEvents);

        ImageButton btnChangeCover = findViewById(R.id.btnChangeCover);
        ImageButton btnChangeAvatar = findViewById(R.id.btnChangeAvatar);

        Button btnPersonalEdit = findViewById(R.id.btnPersonalEdit);
        Button btnPersonalDashboard = findViewById(R.id.btnPersonalDashboard);
        Button btnPersonalAddStory = findViewById(R.id.btnPersonalAddStory);
        rvPersonalPosts = findViewById(R.id.rvPersonalPosts);

        rvPersonalPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPersonalPosts.setHasFixedSize(true);
        postAdapter = new PostAdapter(personalPostsList, currentUserId, new PostAdapter.OnPostClickListener() {
            @Override
            public void onLikeClick(Post post) {
                postRepository.toggleLikePost(post.getId(), currentUserId);
                loadUserDataAndPosts();
            }

            @Override
            public void onReactionClick(Post post, String reactionType) {
                postRepository.setPostReaction(post.getId(), currentUserId, reactionType);
                loadUserDataAndPosts();
            }

            @Override
            public void onCommentClick(Post post) {
                Intent intent = new Intent(PagePersonnelleActivity.this, CommenterActivity.class);
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
                loadUserDataAndPosts();
            }

            @Override
            public void onReportClick(Post post) {
                final EditText etReason = new EditText(PagePersonnelleActivity.this);
                etReason.setHint("Raison du signalement");
                new AlertDialog.Builder(PagePersonnelleActivity.this)
                        .setTitle("Signaler la publication")
                        .setView(etReason)
                        .setPositiveButton("Signaler", (dialog, which) -> {
                            String reason = etReason.getText().toString().trim();
                            if (postRepository.reportPost(post.getId(), currentUserId, reason)) {
                                Toasty.success(PagePersonnelleActivity.this, "Publication signalée", Toasty.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }

            @Override
            public void onEditClick(Post post) {
                showEditPostDialog(post);
            }

            @Override
            public void onDeleteClick(Post post) {
                showDeletePostDialog(post);
            }

            @Override
            public void onImageClick(String imagePath) {
                showImagePreviewDialog(imagePath);
            }

            @Override
            public void onImageClick(Post post) {
                showImagePreviewDialog(post != null ? post.getImagePath() : null);
            }

            @Override
            public void onVideoClick(String videoPath) {
                Toasty.info(PagePersonnelleActivity.this, "Vidéo : " + videoPath, Toasty.LENGTH_SHORT).show();
            }
        });
        rvPersonalPosts.setAdapter(postAdapter);

        if (btnChangeCover != null) {
            btnChangeCover.setOnClickListener(v -> launchCoverImageCropper());
        }
        if (btnChangeAvatar != null) {
            btnChangeAvatar.setOnClickListener(v -> launchProfileImageCropper());
        }

        btnPersonalEdit.setOnClickListener(v -> startActivity(new Intent(PagePersonnelleActivity.this, Mise_a_jour_Activity.class)));
        btnPersonalDashboard.setOnClickListener(v -> showDashboardDialog());
        btnPersonalAddStory.setOnClickListener(v -> {
            Toasty.info(PagePersonnelleActivity.this, "Rendez-vous sur l'accueil pour ajouter une story", Toasty.LENGTH_SHORT).show();
            startActivity(new Intent(PagePersonnelleActivity.this, AcceuilActivity.class));
        });



        // Profile Avatar & Cover Image clicks
        imgPersonalAvatar.setOnClickListener(v -> showProfilePhotoOptions());
        imgPersonalCover.setOnClickListener(v -> showCoverPhotoOptions());

        // Stats summary click (Redirection vers AmiActivity ou défilement publications)
        tvPersonalStatsSummary.setOnClickListener(v -> {
            String[] options = new String[]{"Voir mes ami(e)s (" + friendsCountGlobal + ")", "Voir mes publications (" + postsCountGlobal + ")"};
            new AlertDialog.Builder(this)
                    .setTitle("Mes Statistiques")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            startActivity(new Intent(PagePersonnelleActivity.this, AmiActivity.class));
                        } else if (which == 1) {
                            if (scrollPagePersonnelle != null && rvPersonalPosts != null) {
                                scrollPagePersonnelle.post(() -> scrollPagePersonnelle.smoothScrollTo(0, rvPersonalPosts.getTop()));
                            }
                        }
                    })
                    .show();
        });

        // Quoi de neuf listeners
        if (tvPersonalQuoiDeNeuf != null) {
            tvPersonalQuoiDeNeuf.setOnClickListener(v -> showCreatePostDialog(null));
        }
        if (imgPersonalPostAvatar != null) {
            imgPersonalPostAvatar.setOnClickListener(v -> showCreatePostDialog(null));
        }
        if (imgPersonalPostImageIcon != null) {
            imgPersonalPostImageIcon.setOnClickListener(v -> launchPostImageCropper());
        }

        // Tabs listeners
        if (tabPhotos != null) {
            tabPhotos.setOnClickListener(v -> showUserPhotosDialog());
        }
        if (tabReels != null) {
            tabReels.setOnClickListener(v -> Toasty.info(PagePersonnelleActivity.this, "Reels de vos vidéos", Toasty.LENGTH_SHORT).show());
        }
        if (tabEvents != null) {
            tabEvents.setOnClickListener(v -> Toasty.info(PagePersonnelleActivity.this, "Évènements à venir", Toasty.LENGTH_SHORT).show());
        }
        if (tabAll != null) {
            tabAll.setOnClickListener(v -> loadUserDataAndPosts());
        }
    }

    private void loadUserDataAndPosts() {
        User user = userRepository.getUserById(currentUserId);
        if (user != null) {
            String lName = user.getLastName();
            String fName = user.getFirstName();
            if (lName == null || lName.equalsIgnoreCase("null")) lName = "";
            if (fName == null || fName.equalsIgnoreCase("null")) fName = "";

            String displayName = (lName + " " + fName).trim();
            if (displayName.isEmpty() || displayName.equalsIgnoreCase("null null") || displayName.equalsIgnoreCase("null")) {
                displayName = (user.getName() != null && !user.getName().equalsIgnoreCase("null") && !user.getName().equalsIgnoreCase("null null"))
                        ? user.getName()
                        : (sessionManager.getName() != null ? sessionManager.getName() : "Utilisateur");
            }
            tvProfileUserName.setText(displayName);
            if (tvPersonalBadge != null) {
                tvPersonalBadge.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
            }

            String bio = user.getBio() != null && !user.getBio().trim().isEmpty() ? user.getBio() : "Membre actif BaobabTD";
            tvPersonalBio.setText(bio);

            String city = user.getCity() != null && !user.getCity().trim().isEmpty() ? user.getCity() : "N'Djamena";
            tvPersonalLocation.setText("Habite à " + city);

            String prof = user.getProfession() != null && !user.getProfession().trim().isEmpty() ? user.getProfession() : "Non renseignée";
            tvPersonalProfession.setText("Profession : " + prof);

            String email = user.getEmail() != null && !user.getEmail().trim().isEmpty() ? user.getEmail() : "Non renseigné";
            tvPersonalEmail.setText("Email : " + email);

            String phone = user.getPhone() != null && !user.getPhone().trim().isEmpty() ? user.getPhone() : "Non renseigné";
            tvPersonalPhone.setText("Téléphone : " + phone);

            // Profile photo
            ImageUtils.loadProfileImage(this, user.getProfileImage(), imgPersonalAvatar);

            // Profile photo on Quoi de neuf box
            if (imgPersonalPostAvatar != null) {
                ImageUtils.loadProfileImage(this, user.getProfileImage(), imgPersonalPostAvatar);
            }

            if (user.getCoverImage() != null && !user.getCoverImage().trim().isEmpty()) {
                ImageUtils.loadFullImage(this, user.getCoverImage(), imgPersonalCover);
            } else {
                imgPersonalCover.setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"));
            }
        } else {
            tvProfileUserName.setText(sessionManager.getName() != null ? sessionManager.getName() : "Utilisateur");
            if (tvPersonalBadge != null) {
                tvPersonalBadge.setVisibility(View.GONE);
            }
            tvPersonalEmail.setText("Email : " + sessionManager.getEmail());
            tvPersonalPhone.setText("Téléphone : Non renseigné");
            tvPersonalLocation.setText("Habite à N'Djamena");
            tvPersonalProfession.setText("Profession : Non renseignée");
        }

        StoryRepository storyRepo = new StoryRepository(this);
        List<td.teladoum.baobab.models.Story> recentStories = storyRepo.getRecentStories();
        boolean activeStoryFound = false;
        for (td.teladoum.baobab.models.Story s : recentStories) {
            if (s.getUserId() == currentUserId) {
                activeStoryFound = true;
                break;
            }
        }
        if (activeStoryFound) {
            imgPersonalAvatar.setBackgroundResource(R.drawable.story_border_green);
            imgPersonalAvatar.setPadding(6, 6, 6, 6);
        } else {
            imgPersonalAvatar.setBackgroundResource(android.R.color.white);
            imgPersonalAvatar.setPadding(0, 0, 0, 0);
        }

        List<Post> posts = postRepository.getPostsByUserId(currentUserId, currentUserId);
        personalPostsList.clear();
        personalPostsList.addAll(posts);
        postAdapter.notifyDataSetChanged();

        friendsCountGlobal = friendRepository.getFriendsList(currentUserId, null).size();
        postsCountGlobal = posts.size();
        tvPersonalStatsSummary.setText( friendsCountGlobal + " ami(e)s • " + postsCountGlobal + " publications");

        PostRepository.loadAllPostsFromCloud(this, () -> {
            if (isFinishing() || isDestroyed()) return;
            List<Post> cloudPosts = postRepository.getPostsByUserId(currentUserId, currentUserId);
            personalPostsList.clear();
            personalPostsList.addAll(cloudPosts);
            postAdapter.notifyDataSetChanged();
            friendsCountGlobal = friendRepository.getFriendsList(currentUserId, null).size();
            postsCountGlobal = cloudPosts.size();
            tvPersonalStatsSummary.setText(friendsCountGlobal + " ami(e)s • " + postsCountGlobal + " publications");
        });
    }

    private void showCreatePostDialog(Uri initialImageUri) {
        pendingSelectedImageUri = initialImageUri;
        User user = userRepository.getUserById(currentUserId);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null);
        ImageView imgCreatePostAvatar = dialogView.findViewById(R.id.imgCreatePostAvatar);
        TextView tvCreatePostAuthorName = dialogView.findViewById(R.id.tvCreatePostAuthorName);
        EditText etCreatePostContent = dialogView.findViewById(R.id.etCreatePostContent);

        imgCreatePostPreviewDialog = dialogView.findViewById(R.id.imgCreatePostPreview);
        framePostImagePreviewDialog = dialogView.findViewById(R.id.framePostImagePreview);

        ImageButton ibCloseCreatePost = dialogView.findViewById(R.id.ibCloseCreatePost);
        ImageButton btnRemovePostImage = dialogView.findViewById(R.id.btnRemovePostImage);
        Button btnAddImageToPost = dialogView.findViewById(R.id.btnAddImageToPost);
        Button btnSubmitPost = dialogView.findViewById(R.id.btnSubmitPost);

        if (user != null) {
            ImageUtils.loadProfileImage(this, user.getProfileImage(), imgCreatePostAvatar);
            tvCreatePostAuthorName.setText(user.getName());
        } else {
            tvCreatePostAuthorName.setText(sessionManager.getName());
        }

        if (pendingSelectedImageUri != null) {
            imgCreatePostPreviewDialog.setImageURI(pendingSelectedImageUri);
            framePostImagePreviewDialog.setVisibility(View.VISIBLE);
        } else {
            framePostImagePreviewDialog.setVisibility(View.GONE);
        }

        btnAddImageToPost.setOnClickListener(v -> launchPostImageCropper());

        btnRemovePostImage.setOnClickListener(v -> {
            pendingSelectedImageUri = null;
            framePostImagePreviewDialog.setVisibility(View.GONE);
        });

        activeCreatePostDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        ibCloseCreatePost.setOnClickListener(v -> {
            DialogUtils.safeDismiss(activeCreatePostDialog);
            activeCreatePostDialog = null;
        });

        btnSubmitPost.setOnClickListener(v -> {
            String content = etCreatePostContent.getText().toString().trim();
            if (content.isEmpty() && pendingSelectedImageUri == null) {
                Toasty.warning(this, "Veuillez écrire un texte ou ajouter une image.", Toasty.LENGTH_SHORT).show();
                return;
            }

            String savedImagePath = null;
            if (pendingSelectedImageUri != null) {
                String base64 = ImageUtils.uriToBase64(this, pendingSelectedImageUri);
                if (base64 != null && !base64.trim().isEmpty()) {
                    savedImagePath = base64;
                } else {
                    savedImagePath = ImageUtils.saveImageToInternalStorage(this, pendingSelectedImageUri);
                }
            }

            boolean success = postRepository.createPost(currentUserId, content, savedImagePath);
            if (success) {
                Toasty.success(this, "Publication ajoutée avec succès!", Toasty.LENGTH_SHORT).show();
                DialogUtils.safeDismiss(activeCreatePostDialog);
                activeCreatePostDialog = null;
                pendingSelectedImageUri = null;
                loadUserDataAndPosts();
            } else {
                Toasty.error(this, "Impossible d'ajouter la publication.", Toasty.LENGTH_SHORT).show();
            }
        });

        activeCreatePostDialog.show();
    }

    private void showUserPhotosDialog() {
        User user = userRepository.getUserById(currentUserId);
        List<UserPhotosAdapter.PhotoItem> photoItems = new ArrayList<>();

        if (user != null) {
            if (user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
                photoItems.add(new UserPhotosAdapter.PhotoItem(user.getProfileImage(), "Photo de profil"));
            }
            if (user.getCoverImage() != null && !user.getCoverImage().trim().isEmpty()) {
                photoItems.add(new UserPhotosAdapter.PhotoItem(user.getCoverImage(), "Photo de couverture"));
            }
        }

        for (Post post : personalPostsList) {
            if (post.getImagePath() != null && !post.getImagePath().trim().isEmpty()) {
                photoItems.add(new UserPhotosAdapter.PhotoItem(post.getImagePath(), "Publication (" + post.getCreatedAt() + ")"));
            }
        }

        if (photoItems.isEmpty()) {
            Toasty.info(this, "Vous n'avez pas encore de photos.", Toasty.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_photos, null);
        RecyclerView rvUserPhotos = dialogView.findViewById(R.id.rvUserPhotos);
        ImageButton ibCloseUserPhotos = dialogView.findViewById(R.id.ibCloseUserPhotos);

        rvUserPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        UserPhotosAdapter adapter = new UserPhotosAdapter(photoItems, item -> showImagePreviewDialog(item.getImagePath()));
        rvUserPhotos.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        ibCloseUserPhotos.setOnClickListener(v -> DialogUtils.safeDismiss(dialog));
        dialog.show();
    }

    private void showProfilePhotoOptions() {
        User user = userRepository.getUserById(currentUserId);
        String[] options = {"Voir la photo de profil", "Changer la photo de profil"};
        new AlertDialog.Builder(this)
                .setTitle("Photo de profil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (user != null && user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
                            showImagePreviewDialog(user.getProfileImage());
                        } else {
                            Toasty.info(this, "Aucune photo de profil définie.", Toasty.LENGTH_SHORT).show();
                        }
                    } else if (which == 1) {
                        launchProfileImageCropper();
                    }
                })
                .show();
    }

    private void showCoverPhotoOptions() {
        User user = userRepository.getUserById(currentUserId);
        String[] options = {"Voir la photo de couverture", "Changer la photo de couverture"};
        new AlertDialog.Builder(this)
                .setTitle("Photo de couverture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (user != null && user.getCoverImage() != null && !user.getCoverImage().trim().isEmpty()) {
                            showImagePreviewDialog(user.getCoverImage());
                        } else {
                            Toasty.info(this, "Aucune photo de couverture définie.", Toasty.LENGTH_SHORT).show();
                        }
                    } else if (which == 1) {
                        launchCoverImageCropper();
                    }
                })
                .show();
    }

    private void showImagePreviewDialog(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            Toasty.warning(this, "Aucune photo disponible", Toasty.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_preview, null);
        ImageView imgEnlarged = dialogView.findViewById(R.id.imgEnlarged);
        ImageButton ibClose = dialogView.findViewById(R.id.ibClosePreview);
        ImageButton ibDownload = dialogView.findViewById(R.id.ibDownloadPreview);
        ImageButton ibShare = dialogView.findViewById(R.id.ibSharePreview);

        ImageUtils.loadFullImage(this, imagePath, imgEnlarged);

        ibDownload.setOnClickListener(v -> ImageUtils.downloadImageToGallery(this, imagePath));
        ibShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Photo BaobabTD");
            startActivity(Intent.createChooser(shareIntent, "Partager la photo via"));
        });

        activeImageDialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create();

        ibClose.setOnClickListener(v -> DialogUtils.safeDismiss(activeImageDialog));
        activeImageDialog.show();
    }

    private void showEditPostDialog(Post post) {
        if (post == null) return;
        final EditText etEditContent = new EditText(this);
        etEditContent.setText(post.getContent());
        etEditContent.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("Modifier la publication")
                .setView(etEditContent)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newText = etEditContent.getText().toString().trim();
                    boolean updated = postRepository.updatePost(post.getId(), currentUserId, newText, post.getImagePath());
                    if (updated) {
                        Toasty.success(this, "Publication modifiée avec succès", Toasty.LENGTH_SHORT).show();
                        loadUserDataAndPosts();
                    } else {
                        Toasty.error(this, "Erreur lors de la modification", Toasty.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showDeletePostDialog(Post post) {
        if (post == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la publication")
                .setMessage("Voulez-vous vraiment supprimer cette publication ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    boolean deleted = postRepository.deletePost(post.getId(), currentUserId);
                    if (deleted) {
                        Toasty.success(this, "Publication supprimée", Toasty.LENGTH_SHORT).show();
                        loadUserDataAndPosts();
                    } else {
                        Toasty.error(this, "Erreur lors de la suppression", Toasty.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showDashboardDialog() {
        int postsCount = personalPostsList.size();
        int friendsCount = friendRepository.getFriendsList(currentUserId, null).size();
        int blockedCount = userRepository.getBlockedCount(currentUserId);

        new AlertDialog.Builder(this)
                .setTitle("📊 Tableau de bord personnel")
                .setMessage("Récapitulatif de votre activité :\n\n" +
                        "• Publications : " + postsCount + "\n" +
                        "• Ami(e)s : " + friendsCount + "\n" +
                        "• Comptes bloqué(e)s : " + blockedCount + "\n\n" +
                        "Votre page personnelle résume l'ensemble de vos publications et de vos interactions.")
                .setPositiveButton("Fermer", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDataAndPosts();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
