LOCAL_PATH:= $(call my-dir)

bromLite_dir := $(TARGET_OUT)/bin

#
# Build a statically-linked binary to include in OTA packages
#

include $(CLEAR_VARS)

# Build only in eng, so we don't end up with a copy of this in /system
# on user builds.  (TODO: find a better way to build device binaries
# needed only for OTA packages.)
LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := \
	bootrom_stage.c		\
	da_stage.c		\
	download_images.c	\
	GCC_Utility.c		\
	interface.c		\
	main.c	

LOCAL_CFLAGS := -Wall -W -Wmissing-field-initializers -D_CONSOLE

LOCAL_C_INCLUDES := $(LOCAL_PATH)

LOCAL_MODULE_PATH := $(bromLite_dir)

LOCAL_STATIC_LIBRARIES := libcutils libc

LOCAL_FORCE_STATIC_EXECUTABLE := true

LOCAL_MODULE := brom_lite

include $(BUILD_EXECUTABLE)

#ifeq ($(MODEM_NAME), MT6280) (fake code)
#modem mt6280
PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/Download_Agent/6280/EPP:system/etc/firmware/modem/EPP  \
  $(LOCAL_PATH)/Download_Agent/6280/INT_SYSRAM:system/etc/firmware/modem/INT_SYSRAM	\
  $(LOCAL_PATH)/Download_Agent/6280/NAND_FLASH_TABLE:system/etc/firmware/modem/NAND_FLASH_TABLE \
  $(LOCAL_PATH)/Download_Agent/6280/NOR_FLASH_TABLE:system/etc/firmware/modem/NOR_FLASH_TABLE

#endif


