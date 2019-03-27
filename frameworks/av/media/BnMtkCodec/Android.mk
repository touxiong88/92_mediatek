LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    BnMtkCodec.cpp \
    CAPEWrapper.cpp

LOCAL_C_INCLUDES:= \
	$(TOP)/frameworks/native/include \
        $(TOP)/mediatek/external/apedec \
        $(TOP)/mediatek/frameworks/av/media/libstagefright/include/omx_core


LOCAL_SHARED_LIBRARIES :=       \
        libbinder               \
        libutils                \
        libcutils               \
        libdl                   \
        libui



LOCAL_STATIC_LIBRARIES :=	\
	libapedec_mtk

  
LOCAL_PRELINK_MODULE:= false
ifeq ($(strip $(MTK_HIGH_RESOLUTION_AUDIO_SUPPORT)),yes)
    LOCAL_CFLAGS += -DMTK_24BIT_AUDIO_SUPPORT
endif
LOCAL_MODULE := libBnMtkCodec

include $(BUILD_SHARED_LIBRARY)
