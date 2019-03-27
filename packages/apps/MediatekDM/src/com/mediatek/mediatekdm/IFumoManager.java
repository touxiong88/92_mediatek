
package com.mediatek.mediatekdm;

import java.util.Map;

public interface IFumoManager {
    /**
     * Trigger CI FUMO session to query new version. This method trigger an FUMO CI operation immediately (by invoking
     * triggerNow()).
     * NOTE: This method is asynchronous.
     */
    void queryNewVersion();

    /**
     * Recover an previously aborted FUMO CI session. This method can only be invoked if current FUMI CI operation is
     * in recovery state.
     * NOTE: this method will not perform any DmOperation related task. It's the invoker's responsibility to maintain
     * operation state.
     */
    void retryQueryNewVersion();

    /**
     * Recover an previously aborted DL session. This method can only be invoked if current DL operation is in
     * recovery state.
     * NOTE: this method will not perform any DmOperation related task. It's the invoker's responsibility to maintain
     * operation state.
     */
    void recoverDlPkg();

    /**
     * Update FUMO download progress. MMI progress invokes this when engine updates downloading progress.
     * 
     * @param current Current downloaded size.
     * @param total Total size.
     */
    void updateDownloadProgress(long current, long total);

    /**
     * Trigger FUMO report session via DM engine.
     */
    void reportResult(DmOperation operation);

    /**
     * Release resources. Client should invoke this after 
     */
    void destroy();

    /**
     * Get the session handler for FUMO.
     * 
     * @return
     */
    SessionHandler getSessionHandler();

    /**
     * Whether current FUMO state is DOWNLOAD_COMPLETE.
     * 
     * @return
     */
    boolean isDownloadComplete();

    boolean isDownloadPaused();

    String getDlResumeFilename();

    Map<String, String> generateFumoReportInformation(int resultCode, boolean clearState);

    int queryActions();

    boolean isFumoInitiator(String initiator);

    void initReportActivity(boolean isUpdateSucc);

    void clearDlStateAndReport(int reportDelay, int result);

    int RESULT_SUCCESSFUL = 200; /* MdmFumoUpdateResult.ResultCode.SUCCESSFUL */
    int RESULT_UPDATE_FAILED = 410; /* MdmFumoUpdateResult.ResultCode.UPDATE_FAILED */
}
