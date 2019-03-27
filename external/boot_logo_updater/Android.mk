# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.

# MediaTek Inc. (C) 2010. All rights reserved.
#
# BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
# THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
# RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
# AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
# NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
# SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
# SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
# THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
# THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
# CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
# SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
# STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
# CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
# AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
# OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
# MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
#
# The following software/firmware and/or related documentation ("MediaTek Software")
# have been modified by MediaTek Inc. All revisions are subject to any receiver's
# applicable license agreements with MediaTek Inc.



#########################################################
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_SRC_FILES := boot_logo_updater.c


LOCAL_STATIC_LIBRARIES := libc libcutils
LOCAL_SHARED_LIBRARIES := libcutils libc libstdc++ libz libdl liblog

LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
LOCAL_MODULE := boot_logo_updater
LOCAL_MODULE_PATH := $(TARGET_OUT_BIN)
LOCAL_C_INCLUDES += $(TOP)/external/zlib/
include $(BUILD_EXECUTABLE)


#########################################################
ifneq ($(strip $(MTK_PLATFORM)),)
include $(CLEAR_VARS)
BOOT_LOGO_DIR := $(MTK_ROOT_CUSTOM_OUT)/lk/logo
BMP_TO_RAW := $(BOOT_LOGO_DIR)/tool/bmp_to_raw
BOOT_LOGO_IMAGE := $(BOOT_LOGO_DIR)/boot_logo

$(info BOOT_LOGO_DIR ------$(BOOT_LOGO_DIR))
$(info BMP_TO_RAW   ------$(BMP_TO_RAW))
$(info BOOT_LOGO_IMAGE ----$(BOOT_LOGO_IMAGE))

LOCAL_MODULE := boot_logo
LOCAL_MODULE_CLASS := DATA
LOCAL_MODULE_PATH := $(TARGET_OUT)/media/images

LOCAL_GENERATE_CUSTOM_FOLDER := custom:lk/logo
$(BOOT_LOGO_IMAGE): $(BOOT_LOGO_DIR)/$(BOOT_LOGO)/$(BOOT_LOGO)_kernel.bmp $(LOCAL_PATH)/Android.mk | $(BMP_TO_RAW)
	$(hide)chmod a+x $(BMP_TO_RAW)
	$(hide)$(BMP_TO_RAW) $@ $<

LOCAL_SRC_FILES := custom/$(LOCAL_MODULE)
$(LOCAL_PATH)/$(LOCAL_SRC_FILES): $(BOOT_LOGO_IMAGE) $(LOCAL_PATH)/Android.mk | $(LOCAL_PATH)/custom
include $(BUILD_PREBUILT)
# Vanzo:songlixin on: Wed, 13 Jul 2011 15:02:41 +0800
# to support bootup/shutdown animation and mp3
define expand_prebuilt_res
    $(foreach file1,$(1), \
    $(eval include $(CLEAR_VARS)) \
    $(eval LOCAL_MODULE := $(file1)) \
    $(eval LOCAL_MODULE_TAGS := optional) \
    $(eval LOCAL_MODULE_CLASS := DATA) \
    $(eval LOCAL_MODULE_PATH := $(TARGET_OUT)/media) \
    $(eval LOCAL_SRC_FILES := $(LOCAL_MODULE)) \
    $(eval HAVE_CUST_FILE := $(shell test -f $(LOCAL_PATH)/$(LOCAL_MODULE) && echo yes)) \
    $(eval ifeq ($(HAVE_CUST_FILE), yes)
        include $(BUILD_PREBUILT)
    endif) \
  )
endef

copy_from := bootanimation.zip bootaudio.mp3 shutaudio.mp3 shutanimation.zip shutrotate.zip
$(call expand_prebuilt_res, $(copy_from))
# End of Vanzo:songlixin
endif
