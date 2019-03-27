LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES = \
    display_test_slt.c  \

LOCAL_C_INCLUDES += \
 	$(MTK_PATH_SOURCE)/kernel/include \
 	$(MTK_PATH_SOURCE)/kernel/drivers/video \
    $(TOPDIR)/kernel/include \
    $(TOPDIR)/system/core/include \

LOCAL_MODULE_TAGS := eng
LOCAL_MODULE := display_slt

LOCAL_SHARED_LIBRARIES := libcutils libc libmmprofile libion
include $(BUILD_EXECUTABLE)
