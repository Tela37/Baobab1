package td.teladoum.baobab.data.model;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Modèle Produit / Vente dans data.model.
 */
@IgnoreExtraProperties
public class MarketplaceItem {

    private int id;
    private int userId;
    private String sellerName;
    private String sellerPhone;
    private String title;
    private String description;
    private double price;
    private String category;
    private String city;
    private String imagePath;
    private String createdAt;
    private boolean isAvailable = true;

    public MarketplaceItem() {
    }

    public MarketplaceItem(int id, int userId, String sellerName, String sellerPhone, String title, String description, double price, String category, String city, String imagePath, String createdAt, boolean isAvailable) {
        this.id = id;
        this.userId = userId;
        this.sellerName = sellerName;
        this.sellerPhone = sellerPhone;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.city = city;
        this.imagePath = imagePath;
        this.createdAt = createdAt;
        this.isAvailable = isAvailable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerPhone() {
        return sellerPhone;
    }

    public void setSellerPhone(String sellerPhone) {
        this.sellerPhone = sellerPhone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
