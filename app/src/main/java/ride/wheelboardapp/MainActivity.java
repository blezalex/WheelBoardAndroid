package ride.wheelboardapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

import proto.Protocol;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cfg = Protocol.Config.newBuilder();
        SettingsActivity.setDefaults(cfg);
    }

    Protocol.Config.Builder cfg;

    @Override
    public void onClick(View v) {
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

        }

    }
}
