package td.teladoum.baobab.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.Institution;
import td.teladoum.baobab.models.InstitutionEvent;
import td.teladoum.baobab.models.InstitutionNews;
import td.teladoum.baobab.models.InstitutionResult;

@Singleton
public class InstitutionRepository {

    private final Map<String, Institution> institutionCache = new ConcurrentHashMap<>();
    private final Map<String, List<InstitutionNews>> newsCache = new ConcurrentHashMap<>();
    private final Map<String, List<InstitutionResult>> resultsCache = new ConcurrentHashMap<>();
    private final Map<String, List<InstitutionEvent>> eventsCache = new ConcurrentHashMap<>();

    @Inject
    public InstitutionRepository(@ApplicationContext Context context) {
        fetchAllInstitutions(() -> {});
    }

    public void fetchAllInstitutions(Runnable onComplete) {
        DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("institutions");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                institutionCache.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Institution inst = ds.getValue(Institution.class);
                    if (inst != null) {
                        if (inst.getId() == null) inst.setId(ds.getKey());
                        institutionCache.put(inst.getId(), inst);
                        fetchInstitutionContent(inst.getId());
                    }
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void fetchInstitutionContent(String institutionId) {
        DatabaseReference db = FirebaseHelper.getInstance().getRealtimeDb().getReference();

        // News
        db.child("institution_news").child(institutionId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<InstitutionNews> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    InstitutionNews n = ds.getValue(InstitutionNews.class);
                    if (n != null) {
                        if (n.getId() == null) n.setId(ds.getKey());
                        list.add(n);
                    }
                }
                list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                newsCache.put(institutionId, list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Results
        db.child("institution_results").child(institutionId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<InstitutionResult> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    InstitutionResult r = ds.getValue(InstitutionResult.class);
                    if (r != null) {
                        if (r.getId() == null) r.setId(ds.getKey());
                        list.add(r);
                    }
                }
                list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                resultsCache.put(institutionId, list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Events
        db.child("institution_events").child(institutionId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<InstitutionEvent> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    InstitutionEvent e = ds.getValue(InstitutionEvent.class);
                    if (e != null) {
                        if (e.getId() == null) e.setId(ds.getKey());
                        list.add(e);
                    }
                }
                list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                eventsCache.put(institutionId, list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public List<Institution> getAllInstitutions() {
        List<Institution> list = new ArrayList<>(institutionCache.values());
        list.sort((i1, i2) -> i2.getName().compareToIgnoreCase(i1.getName()));
        return list;
    }

    public List<InstitutionNews> getNews(String institutionId) {
        return newsCache.getOrDefault(institutionId, new ArrayList<>());
    }

    public List<InstitutionResult> getResults(String institutionId) {
        return resultsCache.getOrDefault(institutionId, new ArrayList<>());
    }

    public List<InstitutionEvent> getEvents(String institutionId) {
        return eventsCache.getOrDefault(institutionId, new ArrayList<>());
    }

    public void createInstitution(Institution institution, Runnable onSuccess, Runnable onFailure) {
        DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("institutions").push();
        String generatedId = ref.getKey();
        if (generatedId == null) generatedId = String.valueOf(System.currentTimeMillis());
        institution.setId(generatedId);
        institution.setCreatedAt(System.currentTimeMillis());

        final String finalId = generatedId;
        ref.setValue(institution).addOnSuccessListener(aVoid -> {
            institutionCache.put(finalId, institution);
            if (onSuccess != null) onSuccess.run();
        }).addOnFailureListener(e -> {
            if (onFailure != null) onFailure.run();
        });
    }

    public void addNews(String institutionId, String title, String content, String imageUrl, Runnable onSuccess, Runnable onFailure) {
        DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("institution_news").child(institutionId).push();
        String id = ref.getKey();
        InstitutionNews news = new InstitutionNews(id, institutionId, title, content, imageUrl, System.currentTimeMillis());
        ref.setValue(news).addOnSuccessListener(v -> {
            if (onSuccess != null) onSuccess.run();
        }).addOnFailureListener(e -> {
            if (onFailure != null) onFailure.run();
        });
    }

    public void addResult(String institutionId, String title, String details, String documentUrl, Runnable onSuccess, Runnable onFailure) {
        DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("institution_results").child(institutionId).push();
        String id = ref.getKey();
        InstitutionResult result = new InstitutionResult(id, institutionId, title, details, documentUrl, System.currentTimeMillis());
        ref.setValue(result).addOnSuccessListener(v -> {
            if (onSuccess != null) onSuccess.run();
        }).addOnFailureListener(e -> {
            if (onFailure != null) onFailure.run();
        });
    }

    public void addEvent(String institutionId, String title, String description, String date, String location, String imageUrl, Runnable onSuccess, Runnable onFailure) {
        DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("institution_events").child(institutionId).push();
        String id = ref.getKey();
        InstitutionEvent event = new InstitutionEvent(id, institutionId, title, description, date, location, imageUrl, System.currentTimeMillis());
        ref.setValue(event).addOnSuccessListener(v -> {
            if (onSuccess != null) onSuccess.run();
        }).addOnFailureListener(e -> {
            if (onFailure != null) onFailure.run();
        });
    }
}
