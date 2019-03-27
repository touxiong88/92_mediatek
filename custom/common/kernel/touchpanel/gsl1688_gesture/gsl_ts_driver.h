#ifndef __GSL_TS_DRIVER_H__
#define __GSL_TS_DRIVER_H__
/*********************************/
#define TPD_HAVE_BUTTON		//???
#define GSL_ALG_ID		//???id??
#define GSL_COMPATIBLE_CHIP	//?????
#define GSL_THREAD_EINT		//????
//#define GSL_DEBUG			//??
#define TPD_PROC_DEBUG		//adb??
#define GSL_DRV_WIRE_IDT_TP
//#define GSL_TIMER				//????
//#define GSL_IRQ_CHECK
//#define TPD_PROXIMITY			//??????
#ifdef VANZO_TOUCHPANEL_GESTURES_SUPPORT
#define GSL_GESTURE
#endif

#define GSL_PAGE_REG    0xf0
#define GSL_CLOCK_REG   0xe4
#define GSL_START_REG   0xe0
#define POWE_FAIL_REG    0xbc
#define TOUCH_INFO_REG  0x80
#define TPD_DEBUG_TIME	0x20130424
struct gsl_touch_info
{
	int x[10];
	int y[10];
	int id[10];
	int finger_num;
};

struct gsl_ts_data {
	struct i2c_client *client;
	struct workqueue_struct *wq;
	struct work_struct work;
	unsigned int irq;
	struct early_suspend pm;
};

/*button*/
#define TPD_KEY_COUNT           2
#define TPD_KEYS                {KEY_MENU,KEY_BACK}
#define TPD_KEYS_DIM            {{400,2000,40,40},{800,2000,40,40}}


#ifdef GSL_ALG_ID
extern unsigned int gsl_mask_tiaoping(void);
extern unsigned int gsl_version_id(void);
extern void gsl_alg_id_main(struct gsl_touch_info *cinfo);
extern void gsl_DataInit(int *ret);
#endif
/* Fixme mem Alig */
struct fw_data
{
    u32 offset : 8;
    u32 : 0;
    u32 val;
};
#ifdef GSL_DRV_WIRE_IDT_TP
#include "gsl_idt_tp.h"
#endif
#include "gsl_ts_fw.h"
static unsigned char gsl_cfg_index = 1;

struct fw_config_type
{
	const struct fw_data *fw;
	unsigned int fw_size;
	unsigned int *data_id;
	unsigned int data_size;
};
static struct fw_config_type gsl_cfg_table[9] = {
/*0*/{GSLX68X_FW_1,(sizeof(GSLX68X_FW_1)/sizeof(struct fw_data)),gsl_config_data_id_1,(sizeof(gsl_config_data_id_1)/4)},
/*1*/{GSLX68X_FW_2,(sizeof(GSLX68X_FW_2)/sizeof(struct fw_data)),gsl_config_data_id_2,(sizeof(gsl_config_data_id_2)/4)},
/*2*/{GSLX68X_FW_3,(sizeof(GSLX68X_FW_3)/sizeof(struct fw_data)),gsl_config_data_id_3,(sizeof(gsl_config_data_id_3)/4)},
};

#endif
