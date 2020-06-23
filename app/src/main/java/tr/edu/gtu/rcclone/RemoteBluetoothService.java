package tr.edu.gtu.rcclone;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static tr.edu.gtu.rcclone.MainActivity.sppUUID;

public class RemoteBluetoothService {
    private static final String TAG = "RemoteBluetoothService";

    public enum COMMAND {
        RUOK, // Are you ok?
        RSET, // RESET
        RECV, // RECEIVE
        SRAW, // SEND RAW
        SPRT  // SEND PROTOCOL
    }
    private static final int MAX_RETRY_COUNT = 10;
    private static RemoteBluetoothService mSingletonService = null;
    private Context mContext;

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    private int retryCount = 0;
    private boolean connected;
    private Thread backgroundConnectionChecker = new Thread(new Runnable() {
        @Override
        public void run() {
            while (mSocket != null) {
                if (backgroundConnectionChecker.isInterrupted()) break;
                if (mSocket.isConnected()) {
                    if (sendCommand(COMMAND.RUOK, null)) {
                        String s = receiveMessage();
                        if (s != null && s.equals("OK")){
                            if (!connected) {
                                ((MainActivity)mContext).setChipStatusConnected();
                                Snackbar.make(((MainActivity)mContext).viewPager, "Connection Recovered.", Snackbar.LENGTH_LONG).show();
                            }
                            connected = true;
                            continue;
                        }
                    }
                }

                try {
                    retryCount++;
                    connected = false;
                    if (retryCount > MAX_RETRY_COUNT) {
                        ((MainActivity)mContext).showConnectionError();
                        break;
                    }
                    Snackbar.make(((MainActivity)mContext).viewPager, "Connection error. Reconnecting. ("+retryCount+")", Snackbar.LENGTH_LONG).show();
                    if (mSocket.isConnected()) {
                        mSocket.close();
                    }
                    ((MainActivity)mContext).setChipStatusConnecting();
                    mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(sppUUID);
                    mSocket.connect();
                    mInStream = mSocket.getInputStream();
                    mOutStream = mSocket.getOutputStream();
                    retryCount = 0;

                }
                catch (Exception ex) {
                    ((MainActivity)mContext).setChipStatusFail();
                    System.err.println(ex.toString() + " RETRY COUNT: " + retryCount);
                    if (retryCount > MAX_RETRY_COUNT) break;
                }

                try {
                    Thread.sleep(1000); // Sanity check every second.
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    });

    public static RemoteBluetoothService getInstance(Context ctx, BluetoothDevice device, BluetoothSocket socket) throws IOException {
        if (mSingletonService == null) {
            mSingletonService = new RemoteBluetoothService(ctx, device, socket);
        }
        if (!mSingletonService.backgroundConnectionChecker.isAlive()) {
            mSingletonService.backgroundConnectionChecker.start();
        }
        return mSingletonService;
    }

    public static RemoteBluetoothService getInstance() {
        if (mSingletonService == null) {
            throw new IllegalArgumentException();
        }

        return mSingletonService;
    }

    private RemoteBluetoothService(Context ctx, BluetoothDevice device, BluetoothSocket socket) throws IOException {
        mContext = ctx;
        mSocket = socket;
        mDevice = device;
        mInStream = socket.getInputStream();
        mOutStream = socket.getOutputStream();
        connected = true;
        ((MainActivity)mContext).setChipStatusConnected();
    }

    public void terminateConnection() {
        Log.d(TAG, "Terminating Bluetooth connection");
        if (backgroundConnectionChecker.isAlive()) {
            backgroundConnectionChecker.interrupt();
            Log.d(TAG, "Background checker interrupted.");
        }

        if (mSocket.isConnected()) {
            try {
                mSocket.close();
                mSocket = null;
                Log.d(TAG, "Open BT socket closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ((MainActivity)mContext).setChipStatusFail();
    }

    private boolean sendCommand(COMMAND command, Object data) {
        try {
            switch (command) {
                case RUOK:
                    mOutStream.write(new byte[]{'R', 'U', 'O', 'K'});
                    break;
            }
            mOutStream.flush();
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private String receiveMessage() {
        try {
            byte[] buffer = new byte[300];
            int len = mInStream.read(buffer);
            if (len == 0) return null;

            return new String(buffer, 0, len);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
