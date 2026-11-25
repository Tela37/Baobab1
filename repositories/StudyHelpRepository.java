package td.teladoum.baobab.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.StudyQuestion;
import td.teladoum.baobab.models.StudyReply;
import td.teladoum.baobab.utils.SessionManager;
import td.teladoum.baobab.utils.TimeUtils;

/**
 * Repository Cloud-Native pour la gestion des questions d'entraide scolaire et des réponses.
 */
public class StudyHelpRepository {

    private static final Map<Integer, StudyQuestion> questionCache = new ConcurrentHashMap<>();
    private static final Map<Integer, List<StudyReply>> replyCache = new ConcurrentHashMap<>();
    private final Context context;

    public StudyHelpRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public List<StudyQuestion> getAllQuestions() {
        List<StudyQuestion> list = new ArrayList<>(questionCache.values());
        for (StudyQuestion q : list) {
            List<StudyReply> reps = replyCache.get(q.getId());
            if (reps != null) {
                q.setRepliesCount(reps.size());
            }
        }
        list.sort((q1, q2) -> Integer.compare(q2.getId(), q1.getId()));
        return list;
    }

    public void publishQuestion(int userId, String authorName, String authorAvatar, String title, String content, String category, String imagePath, Runnable onSuccess, Runnable onFailure) {
        int id = Math.abs((userId + "_" + System.currentTimeMillis() + "_" + Math.random()).hashCode());
        String createdAt = TimeUtils.getCurrentLocalDateTime();

        StudyQuestion question = new StudyQuestion(id, userId, authorName, authorAvatar, title, content, category, imagePath, createdAt);
        questionCache.put(id, question);

        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_questions").child(String.valueOf(id));
            ref.setValue(question).addOnSuccessListener(aVoid -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onSuccess != null) onSuccess.run();
            });
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void syncQuestionsFromCloud(Runnable onComplete) {
        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_questions");
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    questionCache.clear();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        try {
                            StudyQuestion q = snap.getValue(StudyQuestion.class);
                            if (q != null) {
                                questionCache.put(q.getId(), q);
                                syncRepliesForCount(q.getId(), onComplete);
                            }
                        } catch (Exception ignored) {}
                    }
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (onComplete != null) onComplete.run();
                }
            });
        } catch (Exception e) {
            if (onComplete != null) onComplete.run();
        }
    }

    private void syncRepliesForCount(int questionId, Runnable onComplete) {
        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId));
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<StudyReply> replies = new ArrayList<>();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        try {
                            StudyReply r = snap.getValue(StudyReply.class);
                            if (r != null && !r.isReported()) {
                                replies.add(r);
                            }
                        } catch (Exception ignored) {}
                    }
                    replyCache.put(questionId, replies);
                    StudyQuestion q = questionCache.get(questionId);
                    if (q != null) {
                        q.setRepliesCount(replies.size());
                    }
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } catch (Exception ignored) {}
    }

    public List<StudyReply> getReplies(int questionId) {
        List<StudyReply> list = replyCache.get(questionId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public void publishReply(int questionId, int userId, String authorName, String authorAvatar, String content, String imagePath, Runnable onSuccess) {
        int id = Math.abs((questionId + "_" + userId + "_" + System.currentTimeMillis() + "_" + Math.random()).hashCode());
        String createdAt = TimeUtils.getCurrentLocalDateTime();

        StudyReply reply = new StudyReply(id, questionId, userId, authorName, authorAvatar, content, createdAt, imagePath);
        List<StudyReply> list = replyCache.computeIfAbsent(questionId, k -> Collections.synchronizedList(new ArrayList<>()));
        list.add(reply);

        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId)).child(String.valueOf(id));
            ref.setValue(reply).addOnSuccessListener(aVoid -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onSuccess != null) onSuccess.run();
            });
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void editReply(int questionId, int replyId, String newContent, Runnable onSuccess) {
        List<StudyReply> list = replyCache.get(questionId);
        if (list != null) {
            synchronized (list) {
                for (StudyReply r : list) {
                    if (r.getId() == replyId) {
                        r.setContent(newContent);
                        break;
                    }
                }
            }
        }
        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId)).child(String.valueOf(replyId));
            ref.child("content").setValue(newContent).addOnSuccessListener(aVoid -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onSuccess != null) onSuccess.run();
            });
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void reportReply(int questionId, int replyId, Runnable onSuccess) {
        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId)).child(String.valueOf(replyId));
            ref.child("reported").setValue(true).addOnSuccessListener(aVoid -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onSuccess != null) onSuccess.run();
            });
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void syncRepliesFromCloud(int questionId, Runnable onComplete) {
        int currentUserId = -1;
        try {
            if (context != null) {
                SessionManager sm = new SessionManager(context);
                currentUserId = sm.getUserId();
            }
        } catch (Exception ignored) {}

        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId));
            final int finalUserId = currentUserId;
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<StudyReply> replies = Collections.synchronizedList(new ArrayList<>());
                    List<com.google.android.gms.tasks.Task<Void>> likeTasks = new ArrayList<>();
                    List<StudyReply> cachedReplies = replyCache.get(questionId);

                    for (DataSnapshot snap : snapshot.getChildren()) {
                        try {
                            StudyReply r = snap.getValue(StudyReply.class);
                            if (r != null && !r.isReported()) {
                                // First check cache to preserve local like state immediately
                                if (cachedReplies != null) {
                                    for (StudyReply cached : cachedReplies) {
                                        if (cached.getId() == r.getId()) {
                                            r.setLikedByCurrentUser(cached.isLikedByCurrentUser());
                                            break;
                                        }
                                    }
                                }

                                if (finalUserId != -1) {
                                    com.google.android.gms.tasks.Task<Void> task = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_reply_likes").child(String.valueOf(r.getId())).child(String.valueOf(finalUserId))
                                            .get().addOnSuccessListener(ds -> {
                                                Boolean liked = ds.getValue(Boolean.class);
                                                r.setLikedByCurrentUser(liked != null && liked);
                                            }).continueWith(t -> null);
                                    likeTasks.add(task);

                                    com.google.android.gms.tasks.Task<Void> reactionTask = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_reply_reactions").child(String.valueOf(r.getId())).child(String.valueOf(finalUserId))
                                            .get().addOnSuccessListener(ds -> {
                                                String reaction = ds.getValue(String.class);
                                                if (reaction != null && !reaction.isEmpty()) {
                                                    r.setUserReactionType(reaction);
                                                    r.setLikedByCurrentUser(true);
                                                }
                                            }).continueWith(t -> null);
                                    likeTasks.add(reactionTask);
                                }
                                replies.add(r);
                            }
                        } catch (Exception ignored) {}
                    }

                    Runnable updateUiAndCache = () -> {
                        // Sort stably: reverse chronologically by createdAt, then by id descending
                        replies.sort((r1, r2) -> {
                            String t1 = r1 != null && r1.getCreatedAt() != null ? r1.getCreatedAt() : "";
                            String t2 = r2 != null && r2.getCreatedAt() != null ? r2.getCreatedAt() : "";
                            int timeCompare = t2.compareTo(t1);
                            if (timeCompare != 0) {
                                return timeCompare;
                            }
                            return Integer.compare(r2 != null ? r2.getId() : 0, r1 != null ? r1.getId() : 0);
                        });

                        replyCache.put(questionId, replies);
                        StudyQuestion q = questionCache.get(questionId);
                        if (q != null) {
                            q.setRepliesCount(replies.size());
                        }
                        if (onComplete != null) onComplete.run();
                    };

                    if (!likeTasks.isEmpty() && finalUserId != -1) {
                        com.google.android.gms.tasks.Tasks.whenAllComplete(likeTasks).addOnCompleteListener(task -> updateUiAndCache.run());
                    } else {
                        updateUiAndCache.run();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (onComplete != null) onComplete.run();
                }
            });
        } catch (Exception e) {
            if (onComplete != null) onComplete.run();
        }
    }

    public void deleteQuestion(int questionId, Runnable onSuccess) {
        questionCache.remove(questionId);
        replyCache.remove(questionId);
        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_questions").child(String.valueOf(questionId));
            ref.removeValue().addOnSuccessListener(aVoid -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onSuccess != null) onSuccess.run();
            });
            DatabaseReference repliesRef = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId));
            repliesRef.removeValue();
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void setReplyReaction(int questionId, int replyId, int userId, String reactionType, Runnable onSuccess) {
        List<StudyReply> list = replyCache.get(questionId);
        if (list != null) {
            synchronized (list) {
                for (StudyReply r : list) {
                    if (r.getId() == replyId) {
                        r.setUserReactionType(reactionType);
                        r.setLikedByCurrentUser(true);
                        r.setLikesCount(Math.max(1, r.getLikesCount() + 1));
                        break;
                    }
                }
            }
        }
        try {
            FirebaseHelper.getInstance().getRealtimeDb().getReference("study_reply_reactions")
                    .child(String.valueOf(replyId))
                    .child(String.valueOf(userId))
                    .setValue(reactionType);

            FirebaseHelper.getInstance().getRealtimeDb().getReference("study_reply_likes")
                    .child(String.valueOf(replyId))
                    .child(String.valueOf(userId))
                    .setValue(true);

            StudyReply targetReply = null;
            if (list != null) {
                for (StudyReply r : list) {
                    if (r.getId() == replyId) {
                        targetReply = r;
                        break;
                    }
                }
            }
            if (targetReply != null) {
                FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId)).child(String.valueOf(replyId))
                        .child("likesCount").setValue(targetReply.getLikesCount());
            }

            if (onSuccess != null) onSuccess.run();
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public boolean toggleLikeReply(int questionId, int replyId, int currentUserId, Runnable onSuccess) {
        List<StudyReply> list = replyCache.get(questionId);
        boolean nowLiked = false;
        if (list != null) {
            synchronized (list) {
                for (StudyReply r : list) {
                    if (r.getId() == replyId) {
                        boolean wasLiked = r.isLikedByCurrentUser();
                        nowLiked = !wasLiked;
                        r.setLikedByCurrentUser(nowLiked);
                        r.setLikesCount(Math.max(0, r.getLikesCount() + (nowLiked ? 1 : -1)));
                        break;
                    }
                }
            }
        }

        try {
            StudyReply targetReply = null;
            if (list != null) {
                for (StudyReply r : list) {
                    if (r.getId() == replyId) {
                        targetReply = r;
                        break;
                    }
                }
            }
            if (targetReply != null) {
                DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("study_help_replies").child(String.valueOf(questionId)).child(String.valueOf(replyId));
                ref.child("likesCount").setValue(targetReply.getLikesCount());
            }

            if (currentUserId != -1) {
                FirebaseHelper.getInstance().getRealtimeDb().getReference("study_reply_likes").child(String.valueOf(replyId)).child(String.valueOf(currentUserId))
                        .setValue(nowLiked ? true : null)
                        .addOnSuccessListener(aVoid -> {
                            if (onSuccess != null) onSuccess.run();
                        })
                        .addOnFailureListener(e -> {
                            if (onSuccess != null) onSuccess.run();
                        });
            } else {
                if (onSuccess != null) onSuccess.run();
            }
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
        return nowLiked;
    }
}
