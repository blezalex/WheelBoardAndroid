package ride.wheelboardapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import proto.Protocol;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SerialComm.ProtoHandler {
    public final int GET_SETTINGS_CODE = 10;
    public final int READ_FILE_REQUEST_CODE = 11;
    public final int WRITE_FILE_REQUEST_CODE = 12;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SETTINGS_CODE && resultCode == RESULT_OK) {
            byte[] config = data.getByteArrayExtra("config");
            try {
                cfg.mergeFrom(config);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            return;
        }

        if (requestCode == WRITE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                OutputStream out = getContentResolver().openOutputStream(data.getData());
                OutputStreamWriter writer = new OutputStreamWriter(out);
                writer.write(cfg.toString());
                writer.close();
                out.close();
            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
            } catch (IOException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
            }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT);
        }

        if (requestCode == READ_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                InputStream in = getContentResolver().openInputStream(data.getData());
              //  Protocol.Config.Builder builder =  Protocol.Config.newBuilder();
                InputStreamReader reader = new InputStreamReader(in);
                cfg.clear();
                SettingsActivity.setDefaults(cfg);
                TextFormat.getParser().merge(reader, cfg);
                reader.close();
                in.close();
            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
            } catch (IOException e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG);
            }
            Toast.makeText(this, "Loaded", Toast.LENGTH_SHORT);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    Handler mHandler;

    static final int MSG_GOT_CONFIG = 1;
    static final int MSG_GOT_STATS = 2;
    static final int MSG_GENERIC = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cfg = Protocol.Config.newBuilder();
        SettingsActivity.setDefaults(cfg);

        communicator = new BluetoothWorker(this, new SerialComm.ProtoHandler() {
            @Override
            public void OnGeneric(Protocol.ReplyId reply) {
                Message message = mHandler.obtainMessage(MSG_GENERIC, reply);
                message.sendToTarget();
            }

            @Override
            public void OnConfig(Protocol.Config cfg) {
                Message message = mHandler.obtainMessage(MSG_GOT_CONFIG, cfg);
                message.sendToTarget();
            }

            @Override
            public void OnStats(Protocol.Stats stats) {
                Message message = mHandler.obtainMessage(MSG_GOT_STATS, stats);
                message.sendToTarget();
            }
        });

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

                switch (inputMessage.what) {
                    case MSG_GOT_CONFIG:
                        OnConfig((Protocol.Config)inputMessage.obj);
                        break;

                    case MSG_GOT_STATS:
                        OnStats((Protocol.Stats)inputMessage.obj);
                        break;

                    case MSG_GENERIC:
                        OnGeneric((Protocol.ReplyId)inputMessage.obj);
                        break;

                    default:
                        super.handleMessage(inputMessage);
                }

            }
        };
    }

    Protocol.Config.Builder cfg;

    BluetoothWorker communicator;

    boolean connected = false;

    @Override
    protected void onStart() {
        super.onStart();

        if (!connected)
            communicator.connectToDevice("00:12:03:27:93:72");

        connected = true;
        //communicator.connectToDevice("98:D3:31:FB:83:85");
    }


    @Override
    public void OnGeneric(Protocol.ReplyId reply) {
        Toast.makeText(getApplicationContext(), reply.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void OnConfig(Protocol.Config deviceConfig) {
        Toast.makeText(getApplicationContext(), "Got config", Toast.LENGTH_SHORT).show();
        cfg.clear().mergeFrom(deviceConfig);
    }

    @Override
    public void OnStats(Protocol.Stats stats) {
        Toast.makeText(getApplicationContext(), "Got stats: " + stats.toString(), Toast.LENGTH_LONG).show();
    }

    void showError(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onClick(View v) {
        try {
        switch (v.getId()) {
            case R.id.changeSettings:
                startActivityForResult(
                    new Intent(this, SettingsActivity.class).putExtra("config", cfg.build().toByteArray()), GET_SETTINGS_CODE);
                break;

                case R.id.readFromFile:
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setType("text/*");
                    startActivityForResult(i, READ_FILE_REQUEST_CODE);
                    break;

            case R.id.saveToFile:
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String currentDateandTime = sdf.format(new Date());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .setType("text/*")
                        .putExtra(Intent.EXTRA_TITLE, "boardConfig_" + currentDateandTime)
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, WRITE_FILE_REQUEST_CODE);
                break;

            case R.id.readFromBoard:
                    communicator.sendMsg(Protocol.RequestId.READ_CONFIG);
                break;

            case R.id.callibrateAcc:
                communicator.sendMsg(Protocol.RequestId.CALLIBRATE_ACC);
                break;

            case R.id.saveToFlash:
                communicator.sendMsg(Protocol.RequestId.SAVE_CONFIG);
                break;

            case R.id.getStats:
                communicator.sendMsg(Protocol.RequestId.GET_STATS);
                break;

            case R.id.saveToBoard:
                communicator.sendConfig(cfg.build());
                break;

        }
        } catch (IOException e) {
            showError(e.toString());
        }
    }
}
