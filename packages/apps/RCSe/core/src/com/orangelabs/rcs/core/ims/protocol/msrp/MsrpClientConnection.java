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

package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.io.IOException;

import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.platform.network.SocketConnection;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * MSRP client connection
 * 
 * @author jexa7410
 */
public class MsrpClientConnection extends MsrpConnection {
	/**
	 * Remote IP address 
	 */
	private String remoteAddress;
	
	/**
	 * Remote TCP port number
	 */
	private int remotePort; 
	
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param session MSRP session
	 * @param remoteAddress Remote IP address
	 * @param remotePort Remote port number
	 */
	public MsrpClientConnection(MsrpSession session, String remoteAddress, int remotePort) {
		super(session);
		
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
	}

	/**
	 * Returns the socket connection
	 * 
	 * @return Socket
	 * @throws IOException
	 */
	public SocketConnection getSocketConnection() throws IOException {
		if (logger.isActivated()) {
			logger.debug("Open client socket to " + remoteAddress + ":" + remotePort);
		}
		SocketConnection socket = NetworkFactory.getFactory().createSocketClientConnection();
		socket.open(remoteAddress, remotePort);		
		if (logger.isActivated()) {
			logger.debug("Socket connected to " + socket.getRemoteAddress() + ":" + socket.getRemotePort());
		}
		return socket;
	}
}
