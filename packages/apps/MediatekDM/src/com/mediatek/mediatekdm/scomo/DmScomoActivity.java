
package com.mediatek.mediatekdm.scomo;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.mediatekdm.DmActivity;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmOperation.KEY;
import com.mediatek.mediatekdm.DmOperation.Type;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmOperationManager;
import com.mediatek.mediatekdm.DmOperationManagerFactory;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.util.DialogFactory;
import com.mediatek.mediatekdm.util.Utilities;

public class DmScomoActivity extends DmActivity
        implements IDmScomoStateObserver, IDmScomoDownloadProgressObserver {
    private static final String TAG = DmConst.TAG.SCOMO + "/DmScomoActivity";

    RelativeLayout mDownloadingLayout;
    RelativeLayout mInstallingLayout;
    LinearLayout mEmptyLayout;
    private boolean mIsPaused = false;
    private ScomoManager mScomo;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "+IDmScomoStateObserver.onServiceConnected()");
        mScomo = (ScomoManager) mService.getScomoManager();
        mScomo.registerObserver(this);
        mScomo.registerDownloadObserver(this);
        updateUI(mScomo.getScomoState().state, null);
    }

    @Override
    public void onServiceDisconnected() {
        mScomo.unregisterDownloadObserver(this);
        mScomo.unregisterObserver(this);
        mScomo = null;
        super.onServiceDisconnected();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        DmOperationManager operationManager = DmOperationManagerFactory.getInstance();
        if (operationManager.isBusy()) {
            DmOperation operation = operationManager.current();
            boolean isScomoDL = operation.getProperty(KEY.TYPE).equals(Type.TYPE_DL) &&
                    operation.getBooleanProperty(KEY.SCOMO_TAG, false);
            if (isScomoDL) {
                Log.d(TAG, "SCOMO downloading. Allow access to UI.");
            } else {
                Log.d(TAG, "Other DM operation running. Disallow access to UI.");
                Toast toast = Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.cmcc_task_running), Toast.LENGTH_LONG);
                toast.show();
                Log.d(TAG, "Display a toast to prompt user.");
                finish();
            }
        }

        this.setContentView(R.layout.scomo);
        this.setTitle(R.string.scomo_activity_title);

        mDownloadingLayout = (RelativeLayout) this.findViewById(R.id.LayoutDownloading);
        mInstallingLayout = (RelativeLayout) this.findViewById(R.id.LayoutInstalling);
        mEmptyLayout = (LinearLayout) this.findViewById(R.id.LayoutEmpty);

        mDownloadingLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                DmOperationManager operationManager = DmOperationManagerFactory.getInstance();
                if (!operationManager.isBusy()
                        || (operationManager.isBusy() &&
                                operationManager.current().getProperty(KEY.TYPE).equals(Type.TYPE_DL) &&
                                operationManager.current().getBooleanProperty(KEY.SCOMO_TAG, false))) {
                    Intent intent = new Intent(DmScomoActivity.this, DmScomoDownloadDetailActivity.class);
                    intent.setAction("download_detail");
                    startActivity(intent);
                } else {
                    showDialog(DIALOG_BUSY);
                }
            }
        });

        if (!bindService()) {
            Log.d(TAG, "Bind service failed. Exit!");
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "+onResume()");
        mIsPaused = false;
        if (mScomo != null) {
            updateUI(mScomo.getScomoState().state, null);
        } else {
            updateUI(-1, null);
        }
        Log.d(TAG, "-onResume()");
    }

    protected void onPause() {
        Log.d(TAG, "+onPause()");
        mIsPaused = true;
        super.onPause();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_BUSY:
                return DialogFactory.newAlert(this).setTitle(R.string.cmcc_task_running)
                        .setMessage(R.string.cmcc_task_running)
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                })
                        .create();
            default:
                return null;
        }
    }

    private void updateUI(int state, Object extra) {
        Log.d(TAG, "+updateUI");
        int registerSimId = Utilities.getRegisteredSimId(this);
        if (registerSimId == -1) {
            Log.w(TAG, "The sim card is not register to DM server, show Empty View");
            showEmpty();
            return;
        }

        Log.d(TAG, "updateUI with state " + state + " extra " + extra);

        hideDownloading();
        hideInstalling();
        hideEmpty();

        if (state == -1
                || !mScomo.getScomoState().verbose) {
            showEmpty();
        } else if (state == DmScomoState.IDLE) {
            if (extra != null) {
                String reason = (String) extra;
                if (reason.equals("DM_NETWORK_ERROR")
                        || reason.equals("DM_FAILED")
                        || reason.equals("INSTALL_FAILED")
                        || reason.equals("INSTALL_OK")) {
                    displayDialog(state, extra);
                } else {
                    showEmpty();
                }
            } else {
                showEmpty();
            }
        } else if (state == DmScomoState.DOWNLOADING) {
            showDownloading(getString(R.string.downloading_scomo));
        } else if (state == DmScomoState.INSTALLING) {
            showInstalling(getString(R.string.installing_scomo));
        } else if (state == DmScomoState.DOWNLOAD_PAUSED) {
            if (extra != null && ((String) extra).equals("USER_CANCELED")) {
                showEmpty();
            } else {
                showDownloading(getString(R.string.paused));
            }
        } else if (state == DmScomoState.NEW_DP_FOUND
                || state == DmScomoState.DOWNLOAD_FAILED) {
            displayDialog(state, extra);
        } else if (state == DmScomoState.CONFIRM_INSTALL) {
            DmOperationManager operationManager = DmOperationManagerFactory.getInstance();
            if (operationManager.isBusy()) {
                displayDialog(state, extra);
                Log.d(TAG, "OperationManager is busy");
            } else {
                Log.d(TAG, "OperationManager is not busy, trigger an fake DL operation");
                DmOperation fakeDlOperation = new DmOperation(DmOperation.generateId(),
                        ScomoManager.DL_TIME_OUT, ScomoManager.DL_MAX_RETRY);
                fakeDlOperation.initDLScomo();
                operationManager.triggerNow(fakeDlOperation, false);
                displayDialog(state, extra);
            }
        }
        Log.d(TAG, "-updateUI");
    }

    public void notify(int state, int previousState, DmOperation operation, Object extra) {
        Log.d(TAG, "+onScomoUpdated(" + state + ", " + previousState + ", " + operation + "," + extra + ")");
        if (mIsPaused || mService == null || mScomo == null) {
            return;
        }
        updateUI(state, extra);
        Log.d(TAG, "-onScomoUpdated()");
    }

    private void showEmpty() {
        Log.d(TAG, "+showEmpty");
        mDownloadingLayout.setVisibility(View.GONE);
        mInstallingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.VISIBLE);
        ((ProgressBar) mEmptyLayout.findViewById(R.id.progressLoading)).setVisibility(View.GONE);
        ((TextView) mEmptyLayout.findViewById(R.id.TextViewEmpty)).setText(R.string.no_activity);
        Log.d(TAG, "-showEmpty");
    }

    private void hideEmpty() {
        Log.d(TAG, "hideEmpty");
        mEmptyLayout.setVisibility(View.GONE);
    }

    private void hideInstalling() {
        Log.d(TAG, "hideInstalling");
        mInstallingLayout.setVisibility(View.GONE);
    }

    private void hideDownloading() {
        Log.d(TAG, "hideDownloading");
        mDownloadingLayout.setVisibility(View.GONE);
    }

    private void displayDialog(int action, Object extra) {
        Log.d(TAG, "+displayDialog" );
        Intent intent = new Intent(this, DmScomoConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("action", action);
        if (action == DmScomoState.IDLE && extra != null) {
            intent.putExtra("reason", (String) extra);
        }
        startActivity(intent);
        Log.d(TAG, "-displayDialog");
    }

    private void showInstalling(String state) {
        Log.d(TAG, "+showInstalling");
        mInstallingLayout.setVisibility(View.VISIBLE);
        mDownloadingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.GONE);
        DmScomoState scomoState = mScomo.getScomoState();

        ((TextView) mInstallingLayout.findViewById(R.id.title)).setText(state);
        ((ImageView) mInstallingLayout.findViewById(R.id.icon)).setImageDrawable(scomoState
                .getIcon());
        ((TextView) mInstallingLayout.findViewById(R.id.name)).setText(scomoState.getName());
        ((TextView) mInstallingLayout.findViewById(R.id.version)).setText(scomoState.getVersion()
                + "  "
                + scomoState.getSize() / 1024 + "KB");
        Log.d(TAG, "-showInstalling");
    }

    private void showDownloading(String state) {
        Log.d(TAG, "+showDownloading");
        mInstallingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.GONE);
        mDownloadingLayout.setVisibility(View.VISIBLE);

        DmScomoState scomoState = mScomo.getScomoState();
        ((TextView) mDownloadingLayout.findViewById(R.id.title)).setText(state);
        ((ImageView) mDownloadingLayout.findViewById(R.id.downloadingIcon))
                .setImageDrawable(scomoState.getIcon());
        ((TextView) mDownloadingLayout.findViewById(R.id.TextViewName)).setText(scomoState
                .getName());
        String ratio = scomoState.currentSize / 1024 + "KB/" + scomoState.totalSize / 1024 + "KB";
        ((TextView) mDownloadingLayout.findViewById(R.id.TextViewSize)).setText(ratio);
        ((ProgressBar) mDownloadingLayout.findViewById(R.id.ProgressBarProgress))
                .setMax((int) scomoState.totalSize);
        ((ProgressBar) mDownloadingLayout.findViewById(R.id.ProgressBarProgress))
                .setProgress((int) scomoState.currentSize);
        Log.d(TAG, "-showDownloading");
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        if (mScomo != null) {
            mScomo.unregisterObserver(this);
            mScomo = null;
        }
        unbindService();
        super.onDestroy();
    }

    static final int DIALOG_BUSY = 0;

    @Override
    public void updateProgress(long current, long total) {
        Log.d(TAG, "+updateProgress(" + current + ", " + total + ")");
        if (mIsPaused || mService == null || mScomo == null) {
            return;
        }
        if (mScomo.getScomoState().state == DmScomoState.DOWNLOADING) {
            updateUI(DmScomoState.DOWNLOADING, null);
        }
        Log.d(TAG, "-updateProgress()");
    }
}
