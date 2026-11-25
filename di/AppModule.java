package td.teladoum.baobab.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import td.teladoum.baobab.db.AppDatabase;
import td.teladoum.baobab.firebase.FirebaseHelper;
import td.teladoum.baobab.repositories.MarketplaceRepository;
import td.teladoum.baobab.repositories.UserRepository;
import td.teladoum.baobab.utils.SessionManager;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    @Singleton
    public FirebaseHelper provideFirebaseHelper() {
        return FirebaseHelper.getInstance();
    }

    @Provides
    @Singleton
    public SessionManager provideSessionManager(@ApplicationContext Context context) {
        return new SessionManager(context);
    }

    @Provides
    @Singleton
    public UserRepository provideUserRepository(@ApplicationContext Context context) {
        return new UserRepository(context);
    }

    @Provides
    @Singleton
    public MarketplaceRepository provideMarketplaceRepository(@ApplicationContext Context context) {
        return new MarketplaceRepository(context);
    }
}
