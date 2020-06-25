package tr.edu.gtu.rcclone.data.models;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.Relation;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Entity(tableName = "remotes")
public class Remote {
    @Ignore
    public final static int MESSAGE_LENGTH = 300;
    @Ignore
    public final static int SHORT_MESSAGE_LENGTH = 50;

    @Entity(tableName = "commands")
    public static class RemoteCommand {
        @PrimaryKey(autoGenerate = true)
        public int id;
        public int remoteId;
        public String name;
        public Integer protocol;
        public String rawTimes;
        public Integer rawLength;
        public Long protocolValue;
        public Integer protocolBits;


        public byte[] getProtocolSend() {
            String pt = Integer.toString(protocolBits);
            String protocol = pt + " " + Long.toString(protocolValue);
            if (protocol.length() < SHORT_MESSAGE_LENGTH) {
                while(protocol.length() < SHORT_MESSAGE_LENGTH)
                    protocol += " ";
            }
            System.out.println("Sending " + protocol + ":");
            return protocol.getBytes();
        }

        public byte[] getRawSend() {
            if (rawTimes.length() < MESSAGE_LENGTH) {
                while(rawTimes.length() < MESSAGE_LENGTH)
                    rawTimes += " ";
            }
            return rawTimes.getBytes();
        }
        // Maybe some information about location. Or just list commands


        @NonNull
        @Override
        public String toString() {
            return name;
        }
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
