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

/**
 * Feature tags
 * 
 * @author jexa7410
 */
public class FeatureTags {
	/**
	 * OMA IM feature tag
	 */
	public final static String FEATURE_OMA_IM = "+g.oma.sip-im";

	/**
	 * 3GPP video share feature tag
	 */
	public final static String FEATURE_3GPP_VIDEO_SHARE = "+g.3gpp.cs-voice";
	
	/**
     * 3GPP image share feature tag
     */
    public final static String FEATURE_3GPP_IMAGE_SHARE = "+g.3gpp.iari-ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is\"";
											
	/**
	 * 3GPP image share feature tag for RCS 2.0
	 */
	public final static String FEATURE_3GPP_IMAGE_SHARE_RCS2 = "+g.3gpp.app_ref=\"urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is\"";
	
	/**
	 * RCS-e feature tag prefix
	 */
	public final static String FEATURE_RCSE = "+g.3gpp.iari-ref";
	
	/**
	 * RCS-e image share feature tag
	 */
	public final static String FEATURE_RCSE_IMAGE_SHARE = "urn%3Aurn-7%3A3gpp-application.ims.iari.gsma-is";

	/**
	 * RCS-e chat feature tag
	 */
	public final static String FEATURE_RCSE_CHAT = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.im";

	/**
	 * RCS-e file transfer feature tag
	 */
	public final static String FEATURE_RCSE_FT = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.ft";

	/**
	 * RCS-e presence discovery feature tag
	 */
	public final static String FEATURE_RCSE_PRESENCE_DISCOVERY = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.dp";

	/**
	 * RCS-e social presence feature tag
	 */
	public final static String FEATURE_RCSE_SOCIAL_PRESENCE = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse.sp";

	/**
	 * RCS-e extension feature tag prefix
	 */
	public final static String FEATURE_RCSE_EXTENSION = "urn%3Aurn-7%3A3gpp-application.ims.iari.rcse";
	
	/**
	 * M: add feature tag for T-Mobile
	 * @{T-Mobile
 	 */
	/**
	 * 3GPP icsi for mmtel feature tag for T-Mobile
	 */
	public final static String FEATURE_3GPP_ICSI_MMTEL= "+g.3gpp.icsi_ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel\"";
	
	/**
	 * 3GPP icsi for content share video feature tag for T-Mobile 
	 */
	public final static String FEATURE_3GPP_ICSI_MMTEL_VIDEO= "urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel.video";

	/**
	 * 3GPP icsi for emergency feature tag for T-Mobile
	 */
	public final static String FEATURE_3GPP_ICSI_EMERGENCY= "+g.3gpp.icsi_ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.e-location\"";
	
	/**
	 * 3GPP SMS over IP feature tag for T-Mobile
	 */
	public final static String FEATURE_3GPP_SMSIP= "+g.3gpp.smsip";
	/** T-Mobile@} */
}
