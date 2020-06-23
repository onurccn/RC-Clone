package tr.edu.gtu.rcclone;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import tr.edu.gtu.rcclone.ui.main.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 150;
    private static final int DISCOVERY_TIMEOUT_MILLIS = 12000;
    private static final int CONNECTION_MAX_RETRY = 3;
    public static final UUID sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice rcClone = null;
    private RemoteBluetoothService remoteBluetoothService = null;

    private long discoveryStartedTime = 0;
    private Thread checkConnection;

    public ViewPager viewPager;
    private Chip statusChip;
    private MainActivity tempThis = this;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                String deviceName = device.getName();
                if (deviceName != null && deviceName.contentEquals("RC-Clone")) {
                    btAdapter.cancelDiscovery();
                    discoveryStartedTime = System.currentTimeMillis();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    rcClone = btAdapter.getRemoteDevice(deviceHardwareAddress);
                    rcClone.setPin(new byte[]{1,2,1,4});

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            createConnection();
                        }
                    });
                    t.start();
                    unregisterReceiver(receiver);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        statusChip = findViewById(R.id.status_indicator);
        setChipStatusFail();
        FloatingActionButton fab = findViewById(R.id.fab);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorBackground));

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        fab.setVisibility(View.GONE);

        if (btAdapter == null) {
            Toast.makeText(this, "No bluetooth adapter found. Need one to use this application.", Toast.LENGTH_LONG).show();
            finish();
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
        else {
            startBTProcess();
        }
        //statusChipThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    startDiscovery();

                }
                break;

            case REQUEST_LOCATION:
                if (resultCode == RESULT_OK) {
                    startBTProcess();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (checkConnection != null && checkConnection.isAlive()) {
            checkConnection.interrupt();
        }

        if (remoteBluetoothService != null) {
            remoteBluetoothService.terminateConnection();
        }

        try {
            unregisterReceiver(receiver);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startBTProcess() {
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            startDiscovery();
        }
    }

    private void startDiscovery(){
        setChipStatusConnecting();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        btAdapter.startDiscovery();
        discoveryStartedTime = System.currentTimeMillis();

        if (checkConnection == null) {
            checkConnection = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(!checkConnection.isInterrupted() && remoteBluetoothService == null && discoveryStartedTime > 0) {
                        try {
                            if (System.currentTimeMillis() - discoveryStartedTime > DISCOVERY_TIMEOUT_MILLIS) {
                                setChipStatusConnecting();
                                if (rcClone != null) {
                                    statusText = "CONNECTING";
                                    Snackbar.make(viewPager, "Connection error. Reconnecting.", Snackbar.LENGTH_LONG).show();
                                    discoveryStartedTime = System.currentTimeMillis();
                                    createConnection();
                                }
                                else if (btAdapter != null){
                                    statusText = "DISCOVERING";
                                    Snackbar.make(viewPager, "Starting RC-Clone discovery again.", Snackbar.LENGTH_LONG).show();
                                    startDiscovery();
                                }
                            }
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                            setChipStatusFail();
                        }
                    }
                    checkConnection = null;
                }
            });
            checkConnection.start();
        }
    }

    private void createConnection() {
        BluetoothSocket rcSocket = null;
        try {
            rcSocket = rcClone.createInsecureRfcommSocketToServiceRecord(sppUUID);
            rcSocket.connect();
            remoteBluetoothService = RemoteBluetoothService.getInstance(tempThis, rcClone, rcSocket);
            discoveryStartedTime = 0;
            Snackbar.make(viewPager, "CONNECTED To RC-Clone", Snackbar.LENGTH_LONG).show();
        }
        catch (IOException ex) {
            // Couldnt connect. Show error.
            ex.printStackTrace();
            remoteBluetoothService = null;
            rcSocket = null;
        }
    }

    public void showConnectionError() {
        setChipStatusFail();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(tempThis)
                    .setTitle("Connection Error")
                    .setMessage("Please make sure RC-Clone is powered up and try again!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            tempThis.finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            }
        });
    }

    public void setChipStatusFail() {
        isInProgress = false;
        statusChip.setChipBackgroundColorResource(R.color.colorFail);
        statusChip.setText("");
        statusChip.clearAnimation();
    }

    public void setChipStatusConnecting() {
        if (isInProgress) return;
        isInProgress = true;
        statusChip.setChipBackgroundColorResource(R.color.colorInProgress);
        //statusChip.setText(statusText + "...");
        Animation blink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
        statusChip.startAnimation(blink);
    }

    public void setChipStatusConnected() {
        isInProgress = false;
        statusChip.setChipBackgroundColorResource(R.color.colorSuccess);
        statusChip.setText("");
        statusChip.clearAnimation();
    }

    private boolean isInProgress = false;
    private String statusText = "DISCOVERING";
    private Thread statusChipThread = new Thread(new Runnable() {
        @Override
        public void run() {
            int state = 0;
            boolean increaseDirection = true;
            while (true) {
                if (isInProgress) {
                    switch (state % 3) {
                        case 0:
                            statusChip.setText(statusText + ".");
                            break;
                        case 1:
                            statusChip.setText(statusText + "..");
                            break;
                        case 2:
                            statusChip.setText(statusText + "...");
                            break;
                    }
                    if (increaseDirection) state++;
                    else state--;

                    if (state > 2) {
                        increaseDirection = false;
                        state = 2;
                    }
                    else if (state < 0) {
                        increaseDirection = true;
                        state = 0;
                    }
                }
                else {
                    state = 0;
                    increaseDirection = true;
                }

                try {
                    Thread.sleep(100);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    });
}