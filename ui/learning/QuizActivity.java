package td.teladoum.baobab.ui.learning;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import td.teladoum.baobab.R;
import td.teladoum.baobab.data.model.QuizQuestion;

/**
 * Activité des Quiz de révision interactifs.
 */
public class QuizActivity extends AppCompatActivity {

    private LearningViewModel viewModel;
    private TextView tvQuestion, tvQuizScore;
    private RadioGroup radioGroupOptions;
    private RadioButton rbOpt1, rbOpt2, rbOpt3, rbOpt4;
    private Button btnNextQuestion;

    private List<QuizQuestion> questionList;
    private int currentIndex = 0;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

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

        Toolbar toolbar = findViewById(R.id.toolbarQuiz);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quiz de Révision ❓");
        }

        viewModel = new ViewModelProvider(this).get(LearningViewModel.class);

        tvQuestion = findViewById(R.id.tvQuizQuestion);
        tvQuizScore = findViewById(R.id.tvQuizScore);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        rbOpt1 = findViewById(R.id.rbOption1);
        rbOpt2 = findViewById(R.id.rbOption2);
        rbOpt3 = findViewById(R.id.rbOption3);
        rbOpt4 = findViewById(R.id.rbOption4);
        btnNextQuestion = findViewById(R.id.btnNextQuestion);

        viewModel.getQuizLiveData().observe(this, questions -> {
            if (questions != null && !questions.isEmpty()) {
                questionList = questions;
                showQuestion(currentIndex);
            }
        });

        btnNextQuestion.setOnClickListener(v -> {
            int selectedId = radioGroupOptions.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Veuillez sélectionner une réponse", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedOptionIndex = -1;
            if (selectedId == R.id.rbOption1) selectedOptionIndex = 0;
            else if (selectedId == R.id.rbOption2) selectedOptionIndex = 1;
            else if (selectedId == R.id.rbOption3) selectedOptionIndex = 2;
            else if (selectedId == R.id.rbOption4) selectedOptionIndex = 3;

            QuizQuestion current = questionList.get(currentIndex);
            if (selectedOptionIndex == current.getCorrectOptionIndex()) {
                score++;
                Toast.makeText(this, "Bonne réponse ! 🎉", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Mauvaise réponse. " + current.getExplanation(), Toast.LENGTH_LONG).show();
            }

            currentIndex++;
            if (currentIndex < questionList.size()) {
                showQuestion(currentIndex);
            } else {
                tvQuestion.setText("Quiz terminé ! Votre score : " + score + "/" + questionList.size());
                radioGroupOptions.setVisibility(android.view.View.GONE);
                btnNextQuestion.setEnabled(false);
            }
        });

        viewModel.loadQuizzes("TOUT");
    }

    private void showQuestion(int index) {
        if (questionList == null || index >= questionList.size()) return;
        QuizQuestion q = questionList.get(index);
        tvQuestion.setText("Question " + (index + 1) + "/" + questionList.size() + "\n\n" + q.getQuestion());
        List<String> options = q.getOptions();
        if (options.size() > 0) rbOpt1.setText(options.get(0));
        if (options.size() > 1) rbOpt2.setText(options.get(1));
        if (options.size() > 2) rbOpt3.setText(options.get(2));
        if (options.size() > 3) rbOpt4.setText(options.get(3));
        radioGroupOptions.clearCheck();
        tvQuizScore.setText("Score : " + score);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
