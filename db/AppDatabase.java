package td.teladoum.baobab.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import td.teladoum.baobab.db.daos.ConversationDao;
import td.teladoum.baobab.db.daos.MarketplaceDao;
import td.teladoum.baobab.db.daos.MessageDao;
import td.teladoum.baobab.db.daos.PostDao;
import td.teladoum.baobab.db.daos.UserDao;
import td.teladoum.baobab.db.entities.ConversationEntity;
import td.teladoum.baobab.db.entities.MarketplaceEntity;
import td.teladoum.baobab.db.entities.MessageEntity;
import td.teladoum.baobab.db.entities.PostEntity;
import td.teladoum.baobab.db.entities.UserEntity;

@Database(entities = {UserEntity.class, PostEntity.class, MessageEntity.class, ConversationEntity.class, MarketplaceEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "baobab_room.db";
    private static volatile AppDatabase instance;

    public abstract UserDao userDao();
    public abstract PostDao postDao();
    public abstract MessageDao messageDao();
    public abstract ConversationDao conversationDao();
    public abstract MarketplaceDao marketplaceDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    Context appContext = context != null ? context.getApplicationContext() : null;
                    if (appContext != null) {
                        instance = Room.databaseBuilder(appContext, AppDatabase.class, DB_NAME)
                                .fallbackToDestructiveMigration()
                                .build();
                    }
                }
            }
        }
        return instance;
    }
}
