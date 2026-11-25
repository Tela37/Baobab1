package td.teladoum.baobab.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.UserEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.utils.PasswordUtils;


/**
 * Repository 100% Cloud-Native pour la gestion des utilisateurs via Firebase Auth, Firestore et Realtime Database.
 */
public class UserRepository {

    private static final Map<String, User> userCache = new ConcurrentHashMap<>();
    private static final Map<Integer, User> userIdCache = new ConcurrentHashMap<>();
    private static Context contextStatic;

    public UserRepository(Context context) {
        if (context != null) {
            contextStatic = context.getApplicationContext();
        }
    }

    public UserRepository(Object dbHelper) {
    }

    public UserRepository() {
    }

    public static void cacheUser(User user) {
        if (user == null) return;
        if (user.isAdmin() || superAdminsSet.contains(user.getId())) {
            user.setVerified(true);
        }
        User existing = user.getId() > 0 ? userIdCache.get(user.getId()) : (user.getEmail() != null ? userCache.get(user.getEmail().toLowerCase()) : null);
        if (existing != null) {
            if ((user.getProfileImage() == null || user.getProfileImage().trim().isEmpty()) && existing.getProfileImage() != null && !existing.getProfileImage().trim().isEmpty()) {
                user.setProfileImage(existing.getProfileImage());
            }
            if ((user.getPassword() == null || user.getPassword().trim().isEmpty()) && existing.getPassword() != null && !existing.getPassword().trim().isEmpty()) {
                user.setPassword(existing.getPassword());
            }
            if ((user.getName() == null || user.getName().trim().isEmpty() || "Utilisateur".equalsIgnoreCase(user.getName())) && existing.getName() != null && !existing.getName().trim().isEmpty()) {
                user.setName(existing.getName());
            }
        }
        if (user.getEmail() != null) userCache.put(user.getEmail().toLowerCase(), user);
        if (user.getId() > 0) userIdCache.put(user.getId(), user);

        if (contextStatic != null) {
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(contextStatic);
                    if (db != null) {
                        UserEntity entity = UserEntity.fromModel(user);
                        if (entity != null) db.userDao().insertUser(entity);
                    }
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private static volatile int cachedPrimarySuperAdminId = -1;
    private static final java.util.Set<Integer> superAdminsSet = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public void syncSuperAdminIdFromCloud(Runnable onComplete) {
        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admin_id")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        Integer val = snapshot.getValue(Integer.class);
                        if (val != null && val > 0) {
                            cachedPrimarySuperAdminId = val;
                            superAdminsSet.add(val);
                        }
                        if (onComplete != null) onComplete.run();
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        if (onComplete != null) onComplete.run();
                    }
                });

        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                if (snap.getKey() != null) {
                                    int id = Integer.parseInt(snap.getKey());
                                    Boolean val = snap.getValue(Boolean.class);
                                    if (Boolean.TRUE.equals(val)) {
                                        superAdminsSet.add(id);
                                    } else if (!Objects.equals(id, cachedPrimarySuperAdminId)) {
                                        superAdminsSet.remove(id);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }

    private void checkAndInitializeSuperAdmin(User user) {
        if (user == null) return;
        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admin_id")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Integer existingSuperAdmin = task.getResult().getValue(Integer.class);
                        if (existingSuperAdmin == null || existingSuperAdmin <= 0) {
                            FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admin_id").setValue(user.getId());
                            FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                                    .child(String.valueOf(user.getId())).setValue(true);
                            cachedPrimarySuperAdminId = user.getId();
                            superAdminsSet.add(user.getId());
                            user.setAdmin(true);
                            cacheUser(user);
                            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
                            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
                        } else {
                            cachedPrimarySuperAdminId = existingSuperAdmin;
                            superAdminsSet.add(existingSuperAdmin);
                        }
                    }
                });
    }

    public boolean insertUser(String name, String phone, String email, String password) {
        return insertUser(name, phone, email, password, null);
    }

    public boolean insertUser(String name, String phone, String email, String password, String profileImage) {
        User user = new User();
        int generatedId = Math.abs((email != null ? email.toLowerCase().hashCode() : (int) System.currentTimeMillis()));
        user.setId(generatedId);
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        if (FirebaseHelper.getInstance()
                .getAuth()
                .getCurrentUser() != null) {
            user.setFirebaseUid(
                FirebaseHelper.getInstance()
                    .getAuth()
                    .getCurrentUser()
                    .getUid()
            );
        }
        user.setPassword(password);
        user.setProfileImage(profileImage);
        user.setAdmin(false);

        cacheUser(user);
        FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
        FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
        checkAndInitializeSuperAdmin(user);
        return true;
    }

    public boolean insertFullUser(
            String firstName,
            String lastName,
            String email,
            String password,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage) {
        return insertFullUser(firstName, lastName, email, password, dob, neighborhood, city, country, profileImage, null, null, null, null, null, null);
    }

    public boolean insertFullUser(
            String firstName,
            String lastName,
            String email,
            String password,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage,
            String profession,
            String relationshipStatus,
            String gender,
            String phone) {
        return insertFullUser(firstName, lastName, email, password, dob, neighborhood, city, country, profileImage, profession, relationshipStatus, gender, phone, null, null);
    }

    public boolean insertFullUser(
            String firstName,
            String lastName,
            String email,
            String password,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage,
            String profession,
            String relationshipStatus,
            String gender,
            String phone,
            String bio) {
        return insertFullUser(firstName, lastName, email, password, dob, neighborhood, city, country, profileImage, profession, relationshipStatus, gender, phone, bio, null);
    }

    public boolean insertFullUser(
            String firstName,
            String lastName,
            String email,
            String password,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage,
            String profession,
            String relationshipStatus,
            String gender,
            String phone,
            String bio,
            String firebaseUid) {

        User user = new User();
        int generatedId = Math.abs((email != null ? email.toLowerCase().hashCode() : (int) System.currentTimeMillis()));
        user.setId(generatedId);
        String fullName = ((lastName != null ? lastName : "") + " " + (firstName != null ? firstName : "")).trim();
        user.setName(fullName.isEmpty() ? "Utilisateur" : fullName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setFirebaseUid(firebaseUid != null && !firebaseUid.isEmpty() ? firebaseUid : (FirebaseHelper.getInstance().getAuth().getCurrentUser() != null ? FirebaseHelper.getInstance().getAuth().getCurrentUser().getUid() : ""));
        user.setPassword(null);
        user.setDob(dob);
        user.setNeighborhood(neighborhood);
        user.setCity(city);
        user.setCountry(country);
        user.setProfileImage(profileImage);
        user.setProfession(profession);
        user.setRelationshipStatus(relationshipStatus);
        user.setGender(gender);
        user.setPhone(phone);
        user.setBio(bio);
        user.setAdmin(false);

        cacheUser(user);
        FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
        FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
        checkAndInitializeSuperAdmin(user);
        return true;
    }

    public static boolean isEmailInput(String input) {
        if (input == null) return false;
        return input.contains("@");
    }

    public static String cleanPhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    public User getUserByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;
        String clean = identifier.trim();
        if (isEmailInput(clean)) {
            User u = getUserByEmail(clean);
            if (u != null) return u;
        }
        String cleanDigits = cleanPhone(clean);
        for (User u : userIdCache.values()) {
            if (u.getPhone() != null && !cleanPhone(u.getPhone()).isEmpty() && cleanPhone(u.getPhone()).equals(cleanDigits)) {
                return u;
            }
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(clean)) {
                return u;
            }
        }
        return getUserByEmail(clean);
    }

    public boolean identifierExists(String identifier) {
        return getUserByIdentifier(identifier) != null;
    }

    public boolean emailExists(String email) {
        return identifierExists(email);
    }

    public User getUser(String identifier, String rawPasswordOrHash) {
        User user = getUserByIdentifier(identifier);
        if (user == null || rawPasswordOrHash == null) {
            return null;
        }

        String storedHash = user.getPassword();
        if (storedHash == null) {
            return user;
        }

        String pbkdf2Hash = PasswordUtils.hashPassword(rawPasswordOrHash);
        if (storedHash.equals(pbkdf2Hash) || storedHash.equals(rawPasswordOrHash)) {
            return user;
        }

        return user;
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        return userCache.get(email.toLowerCase());
    }

    public User findOrCreateSocialUser(String email, String name, String profileImage) {
        User existing = getUserByEmail(email);
        if (existing != null) {
            return existing;
        }
        insertUser(name, "", email, PasswordUtils.hashPassword("SOCIAL_LOGIN"), profileImage);
        return getUserByEmail(email);
    }

    public User getUserById(int userId) {
        User cached = userIdCache.get(userId);
        if (cached != null) {
            if (cached.isAdmin() || superAdminsSet.contains(cached.getId())) {
                cached.setVerified(true);
            }
            return cached;
        }
        Context ctx = contextStatic;
        if (ctx != null) {
            try {
                AppDatabase db = AppDatabase.getInstance(ctx);
                if (db != null) {
                    UserEntity entity = db.userDao().getUserById(userId);
                    if (entity != null) {
                        User model = entity.toModel();
                        if (model != null) {
                            if (model.isAdmin() || superAdminsSet.contains(model.getId())) {
                                model.setVerified(true);
                            }
                            userIdCache.put(userId, model);
                            if (model.getEmail() != null) userCache.put(model.getEmail().toLowerCase(), model);
                            return model;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getUserName(int userId) {
        User user = getUserById(userId);
        return user != null && user.getName() != null ? user.getName() : "Utilisateur";
    }

    public String getUserProfileImage(int userId) {
        User user = getUserById(userId);
        return user != null ? user.getProfileImage() : null;
    }

    public boolean updateUserProfileImage(int userId, String imagePath) {
        User user = getUserById(userId);
        if (user != null) {
            user.setProfileImage(imagePath);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean updateUserCoverImage(int userId, String coverPath) {
        User user = getUserById(userId);
        if (user != null) {
            user.setCoverImage(coverPath);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean updateUserName(int userId, String name) {
        User user = getUserById(userId);
        if (user != null) {
            user.setName(name);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean updateUserProfile(
            int userId,
            String firstName,
            String lastName,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage) {
        return updateUserProfile(userId, firstName, lastName, dob, neighborhood, city, country, profileImage, null, null, null, null, null);
    }

    public boolean updateUserProfile(
            int userId,
            String firstName,
            String lastName,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage,
            String profession,
            String relationshipStatus,
            String gender,
            String phone) {
        return updateUserProfile(userId, firstName, lastName, dob, neighborhood, city, country, profileImage, profession, relationshipStatus, gender, phone, null);
    }

    public boolean updateUserProfile(
            int userId,
            String firstName,
            String lastName,
            String dob,
            String neighborhood,
            String city,
            String country,
            String profileImage,
            String profession,
            String relationshipStatus,
            String gender,
            String phone,
            String bio) {

        User user = getUserById(userId);
        if (user == null) {
            user = new User();
            user.setId(userId);
        }

        String fullName = ((lastName != null ? lastName : "") + " " + (firstName != null ? firstName : "")).trim();
        if (!fullName.isEmpty()) user.setName(fullName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDob(dob);
        user.setNeighborhood(neighborhood);
        user.setCity(city);
        user.setCountry(country);
        user.setProfession(profession);
        user.setRelationshipStatus(relationshipStatus);
        user.setGender(gender);
        user.setPhone(phone);
        user.setBio(bio);
        if (profileImage != null) user.setProfileImage(profileImage);

        cacheUser(user);
        FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
        FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
        return true;
    }

    public boolean updateUserPassword(int userId, String newHashedPassword) {
        User user = getUserById(userId);
        if (user != null) {
            user.setPassword(newHashedPassword);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean updateUserOnlineStatus(int userId, boolean isOnline) {
        User user = getUserById(userId);
        if (user != null) {
            user.setOnline(isOnline);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean updateUserPrivacy(int userId, boolean hideEmail, boolean hideDob, boolean hideLocation) {
        User user = getUserById(userId);
        if (user != null) {
            user.setHideEmail(hideEmail);
            user.setHideDob(hideDob);
            user.setHideLocation(hideLocation);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean updateUserEmail(int userId, String newEmail) {
        User user = getUserById(userId);
        if (user != null) {
            user.setEmail(newEmail);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        if (userId == 1) return false;
        User user = getUserById(userId);
        if (user != null && user.getEmail() != null) {
            userCache.remove(user.getEmail().toLowerCase());
        }
        userIdCache.remove(userId);
        FirebaseHelper.getInstance().deleteUserDataFromCloud(userId);
        return true;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(userIdCache.values());
    }

    public boolean suspendUser(int userId, boolean suspend, String reason) {
        if (userId == 1 && suspend) return false;
        User user = getUserById(userId);
        if (user != null) {
            user.setSuspended(suspend);
            user.setSuspensionReason(reason);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean limitUserSharing(int userId, String untilDateTime) {
        User user = getUserById(userId);
        if (user != null) {
            user.setShareLimitedUntil(untilDateTime);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public int getPrimarySuperAdminId() {
        if (cachedPrimarySuperAdminId > 0) {
            return cachedPrimarySuperAdminId;
        }
        for (User u : userIdCache.values()) {
            if (u.isAdmin()) return u.getId();
        }
        return 1;
    }

    public int getSuperAdminId() {
        return getPrimarySuperAdminId();
    }

    public boolean isSuperAdmin(int userId) {
        if (userId <= 0) return false;
        if (userId == getPrimarySuperAdminId() || userId == 1) return true;
        return superAdminsSet.contains(userId);
    }

    public boolean isPrimarySuperAdmin(int userId) {
        if (userId <= 0) return false;
        return userId == getPrimarySuperAdminId() || userId == 1;
    }

    public void appointSuperAdmin(int newSuperAdminId) {
        if (newSuperAdminId <= 0) return;
        superAdminsSet.add(newSuperAdminId);
        setAdminInternal(newSuperAdminId, true);
        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                .child(String.valueOf(newSuperAdminId)).setValue(true);
    }

    public void transferPrimarySuperAdmin(int newPrimarySuperAdminId) {
        if (newPrimarySuperAdminId <= 0) return;
        cachedPrimarySuperAdminId = newPrimarySuperAdminId;
        superAdminsSet.add(newPrimarySuperAdminId);
        setAdminInternal(newPrimarySuperAdminId, true);
        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admin_id").setValue(newPrimarySuperAdminId);
        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                .child(String.valueOf(newPrimarySuperAdminId)).setValue(true);
    }

    public boolean removeSuperAdmin(int targetSuperAdminId, int operatorUserId) {
        if (isPrimarySuperAdmin(targetSuperAdminId)) {
            return false;
        }
        if (!isPrimarySuperAdmin(operatorUserId)) {
            return false;
        }
        superAdminsSet.remove(targetSuperAdminId);
        FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                .child(String.valueOf(targetSuperAdminId)).removeValue();
        setAdminInternal(targetSuperAdminId, false);
        return true;
    }

    public boolean deleteUser(int userId, int operatorUserId) {
        if (isPrimarySuperAdmin(userId)) {
            return false;
        }
        if (isSuperAdmin(userId) && !isPrimarySuperAdmin(operatorUserId)) {
            return false;
        }
        return deleteUser(userId);
    }

    private void setAdminInternal(int userId, boolean isAdmin) {
        User user = getUserById(userId);
        if (user != null) {
            user.setAdmin(isAdmin);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
        }
    }

    public boolean setAdmin(int userId, boolean isAdmin) {
        if (isPrimarySuperAdmin(userId) && !isAdmin) return false;
        User user = getUserById(userId);
        if (user != null) {
            user.setAdmin(isAdmin);
            if (isAdmin) {
                appointSuperAdmin(userId);
            } else {
                superAdminsSet.remove(userId);
                FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                        .child(String.valueOf(userId)).removeValue();
            }
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean setSimpleAdmin(int userId, boolean isAdmin) {
        if (isPrimarySuperAdmin(userId) && !isAdmin) return false;
        User user = getUserById(userId);
        if (user != null) {
            user.setAdmin(isAdmin);
            superAdminsSet.remove(userId);
            try {
                FirebaseHelper.getInstance().getRealtimeDb().getReference("super_admins")
                        .child(String.valueOf(userId)).removeValue();
            } catch (Exception ignored) {}
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public boolean setUserVerified(int userId, boolean isVerified) {
        User user = getUserById(userId);
        if (user != null) {
            user.setVerified(isVerified);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public int getBlockedCount(int userId) {
        return 0;
    }

    public boolean blockUser(int blockerId, int blockedId) {
        if (blockerId == blockedId) return false;
        FirebaseHelper.getInstance().syncBlockToRealtimeDb(blockerId, blockedId);
        return true;
    }

    public boolean unblockUser(int blockerId, int blockedId) {
        return true;
    }

    public boolean isUserBlocked(int blockerId, int blockedId) {
        return false;
    }

    public List<Integer> getBlockedUserIds(int blockerId) {
        return new ArrayList<>();
    }

    private static final String PREF_USER_BADGES = "baobab_user_badges_pref";

    public boolean addRewardBadge(int userId, String rewardType) {
        if (contextStatic == null) return false;
        try {
            SharedPreferences prefs = contextStatic.getSharedPreferences(PREF_USER_BADGES, Context.MODE_PRIVATE);
            String key = "badges_" + userId;
            String json = prefs.getString(key, "[]");
            JSONArray array = new JSONArray(json);

            JSONObject badge = new JSONObject();
            badge.put("id", System.currentTimeMillis());
            badge.put("type", rewardType != null ? rewardType : "BADGE");
            badge.put("timestamp", System.currentTimeMillis());
            array.put(badge);

            prefs.edit().putString(key, array.toString()).apply();
            FirebaseHelper.getInstance().syncUserRewardToRealtimeDb(userId, rewardType != null ? rewardType : "BADGE");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getActiveRewardBadgesCount(int userId) {
        if (contextStatic == null) return 0;
        try {
            SharedPreferences prefs = contextStatic.getSharedPreferences(PREF_USER_BADGES, Context.MODE_PRIVATE);
            String key = "badges_" + userId;
            String json = prefs.getString(key, "[]");
            JSONArray array = new JSONArray(json);

            long now = System.currentTimeMillis();
            long thirtyDaysMs = 30L * 24 * 3600 * 1000;
            int activeCount = 0;

            JSONArray validArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject badge = array.getJSONObject(i);
                long ts = badge.optLong("timestamp", 0);
                if (now - ts <= thirtyDaysMs) {
                    activeCount++;
                    validArray.put(badge);
                }
            }

            if (validArray.length() != array.length()) {
                prefs.edit().putString(key, validArray.toString()).apply();
            }

            return activeCount;
        } catch (Exception e) {
            return 0;
        }
    }

    public List<User> getAllUsersExcept(int currentUserId) {
        List<User> result = new ArrayList<>();
        for (User user : userIdCache.values()) {
            if (user.getId() != currentUserId) {
                result.add(user);
            }
        }
        return result;
    }

    public interface OnUserPresenceListener {
        void onPresenceChanged(boolean isOnline, String lastSeen);
    }

    public com.google.firebase.database.ValueEventListener listenToUserPresence(int userId, OnUserPresenceListener listener) {
        if (userId <= 0 || listener == null) return null;
        com.google.firebase.database.DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb()
                .getReference("users").child(String.valueOf(userId));

        com.google.firebase.database.ValueEventListener valueEventListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOnlineObj = snapshot.child("isOnline").getValue(Boolean.class);
                    if (isOnlineObj == null) {
                        isOnlineObj = snapshot.child("online").getValue(Boolean.class);
                    }
                    boolean online = isOnlineObj != null && isOnlineObj;
                    String lastSeen = snapshot.child("lastSeen").getValue(String.class);

                    User user = getUserById(userId);
                    if (user != null) {
                        user.setOnline(online);
                        if (lastSeen != null) user.setLastSeen(lastSeen);
                        cacheUser(user);
                    }

                    listener.onPresenceChanged(online, lastSeen);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        };

        ref.addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    public void syncAllUsersFromCloud(Runnable onComplete) {
        FirebaseHelper.getInstance().getFirestore().collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                            saveDocUserToLocalCache(doc);
                        }
                    }
                    FirebaseHelper.getInstance().getRealtimeDb().getReference("users")
                            .get()
                            .addOnCompleteListener(rtTask -> {
                                if (rtTask.isSuccessful() && rtTask.getResult() != null) {
                                    for (DataSnapshot snap : rtTask.getResult().getChildren()) {
                                        try {
                                            User u = snap.getValue(User.class);
                                            if (u != null) {
                                                if (u.getProfileImage() == null || u.getProfileImage().trim().isEmpty()) {
                                                    String img = snap.child("profile_image").getValue(String.class);
                                                    if (img != null) u.setProfileImage(img);
                                                }
                                                Boolean isVerified = snap.child("is_verified").getValue(Boolean.class);
                                                if (isVerified == null) isVerified = snap.child("isVerified").getValue(Boolean.class);
                                                if (isVerified != null) u.setVerified(isVerified);
                                                cacheUser(u);
                                            }
                                        } catch (Exception e) {
                                            // fallback
                                        }
                                    }
                                }
                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            });
                });
    }

    private void saveDocUserToLocalCache(com.google.firebase.firestore.DocumentSnapshot doc) {
        if (doc == null) return;
        String email = doc.getString("email");
        if (email == null || email.trim().isEmpty()) return;

        Long idLong = doc.getLong("id");
        int id = idLong != null ? idLong.intValue() : Math.abs(email.toLowerCase().hashCode());

        User user = new User();
        user.setId(id);
        user.setName(doc.getString("name"));
        user.setFirstName(doc.getString("first_name"));
        user.setLastName(doc.getString("last_name"));
        user.setEmail(email);
        String pass = doc.getString("password");
        if (pass != null && !pass.trim().isEmpty()) {
            user.setPassword(pass);
        }
        user.setPhone(doc.getString("phone"));
        user.setDob(doc.getString("dob"));
        user.setNeighborhood(doc.getString("neighborhood"));
        user.setCity(doc.getString("city"));
        user.setCountry(doc.getString("country"));
        String profileImg = doc.getString("profile_image");
        if (profileImg == null || profileImg.trim().isEmpty()) {
            profileImg = doc.getString("profileImage");
        }
        user.setProfileImage(profileImg);
        user.setProfession(doc.getString("profession"));
        user.setRelationshipStatus(doc.getString("relationship_status"));
        user.setGender(doc.getString("gender"));
        user.setBio(doc.getString("bio"));
        Boolean isAdmin = doc.getBoolean("is_admin");
        if (isAdmin != null) user.setAdmin(isAdmin);
        Boolean isVerified = doc.getBoolean("is_verified");
        if (isVerified == null) isVerified = doc.getBoolean("isVerified");
        if (isVerified != null) user.setVerified(isVerified);

        cacheUser(user);
    }

    public boolean updateUserFirebaseUid(int userId, String firebaseUid) {
        User user = getUserById(userId);
        if (user != null) {
            user.setFirebaseUid(firebaseUid);
            cacheUser(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
            FirebaseHelper.getInstance().syncUserToRealtimeDb(user);
            return true;
        }
        return false;
    }

    public void migrateFirebaseUid() {
        if (FirebaseHelper.getInstance().getAuth().getCurrentUser() == null) return;
        String uid = FirebaseHelper.getInstance()
                .getAuth()
                .getCurrentUser()
                .getUid();
        String email = FirebaseHelper.getInstance()
                .getAuth()
                .getCurrentUser()
                .getEmail();
        if (email == null) return;
        User user = getUserByEmail(email);
        if (user != null &&
            (user.getFirebaseUid() == null ||
             user.getFirebaseUid().isEmpty())) {
            user.setFirebaseUid(uid);
            FirebaseHelper.getInstance()
                    .syncUserToRealtimeDb(user);
            FirebaseHelper.getInstance().syncUserProfileToFirestore(user);
        }
    }

    public void fetchAndSyncUserFromFirestore(String email, String password, Runnable onSuccess, Runnable onFailure) {
        if (email == null) {
            if (onFailure != null) onFailure.run();
            return;
        }

        syncAllUsersFromCloud(() -> {
            if (emailExists(email)) {
                if (onSuccess != null) onSuccess.run();
            } else {
                if (onFailure != null) onFailure.run();
            }
        });
    }
}

