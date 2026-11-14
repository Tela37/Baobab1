package td.teladoum.baobab.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.helpers.ContactSyncHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.FriendRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité de recherche d'amis.
 * Permet de rechercher des utilisateurs par nom ou email et de leur envoyer une demande d'ami.
 */
public class TrouverAmisActivity extends AppCompatActivity {

    private EditText etSearchUsersToBefriend;
    private RecyclerView rvUsersToBefriend;

    private FriendRepository friendRepository;
    private SessionManager sessionManager;
    private int currentUserId;

    private UserSearchAdapter adapter;
    private final List<FriendRepository.UserSearchItem> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouver_amis);

        Toolbar toolbar = findViewById(R.id.toolbarTrouverAmis);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        friendRepository = new FriendRepository(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        etSearchUsersToBefriend = findViewById(R.id.etSearchUsersToBefriend);
        Button btnSyncPhonebookContacts = findViewById(R.id.btnSyncPhonebookContacts);
        if (btnSyncPhonebookContacts != null) {
            btnSyncPhonebookContacts.setOnClickListener(v -> checkAndSyncContacts());
        }
        rvUsersToBefriend = findViewById(R.id.rvUsersToBefriend);

        rvUsersToBefriend.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserSearchAdapter(userList, new OnUserSearchActionListener() {
            @Override
            public void onAddClick(FriendRepository.UserSearchItem item) {
                boolean sent = friendRepository.sendFriendRequest(currentUserId, item.userId);
                if (sent) {
                    Toasty.success(TrouverAmisActivity.this, "Invitation envoyée !", Toasty.LENGTH_SHORT).show();
                    item.requestSent = true;
                    adapter.notifyDataSetChanged();
                } else {
                    Toasty.error(TrouverAmisActivity.this, "Erreur d'envoi", Toasty.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelClick(FriendRepository.UserSearchItem item) {
                boolean canceled = friendRepository.cancelFriendRequest(currentUserId, item.userId);
                if (canceled) {
                    Toasty.info(TrouverAmisActivity.this, "Demande annulée", Toasty.LENGTH_SHORT).show();
                    item.requestSent = false;
                    adapter.notifyDataSetChanged();
                } else {
                    Toasty.error(TrouverAmisActivity.this, "Erreur d'annulation", Toasty.LENGTH_SHORT).show();
                }
            }
        });
        rvUsersToBefriend.setAdapter(adapter);

        etSearchUsersToBefriend.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        UserRepository userRepo = new UserRepository(this);
        userRepo.syncAllUsersFromCloud(() -> {
            FriendRepository.syncFriendDataFromCloud(() -> {
                searchUsers("");
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (etSearchUsersToBefriend != null) {
            searchUsers(etSearchUsersToBefriend.getText().toString());
        } else {
            searchUsers("");
        }
    }

    private void checkAndSyncContacts() {
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, 105);
        } else {
            loadAndDisplayPhonebookContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 105) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                loadAndDisplayPhonebookContacts();
            } else {
                Toasty.warning(this, "Permission d'accès aux contacts refusée", Toasty.LENGTH_SHORT).show();
            }
        }
    }

    private void loadAndDisplayPhonebookContacts() {
        List<ContactSyncHelper.PhoneContactItem> matchedList = ContactSyncHelper.matchContactsWithAppUsers(this, currentUserId);
        if (matchedList.isEmpty()) {
            Toasty.info(this, "Aucun contact trouvé dans le répertoire", Toasty.LENGTH_SHORT).show();
            return;
        }

        List<ContactSyncHelper.PhoneContactItem> appContacts = new ArrayList<>();
        List<ContactSyncHelper.PhoneContactItem> otherContacts = new ArrayList<>();

        for (ContactSyncHelper.PhoneContactItem c : matchedList) {
            if (c.isRegisteredOnApp && c.matchedUser != null) {
                appContacts.add(c);
            } else {
                otherContacts.add(c);
            }
        }

        List<String> displayTitles = new ArrayList<>();
        List<ContactSyncHelper.PhoneContactItem> combinedList = new ArrayList<>();

        for (ContactSyncHelper.PhoneContactItem c : appContacts) {
            displayTitles.add("🟢 " + c.contactName + " (Sur BaobabTD)");
            combinedList.add(c);
        }

        for (ContactSyncHelper.PhoneContactItem c : otherContacts) {
            displayTitles.add("✉️ " + c.contactName + " (" + c.phoneNumber + ")");
            combinedList.add(c);
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📱 Contacts du téléphone (" + matchedList.size() + ")")
                .setItems(displayTitles.toArray(new String[0]), (dialog, which) -> {
                    ContactSyncHelper.PhoneContactItem selected = combinedList.get(which);
                    if (selected.isRegisteredOnApp && selected.matchedUser != null) {
                        User u = selected.matchedUser;
                        String[] actions = {"💬 Discuter", "📞 Appeler", "👤 Voir le profil", "➕ Ajouter en ami"};
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle(selected.contactName)
                                .setItems(actions, (dialog2, whichAction) -> {
                                    if (whichAction == 0) {
                                        td.teladoum.baobab.repositories.ConversationRepository convRepo = new td.teladoum.baobab.repositories.ConversationRepository(this);
                                        int convId = convRepo.createConversationIfNotExists(currentUserId, u.getId());
                                        Intent intent = new Intent(TrouverAmisActivity.this, ChatActivity.class);
                                        intent.putExtra("conversation_id", convId);
                                        intent.putExtra("receiver_id", u.getId());
                                        intent.putExtra("receiver_name", u.getName());
                                        intent.putExtra("receiver_profile_image", u.getProfileImage());
                                        startActivity(intent);
                                    } else if (whichAction == 1) {
                                        Intent intent = new Intent(TrouverAmisActivity.this, CallActivity.class);
                                        intent.putExtra("target_id", u.getId());
                                        intent.putExtra("target_name", u.getName());
                                        intent.putExtra("is_outgoing", true);
                                        startActivity(intent);
                                    } else if (whichAction == 2) {
                                        Intent intent = new Intent(TrouverAmisActivity.this, PageUtilisateurActivity.class);
                                        intent.putExtra("user_id", u.getId());
                                        startActivity(intent);
                                    } else if (whichAction == 3) {
                                        friendRepository.sendFriendRequest(currentUserId, u.getId());
                                        Toasty.success(TrouverAmisActivity.this, "Invitation envoyée à " + u.getName(), Toasty.LENGTH_SHORT).show();
                                    }
                                })
                                .show();
                    } else {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Inviter " + selected.contactName)
                                .setMessage("Ce contact n'est pas encore inscrit sur BaobabTD. Voulez-vous lui envoyer une invitation ?")
                                .setPositiveButton("Inviter 📩", (dialog2, which2) -> {
                                    try {
                                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                        sendIntent.setType("text/plain");
                                        sendIntent.putExtra(Intent.EXTRA_TEXT, "Rejoins-moi sur BaobabTD ! Télécharge l'application ici : https://play.google.com/store/apps/details?id=" + getPackageName());
                                        startActivity(Intent.createChooser(sendIntent, "Inviter via"));
                                    } catch (Exception e) {
                                        Toasty.error(TrouverAmisActivity.this, "Erreur d'envoi d'invitation", Toasty.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Annuler", null)
                                .show();
                    }
                })
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void searchUsers(String query) {
        List<FriendRepository.UserSearchItem> results = friendRepository.searchUsersToBefriend(currentUserId, query);
        userList.clear();
        userList.addAll(results);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private interface OnUserSearchActionListener {
        void onAddClick(FriendRepository.UserSearchItem item);
        void onCancelClick(FriendRepository.UserSearchItem item);
    }

    private static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private final List<FriendRepository.UserSearchItem> list;
        private final OnUserSearchActionListener listener;

        public UserSearchAdapter(List<FriendRepository.UserSearchItem> list, OnUserSearchActionListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FriendRepository.UserSearchItem item = list.get(position);

            holder.tvUserSearchName.setText(item.name != null ? item.name : "Utilisateur");

            String avatarUrl = item.profileImage;
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                UserRepository uRepo = new UserRepository(holder.itemView.getContext());
                User u = uRepo.getUserById(item.userId);
                if (u != null && u.getProfileImage() != null && !u.getProfileImage().trim().isEmpty()) {
                    avatarUrl = u.getProfileImage();
                }
            }
            ImageUtils.loadProfileImage(holder.itemView.getContext(), avatarUrl, holder.imgUserSearchAvatar);

            View.OnClickListener profileClickListener = v -> {
                Intent intent = new Intent(v.getContext(), PageUtilisateurActivity.class);
                intent.putExtra("user_id", item.userId);
                intent.putExtra("user_name", item.name);
                intent.putExtra("user_profile_image", item.profileImage);
                v.getContext().startActivity(intent);
            };

            holder.imgUserSearchAvatar.setOnClickListener(profileClickListener);
            holder.tvUserSearchName.setOnClickListener(profileClickListener);

            FriendRepository friendRepo = new FriendRepository(holder.itemView.getContext());
            SessionManager sessionManager = new SessionManager(holder.itemView.getContext());
            int currentUserId = sessionManager.getUserId();
            FriendRepository.FriendRequestItem incomingReq = friendRepo.getIncomingRequestFromUser(currentUserId, item.userId);

            if (incomingReq != null) {
                holder.btnAddFriend.setText("Accepter");
                holder.btnAddFriend.setEnabled(true);
                holder.btnAddFriend.setTextColor(Color.WHITE);
                holder.btnAddFriend.setVisibility(View.VISIBLE);
                holder.btnAddFriend.setOnClickListener(v -> {
                    friendRepo.acceptFriendRequest(incomingReq.requestId, incomingReq.senderId, incomingReq.receiverId);
                    Toasty.success(holder.itemView.getContext(), "Demande acceptée !", android.widget.Toast.LENGTH_SHORT).show();
                    item.isFriend = true;
                    notifyItemChanged(position);
                });
                if (holder.btnCancelRequest != null) {
                    holder.btnCancelRequest.setText("Refuser");
                    holder.btnCancelRequest.setVisibility(View.VISIBLE);
                    holder.btnCancelRequest.setOnClickListener(v -> {
                        friendRepo.declineFriendRequest(incomingReq.requestId);
                        Toasty.info(holder.itemView.getContext(), "Demande refusée", android.widget.Toast.LENGTH_SHORT).show();
                        notifyItemChanged(position);
                    });
                }
            } else if (item.isFriend) {
                holder.btnAddFriend.setText("Déjà amis");
                holder.btnAddFriend.setEnabled(false);
                holder.btnAddFriend.setTextColor(Color.GRAY);
                holder.btnAddFriend.setVisibility(View.VISIBLE);
                if (holder.btnCancelRequest != null) holder.btnCancelRequest.setVisibility(View.GONE);
            } else if (item.requestSent) {
                holder.btnAddFriend.setText("Demande d'ami envoyée");
                holder.btnAddFriend.setEnabled(false);
                holder.btnAddFriend.setTextColor(Color.parseColor("#43A047"));
                holder.btnAddFriend.setVisibility(View.VISIBLE);

                if (holder.btnCancelRequest != null) {
                    holder.btnCancelRequest.setText("Annuler");
                    holder.btnCancelRequest.setVisibility(View.VISIBLE);
                    holder.btnCancelRequest.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onCancelClick(item);
                        }
                    });
                }
            } else {
                holder.btnAddFriend.setText("Ajouter");
                holder.btnAddFriend.setEnabled(true);
                holder.btnAddFriend.setTextColor(Color.WHITE);
                holder.btnAddFriend.setVisibility(View.VISIBLE);
                holder.btnAddFriend.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddClick(item);
                    }
                });
                if (holder.btnCancelRequest != null) holder.btnCancelRequest.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgUserSearchAvatar;
            TextView tvUserSearchName;
            Button btnAddFriend;
            Button btnCancelRequest;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgUserSearchAvatar = itemView.findViewById(R.id.imgUserSearchAvatar);
                tvUserSearchName = itemView.findViewById(R.id.tvUserSearchName);
                btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
                btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            }
        }
    }
}

