/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.richcall.image;

import java.io.IOException;
import java.util.Vector;

import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpManager;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.KeyStoreManager;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Terminating content sharing session (transfer)
 * 
 * @author jexa7410
 */
public class TerminatingImageTransferSession extends ImageTransferSession implements MsrpEventListener {
	/**
	 * MSRP manager
	 */
	private MsrpManager msrpMgr = null;
	
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
	 * @param parent IMS service
	 * @param invite Initial INVITE request
	 */
	public TerminatingImageTransferSession(ImsService parent, SipRequest invite) {
		super(parent, ContentManager.createMmContentFromSdp(invite.getContent()), SipUtils.getAssertedIdentity(invite));

		// Create dialog path
		createTerminatingDialogPath(invite);
	}
	
	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new sharing session as terminating");
	    	}
	
	    	// Send a 180 Ringing response
	    	send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

	    	// Check if the MIME type is supported
	    	if (getContent() == null) {
	    		if (logger.isActivated()){
    				logger.debug("MIME type is not supported");
    			}

    			// Send a 415 Unsupported media type response
				send415Error(getDialogPath().getInvite());

				// Unsupported media type
				handleError(new ContentSharingError(ContentSharingError.UNSUPPORTED_MEDIA_TYPE));
        		return;
        	}
            
			// Wait invitation answer
	    	int answer = waitInvitationAnswer();
			if (answer == ImsServiceSession.INVITATION_REJECTED) {
				if (logger.isActivated()) {
					logger.debug("Session has been rejected by user");
				}
				
		    	// Remove the current session
		    	getImsService().removeSession(this);

		    	// Notify listeners
		    	for(int i=0; i < getListeners().size(); i++) {
		    		getListeners().get(i).handleSessionAborted();
		        }
				return;
			} else
			if (answer == ImsServiceSession.INVITATION_NOT_ANSWERED) {
				if (logger.isActivated()) {
					logger.debug("Session has been rejected on timeout");
				}

                /**
                 * M: modified for IOT item that it should get "No answer"
                 * notification once timed out @{
                 */
                // Ringing period timeout
                send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());
                /**
                 * @}
                 */
		    	// Remove the current session
		    	getImsService().removeSession(this);

		    	// Notify listeners
		    	for(int i=0; i < getListeners().size(); i++) {
		    		getListeners().get(i).handleSessionAborted();
		        }
				return;
			}
			
	    	// Parse the remote SDP part
        	SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
    		Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription mediaDesc = media.elementAt(0);
			MediaAttribute attr1 = mediaDesc.getMediaAttribute("file-selector");
            String fileSelector = attr1.getName() + ":" + attr1.getValue();
			MediaAttribute attr2 = mediaDesc.getMediaAttribute("file-transfer-id");
            String fileTransferId = attr2.getName() + ":" + attr2.getValue();
			MediaAttribute attr3 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr3.getValue();
    		String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription.connectionInfo);
    		int remotePort = mediaDesc.port;
			
            // Extract the "setup" parameter
            String remoteSetup = "passive";
			MediaAttribute attr4 = mediaDesc.getMediaAttribute("setup");
			if (attr4 != null) {
				remoteSetup = attr4.getValue();
			}
            if (logger.isActivated()){
				logger.debug("Remote setup attribute is " + remoteSetup);
			}
            
    		// Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (logger.isActivated()){
				logger.debug("Local setup attribute is " + localSetup);
			}

    		// Set local port
	    	int localMsrpPort;
	    	if (localSetup.equals("active")) {
		    	localMsrpPort = 9; // See RFC4145, Page 4
	    	} else {
				localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
	    	}
	    	
            // Create the MSRP manager
			String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
			msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
    		
			// Build SDP part
	    	String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            /** M: add for MSRPoTLS @{ */
            String protocol = getCurrentProtocol();
            String sdp = null;
            if (PROTOCOL_TLS.equals(protocol)) {
                sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                        + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-"
                        + SipUtils.CRLF + "c=" + SdpUtils.formatAddressType(ipAddress)
                        + SipUtils.CRLF + "t=0 0" + SipUtils.CRLF + "m=message " + localMsrpPort
                        + " TCP/TLS/MSRP *" + SipUtils.CRLF + "a=" + fileSelector + SipUtils.CRLF
                        + "a=" + fileTransferId + SipUtils.CRLF
                        + "a=accept-types:"
                        + getContent().getEncoding() + SipUtils.CRLF + "a=fingerprint:"
                        + KeyStoreManager.getFingerPrint() + SipUtils.CRLF + "a=setup:"
                        + localSetup + SipUtils.CRLF + "a=path:" + msrpMgr.getLocalMsrpsPath()
                        + SipUtils.CRLF + "a=recvonly" + SipUtils.CRLF;
            } else {
                sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " "
                        + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "s=-"
                        + SipUtils.CRLF + "c=" + SdpUtils.formatAddressType(ipAddress)
                        + SipUtils.CRLF + "t=0 0" + SipUtils.CRLF + "m=message " + localMsrpPort
                        + " TCP/MSRP *" + SipUtils.CRLF + "a=" + fileSelector + SipUtils.CRLF
                        + "a=" + fileTransferId + SipUtils.CRLF
                        + "a=accept-types:"
                        + getContent().getEncoding() + SipUtils.CRLF + "a=setup:" + localSetup
                        + SipUtils.CRLF + "a=path:" + msrpMgr.getLocalMsrpPath() + SipUtils.CRLF
                        + "a=recvonly" + SipUtils.CRLF;
            }
	    	int maxSize = ImageTransferSession.getMaxImageSharingSize();
	    	if (maxSize > 0) {
	    		sdp += "a=max-size:" + maxSize + SipUtils.CRLF;
	    	}

            /** @} */
	    	// Set the local SDP part in the dialog path
	        getDialogPath().setLocalContent(sdp);

    		// Create the MSRP server session
            if (localSetup.equals("passive")) {
            	// Passive mode: client wait a connection
            	msrpMgr.createMsrpServerSession(remotePath, this);
            	
    			// Open the connection
    			Thread thread = new Thread(){
    				public void run(){
    					try {
    						// Open the MSRP session
    						msrpMgr.openMsrpSession(ImageTransferSession.DEFAULT_SO_TIMEOUT);

			    	        // Send an empty packet
			            	sendEmptyDataChunk();
    					} catch (IOException e) {
							if (logger.isActivated()) {
				        		logger.error("Can't create the MSRP server session", e);
				        	}
						}		
    				}
    			};
    			thread.start();            
    		}
            
            // Create a 200 OK response
        	if (logger.isActivated()) {
        		logger.info("Send 200 OK");
        	}
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
            		RichcallService.FEATURE_TAGS_IMAGE_SHARE, sdp);
            
	        // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSipMessageAndWait(resp);
    		
	        // The signalisation is established
	        getDialogPath().sigEstablished();

            // Wait response
            ctx.waitResponse(SipManager.TIMEOUT);
            
            // Analyze the received response 
            if (ctx.isSipAck()) {
    	        // ACK received
    			if (logger.isActivated()) {
    				logger.info("ACK request received");
    			}

    	        // The session is established
    	        getDialogPath().sessionEstablished();

    	        // Create the MSRP client session
                if (localSetup.equals("active")) {
                	// Active mode: client should connect
                	msrpMgr.createMsrpClientSession(remoteHost, remotePort, remotePath, this);

					// Open the MSRP session
					msrpMgr.openMsrpSession(ImageTransferSession.DEFAULT_SO_TIMEOUT);
					
	    	        // Send an empty packet
	            	sendEmptyDataChunk();
                }
                
            	// Start session timer
            	if (getSessionTimerManager().isSessionTimerActivated(resp)) {        	
            		getSessionTimerManager().start(SessionTimerManager.UAS_ROLE, getDialogPath().getSessionExpireTime());
            	}

            	// Notify listeners
		    	for(int i=0; i < getListeners().size(); i++) {
		    		getListeners().get(i).handleSessionStarted();
		        }
            } else {
        		if (logger.isActivated()) {
            		logger.debug("No ACK received for INVITE");
            	}

        		// No response received: timeout
            	handleError(new ContentSharingError(ContentSharingError.SESSION_INITIATION_FAILED));
            }
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new ContentSharingError(ContentSharingError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}		

		if (logger.isActivated()) {
    		logger.debug("End of thread");
    	}
	}

	/**
	 * Send an empty data chunk
	 */
	public void sendEmptyDataChunk() {
		try {
			msrpMgr.sendEmptyChunk();
		} catch(Exception e) {
	   		if (logger.isActivated()) {
	   			logger.error("Problem while sending empty data chunk", e);
	   		}
		}
	}	

	/**
	 * Handle error 
	 * 
	 * @param error Error
	 */
	public void handleError(ContentSharingError error) {
		if (isInterrupted()) {
			return;
		}
		
		// Error	
    	if (logger.isActivated()) {
    		logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
    	}

    	// Close the MSRP session
    	closeMsrpSession();

		// Remove the current session
    	getImsService().removeSession(this);

		// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(error);
        }
	}

	/**
	 * Data has been transfered
	 * 
	 * @param msgId Message ID
	 */
	public void msrpDataTransfered(String msgId) {
		// Not used in terminating side
	}
	
	/**
	 * Data transfer has been received
	 * 
	 * @param msgId Message ID
	 * @param data Received data
	 * @param mimeType Data mime-type 
	 */
	public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
    	if (logger.isActivated()) {
    		logger.info("Data received");
    	}
    	
    	// Image has been transfered
    	imageTransfered();
	
	   	try {
	    	// Update the content with the received data 
	    	getContent().setData(data);

	    	// Save data into a filename
	    	ContentManager.saveContent(getContent());
	    	
	    	// Notify listeners
	    	for(int j=0; j < getListeners().size(); j++) {
	    		((ImageTransferSessionListener)getListeners().get(j)).handleContentTransfered(getContent().getUrl());
	    	}	   	
	   	} catch(IOException e) {
	   		if (logger.isActivated()) {
	   			logger.error("Can't save received image", e);
	   		}

	   		// Notify listeners
	    	for(int j=0; j < getListeners().size(); j++) {
	    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_SAVING_FAILED));
	        }	   		
	   	} catch(Exception e) {
	   		// Notify listeners
	    	for(int j=0; j < getListeners().size(); j++) {
	    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED));
	        }	   		
	   	}
	}
    
	/**
	 * Data transfer in progress
	 * 
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size in bytes
	 */
	public void msrpTransferProgress(long currentSize, long totalSize) {
		// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingProgress(currentSize, totalSize);
        }
	}	

	/**
	 * Data transfer has been aborted
	 */
	public void msrpTransferAborted() {
    	if (logger.isActivated()) {
    		logger.info("Data transfer aborted");
    	}
	}	

	/**
	 * Data transfer error
	 * 
	 * @param error Error
	 */
	public void msrpTransferError(String error) {
		if (isInterrupted()) {
			return;
		}

		if (logger.isActivated()) {
    		logger.info("Data transfer error: " + error);
    	}

        // Close the media session
        closeMediaSession();
        
		// Terminate session
		terminateSession();

    	// Remove the current session
    	getImsService().removeSession(this);

    	// Notify listeners
    	for(int j=0; j < getListeners().size(); j++) {
    		((ImageTransferSessionListener)getListeners().get(j)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_TRANSFER_FAILED, error));
        }
	}
	
	/**
	 * Close the MSRP session
	 */
	private void closeMsrpSession() {
    	if (msrpMgr != null) {
    		msrpMgr.closeSession();
    	}
		if (logger.isActivated()) {
			logger.debug("MSRP session has been closed");
		}
	}

	/**
	 * Close media session
	 */
	public void closeMediaSession() {
		// Close MSRP session
		closeMsrpSession();
	}
}
