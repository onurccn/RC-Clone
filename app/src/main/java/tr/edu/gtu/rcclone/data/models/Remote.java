package tr.edu.gtu.rcclone.data.models;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Relation;

import java.util.List;

@Entity(tableName = "remotes")
public class Remote {
    @Entity(tableName = "commands")
    public static class RemoteCommand {
        @PrimaryKey
        public int id;
        public int remoteId;
        public String name;
        public Integer protocol;
        public String rawTimes;
        public Integer rawLength;
        public Long protocolValue;
        public Integer protocolBits;


        public byte[] getRawSend() {
            return rawTimes.getBytes();
        }
        // Maybe some information about location. Or just list commands
    }

    //DAO
    public static class RemoteWithCommands {
        @Embedded
        public Remote remote;
        @Relation(
                parentColumn = "id",
                entityColumn = "remoteId"
        )
        public List<RemoteCommand> commands;
    }

    @PrimaryKey
    public int id;
    public String name;
}
