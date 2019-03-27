# Copyright Statement:
#
# This software/firmware and related documentation ("MediaTek Software") are
# protected under relevant copyright laws. The information contained herein
# is confidential and proprietary to MediaTek Inc. and/or its licensors.
# Without the prior written permission of MediaTek inc. and/or its licensors,
# any reproduction, modification, use or disclosure of MediaTek Software,
# and information contained herein, in whole or in part, shall be strictly prohibited.
#
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

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

define all-json-files-under
$(shell find $(1) -name "*.json")
endef

define all-atlas-files-under
$(shell find $(1) -name "*.*" -and -not -name ".*")
endef

# Tool folders
WINE := /mtkoss/wine/1.1.33-ubuntu-10.04/x86_64/bin/wine
TEXTURE_PACKER := $(LOCAL_PATH)/textures/texturepacker/TexturePacker.exe
TEXTURE_PVRCOMPRESS := $(LOCAL_PATH)/textures/PVRTexTool/PVRTexTool

# Atlas source folder
MEDIA3D_ATLAS := $(LOCAL_PATH)/textures/media3d_atlas

# Atlas generation folder
ATLAS_SHEET := $(LOCAL_PATH)/textures/media3d_atlas.png
ATLAS_JSON := $(LOCAL_PATH)/res/raw/media3d.json
ATLAS_PVR_SHEET := $(LOCAL_PATH)/res/raw/media3d_atlas.pvr

# Atlas for xxhdpi source folder
MEDIA3D_ATLAS_XXHDPI := $(LOCAL_PATH)/textures/media3d_atlas_xxhdpi

# Atlas for xxhdpi generation folder
ATLAS_JSON_XXHDPI := $(LOCAL_PATH)/res/raw-xxhdpi/media3d.json
ATLAS_SHEET_XXHDPI := $(LOCAL_PATH)/textures/media3d_atlas_xxhdpi.png
ATLAS_PVR_SHEET_XXHDPI := $(LOCAL_PATH)/res/raw-xxhdpi/media3d_atlas.pvr

FORCE_REPLACE := $(LOCAL_PATH)/.replace

# Generate resources
HASH_ANIM := $(LOCAL_PATH)/hashAnim.py
JSON_HASH := $(LOCAL_PATH)/res/values/jsonhash.xml
LOCAL_GENERATED_RESOURCES := $(FORCE_REPLACE) $(JSON_HASH) $(ATLAS_JSON) $(ATLAS_PVR_SHEET) $(ATLAS_JSON_XXHDPI) $(ATLAS_PVR_SHEET_XXHDPI)

# Replace AndroidManifest for tablet
ifneq ($(strip $(findstring rel_customer_tablet, $(MTK_RELEASE_PACKAGE))),)
MANIFEST_SRC := $(LOCAL_PATH)/AndroidManifest-tablet.xml
else
MANIFEST_SRC := $(LOCAL_PATH)/AndroidManifest-phone.xml
endif

MANIFEST_TARGET := $(LOCAL_PATH)/AndroidManifest.xml

$(FORCE_REPLACE): $(MANIFEST_SRC)
	cp -f $(MANIFEST_SRC) $(MANIFEST_TARGET)
	touch $(FORCE_REPLACE)

# ==============================================================
# Resource generation
# ==============================================================
# Generate jsonhash.xml for cache filenames
$(JSON_HASH): $(call all-json-files-under, $(LOCAL_PATH)/res/raw)
	python $(HASH_ANIM) $@

$(LOCAL_PATH)/res/values/array.xml: $(JSON_HASH)

# ==============================================================
# Generate for mdpi, hdpi, xhdpi
# ==============================================================
# Generate media3d_atlas.pvr
$(ATLAS_PVR_SHEET): $(ATLAS_SHEET)
	$(TEXTURE_PVRCOMPRESS) -f OGLPVRTC4 -i $(ATLAS_SHEET) -o $(ATLAS_PVR_SHEET) -pvrtcbest -yflip0

# Generate media3d.json & media3d_atlas.png
#$(ATLAS_JSON) $(ATLAS_SHEET): $(call all-atlas-files-under, $(MEDIA3D_ATLAS))
#	$(WINE) $(TEXTURE_PACKER) --data $(ATLAS_JSON) --format json --inner-padding 2 --sheet $(ATLAS_SHEET) $(MEDIA3D_ATLAS) > /dev/null 2>&1

# ==============================================================
# Generate for xxhpdi
# ==============================================================
# Generate raw-xxhdpi/media3d_atlas.pvr
$(ATLAS_PVR_SHEET_XXHDPI): $(ATLAS_SHEET_XXHDPI)
	$(TEXTURE_PVRCOMPRESS) -f OGLPVRTC4 -i $(ATLAS_SHEET_XXHDPI) -o $(ATLAS_PVR_SHEET_XXHDPI) -pvrtcbest -yflip0

# Generate raw-xxhdpi/media3d.json & media3d_atlas_xxhdpi.png
#$(ATLAS_JSON_XXHDPI) $(ATLAS_SHEET_XXHDPI): $(call all-atlas-files-under, $(MEDIA3D_ATLAS_XXHDPI))
#	$(WINE) $(TEXTURE_PACKER) --data $(ATLAS_JSON_XXHDPI) --format json --inner-padding 2 --sheet $(ATLAS_SHEET_XXHDPI) $(MEDIA3D_ATLAS_XXHDPI) > /dev/null 2>&1

# ==============================================================
# Build configuration
# ==============================================================
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
	$(call all-java-files-under, src) \
	src/com/mediatek/weather/IWeatherServiceCallback.aidl \
	src/com/mediatek/weather/IWeatherService.aidl

LOCAL_PACKAGE_NAME := Media3D

LOCAL_STATIC_JAVA_LIBRARIES := com.mediatek.ngin3d-static
LOCAL_JNI_SHARED_LIBRARIES := liba3m libja3m

LOCAL_EMMA_COVERAGE_FILTER := +com.mediatek.media3d.*, -com.mediatek.media3d.R.*

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))