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

import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

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
@Singleton
public class MarketplaceRepository {

    private static final Map<Integer, MarketplaceItem> itemCache = new ConcurrentHashMap<>();
    private final Context context;

    @Inject
    public MarketplaceRepository(@ApplicationContext Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        loadFromRoom();
    }

    public List<MarketplaceItem> getAllItems() {
        loadFromRoom();
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
            String itemCat = item.getCategory() != null ? item.getCategory() : "";
            if (category.equalsIgnoreCase(itemCat) ||
                ("IMMOBILIER".equalsIgnoreCase(category) && "LOCATION".equalsIgnoreCase(itemCat))) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public List<MarketplaceItem> searchItems(String query) {
        List<MarketplaceItem> all = getAllItems();
        if (query == null || query.trim().isEmpty()) return all;
        String lower = query.trim().toLowerCase();
        List<MarketplaceItem> filtered = new ArrayList<>();
        for (MarketplaceItem item : all) {
            if ((item.getTitle() != null && item.getTitle().toLowerCase().contains(lower)) ||
                (item.getDescription() != null && item.getDescription().toLowerCase().contains(lower)) ||
                (item.getCity() != null && item.getCity().toLowerCase().contains(lower))) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public void loadItemsFromCloud(Runnable onComplete) {
        syncFromCloud(onComplete);
    }

    public void publishItem(int userId, String sellerName, String sellerPhone, String title, String description, double price, String category, String city, String imagePath, Runnable onSuccess, Runnable onFailure) {
        int id = Math.abs((userId + "_" + System.currentTimeMillis() + "_" + Math.random()).hashCode());
        String createdAt = TimeUtils.getCurrentLocalDateTime();

        MarketplaceItem item = new MarketplaceItem(id, userId, sellerName, sellerPhone, title, description, price, category, city, imagePath, createdAt, true);
        itemCache.put(id, item);

        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null) {
                MarketplaceEntity entity = MarketplaceEntity.fromModel(item);
                if (entity != null) db.marketplaceDao().insertItem(entity);
            }
        } catch (Exception ignored) {}

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

    private static final List<Runnable> syncCallbacks = Collections.synchronizedList(new ArrayList<>());
    private static boolean isSyncInitialized = false;

    public void startRealtimeSync(Runnable onChanged) {
        if (onChanged != null && !syncCallbacks.contains(onChanged)) {
            syncCallbacks.add(onChanged);
        }

        if (onChanged != null) {
            onChanged.run();
        }

        if (!isSyncInitialized) {
            isSyncInitialized = true;
            try {
                DatabaseReference ref = FirebaseHelper.getInstance().getRealtimeDb().getReference("marketplace_items");
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MarketplaceItem> cloudItems = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            try {
                                Integer id = snap.child("id").getValue(Integer.class);
                                if (id == null) {
                                    Long idLong = snap.child("id").getValue(Long.class);
                                    if (idLong != null) id = idLong.intValue();
                                }
                                if (id == null) id = snap.getKey() != null ? Math.abs(snap.getKey().hashCode()) : 0;

                                Integer userId = snap.child("userId").getValue(Integer.class);
                                if (userId == null) {
                                    Long userIdLong = snap.child("userId").getValue(Long.class);
                                    if (userIdLong != null) userId = userIdLong.intValue();
                                }

                                String title = snap.child("title").getValue(String.class);
                                String description = snap.child("description").getValue(String.class);

                                Double price = snap.child("price").getValue(Double.class);
                                if (price == null) {
                                    Long priceLong = snap.child("price").getValue(Long.class);
                                    if (priceLong != null) price = priceLong.doubleValue();
                                    else {
                                        String priceStr = snap.child("price").getValue(String.class);
                                        if (priceStr != null) {
                                            try { price = Double.parseDouble(priceStr); } catch (Exception e) { price = 0.0; }
                                        }
                                    }
                                }

                                String category = snap.child("category").getValue(String.class);
                                String city = snap.child("city").getValue(String.class);
                                String imagePath = snap.child("imagePath").getValue(String.class);
                                if (imagePath == null) imagePath = snap.child("image_path").getValue(String.class);

                                String sellerName = snap.child("sellerName").getValue(String.class);
                                if (sellerName == null) sellerName = snap.child("seller_name").getValue(String.class);

                                String sellerPhone = snap.child("sellerPhone").getValue(String.class);
                                if (sellerPhone == null) sellerPhone = snap.child("seller_phone").getValue(String.class);

                                String createdAt = snap.child("createdAt").getValue(String.class);
                                if (createdAt == null) createdAt = snap.child("created_at").getValue(String.class);

                                Boolean isAvailable = snap.child("available").getValue(Boolean.class);
                                if (isAvailable == null) isAvailable = true;

                                Integer viewsCount = snap.child("viewsCount").getValue(Integer.class);
                                if (viewsCount == null) {
                                    Long viewsLong = snap.child("viewsCount").getValue(Long.class);
                                    if (viewsLong != null) viewsCount = viewsLong.intValue();
                                    else viewsCount = 0;
                                }

                                MarketplaceItem item = new MarketplaceItem(
                                        id,
                                        userId != null ? userId : 0,
                                        sellerName != null ? sellerName : "Vendeur",
                                        sellerPhone != null ? sellerPhone : "",
                                        title != null ? title : "",
                                        description != null ? description : "",
                                        price != null ? price : 0.0,
                                        category != null ? category : "ACHAT",
                                        city != null ? city : "N'Djamena",
                                        imagePath,
                                        createdAt != null ? createdAt : "",
                                        isAvailable
                                );
                                item.setViewsCount(viewsCount);

                                itemCache.put(item.getId(), item);
                                cloudItems.add(item);
                            } catch (Exception e) {
                                try {
                                    MarketplaceItem item = snap.getValue(MarketplaceItem.class);
                                    if (item != null) {
                                        itemCache.put(item.getId(), item);
                                        cloudItems.add(item);
                                    }
                                } catch (Exception ignored2) {}
                            }
                        }
                        saveAllToRoom(cloudItems);
                        synchronized (syncCallbacks) {
                            for (Runnable cb : syncCallbacks) {
                                if (cb != null) cb.run();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        synchronized (syncCallbacks) {
                            for (Runnable cb : syncCallbacks) {
                                if (cb != null) cb.run();
                            }
                        }
                    }
                });
            } catch (Exception e) {
                synchronized (syncCallbacks) {
                    for (Runnable cb : syncCallbacks) {
                        if (cb != null) cb.run();
                    }
                }
            }
        }
    }

    public void syncFromCloud(Runnable onComplete) {
        startRealtimeSync(onComplete);
    }

    public void deleteItem(int itemId) {
        itemCache.remove(itemId);
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null) {
                db.marketplaceDao().deleteItemById(itemId);
            }
        } catch (Exception ignored) {}
        try {
            FirebaseHelper.getInstance().getRealtimeDb().getReference("marketplace_items").child(String.valueOf(itemId)).removeValue();
        } catch (Exception ignored) {}
    }

    public void incrementViews(int itemId) {
        MarketplaceItem item = itemCache.get(itemId);
        if (item != null) {
            item.setViewsCount(item.getViewsCount() + 1);
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                if (db != null) {
                    MarketplaceEntity entity = MarketplaceEntity.fromModel(item);
                    if (entity != null) db.marketplaceDao().insertItem(entity);
                }
            } catch (Exception ignored) {}
            try {
                FirebaseHelper.getInstance().getRealtimeDb().getReference("marketplace_items").child(String.valueOf(itemId)).child("viewsCount").setValue(item.getViewsCount());
            } catch (Exception ignored) {}
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
