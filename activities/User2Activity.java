package td.teladoum.baobab.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.ConversationRepository;
import td.teladoum.baobab.repositories.FriendRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité d'affichage détaillé du profil utilisateur (User2Activity).
 * Affiche l'ensemble des informations de profil (bio, coordonnées, adresse, statut)
 * et permet les interactions : Ajout d'ami, Démarrer un chat, Bloquer/Débloquer.
 */
public class User2Activity extends AppCompatActivity {

    private Toolbar toolbarProfileUser2;
    private ImageView imgProfileAvatar;
    private TextView tvDialogName, statusPersonProfile, tvDialogDob, genrePersonProfile,
            professionPersonProfile, matrimonialPersonProfile, tvDialogEmail, phonePersonProfile,
            tvDialogNeighborhood, tvDialogCity, tvDialogCountry;
    private Button btnAddFriend, btnDialogChat, btnBlockUser;

    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private ConversationRepository conversationRepository;
    private SessionManager sessionManager;

    private int targetUserId = -1;
    private int currentUserId = -1;
    private String targetUserName = "";
    private String targetUserProfileImage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user2);

        userRepository = new UserRepository(this);
        friendRepository = new FriendRepository(this);
        conversationRepository = new ConversationRepository(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        targetUserId = getIntent().getIntExtra("user_id", -1);
        targetUserName = getIntent().getStringExtra("user_name");
        targetUserProfileImage = getIntent().getStringExtra("user_profile_image");

        initViews();
        setupToolbar();

        loadUserData();
        if (conversationRepository != null && currentUserId > 0) {
            conversationRepository.syncConversationsFromCloud(currentUserId, this::loadUserData);
        }

        User u = userRepository.getUserById(targetUserId);
        String targetEmail = u != null ? u.getEmail() : null;
        userRepository.fetchAndSyncUserFromFirestore(
                targetEmail,
                null,
                this::loadUserData,
                null
        );
    }

    private void initViews() {
        toolbarProfileUser2 = findViewById(R.id.toolbarProfileUser2);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvDialogName = findViewById(R.id.tvDialogName);
        statusPersonProfile = findViewById(R.id.status_person_profile);
        tvDialogDob = findViewById(R.id.tvDialogDob);
        genrePersonProfile = findViewById(R.id.genre_person_profile);
        professionPersonProfile = findViewById(R.id.profession_person_profile);
        matrimonialPersonProfile = findViewById(R.id.matrimonial_person_profile);
        tvDialogEmail = findViewById(R.id.tvDialogEmail);
        phonePersonProfile = findViewById(R.id.phone_person_profile);
        tvDialogNeighborhood = findViewById(R.id.tvDialogNeighborhood);
        tvDialogCity = findViewById(R.id.tvDialogCity);
        tvDialogCountry = findViewById(R.id.tvDialogCountry);

        btnAddFriend = findViewById(R.id.ajouetr_person_profil);
        btnDialogChat = findViewById(R.id.btnDialogChat);
        btnBlockUser = findViewById(R.id.btnBlockUser);
    }

    private void setupToolbar() {
        if (toolbarProfileUser2 != null) {
            setSupportActionBar(toolbarProfileUser2);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Profil Utilisateur");
            }
            toolbarProfileUser2.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadUserData() {
        User user = userRepository.getUserById(targetUserId);

        String name = user != null && user.getName() != null ? user.getName() : targetUserName;
        if (user != null && user.isVerified()) {
            name = name + " 🔵";
        }
        String img = user != null && user.getProfileImage() != null ? user.getProfileImage() : targetUserProfileImage;

        if (tvDialogName != null) tvDialogName.setText(name != null && !name.trim().isEmpty() ? name : "Utilisateur");
        if (imgProfileAvatar != null) ImageUtils.loadProfileImage(this, img, imgProfileAvatar);

        boolean isSelf = (targetUserId == currentUserId);

        if (user == null) {
            if (tvDialogEmail != null) tvDialogEmail.setText("Non disponible");
            if (tvDialogDob != null) tvDialogDob.setText("Non disponible");
            if (genrePersonProfile != null) genrePersonProfile.setText("Non disponible");
            if (professionPersonProfile != null) professionPersonProfile.setText("Non disponible");
            if (matrimonialPersonProfile != null) matrimonialPersonProfile.setText("Non disponible");
            if (phonePersonProfile != null) phonePersonProfile.setText("Non disponible");
            if (tvDialogNeighborhood != null) tvDialogNeighborhood.setText("Non disponible");
            if (tvDialogCity != null) tvDialogCity.setText("Non disponible");
            if (tvDialogCountry != null) tvDialogCountry.setText("Non disponible");
        } else {
            if (statusPersonProfile != null) statusPersonProfile.setText("Membre BaobabTD");

            // DOB
            if (tvDialogDob != null) {
                if ((user.isHideDob() || user.isPrivateProfile()) && !isSelf) {
                    tvDialogDob.setText("********");
                } else {
                    tvDialogDob.setText(user.getDob() != null && !user.getDob().isEmpty() ? user.getDob() : "Non renseignée");
                }
            }

            // Gender / Sexe
            if (genrePersonProfile != null) {
                genrePersonProfile.setText(user.getGender() != null && !user.getGender().isEmpty() ? user.getGender() : "Non renseigné");
            }

            // Profession
            if (professionPersonProfile != null) {
                professionPersonProfile.setText(user.getProfession() != null && !user.getProfession().isEmpty() ? user.getProfession() : "Non renseignée");
            }

            // Relationship Status / Situation amoureuse
            if (matrimonialPersonProfile != null) {
                matrimonialPersonProfile.setText(user.getRelationshipStatus() != null && !user.getRelationshipStatus().isEmpty() ? user.getRelationshipStatus() : "Non renseignée");
            }

            // Email
            if (tvDialogEmail != null) {
                if ((user.isHideEmail() || user.isPrivateProfile()) && !isSelf) {
                    tvDialogEmail.setText("********");
                } else {
                    tvDialogEmail.setText(user.getEmail() != null ? user.getEmail() : "Non renseigné");
                }
            }

            // Phone
            if (phonePersonProfile != null) {
                if (user.isPrivateProfile() && !isSelf) {
                    phonePersonProfile.setText("********");
                } else {
                    phonePersonProfile.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Non renseigné");
                }
            }

            // Location
            if ((user.isHideLocation() || user.isPrivateProfile()) && !isSelf) {
                if (tvDialogNeighborhood != null) tvDialogNeighborhood.setText("********");
                if (tvDialogCity != null) tvDialogCity.setText("********");
                if (tvDialogCountry != null) tvDialogCountry.setText("********");
            } else {
                if (tvDialogNeighborhood != null)
                    tvDialogNeighborhood.setText(user.getNeighborhood() != null && !user.getNeighborhood().isEmpty() ? user.getNeighborhood() : "Non renseigné");
                if (tvDialogCity != null)
                    tvDialogCity.setText(user.getCity() != null && !user.getCity().isEmpty() ? user.getCity() : "Non renseignée");
                if (tvDialogCountry != null)
                    tvDialogCountry.setText(user.getCountry() != null && !user.getCountry().isEmpty() ? user.getCountry() : "Non renseigné");
            }
        }

        setupActionButtons(name, img);
    }

    private void setupActionButtons(String name, String img) {
        if (targetUserId == currentUserId || targetUserId == -1) {
            if (btnAddFriend != null) btnAddFriend.setVisibility(View.GONE);
            if (btnDialogChat != null) btnDialogChat.setVisibility(View.GONE);
            if (btnBlockUser != null) btnBlockUser.setVisibility(View.GONE);
            return;
        }

        // Add Friend Button
        if (btnAddFriend != null) {
            btnAddFriend.setVisibility(View.VISIBLE);
            FriendRepository.FriendRequestItem incomingReq = friendRepository.getIncomingRequestFromUser(currentUserId, targetUserId);
            boolean isFriendsList = friendRepository.getFriendsList(currentUserId, null)
                    .stream().anyMatch(usr -> usr.getId() == targetUserId);

            if (isFriendsList) {
                btnAddFriend.setText("Déjà amis");
                btnAddFriend.setEnabled(false);
            } else if (incomingReq != null) {
                btnAddFriend.setText("Accepter");
                btnAddFriend.setEnabled(true);
                btnAddFriend.setOnClickListener(v -> {
                    friendRepository.acceptFriendRequest(incomingReq.requestId, incomingReq.senderId, incomingReq.receiverId);
                    Toasty.success(this, "Demande acceptée !", Toast.LENGTH_SHORT).show();
                    loadUserData();
                });
            } else {
                btnAddFriend.setText("Ajouter");
                btnAddFriend.setEnabled(true);
                btnAddFriend.setOnClickListener(v -> {
                    boolean sent = friendRepository.sendFriendRequest(currentUserId, targetUserId);
                    if (sent) {
                        Toasty.success(this, "Invitation envoyée !", Toast.LENGTH_SHORT).show();
                        btnAddFriend.setText("Invitation envoyée");
                        btnAddFriend.setEnabled(false);
                    }
                });
            }
        }

        // Chat Button
        if (btnDialogChat != null) {
            btnDialogChat.setVisibility(View.VISIBLE);
            boolean hasConv = conversationRepository.hasConversation(currentUserId, targetUserId);
            if (hasConv) {
                btnDialogChat.setText("Envoyer un message 💬");
            } else {
                btnDialogChat.setText("Démarrer message 💬");
            }

            btnDialogChat.setOnClickListener(v -> {
                int conversationId = conversationRepository.createConversationIfNotExists(currentUserId, targetUserId);
                Intent intent = new Intent(User2Activity.this, ChatActivity.class);
                intent.putExtra("conversation_id", conversationId);
                intent.putExtra("receiver_id", targetUserId);
                intent.putExtra("receiver_name", name);
                intent.putExtra("receiver_profile_image", img);
                startActivity(intent);
            });
        }

        // Block Button
        if (btnBlockUser != null) {
            btnBlockUser.setVisibility(View.VISIBLE);
            boolean isBlocked = userRepository.isUserBlocked(currentUserId, targetUserId);
            updateBlockButtonState(isBlocked);

            btnBlockUser.setOnClickListener(v -> {
                boolean blocked = userRepository.isUserBlocked(currentUserId, targetUserId);
                if (blocked) {
                    userRepository.unblockUser(currentUserId, targetUserId);
                    Toasty.success(this, "Utilisateur débloqué", Toast.LENGTH_SHORT).show();
                } else {
                    userRepository.blockUser(currentUserId, targetUserId);
                    Toasty.warning(this, "Utilisateur bloqué", Toast.LENGTH_SHORT).show();
                }
                boolean nowBlocked = userRepository.isUserBlocked(currentUserId, targetUserId);
                updateBlockButtonState(nowBlocked);
            });
        }
    }

    private void updateBlockButtonState(boolean isBlocked) {
        if (btnBlockUser == null) return;
        if (isBlocked) {
            btnBlockUser.setText("Débloquer 🟢");
            btnBlockUser.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#388E3C")));
        } else {
            btnBlockUser.setText("Bloquer 🚫");
            btnBlockUser.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B71C1C")));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
