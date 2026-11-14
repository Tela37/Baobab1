package td.teladoum.baobab.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.helpers.GeminiHelper;

/**
 * Activité Hub de l'Assistant IA Google Gemini.
 * Permet de générer des textes d'annonces de vente, des publications attrayantes ou des messages.
 */
public class GeminiAssistantActivity extends AppCompatActivity {

    private EditText etAiPrompt;
    private ProgressBar progressLoading;
    private View cardResult;
    private TextView tvAiResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini_assistant);

        Toolbar toolbar = findViewById(R.id.toolbarGeminiAssistant);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etAiPrompt = findViewById(R.id.etAiPrompt);
        progressLoading = findViewById(R.id.progressAiLoading);
        cardResult = findViewById(R.id.cardResult);
        tvAiResult = findViewById(R.id.tvAiResult);

        Button btnQuickSale = findViewById(R.id.btnQuickSale);
        Button btnQuickPost = findViewById(R.id.btnQuickPost);
        Button btnQuickService = findViewById(R.id.btnQuickService);

        if (btnQuickSale != null) {
            btnQuickSale.setOnClickListener(v -> etAiPrompt.setText("Rédige une annonce de vente très attrayante pour un téléphone Samsung en parfait état à N'Djamena."));
        }
        if (btnQuickPost != null) {
            btnQuickPost.setOnClickListener(v -> etAiPrompt.setText("Rédige une publication engageante pour partager mes salutations et mes projets avec mes amis sur BaobabTD."));
        }
        if (btnQuickService != null) {
            btnQuickService.setOnClickListener(v -> etAiPrompt.setText("Rédige une offre de service professionnelle en électricité et dépannage à domicile à Moundou."));
        }

        Button btnGenerate = findViewById(R.id.btnGenerateAi);
        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> generateAiContent());
        }

        Button btnCopy = findViewById(R.id.btnCopyResult);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> copyResultToClipboard());
        }

        String initialPrompt = getIntent().getStringExtra("initial_prompt");
        if (initialPrompt != null && !initialPrompt.trim().isEmpty()) {
            etAiPrompt.setText(initialPrompt);
            generateAiContent();
        }
    }

    private void generateAiContent() {
        String prompt = etAiPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toasty.warning(this, "Veuillez saisir votre demande ou sujet", Toasty.LENGTH_SHORT).show();
            return;
        }

        if (progressLoading != null) progressLoading.setVisibility(View.VISIBLE);
        if (cardResult != null) cardResult.setVisibility(View.GONE);

        GeminiHelper.generateWritingAssistant(prompt, new GeminiHelper.AiCallback() {
            @Override
            public void onSuccess(String result) {
                if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                if (cardResult != null) cardResult.setVisibility(View.VISIBLE);
                if (tvAiResult != null) tvAiResult.setText(result);
            }

            @Override
            public void onError(String error) {
                if (progressLoading != null) progressLoading.setVisibility(View.GONE);
                Toasty.error(GeminiAssistantActivity.this, "Erreur IA : " + error, Toasty.LENGTH_SHORT).show();
            }
        });
    }

    private void copyResultToClipboard() {
        String text = tvAiResult != null ? tvAiResult.getText().toString() : "";
        if (text.isEmpty()) return;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("IA Gemini", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toasty.success(this, "Texte copié dans le presse-papier ! 📋", Toasty.LENGTH_SHORT).show();
        }
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
