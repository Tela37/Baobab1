package td.teladoum.baobab.ui.learning;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;

/**
 * Activité de lecture PDF intégrée utilisant l'API native PdfRenderer d'Android.
 */
public class PdfViewerActivity extends AppCompatActivity {

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private RecyclerView rvPdfPages;
    private TextView tvPageInfo;
    private PdfPageAdapter pageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

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

        Toolbar toolbar = findViewById(R.id.toolbarPdf);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lecteur PDF 📖");
        }

        rvPdfPages = findViewById(R.id.rvPdfPages);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        rvPdfPages.setLayoutManager(new LinearLayoutManager(this));

        String pdfUrlOrPath = getIntent().getStringExtra("pdf_path");
        String title = getIntent().getStringExtra("pdf_title");
        if (title != null && !title.isEmpty() && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        openRenderer(pdfUrlOrPath);
    }

    private void openRenderer(String path) {
        try {
            File file;
            if (path != null && path.startsWith("content://")) {
                file = File.createTempFile("temp_book", ".pdf", getCacheDir());
                try (InputStream rawInputStream = getContentResolver().openInputStream(Uri.parse(path));
                     BufferedInputStream inputStream = new BufferedInputStream(rawInputStream);
                     BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                }
            } else if (path != null && new File(path).exists()) {
                file = new File(path);
            } else {
                // Fichier exemple par défaut ou message si chemin vide
                file = File.createTempFile("sample_book", ".pdf", getCacheDir());
                // Créer un PDF vide ou copier un asset si besoin
                Toasty.info(this, "Ouverture du lecteur PDF", Toasty.LENGTH_SHORT).show();
            }

            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            if (parcelFileDescriptor != null) {
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                pageAdapter = new PdfPageAdapter();
                rvPdfPages.setAdapter(pageAdapter);

                tvPageInfo.setText("Total pages : " + pdfRenderer.getPageCount());
            }
        } catch (Exception e) {
            Toasty.error(this, "Erreur d'ouverture du PDF : " + e.getMessage(), Toasty.LENGTH_LONG).show();
            finish();
        }
    }

    private class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            try {
                PdfRenderer.Page page = pdfRenderer.openPage(position);
                int width = getResources().getDisplayMetrics().widthPixels - 32;
                int height = (int) (width * ((float) page.getHeight() / page.getWidth()));
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                holder.imgPdfPage.setImageBitmap(bitmap);
                page.close();
            } catch (Exception e) {
                // Ignored
            }
        }

        @Override
        public int getItemCount() {
            return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
        }

        public class PageViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPdfPage;

            public PageViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPdfPage = itemView.findViewById(R.id.imgPdfPage);
            }
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (parcelFileDescriptor != null) parcelFileDescriptor.close();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
