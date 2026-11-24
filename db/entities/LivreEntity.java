package td.teladoum.baobab.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import td.teladoum.baobab.data.model.Livre;

/**
 * Entité Room pour le stockage local des livres et annales (Baobab Learning).
 */
@Entity(tableName = "livres")
public class LivreEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String titre;
    private String auteur;
    private String niveau;
    private String matiere;
    private String type;
    private String urlPdf;
    private String couverture;
    private String corrigeUrl;
    private boolean isFavorite;
    private boolean isDownloaded;
    private boolean isDownloadable;
    private String localPath;

    public LivreEntity() {
        this.id = "";
    }

    public static LivreEntity fromModel(Livre livre) {
        if (livre == null) return null;
        LivreEntity entity = new LivreEntity();
        entity.id = livre.getId() != null ? livre.getId() : String.valueOf(livre.hashCode());
        entity.titre = livre.getTitre();
        entity.auteur = livre.getAuteur();
        entity.niveau = livre.getNiveau();
        entity.matiere = livre.getMatiere();
        entity.type = livre.getType();
        entity.urlPdf = livre.getUrlPdf();
        entity.couverture = livre.getCouverture();
        entity.corrigeUrl = livre.getCorrigeUrl();
        entity.isFavorite = livre.isFavorite();
        entity.isDownloaded = livre.isDownloaded();
        entity.isDownloadable = livre.isDownloadable();
        entity.localPath = livre.getLocalPath();
        return entity;
    }

    public Livre toModel() {
        Livre livre = new Livre(id, titre, auteur, niveau, matiere, type, urlPdf, couverture, corrigeUrl);
        livre.setFavorite(isFavorite);
        livre.setDownloaded(isDownloaded);
        livre.setDownloadable(isDownloadable);
        livre.setLocalPath(localPath);
        return livre;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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
