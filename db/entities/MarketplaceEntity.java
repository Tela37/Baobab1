package td.teladoum.baobab.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.models.MarketplaceItem;

@Entity(tableName = "marketplace")
public class MarketplaceEntity {

    @PrimaryKey
    public int id;
    public int userId;
    public String sellerName;
    public String sellerPhone;
    public String title;
    public String description;
    public double price;
    public String category;
    public String city;
    public String imagePath;
    public String createdAt;
    public boolean isAvailable;

    public MarketplaceEntity() {}

    public static MarketplaceEntity fromModel(MarketplaceItem item) {
        if (item == null) return null;
        MarketplaceEntity entity = new MarketplaceEntity();
        entity.id = item.getId();
        entity.userId = item.getUserId();
        entity.sellerName = item.getSellerName();
        entity.sellerPhone = item.getSellerPhone();
        entity.title = item.getTitle();
        entity.description = item.getDescription();
        entity.price = item.getPrice();
        entity.category = item.getCategory();
        entity.city = item.getCity();
        entity.imagePath = item.getImagePath();
        entity.createdAt = item.getCreatedAt();
        entity.isAvailable = item.isAvailable();
        return entity;
    }

    public MarketplaceItem toModel() {
        return new MarketplaceItem(id, userId, sellerName, sellerPhone, title, description, price, category, city, imagePath, createdAt, isAvailable);
    }
}
