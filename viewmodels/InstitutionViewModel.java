package td.teladoum.baobab.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import td.teladoum.baobab.models.Institution;
import td.teladoum.baobab.models.InstitutionEvent;
import td.teladoum.baobab.models.InstitutionNews;
import td.teladoum.baobab.models.InstitutionResult;
import td.teladoum.baobab.repositories.InstitutionRepository;

@HiltViewModel
public class InstitutionViewModel extends ViewModel {

    private final InstitutionRepository repository;
    private final MutableLiveData<List<Institution>> institutionsLiveData = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public InstitutionViewModel(InstitutionRepository repository) {
        this.repository = repository;
        loadInstitutions();
    }

    public void loadInstitutions() {
        repository.fetchAllInstitutions(() -> {
            institutionsLiveData.postValue(repository.getAllInstitutions());
        });
    }

    public LiveData<List<Institution>> getInstitutionsLiveData() {
        institutionsLiveData.setValue(repository.getAllInstitutions());
        return institutionsLiveData;
    }

    public List<InstitutionNews> getNews(String institutionId) {
        return repository.getNews(institutionId);
    }

    public List<InstitutionResult> getResults(String institutionId) {
        return repository.getResults(institutionId);
    }

    public List<InstitutionEvent> getEvents(String institutionId) {
        return repository.getEvents(institutionId);
    }

    public void createInstitution(Institution institution, Runnable onSuccess, Runnable onFailure) {
        repository.createInstitution(institution, () -> {
            institutionsLiveData.postValue(repository.getAllInstitutions());
            if (onSuccess != null) onSuccess.run();
        }, onFailure);
    }

    public void addNews(String institutionId, String title, String content, String imageUrl, Runnable onSuccess, Runnable onFailure) {
        repository.addNews(institutionId, title, content, imageUrl, onSuccess, onFailure);
    }

    public void addResult(String institutionId, String title, String details, String documentUrl, Runnable onSuccess, Runnable onFailure) {
        repository.addResult(institutionId, title, details, documentUrl, onSuccess, onFailure);
    }

    public void addEvent(String institutionId, String title, String description, String date, String location, String imageUrl, Runnable onSuccess, Runnable onFailure) {
        repository.addEvent(institutionId, title, description, date, location, imageUrl, onSuccess, onFailure);
    }
}
