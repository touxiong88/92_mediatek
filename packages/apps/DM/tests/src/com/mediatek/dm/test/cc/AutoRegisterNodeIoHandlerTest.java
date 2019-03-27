/**
 * 
 */
package com.mediatek.dm.test.cc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import com.mediatek.dm.DmService;
import com.mediatek.dm.cc.DmAutoRegisterNodeIoHandler;
import com.redbend.vdm.VdmException;

import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

/**
 * @author MTK80987
 *
 */
public class AutoRegisterNodeIoHandlerTest extends AndroidTestCase {
	private static final String TAG = "[AutoRegisterNodeIoHandlerTest]";
	private Context mContext;
	private static String TREE_URI_SMSC = "AutoRegSMSC";
	private static String TREE_URI_SMPORT = "AutoRegSMSport";
	private static final int ARG0 = 0;
	private static final int UNEXCEPTED_RET = 0;
	private static final int MAX_LEN = 1000;
	
	protected void setUp() throws Exception {
		super.setUp();
		mContext = getContext();
		prepare();
	}
	
	private void prepare() throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
		Log.i(TAG, "prepare the test enviroment");
		
		if (DmService.getInstance() != null) {
			Object obj = Class.forName(DmService.class.getName()).newInstance();
			Class<?> cls = obj.getClass();
			Field field = cls.getDeclaredField("sCCStoredParams");
			field.setAccessible(true);
			
			Map<String, String> map = new HashMap<String, String>();
			field.set(obj, map);
		}
	}
	
	
	public void testRead() throws VdmException {
		Log.i(TAG, "test read function begin");
		
		DmAutoRegisterNodeIoHandler handler = new DmAutoRegisterNodeIoHandler(mContext,
				Uri.parse(TREE_URI_SMSC));
		byte[] data = new byte[MAX_LEN];
		int ret = handler.read(ARG0, data);

		Assert.assertFalse(ret<UNEXCEPTED_RET);
		String str = new String(data);
		if (ret != 0 ) {
			handler.write(ARG0, data, str.length());
		}
	}

	
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
