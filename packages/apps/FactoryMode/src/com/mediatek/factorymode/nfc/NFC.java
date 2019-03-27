
package com.mediatek.factorymode.nfc;

import java.util.Calendar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.mediatek.factorymode.AppDefine;
import com.mediatek.factorymode.FactoryMode;
import com.mediatek.factorymode.R;
import com.mediatek.factorymode.Utils;
import android.provider.Settings;

public class NFC extends Activity implements OnClickListener {
    private final static String TAG = "NFC_FactoryMode";
    NfcAdapter mNfcAdapter;
    TextView mNfcStateView;
    TextView mNfcTestTimeView;
    TextView mNfcIDView;
    TextView mNfcTypeView;
    TextView mNfcSectorsView;
    TextView mNfcBlocksView;
    TextView mNfcSizeView;
    TextView mNfcTestTipView;
    TextView mNfcTestResultView;
    Button mBtOk;
    Button mBtFailed;
    private static boolean mSendNfcOnBroadcast;
    private static String FACTORYMODE_NFC_TESTING = "factorymode_nfc_testing";
    private static int NFC_OPENNING = 0;
    private static int NFC_OPENED = 1;
    SharedPreferences mSp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc);
        if (FactoryMode.mHaveNFC == false) {
            finish();
            return;
        }
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mNfcStateView = (TextView) findViewById(R.id.nfc_state);
        mNfcTestTimeView = (TextView) findViewById(R.id.nfc_test_time);
        mNfcIDView = (TextView) findViewById(R.id.nfc_tag_id);
        mNfcTypeView = (TextView) findViewById(R.id.nfc_tag_type);
        mNfcSectorsView = (TextView) findViewById(R.id.nfc_tag_sectors);
        mNfcBlocksView = (TextView) findViewById(R.id.nfc_tag_blocks);
        mNfcSizeView = (TextView) findViewById(R.id.nfc_tag_size);
        mNfcTestTipView = (TextView) findViewById(R.id.nfc_test_tip);
        mNfcTestResultView = (TextView) findViewById(R.id.nfc_test_result);
        mBtOk = (Button) findViewById(R.id.nfc_btn_sucess);
        mBtFailed = (Button) findViewById(R.id.nfc_btn_failed);
        mBtOk.setOnClickListener(this);
        mBtFailed.setOnClickListener(this);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.i(TAG, "onResume");
        Settings.System.putInt(getContentResolver(), FACTORYMODE_NFC_TESTING, 1);
        if (!mNfcAdapter.isEnabled()) {
            mNfcStateView.setText(R.string.nfc_is_openning);
            mNfcTestTipView.setVisibility(View.GONE);
            mSwitchNfcHandler.postDelayed(mSwitchNfcRunnable, 100);
        } else {
            mNfcStateView.setText(R.string.nfc_is_opened);
            mNfcTestTipView.setVisibility(View.VISIBLE);
            mSwitchNfcHandler.removeCallbacks(mSwitchNfcRunnable);
        }
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
            if (mNfcAdapter.isEnabled()) {
                processIntent(getIntent());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.i(TAG, "onpause");
        mSendNfcOnBroadcast = false;
        Settings.System.putInt(getContentResolver(), FACTORYMODE_NFC_TESTING, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.i(TAG, "onDestroy");
        if (mSwitchNfcHandler != null) {
            mSwitchNfcHandler.removeCallbacks(mSwitchNfcRunnable);
        }
    }

    private String bytes2DecString(byte[] b) {
        String ret = "";
        ret = String.valueOf(Integer.valueOf(bytes2HexString(b), 16));
        return ret;
    }

    private String bytes2OctString(byte[] b) {
        String ret = "";
        ret = Integer.toOctalString(Integer.valueOf(bytes2DecString(b)));
        return ret;
    }

    private String bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

    private void processIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        MifareClassic mfc = MifareClassic.get(tagFromIntent);
        try {
            mfc.connect();
            int type = mfc.getType();
            int sectorCount = mfc.getSectorCount();
            String typeS = "";
            switch (type) {
                case MifareClassic.TYPE_CLASSIC:
                    typeS = "CLASSIC";
                    break;
                case MifareClassic.TYPE_PLUS:
                    typeS = "PLUS";
                    break;
                case MifareClassic.TYPE_PRO:
                    typeS = "PRO";
                    break;
                case MifareClassic.TYPE_UNKNOWN:
                    typeS = "UNKNOWN";
                    break;

            }
            byte[] typeId = mfc.getTag().getId();
            int blocksCount = mfc.getBlockCount();
            int size = mfc.getSize();
            Calendar calendar = Calendar.getInstance();
            String testTime = calendar.getTime().toLocaleString();
            mNfcTestTimeView.setText(getString(R.string.nfc_test_time) + testTime);
            mNfcIDView.setText(getString(R.string.nfc_tag_id) + bytes2HexString(typeId));
            mNfcTypeView.setText(getString(R.string.nfc_tag_type) + typeS);
            mNfcSectorsView.setText(getString(R.string.nfc_tag_sectors)
                    + String.valueOf(sectorCount));
            mNfcBlocksView
                    .setText(getString(R.string.nfc_tag_blocks) + String.valueOf(blocksCount));
            mNfcSizeView
                    .setText(getString(R.string.nfc_tag_size) + String.valueOf(size) + " bytes");
            setViewVilibility(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setViewVilibility(boolean visible) {
        if (visible) {
            mNfcTestTimeView.setVisibility(View.VISIBLE);
            mNfcIDView.setVisibility(View.VISIBLE);
            mNfcTypeView.setVisibility(View.VISIBLE);
            mNfcSectorsView.setVisibility(View.VISIBLE);
            mNfcBlocksView.setVisibility(View.VISIBLE);
            mNfcSizeView.setVisibility(View.VISIBLE);
            mNfcTestResultView.setVisibility(View.VISIBLE);
        } else {
            mNfcTestTimeView.setVisibility(View.GONE);
            mNfcIDView.setVisibility(View.GONE);
            mNfcTypeView.setVisibility(View.GONE);
            mNfcSectorsView.setVisibility(View.GONE);
            mNfcBlocksView.setVisibility(View.GONE);
            mNfcSizeView.setVisibility(View.GONE);
            mNfcTestResultView.setVisibility(View.GONE);
        }
    }

    Handler mSwitchNfcHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.getData().getInt("nfc_state")) {
                case 0:
                    mNfcStateView.setText(R.string.nfc_is_openning);
                    mNfcTestTipView.setVisibility(View.GONE);
                    setViewVilibility(false);
                    break;
                case 1:
                    mNfcStateView.setText(R.string.nfc_is_opened);
                    mNfcTestTipView.setVisibility(View.VISIBLE);
                    break;
            }
        }

    };
    Runnable mSwitchNfcRunnable = new Runnable() {

        @Override
        public void run() {
            if (!mSendNfcOnBroadcast) {
                Intent intent = new Intent("com.mediatek.factorymode.NFC");
                intent.putExtra("nfc_cmd", "on");
                sendBroadcast(intent);
                mSendNfcOnBroadcast = false;
            }
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            if (!mNfcAdapter.isEnabled()) {
                bundle.putInt("nfc_state", NFC_OPENNING);
                msg.setData(bundle);
                mSwitchNfcHandler.sendMessage(msg);
                mSwitchNfcHandler.postDelayed(mSwitchNfcRunnable, 100);
            } else {
                bundle.putInt("nfc_state", NFC_OPENED);
                msg.setData(bundle);
                mSwitchNfcHandler.sendMessage(msg);
                mSwitchNfcHandler.removeCallbacks(mSwitchNfcRunnable);
            }
        }
    };

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.nfc_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        Intent intent = new Intent("com.mediatek.factorymode.NFC");
        intent.putExtra("nfc_cmd", "off");
        sendBroadcast(intent);
        finish();
    }
}
