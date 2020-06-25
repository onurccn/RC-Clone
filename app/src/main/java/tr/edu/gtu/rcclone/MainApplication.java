package tr.edu.gtu.rcclone;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

import tr.edu.gtu.rcclone.data.models.RemoteDB;

public class MainApplication extends Application {
    private static RemoteDB remoteDB;
    private static String DB_NAME = "RC-DB";


    @Override
    public void onCreate() {
        super.onCreate();
        getRemoteDB(this); // Initialize db
    }

    public static RemoteDB getRemoteDB(@Nullable Context context) {
        if (remoteDB == null && context != null) {
            remoteDB = Room.databaseBuilder(context,
                    RemoteDB.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return remoteDB;
    }
}
