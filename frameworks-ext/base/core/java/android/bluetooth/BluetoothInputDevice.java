/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*Add by Mtk80684, 2011.12.26*/
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
/*Mtk80684, 2011.12.26 end*/

/**
 * This class provides the public APIs to control the Bluetooth Input
 * Device Profile.
 *
 *<p>BluetoothInputDevice is a proxy object for controlling the Bluetooth
 * Service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothInputDevice proxy object.
 *
 *<p>Each method is protected with its appropriate permission.
 *@hide
 */
public final class BluetoothInputDevice implements BluetoothProfile {
    private static final String TAG = "BluetoothInputDevice";
    private static final boolean DBG = true;
	
	/**
		 * Intent used to broadcast the change in connection state of the Input
		 * Device profile.
		 *
		 * <p>This intent will have 3 extras:
		 * <ul>
		 *	 <li> {@link #EXTRA_STATE} - The current state of the profile. </li>
		 *	 <li> {@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.</li>
		 *	 <li> {@link BluetoothDevice#EXTRA_DEVICE} - The remote device. </li>
		 * </ul>
		 *
		 * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of
		 * {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING},
		 * {@link #STATE_CONNECTED}, {@link #STATE_DISCONNECTING}.
		 *
		 * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to
		 * receive.
		 */
		@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
		public static final String ACTION_CONNECTION_STATE_CHANGED =
			"android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
	
		/**
		 * @hide
		 */
		@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
		public static final String ACTION_PROTOCOL_MODE_CHANGED =
			"android.bluetooth.input.profile.action.PROTOCOL_MODE_CHANGED";
	
	
		/**
		 * @hide
		 */
		@SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
		public static final String ACTION_VIRTUAL_UNPLUG_STATUS =
			"android.bluetooth.input.profile.action.VIRTUAL_UNPLUG_STATUS";
	
	
		/**
		 * Return codes for the connect and disconnect Bluez / Dbus calls.
		 * @hide
		 */
		public static final int INPUT_DISCONNECT_FAILED_NOT_CONNECTED = 5000;
	
		/**
		 * @hide
		 */
		public static final int INPUT_CONNECT_FAILED_ALREADY_CONNECTED = 5001;
	
		/**
		 * @hide
		 */
		public static final int INPUT_CONNECT_FAILED_ATTEMPT_FAILED = 5002;
	
		/**
		 * @hide
		 */
		public static final int INPUT_OPERATION_GENERIC_FAILURE = 5003;
	
		/**
		 * @hide
		 */
		public static final int INPUT_OPERATION_SUCCESS = 5004;
	
		/**
		 * @hide
		 */
		public static final int PROTOCOL_REPORT_MODE = 0;
	
		/**
		 * @hide
		 */
		public static final int PROTOCOL_BOOT_MODE = 1;
	
		/**
		 * @hide
		 */
		public static final int PROTOCOL_UNSUPPORTED_MODE = 255;
	
		/*	int reportType, int reportType, int bufferSize */
		/**
		 * @hide
		 */
		public static final byte REPORT_TYPE_INPUT = 1;
	
		/**
		 * @hide
		 */
		public static final byte REPORT_TYPE_OUTPUT = 2;
	
		/**
		 * @hide
		 */
		public static final byte REPORT_TYPE_FEATURE = 3;
	
		/**
		 * @hide
		 */
		public static final int VIRTUAL_UNPLUG_STATUS_SUCCESS = 0;
	
		/**
		 * @hide
		 */
		public static final int VIRTUAL_UNPLUG_STATUS_FAIL = 1;
	
		/**
		 * @hide
		 */
		public static final String EXTRA_PROTOCOL_MODE = "android.bluetooth.BluetoothInputDevice.extra.PROTOCOL_MODE";
	
		/**
		 * @hide
		 */
		public static final String EXTRA_REPORT_TYPE = "android.bluetooth.BluetoothInputDevice.extra.REPORT_TYPE";
	
		/**
		 * @hide
		 */
		public static final String EXTRA_REPORT_ID = "android.bluetooth.BluetoothInputDevice.extra.REPORT_ID";
	
		/**
		 * @hide
		 */
		public static final String EXTRA_REPORT_BUFFER_SIZE = "android.bluetooth.BluetoothInputDevice.extra.REPORT_BUFFER_SIZE";
	
		/**
		 * @hide
		 */
		public static final String EXTRA_REPORT = "android.bluetooth.BluetoothInputDevice.extra.REPORT";
	
		/**
		 * @hide
		 */
		public static final String EXTRA_VIRTUAL_UNPLUG_STATUS = "android.bluetooth.BluetoothInputDevice.extra.VIRTUAL_UNPLUG_STATUS";


    public static final String ACTION_BIND_SERVICE = "android.bluetooth.input.profile.action.ACTION_BIND_SERVICE";


    private ServiceListener mServiceListener;
    private BluetoothAdapter mAdapter;
    private IBluetooth mService;
	
    /*Add by Mtk80684, 2011.12.26*/
    private static IBluetoothHid mHidService;
    private final Context mContext;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
	    Log.v(TAG, "onServiceConnected");
            mHidService = IBluetoothHid.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
	    Log.v(TAG, "onServiceDisconnected");
            mHidService = null;
        }
    };
	
private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BIND_SERVICE.equals(intent.getAction())) {
	    		if(DBG)Log.i(TAG,"receiver receives ACTION_BIND_SERVICE intent");
			Log.v(TAG, "Bind to Bluetooth HID Service");
			if (!mContext.bindService(
				new Intent(IBluetoothHid.class.getName()), mConnection, 0)) {
					Log.e(TAG, "Could not bind to Bluetooth HID Service");
				}
	    	 }
         }        
};
    /*Mtk80684, 2011.12.26 end*/

    /**
     * Create a BluetoothInputDevice proxy object for interacting with the local
     * Bluetooth Service which handles the InputDevice profile
     *
     */
    /*package*/ BluetoothInputDevice(Context context, ServiceListener l) {
        IBinder b = ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE);
        mServiceListener = l;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (b != null) {
            mService = IBluetooth.Stub.asInterface(b);
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.INPUT_DEVICE, this);
            }
        } else {
            Log.w(TAG, "Bluetooth Service not available!");

            // Instead of throwing an exception which prevents people from going
            // into Wireless settings in the emulator. Let it crash later when it is actually used.
            mService = null;
        }
		/*Add by Mtk80684, 2011.12.26*/
		mContext = context;
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_BIND_SERVICE);
		mContext.registerReceiver(mReceiver, filter);
		
		Log.v(TAG, "Bind to Bluetooth HID Service");
		if (!mContext.bindService(
			new Intent(IBluetoothHid.class.getName()), mConnection, 0)) {
				Log.e(TAG, "Could not bind to Bluetooth HID Service");
		}
		
		/*Mtk80684, 2011.12.26 end*/
    }

    /*package*/ void close() {
        mServiceListener = null;
		/*Add by Mtk80684, 2011.12.26*/
		mContext.unregisterReceiver(mReceiver);
		
		try {
			Log.v(TAG, "Unbind Bluetooth Hid service");
			if (mHidService != null) {
				mHidService = null;
			}
		
			if (mConnection != null) {
				mContext.unbindService(mConnection);
				mConnection = null;
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Exception occurred in close(): " + e);
		}
		
		/*Mtk80684, 2011.12.26 end*/
    }

    /**
     * Initiate connection to a profile of the remote bluetooth device.
     *
     * <p> The system supports connection to multiple input devices.
     *
     * <p> This API returns false in scenarios like the profile on the
     * device is already connected or Bluetooth is not turned on.
     * When this API returns true, it is guaranteed that
     * connection state intent for the profile will be broadcasted with
     * the state. Users can get the connection state of the profile
     * from this intent.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     * permission.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean connect(BluetoothDevice device) {
        if (DBG) log("connect(" + device + ")");
	/*Modify by Mtk80684, 2011.12.26*/
        //if (mService != null && isEnabled() &&
        if (mHidService != null && isEnabled() &&
            isValidDevice(device)) {
            try {
                //return mService.connectInputDevice(device);
                mHidService.connect(device);
				return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        //if (mService == null) Log.w(TAG, "Proxy not attached to service");
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
	/*Mtk80684, 2011.12.26 end*/
        return false;
    }

    /**
     * Initiate disconnection from a profile
     *
     * <p> This API will return false in scenarios like the profile on the
     * Bluetooth device is not in connected state etc. When this API returns,
     * true, it is guaranteed that the connection state change
     * intent will be broadcasted with the state. Users can get the
     * disconnection state of the profile from this intent.
     *
     * <p> If the disconnection is initiated by a remote device, the state
     * will transition from {@link #STATE_CONNECTED} to
     * {@link #STATE_DISCONNECTED}. If the disconnect is initiated by the
     * host (local) device the state will transition from
     * {@link #STATE_CONNECTED} to state {@link #STATE_DISCONNECTING} to
     * state {@link #STATE_DISCONNECTED}. The transition to
     * {@link #STATE_DISCONNECTING} can be used to distinguish between the
     * two scenarios.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     * permission.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean disconnect(BluetoothDevice device) {
        if (DBG) log("disconnect(" + device + ")");
	/*Modify by Mtk80684, 2011.12.26*/
        //if (mService != null && isEnabled() &&
        if (mHidService != null && isEnabled() &&
            isValidDevice(device)) {
            try {
                //return mService.disconnectInputDevice(device);
                mHidService.disconnect(device);
				return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        //if (mService == null) Log.w(TAG, "Proxy not attached to service");
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
	/*Mtk80684, 2011.12.26 end*/
        return false;
    }
		
    /**
     * {@inheritDoc}
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) log("getConnectedDevices()");
	/*Modify by Mtk80684, 2011.12.26*/
        //if (mService != null && isEnabled()) {
        if (mHidService != null && isEnabled()) {
            try {
                //return mService.getConnectedInputDevices();
		    	return Collections.unmodifiableList(new ArrayList<BluetoothDevice>(Arrays.asList(mHidService.getCurrentDevices())));
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        //if (mService == null) Log.w(TAG, "Proxy not attached to service");
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
	/*Mtk80684, 2011.12.26 end*/
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (DBG) log("getDevicesMatchingStates()");
        if (mService != null && isEnabled()) {
            try {
                return mService.getInputDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * {@inheritDoc}
     */
    public int getConnectionState(BluetoothDevice device) {
        if (DBG) log("getState(" + device + ")");
	/*Modify by Mtk80684, 2011.12.26*/
        //if (mService != null && isEnabled()) {
        if (mHidService != null && isEnabled()
            && isValidDevice(device)) {
            try {
                //return mService.getInputDeviceConnectionState(device);
                return mHidService.getState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        //if (mService == null) Log.w(TAG, "Proxy not attached to service");
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
	/*Mtk80684, 2011.12.26 end*/
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Set priority of the profile
     *
     * <p> The device should already be paired.
     *  Priority can be one of {@link #PRIORITY_ON} or
     * {@link #PRIORITY_OFF},
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     * permission.
     *
     * @param device Paired bluetooth device
     * @param priority
     * @return true if priority is set, false on error
     * @hide
     */
    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) log("setPriority(" + device + ", " + priority + ")");
        if (mService != null && isEnabled()
            && isValidDevice(device)) {
            if (priority != BluetoothProfile.PRIORITY_OFF &&
                priority != BluetoothProfile.PRIORITY_ON) {
              return false;
            }
            try {
                return mService.setInputDevicePriority(device, priority);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Get the priority of the profile.
     *
     * <p> The priority can be any of:
     * {@link #PRIORITY_AUTO_CONNECT}, {@link #PRIORITY_OFF},
     * {@link #PRIORITY_ON}, {@link #PRIORITY_UNDEFINED}
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device Bluetooth device
     * @return priority of the device
     * @hide
     */
    public int getPriority(BluetoothDevice device) {
        if (DBG) log("getPriority(" + device + ")");
        if (mService != null && isEnabled()
            && isValidDevice(device)) {
            try {
                return mService.getInputDevicePriority(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.PRIORITY_OFF;
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return BluetoothProfile.PRIORITY_OFF;
    }

    private boolean isEnabled() {
       if (mAdapter.getState() == BluetoothAdapter.STATE_ON) return true;
       return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
       if (device == null) return false;

       if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
       return false;
    }

    /**
     * Initiate virtual unplug for a HID input device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean virtualUnplug(BluetoothDevice device) {
        if (DBG) log("virtualUnplug(" + device + ")");
        if (mHidService != null && isEnabled() && isValidDevice(device)) {
            try {
                mHidService.virtualUnplug(device);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }

        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
        return false;

    }

    /**
    * Send Get_Protocol_Mode command to the connected HID input device.
    *
    * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
    *
    * @param device Remote Bluetooth Device
    * @return false on immediate error,
    *true otherwise
    * @hide
    */
    public boolean getProtocolMode(BluetoothDevice device) {
        if (DBG) log("getProtocolMode(" + device + ")");
        if (mHidService != null && isEnabled() && isValidDevice(device)) {
            try {
                mHidService.getProtocolMode(device);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
            return false;
    }

    /**
     * Send Set_Protocol_Mode command to the connected HID input device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean setProtocolMode(BluetoothDevice device, int protocolMode) {
        if (DBG) log("setProtocolMode(" + device + ")");
        if (mHidService != null && isEnabled() && isValidDevice(device)) {
            try {
                mHidService.setProtocolMode(device, protocolMode);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Send Get_Report command to the connected HID input device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @param reportType Report type
     * @param reportId Report ID
     * @param bufferSize Report receiving buffer size
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean getReport(BluetoothDevice device, byte reportType, byte reportId, int bufferSize) {
        if (DBG) log("getReport(" + device + "), reportType=" + reportType + " reportId=" + reportId + "bufferSize=" + bufferSize);
        if (mHidService != null && isEnabled() && isValidDevice(device)) {
            try {
                mHidService.getReport(device, reportType, reportId, bufferSize);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Send Set_Report command to the connected HID input device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @param reportType Report type
     * @param report Report receiving buffer size
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean setReport(BluetoothDevice device, byte reportType, String report) {
        if (DBG) log("setReport(" + device + "), reportType=" + reportType + " report=" + report);
        if (mHidService != null && isEnabled() && isValidDevice(device)) {
            try {
                mHidService.setReport(device, reportType, report);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    /**
     * Send Send_Data command to the connected HID input device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param device Remote Bluetooth Device
     * @param data Data to send
     * @return false on immediate error,
     *               true otherwise
     * @hide
     */
    public boolean sendData(BluetoothDevice device, String report) {
        if (DBG) log("sendData(" + device + "), report=" + report);
        if (mHidService != null && isEnabled() && isValidDevice(device)) {
            try {
                mHidService.sendData(device, report);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mHidService == null) Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    private static void log(String msg) {
      Log.d(TAG, msg);
    }
}
