package tr.edu.gtu.rcclone.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import java.util.ArrayList;
import java.util.List;

import tr.edu.gtu.rcclone.MainActivity;
import tr.edu.gtu.rcclone.MainApplication;
import tr.edu.gtu.rcclone.R;
import tr.edu.gtu.rcclone.data.models.AppModel;
import tr.edu.gtu.rcclone.data.models.Remote;
import tr.edu.gtu.rcclone.data.models.SendViewModel;
import tr.edu.gtu.rcclone.data.service.RemoteBluetoothService;

/**
 * A placeholder fragment containing a simple view.
 */
public class SendFragment extends Fragment {

    public static final int TAB_TITLE_RES = R.string.TAB_TEXT_SEND;

    private static final String ARG_SECTION_NUMBER = "section_number";

    private SendViewModel sendModel;
    private MainActivity mainActivity;
    private Button btn;
    private SearchableSpinner spinner;
    ArrayAdapter<Remote.RemoteCommand> adapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sendModel = ViewModelProviders.of(this).get(SendViewModel.class);
        mainActivity = (MainActivity) getActivity();

        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        sendModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        btn = root.findViewById(R.id.button2);
        spinner = root.findViewById(R.id.searchableSpinner);
        btn.setEnabled(false);
        adapter = new ArrayAdapter<>(mainActivity, R.layout.adapter_layout, new ArrayList<Remote.RemoteCommand>());
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppModel.command.setValue(adapter.getItem(position));
                //btn.setEnabled(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                AppModel.command.setValue(null);
                //btn.setEnabled(false);
            }
        });
        AppModel.updateCommandList.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<Remote.RemoteCommand> commandList = MainApplication.getRemoteDB(mainActivity.getApplicationContext()).remoteDAO().getCommands();
                        adapter.clear();
                        adapter.addAll(commandList);
                        //AppModel.command.setValue(commandList.get(0));
                    }
                }).start();
            }
        });
        AppModel.updateCommandList.setValue(true);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteBluetoothService.COMMANDObj commandObj = new RemoteBluetoothService.COMMANDObj(RemoteBluetoothService.COMMAND.SEND, AppModel.command.getValue(), new RemoteBluetoothService.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(String response) {
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mainActivity, "SENT COMMM", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCommandError() {

                    }
                });
                mainActivity.remoteBluetoothService.commandQueue.offer(commandObj);
            }
        });

        AppModel.command.observe(getViewLifecycleOwner(), new Observer<Remote.RemoteCommand>() {
            @Override
            public void onChanged(Remote.RemoteCommand remoteCommand) {
                if (remoteCommand == null) {
                    btn.setEnabled(false);
                }
                else {
                    btn.setEnabled(true);
                    btn.setText(remoteCommand.name);
                }

            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Remote.RemoteCommand> commandList = MainApplication.getRemoteDB(mainActivity.getApplicationContext()).remoteDAO().getCommands();
                    adapter.clear();
                    adapter.addAll(commandList);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }
}