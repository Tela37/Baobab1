package td.teladoum.baobab.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;

/**
 * Vue détaillée d'une annonce Marketplace (Acheter, Vendre, Services, Location, Emploi).
 * Propose des actions directes : Appeler le vendeur, WhatsApp et Chat In-App.
 */
public class MarketplaceDetailActivity extends AppCompatActivity {

    private String sellerPhone;
    private int sellerId;
    private String sellerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace_detail);

        Toolbar toolbar = findViewById(R.id.toolbarMarketplaceDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        double price = intent.getDoubleExtra("price", 0);
        String category = intent.getStringExtra("category");
        String city = intent.getStringExtra("city");
        sellerName = intent.getStringExtra("seller_name");
        sellerPhone = intent.getStringExtra("seller_phone");
        sellerId = intent.getIntExtra("seller_id", -1);
        String imagePath = intent.getStringExtra("image_path");

        ImageView imgPhoto = findViewById(R.id.imgDetailPhoto);
        TextView tvCategoryBadge = findViewById(R.id.tvDetailCategoryBadge);
        TextView tvCityBadge = findViewById(R.id.tvDetailCityBadge);
        TextView tvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvSeller = findViewById(R.id.tvDetailSeller);
        TextView tvDescription = findViewById(R.id.tvDetailDescription);

        if (tvTitle != null) tvTitle.setText(title != null ? title : "Sans titre");
        if (tvCategoryBadge != null) tvCategoryBadge.setText(category != null ? category : "ACHAT");
        if (tvCityBadge != null) tvCityBadge.setText(city != null ? city : "N'Djamena");
        if (tvPrice != null) {
            if (price <= 0) {
                tvPrice.setText("Sur devis / Gratuit");
            } else {
                long priceLong = (long) price;
                tvPrice.setText(String.format(java.util.Locale.FRANCE, "%,d FCFA", priceLong).replace(',', ' '));
            }
        }
        if (tvSeller != null) tvSeller.setText("👤 Vendeur / Contact : " + (sellerName != null ? sellerName : "Inconnu"));
        if (tvDescription != null) tvDescription.setText(description != null && !description.trim().isEmpty() ? description : "Aucune description fournie.");

        if (imgPhoto != null) {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                Glide.with(this)
                        .load(imagePath)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(imgPhoto);
            } else {
                imgPhoto.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        Button btnCall = findViewById(R.id.btnCallSeller);
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> callSeller());
        }

        Button btnWhatsapp = findViewById(R.id.btnWhatsappSeller);
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> openWhatsapp());
        }

        Button btnChat = findViewById(R.id.btnChatSeller);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> openInAppChat());
        }
    }

    private void callSeller() {
        if (sellerPhone == null || sellerPhone.trim().isEmpty()) {
            Toasty.warning(this, "Aucun numéro de téléphone disponible", Toasty.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + sellerPhone.replaceAll("[^0-9+]", "")));
            startActivity(intent);
        } catch (Exception e) {
            Toasty.error(this, "Impossible de passer l'appel", Toasty.LENGTH_SHORT).show();
        }
    }

    private void openWhatsapp() {
        if (sellerPhone == null || sellerPhone.trim().isEmpty()) {
            Toasty.warning(this, "Aucun numéro WhatsApp disponible", Toasty.LENGTH_SHORT).show();
            return;
        }
        try {
            String digits = sellerPhone.replaceAll("[^0-9]", "");
            String msg = "Bonjour " + (sellerName != null ? sellerName : "") + ", je vous contacte depuis la Marketplace BaobabTD.";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + digits + "&text=" + Uri.encode(msg)));
            startActivity(intent);
        } catch (Exception e) {
            Toasty.error(this, "Impossible d'ouvrir WhatsApp", Toasty.LENGTH_SHORT).show();
        }
    }

    private void openInAppChat() {
        if (sellerId <= 0) {
            Toasty.warning(this, "Vendeur introuvable pour le chat in-app", Toasty.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiver_id", sellerId);
        intent.putExtra("receiver_name", sellerName != null ? sellerName : "Vendeur");
        startActivity(intent);
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
