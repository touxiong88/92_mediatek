package com.mediatek.updatesystem;

import java.io.File;
import java.io.FileFilter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mediatek.common.featureoption.FeatureOption;

public class UpdateSystem extends Activity implements OnClickListener {

    private Intent mIntent;
    private View mInflater;
    private LinearLayout mLayoutView;
    private Display mDisplay;
    private boolean bZero = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mInflater = getLayoutInflater().inflate(R.layout.nothing, null);
        setContentView(mInflater);

        File file = null;
        File[] files = null;
        FileFilter myFilter = new MyFileFilter();
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            file = new File("/mnt/sdcard2");
            files = file.listFiles(myFilter);
            if (files == null || files.length == 0) {
                file = new File("/mnt/sdcard");
                bZero = true;
                files = file.listFiles(myFilter);
                if (files == null || files.length == 0) {
                    return;
                }
            }
        } else {
            return;
        }

        mDisplay = getWindowManager().getDefaultDisplay();

        mIntent = new Intent();
        mIntent.setClassName("com.android.settings", "com.android.settings.UpdateSystem");
        if (files.length < 5) {
            mInflater = getLayoutInflater().inflate(R.layout.fewpack, null);
            mLayoutView = (LinearLayout) mInflater.findViewById(R.id.main2_layout);
        } else {
            mInflater = getLayoutInflater().inflate(R.layout.abundantpack, null);
            mLayoutView = (LinearLayout) mInflater.findViewById(R.id.abundantpack_layout);
        }
        addViewToLayout(mLayoutView, files);
        setContentView(mInflater);
    }

    private void addViewToLayout(LinearLayout Layout, File[] files) {
        ViewHolder viewHolder = new ViewHolder();
        // view.height = files.length < 5 ? use this height : use display.getheight / 5
        int height = (int) (mDisplay.getHeight() / (files.length + 1));
        int num = -1;
        String content;
        String temp;
        for (int i = 0; i < files.length; i++) {
            content = files[i].getName().substring(0, files[i].getName().length() - 4);
            temp = content.substring(6);
            temp = temp.equals("") ? "1" : temp;
            num = Integer.parseInt(temp);
            View updateView = getLayoutInflater().inflate(R.layout.update, null);

            viewHolder.updateName = (TextView) updateView
                    .findViewById(R.id.Update_text_name);
            viewHolder.updateContent = (TextView) updateView
                    .findViewById(R.id.Update_text_content);
            viewHolder.updateButton = (Button) updateView
                    .findViewById(R.id.update_button);
            viewHolder.updateName.setText(viewHolder.updateName.getText().toString()
                    + num);
            viewHolder.updateContent.setText(viewHolder.updateContent.getText()
                    .toString() + getString(R.string.update_noInfo));
            viewHolder.updateButton.setId(num);
            viewHolder.updateButton.setOnClickListener(this);
            if (i > 0) {
                updateView.findViewById(R.id.update_line).setVisibility(View.VISIBLE);
            }

            if (files.length >= 5) {
                height = mDisplay.getHeight() / 5;
            }
            updateView
                    .setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, height));
            Layout.addView(updateView);
        }
    }

    public static int[] bubbleSort(int[] list) {
        int temp;
        for (int j = 0; j < list.length; j++) {
            for (int i = list.length - 1; i > j; i--) {
                if (list[j] > list[i]) {
                    temp = list[j];
                    list[j] = list[i];
                    list[i] = temp;
                }
            }
        }
        return list;
    }

    @Override
    public void onClick(View v) {
        Bundle b = new Bundle();
        b.putString("update", "" + v.getId());
        b.putBoolean("zero", bZero);
        mIntent.putExtras(b);
        this.startActivity(mIntent);
    }
}

class ViewHolder {
    ImageView logo;
    TextView updateName;
    TextView updateContent;
    Button updateButton;
}
