LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# files that live under /system/etc/...
$(shell mkdir -p $(PRODUCT_OUT)/system/etc/oupeng/res)
$(shell cp -rf $(LOCAL_PATH)/res/* $(PRODUCT_OUT)/system/etc/oupeng/res/ ;)
PRODUCT_COPY_FILES += $(LOCAL_PATH)/config.xml:system/etc/oupeng/config.xml
