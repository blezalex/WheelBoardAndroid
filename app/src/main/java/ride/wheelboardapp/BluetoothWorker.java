package ride.wheelboardapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import proto.Protocol;

public class BluetoothWorker {
    public BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mSerialDevice;
    BluetoothSocket btSocket;
    OutputStream outputStream;

    static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Thread btRecieveThrread;
    private Activity host;
    SerialComm comm;

    public BluetoothWorker(Activity host, SerialComm.ProtoHandler protoHandler) {
        this.host = host;
        comm = new SerialComm(protoHandler);
    }

    public void Stop() {
        if (btSocket != null) {
            try {
                btSocket.close();
                btSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsg(Protocol.RequestId id) throws IOException {
        if (outputStream != null)
            SerialComm.sendMsg(outputStream, id);
        else
            showError("Not connected!");
    }

    public void sendConfig(Protocol.Config cfg) throws IOException {
        if (outputStream != null)
            SerialComm.sendConfig(outputStream, cfg);
        else
            showError("Not connected!");
    }

    public void setupBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(host.getApplicationContext(), "No Bluetooth!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            host.startActivityForResult(enableBtIntent, 1);
        }
    }

    void showError(String text) {
        Toast.makeText(host.getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    public void connectToDevice(String address) {
        setupBluetooth();
        mSerialDevice = mBluetoothAdapter.getRemoteDevice(address); // TODO: congifure bt device for 115200!
        if (mSerialDevice == null) {
            Toast.makeText(host.getApplicationContext(), "No paired device", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(host.getApplicationContext(), "Found device :)", Toast.LENGTH_LONG).show();
        }

        try {
            btSocket = mSerialDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            btSocket.connect();
        } catch (IOException e) {
            showError("socket create failed: " + e.getMessage() + ".");
            return;
        }

        try {
            outputStream = btSocket.getOutputStream();
            btRecieveThrread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try{
                            comm.RunReader(btSocket.getInputStream());
                        } catch (IOException e) {
                            showError(e.toString()); // TODO: dispatch error via handler (cross thread)
                        }
                    }
                }
            });
            btRecieveThrread.start();
        } catch (IOException e) {
            showError("failed to get stream: " + e.getMessage() + ".");
        }
    }
}
