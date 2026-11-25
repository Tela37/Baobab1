package td.teladoum.baobab.ui.learning;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import td.teladoum.baobab.R;
import td.teladoum.baobab.helpers.BottomNavigationHelper;
import td.teladoum.baobab.utils.SessionManager;

/**
 * Activité principale de Baobab Learning (Bibliothèque, Annales, Quiz, Tableau de bord).
 */
public class LearningMainActivity extends AppCompatActivity {

    private LearningViewModel viewModel;
    private SessionManager sessionManager;
    private TextView tvDocsRead, tvCoursesDown, tvStudyTime, tvQuizzesPassed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_main);

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

        Toolbar toolbar = findViewById(R.id.toolbarLearning);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Baobab Learning 📖");
        }

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(LearningViewModel.class);

        tvDocsRead = findViewById(R.id.tvDocsRead);
        tvCoursesDown = findViewById(R.id.tvCoursesDown);
        tvStudyTime = findViewById(R.id.tvStudyTime);
        tvQuizzesPassed = findViewById(R.id.tvQuizzesPassed);

        Button btnLibrary = findViewById(R.id.btnOpenLibrary);
        Button btnAnnales = findViewById(R.id.btnOpenAnnales);
        Button btnQuiz = findViewById(R.id.btnOpenQuiz);
        Button btnFavorites = findViewById(R.id.btnOpenFavorites);

        btnLibrary.setOnClickListener(v -> startActivity(new Intent(this, LibraryActivity.class)));
        btnAnnales.setOnClickListener(v -> startActivity(new Intent(this, ExamAnnalesActivity.class)));
        btnQuiz.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        btnFavorites.setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));

        viewModel.getProgressLiveData().observe(this, progress -> {
            if (progress != null) {
                tvDocsRead.setText(String.valueOf(progress.getDocumentsReadCount()));
                tvCoursesDown.setText(String.valueOf(progress.getCoursesDownloadedCount()));
                tvStudyTime.setText(progress.getStudyTimeMinutes() / 60 + "h");
                tvQuizzesPassed.setText(String.valueOf(progress.getQuizzesPassedCount()));
            }
        });

        viewModel.loadStudentProgress(sessionManager.getUserId());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
