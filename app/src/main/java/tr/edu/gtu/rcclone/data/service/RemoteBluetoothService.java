package tr.edu.gtu.rcclone.data.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;


import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import tr.edu.gtu.rcclone.MainActivity;
import tr.edu.gtu.rcclone.data.models.Remote;

import static tr.edu.gtu.rcclone.MainActivity.sppUUID;

public class RemoteBluetoothService {
    private static final String TAG = "RemoteBluetoothService";
    public enum COMMAND {
        RUOK, // Are you ok?
        RSET, // RESET
        RECV, // RECEIVE
        SEND, // SEND
        HRST
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
    private boolean skipSanityCheck = false;
    public Queue<COMMANDObj> commandQueue = new LinkedList<>();
    public Thread backgroundConnectionChecker = null;

    public void initThread () {
        if (backgroundConnectionChecker != null) {
            backgroundConnectionChecker.interrupt();
            backgroundConnectionChecker = null;
        }
        backgroundConnectionChecker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mSocket != null) {
                    COMMANDObj nextCommand = null;
                    try {
                        if (backgroundConnectionChecker.isInterrupted()) break;

                        if (mSocket.isConnected()) {
                            //if (skipSanityCheck) continue;
                            nextCommand = commandQueue.remove();
                            if (sendCommand(nextCommand.command, nextCommand.data)) {
                                System.out.println("SENT");
                                String s = receiveMessage();
                                System.out.println("REC");
                                System.out.println(s);
                                if (s != null && s.startsWith("OK")) {
                                    ((MainActivity) mContext).setChipStatusConnected();
                                    if (!connected) {
                                        Snackbar.make(((MainActivity)mContext).viewPager, "Connection Recovered.", Snackbar.LENGTH_LONG).show();
                                    }
                                    connected = true;
//                                    if (nextCommand.command == COMMAND.SRAW || nextCommand.command == COMMAND.SPRT) {
//                                        if (!s.contains("OK\nOK")) {
//                                            s = receiveMessage();
//                                        }
//                                    }
                                    if (nextCommand.command == COMMAND.RECV ) {
                                        // Wait for rc response with message
                                        s = receiveMessage();
                                        if (s.contains("\n")) {
                                            s = s.substring(0, s.length() - 1);
                                        }
                                        System.out.println("REC");
                                    }
                                    if (nextCommand.listener != null) nextCommand.listener.onCommandResult(s);
                                    continue;
                                }
                            }
                        }

                        try {
                            if (nextCommand != null && nextCommand.listener != null) {
                                nextCommand.listener.onCommandError();
                                System.out.println("ERR");
                            }

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

                    }
                    catch (Exception ex) {
                        // Probably queue empty. do nthng.
                        ex.printStackTrace();
                        commandQueue.add(new COMMANDObj(COMMAND.RSET, null, null));

                    }
                    finally {
                        try {
                            Thread.sleep(500);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }, "BT Thread");
        backgroundConnectionChecker.start();
    }

    public static RemoteBluetoothService getInstance(Context ctx, BluetoothDevice device, BluetoothSocket socket) throws IOException {
        if (mSingletonService == null) {
            mSingletonService = new RemoteBluetoothService(ctx, device, socket);
        }
        mSingletonService.initThread();
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

    public void terminateConnection(boolean softReset) {
        if (!softReset) {
            Log.d(TAG, "Terminating Bluetooth connection");
            if (backgroundConnectionChecker.isAlive()) {
                backgroundConnectionChecker.interrupt();
                Log.d(TAG, "Background checker interrupted.");
            }
        }
        else {
            this.commandQueue.offer(new COMMANDObj(COMMAND.HRST, null, null));
        }

        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
                if (!softReset) mSocket = null;
                Log.d(TAG, "Open BT socket closed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ((MainActivity)mContext).setChipStatusFail();
    }

    private boolean sendCommand(COMMAND command, Remote.RemoteCommand data) {
        try {
            switch (command) {
                case RUOK:
                    mOutStream.write(new byte[]{'R', 'U', 'O', 'K'});
                    break;
                case RSET:
                    mOutStream.write(new byte[]{'R', 'S', 'E', 'T'});
                    break;
                case RECV:
                    skipSanityCheck = true;
                    mOutStream.write(new byte[]{'R', 'E', 'C', 'V'});
                    break;
                case HRST:
                    skipSanityCheck = true;
                    StringBuilder sb = new StringBuilder();
                    while (sb.length() < 300){
                        sb.append(" ");
                    }
                    mOutStream.write(sb.toString().getBytes());
                    break;
                case SEND:
                    skipSanityCheck = true;
                    if (data.protocol > 0) {
                        mOutStream.write(new byte[]{'R', '0', Integer.valueOf((data.protocol/10)).toString().getBytes()[0], Integer.valueOf((data.protocol%10)).toString().getBytes()[0]});
                        mOutStream.flush();
                        Thread.sleep(50);
                        mOutStream.write(data.getProtocolSend());
                    }
                    else {
                        mOutStream.write(new byte[]{'R', 'W', Integer.valueOf((data.rawLength/10)).toString().getBytes()[0], Integer.valueOf((data.rawLength%10)).toString().getBytes()[0]});
                        mOutStream.flush();
                        Thread.sleep(50);
                        mOutStream.write(data.getRawSend());
                        System.out.println("Sending " + data.rawTimes + ":");
                    }

                    break;
            }
            mOutStream.flush();
            return true;
        }
        catch (Exception ex) {
            skipSanityCheck = false;
            ex.printStackTrace();
            return false;
        }
    }

    private String receiveMessage() {
        try {
            byte[] buffer = new byte[300];
            int len = 0;
            StringBuilder sb = new StringBuilder();
            do {
                len = mInStream.read(buffer);
                if (len == 0) break;
                sb.append(new String(buffer, 0, len));
            } while (buffer[len - 1] != '\n');
            skipSanityCheck = false;
            if (len == 0) return null;
            return sb.toString();
        }
        catch (Exception ex) {
            skipSanityCheck = false;
            ex.printStackTrace();
            return null;
        }
    }

    public static class COMMANDObj {
        public COMMAND command;
        public Remote.RemoteCommand data;
        public OnCommandResultListener listener;
        public COMMANDObj(COMMAND com, Remote.RemoteCommand mData, OnCommandResultListener listener){
            command = com;
            this.listener = listener;
            data = mData;
        }
    }

    public interface OnCommandResultListener {
        void onCommandResult(String response);
        void onCommandError();
    }
}
