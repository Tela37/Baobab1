package td.teladoum.baobab.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import td.teladoum.baobab.models.MarketplaceItem;
import td.teladoum.baobab.repositories.MarketplaceRepository;

/**
 * ViewModel pour le Marché / Marketplace (MarketplaceActivity) suivant l'architecture MVVM avec Hilt DI.
 */
@HiltViewModel
public class MarketplaceViewModel extends ViewModel {

    private final MarketplaceRepository marketplaceRepository;

    private final MutableLiveData<List<MarketplaceItem>> itemsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    @Inject
    public MarketplaceViewModel(MarketplaceRepository marketplaceRepository) {
        this.marketplaceRepository = marketplaceRepository;
    }

    public LiveData<List<MarketplaceItem>> getItemsLiveData() {
        return itemsLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public void loadMarketplaceItems(String category) {
        List<MarketplaceItem> localList = marketplaceRepository.getItemsByCategory(category);
        itemsLiveData.setValue(localList != null ? localList : new ArrayList<>());

        isLoadingLiveData.setValue(true);
        marketplaceRepository.startRealtimeSync(() -> {
            List<MarketplaceItem> list = marketplaceRepository.getItemsByCategory(category);
            itemsLiveData.postValue(list != null ? list : new ArrayList<>());
            isLoadingLiveData.postValue(false);
        });
    }

    public void publishItem(int userId, String sellerName, String sellerPhone, String title, String description, double price, String category, String city, String imagePath, Runnable onSuccess, Runnable onFailure) {
        marketplaceRepository.publishItem(userId, sellerName, sellerPhone, title, description, price, category, city, imagePath, onSuccess, onFailure);
    }
}
