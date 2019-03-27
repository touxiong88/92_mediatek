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

package com.orangelabs.rcs.core.ims.network.sip;

import com.orangelabs.rcs.core.ims.network.ImsNetworkInterface;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipInterface;
import com.orangelabs.rcs.core.ims.protocol.sip.SipMessage;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SIP manager
 *
 * @author JM. Auffret
 */
public class SipManager {

	/**
	 * SIP timeout for SIP transaction (in seconds)
	 */
	public static int TIMEOUT = 300;

	/**
     * IMS network interface
     */
    private ImsNetworkInterface networkInterface;

    /**
	 * SIP stack
	 */
	private SipInterface sipstack = null;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
     * Constructor
     *
     * @param parent IMS network interface
     */
	public SipManager(ImsNetworkInterface parent) {
		this.networkInterface = parent;

		if (logger.isActivated()) {
			logger.info("SIP manager started");
		}
	}

	/**
     * Returns the network interface
     *
     * @return Network interface
     */
	public ImsNetworkInterface getNetworkInterface() {
		return networkInterface;
    }

	/**
     * Returns the SIP stack
     *
     * @return SIP stack
     */
	public SipInterface getSipStack() {
		return sipstack;
    }

	/**
	 * Terminate the manager
	 */
	public void terminate() {
		if (logger.isActivated()) {
			logger.info("Terminate the SIP manager");
		}

		// Close the SIP stack
		if (sipstack != null) {
			closeStack();
		}

		if (logger.isActivated()) {
			logger.info("SIP manager has been terminated");
		}
	}

	/**
     * Initialize the SIP stack
     *
     * @param localAddr Local IP address
     * @param proxyAddr Outbound proxy address
     * @param proxyPort Outbound proxy port
     * @param isSecure Need secure connection or not
     * @return SIP stack
     * @throws SipException
     */
    public synchronized void initStack(String localAddr, String proxyAddr,
    		int proxyPort, String protocol) throws SipException {
		// Close the stack if necessary
		closeStack();

		// Create the SIP stack
        sipstack = new SipInterface(localAddr, proxyAddr, proxyPort, protocol);
    }

	/**
	 * Close the SIP stack
	 */
	public synchronized void closeStack() {
		if (sipstack == null) {
			// Already closed
			return;
		}

		try {
			// Close the SIP stack
			sipstack.close();
			sipstack = null;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't close SIP stack properly", e);
			}
		}
	}

	/**
     * Send a SIP message and create a context to wait a response
     *
     * @param message SIP message
     * @return Transaction context
     * @throws SipException
     */
	public SipTransactionContext sendSipMessageAndWait(SipMessage message) throws SipException {
		if (sipstack != null) {
			return sipstack.sendSipMessageAndWait(message);
		} else {
			throw new SipException("Stack not initialized");
		}
	}

	/**
     * Send a SIP response
     *
     * @param response SIP response
     * @throws SipException
     */
	public void sendSipResponse(SipResponse response) throws SipException {
		if (sipstack != null) {
			sipstack.sendSipResponse(response);
		} else {
			throw new SipException("Stack not initialized");
		}
	}

    /**
     * Send a SIP ACK
     *
     * @param dialog Dialog path
     * @throws SipException
     */
	public void sendSipAck(SipDialogPath dialog) throws SipException {
		if (sipstack != null) {
			sipstack.sendSipAck(dialog);
		} else {
			throw new SipException("Stack not initialized");
		}
	}

    /**
     * Send a SIP BYE
     *
     * @param dialog Dialog path
     * @throws SipException
     */
	public void sendSipBye(SipDialogPath dialog) throws SipException {
		if (sipstack != null) {
			sipstack.sendSipBye(dialog);
		} else {
			throw new SipException("Stack not initialized");
		}
	}

    /**
     * Send a SIP CANCEL
     *
     * @param dialog Dialog path
     * @throws SipException
     */
	public void sendSipCancel(SipDialogPath dialog) throws SipException {
		if (sipstack != null) {
			sipstack.sendSipCancel(dialog);
		} else {
			throw new SipException("Stack not initialized");
		}
	}

    /**
     * Send a subsequent SIP request
     *
     * @param dialog Dialog path
     * @param request Request
     * @throws SipException
     */
	public SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request) throws SipException {
		if (sipstack != null) {
			return sipstack.sendSubsequentRequest(dialog, request);
		} else {
			throw new SipException("Stack not initialized");
		}
	}
}
