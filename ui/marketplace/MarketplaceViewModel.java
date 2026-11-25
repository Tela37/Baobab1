package td.teladoum.baobab.ui.marketplace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import td.teladoum.baobab.models.MarketplaceItem;
import td.teladoum.baobab.repositories.MarketplaceRepository;

/**
 * ViewModel pour le Marché/Marketplace dans le package ui.marketplace.
 */
public class MarketplaceViewModel extends AndroidViewModel {

    private final MarketplaceRepository marketplaceRepository;

    private final MutableLiveData<List<MarketplaceItem>> itemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    public MarketplaceViewModel(@NonNull Application application) {
        super(application);
        this.marketplaceRepository = new MarketplaceRepository(application);
    }

    public LiveData<List<MarketplaceItem>> getItemsLiveData() {
        return itemsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public void loadItems() {
        isLoadingLiveData.setValue(true);
        List<MarketplaceItem> items = marketplaceRepository.getAllItems();
        itemsLiveData.setValue(items != null ? items : new ArrayList<>());
        isLoadingLiveData.setValue(false);

        marketplaceRepository.loadItemsFromCloud(() -> {
            List<MarketplaceItem> updated = marketplaceRepository.getAllItems();
            itemsLiveData.postValue(updated != null ? updated : new ArrayList<>());
        });
    }

    public void filterByCategory(String category) {
        List<MarketplaceItem> filtered = marketplaceRepository.getItemsByCategory(category);
        itemsLiveData.setValue(filtered != null ? filtered : new ArrayList<>());
    }

    public void searchItems(String query) {
        List<MarketplaceItem> searched = marketplaceRepository.searchItems(query);
        itemsLiveData.setValue(searched != null ? searched : new ArrayList<>());
    }
}
