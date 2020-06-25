package tr.edu.gtu.rcclone.ui.main;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import tr.edu.gtu.rcclone.MainActivity;
import tr.edu.gtu.rcclone.MainApplication;
import tr.edu.gtu.rcclone.R;
import tr.edu.gtu.rcclone.data.models.AppModel;
import tr.edu.gtu.rcclone.data.models.ReceiveViewModel;
import tr.edu.gtu.rcclone.data.models.Remote;
import tr.edu.gtu.rcclone.data.service.RemoteBluetoothService;

public class ReceiveFragment extends Fragment {
    private static int RETRY_MAX = 3;

    private ReceiveViewModel mViewModel;
    private TextView text;
    private TextView text_debug;
    private Button button;
    private Button receiveButton;

    private boolean ignoreBT = false;

    private MainActivity mainActivity;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.receive_fragment, container, false);
        mainActivity = (MainActivity) getActivity();
        text_debug = v.findViewById(R.id.textView2);
        button = v.findViewById(R.id.button);
        button.setEnabled(false);
        receiveButton = v.findViewById(R.id.rec_button);
        receiveButton.setEnabled(false);
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity != null) {
                    if (AppModel.isBtConnected.getValue()) {
                        AppModel.command.setValue(null);
                        RemoteBluetoothService.COMMANDObj command = new RemoteBluetoothService.COMMANDObj(RemoteBluetoothService.COMMAND.RECV, null, new RemoteBluetoothService.OnCommandResultListener() {
                            @Override
                            public void onCommandResult(final String response) {
                                ignoreBT = false;
                                String[] commandArr = response.split(" ");
                                final Remote.RemoteCommand command = new Remote.RemoteCommand();
                                command.protocol = Integer.parseInt(commandArr[0]); // add checks
                                command.rawLength = Integer.parseInt(commandArr[1]);
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < command.rawLength; i++) {
                                    sb.append(commandArr[i + 2]);
                                    sb.append(" ");
                                }
                                sb.deleteCharAt(sb.length() - 1);
                                command.rawTimes = sb.toString();
                                if (command.protocol > 0) {
                                    command.protocolBits = Integer.parseInt(commandArr[commandArr.length - 2]);
                                    command.protocolValue = Long.parseLong(commandArr[commandArr.length - 1]);
                                }
                                command.name = AppModel.currentRemoteName.getValue();
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        text_debug.setText(response);
                                        //AppModel.command.setValue(command);
                                        receiveButton.setEnabled(true);

                                        final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                                        builder.setMessage("Do you want to add " + command.name + "to db?")
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        MainApplication.getRemoteDB(mainActivity.getApplicationContext()).remoteDAO().insertCommand(command);
                                                    }
                                                }).start();
                                            }
                                        })
                                        .setNegativeButton("NO", null).show();
                                    }
                                });
                            }

                            @Override
                            public void onCommandError() {
                                receiveButton.setEnabled(true);
                                ignoreBT = false;
                            }
                        });
                        if (mainActivity.remoteBluetoothService.commandQueue.offer(command)) {
                            receiveButton.setEnabled(false);
                            ignoreBT = true;
                        }
                    }
                }
            }
        });
        text = v.findViewById(R.id.textView);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ReceiveViewModel.class);
        // TODO: Use the ViewModel
        AppModel.currentRemoteName.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                text.setText("COMMAND: " + s);
                if (s.length() > 0) {
                    receiveButton.setEnabled(true);
                }
            }
        });

        AppModel.isBtConnected.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (ignoreBT) return;
                if (aBoolean) {
                    receiveButton.setEnabled(true);
                }
                else {
                    receiveButton.setEnabled(false);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

    }
}