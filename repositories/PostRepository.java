package td.teladoum.baobab.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import td.teladoum.baobab.R;
import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.PostEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.Post;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.utils.TimeUtils;


/**
 * Repository Cloud-Native pour la gestion des publications (posts) exclusivement via Realtime Database,
 * avec persistance locale complète (cache de flux persistant hors-ligne).
 */
public class PostRepository {

    private static final Map<Integer, Post> postsCache = new ConcurrentHashMap<>();
    private static final Map<String, String> userReactionsMap = new ConcurrentHashMap<>();
    private static final List<ReportItem> reportsCache = Collections.synchronizedList(new ArrayList<>());
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Context context;
    private static Context contextStatic;

    private static final String PREF_OFFLINE_POSTS = "baobab_offline_posts_pref";
    private static final String KEY_OFFLINE_POSTS_JSON = "offline_posts_json";

    private static final String PREF_CACHED_FEED = "baobab_cached_feed_pref";
    private static final String KEY_CACHED_FEED_JSON = "cached_feed_json";

    public PostRepository(Context context) {
        this.notificationRepository = new NotificationRepository(context);
        this.userRepository = new UserRepository(context);
        this.context = context != null ? context.getApplicationContext() : null;
        if (this.context != null) {
            contextStatic = this.context;
        }
    }

    public PostRepository(Object unused) {
        this.notificationRepository = new NotificationRepository(null);
        this.userRepository = new UserRepository();
        this.context = null;
    }

    public boolean createPost(int userId, String content, String imagePath) {
        return createPost(userId, 0, content, imagePath, null, false);
    }

    public boolean createPost(int userId, String content, String imagePath, String videoPath) {
        return createPost(userId, 0, content, imagePath, videoPath, false);
    }

    public boolean createPost(int userId, String content, String imagePath, String videoPath, boolean isSensitive) {
        return createPost(userId, 0, content, imagePath, videoPath, isSensitive);
    }

    public boolean createPost(int userId, int targetUserId, String content, String imagePath, String videoPath, boolean isSensitive) {
        if (isUserRestricted(userId)) {
            return false;
        }

        int id = Math.abs((userId + "_" + System.currentTimeMillis() + "_" + Math.random()).hashCode());
        String createdAt = TimeUtils.getCurrentLocalDateTime();
        String authorName = getUserName(userId);
        String authorProfileImage = getUserProfileImage(userId);

        Post post = new Post(id, userId, authorName, authorProfileImage, content, imagePath, videoPath, 0, false, createdAt);
        post.setSensitive(isSensitive);
        post.setTargetUserId(targetUserId);

        postsCache.put(id, post);
        saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));

        if (isNetworkAvailable(context)) {
            try {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
            } catch (Exception e) {
                saveOfflinePost(context, post);
                if (context != null) {
                    es.dmoral.toasty.Toasty.info(context, context.getString(R.string.toast_post_saved_offline), es.dmoral.toasty.Toasty.LENGTH_LONG).show();
                }
            }
        } else {
            saveOfflinePost(context, post);
            if (context != null) {
                es.dmoral.toasty.Toasty.info(context, context.getString(R.string.toast_post_saved_offline), es.dmoral.toasty.Toasty.LENGTH_LONG).show();
            }
        }

        if (targetUserId > 0 && targetUserId != userId) {
            notificationRepository.addNotification(
                    targetUserId,
                    userId,
                    "POST_ON_WALL",
                    "POST",
                    id,
                    "a publié une publication sur votre profil"
            );
        }

        return true;
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return true;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            // ignore
        }
        return true;
    }

    public static void saveOfflinePost(Context context, Post post) {
        if (context == null || post == null) return;
        try {
            List<Post> list = getOfflinePendingPosts(context);
            boolean exists = false;
            for (Post p : list) {
                if (p.getId() == post.getId()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                list.add(post);
                saveAllOfflinePosts(context, list);
            }
        } catch (Exception ignored) {}
    }

    public static List<Post> getOfflinePendingPosts(Context context) {
        List<Post> list = new ArrayList<>();
        if (context == null) return list;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_POSTS, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_OFFLINE_POSTS_JSON, "[]");
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Post p = jsonToPost(obj);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void saveAllOfflinePosts(Context context, List<Post> posts) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_POSTS, Context.MODE_PRIVATE);
            JSONArray array = new JSONArray();
            if (posts != null) {
                for (Post p : posts) {
                    JSONObject obj = postToJson(p);
                    if (obj != null) {
                        array.put(obj);
                    }
                }
            }
            prefs.edit().putString(KEY_OFFLINE_POSTS_JSON, array.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void saveCachedFeedPosts(Context context, List<Post> posts) {
        if (context == null || posts == null) return;
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    List<PostEntity> entities = new ArrayList<>();
                    for (Post p : posts) {
                        PostEntity entity = PostEntity.fromModel(p);
                        if (entity != null) entities.add(entity);
                    }
                    db.postDao().insertPosts(entities);
                }
            } catch (Exception ignored) {}
        }).start();

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_CACHED_FEED, Context.MODE_PRIVATE);
            JSONArray array = new JSONArray();
            for (Post p : posts) {
                JSONObject obj = postToJson(p);
                if (obj != null) {
                    array.put(obj);
                }
            }
            prefs.edit().putString(KEY_CACHED_FEED_JSON, array.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<Post> getCachedFeedPosts(Context context) {
        List<Post> list = new ArrayList<>();
        if (context == null) return list;
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null) {
                List<PostEntity> entities = db.postDao().getAllPosts();
                if (entities != null && !entities.isEmpty()) {
                    for (PostEntity entity : entities) {
                        Post p = entity.toModel();
                        if (p != null) list.add(p);
                    }
                    return list;
                }
            }
        } catch (Exception ignored) {}

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_CACHED_FEED, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_CACHED_FEED_JSON, "[]");
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Post p = jsonToPost(obj);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static int syncPendingPosts(Context context) {
        if (context == null || !isNetworkAvailable(context)) return 0;
        List<Post> pending = getOfflinePendingPosts(context);
        if (pending.isEmpty()) return 0;

        int syncedCount = 0;
        List<Post> remaining = new ArrayList<>();
        for (Post post : pending) {
            try {
                if (!postsCache.containsKey(post.getId())) {
                    FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
                    postsCache.put(post.getId(), post);
                    syncedCount++;
                }
            } catch (Exception e) {
                remaining.add(post);
            }
        }
        saveAllOfflinePosts(context, remaining);
        if (!postsCache.isEmpty()) {
            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));
        }
        return syncedCount;
    }

    private static String oldestLoadedPostKey = null;
    private static boolean hasMorePostsToLoad = true;

    public static boolean hasMorePosts() {
        return hasMorePostsToLoad;
    }

    public static void resetPagination() {
        oldestLoadedPostKey = null;
        hasMorePostsToLoad = true;
    }

    public static void loadFirstPage(Context context, int limit, Runnable onComplete) {
        resetPagination();
        loadPostsPage(context, limit, onComplete);
    }

    public static void loadNextPage(Context context, int limit, Runnable onComplete) {
        if (!hasMorePostsToLoad) {
            if (onComplete != null) onComplete.run();
            return;
        }
        loadPostsPage(context, limit, onComplete);
    }

    public static Post snapToPost(com.google.firebase.database.DataSnapshot snap) {
        if (snap == null) return null;
        try {
            Post p = snap.getValue(Post.class);
            if (p != null && p.getId() != 0) {
                return p;
            }
        } catch (Exception e) {
            android.util.Log.e("PostRepository", "snap.getValue(Post.class) failed for key " + snap.getKey() + ": " + e.getMessage());
        }

        try {
            int id = 0;
            Long idObj = snap.child("id").getValue(Long.class);
            if (idObj != null) {
                id = idObj.intValue();
            } else {
                try {
                    id = Integer.parseInt(snap.getKey());
                } catch (Exception ignored) {}
            }

            int userId = 0;
            Long uIdObj = snap.child("userId").getValue(Long.class);
            if (uIdObj == null) uIdObj = snap.child("user_id").getValue(Long.class);
            if (uIdObj != null) userId = uIdObj.intValue();

            String authorName = snap.child("authorName").getValue(String.class);
            if (authorName == null) authorName = snap.child("author_name").getValue(String.class);

            String authorProfileImage = snap.child("authorProfileImage").getValue(String.class);
            if (authorProfileImage == null) authorProfileImage = snap.child("author_profile_image").getValue(String.class);

            String content = snap.child("content").getValue(String.class);

            String imagePath = snap.child("imagePath").getValue(String.class);
            if (imagePath == null) imagePath = snap.child("image_path").getValue(String.class);

            String videoPath = snap.child("videoPath").getValue(String.class);
            if (videoPath == null) videoPath = snap.child("video_path").getValue(String.class);

            int likesCount = 0;
            Long likesObj = snap.child("likesCount").getValue(Long.class);
            if (likesObj != null) likesCount = likesObj.intValue();

            int commentsCount = 0;
            Long commentsObj = snap.child("commentsCount").getValue(Long.class);
            if (commentsObj != null) commentsCount = commentsObj.intValue();

            int sharesCount = 0;
            Long sharesObj = snap.child("sharesCount").getValue(Long.class);
            if (sharesObj != null) sharesCount = sharesObj.intValue();

            Boolean sensitiveObj = snap.child("isSensitive").getValue(Boolean.class);
            if (sensitiveObj == null) sensitiveObj = snap.child("sensitive").getValue(Boolean.class);
            boolean isSensitive = sensitiveObj != null && sensitiveObj;

            Boolean pinnedObj = snap.child("isPinned").getValue(Boolean.class);
            if (pinnedObj == null) pinnedObj = snap.child("pinned").getValue(Boolean.class);
            boolean isPinned = pinnedObj != null && pinnedObj;

            String createdAt = snap.child("createdAt").getValue(String.class);
            if (createdAt == null) createdAt = snap.child("created_at").getValue(String.class);

            Post post = new Post(id, userId, authorName, authorProfileImage, content, imagePath, videoPath, likesCount, false, createdAt);
            post.setCommentsCount(commentsCount);
            post.setSharesCount(sharesCount);
            post.setSensitive(isSensitive);
            post.setPinned(isPinned);

            Long targetIdObj = snap.child("targetUserId").getValue(Long.class);
            if (targetIdObj == null) targetIdObj = snap.child("target_user_id").getValue(Long.class);
            if (targetIdObj != null) post.setTargetUserId(targetIdObj.intValue());

            return post;
        } catch (Exception e) {
            android.util.Log.e("PostRepository", "Manual snapToPost failed: " + e.getMessage(), e);
            return null;
        }
    }

    public static void loadPostsPage(Context context, int limit, Runnable onComplete) {
        Context ctx = context != null ? context : contextStatic;
        com.google.firebase.database.DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("posts");

        com.google.firebase.database.Query query;
        if (oldestLoadedPostKey == null) {
            query = ref.orderByKey().limitToLast(limit);
        } else {
            query = ref.orderByKey().endBefore(oldestLoadedPostKey).limitToLast(limit);
        }

        query.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                List<Post> pagePosts = new ArrayList<>();
                String firstKeyInBatch = null;

                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    if (firstKeyInBatch == null) {
                        firstKeyInBatch = snap.getKey();
                    }
                    Post post = snapToPost(snap);
                    if (post != null) {
                        postsCache.put(post.getId(), post);
                        pagePosts.add(post);
                    }
                }

                if (firstKeyInBatch != null) {
                    oldestLoadedPostKey = firstKeyInBatch;
                }

                if (snapshot.getChildrenCount() < limit) {
                    hasMorePostsToLoad = false;
                }

                if (ctx != null && !pagePosts.isEmpty()) {
                    saveCachedFeedPosts(ctx, pagePosts);
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private static com.google.firebase.database.ValueEventListener livePostsListener = null;

    public static com.google.firebase.database.ValueEventListener listenToRealtimePosts(Context context, Runnable onPostsUpdated) {
        Context ctx = context != null ? context : contextStatic;
        com.google.firebase.database.DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("posts");

        if (livePostsListener != null) {
            ref.removeEventListener(livePostsListener);
        }

        livePostsListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                List<Post> allPosts = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot snap : snapshot.getChildren()) {
                    Post post = snapToPost(snap);
                    if (post != null) {
                        postsCache.put(post.getId(), post);
                        allPosts.add(post);
                    }
                }

                if (ctx != null && !allPosts.isEmpty()) {
                    saveCachedFeedPosts(ctx, allPosts);
                }

                if (onPostsUpdated != null) {
                    onPostsUpdated.run();
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                if (onPostsUpdated != null) {
                    onPostsUpdated.run();
                }
            }
        };

        ref.addValueEventListener(livePostsListener);

        try {
            com.google.firebase.database.DatabaseReference reactionsRef = FirebaseHelper.getInstance().getRealtimeDb().getReference("post_reactions");
            reactionsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    for (com.google.firebase.database.DataSnapshot postSnap : snapshot.getChildren()) {
                        String postIdStr = postSnap.getKey();
                        for (com.google.firebase.database.DataSnapshot userSnap : postSnap.getChildren()) {
                            String userIdStr = userSnap.getKey();
                            String reaction = userSnap.getValue(String.class);
                            if (postIdStr != null && userIdStr != null && reaction != null) {
                                String key = postIdStr + "_" + userIdStr;
                                userReactionsMap.put(key, reaction);
                            }
                        }
                    }
                    if (onPostsUpdated != null) {
                        onPostsUpdated.run();
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
        } catch (Exception ignored) {}

        return livePostsListener;
    }

    public static void loadAllPostsFromCloud(Runnable onComplete) {
        loadAllPostsFromCloud(contextStatic, onComplete);
    }

    public static void loadAllPostsFromCloud(Context context, Runnable onComplete) {
        Context ctx = context != null ? context : contextStatic;
        if (ctx != null) {
            List<Post> cached = getCachedFeedPosts(ctx);
            for (Post p : cached) {
                postsCache.put(p.getId(), p);
            }
        }
        listenToRealtimePosts(ctx, onComplete);
    }

    private static JSONObject postToJson(Post p) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", p.getId());
            obj.put("userId", p.getUserId());
            obj.put("authorName", p.getAuthorName());
            obj.put("authorProfileImage", p.getAuthorProfileImage());
            obj.put("content", p.getContent());
            obj.put("imagePath", p.getImagePath());
            obj.put("videoPath", p.getVideoPath());
            obj.put("likesCount", p.getLikesCount());
            obj.put("likedByCurrentUser", p.isLikedByCurrentUser());
            obj.put("createdAt", p.getCreatedAt());
            obj.put("isSensitive", p.isSensitive());
            obj.put("userReactionType", p.getUserReactionType());
            obj.put("sharesCount", p.getSharesCount());
            obj.put("commentsCount", p.getCommentsCount());
            obj.put("isSavedByCurrentUser", p.isSavedByCurrentUser());
            obj.put("isPinned", p.isPinned());
            return obj;
        } catch (Exception e) {
            return null;
        }
    }

    private static Post jsonToPost(JSONObject obj) {
        try {
            Post p = new Post(
                    obj.optInt("id"),
                    obj.optInt("userId"),
                    obj.optString("authorName", null),
                    obj.optString("authorProfileImage", null),
                    obj.optString("content", null),
                    obj.optString("imagePath", null),
                    obj.optString("videoPath", null),
                    obj.optInt("likesCount", 0),
                    obj.optBoolean("likedByCurrentUser", false),
                    obj.optString("createdAt", null)
            );
            p.setSensitive(obj.optBoolean("isSensitive", false));
            p.setUserReactionType(obj.optString("userReactionType", null));
            p.setSharesCount(obj.optInt("sharesCount", 0));
            p.setCommentsCount(obj.optInt("commentsCount", 0));
            p.setSavedByCurrentUser(obj.optBoolean("isSavedByCurrentUser", false));
            p.setPinned(obj.optBoolean("isPinned", false));
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Post> getAllPosts(int currentUserId) {
        if (postsCache.isEmpty() && contextStatic != null) {
            List<Post> cached = getCachedFeedPosts(contextStatic);
            for (Post p : cached) {
                postsCache.put(p.getId(), p);
            }
        }
        Map<Integer, Post> map = new LinkedHashMap<>();
        List<Post> list = new ArrayList<>(postsCache.values());
        for (Post post : list) {
            enrichPostAuthorData(post);
            if (currentUserId > 0) {
                String key = post.getId() + "_" + currentUserId;
                String reaction = userReactionsMap.get(key);
                if (reaction != null && !"UNLIKE".equalsIgnoreCase(reaction)) {
                    post.setLikedByCurrentUser(true);
                    post.setUserReactionType(reaction);
                } else if ("UNLIKE".equalsIgnoreCase(reaction)) {
                    post.setLikedByCurrentUser(false);
                    post.setUserReactionType(null);
                }
            }
            map.put(post.getId(), post);
        }
        List<Post> uniqueList = new ArrayList<>(map.values());
        Collections.sort(uniqueList, (p1, p2) -> {
            String t1 = p1.getCreatedAt() != null ? p1.getCreatedAt() : "";
            String t2 = p2.getCreatedAt() != null ? p2.getCreatedAt() : "";
            return -t1.compareTo(t2);
        });
        return uniqueList;
    }

    public List<Post> getPostsByUserId(int targetUserId, int currentUserId) {
        List<Post> result = new ArrayList<>();
        for (Post p : getAllPosts(currentUserId)) {
            if (p.getUserId() == targetUserId || p.getTargetUserId() == targetUserId) {
                result.add(p);
            }
        }
        return result;
    }

    public static int getLikesCountForPost(int postId) {
        int count = 0;
        String prefix = postId + "_";
        for (Map.Entry<String, String> entry : userReactionsMap.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String val = entry.getValue();
                if (val != null && !"UNLIKE".equalsIgnoreCase(val)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void enrichPostAuthorData(Post post) {
        if (post == null) return;
        User user = userRepository.getUserById(post.getUserId());
        if (user != null) {
            if (user.getName() != null && !user.getName().trim().isEmpty()) {
                post.setAuthorName(user.getName());
            }
            if (user.getProfileImage() != null && !user.getProfileImage().trim().isEmpty()) {
                post.setAuthorProfileImage(user.getProfileImage());
            }
        }
        int commentCount = CommentRepository.getCommentsCountForPost(post.getId());
        if (commentCount > post.getCommentsCount()) {
            post.setCommentsCount(commentCount);
        }
        int realLikesCount = getLikesCountForPost(post.getId());
        if (realLikesCount > post.getLikesCount()) {
            post.setLikesCount(realLikesCount);
        }
    }

    public static void incrementPostCommentsCount(int postId) {
        Post post = postsCache.get(postId);
        if (post != null) {
            post.setCommentsCount(post.getCommentsCount() + 1);
            if (isNetworkAvailable(contextStatic)) {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
            }
        }
    }

    public boolean updatePost(int postId, int userId, String newText, String imagePath) {
        Post post = postsCache.get(postId);
        if (post != null && post.getUserId() == userId) {
            post.setContent(newText);
            if (imagePath != null) post.setImagePath(imagePath);
            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));
            if (isNetworkAvailable(context)) {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
            }
            return true;
        }
        return false;
    }

    public boolean deletePost(int postId, int userId) {
        Post post = postsCache.get(postId);
        if (post != null && post.getUserId() == userId) {
            postsCache.remove(postId);
            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));
            if (isNetworkAvailable(context)) {
                FirebaseHelper.getInstance().deletePostFromCloud(postId);
            }
            return true;
        }
        return false;
    }

    public boolean toggleLikePost(int postId, int userId) {
        Post post = postsCache.get(postId);
        if (post != null) {
            String key = postId + "_" + userId;
            String currentReaction = userReactionsMap.get(key);
            boolean wasLiked = (currentReaction != null && !"UNLIKE".equalsIgnoreCase(currentReaction)) || post.isLikedByCurrentUser();
            boolean nowLiked = !wasLiked;

            post.setLikedByCurrentUser(nowLiked);
            post.setUserReactionType(nowLiked ? "LIKE" : null);
            userReactionsMap.put(key, nowLiked ? "LIKE" : "UNLIKE");

            int realCount = getLikesCountForPost(postId);
            post.setLikesCount(realCount);

            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));

            if (isNetworkAvailable(context)) {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
                FirebaseHelper.getInstance().syncPostReactionToRealtimeDb(postId, userId, nowLiked ? "LIKE" : "UNLIKE");
            }

            if (nowLiked && post.getUserId() != userId) {
                notificationRepository.addNotification(post.getUserId(), userId, "POST_LIKE", "POST", postId, getUserName(userId) + " a aimé votre publication.");
            }
            return nowLiked;
        }
        return false;
    }

    public void setPostReaction(int postId, int userId, String reactionType) {
        Post post = postsCache.get(postId);
        if (post != null) {
            String key = postId + "_" + userId;
            boolean isPositiveReaction = reactionType != null && !reactionType.trim().isEmpty() && !"UNLIKE".equalsIgnoreCase(reactionType);

            if (isPositiveReaction) {
                post.setLikedByCurrentUser(true);
                post.setUserReactionType(reactionType);
                userReactionsMap.put(key, reactionType);
                if (isNetworkAvailable(context)) {
                    FirebaseHelper.getInstance().syncPostReactionToRealtimeDb(postId, userId, reactionType);
                }
                if (post.getUserId() != userId) {
                    notificationRepository.addNotification(post.getUserId(), userId, "POST_REACTION", "POST", postId, getUserName(userId) + " a réagi à votre publication.");
                }
            } else {
                post.setLikedByCurrentUser(false);
                post.setUserReactionType(null);
                userReactionsMap.put(key, "UNLIKE");
                if (isNetworkAvailable(context)) {
                    FirebaseHelper.getInstance().syncPostReactionToRealtimeDb(postId, userId, "UNLIKE");
                }
            }

            int realCount = getLikesCountForPost(postId);
            post.setLikesCount(realCount);

            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));

            if (isNetworkAvailable(context)) {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
            }
        }
    }

    public void recordPostShare(int postId, int userId) {
        Post post = postsCache.get(postId);
        if (post != null) {
            post.setSharesCount(post.getSharesCount() + 1);
        }
    }

    public boolean toggleSavePost(int postId, int userId) {
        Post post = postsCache.get(postId);
        if (post != null) {
            boolean wasSaved = post.isSavedByCurrentUser();
            post.setSavedByCurrentUser(!wasSaved);
            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));
            if (isNetworkAvailable(context)) {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
            }
            return !wasSaved;
        }
        return false;
    }

    public List<Post> getSavedPosts(int currentUserId) {
        List<Post> saved = new ArrayList<>();
        for (Post p : getAllPosts(currentUserId)) {
            if (p.isSavedByCurrentUser()) {
                saved.add(p);
            }
        }
        return saved;
    }

    public boolean togglePinPost(int postId, int userId) {
        Post post = postsCache.get(postId);
        if (post != null && post.getUserId() == userId) {
            boolean wasPinned = post.isPinned();
            post.setPinned(!wasPinned);
            saveCachedFeedPosts(context, new ArrayList<>(postsCache.values()));
            if (isNetworkAvailable(context)) {
                FirebaseHelper.getInstance().syncPostToRealtimeDb(post);
            }
            return !wasPinned;
        }
        return false;
    }

    public boolean reportPost(int postId, int reporterId, String reason) {
        ReportItem item = new ReportItem();
        item.reportId = Math.abs((postId + "_" + reporterId + "_" + System.currentTimeMillis()).hashCode());
        item.postId = postId;
        item.reporterId = reporterId;
        item.reporterName = getUserName(reporterId);
        item.reason = reason != null ? reason : "Contenu inapproprié";
        item.createdAt = TimeUtils.getCurrentLocalDateTime();

        Post p = postsCache.get(postId);
        if (p != null) {
            item.postContent = p.getContent();
            item.imagePath = p.getImagePath();
            item.videoPath = p.getVideoPath();
        }

        reportsCache.add(item);
        return true;
    }

    public static class ReportItem {
        public int reportId;
        public int postId;
        public int reporterId;
        public String reporterName;
        public String postContent;
        public String reason;
        public String createdAt;
        public String imagePath;
        public String videoPath;
    }

    public List<ReportItem> getReports() {
        return new ArrayList<>(reportsCache);
    }

    public boolean deleteReport(int reportId) {
        return reportsCache.removeIf(item -> item.reportId == reportId);
    }

    private String getUserName(int userId) {
        User user = userRepository.getUserById(userId);
        return (user != null && user.getName() != null && !user.getName().trim().isEmpty()) ? user.getName() : "Utilisateur";
    }

    private String getUserProfileImage(int userId) {
        User user = userRepository.getUserById(userId);
        return user != null ? user.getProfileImage() : null;
    }

    public boolean isUserRestricted(int userId) {
        return false;
    }
}

