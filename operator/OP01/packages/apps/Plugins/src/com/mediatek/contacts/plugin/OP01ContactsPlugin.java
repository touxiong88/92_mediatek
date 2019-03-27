
package com.mediatek.contacts.plugin;

import com.android.contacts.ext.CallDetailExtension;
import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.ext.ContactDetailExtension;
import com.android.contacts.ext.ContactListExtension;
import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.ext.DialPadExtension;
import com.android.contacts.ext.DialtactsExtension;
import com.android.contacts.ext.QuickContactExtension;
import com.android.contacts.ext.SpeedDialExtension;

public class OP01ContactsPlugin extends ContactPluginDefault {
    public CallDetailExtension createCallDetailExtension() {
        return new OP01CallDetailExtension();
    }

    public ContactAccountExtension createContactAccountExtension() {
        return new OP01ContactAccountExtension(); 
    }

    public ContactDetailExtension createContactDetailExtension() {
        return new OP01ContactDetailExtension();
    }

    public ContactListExtension createContactListExtension() {
        return new OP01ContactListExtension();
    }

    public DialPadExtension createDialPadExtension() {
        return new OP01DialPadExtension();
    }

    public DialtactsExtension createDialtactsExtension() {
        return new OP01DialtactsExtension();
    }

    public SpeedDialExtension createSpeedDialExtension() {
        return new OP01SpeedDialExtension();
    }    

    public QuickContactExtension createQuickContactExtension() {
        return new OP01QuickContactExtension();
    }
}
