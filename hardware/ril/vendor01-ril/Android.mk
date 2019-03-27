# Copyright 2006 The Android Open Source Project
#

ifeq ($(MTK_3GDONGLE_SUPPORT), yes)


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
PRODUCT_COPY_FILES += $(LOCAL_PATH)/libhuawei-ril.so:system/lib/libhuawei-ril.so
PRODUCT_COPY_FILES += $(LOCAL_PATH)/ip-script/ip-up:system/etc/ppp/ip-up
PRODUCT_COPY_FILES += $(LOCAL_PATH)/ip-script/ip-down:system/etc/ppp/ip-down

endif #($(MTK_3GDONGLE_SUPPORT), yes)
