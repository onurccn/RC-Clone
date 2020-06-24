package tr.edu.gtu.rcclone.data.models;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface RemoteDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertRemote(Remote... remotes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertCommand(Remote.RemoteCommand... commands);

    @Delete
    public void deleteRemote(Remote... remotes);

    @Delete
    public void deleteCommand(Remote.RemoteCommand... commands);

    @Transaction
    @Query("SELECT * FROM remotes")
    public List<Remote.RemoteWithCommands> getRemotesWithCommands();

    @Query("SELECT * FROM remotes")
    public List<Remote> getRemotes();
}
