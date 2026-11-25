package td.teladoum.baobab.ui.learning;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import td.teladoum.baobab.R;
import td.teladoum.baobab.adapters.LivreAdapter;
import td.teladoum.baobab.adapters.StudyQuestionAdapter;
import td.teladoum.baobab.data.model.Livre;
import td.teladoum.baobab.data.repository.LearningRepository;
import td.teladoum.baobab.models.StudyQuestion;
import td.teladoum.baobab.models.User;
import td.teladoum.baobab.repositories.StudyHelpRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.ImageUtils;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité de la Bibliothèque Numérique (Par Niveaux et Matières) avec module d'entraide scolaire (Q&A).
 */
public class LibraryActivity extends AppCompatActivity {

    private LearningViewModel viewModel;
    private Spinner spinnerNiveau, spinnerMatiere;
    private RecyclerView rvLivres, rvStudyQuestions;
    private View layoutBooksTab, layoutStudyTab;
    private FloatingActionButton fabAskQuestion;

    private StudyHelpRepository studyHelpRepository;
    private SessionManager sessionManager;
    private UserRepository userRepository;

    private StudyQuestionAdapter studyAdapter;
    private final List<StudyQuestion> studyQuestionList = new ArrayList<>();

    private LivreAdapter livreAdapter;
    private final List<Livre> livreList = new ArrayList<>();

    private String savedFilePath;
    private TextView tvAttachedFileName;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    savedFilePath = ImageUtils.saveImageToInternalStorage(this, uri);
                    if (savedFilePath == null) savedFilePath = uri.toString();
                    if (tvAttachedFileName != null) {
                        tvAttachedFileName.setText("📎 Fichier attaché : " + uri.getLastPathSegment());
                        tvAttachedFileName.setVisibility(View.VISIBLE);
                    }
                    Toasty.success(this, "Fichier/Image attaché avec succès !", Toasty.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> filePickerLauncherForPdf =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Intent intent = new Intent(LibraryActivity.this, PdfViewerActivity.class);
                    intent.putExtra("pdf_path", uri.toString());
                    intent.putExtra("pdf_title", uri.getLastPathSegment());
                    startActivity(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

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

        Toolbar toolbar = findViewById(R.id.toolbarLibrary);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bibliothèque Numérique 📖");
        }

        viewModel = new ViewModelProvider(this).get(LearningViewModel.class);
        studyHelpRepository = new StudyHelpRepository(this);
        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);

        spinnerNiveau = findViewById(R.id.spinnerNiveau);
        spinnerMatiere = findViewById(R.id.spinnerMatiere);
        rvLivres = findViewById(R.id.rvLivres);
        rvLivres.setLayoutManager(new LinearLayoutManager(this));

        layoutBooksTab = findViewById(R.id.layoutBooksTab);
        layoutStudyTab = findViewById(R.id.layoutStudyTab);
        rvStudyQuestions = findViewById(R.id.rvStudyQuestions);
        rvStudyQuestions.setLayoutManager(new LinearLayoutManager(this));
        fabAskQuestion = findViewById(R.id.fabAskQuestion);

        studyAdapter = new StudyQuestionAdapter(studyQuestionList, question -> {
            Intent intent = new Intent(LibraryActivity.this, StudyQuestionDetailActivity.class);
            intent.putExtra("question_id", question.getId());
            intent.putExtra("author_name", question.getAuthorName());
            intent.putExtra("author_avatar", question.getAuthorProfileImage());
            intent.putExtra("title", question.getTitle());
            intent.putExtra("content", question.getContent());
            intent.putExtra("category", question.getCategory());
            intent.putExtra("image_path", question.getImagePath());
            intent.putExtra("created_at", question.getCreatedAt());
            intent.putExtra("author_user_id", question.getUserId());
            startActivity(intent);
        });
        rvStudyQuestions.setAdapter(studyAdapter);

        TabLayout tabLibrary = findViewById(R.id.tabLibrary);
        setupTabs(tabLibrary);
        setupSpinners();

        fabAskQuestion.setOnClickListener(v -> showPublishQuestionDialog());

        Button btnAddBookAdmin = findViewById(R.id.btnAddBookAdmin);
        int userId = sessionManager.getUserId();
        User currentUser = userRepository.getUserById(userId);
        if (currentUser != null && (currentUser.isAdmin() || userRepository.isSuperAdmin(userId))) {
            if (btnAddBookAdmin != null) {
                btnAddBookAdmin.setVisibility(View.VISIBLE);
                btnAddBookAdmin.setOnClickListener(v -> showAddBookDialog());
            }
        } else {
            if (btnAddBookAdmin != null) {
                btnAddBookAdmin.setVisibility(View.GONE);
            }
        }

        livreAdapter = new LivreAdapter(livreList, new LivreAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Livre livre) {
                if (!livre.isDownloadable()) {
                    Toasty.info(LibraryActivity.this, "Ce document est protégé (lecture en ligne uniquement, téléchargement désactivé) 🔒", Toasty.LENGTH_LONG).show();
                }
                Intent intent = new Intent(LibraryActivity.this, PdfViewerActivity.class);
                intent.putExtra("pdf_path", livre.getUrlPdf());
                intent.putExtra("pdf_title", livre.getTitre());
                intent.putExtra("is_downloadable", livre.isDownloadable());
                startActivity(intent);
            }

            @Override
            public void onFavoriteClick(Livre livre) {
                LearningRepository learningRepo = new LearningRepository(LibraryActivity.this);
                learningRepo.toggleFavorite(livre.getId());
                livre.setFavorite(!livre.isFavorite());
                livreAdapter.notifyDataSetChanged();
                Toasty.success(LibraryActivity.this, livre.isFavorite() ? "Ajouté aux favoris ⭐" : "Retiré des favoris", Toasty.LENGTH_SHORT).show();
            }

            @Override
            public void onBookLongClick(Livre livre) {
                int userId = sessionManager.getUserId();
                User currentUser = userRepository.getUserById(userId);
                if (currentUser != null && (currentUser.isAdmin() || userRepository.isSuperAdmin(userId))) {
                    showEditBookDialog(livre);
                } else {
                    Toasty.info(LibraryActivity.this, "Seul un administrateur peut modifier les paramètres du document.", Toasty.LENGTH_SHORT).show();
                }
            }
        });
        rvLivres.setAdapter(livreAdapter);

        viewModel.getLivresLiveData().observe(this, livres -> {
            livreList.clear();
            if (livres != null) {
                livreList.addAll(livres);
            }
            if (livreAdapter != null) {
                livreAdapter.updateData(livreList);
            }
            View cardNotice = findViewById(R.id.cardNotice);
            if (cardNotice != null) {
                cardNotice.setVisibility(livreList.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.loadLivres("TOUT", "TOUT");

        EditText etSearchQuestions = findViewById(R.id.etSearchQuestions);
        if (etSearchQuestions != null) {
            etSearchQuestions.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterQuestions(s.toString());
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        studyHelpRepository.syncQuestionsFromCloud(() -> {
            List<StudyQuestion> questions = studyHelpRepository.getAllQuestions();
            studyAdapter.updateData(questions);
        });
    }

    private void setupTabs(TabLayout tabLibrary) {
        if (tabLibrary == null) return;
        tabLibrary.addTab(tabLibrary.newTab().setText("📚 Livres & Cours"));
        tabLibrary.addTab(tabLibrary.newTab().setText("💬 Entraide & Q&A"));

        tabLibrary.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutBooksTab.setVisibility(View.VISIBLE);
                    layoutStudyTab.setVisibility(View.GONE);
                    fabAskQuestion.setVisibility(View.GONE);
                } else {
                    layoutBooksTab.setVisibility(View.GONE);
                    layoutStudyTab.setVisibility(View.VISIBLE);
                    fabAskQuestion.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSpinners() {
        String[] niveaux = {"TOUT", "Primaire", "Collège", "Lycée", "Université", "Formation professionnelle"};
        String[] matieres = {"TOUT", "Mathématiques", "Français", "Physique", "SVT", "Histoire", "Géographie", "Anglais", "Informatique", "Économie"};

        spinnerNiveau.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, niveaux));
        spinnerMatiere.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, matieres));

        android.widget.AdapterView.OnItemSelectedListener spinnerListener = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String niveau = spinnerNiveau.getSelectedItem() != null ? spinnerNiveau.getSelectedItem().toString() : "TOUT";
                String matiere = spinnerMatiere.getSelectedItem() != null ? spinnerMatiere.getSelectedItem().toString() : "TOUT";
                viewModel.loadLivres(niveau, matiere);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };

        spinnerNiveau.setOnItemSelectedListener(spinnerListener);
        spinnerMatiere.setOnItemSelectedListener(spinnerListener);
    }

    private void showPublishQuestionDialog() {
        savedFilePath = null;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Titre du sujet / de l'exercice...");

        EditText etContent = new EditText(this);
        etContent.setHint("Description détaillée de votre question ou problème...");
        etContent.setMinLines(3);

        Spinner spinnerCat = new Spinner(this);
        String[] categories = new String[]{"Mathématiques", "Français", "Physique", "SVT", "Histoire", "Géographie", "Anglais", "Informatique", "Économie"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCat.setAdapter(catAdapter);

        Button btnAttach = new Button(this);
        btnAttach.setText("📎 Joindre une image, vidéo ou PDF");
        btnAttach.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32")));
        btnAttach.setTextColor(android.graphics.Color.WHITE);
        btnAttach.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        tvAttachedFileName = new TextView(this);
        tvAttachedFileName.setText("Aucun fichier attaché");
        tvAttachedFileName.setTextColor(android.graphics.Color.GRAY);
        tvAttachedFileName.setVisibility(View.GONE);

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);

        layout.addView(etTitle);
        layout.addView(etContent);
        layout.addView(spinnerCat);
        layout.addView(btnAttach);
        layout.addView(tvAttachedFileName);
        layout.addView(progressBar);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Poser une question / Demander de l'aide 💬")
                .setView(layout)
                .setPositiveButton("Publier", null)
                .setNegativeButton("Annuler", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            String category = spinnerCat.getSelectedItem() != null ? spinnerCat.getSelectedItem().toString() : "Mathématiques";

            if (title.isEmpty() || content.isEmpty()) {
                Toasty.warning(this, "Veuillez remplir le titre et la description", Toasty.LENGTH_SHORT).show();
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

            boolean isOnline = td.teladoum.baobab.utils.NetworkUtils.isNetworkAvailable(this);

            int userId = sessionManager.getUserId();
            User currentUser = userRepository.getUserById(userId);
            String name = (currentUser != null && currentUser.getName() != null) ? currentUser.getName() : sessionManager.getName();
            String avatar = (currentUser != null && currentUser.getProfileImage() != null) ? currentUser.getProfileImage() : sessionManager.getProfileImage();

            studyHelpRepository.publishQuestion(userId, name, avatar, title, content, category, savedFilePath, () -> {
                if (isOnline) {
                    Toasty.success(LibraryActivity.this, "Sujet publié avec succès ! 🚀", Toasty.LENGTH_SHORT).show();
                } else {
                    Toasty.info(LibraryActivity.this, "Mode hors-ligne : Sujet enregistré en mémoire et sera envoyé dès le retour du réseau 📡", Toasty.LENGTH_LONG).show();
                }
                dialog.dismiss();
                studyHelpRepository.syncQuestionsFromCloud(() -> studyAdapter.updateData(studyHelpRepository.getAllQuestions()));
            }, () -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toasty.error(LibraryActivity.this, "Erreur lors de la publication. Veuillez réessayer.", Toasty.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, 1001, 0, "🤖 Assistant IA Exercice");
        menu.add(0, 1002, 1, "📖 Ouvrir un PDF...");

        int userId = sessionManager.getUserId();
        User currentUser = userRepository.getUserById(userId);
        if (currentUser != null && (currentUser.isAdmin() || userRepository.isSuperAdmin(userId))) {
            menu.add(0, 1003, 2, "➕ Ajouter un livre / lien (Admin)");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1001) {
            td.teladoum.baobab.helpers.GeminiHelper.showExerciseSolverDialog(this);
            return true;
        } else if (item.getItemId() == 1002) {
            filePickerLauncherForPdf.launch("application/pdf");
            return true;
        } else if (item.getItemId() == 1003) {
            showAddBookDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddBookDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Titre du livre / cours / annale...");

        EditText etAuthor = new EditText(this);
        etAuthor.setHint("Auteur / Éditeur (ex: Prof. Kouloum)...");

        Spinner spinnerNiveauBook = new Spinner(this);
        String[] niveaux = {"Primaire", "Collège", "Lycée", "Université", "Formation professionnelle", "Concours"};
        spinnerNiveauBook.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, niveaux));

        Spinner spinnerMatiereBook = new Spinner(this);
        String[] matieres = {"Mathématiques", "Français", "Physique", "SVT", "Histoire", "Géographie", "Anglais", "Informatique", "Économie"};
        spinnerMatiereBook.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, matieres));

        Spinner spinnerTypeBook = new Spinner(this);
        String[] types = {"LIVRE", "COURS", "ANNALE", "CORRIGE"};
        spinnerTypeBook.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));

        EditText etUrlPdf = new EditText(this);
        etUrlPdf.setHint("Lien PDF / URL de la ressource (https://...)");

        EditText etCouverture = new EditText(this);
        etCouverture.setHint("Lien image de couverture (optionnel)");

        android.widget.CheckBox cbDownloadable = new android.widget.CheckBox(this);
        cbDownloadable.setText("Document téléchargeable par les utilisateurs");
        cbDownloadable.setChecked(true);

        layout.addView(etTitle);
        layout.addView(etAuthor);
        layout.addView(spinnerNiveauBook);
        layout.addView(spinnerMatiereBook);
        layout.addView(spinnerTypeBook);
        layout.addView(etUrlPdf);
        layout.addView(etCouverture);
        layout.addView(cbDownloadable);

        new AlertDialog.Builder(this)
                .setTitle("Ajouter un livre / lien (Admin) 📚")
                .setView(layout)
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String author = etAuthor.getText().toString().trim();
                    String niveau = spinnerNiveauBook.getSelectedItem() != null ? spinnerNiveauBook.getSelectedItem().toString() : "Lycée";
                    String matiere = spinnerMatiereBook.getSelectedItem() != null ? spinnerMatiereBook.getSelectedItem().toString() : "Mathématiques";
                    String type = spinnerTypeBook.getSelectedItem() != null ? spinnerTypeBook.getSelectedItem().toString() : "LIVRE";
                    String urlPdf = etUrlPdf.getText().toString().trim();
                    String couverture = etCouverture.getText().toString().trim();
                    boolean isDownloadable = cbDownloadable.isChecked();

                    if (title.isEmpty() || urlPdf.isEmpty()) {
                        Toasty.warning(this, "Veuillez remplir au moins le titre et le lien PDF", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    String bookId = "book_" + System.currentTimeMillis();
                    if (couverture.isEmpty()) {
                        couverture = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e";
                    }

                    Livre newLivre = new Livre(bookId, title, author.isEmpty() ? "Administration" : author, niveau, matiere, type, urlPdf, couverture, "");
                    newLivre.setDownloadable(isDownloadable);
                    LearningRepository learningRepo = new LearningRepository(this);
                    learningRepo.addLivre(newLivre, () -> {
                        Toasty.success(LibraryActivity.this, "Livre / lien ajouté avec succès ! 🚀", Toasty.LENGTH_SHORT).show();
                        viewModel.loadLivres("TOUT", "TOUT");
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showEditBookDialog(Livre livre) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Titre du livre / cours / annale...");
        etTitle.setText(livre.getTitre());

        EditText etAuthor = new EditText(this);
        etAuthor.setHint("Auteur / Éditeur...");
        etAuthor.setText(livre.getAuteur());

        android.widget.CheckBox cbDownloadable = new android.widget.CheckBox(this);
        cbDownloadable.setText("Document téléchargeable par les utilisateurs");
        cbDownloadable.setChecked(livre.isDownloadable());

        layout.addView(etTitle);
        layout.addView(etAuthor);
        layout.addView(cbDownloadable);

        new AlertDialog.Builder(this)
                .setTitle("Modifier le document (Admin) ⚙️")
                .setView(layout)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String author = etAuthor.getText().toString().trim();
                    boolean isDownloadable = cbDownloadable.isChecked();

                    if (title.isEmpty()) {
                        Toasty.warning(this, "Le titre ne peut pas être vide", Toasty.LENGTH_SHORT).show();
                        return;
                    }

                    livre.setTitre(title);
                    if (!author.isEmpty()) {
                        livre.setAuteur(author);
                    }
                    livre.setDownloadable(isDownloadable);

                    LearningRepository learningRepo = new LearningRepository(this);
                    learningRepo.addLivre(livre, () -> {
                        Toasty.success(LibraryActivity.this, "Document mis à jour ! (Téléchargeable : " + (isDownloadable ? "Oui 📥" : "Non 🔒") + ")", Toasty.LENGTH_SHORT).show();
                        viewModel.loadLivres("TOUT", "TOUT");
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void filterQuestions(String query) {
        List<StudyQuestion> all = studyHelpRepository.getAllQuestions();
        if (query == null || query.trim().isEmpty()) {
            studyAdapter.updateData(all);
            return;
        }
        String lower = query.toLowerCase().trim();
        List<StudyQuestion> filtered = new ArrayList<>();
        for (StudyQuestion q : all) {
            if ((q.getTitle() != null && q.getTitle().toLowerCase().contains(lower)) ||
                    (q.getContent() != null && q.getContent().toLowerCase().contains(lower)) ||
                    (q.getCategory() != null && q.getCategory().toLowerCase().contains(lower)) ||
                    (q.getAuthorName() != null && q.getAuthorName().toLowerCase().contains(lower))) {
                filtered.add(q);
            }
        }
        studyAdapter.updateData(filtered);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
