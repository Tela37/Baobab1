package td.teladoum.baobab.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
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

    private td.teladoum.baobab.viewmodels.MarketplaceViewModel viewModel;
    private MarketplaceAdapter adapter;
    private final List<MarketplaceItem> fullItemList = new ArrayList<>();
    private final List<MarketplaceItem> displayedItemList = new ArrayList<>();

    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentCategory = "TOUT";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);

        // 2. Ajuster les paddings pour ne pas recouvrir le contenu sous les barres système
        android.view.View mainContainer = findViewById(getResources().getIdentifier("main_container", "id", getPackageName()));
        if (mainContainer == null) {
            mainContainer = findViewById(android.R.id.content);
        }
        if (mainContainer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                androidx.core.graphics.Insets navBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
                v.setPadding(0, statusBarInsets.top, 0, navBarInsets.bottom);
                return insets;
            });
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            // true = icônes sombres (pour fond clair), false = icônes blanches (pour fond sombre)
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        td.teladoum.baobab.helpers.BottomNavigationHelper.setupBottomNavigation(this, td.teladoum.baobab.helpers.BottomNavigationHelper.TAB_MARKETPLACE);

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(td.teladoum.baobab.viewmodels.MarketplaceViewModel.class);
        setupViewModelObservers();

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

        adapter = new MarketplaceAdapter(new ArrayList<>(), item -> {
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
            intent.putExtra("views_count", item.getViewsCount());
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

            fabPublish.setOnTouchListener(new View.OnTouchListener() {
                private float dX, dY;
                private float startX, startY;
                private long startTime;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            dX = v.getX() - event.getRawX();
                            dY = v.getY() - event.getRawY();
                            startX = event.getRawX();
                            startY = event.getRawY();
                            startTime = System.currentTimeMillis();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() + dX;
                            float newY = event.getRawY() + dY;

                            View parent = (View) v.getParent();
                            if (parent != null) {
                                float maxX = parent.getWidth() - v.getWidth();
                                float maxY = parent.getHeight() - v.getHeight();
                                newX = Math.max(0, Math.min(newX, maxX));
                                newY = Math.max(0, Math.min(newY, maxY));
                            }

                            v.animate()
                                    .x(newX)
                                    .y(newY)
                                    .setDuration(0)
                                    .start();
                            return true;

                        case MotionEvent.ACTION_UP:
                            float deltaX = Math.abs(event.getRawX() - startX);
                            float deltaY = Math.abs(event.getRawY() - startY);
                            if (deltaX < 15 && deltaY < 15 && (System.currentTimeMillis() - startTime) < 250) {
                                v.performClick();
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        loadLocalItems();
        loadItemsFromCloud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocalItems();
        td.teladoum.baobab.helpers.BottomNavigationHelper.setupBottomNavigation(this, td.teladoum.baobab.helpers.BottomNavigationHelper.TAB_MARKETPLACE);
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
                loadLocalItems();
                applyFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupViewModelObservers() {
        if (viewModel == null) return;

        viewModel.getItemsLiveData().observe(this, items -> {
            fullItemList.clear();
            if (items != null) fullItemList.addAll(items);
            applyFilters();
        });

        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(Boolean.TRUE.equals(isLoading));
            }
        });
    }

    private void loadLocalItems() {
        viewModel.loadMarketplaceItems(currentCategory);
    }

    private void loadItemsFromCloud() {
        viewModel.loadMarketplaceItems(currentCategory);
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

        adapter.updateItems(filtered);
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
