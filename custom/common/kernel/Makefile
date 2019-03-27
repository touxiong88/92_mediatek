include $(MTK_PATH_BUILD)/common.mk
$(call all-modules-src-or-makefile,$(obj),CUSTOM_KERNEL_)
obj-n := dummy.o
ifeq ($(VANZO_TOUCHPANEL_GESTURES_SUPPORT), yes)
ifeq ($(findstring focaltech,$(CUSTOM_KERNEL_TOUCHPANEL)), focaltech)
obj-y += ft_gesture_lib.a
$(obj)/ft_gesture_lib.a: $(srctree)/../$(obj)/touchpanel/ft_gesture_lib.a
	cp $(srctree)/../$(obj)/touchpanel/ft_gesture_lib.a $(obj)/ft_gesture_lib.a
endif
endif
TOUCHPANEL_GSL1688 := $(if $(filter $(CUSTOM_KERNEL_TOUCHPANEL), gsl1688),yes,no)
ifeq ($(TOUCHPANEL_GSL1688), yes)
obj-y += gsl_point_id.o
$(obj)/gsl_point_id.o: $(srctree)/../$(obj)/touchpanel/gsl_point_id
	cp $(srctree)/../$(obj)/touchpanel/gsl_point_id $(obj)/gsl_point_id.o
endif
TOUCHPANEL_GSL1688_GESTURE := $(if $(filter $(CUSTOM_KERNEL_TOUCHPANEL), gsl1688_gesture),yes,no)
ifeq ($(TOUCHPANEL_GSL1688_GESTURE), yes)
obj-y += gsl_point_id.o
$(obj)/gsl_point_id.o: $(srctree)/../$(obj)/touchpanel/gsl_point_id
	cp $(srctree)/../$(obj)/touchpanel/gsl_point_id $(obj)/gsl_point_id.o
endif