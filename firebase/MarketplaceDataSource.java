package td.teladoum.baobab.firebase;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import td.teladoum.baobab.models.MarketplaceItem;

/**
 * DataSource distante dédiée au Marketplace (Articles, Produits, Ventes) dans Firebase Realtime Database.
 */
public class MarketplaceDataSource {

    private final FirebaseDatabase realtimeDb;

    public MarketplaceDataSource() {
        this.realtimeDb = FirebaseHelper.getInstance().getRealtimeDb();
    }

    public void syncMarketplaceItemToRealtimeDb(MarketplaceItem item) {
        if (item == null) return;
        realtimeDb.getReference("marketplace_items").child(String.valueOf(item.getId())).setValue(item);
    }

    public void deleteMarketplaceItemFromCloud(int itemId) {
        realtimeDb.getReference("marketplace_items").child(String.valueOf(itemId)).removeValue();
    }

    public void listenToMarketplaceItems(ValueEventListener listener) {
        if (listener == null) return;
        realtimeDb.getReference("marketplace_items").addValueEventListener(listener);
    }
}
