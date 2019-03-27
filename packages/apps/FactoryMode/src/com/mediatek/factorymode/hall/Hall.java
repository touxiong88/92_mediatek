
package com.mediatek.factorymode.hall;

import java.io.FileNotFoundException;
import java.io.FileReader;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;

public class Hall extends Activity implements OnClickListener {
    private Handler myHandler;
    private Button mBtOk;
    private int mHallState = 1;
    private String HALL_PATH = "/sys/class/switch/hall/state";
    private Button mBtFailed;
    SharedPreferences mSp;
    private TextView hallStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.hall);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        hallStatus = (TextView) findViewById(R.id.hall_status);
        mBtOk = (Button) findViewById(R.id.hall_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.hall_bt_failed);
        mBtFailed.setOnClickListener(this);
        myHandler = new Handler();
        myHandler.post(myRunnable);
    }

    public Runnable myRunnable = new Runnable() {
        int newState = mHallState;

        public void run() {
            char[] buffer = new char[8];
            try {
                FileReader file = new FileReader(HALL_PATH);
                int len = file.read(buffer, 0, 8);
                newState = Integer.valueOf((new String(buffer, 0, len)).trim());
                if (newState == mHallState) {
                    hallStatus.setText(getResources().getString(R.string.hall_status_away));
                    hallStatus.setTextColor(getApplicationContext().getResources().getColor(
                            R.color.Blue));
                } else {
                    hallStatus.setText(getResources().getString(R.string.hall_status_near));
                    hallStatus.setTextColor(getApplicationContext().getResources().getColor(
                            R.color.Green));
/* Vanzo:libing on: Mon, 17 Feb 2014 17:22:54 +0800
 * fixbug: show right hall state as the hardware
                    mHallState = newState;
 */
// End of Vanzo:libing
                }
            } catch (FileNotFoundException e) {
                Log.v("error", "This kernel does not have hall switch support");
            } catch (Exception e) {
                Log.v("error", "", e);
            }
            myHandler.postDelayed(myRunnable, 200);
        }
    };

    public void onClick(View v) {
        myHandler.removeCallbacks(myRunnable);
        Utils.SetPreferences(this, mSp, R.string.hall,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
