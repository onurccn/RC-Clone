package tr.edu.gtu.rcclone.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import tr.edu.gtu.rcclone.MainActivity;
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
        btn.setEnabled(false);
        final TextView textView = root.findViewById(R.id.section_label);
        sendModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainActivity.remoteBluetoothService.sendCommand(RemoteBluetoothService.COMMAND.SRAW, AppModel.command.getValue())){
                    String resp = mainActivity.remoteBluetoothService.receiveMessage();
                    if (resp != null && resp.equals("OK")){
                        Toast.makeText(getActivity(), "SENT COMMM", Toast.LENGTH_LONG).show();
                    }
                }
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
                }

            }
        });

        return root;
    }
}