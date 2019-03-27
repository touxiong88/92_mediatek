ifneq ($(strip $(MTK_PLATFORM)),)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES:=       \
	audiocommand.cpp
	
LOCAL_C_INCLUDES=       \
       $(MTK_PATH_SOURCE)/frameworks/base/include/media

       
LOCAL_C_INCLUDES+=       \
    $(MTK_PATH_SOURCE)/platform/common/hardware/audio/aud_drv \
    $(MTK_PATH_SOURCE)/platform/common/hardware/audio/include \
    $(MTK_PATH_SOURCE)/platform/common/hardware/audio/speech_driver \
    $(MTK_PATH_PLATFORM)/hardware/audio/aud_drv \
    $(MTK_PATH_PLATFORM)/hardware/audio/include \
    $(MTK_PATH_PLATFORM)/hardware/audio/speech_driver \
    $(MTK_PATH_SOURCE)/external/aee/binary/inc

LOCAL_SHARED_LIBRARIES := libcutils libutils libbinder libmedia libaudioflinger

LOCAL_SHARED_LIBRARIES += libaudio.primary.default libaed

LOCAL_MODULE:= audiocommand

include $(BUILD_EXECUTABLE)
endif
