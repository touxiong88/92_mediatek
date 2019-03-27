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

package com.orangelabs.rcs.provider.messaging;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.SessionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.service.api.client.eventslog.EventsLogApi;
import com.orangelabs.rcs.service.api.client.messaging.GroupChatInfo;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;

/**
 * Rich messaging history. This content provider removes old messages if there is no enough space.
 * 
 * @author mhsm6403
 */
public class RichMessaging {
	/**
	 * Current instance
	 */
	private static RichMessaging instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Database URI
	 */
	private Uri databaseUri = RichMessagingData.CONTENT_URI;

	/**
	 * Max log entries
	 */
	private int maxLogEntries;
	/**
	 * To check group chat exists or not already
	 */
	private boolean isGroupChatExists = false;
	
	public boolean isGroupChatExists() {
        return isGroupChatExists;
    }

    public void setGroupChatExists(boolean isGroupChatExists) {
        this.isGroupChatExists = isGroupChatExists;
    }
	
	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new RichMessaging(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static RichMessaging getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */

	private RichMessaging(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
        this.maxLogEntries = RcsSettings.getInstance().getMaxChatLogEntriesPerContact();
	}

    /**
     * Get list of participants into a string
     * 
     * @param session Chat session
     * @return String (contacts are comma separated)
     */
    private String getParticipants(ChatSession session) {
        StringBuffer participants = new StringBuffer();
        if (session.isGroupChat()) {
            for (String contact : session.getParticipants().getList()) {
                if (contact != null) {
                    participants.append(PhoneUtils.extractNumberFromUri(contact) + ";");
                }
            }
        } else {
            participants.append(PhoneUtils.extractNumberFromUri(session.getRemoteContact()));
        }
        return participants.toString();
    }

    /**
     * Get type corresponding to a chat system event
     * 
     * @param session Chat session
     * @return Type
     */
    private int getChatSystemEventType(ChatSession session) {
        if (session.isGroupChat()) {
            return EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE;
        } else {
            return EventsLogApi.TYPE_CHAT_SYSTEM_MESSAGE;
        }
    }

    /**
     * Add a new conference event
     * 
     * @param session Chat session
     * @param contact Contact
     * @param state Conference state
     */
    public void addConferenceEvent(ChatSession session, String contact, String state) {
        int event = -1;
        if (state.equals(User.STATE_BOOTED)) {
            // Contact has lost the session and may rejoin the session after
            event = EventsLogApi.EVENT_DISCONNECT_CHAT;
        } else
        if (state.equals(User.STATE_DEPARTED)) {
            // Contact has left voluntary the session
            event = EventsLogApi.EVENT_LEFT_CHAT;
        } else
        if (state.equals(User.STATE_DISCONNECTED)) {
            // Contact has left voluntary the session
            event = EventsLogApi.EVENT_LEFT_CHAT;
        } else
        if (state.equals(User.STATE_CONNECTED)) {
            // Contact has joined the session
            event = EventsLogApi.EVENT_JOINED_CHAT;
        } else
        if (state.equals(User.STATE_BUSY)) {
            // Contact is busy
            event = EventsLogApi.EVENT_BUSY;
        } else
        if (state.equals(User.STATE_DECLINED)) {
            // Contact has declined the invitation
            event = EventsLogApi.EVENT_DECLINED;
        } else
        if (state.equals(User.STATE_FAILED)) {
            // Contact has declined the invitation or any SIP error related to the contact invitation 
            event = EventsLogApi.EVENT_FAILED;
        }
        
        if (event != -1) {
            addEntry(EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE,
                    session.getSessionID(), session.getContributionID(),
                    null, contact, null, null, null, 0, null, event);
        }
    }
	
	   /**
     * Add an incoming chat session
     * 
     * @param session Chat session
     */
    public void addIncomingChatSession(ChatSession session){
        // Add session entry
        String sessionId = session.getSessionID();
        String chatId = session.getContributionID();
        String participants = getParticipants(session);
        String subject = session.getSubject();
        int type = getChatSystemEventType(session);
        boolean shouldStoreFirstMessage = true;
        /**
         * M: if restart, do not restore first message which is already stored. @{
         */
        if (session.isGroupChat())               
         shouldStoreFirstMessage = false;
               
        if (session.isGroupChat() && isChatExisted(chatId)) {
            setGroupChatExists(true);
        }
        addEntry(type, sessionId, chatId, null, participants, subject, null, null, 0, null,
                EventsLogApi.EVENT_INVITED);

        if (shouldStoreFirstMessage) {
            // Add first message entry
            InstantMessage firstMsg = session.getFirstMessage();
            if (firstMsg != null) {
            	
            	//To format contact  in database correctly
        		String remote = null;
        		remote = PhoneUtils.extractNumberFromUri(firstMsg.getRemote());
        		
        		if (remote != null)
        		{
        			firstMsg.setRemote(remote);

        		}
        		
        		addIncomingChatMessage(firstMsg, session);
        	

                addIncomingChatMessage(firstMsg, session);
            }
        }
        /** @} */
    }

    /**
     * M: if restart, do not restore first message which is already stored. @{
     */
    private boolean isChatExisted(String chatId) {
        boolean result = false;
        String[] project = new String[] {
            RichMessagingData.KEY_STATUS
        };
        
        //1 for KEY status is included in case of time out.
        String selection = "(" + RichMessagingData.KEY_CHAT_ID + "='" + chatId + "')"
        					+ " AND ( "+RichMessagingData.KEY_STATUS + " = 12  OR "+ RichMessagingData.KEY_STATUS + " = 16 " +
        							" OR "+ RichMessagingData.KEY_STATUS + " = 1 )";
        					
        					
        Cursor cursor = cr.query(databaseUri, project, selection, null, null);
        try {
            if (cursor != null) {
                int count = cursor.getCount();
                result = (count != 0 ? true : false);
                if (logger.isActivated()) {
                    logger.debug("isChatExisted() count: " + count);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /** @} */

	/**
	 * Add an incoming file transfer
	 * 
	 * @param contact Contact
	 * @param chatSessionId Chat session ID which may be null if file transfer is outside of a chat session
	 * @param ftSessionId File transfer session ID
	 * @param content File content
	 */
	public void addIncomingFileTransfer(String contact, String chatSessionId, String ftSessionId, MmContent content) {
		// Add session entry
		addEntry(EventsLogApi.TYPE_INCOMING_FILE_TRANSFER, chatSessionId, null, ftSessionId, contact, null, content.getEncoding(), content.getName(), content.getSize(), null, EventsLogApi.STATUS_STARTED);	
	}
	
	   /**
     * Add outgoing chat session
     * 
     * @param session Chat session 
     */
    public void addOutgoingChatSession(ChatSession session){
        // Add session entry
        String sessionId = session.getSessionID();
        String chatId = session.getContributionID();
        String participants = getParticipants(session);
        String subject = session.getSubject();
        int type = getChatSystemEventType(session);
        addEntry(type, sessionId, chatId, null, participants, subject, null, null, 0, null, EventsLogApi.EVENT_INITIATED);

        // Add first message entry
        InstantMessage firstMsg = session.getFirstMessage();
        if (firstMsg != null) {
        	
        	String remote = null;
    		remote = PhoneUtils.extractNumberFromUri(firstMsg.getRemote());
    		firstMsg.setRemote(remote);

            addOutgoingChatMessage(firstMsg, session);
        }
    }
	/**
	 * Add outgoing file transfer
	 * 
	 * @param contact Contact
	 * @param chatSessionId Chat session ID which may be null if file transfer took place outside of a chat session
	 * @param sessionId Session ID
	 * @param filename Filename
	 * @param content File content 
	 */
	public void addOutgoingFileTransfer(String contact, String chatSessionId, String ftSessionId, String fileName, MmContent content){
		// Add session entry
		addEntry(EventsLogApi.TYPE_OUTGOING_FILE_TRANSFER, chatSessionId, null, ftSessionId, contact, fileName, content.getEncoding(), content.getName(), content.getSize(), null, EventsLogApi.EVENT_INITIATED);	
	}
	
	/**
	 * Add incoming chat message
	 * 
	 * @param msg Chat message
	 * @param session Chat session
	 */
	public void addIncomingChatMessage(InstantMessage msg, ChatSession session) {
		// Add message entry
		int type;
		if (session.isGroupChat()) {
			type = EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE;
		} else {
			type = EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE;
		}		
		int status = EventsLogApi.STATUS_RECEIVED;
		if (msg.isImdnDisplayedRequested()){
			status = EventsLogApi.STATUS_REPORT_REQUESTED;
		}
		addEntry(type, session.getSessionID(), session.getContributionID(), msg.getMessageId(),
				msg.getRemote(), msg.getTextMessage(), InstantMessage.MIME_TYPE, msg.getRemote(),
				msg.getTextMessage().getBytes().length, msg.getDate(), status);
	}
	
	/**
	 * Add outgoing chat message
	 * 
	 * @param msg Chat message
	 * @param session Chat session
	 */
	public void addOutgoingChatMessage(InstantMessage msg, ChatSession session){
		// Add session entry
		int type;
		if (session.isGroupChat()){
			type = EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE;
		} else {
			type = EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE;
		}
		addEntry(type, session.getSessionID(), session.getContributionID(),
				msg.getMessageId(), msg.getRemote(), msg.getTextMessage(),
				InstantMessage.MIME_TYPE, msg.getRemote(),
				msg.getTextMessage().getBytes().length, msg.getDate(), EventsLogApi.STATUS_SENT);			
	}
	
	/**
	 * Set the delivery status of a chat message
	 * 
	 * @param msgId Message ID
	 * @param status Status
	 */
	public void setChatMessageDeliveryStatus(String msgId, String status) {
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_DISPLAYED);
		} else
		if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_DELIVERED);			
		} else
		if ((status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_ERROR)) ||
				(status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_FAILED)) ||
					(status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_FORBIDDEN))) {
			setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_FAILED);
		}
	}

    /** M: add server date for delivery status @{ */
    /**
     * Set the delivery status of a chat message
     * 
     * @param msgId Message ID
     * @param status Status
     */
    public void setChatMessageDeliveryStatus(String msgId, String status, long date) {
        if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
            setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_DISPLAYED);
        } else if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
            setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_DELIVERED, date);
        } else if ((status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_ERROR))
                || (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_FAILED))
                || (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_FORBIDDEN))) {
            setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_FAILED);
        }
    }

    /** @} */

	/**
	 * A delivery report "displayed" is requested for a given chat message
	 * 
	 * @param msgId Message ID
	 */
	public void setChatMessageDeliveryRequested(String msgId) {
		setChatMessageDeliveryStatus(msgId, EventsLogApi.STATUS_REPORT_REQUESTED);
	}
		
	/**
	 * Set the delivery status of a chat message
	 * 
	 * @param msgId Message ID
	 * @param status Status
	 */
	private void setChatMessageDeliveryStatus(String msgId, int status) {
		ContentValues values = new ContentValues();
		values.put(RichMessagingData.KEY_STATUS, status);
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_MESSAGE_ID + " = \'" + msgId + "\' and "
			       + RichMessagingData.KEY_STATUS + " !=  " + EventsLogApi.STATUS_DISPLAYED, 
				null);
	}

    /** M: add server date for delivery status @{ */
    /**
     * Set the delivery status of a chat message
     * 
     * @param msgId Message ID
     * @param status Status
     * @param date The date for delivery status
     */
    private void setChatMessageDeliveryStatus(String msgId, int status, long date) {
        ContentValues values = new ContentValues();
        values.put(RichMessagingData.KEY_STATUS, status);
        
        /**
         * M: Modified to create a InstantMessage object with  system date. dont use server time stamp @{
         */
        
        cr.update(databaseUri, values, RichMessagingData.KEY_MESSAGE_ID + " = \'" + msgId
                + "\' and " + RichMessagingData.KEY_STATUS + " !=  "
                + EventsLogApi.STATUS_DISPLAYED, null);
        /**
		 * @}
		 */
    }

    /** @} */

	/**
	 * Add chat session termination
	 * 
	 * @param session Chat session
	 */
	public void addChatSessionTermination(ChatSession session) {
		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_TERMINATED);
	}
	
	/**
     * Add chat session termination
     * 
     * @param session Chat session
     */
    public void addChatSessionTerminationOnQuitGroup(String chatId, String sessionId, String participants) {
             
       addEntry(EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_TERMINATED);
    }
		
	/**
	 * Add a chat session error
	 * 
	 * @param session Chat session
	 */
	public void addChatSessionError(ChatSession session) {
		String sessionId = session.getSessionID();
		String chatId = session.getContributionID();
		String participants = getParticipants(session);
		int type = getChatSystemEventType(session);
		addEntry(type, sessionId, chatId, null, participants, null, null, null, 0, new Date(), EventsLogApi.STATUS_FAILED);
	}
	
    /**
     * Mark a chat session as started
     * 
     * @param session Chat session
     */
    public void markChatSessionStarted(ChatSession session) {
        int type = getChatSystemEventType(session);
        ContentValues values = new ContentValues();
        values.put(RichMessagingData.KEY_CHAT_REJOIN_ID, session.getImSessionIdentity());
        cr.update(databaseUri, values, "(" + RichMessagingData.KEY_CHAT_SESSION_ID + " = \""
                + session.getSessionID() + "\") AND (" + RichMessagingData.KEY_TYPE + " =" + type
                + ")", null);

    }

	/**
	 * Mark a chat session as failed
	 * 
	 * @param sessionId Session ID
	 */
	public void markChatSessionFailed(String sessionId) {
		ContentValues values = new ContentValues();
		values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_FAILED);
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_CHAT_SESSION_ID +" = \""+sessionId+"\"" + " AND " +
				RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE, 
				null);
	}
	
	/**
	 * Mark first message as failed
	 * 
	 * @param sessionId Session ID
	 */
	public void markFirstMessageFailed(String sessionId) {
		ContentValues values = new ContentValues();
		values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_FAILED);
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_CHAT_SESSION_ID +" = \""+sessionId+"\"" + " AND " +
				RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE, 
				null);
	}
	
	/**
	 * Mark a chat message as failed
	 * 
	 * @param msgId Message ID
	 */
	public void markChatMessageFailed(String msgId) {
		ContentValues values = new ContentValues();
		values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_FAILED);
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_MESSAGE_ID +" = \""+msgId+"\"", 
				null);
	}	
	
	/**
	 * Mark a chat message as read or not
	 * 
	 * @param msgId Message ID
	 * @param isRead Read flag
	 */
	public void markChatMessageAsRead(String msgId, boolean isRead) {
		ContentValues values = new ContentValues();
		if (isRead){
			values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_DISPLAYED);
		}else{
			values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_RECEIVED);
		}
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_MESSAGE_ID +" = \""+msgId+"\"", 
				null);
	}
	
	/**
	 * Add a spam message
	 * 
	 * @param msg Chat message
	 */
	public void addSpamMessage(InstantMessage msg) {
		// TODO: 2 queries may be avoided
		String id = SessionIdGenerator.getNewId();
		addEntry(EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE, id, id, msg.getMessageId(), msg.getRemote(), msg.getTextMessage(), InstantMessage.MIME_TYPE, msg.getRemote(), msg.getTextMessage().getBytes().length, msg.getDate(), EventsLogApi.STATUS_RECEIVED);
		markChatMessageAsSpam(msg.getMessageId(), true);
	}
	
	/**
	 * Add incoming chat message
	 * 
	 * @param msg Chat message
	 * @param chatId Chat ID
	 */
	public void addIncomingChatMessage(InstantMessage msg, String chatId) {
		int status = EventsLogApi.STATUS_RECEIVED;
		if (msg.isImdnDisplayedRequested()){
			status = EventsLogApi.STATUS_REPORT_REQUESTED;
		}
		addEntry(EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE, SessionIdGenerator.getNewId(), chatId, msg.getMessageId(), msg.getRemote(), msg.getTextMessage(), InstantMessage.MIME_TYPE, msg.getRemote(), msg.getTextMessage().getBytes().length, msg.getDate(), status);
	}
	
	/**
	 * Mark a chat message as spam or not
	 * 
	 * @param msgId Message ID
	 * @param isSpam Spam flag
	 */
	public void markChatMessageAsSpam(String msgId, boolean isSpam) {
		ContentValues values = new ContentValues();
		if (isSpam){
			values.put(RichMessagingData.KEY_IS_SPAM, EventsLogApi.MESSAGE_IS_SPAM);
		}else{
			values.put(RichMessagingData.KEY_IS_SPAM, EventsLogApi.MESSAGE_IS_NOT_SPAM);
		}
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_MESSAGE_ID +" = \""+msgId+"\"" +" AND " + RichMessagingData.KEY_TYPE + " = " + EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE, 
				null);
	}
	
	/**
	 * Delete all spam messages
	 */
	public void deleteAllSpams(){
		Cursor c = cr.query(databaseUri,
				new String[]{RichMessagingData.KEY_ID},
				RichMessagingData.KEY_IS_SPAM + " = \"" + EventsLogApi.MESSAGE_IS_SPAM +"\"",
				null,
				null);
        if (c != null) {
		while (c.moveToNext()){
			long rowId = c.getLong(0);
			deleteEntry(rowId);
		}
		c.close();
	}
	}

     /**
     * Add a new entry (chat event, chat message or file transfer)
     * 
     * @param type Type of entry
     * @param sessionId Session ID of a chat session or a file transfer session
     * @param chatId Chat ID of a chat session
     * @param messageId Message ID of a chat message 
     * @param contact Contact phone number
     * @param data Content of the entry (an URI for FT or a simple text for IM)
     * @param mimeType MIME type for a file transfer
     * @param name Name of the transfered file
     * @param size Size of the  transfered file
     * @param status Status of the entry
     * @return URI of the new entry
     */
    private Uri addEntry(int type, String sessionId, String chatId, String messageId, String contact, String data, String mimeType, String name, long size, Date date, int status) {
        if (logger.isActivated()){
            logger.debug("Add new entry: type=" + type + ", sessionID=" + sessionId +
                    ", chatID=" + chatId + ", messageID=" + messageId + ", contact=" + contact +
                    ", MIME=" + mimeType + ", status=" + status);
        }
        ContentValues values = new ContentValues();
        values.put(RichMessagingData.KEY_TYPE, type);
        values.put(RichMessagingData.KEY_CHAT_SESSION_ID, sessionId);
        values.put(RichMessagingData.KEY_CHAT_ID, chatId);
        values.put(RichMessagingData.KEY_MESSAGE_ID, messageId);
        values.put(RichMessagingData.KEY_CONTACT, contact);
        values.put(RichMessagingData.KEY_MIME_TYPE, mimeType);
        values.put(RichMessagingData.KEY_TOTAL_SIZE, size);
        values.put(RichMessagingData.KEY_NAME, name);
        values.put(RichMessagingData.KEY_DATA, data);
        values.put(RichMessagingData.KEY_STATUS, status);
        values.put(RichMessagingData.KEY_NUMBER_MESSAGES, recycler(contact)+1);
        values.put(RichMessagingData.KEY_IS_SPAM, EventsLogApi.MESSAGE_IS_NOT_SPAM);
        if(date == null) {
            values.put(RichMessagingData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        } else {
            values.put(RichMessagingData.KEY_TIMESTAMP, date.getTime());
        }
        return cr.insert(databaseUri, values);
    }
    
	/**
	 * Manage the max size of the history for a given contact
	 * 
	 * @param contact Contact
	 * @return History size
	 */
	private int recycler(String contact) {
		// Get first and last message dates for the contact
		Cursor extrem = cr.query(databaseUri, 
				new String[]{"min("+RichMessagingData.KEY_TIMESTAMP+")", "max("+RichMessagingData.KEY_TIMESTAMP+")"}, 
				RichMessagingData.KEY_CONTACT +" = \'"+contact+"\'", 
				null, 
				null);
		long minDate = -1 ,maxDate = -1;
        /**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (extrem != null) {
            if (extrem.moveToFirst()) {
                minDate = extrem.getLong(0);
                maxDate = extrem.getLong(1);
            }
            extrem.close();
        }
        /**
         * @}
         */
		if(logger.isActivated()){
			logger.debug("Recycler : minDate = "+minDate+" maxDate "+ maxDate);
		}
		
		// If no entry for this contact return 0
		if(minDate == -1 && maxDate == -1){
			return 0;
		}
		
		Cursor c = cr.query(databaseUri, 
				new String[] { RichMessagingData.KEY_NUMBER_MESSAGES, RichMessagingData.KEY_CHAT_SESSION_ID, RichMessagingData.KEY_TIMESTAMP },
				RichMessagingData.KEY_CONTACT + " = \'" + contact + "\'"+
						" AND (" + RichMessagingData.KEY_TIMESTAMP+ " = " + minDate + 
						" OR "+ RichMessagingData.KEY_TIMESTAMP + " = " + maxDate+ ")",
				null, 
				RichMessagingData.KEY_TIMESTAMP + " ASC");
		int numberOfMessagesForContact = 0;
		long dateForLastMessage = 0;
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
		if(c == null){
		    return numberOfMessagesForContact;
		}
		/**
		 * @}
		 */
		if(c.moveToLast()){
			numberOfMessagesForContact = c.getInt(0);
			if(logger.isActivated()){
				logger.debug("Recycler : number of messages for this contact = "+numberOfMessagesForContact);
			}
			if(numberOfMessagesForContact < maxLogEntries) {
				// Enough place for another message... do nothing return
				if(logger.isActivated()){
					logger.debug("Recycler : Enough place for another message, do nothing return");
				}
				c.close();
				return numberOfMessagesForContact;
			}
			if(logger.isActivated()){
				logger.debug("Recycler : Not enough place for another message, we will have to remove something");
			}
			// Not enough place for another message... we will have to remove something
			dateForLastMessage = c.getLong(2);
			if(logger.isActivated()){
				logger.debug("Recycler : dateForLastMessage ="+new Date(dateForLastMessage).toString()+" ["+dateForLastMessage+"]");
			}
		}
		int removedMessages = 0;
		if(c.moveToFirst()){
			// Remove the first message and all the associated messages from its session
			String sessionId = c.getString(1);
			long firstDate = c.getLong(2);
			if(logger.isActivated()){
				logger.debug("Recycler : deleting entries for (the first) sessionID : "+sessionId + " for the date : "+new Date(firstDate).toString()+" ["+firstDate+"]");
			}
			removedMessages = cr.delete(databaseUri, 
					RichMessagingData.KEY_CHAT_SESSION_ID + " = \'" + sessionId+ "\'", 
					null);
			if(logger.isActivated()){
				logger.debug("Recycler : messages removed : "+removedMessages);
			}
			
			// We also will have to set the new number of message after removing, for the last entry
			if(logger.isActivated()){
				logger.debug("Recycler : set the new number of messages after removing...");
			}
			ContentValues values = new ContentValues();
			numberOfMessagesForContact -= removedMessages;
			if(logger.isActivated()){
				logger.debug("Recycler : new number of message after deletion : "+numberOfMessagesForContact);
			}
			values.put(RichMessagingData.KEY_NUMBER_MESSAGES, numberOfMessagesForContact);
			int updatedRows = cr.update(databaseUri, 
					values, 
					RichMessagingData.KEY_CONTACT +" = \'"+contact+"\' AND "+RichMessagingData.KEY_TIMESTAMP+ " = "+dateForLastMessage, 
					null);
			if(logger.isActivated()){
				logger.debug("Recycler : updated rows for the contact (must be 1) : "+updatedRows);
			}
		}
		c.close();
		return numberOfMessagesForContact;
	}
	
	/**
	 * Update file transfer status
	 * 
	 * @param sessionId Session Id
	 * @param status New status
	 */
	public void updateFileTransferStatus(String sessionId, int status) {
		ContentValues values = new ContentValues();
		values.put(RichMessagingData.KEY_STATUS, status);
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_MESSAGE_ID + " = " + sessionId, 
				null);
	}
	
	/**
	 * Update file transfer download progress
	 * 
	 * @param sessionId Session Id
	 * @param size Downloaded size
	 * @param totalSize Total size to download 
	 */
	public void updateFileTransferProgress(String sessionId, long size, long totalSize) {
		ContentValues values = new ContentValues();
		
		Cursor cursor = cr.query(RichMessagingData.CONTENT_URI, 
				new String[]{RichMessagingData.KEY_SIZE}, 
				RichMessagingData.KEY_MESSAGE_ID + "='" + sessionId + "'",
				null, 
				null);
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                long downloadedSize = cursor.getLong(cursor
                        .getColumnIndexOrThrow(RichMessagingData.KEY_SIZE));
                if ((size >= downloadedSize + totalSize / 10) || size == totalSize) {
                    // Update size if we have done at least 10 more percent from
                    // total size since last update
                    // Or if we are at the end of the download (ensure we update
                    // when transfer is finished)
                    // This is to avoid too much updates, as the ui refreshes
                    // each time
                    values.put(RichMessagingData.KEY_SIZE, size);
                    values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_IN_PROGRESS);
                    cr.update(databaseUri, values, RichMessagingData.KEY_MESSAGE_ID + " = "
                            + sessionId, null);
                }
            }
            cursor.close();
        }
        /**
         * @}
         */
	}

	/**
	 * Update file transfer URL
	 * 
	 * @param sessionId Session Id
	 * @param url File URL
	 */
	public void updateFileTransferUrl(String sessionId, String url) {
		ContentValues values = new ContentValues();
		values.put(RichMessagingData.KEY_DATA, url);
		values.put(RichMessagingData.KEY_STATUS, EventsLogApi.STATUS_TERMINATED);
		cr.update(databaseUri, 
				values, 
				RichMessagingData.KEY_MESSAGE_ID + " = " + sessionId, 
				null);
	}
	
	/**
	 * Delete a file transfer session
	 * 
	 * @param sessionId Session ID
	 * @param contact Contact
	 */
	public void deleteFileTransferSession(String sessionId, String contact) {
		// Count entries to be deleted
		Cursor count = cr.query(databaseUri, null, RichMessagingData.KEY_MESSAGE_ID + " = " + sessionId, null, null);
		int toBeDeletedRows = 0;
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (count != null) {
            count.getCount();
            if (logger.isActivated()) {
                logger.debug("Delete " + toBeDeletedRows + " rows");
            }
            count.close();
        }
        /**
         * @}
         */
		if (toBeDeletedRows==0) {
			return;
		}
		
		
		// Manage recycling
		Cursor c  = cr.query(databaseUri, 
				new String[]{RichMessagingData.KEY_TIMESTAMP, RichMessagingData.KEY_NUMBER_MESSAGES, RichMessagingData.KEY_MESSAGE_ID}, 
				RichMessagingData.KEY_CONTACT+" = \'"+contact+"\'", 
				null, 
				RichMessagingData.KEY_TIMESTAMP + " DESC");
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
		if(c == null){
		    return;
		}
		/**
		 * @}
		 */
		if (c.moveToFirst()) {
			long maxDate = c.getLong(0);
			int numberForLast = c.getInt(1);
			String lastSessionId = c.getString(2);

			ContentValues values = new ContentValues();
			values.put(RichMessagingData.KEY_NUMBER_MESSAGES, numberForLast-toBeDeletedRows);
			// If last entry for this contact equals to this file transfer message
			if (sessionId.equals(lastSessionId)){
				// Update the previous one
				if(c.moveToNext()){
					maxDate = c.getLong(0);
				}
			}
			/*
			 * TODO : 
			 * If no more message exists after deleting this one for this contact,
			 * the update is useless because it will be made on the message to be deleted.
			 */
			int updatedRows = cr.update(databaseUri, 
					values, 
					RichMessagingData.KEY_TIMESTAMP+ " = "+maxDate+" AND "+RichMessagingData.KEY_CONTACT+" = \'"+contact+"\'", 
					null);
			if (logger.isActivated()) {
				logger.debug("DeleteFileTransfer : recycling updated rows (should be 1) : "+updatedRows);
			}
		}
		c.close();
		
		/* Delete entry */
		int deletedRows = cr.delete(databaseUri, 
				RichMessagingData.KEY_MESSAGE_ID + " = " + sessionId, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteFileTransfer : deleted rows (should be 1) : "+deletedRows);
		}
	}
	
	/**
	 * Delete history associated to a contact
	 * 
	 * @param contact Contact
	 */
	public void deleteContactHistory(String contact) {
		String excludeGroupChat = RichMessagingData.KEY_TYPE + "<>" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + " AND " +
			RichMessagingData.KEY_TYPE + "<>" + EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE + " AND " +
			RichMessagingData.KEY_TYPE + "<>" + EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE;
		
		/**
         * M: Modified to search the contact number pattern. @{
         */
		// Delete entries
		int deletedRows = cr.delete(databaseUri, 
				RichMessagingData.KEY_CONTACT + " LIKE '%" + contact + "%' AND " + excludeGroupChat, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteSession: deleted rows : "+deletedRows);
		}	
		 /**
         * @}
         */
	}
	
	/**
	 * Delete a chat session. If file transfer is enclosed in the session, it will also be deleted.
	 * 
	 * @param sessionId Session ID
	 */
	public void deleteChatSession(String sessionId) {
		// Count entries to be deleted
		Cursor count = cr.query(databaseUri, 
				null, 
				RichMessagingData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
				null, 
				null);
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
		int toBeDeletedRows = 0;
		boolean isGroupChat = false;
		String contact = null;
        if (count != null) {
            toBeDeletedRows = count.getCount();
            if (count.moveToFirst()) {
                contact = count.getString(count.getColumnIndex(RichMessagingData.KEY_CONTACT));
                int type = count.getInt(count.getColumnIndex(RichMessagingData.KEY_TYPE));
                if (type >= EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE
                        && type <= EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE) {
                    isGroupChat = true;
                }
            }
            if (logger.isActivated()) {
                logger.debug("DeleteSession: rows to be deleted : " + toBeDeletedRows);
            }
            count.close();
        }
        /**
         * @}
         */
		if (toBeDeletedRows == 0){
			return;
		}
		
		if (!isGroupChat){
			// Manage recycling
			Cursor c  = cr.query(databaseUri, new String[]{
					RichMessagingData.KEY_TIMESTAMP,
					RichMessagingData.KEY_NUMBER_MESSAGES, 
					RichMessagingData.KEY_CHAT_SESSION_ID}, 
					RichMessagingData.KEY_CONTACT+" = \'"+contact+"\'", 
					null, 
					RichMessagingData.KEY_TIMESTAMP + " DESC");
			/**
	         * M: Modified to resolve the null pointer exception. @{
	         */
            if (c == null) {
                return;
            }
            /**
             * @}
             */
			if(c.moveToFirst()){
				long maxDate = c.getLong(0);
				int numberForLast = c.getInt(1);
				String lastSessionId = c.getString(2);
				ContentValues values = new ContentValues();
				values.put(RichMessagingData.KEY_NUMBER_MESSAGES, numberForLast-toBeDeletedRows);

				if(sessionId.equals(lastSessionId)){
					// Find the last message from another session for the same contact
					if(logger.isActivated()){
						logger.debug("DeleteSession : the deleted session is the last one... looking for the previous one for this contact");
					}
					while(c.moveToNext()){
						if(!sessionId.equals(c.getString(2))){
							maxDate = c.getLong(0);
							if(logger.isActivated()){
								logger.debug("DeleteSession : find the previous session with date "+maxDate);
							}
							break;
						}
					}
				}
				if(logger.isActivated()){
					logger.debug("DeleteSession : updating the row of date "+maxDate);
				}
				/*
				 * TODO : 
				 * If no more session exists after deleting this one for this contact,
				 * the update is useless because it will be made on the session to be deleted.
				 */
				int updatedRows = cr.update(databaseUri, 
						values, 
						RichMessagingData.KEY_TIMESTAMP+ " = "+maxDate +" AND "+RichMessagingData.KEY_CONTACT+" = \'"+contact+"\'", 
						null);
				if(logger.isActivated()){
					logger.debug("DeleteSession : recycling updated rows (should be 1) : "+updatedRows);
				}
			}
			c.close();
		}
		
		// Delete entry
		int deletedRows = cr.delete(databaseUri, 
				RichMessagingData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteSession: deleted rows : "+deletedRows);
		}	
	}
	
	/**
	 * Clear the history for a given contact
	 * 
	 * @param contact Contact
	 */
	public void clearHistory(String contact) {
		int deletedRows = cr.delete(databaseUri, RichMessagingData.KEY_CONTACT+"='"+contact+"'", null);
		if (logger.isActivated()) {
			logger.debug("Clear history of contact " + contact + ": deleted rows =" + deletedRows);
		}
	}
	
	/**
	 * Delete an entry (chat message or file transfer) from its row id
	 *  
	 * @param rowId Row ID
	 */
	public void deleteEntry(long rowId) {
		Cursor count = cr.query(Uri.withAppendedPath(databaseUri, ""+rowId),
				null,
				null,
				null,
				null);
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (count == null) {
            return;
        }
        /**
         * @}
         */
		if(count.getCount()==0) {
			count.close();
			return;
		}
		count.moveToFirst();
		String contactNumber = count.getString(count.getColumnIndexOrThrow((RichMessagingData.KEY_CONTACT)));

		// Manage recycling
		Cursor c  = cr.query(databaseUri, new String[]{
				RichMessagingData.KEY_TIMESTAMP,
				RichMessagingData.KEY_NUMBER_MESSAGES, 
				RichMessagingData.KEY_CHAT_SESSION_ID, 
				RichMessagingData.KEY_DATA}, 
				RichMessagingData.KEY_CONTACT + " = \'"+contactNumber + "\'", 
				null, 
				RichMessagingData.KEY_TIMESTAMP + " DESC");
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (c == null) {
            return;
        }
        /**
         * @}
         */
		// Get the first last entry for this contact
		if(c.moveToFirst()){
			long maxDate = c.getLong(0);
			int numberForLast = c.getInt(1);
			String lastSessionId = c.getString(2);
			
			// We are going to delete one message
			ContentValues values = new ContentValues();
			values.put(RichMessagingData.KEY_NUMBER_MESSAGES, numberForLast-1);
			
			// Check if this message has the same sessionID, timestamp and content as the one to be deleted
			String sessionId = count.getString(count.getColumnIndexOrThrow(RichMessagingData.KEY_CHAT_SESSION_ID));
			long date = count.getLong(count.getColumnIndexOrThrow(RichMessagingData.KEY_TIMESTAMP));
			String message = count.getString(count.getColumnIndexOrThrow(RichMessagingData.KEY_DATA));
			if(sessionId.equals(lastSessionId) && (date == maxDate) && message.equals(c.getString(3))){
				/* It's the lastest message for this contact, 
				 * find the previous message for the same contact */
				if(logger.isActivated()){
					logger.debug("DeleteSession : the deleted message is the last one... looking for the previous one for this contact");
				}
				// Find the date of the previous message
				if(c.moveToNext()){
					maxDate = c.getLong(0);
					if (logger.isActivated()) {
						logger.debug("DeleteSession : find the previous message with date "+ maxDate);
					}
				}
			}
			if(logger.isActivated()){
				logger.debug("DeleteSession : updating the row of date "+maxDate);
			}
			/* Update the first message or the previous one with the new number of message for this contact. 
			 * 
			 * TODO :
			 * If the first message is the message to be deleted and no more messages are available for the same contact, 
			 * then the update is useless because it will be made on the message to be deleted.
			 */
			int updatedRows = cr.update(databaseUri, values, 
					RichMessagingData.KEY_TIMESTAMP+ " = "+maxDate
					+" AND "+RichMessagingData.KEY_CONTACT+" = \'"
					+contactNumber
					+"\'", 
					null);
			if(logger.isActivated()){
				logger.debug("DeleteSession : recycling updated rows (should be 1) : "+updatedRows);
			}
		}
		count.close();
		c.close();
		
		// Delete entry
		int deletedRows = cr.delete(Uri.withAppendedPath(
				databaseUri, ""+rowId), 
				null, 
				null);
		if(logger.isActivated()){
			logger.debug("DeleteSession: deleted rows : "+deletedRows);
		}	
	}
	
	/**
	 * Check if a session is terminated
	 * 
	 * @param sessionId Session ID
	 * @return Boolean
	 */
	public boolean isSessionTerminated(String sessionId) {
		Cursor cursor = cr.query(databaseUri, 
				new String[]{RichMessagingData.KEY_STATUS}, 
				RichMessagingData.KEY_CHAT_SESSION_ID + " = " + sessionId, 
				null, 
				RichMessagingData.KEY_TIMESTAMP + " DESC");
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (cursor == null) {
            return false;
        }
        /**
         * @}
         */
		if(cursor.moveToFirst()){
			int status = cursor.getInt(0);
			if(status==EventsLogApi.STATUS_TERMINATED){
				cursor.close();
				return true;
			}
		}
		cursor.close();
		return false;
	}
	
	/**
	 * Get all outgoing messages still marked undisplayed for a given contact
	 * 
	 * @param contact Contact
	 * @return list of ids of the undisplayed messages
	 */
	public List<String> getAllOutgoingUndisplayedMessages(String contact){
		List<String> msgIds = new ArrayList<String>();
		Cursor cursor = cr.query(databaseUri, 
				new String[]{RichMessagingData.KEY_MESSAGE_ID}, 
				RichMessagingData.KEY_CONTACT + "=?" + " AND " + RichMessagingData.KEY_TYPE + "=?" + " AND (" + RichMessagingData.KEY_STATUS + "=?" + " OR " + RichMessagingData.KEY_STATUS + "=?)", 
				new String[]{contact, String.valueOf(EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE), String.valueOf(EventsLogApi.STATUS_SENT), String.valueOf(EventsLogApi.STATUS_DELIVERED)}, 
				null);
		/**
         * M: Modified to resolve the null pointer exception. @{
         */
        if (cursor != null) {
            while (cursor.moveToNext()) {
                msgIds.add(cursor.getString(0));
            }
            cursor.close();
        }
        /**
         * @}
         */
		return msgIds;
	}
	
    /**
     * Get the group chat rejoin ID
     * 
     * @param sessionId Session ID
     * @result Rejoin ID or null
     */
    public String getGroupChatRejoinId(String sessionId) {
        String result = null;
        Cursor cursor = cr.query(databaseUri, new String[] {
            RichMessagingData.KEY_CHAT_REJOIN_ID
        }, "(" + RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "') AND ("
                + RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE
                + ") AND (" + RichMessagingData.KEY_CHAT_REJOIN_ID + " NOT NULL)", null,
                RichMessagingData.KEY_TIMESTAMP + " DESC");
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

	/**
	 * Get the group chat ID
	 * 
	 * @param sessionId Session ID
	 * @result Chat ID or null
	 */
	public String getGroupChatId(String sessionId) {
		String result = null;
    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				RichMessagingData.KEY_CHAT_ID
    			},
    			"(" + RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "') AND (" + 
    				RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
    				RichMessagingData.KEY_CHAT_ID + " NOT NULL)", 
    			null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getString(0);
    	}
    	cursor.close();
    	return result;
	}	

    /**
     * M: Added to resolve the issue of receiving the same messages several
     * times. @{
     */
    /**
     * Weather the message with specify message id is exist in the database
     * 
     * @param messageId The message id of specify message
     * @return True for exist, false for not exist
     */
    public boolean isOneToOneChatMessageExist(String messageId) {
        String where = RichMessagingData.KEY_MESSAGE_ID + "=? ";
        String[] selectionArgs = new String[] {
            messageId
        };
        Cursor cursor = null;
        boolean retVal = false;
        try {
            cursor = cr.query(databaseUri, null, where, selectionArgs, null);
            long size = cursor.getCount();
            retVal = (size > 0 ? true : false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            return retVal;
        }
    }

    /**
     * @}
     */

	/**
	 * Get group chat status
	 * 
	 * @param sessionId Session ID
	 * @return Status
	 */
	public int getGroupChatStatus(String sessionId) {
		int result = -1;
    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				RichMessagingData.KEY_STATUS
    			},
    			"(" + RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "') AND (" + 
    				RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ")", 
    			null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getInt(0);
    	}
    	cursor.close();
    	return result;
	}

	/**
	 * Get the group chat participants
	 * 
	 * @param sessionId Session ID
	 * @result List of contacts
	 */
	public List<String> getGroupChatParticipants(String sessionId) {
    	List<String> result = new ArrayList<String>();
    	String participants = null;
    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				RichMessagingData.KEY_CONTACT
    			},
    			"(" + RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "') AND (" + 
    				RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
    				RichMessagingData.KEY_STATUS + "=" + EventsLogApi.EVENT_INITIATED+ ")", 
    			null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		participants = cursor.getString(0);
    	}
    	cursor.close();
    	
    	if (participants != null) {
    		String[] contacts = participants.split(";");
    		for(int i=0; i < contacts.length; i++) {
    			result.add(contacts[i]);
    		}
    	}
    	return result;
	}
	
	/**
	 * Get the group chat subject
	 * 
	 * @param sessionId Session ID
	 * @result Subject or null
	 */
	public String getGroupChatSubject(String sessionId) {
		String result = null;
    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				RichMessagingData.KEY_DATA
    			},
    			"(" + RichMessagingData.KEY_CHAT_SESSION_ID + "='" + sessionId + "') AND (" + 
    				RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
    				RichMessagingData.KEY_STATUS + "=" + EventsLogApi.EVENT_INITIATED+ ")", 
    			null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
    	if (cursor.moveToFirst()) {
    		result = cursor.getString(0);
    	}
    	cursor.close();
    	return result;
	}

	/**
	 * Get the group chat info
	 * 
	 * @param chatId Chat ID
	 * @result Group chat info
	 */
	public GroupChatInfo getGroupChatInfoFromChatId(String chatId) {
		GroupChatInfo result = null;
    	Cursor cursor = cr.query(databaseUri, 
    			new String[] {
    				RichMessagingData.KEY_CHAT_SESSION_ID,
    				RichMessagingData.KEY_CHAT_REJOIN_ID,
    				RichMessagingData.KEY_CONTACT,
    				RichMessagingData.KEY_DATA
    			},
    			"(" + RichMessagingData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
    				RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND ((" +
    				RichMessagingData.KEY_STATUS + "=" + EventsLogApi.EVENT_INITIATED + ") OR (" +
    				RichMessagingData.KEY_STATUS + "=" + EventsLogApi.EVENT_INVITED + "))", 
    				null, 
    			RichMessagingData.KEY_TIMESTAMP + " DESC");
        /**
         * M: resolve the issue that rejoin id is null, when session is not
         * actually started. @{
         */
        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String participants = cursor.getString(2);
                    List<String> list = new ArrayList<String>();
                    if (participants != null) {
                        String[] contacts = participants.split(";");
                        for (int i = 0; i < contacts.length; i++) {
                            list.add(contacts[i]);
                        }
                    }
                    String rejoinId = cursor.getString(1);
                    if (rejoinId != null) {
                        result = new GroupChatInfo(cursor.getString(0), rejoinId, chatId, list,
                                cursor.getString(3));
                        break;
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        /** @} */
        return result;
	}
	
	public String getGroupChatSubjectByChatID(String chatId){
	    String subject = "";
	    if (logger.isActivated()) {
            logger.debug("getGroupChatSubjectByChatID() entry : ");
        }
	    
	    
	    Cursor cursor = cr.query(databaseUri, 
                new String[] {
                    RichMessagingData.KEY_DATA
                },
                "(" + RichMessagingData.KEY_CHAT_ID + "='" + chatId + "') AND (" + 
                    RichMessagingData.KEY_TYPE + "=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE + ") AND (" +
                    RichMessagingData.KEY_DATA + "<> '' )", 
                    null, null);
	    try {
            if (cursor != null) {
                while (cursor.moveToFirst()) {
                    subject = cursor.getString(0);
                    
                    if (logger.isActivated()) {
                        logger.debug("getGroupChatSubjectByChatID() subject : " + subject);
                    }
                    
                    break;
                }
            }
            else{
                if (logger.isActivated()) {
                    logger.debug("getGroupChatSubjectByChatID()  : no rows found" );
                }   
            }
	    }
	    catch (Exception e) {
	        if (logger.isActivated()) {
                logger.debug("getGroupChatSubjectByChatID() error");
            }  
        }
	    finally {
            if (cursor != null) {
                cursor.close();
            }
        }
	    
	    return subject;
	    
	}
}
