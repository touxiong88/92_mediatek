

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Module name should match apk name to be installed
LOCAL_MODULE := Lingxi
LOCAL_MODULE_TAGS := optional
ifeq ($(strip $(GEMINI)), yes)
LOCAL_SRC_FILES := Lingxi_dual.apk
else
LOCAL_SRC_FILES := Lingxi.apk
endif
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_PATH := $(TARGET_OUT)/vendor/operator/app
include $(BUILD_PREBUILT)

