package td.teladoum.baobab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.MarketplaceAdapter;
import td.teladoum.baobab.models.MarketplaceItem;
import td.teladoum.baobab.repositories.MarketplaceRepository;

/**
 * Hub principal de la Marketplace (Acheter, Vendre, Services, Location, Emploi).
 * Permet de filtrer par catégorie, chercher par mot-clé et publier une annonce.
 */
public class MarketplaceActivity extends AppCompatActivity {

    private MarketplaceRepository repository;
    private MarketplaceAdapter adapter;
    private final List<MarketplaceItem> fullItemList = new ArrayList<>();
    private final List<MarketplaceItem> displayedItemList = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentCategory = "TOUT";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);

        repository = new MarketplaceRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbarMarketplace);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TabLayout tabLayout = findViewById(R.id.tabCategories);
        setupTabs(tabLayout);

        RecyclerView rvMarketplace = findViewById(R.id.rvMarketplace);
        rvMarketplace.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new MarketplaceAdapter(displayedItemList, item -> {
            Intent intent = new Intent(MarketplaceActivity.this, MarketplaceDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("price", item.getPrice());
            intent.putExtra("category", item.getCategory());
            intent.putExtra("city", item.getCity());
            intent.putExtra("seller_name", item.getSellerName());
            intent.putExtra("seller_phone", item.getSellerPhone());
            intent.putExtra("seller_id", item.getUserId());
            intent.putExtra("image_path", item.getImagePath());
            startActivity(intent);
        });
        rvMarketplace.setAdapter(adapter);

        swipeRefreshLayout = findViewById(R.id.swipeMarketplace);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadItemsFromCloud);
        }

        SearchView searchView = findViewById(R.id.searchMarketplace);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentQuery = query;
                    applyFilters();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentQuery = newText;
                    applyFilters();
                    return true;
                }
            });
        }

        FloatingActionButton fabPublish = findViewById(R.id.fabPublishItem);
        if (fabPublish != null) {
            fabPublish.setOnClickListener(v -> {
                Intent intent = new Intent(MarketplaceActivity.this, PublishMarketplaceActivity.class);
                startActivity(intent);
            });
        }

        loadLocalItems();
        loadItemsFromCloud();
    }

    private void setupTabs(TabLayout tabLayout) {
        if (tabLayout == null) return;
        tabLayout.addTab(tabLayout.newTab().setText("Tout"));
        tabLayout.addTab(tabLayout.newTab().setText("Acheter"));
        tabLayout.addTab(tabLayout.newTab().setText("Services"));
        tabLayout.addTab(tabLayout.newTab().setText("Location"));
        tabLayout.addTab(tabLayout.newTab().setText("Emploi"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 1: currentCategory = "ACHAT"; break;
                    case 2: currentCategory = "SERVICES"; break;
                    case 3: currentCategory = "LOCATION"; break;
                    case 4: currentCategory = "EMPLOI"; break;
                    default: currentCategory = "TOUT"; break;
                }
                applyFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLocalItems() {
        List<MarketplaceItem> localItems = repository.getAllItems();
        fullItemList.clear();
        fullItemList.addAll(localItems);
        applyFilters();
    }

    private void loadItemsFromCloud() {
        repository.syncFromCloud(() -> {
            loadLocalItems();
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void applyFilters() {
        List<MarketplaceItem> filtered = new ArrayList<>();
        String q = currentQuery.toLowerCase().trim();

        for (MarketplaceItem item : fullItemList) {
            boolean matchCategory = "TOUT".equalsIgnoreCase(currentCategory) || currentCategory.equalsIgnoreCase(item.getCategory());
            boolean matchQuery = q.isEmpty()
                    || (item.getTitle() != null && item.getTitle().toLowerCase().contains(q))
                    || (item.getDescription() != null && item.getDescription().toLowerCase().contains(q))
                    || (item.getCity() != null && item.getCity().toLowerCase().contains(q));

            if (matchCategory && matchQuery) {
                filtered.add(item);
            }
        }

        displayedItemList.clear();
        displayedItemList.addAll(filtered);
        adapter.updateItems(displayedItemList);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.color.background_dark || item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
