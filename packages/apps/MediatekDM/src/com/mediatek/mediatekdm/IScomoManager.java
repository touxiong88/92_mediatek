package com.mediatek.mediatekdm;

public interface IScomoManager {
    int DL_MAX_RETRY = 1;
    int DL_TIME_OUT = 5 * 60 * 1000; // 5 minutes

    /**
     * Get file name used as DL package file for SCOMO.
     * @return File name without full path.
     * 
     */
    String getDlPackageFilename();
    
    /**
     * Get file name used as DL resume file for SCOMO.
     * 
     * @return File name without full path.
     */
    String getDlResumeFilename();
    
    /**
     * Clear SCOMO DL states and schedule an report operation. This method will not trigger DM engine to start
     * an report session. The report session will be triggered in reportResult() later when the operation is processed.
     * 
     * @param reportDelay Time delay for report operation. 0 means no delay.
     */
    void clearDlStateAndReport(int reportDelay);

    void setScomoState(final int state, final DmOperation operation, final Object extra);

    void startDlPkg();

    void pauseDlPkg();

    void resumeDlPkg();
    
    void recoverDlPkg();

    void cancelDlPkg();
    
    void updateDownloadProgress(long current, long total);
    
    void reportResult(DmOperation operation);
    
    void setVerbose(boolean flag);
    
    void scomoScanPackage();
    
    SessionHandler getSessionHandler();
    
    void destroy();
    
    int queryActions();
    
    boolean isScomoInitiator(String initiator);
}