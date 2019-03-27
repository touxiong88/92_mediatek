
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	GraphicBufferExtra.cpp \
	GraphicBufferExtra_hal.cpp
	
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/include
   
#LOCAL_SHARED_LIBRARIES := \
#	libcutils \
#	libhardware \
#	libutils \
#	libdl \

LOCAL_MODULE:= libgralloc_extra

include $(BUILD_STATIC_LIBRARY)

