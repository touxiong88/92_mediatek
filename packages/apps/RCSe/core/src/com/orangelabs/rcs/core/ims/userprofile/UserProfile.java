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

package com.orangelabs.rcs.core.ims.userprofile;

import com.orangelabs.rcs.provider.settings.RcsSettings;

import java.util.ListIterator;
import java.util.Vector;

import javax.sip.header.ExtensionHeader;
import javax.sip.header.Header;

/**
 * User profile 
 *
 * @author JM. Auffret
 */
public class UserProfile {
	
	/**
	 * User name
	 */
	private String username;

	/**
	 * User private ID
	 */
	private String privateID;

	/**
	 * User password
	 */
	private String password;

	/**
	 * Home domain
	 */
	private String homeDomain;

	/**
	 * XDM server address
	 */
	private String xdmServerAddr;

	/**
	 * XDM server login
	 */
	private String xdmServerLogin;

	/**
	 * XDM server password
	 */
	private String xdmServerPassword;

	/**
	 * IM conference URI
	 */
	private String imConferenceUri;

	/**
	 * Associated URIs
	 */
	private Vector<String> associatedUriList = new Vector<String>();
	
	/**
	 * Preferred URI
	 */
	private String preferredUri = null;
	
	/**
	 * M:Access Network Info.@{T-Mobile
	 */
	public static String currentAccessNetworkInfo;
	/**
	 * @}
	 */

	/**
	 * M:Last Access Network Info.@{T-Mobile
	 */
	public static String lastAccessNetworkInfo;

	/**
	 * @}
	 */
	/**
	 * Constructor
	 * 
	 * @param username
	 *            Username
	 * @param privateID
	 *            Private id
	 * @param password
	 *            Password
	 * @param homeDomain
	 *            Home domain
	 * @param xdmServerAddr
	 *            XDM server address
	 * @param xdmServerLogin
	 *            Outbound proxy address
	 * @param xdmServerPassword
	 *            Outbound proxy address
	 * @param imConferenceUri
	 *            IM conference factory URI
	 */
	public UserProfile(String username,
			String privateID,
			String password,
			String homeDomain,
			String xdmServerAddr,
			String xdmServerLogin,
			String xdmServerPassword,
            String imConferenceUri, String lastAccessNetworkInfo, String currentAccessNetworkInfo) {
		this.username = username;
		this.privateID = privateID;
		this.password = password;
		this.homeDomain = homeDomain;
		this.xdmServerAddr = xdmServerAddr;
		this.xdmServerLogin = xdmServerLogin;
		this.xdmServerPassword = xdmServerPassword;
		this.imConferenceUri = imConferenceUri;
		this.preferredUri = "sip:" + username + "@" + homeDomain;
		/**
		 * M:Add for P-Last-Access-Network-Info and P-Access-Network-Info
		 * headers.@{T-Mobile
		 */
//		this.lastAccessNetworkInfo = lastAccessNetworkInfo;
//		this.currentAccessNetworkInfo = currentAccessNetworkInfo;
		/**
		 * @}
		 */
	}

	/**
	 * Get the user name
	 * 
	 * @return Username
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Set the user name
	 * 
	 * @param username Username
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Get the user preferred URI
	 * 
	 * @return Preferred URI
	 */
	public String getPreferredUri() {
		return preferredUri;
	}	

	/**
	 * Get the user public URI
	 * 
	 * @return Public URI
	 */
	public String getPublicUri() {
		if (preferredUri == null) { 
			return "sip:" + username + "@" + homeDomain;
		} else {
			return preferredUri;
		}
	}
	
	/**
	 * Set the user associated URIs
	 * 
	 * @param uris List of URIs
	 */
	public void setAssociatedUri(ListIterator<Header> uris) {
		if (uris == null) {
			return;
		}
        /**
         * M: Added to resolve the issue of "from" & "to" header. Changed to use the same uri format.@
         */
		String sipUri = null;
		String telUri = null;
		while(uris.hasNext()) {
			ExtensionHeader header = (ExtensionHeader)uris.next();
			String value = header.getValue();
			associatedUriList.addElement(value);
			if (value.startsWith("<sip:")) {
				sipUri = value;
            } else if (value.startsWith("<tel:")) {
				telUri = value;
			}
		}
		
        boolean isTelUriEnabled = RcsSettings.getInstance().isTelUriFormatUsed();
        if (isTelUriEnabled) {
		if ((sipUri != null) && (telUri != null)) {
			preferredUri = telUri;
            } else if (telUri != null) {
			preferredUri = telUri;
            } else if (sipUri != null) {
			preferredUri = sipUri;
		}
        } else {
            if ((sipUri != null) && (telUri != null)) {
                preferredUri = sipUri;
            } else if (sipUri != null) {
                preferredUri = sipUri;
            } else if (telUri != null) {
                preferredUri = telUri;
            }
        }
        /**
         * @}
         */
	}
	
	/**
	 * Get the user private ID
	 * 
	 * @return Private ID
	 */
	public String getPrivateID() {
		return privateID;
	}
	
	/**
	 * Returns the user password
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Returns the home domain
	 * 
	 * @return Home domain
	 */
	public String getHomeDomain() {
		return homeDomain;
	}
	
	/**
	 * Set the home domain
	 * 
	 * @param domain Home domain
	 */
	public void setHomeDomain(String domain) {
		this.homeDomain = domain;
	}
	
	/**
	 * Set the XDM server address
	 * 
	 * @param addr Server address
	 */
	public void setXdmServerAddr(String addr) {
		this.xdmServerAddr = addr;
	}

	/**
	 * Returns the XDM server address
	 * 
	 * @return Server address
	 */
	public String getXdmServerAddr() {
		return xdmServerAddr;
	}
	
	/**
	 * Set the XDM server login
	 * 
	 * @param login Login
	 */
	public void setXdmServerLogin(String login) {
		this.xdmServerLogin = login;
	}

	/**
	 * Returns the XDM server login
	 * 
	 * @return Login
	 */
	public String getXdmServerLogin() {
		return xdmServerLogin;
	}

	/**
	 * Set the XDM server password
	 * 
	 * @param pwd Password
	 */
	public void setXdmServerPassword(String pwd) {
		this.xdmServerPassword = pwd;
	}

	/**
	 * Returns the XDM server password
	 * 
	 * @return Password
	 */
	public String getXdmServerPassword() {
		return xdmServerPassword;
	}
	
	/**
	 * Set the IM conference URI
	 * 
	 * @param uri URI
	 */
	public void setImConferenceUri(String uri) {
		this.imConferenceUri = uri;
	}

	/**
	 * Returns the IM conference URI
	 * 
	 * @return URI
	 */
	public String getImConferenceUri() {
		return imConferenceUri;
	}

	/**
	 * M:Returns the information of last access. @{T-Mobile
	 */
	/**
	 * Returns the last access position information
	 * 
	 * @return The last access position information
	 */
	public String getLastAccessNetworkInfo() {
		return this.lastAccessNetworkInfo;
	}

	/**
	 * M:Returns the information of current access. @{T-Mobile
	 */
	/**
	 * Returns the current access position information
	 * 
	 * @return The current access position information
	 */
	public String getAccessNetworkInfo() {
		return this.currentAccessNetworkInfo;
	}

	/**
	 * @}
	 */

     /** Returns the profile value as string
     * 
     * @return String
     */
	public String toString() {
		String result = "IMS username=" + username + ", " 
			+ "IMS private ID=" + privateID + ", "
			+ "IMS password=" + password + ", "
			+ "IMS home domain=" + homeDomain + ", "
			+ "XDM server=" + xdmServerAddr + ", "
			+ "XDM login=" + xdmServerLogin + ", "
			+ "XDM password=" + xdmServerPassword + ", " 
			+ "IM Conference URI=" + imConferenceUri;
		return result;
	}
}
