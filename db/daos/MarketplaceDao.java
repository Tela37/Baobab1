package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.MarketplaceEntity;

@Dao
public interface MarketplaceDao {

    @Query("SELECT * FROM marketplace ORDER BY id DESC")
    List<MarketplaceEntity> getAllItems();

    @Query("SELECT * FROM marketplace WHERE category = :category ORDER BY id DESC")
    List<MarketplaceEntity> getItemsByCategory(String category);

    @Query("SELECT * FROM marketplace WHERE id = :id LIMIT 1")
    MarketplaceEntity getItemById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItem(MarketplaceEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItems(List<MarketplaceEntity> items);

    @Query("DELETE FROM marketplace WHERE id = :id")
    void deleteItemById(int id);

    @Query("DELETE FROM marketplace")
    void deleteAll();
}
