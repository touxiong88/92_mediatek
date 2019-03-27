package com.mediatek.configurecheck;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author MTK80793
 * 
 */
public class CustomAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private ArrayList<HashMap<String, Object>> mList;
    private int mLayoutID;
    private String mFlag[];
    private int mItemIDs[];

    public CustomAdapter(Context context,
            ArrayList<HashMap<String, Object>> list, int layoutID,
            String flag[], int itemIDs[]) {
        this.mInflater = LayoutInflater.from(context);
        this.mList = list;
        this.mLayoutID = layoutID;
        this.mFlag = flag;
        this.mItemIDs = itemIDs;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mList.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(mLayoutID, null);


        TextView title = (TextView) convertView.findViewById(mItemIDs[0]);
        title.setText((String) mList.get(position).get(mFlag[0]));

        TextView info = (TextView) convertView.findViewById(mItemIDs[1]);
        info.setText((String) mList.get(position).get(mFlag[1]));

        ImageView img = (ImageView) convertView.findViewById(R.id.lv_check_result);
        img.setBackgroundResource((Integer) mList.get(position).get(
                    mFlag[2]));
        
        return convertView;
    }
}
