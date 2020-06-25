package tr.edu.gtu.rcclone.data.models;

import androidx.lifecycle.MutableLiveData;

public class AppModel {
    private static AppModel model;

    public static AppModel getInstance() {
        if (model == null) {
            model = new AppModel();
        }

        return model;
    }

    public static boolean isWaitingRecvResp = false;
    public static MutableLiveData<String> currentRemoteName = new MutableLiveData<>();
    public static MutableLiveData<Boolean> isBtConnected = new MutableLiveData<>(false);
    public static MutableLiveData<Remote.RemoteCommand> command = new MutableLiveData<>();
    public static MutableLiveData<Boolean> updateCommandList = new MutableLiveData<>(false);
}
