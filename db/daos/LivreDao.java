package td.teladoum.baobab.db.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import td.teladoum.baobab.db.entities.LivreEntity;

/**
 * DAO Room pour les livres et annales de Baobab Learning.
 */
@Dao
public interface LivreDao {

    @Query("SELECT * FROM livres")
    List<LivreEntity> getAllLivres();

    @Query("SELECT * FROM livres WHERE niveau = :niveau")
    List<LivreEntity> getLivresByNiveau(String niveau);

    @Query("SELECT * FROM livres WHERE matiere = :matiere")
    List<LivreEntity> getLivresByMatiere(String matiere);

    @Query("SELECT * FROM livres WHERE type = :type")
    List<LivreEntity> getLivresByType(String type);

    @Query("SELECT * FROM livres WHERE isFavorite = 1")
    List<LivreEntity> getFavoriteLivres();

    @Query("SELECT * FROM livres WHERE isDownloaded = 1")
    List<LivreEntity> getDownloadedLivres();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLivre(LivreEntity livre);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLivres(List<LivreEntity> livres);

    @Query("DELETE FROM livres WHERE id = :id")
    void deleteLivre(String id);
}
