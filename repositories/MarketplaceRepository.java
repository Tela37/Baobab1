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

import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.db.entities.MarketplaceEntity;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.models.MarketplaceItem;
import td.teladoum.baobab.utils.TimeUtils;

/**
 * Repository pour la Marketplace (Acheter, Vendre, Services, Location, Emploi).
 * Synchronise les annonces en temps réel via Firebase Realtime Database (`marketplace_items`)
 * et assure la persistance hors-ligne locale avec Room Database.
 */
public class MarketplaceRepository {

    private static final Map<Integer, MarketplaceItem> itemCache = new ConcurrentHashMap<>();
    private final Context context;

    public MarketplaceRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public List<MarketplaceItem> getAllItems() {
        if (itemCache.isEmpty() && context != null) {
            loadFromRoom();
        }
        List<MarketplaceItem> list = new ArrayList<>(itemCache.values());
        list.sort((i1, i2) -> Integer.compare(i2.getId(), i1.getId()));
        return list;
    }

    public List<MarketplaceItem> getItemsByCategory(String category) {
        List<MarketplaceItem> all = getAllItems();
        if (category == null || category.trim().isEmpty() || "TOUT".equalsIgnoreCase(category) || "ALL".equalsIgnoreCase(category)) {
            return all;
        }
        List<MarketplaceItem> filtered = new ArrayList<>();
        for (MarketplaceItem item : all) {
            if (category.equalsIgnoreCase(item.getCategory())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public void publishItem(int userId, String sellerName, String sellerPhone, String title, String description, double price, String category, String city, String imagePath, Runnable onSuccess, Runnable onFailure) {
        int id = Math.abs((userId + "_" + System.currentTimeMillis() + "_" + Math.random()).hashCode());
        String createdAt = TimeUtils.getCurrentLocalDateTime();

        MarketplaceItem item = new MarketplaceItem(id, userId, sellerName, sellerPhone, title, description, price, category, city, imagePath, createdAt, true);
        itemCache.put(id, item);

        saveToRoom(item);

        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("marketplace_items").child(String.valueOf(id));
            ref.setValue(item).addOnSuccessListener(aVoid -> {
                if (onSuccess != null) onSuccess.run();
            }).addOnFailureListener(e -> {
                if (onSuccess != null) onSuccess.run(); // Posté localement
            });
        } catch (Exception e) {
            if (onSuccess != null) onSuccess.run();
        }
    }

    public void syncFromCloud(Runnable onComplete) {
        try {
            DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("marketplace_items");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<MarketplaceItem> cloudItems = new ArrayList<>();
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        try {
                            MarketplaceItem item = snap.getValue(MarketplaceItem.class);
                            if (item != null) {
                                itemCache.put(item.getId(), item);
                                cloudItems.add(item);
                            }
                        } catch (Exception ignored) {}
                    }
                    saveAllToRoom(cloudItems);
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

    private void loadFromRoom() {
        if (context == null) return;
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null) {
                List<MarketplaceEntity> entities = db.marketplaceDao().getAllItems();
                if (entities != null) {
                    for (MarketplaceEntity entity : entities) {
                        MarketplaceItem item = entity.toModel();
                        if (item != null) {
                            itemCache.put(item.getId(), item);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveToRoom(MarketplaceItem item) {
        if (context == null || item == null) return;
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    MarketplaceEntity entity = MarketplaceEntity.fromModel(item);
                    if (entity != null) db.marketplaceDao().insertItem(entity);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void saveAllToRoom(List<MarketplaceItem> items) {
        if (context == null || items == null) return;
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    List<MarketplaceEntity> entities = new ArrayList<>();
                    for (MarketplaceItem item : items) {
                        MarketplaceEntity entity = MarketplaceEntity.fromModel(item);
                        if (entity != null) entities.add(entity);
                    }
                    db.marketplaceDao().insertItems(entities);
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
