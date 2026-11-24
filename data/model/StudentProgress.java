package td.teladoum.baobab.data.model;

/**
 * Modèle pour le tableau de bord de l'étudiant / élève.
 */
public class StudentProgress {

    private int userId;
    private int documentsReadCount;
    private int coursesDownloadedCount;
    private int studyTimeMinutes;
    private int quizzesPassedCount;

    public StudentProgress() {
    }

    public StudentProgress(int userId, int documentsReadCount, int coursesDownloadedCount, int studyTimeMinutes, int quizzesPassedCount) {
        this.userId = userId;
        this.documentsReadCount = documentsReadCount;
        this.coursesDownloadedCount = coursesDownloadedCount;
        this.studyTimeMinutes = studyTimeMinutes;
        this.quizzesPassedCount = quizzesPassedCount;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getDocumentsReadCount() {
        return documentsReadCount;
    }

    public void setDocumentsReadCount(int documentsReadCount) {
        this.documentsReadCount = documentsReadCount;
    }

    public int getCoursesDownloadedCount() {
        return coursesDownloadedCount;
    }

    public void setCoursesDownloadedCount(int coursesDownloadedCount) {
        this.coursesDownloadedCount = coursesDownloadedCount;
    }

    public int getStudyTimeMinutes() {
        return studyTimeMinutes;
    }

    public void setStudyTimeMinutes(int studyTimeMinutes) {
        this.studyTimeMinutes = studyTimeMinutes;
    }

    public int getQuizzesPassedCount() {
        return quizzesPassedCount;
    }

    public void setQuizzesPassedCount(int quizzesPassedCount) {
        this.quizzesPassedCount = quizzesPassedCount;
    }
}
