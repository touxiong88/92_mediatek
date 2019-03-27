/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.configurecheck;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;

public class MyListActivity extends Activity {
    /** Called when the activity is first created. */
    private ListView mListView;
    private ArrayList<HashMap<String, Object>> mArrayItems;
    private CustomAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_items_list);
        final Builder builder = new AlertDialog.Builder(this);

        mListView = (ListView) findViewById(R.id.checkList);
        mArrayItems = new ArrayList<HashMap<String, Object>>();
        

        mAdapter = new CustomAdapter(this, mArrayItems, R.layout.custom_listview_item,
                new String[] {"lv_custom_title", 
                        "lv_result_str", "lv_result_img" },
                new int[] { R.id.lv_custom_name,
                        R.id.lv_custom_result, R.id.lv_check_result });
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub
                HashMap<String, Object> hash = mArrayItems.get(arg2);
                if (hash.get("lv_custom_title") == getResources().getString(R.string.eigenvalue)) {
                    if (hash.get("lv_result_info") == null) {
                        Intent intent = new Intent(MyListActivity.this, EigenvalueActivity.class);
                        startActivity(intent);
                    } else {
                        builder.setMessage(hash.get("lv_result_info").toString());
                        builder.setPositiveButton(
                                getResources().getString(R.string.AlertDialog_OK),
                                null);
                        builder.create().show();
                    }
                } else {
                    if (hash.get("lv_result_info") != null) {
                        builder.setMessage(hash.get("lv_result_info").toString());
                        builder.setPositiveButton(
                                getResources().getString(R.string.AlertDialog_OK),
                                null);
                        builder.create().show();
                    }
                }
            }
        });

    }

    public void onResume() {
        super.onResume();

    }

    public void addItem(ItemConfigure item) {
        HashMap<String, Object> adapterItem = new HashMap<String, Object>();
        boolean contains = false;
        int index;
        adapterItem = setArrayItem(item);
        for (index = 0; index < mArrayItems.size(); index++) {
            if (mArrayItems.get(index).get("lv_custom_title").equals(item.mTitle)) {
                contains = true;
                break;
            }
        }
        if (contains) {
            System.out.println("addItem " + index + " set");
            mArrayItems.set(index, adapterItem);
        } else {
            System.out.println("addItem " + index + " add");
            mArrayItems.add(adapterItem);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void removeItem(int index) {
        mArrayItems.remove(index);
        mAdapter.notifyDataSetChanged();
    }

    public int getItemNum() {
        return mArrayItems.size();
    }
    public void clearItems() {
        mArrayItems.clear();
        mAdapter.notifyDataSetChanged();
    }

    private HashMap<String, Object> setArrayItem(ItemConfigure item) {
        HashMap<String, Object> adapterItem = new HashMap<String, Object>();
        adapterItem.put("lv_custom_title", item.mTitle);
        adapterItem.put("lv_result_img", item.mIcon);
        adapterItem.put("lv_result_str", item.mResult);
        adapterItem.put("lv_result_info", item.mInfo);
        return adapterItem;
    }

};
