package tr.edu.gtu.rcclone.data.models;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Remote.class, Remote.RemoteCommand.class}, version = 1)
public abstract class RemoteDB extends RoomDatabase {
    public abstract RemoteDAO remoteDAO();
}
