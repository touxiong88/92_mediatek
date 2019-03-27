include $(MTK_PATH_BUILD)/common.mk
#$(call all-subdir-src-or-makefile)
obj-y   +=  keypad/ #for Kernel HAL Structure
obj-y   +=  mtprof/
obj-y   +=  clock/
obj-y   +=  sched/
obj-y   +=  mmp/
obj-y   +=  xlog/
obj-y   +=  uart/
obj-y   +=  eint/
obj-y   +=  boot/
obj-$(CONFIG_ZRAM) +=  mlog/
obj-y	+=	gpio/
obj-y   +=  aee/
obj-y   +=  wdk/
obj-y   +=  ion_drv/
obj-y   +=  m4u/
obj-y   +=  video/
obj-y   +=  pmic_wrap/
obj-y   +=  accdet/
obj-y   +=  leds/
obj-y   +=  vibrator/
ifneq ($(MTK_BATTERY_NO_HAL),yes)
obj-y   += power/
endif
obj-y   +=  usb20/
obj-$(CONFIG_MTK_BTIF)   +=  btif/
obj-y   +=  dum-char/
obj-y   +=  freqhopping/

obj-$(CONFIG_MD32_SUPPORT)   +=  md32_wdt/
ifneq ($(CONFIG_MTK_FPGA),y)
obj-y   +=  rtc/
obj-y   +=  ccci/
obj-y   +=  dual_ccci/
obj-y   +=  fmradio/
obj-y   +=  gps/
obj-y   +=  hwmon/
obj-y   +=  mhl/
obj-y   +=  usb11/
obj-y   +=  almk/


obj-y   +=  bthid/
obj-y   +=  ccmni/

obj-y   +=  emd_ctl/
obj-$(CONFIG_MTK_SYSTRACKER) 	+=  systracker/

# gator-driver is not custom-release and will be removed
obj-y	+= $(call all-subdirs-with-makefile,gator-driver)


obj-y   +=  hdmitx/
obj-y   +=  auxadc/
obj-y   +=  mmp/
obj-y		+=  ram_console/
obj-y   +=  net/
obj-y   +=  thermal/
obj-y   +=  videocodec/
obj-y   +=  evdo_support/
obj-$(CONFIG_MTK_TEE_SUPPORT)  +=  trustzone/
obj-y   +=  wmt_ccci/

ifeq ($(MTK_NFC_SUPPORT),yes)
obj-y	+= nfc/
endif

ifeq ($(CONFIG_MTK_COMBO),m)

	BUILD_COMBO := no
	BUILD_SOC := no
	
	ifneq ($(filter MT6620E3,$(MTK_COMBO_CHIP)),)
		BUILD_COMBO := yes
	endif

	ifneq ($(filter MT6628,$(MTK_COMBO_CHIP)),)
		BUILD_COMBO := yes
	endif
	
	ifneq ($(filter MT6630,$(MTK_COMBO_CHIP)),)
		BUILD_COMBO := yes
	endif
	
	ifeq ($(BUILD_COMBO), yes)
		obj-y   +=  combo/
		subdir-ccflags-y += -D MTK_WCN_COMBO_CHIP_SUPPORT
	endif

	ifneq ($(filter CONSYS_6572,$(MTK_COMBO_CHIP)),)
		BUILD_SOC := yes
	endif
	
	ifneq ($(filter CONSYS_6582,$(MTK_COMBO_CHIP)),)
		BUILD_SOC := yes
	endif
	
	ifneq ($(filter CONSYS_6592,$(MTK_COMBO_CHIP)),)
		BUILD_SOC := yes
	endif
	
	ifeq ($(BUILD_SOC), yes)
		obj-y   +=  conn_soc/
	endif

	obj-y   +=  common_detect/
	subdir-ccflags-y += -D MTK_WCN_WMT_STP_EXP_SYMBOL_ABSTRACT

endif

#ifeq ($(CONFIG_MTK_WIFI),m)
#obj-y   +=  conn_soc/
#endif


obj-y	+=	pwm/

obj-y   += sync/
obj-$(CONFIG_MTK_ETM) += etm/
obj-y   +=      dbg_dump/
obj-y	+=	misc_via/
endif#!CONFIG_MTK_FPGA
obj-$(CONFIG_MTK_SCHED_CMP_POWER_AWARE_CONTROLLER) +=  pa/
ifeq ($(MTK_EMMC_SUPPORT),yes)
obj-y	+= flash_info/
endif

obj-y	+= lcm3d/
obj-y	+= gesture/
obj-y	+= pac7620/
#obj-y	+= at24xx/
#obj-y	+= stm8/