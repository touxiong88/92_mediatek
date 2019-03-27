LOCAL_PATH := $(my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
   dc_removal_flt.c
   
LOCAL_ARM_MODE := arm   

LOCAL_PRELINK_MODULE := false
	
LOCAL_MODULE := libaudiodcrflt

include $(BUILD_STATIC_LIBRARY)
