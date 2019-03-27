#adupsfota
ifeq ($(strip $(MTK_ADUPS_FOTA_SUPPORT)), yes)
BASE_PATH := $(call my-dir)
include $(CLEAR_VARS)

include $(BASE_PATH)/AdupsFota/Android.mk
include $(BASE_PATH)/AdupsFotaReboot/Android.mk
endif
