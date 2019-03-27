
package com.mediatek.engineermode;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DeviceInfoSettings extends Activity {
    private TableLayout mTableLayout;
    private Button mButtonOK;
    private TextView mProject;
    private String project = SystemProperties.get(
            "ro.build.flavor", "Device info");
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.device_info);
        mTableLayout = (TableLayout) findViewById(R.id.tableLayout);
        mButtonOK = (Button) findViewById(R.id.button_ok);
        mButtonOK.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        mProject = (TextView) findViewById(R.id.project);
        mProject.setText(project);

        getDeviceInfo();
    }

    private void addRow(String name, String value)
    {
        TableRow tableRow = new TableRow(this);
        TextView device_name = new TextView(this);
        TextView device_value = new TextView(this);

        device_name.setText(name);
        device_name.setTextSize(15);
        device_value.setText(value);
        device_value.setTextSize(15);

        device_value.setGravity(Gravity.RIGHT);

        tableRow.addView(device_name);
        tableRow.addView(device_value);

        mTableLayout.addView(tableRow);
    }
    private void getDeviceInfo(){
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/mtkdev"), 256);
            if (reader != null) {
                String line = reader.readLine();
                line = reader.readLine();
                for (;line != null;) {
                    int colon = line.lastIndexOf(":");
                    if (colon < 1) break;
                    String name = line.substring(0, colon + 1);
                    String value = line.substring(colon + 2);
                    addRow(name,value);
                    line = reader.readLine();
                }
            }

            reader.close();
        } catch (IOException e) {

        }
    }

}
