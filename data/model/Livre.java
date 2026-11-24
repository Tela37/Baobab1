package td.teladoum.baobab.data.model;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Modèle représentant un livre, un cours ou une annale d'examen dans Baobab Learning.
 */
@IgnoreExtraProperties
public class Livre {

    private String id;
    private String titre;
    private String auteur;
    private String niveau; // Primaire, Collège, Lycée, Université, Pro, BEPC, BAC, ENA, etc.
    private String matiere; // Mathématiques, Français, Physique, SVT, etc.
    private String type; // LIVRE, COURS, ANNALE, CORRIGE
    private String urlPdf;
    private String couverture;
    private String corrigeUrl;
    private boolean isFavorite;
    private boolean isDownloaded;
    private boolean isDownloadable = true;
    private String localPath;

    public Livre() {
    }

    public Livre(String id, String titre, String auteur, String niveau, String matiere, String type, String urlPdf, String couverture, String corrigeUrl) {
        this.id = id;
        this.titre = titre;
        this.auteur = auteur;
        this.niveau = niveau;
        this.matiere = matiere;
        this.type = type;
        this.urlPdf = urlPdf;
        this.couverture = couverture;
        this.corrigeUrl = corrigeUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getMatiere() {
        return matiere;
    }

    public void setMatiere(String matiere) {
        this.matiere = matiere;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrlPdf() {
        return urlPdf;
    }

    public void setUrlPdf(String urlPdf) {
        this.urlPdf = urlPdf;
    }

    public String getCouverture() {
        return couverture;
    }

    public void setCouverture(String couverture) {
        this.couverture = couverture;
    }

    public String getCorrigeUrl() {
        return corrigeUrl;
    }

    public void setCorrigeUrl(String corrigeUrl) {
        this.corrigeUrl = corrigeUrl;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isDownloadable() {
        return isDownloadable;
    }

    public void setDownloadable(boolean downloadable) {
        isDownloadable = downloadable;
    }
}
