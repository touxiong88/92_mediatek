package com.mediatek.factorymode;
import java.io.File;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ThreedTest extends Activity implements OnClickListener {

    private Button mBtOk;

    private Button mBtFailed;

    SharedPreferences mSp;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.threed);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.threed_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.threed_bt_failed);
        mBtFailed.setOnClickListener(this);
        Intent intent = new Intent();
        intent.setClassName("com.estar.holotestcross", "com.estar.holotestcross.MainActivity");
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        File f= new File("/data/HolographyProfile");
        if (f.exists() && f.isFile()){
            Long size = f.length();
            if(size>150){
                mBtOk.setEnabled(true);

            }else {
                mBtOk.setEnabled(false);

            }
        }else{
            mBtOk.setEnabled(false);
        }
    }

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.threed_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS
                        : AppDefine.FT_FAILED);
        finish();
    }

}
