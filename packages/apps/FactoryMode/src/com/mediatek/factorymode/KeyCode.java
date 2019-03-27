
package com.mediatek.factorymode;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.WindowManager;

public class KeyCode extends Activity implements OnClickListener {
    SharedPreferences mSp;

    TextView mInfo;

    Button mBtOk;

    Button mBtFailed;

    String mKeycode = "";

    private GridView mGrid;

    private List<Integer> mListData;

    final int imgString[] = {
            R.drawable.home, R.drawable.menu, R.drawable.vldown, R.drawable.vlup, R.drawable.back,
            R.drawable.search, R.drawable.camera, R.drawable.unknown
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keycode);
        mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        mInfo = (TextView) findViewById(R.id.keycode_info);
        mBtOk = (Button) findViewById(R.id.keycode_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.keycode_bt_failed);
        mBtFailed.setOnClickListener(this);
        mListData = new ArrayList<Integer>();
        mGrid = (GridView) findViewById(R.id.keycode_grid);;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                if (mKeycode.indexOf("CAMERA") >= 0) {
                    return false;
                }
                mKeycode += "CAMERA\n";
                mListData.add(imgString[6]);
                break;
            case KeyEvent.KEYCODE_HOME:
                if (mKeycode.indexOf("HOME") >= 0) {
                    return false;
                }
                mKeycode += "HOME\n";
                mListData.add(imgString[0]);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mKeycode.indexOf("VLDOWN") >= 0) {
                    return false;
                }
                mKeycode += "VLDOWN\n";
                mListData.add(imgString[2]);
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mKeycode.indexOf("VLUP") >= 0) {
                    return false;
                }
                mKeycode += "VLUP\n";
                mListData.add(imgString[3]);
                break;
            case KeyEvent.KEYCODE_BACK:
                if (mKeycode.indexOf("BACK") >= 0) {
                    return false;
                }
                mKeycode += "BACK\n";
                mListData.add(imgString[4]);
                break;
            case KeyEvent.KEYCODE_SEARCH:
                if (mKeycode.indexOf("SEARCH") >= 0) {
                    return false;
                }
                mKeycode += "SEARCH\n";
                mListData.add(imgString[5]);
                break;
            case KeyEvent.KEYCODE_MENU:
                if (mKeycode.indexOf("MENU") >= 0) {
                    return false;
                }
                mKeycode += "MENU\n";
                mListData.add(imgString[1]);
                break;
            default:
                mListData.add(imgString[7]);
                break;

        }
        mGrid.setAdapter(new MyAdapter(this));
        return true;
    }

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public MyAdapter(FactoryMode factoryMode, int factoryButton) {
        }

        public int getCount() {
            if (mListData == null) {
                return 0;
            }
            return mListData.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.keycode_grid, null);
            ImageView imgview = (ImageView) convertView.findViewById(R.id.imgview);
            imgview.setBackgroundResource(mListData.get(position));
            return convertView;
        }
    }
/* Vanzo:zhangjingzhi on: Fri, 01 Nov 2013 14:35:52 +0800
 * TODO: add HOME key test
 */
    @Override
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED);
        }
// End of Vanzo: zhangjingzhi

    @Override
    public void onClick(View v) {
        Utils.SetPreferences(this, mSp, R.string.KeyCode_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.FT_SUCCESS : AppDefine.FT_FAILED);
        finish();
    }
}
