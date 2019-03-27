/*
 * Copyright (C) 2010 The Android Open Source Project
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
 */
package android.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

///M: For BT debug ALPS00646238 @{
import java.io.CharArrayWriter;
import java.io.PrintWriter;
/// @}

/**
 * This state machine is used to serialize the connections
 * to a particular profile. Currently, we only allow one device
 * to be connected to a particular profile.
 * States:
 *      {@link StableState} : No pending commands. Send the
 *      command to the appropriate remote device specific state machine.
 *
 *      {@link PendingCommandState} : A profile connection / disconnection
 *      command is being executed. This will result in a profile state
 *      change. Defer all commands.
 * @hide
 */

public class BluetoothProfileState extends StateMachine {
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothProfileState";

    public static final int HFP = 0;
    public static final int A2DP = 1;
    public static final int HID = 2;

    static final int TRANSITION_TO_STABLE = 100;

///M: For BT debug ALPS00646238 @{
    private static final int DUMP_TRIGGER_COUNT = 2;
    private int mCounter = 0;
/// @}

    private int mProfile;
    private BluetoothDevice mPendingDevice;
    private PendingCommandState mPendingCommandState = new PendingCommandState();
    private StableState mStableState = new StableState();

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) {
                return;
            }
            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                if (mProfile == HFP && (newState == BluetoothProfile.STATE_CONNECTED ||
                    newState == BluetoothProfile.STATE_DISCONNECTED)) {
                    sendMessage(TRANSITION_TO_STABLE);
                }
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                if (mProfile == A2DP && (newState == BluetoothProfile.STATE_CONNECTED ||
                    newState == BluetoothProfile.STATE_DISCONNECTED)) {
                    sendMessage(TRANSITION_TO_STABLE);
                }
            } else if (action.equals(BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                if (mProfile == HID && (newState == BluetoothProfile.STATE_CONNECTED ||
                    newState == BluetoothProfile.STATE_DISCONNECTED)) {
                    sendMessage(TRANSITION_TO_STABLE);
                }
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
            	// This is technically not needed, but we can get stuck sometimes.
                // For example, if incoming A2DP fails, we are not informed by Bluez
                /* blueangle is not need handle this intent
                if (device.equals(mPendingDevice)) {
                    sendMessage(TRANSITION_TO_STABLE);
                }
                */
            }
        }
    };

    public BluetoothProfileState(Context context, int profile) {
        super("BluetoothProfileState:" + profile);
        mProfile = profile;
        addState(mStableState);
        addState(mPendingCommandState);
        setInitialState(mStableState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(mBroadcastReceiver, filter);
    }

    private class StableState extends State {
        @Override
        public void enter() {
            log("Entering Stable State");
            mPendingDevice = null;
            ///M: For BT debug ALPS00646238 @{
            mCounter = 0;
            /// @}
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what != TRANSITION_TO_STABLE) {
                transitionTo(mPendingCommandState);
            }
            return true;
        }
    }

    private class PendingCommandState extends State {
        @Override
        public void enter() {
            log("Entering PendingCommandState State: Profile = "+ mProfile + ", Instance = " +BluetoothProfileState.this);
            dispatchMessage(getCurrentMessage());
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what == TRANSITION_TO_STABLE) {
                transitionTo(mStableState);
            } else {
                dispatchMessage(msg);
            }
            return true;
        }

        private void dispatchMessage(Message msg) {
            BluetoothDeviceProfileState deviceProfileMgr =
              (BluetoothDeviceProfileState)msg.obj;
            int cmd = msg.arg1;
            
            ///M: For BT debug ALPS00646238 @{
            log("dispatchMessage(): Cmd = "+ cmd +", mPendingDevice = " + mPendingDevice + ", deviceProfileMgr = "+ deviceProfileMgr.getDevice());
            /// @}

            if (mPendingDevice == null || mPendingDevice.equals(deviceProfileMgr.getDevice())) {
                mPendingDevice = deviceProfileMgr.getDevice();
                deviceProfileMgr.sendMessage(cmd);

                ///M: For BT debug ALPS00646238 @{
                if(BluetoothDeviceProfileState.DISCONNECT_A2DP_OUTGOING == cmd) {
                    mCounter++;
                    log("dispatchMessage(): DISCONNECT_A2DP_OUTGOING message sent to BluetoothDeviceProfileState, count = "+ mCounter);

                    if(mCounter >= DUMP_TRIGGER_COUNT) { ///Try dump state machine processed messages
                        CharArrayWriter cw = new CharArrayWriter(100);
                        PrintWriter pw = new PrintWriter(cw);
                        deviceProfileMgr.dump(null, pw, null);
                        log("Dump BluetoothDeviceProfileState Info = \n" + cw);
                    }
                }
                /// @}

            } else {
                log("dispatchMessage(): Message deferred = " + msg);
                Message deferMsg = new Message();
                deferMsg.arg1 = cmd;
                deferMsg.obj = deviceProfileMgr;
                deferMessage(deferMsg);
            }
        }
    }

    private void log(String message) {
        if (DBG) {
            Log.i(TAG, "Message:" + message);
        }
    }
}
