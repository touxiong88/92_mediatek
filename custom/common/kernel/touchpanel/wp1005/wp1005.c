/*
   For MTK System
 * Winpower touch driver Version 2.0
 *
 * Copyright (C) 2014 Winpower Co.Ltd
 *
 * This program is free software; you can redistribute  it and/or modify it
 * under  the terms of  the GNU General  Public License as published by the
 * Free Software Foundation.
 *
 *
 * Version : 2.0 , winpower touch panel driver for 1005 Chip on omap platform.
 * Date: 2014/04/16

 */
 
#include "tpd.h" 
#include <linux/cdev.h>
#include <linux/fs.h> 
#include <asm/uaccess.h> 
#include <linux/interrupt.h>
#include <cust_eint.h>  
#include <linux/i2c.h>
#include <linux/sched.h>
#include <linux/kthread.h>
#include <linux/rtpm_prio.h> 
#include <linux/wait.h> 
#include <linux/time.h> 
#include <linux/delay.h>
#include <linux/slab.h> //add
#include "wp1005.h"
#include "tpd_custom_wp1005.h"
//#define PowerOnUpdate
//#define WP1005_IOCTL
//#define WP1005DEBUG
//#define WP1005RAWDEBUG  
//#ifdef  WP1005RAWDEBUG

//#define OWEN_TEST
#define DEBUGAPK
static struct cdev *chardev;

static void 	wp1005_apk_file_analysis(int cmd,int pos);
static int 		fw_download_05(int loadMode);
static u8  		ReadBack[512];
struct file 	*pLogDataFile;
static char     LogDataFileName[80];
static u8  		NoiseBuf[NOISE_LEVEL_DATA_LEN]  = {0};  //For store FNSL data  //Jack
static s16      NoisePtrBuf[NOISE_LEVEL_DATA_LEN]  = {0};  //For store FNSL data  //Jack
static u8		RegDataBuf[MAX_REG_DATA_LEN] = {0}; //Jack 20131220 add to record register read data
static unsigned char MsgBuf[258];     //for ioctl
//static unsigned char I2cBuf[258];     //for ioctl
//static uint16_t GlobaNum[16] = {0};
static int  	APKFWUPDATE = 0; //JAN
static int 		fwcount = 0;
static int  	BIN_LENGTH = 0;  // new add for APKUPDATEFW
//static char 	wp1005_buffer[80]; //="";//update fw status via APK by jan 20140124
static char 	ch_buf[80];//update fw status via APK by jan 20140124
static uint8_t  ApkFWUpdateStatusData[2]={0}; //update fw state by jan 20140124//UpdateFWState[2] = {0};
static int       SelfLen = 0;
static int       MutualLen1 = 0;
static int       MutualLen2 = 0;
static u8   	MutualDataLen[3] = {0};
static u8   	SelfDataLen[2]   = {0};
static u8   	CheckStateBuf[1] = {0};
static u8   	RawBuf[1024]     = {0};
static uint16_t  Mutual_Node_No = 1; //Jack add for record overall mutual node number
static uint16_t  Self_CH_No = 1; //Jack add for record overall self channel number
static uint16_t  self_data_log_len = 0; //Jack 20131218 add self record len 
static uint16_t  mutual_data_log_len = 0; //Jack 20131218 add mutual record len
static uint16_t  ioctl_num = 0;
//static uint16_t  SelfMode = 0;
//static uint16_t  MutualMode = 0;
static uint16_t  Wp1004DebugMode = 0;
static u8   CurDbgDataType = DATA_ALL_NONE; //Jack add for record current debug data type
static u8   CurSensingMode = MUTUAL_MODE; //Jack add for record current sensing mode
//static uint8_t   RegAddr = 0; //Jack 20131210 add for recording register addr
static u8   RegReadDataIdx = 0; //Jack 20131210 add for recording register read data index
//static uint32_t  SaveDataPos = 0; //Jack 20131205 add for record current start log data position 
static u8   IsReportTouchDataToAPK = 0; //Jack 20131213 add for record touch data. 1: report touch data to APK, 0: otherwise
static u8   IsReportKeyDataToAPK = 0;   //Jack 20131213 add for record key data. 1: report key data to APK, 0: otherwise
static u8   IsReportFWUpdateStatusToAPK = 0;   //Jack 20131213 add for record fw update status. 1: report fw update status to APK, 0: otherwise
static u8   IsDataLogbyDriver = 1; //Jack 20131219 add to record FW debug data log by Kernel driver. 1: log by driver, 0: otherwise
static struct class 	*winic_i2c_class2;
//static dev_t 	dev_id;
static dev_t 	dev_id2;
static 	u8 		saveLogNow = 0;

/*static struct wp1005_ioctl_finger_data ApkFingerData;
  static struct wp1005_ioctl_key_data ApkKeyData;
  static struct wp1005_ioctl_fw_update_status ApkFWUpdateStatusData;*/
static u16 		ApkFingerData[MAX_SUPPORT_FINGER_NO*TOUCH_NUM];    
static u8 		ApkKeyData[MAX_SUPPORT_KEY_NO];    

static void 	Clear_I2C_End(void);
static ssize_t 	wp1005_ioctl_write(struct file *filp, const char __user *buff, size_t count, loff_t *pos);
static ssize_t 	wp1005_ioctl_read(struct file *filp, char __user *buf, size_t count, loff_t *f_pos);
static int 		wp1005_ioctl_open(struct inode *inode, struct file *filp);
static int 		wp1005_ioctl_close(struct inode *inode, struct file *filp);
static long 	wp1005_ioctl(struct file *filp, unsigned int cmd, unsigned long arg);
static void 	swap2(int len);
static int 		wp1005_Mutualmode_Delta(void);
static int 		wp1005_Mutualmode_RawData(void);
static int 		wp1005_Mutualmode_Baseline(void);
static int 		wp1005_Mutualmode_None(void);
static int 		wp1005_selfmode_Delta(void);
static int 		wp1005_selfmode_RawData(void);
static int 		wp1005_selfmode_Baseline(void);
static int 		wp1005_selfmode_None(void);
static int 		wp1005_setmode_none(void);
static int 		wp1005_ic_reset(void); 
static int 		wp1005_read_raw_data(void);
static void 	wp1005_apk_cmd_analysis(unsigned long cmd);
//static void 	wp1005_dec_to_binary(int dec);
static int 		wp1005_get_current_sensing_mode(void); 
static void 	wp1005_read_register(u8 reg, u8 idx); //Jack 20131210 add for register data read
static void 	wp1005_log_file_name_handler(u8 DataType); //Jack 20131219 add for naming data log file name
static void 	wp1005_log_file_close(void); //Jack 20131219 add for close data log file
static int 		get_header(void);
static void 	save_wp1005_header_data(void);
static void 	print_header(void);
static void 	Flash_Erase(void);

static int 		Flash_Write_05(u8 *src, int binSize);// Jack Max: 256bytes
static int 		wp100x_check_ROM_isNotEmpty(void);
static int 		Prot_Init(void);
//static int  PollFlag = 0;
static uint8_t  CheckBinFlag[2] = {0};
static uint8_t  UpdateFWState[2] = {0};
//static uint8_t FWUPDATEWRITELEN = 4; //default 4 byte for wp1005
struct file *fp_global;

static char HeaderBuf[32] ={0};
//static int UPDATENOSLEEP = 0;
static uint32_t Wp1004PrintLevel = 0;
//static uint16_t FilePos  = 0;
//static uint8_t  WP1005PrintMsg = 0;
//static uint8_t  ErasePageNo;
//struct class *winic_i2c_class;

//static uint16_t KeyType = 0;
static char *str = "/storage/sdcard0/W1005.bin";

//static int major = 255;
static struct cdev *chardev;


int wp1005_touch_key[WP1005_KEY_NUMBER] = {
    KEY_BACK,
    KEY_HOMEPAGE,
    KEY_MENU
};

tProtocolHeader tpheader; 
//tParameters Param;


#ifdef PWON_CHECK_INC
static u8 File_inc[]=
{ 
    //#include "WP1005_51.inc" 
#include "W1005.inc"
};
#endif

//static char FileBuf[33048];
//static int  PollFlag = 0;
static int wp1005_write_reg(struct i2c_client *client, char *data, int count);
//int wp1005_get_header(void);
//static int wp1005_check_update_bin(void);
//static int wp1005_load_fw_bin(void);
//static int wp1005_load_fw(unsigned char *flash_buf, int flash_len);

/**************************************************************************/
//#include "tpd_custom_WP1005.h" 
#ifdef MT6575
#include <mach/mt6575_pm_ldo.h>
#include <mach/mt6575_typedefs.h>
#include <mach/mt6575_boot.h>
#endif


#ifdef MT6577
#include <mach/mt6577_pm_ldo.h>
#include <mach/mt6577_typedefs.h>
#include <mach/mt6577_boot.h>
#endif

#ifdef MT6589
#include <mach/mt_pm_ldo.h>
#include <mach/mt_typedefs.h>
#include <mach/mt_boot.h>
#endif

#include "cust_gpio_usage.h" 

//extern bool mt_usb_is_device(void);
extern kal_bool upmu_is_chr_det(void);
extern struct tpd_device *tpd;

static struct i2c_client 		*i2c_client = NULL;
struct winic_i2c_data 	*winic_i2c_data_ptr;
static struct task_struct *thread = NULL;

static DECLARE_WAIT_QUEUE_HEAD(waiter);
static void tpd_eint_interrupt_handler(void);

#ifdef MT6575 
extern void mt65xx_eint_unmask(unsigned int line);
extern void mt65xx_eint_mask(unsigned int line);
extern void mt65xx_eint_set_hw_debounce(kal_uint8 eintno, kal_uint32 ms);
extern kal_uint32 mt65xx_eint_set_sens(kal_uint8 eintno, kal_bool sens);
extern void mt65xx_eint_registration(kal_uint8 eintno, kal_bool Dbounce_En,
        kal_bool ACT_Polarity, void (EINT_FUNC_PTR)(void),
        kal_bool auto_umask);
#endif
#ifdef MT6577
extern void mt65xx_eint_unmask(unsigned int line);
extern void mt65xx_eint_mask(unsigned int line);
extern void mt65xx_eint_set_hw_debounce(unsigned int eint_num, unsigned int ms);
extern unsigned int mt65xx_eint_set_sens(unsigned int eint_num, unsigned int sens);
extern void mt65xx_eint_registration(unsigned int eint_num, unsigned int is_deb_en, unsigned int pol, void (EINT_FUNC_PTR)(void), unsigned int is_auto_umask);
#endif

#ifdef MT6589
extern void mt65xx_eint_unmask(unsigned int line);
extern void mt65xx_eint_mask(unsigned int line);
extern void mt65xx_eint_set_hw_debounce(unsigned int eint_num, unsigned int ms);
extern unsigned int mt65xx_eint_set_sens(unsigned int eint_num, unsigned int sens);
extern void mt65xx_eint_registration(unsigned int eint_num, unsigned int is_deb_en, unsigned int pol, void (EINT_FUNC_PTR)(void), unsigned int is_auto_umask);
#endif

static int __devinit tpd_probe(struct i2c_client *client, const struct i2c_device_id *id);
static int tpd_detect (struct i2c_client *client, struct i2c_board_info *info);
static int __devexit tpd_remove(struct i2c_client *client);
static int touch_event_handler(void *unused);
static int tpd_flag = 0;
//static int point_num = 0;
//static unsigned char WP1005Key = 0;
static bool isChargerOnNow = 0;
//static int p_point_num = 0;

//--------------------------------------------------------------------------------------------------
// ---- Owen: Check it can be deleted -----------------/
#ifdef TPD_HAVE_BUTTON 
static int tpd_keys_local[TPD_KEY_COUNT] = TPD_KEYS;
static int tpd_keys_dim_local[TPD_KEY_COUNT][4] = TPD_KEYS_DIM;
#endif
#if (defined(TPD_WARP_START) && defined(TPD_WARP_END))
static int tpd_wb_start_local[TPD_WARP_CNT] = TPD_WARP_START;
static int tpd_wb_end_local[TPD_WARP_CNT]   = TPD_WARP_END;
#endif
#if (defined(TPD_HAVE_CALIBRATION) && !defined(TPD_CUSTOM_CALIBRATION))
static int tpd_calmat_local[8]     = TPD_CALIBRATION_MATRIX;
static int tpd_def_calmat_local[8] = TPD_CALIBRATION_MATRIX;
#endif   

// -----------------------------------------------------------------------------------------------------------------
static int wp1005_i2c_txdata(char *txdata, int length)
{
    int ret = 0;

    struct i2c_msg msg[] = {
        {
            .addr	= i2c_client->addr,
            .flags	= 0,
            .len	= length,
            .buf	= txdata,
        },
    };

    ret = i2c_transfer(i2c_client->adapter, msg, 1);
    if (ret < 0)
        pr_err("%s i2c write error: %d\n", __func__, ret);

    return ret;
}
// ---------------------------------------------------------------------------------------------------------------
static int wp1005_i2c_wr(char *writeDataCmd, int lenOfCmd, char *rxdata, int length)
{
    int ret=0;
    //printk("wp1 wr:%x; len:%d; addr=%x\n", writeDataCmd[0], lenOfCmd, i2c_client->addr);
    struct i2c_msg msgs[] = {
        {
            .addr	= i2c_client->addr,
            .flags	= 0,      //W
            .len	= lenOfCmd,
            .buf	= writeDataCmd,
        },

        {
            .addr	= i2c_client->addr,
            .flags	= I2C_M_RD,   //R
            .len	= length,
            .buf	= rxdata,
        },
    };

    ret = i2c_transfer(i2c_client->adapter, msgs, 2);
    if (ret < 0)
        printk("wp1:msg %s i2c read error: %d\n", __func__, ret);
    return ret;
}
// -------------------------------------------------------------------------------------------------------

static int WriteFile(struct file *fp,char *buf,int readlen) 
{    
    if (fp->f_op && fp->f_op->read)       
        return fp->f_op->write(fp,buf,readlen, &fp->f_pos);    
    else       
        return -1; 
} 
// -------------------------------------------------------------------------------------------------------
static void Clear_I2C_End()
{
    uint8_t i2c_end[2] ={0x18,0x0};
    //struct winic_i2c_data *ts  = winic_i2c_data_ptr;

    wp1005_write_reg(i2c_client,i2c_end,I2C_END_LEN);          

    CheckStateBuf[0] = 0;
    //SaveDataPos = 0; //Jack 20131205 add to clear current log data position
}
// -------------------------------------------------------------------------------------------------------


static int wp1005_charge_on(void)
{
    uint8_t charge_on[2] = {0x1B,0x1};

    i2c_master_send(i2c_client, charge_on, 2);   

    WP1005_PRINT(1, "wp1: Charge ON: at %d line : in %s\n",__LINE__,__FILE__);

    return 0;
}
//EXPORT_SYMBOL(wp1005_charge_on);
// ---------------------------------------------------------------------------------------------------------------------------
static int wp1005_charge_off(void)
{
    uint8_t charge_off[2] = {0x1B,0x0};

    i2c_master_send(i2c_client, charge_off, 2);   

    WP1005_PRINT(1, "wp1: Charge Off : at %d line : in %s\n",__LINE__,__FILE__);

    return 0;
}
//EXPORT_SYMBOL(wp1005_charge_off);
// ---------------------------------------------------------------------------------------------------------------------------
static void wp1005_ac_judge(void)
{
    int ret  = 0;
    //ret = get_gFG_Is_Charging();   //
    ret = upmu_is_chr_det();  
    WP1005_PRINT(1, "wp1: After Read Charger:%d \n", ret);
    if(ret) 
    {		
        if(!isChargerOnNow)
        {
            wp1005_charge_on(); 
            isChargerOnNow = true;			
        }
    }
    else if (ret == 0)
    {
        if(isChargerOnNow)
        {
            wp1005_charge_off();
            isChargerOnNow = false;
        }
    }
}

//-----------------------------------------------------------------------------------------------
struct file_operations wp1005_ioctl_fops = {
    .owner   = THIS_MODULE,
    .open    = wp1005_ioctl_open,
    .release = wp1005_ioctl_close,
    .read    = wp1005_ioctl_read,
    .write   = wp1005_ioctl_write,
    .unlocked_ioctl = wp1005_ioctl,
};

struct touch_info {
    int y[TOUCH_NUM];
    int x[TOUCH_NUM];
    int p[TOUCH_NUM];
    int id[TOUCH_NUM];
    int count;
};

static const struct i2c_device_id wp1005_tpd_id[] = {{"wp1005",0},{}};

/// static unsigned short force[] = {0, 0xDC, I2C_CLIENT_END, I2C_CLIENT_END};
///static const unsigned short *const forces[] = { force, NULL };

static struct i2c_board_info __initdata wp1005_i2c_tpd={ I2C_BOARD_INFO("wp1005", (0xDC>>1))}; 

static struct i2c_driver tpd_i2c_driver = {
    .driver = {
        .name = "wp1005",
        //	 .owner = THIS_MODULE,
    },
    .probe = tpd_probe,
    .remove = __devexit_p(tpd_remove),
    .id_table = wp1005_tpd_id,    
    .detect = tpd_detect,
    ///  .address_list = (const unsigned short *) forces,
};




static void wp1005_ts_release(void)
{
    //struct winic_i2c_data *ts = winic_i2c_data_ptr;
    /*
       int i = 0;
       for (i=0; i<3; i++)
       input_report_key(tpd->dev, wp1005_touch_key[i],	0);

       input_report_abs(tpd->dev, ABS_MT_PRESSURE, 0);
     */
    input_report_key(tpd->dev, BTN_TOUCH,	0);
    input_mt_sync(tpd->dev);
    WP1005_PRINT(3, "rel\n");
    input_sync(tpd->dev);

}
// ---------------------------------------------------------------------------------------
static int wp1005_i2c_get_report(void)
{
    //struct winic_i2c_data *ts = winic_i2c_data_ptr;
    //struct input_dev *input_dev = ts->input_dev;
    int 	i = 0,	ret = 0,	finger_number = 0;
    u8 		report_len = 0;
    u8		buf[64];  //43		
    u8	 	key_flag[3] = {0, 0, 0}; 
    u8 		ids = 0;
    u16		xs, ys = 0;
    u8      readCmd[2] = {0x03, 0x00};

    int TouchDataRecordCount = 1; //Jack 2013 add for record touch data count. [0]: current touch valid data len

    ret = wp1005_i2c_wr(readCmd, 2, &report_len,1); 
    if (ret < 0)
        return -1;

    //printk("wp1:get 1: len=%d\n", report_len);
    for (i=0; i<((report_len>>3) + 1); i++)
    {
        readCmd[1] = i;
        ret = wp1005_i2c_wr(readCmd, 2, &buf[i<<3],8); //(((i+1)<<3)<report_len) ? 8: (report_len - (i<<3)));     
        if (ret < 0) {
            printk("wp1:%s read_data i2c_rxdata failed: %d\n", __func__, ret);
            return ret;
        }
    }

    if(Wp1004PrintLevel == 3)
    {
        printk("wp1:");
		for (i=0; i<43; i++)
            printk("%x ", buf[i]);

        printk("\n");
    }

    WP1005_PRINT(3, "len:%d \n", report_len);

    if ((report_len >=3) && (report_len <=43))
    {		
        if (buf[report_len - 2] != 0x20)
        {
            WP1005_PRINT(3, "ERROR: Wrong Data Format !\n");
            return -1;
        }

        if (buf[0] == 0x03)
        {	
            if (buf[2] == 0)
            {
                wp1005_ts_release();
                return 1; 
            }
            else
            {
                //printk("wrong data format: %x \n", Param.tp_reports[2]);
                printk("wp1:wrong data format: %x \n", buf[2]);
                return -1;
            }
        }
        else
        { 
            finger_number = (buf[0]-3)/4; 
            //printk("wp1: num=%d \n", finger_number);

#ifdef WP_TEST
            if(Wp1004DebugMode == 1)
                return 2;   //  Owen add for test
#endif

            if(IsReportTouchDataToAPK == 1)
            {
                ApkFingerData[0] = finger_number * TOUCH_NUM;
            }
            for (i=0; i<finger_number; i++)
            {
                ids  = ((buf[i*4+1]&0x78)>>3); 
                xs = (u16)(buf[i*4+1] & 0x07)<<8 | (u16)buf[i*4+2];
                ys = (u16)(buf[i*4+3] & 0x07)<<8 | (u16)buf[i*4+4];

                input_report_abs(tpd->dev, ABS_MT_TOUCH_MAJOR, 	20);
                input_report_abs(tpd->dev, ABS_MT_TRACKING_ID, 	ids);	
                input_report_abs(tpd->dev, ABS_MT_POSITION_X, 		xs);
                input_report_abs(tpd->dev, ABS_MT_POSITION_Y, 	    ys);

                //printk("wp1: %d(%d %d) \n", ids, xs, ys);
                //input_report_abs(tpd->dev, ABS_MT_PRESSURE, 		200);
                input_report_key(tpd->dev, BTN_TOUCH,				1);
                input_mt_sync(tpd->dev);

                if(IsReportTouchDataToAPK == 1)
                {				
                    ApkFingerData[TouchDataRecordCount++] = ids+1;
                    ApkFingerData[TouchDataRecordCount++] = xs;
                    ApkFingerData[TouchDataRecordCount++] = ys;
                    ApkFingerData[TouchDataRecordCount++] = (buf[i*4+1+2] & 0xF8);
                    ApkFingerData[TouchDataRecordCount++] = 1; 
                }				
                WP1005_PRINT(1, "id:%d,(%04d,%04d) \n",ids,xs,ys);
            }

            input_sync(tpd->dev);
        }	
    }
    return 1;
}
//-------------------------------------------------------------------------------------------------------

static int touch_event_handler(void *unused)
{
    int ret = 0;
    //int TouchDataRecordCount = 1; //Jack 2013 add for record touch data count. [0]: current touch valid data len

    //unsigned char key_flag[3] = {0, 0, 0}; 

    struct sched_param param = { .sched_priority = RTPM_PRIO_TPD };
    sched_setscheduler(current, SCHED_RR, &param);
    WP1005_PRINT(3, "wp1: -- event -- \n");
    do
    {
        mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);    // enable IRQ	
        set_current_state(TASK_INTERRUPTIBLE); 	 
        wait_event_interruptible(waiter,tpd_flag!=0);	   
        tpd_flag = 0;		 
        set_current_state(TASK_RUNNING);

        ret = wp1005_i2c_get_report();

        if (ret >0)
        {
            if(Wp1004DebugMode == 1){
                WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
                wp1005_read_raw_data();	
            }
        }
    }while(!kthread_should_stop());
    //printk("WP1X04: !!!!!! !\n");
    return 0;
}

static int tpd_detect (struct i2c_client *client, struct i2c_board_info *info) 
{
    strcpy(info->type, TPD_DEVICE);	 
    return 0;
}

static void tpd_eint_interrupt_handler(void)
{
    TPD_DEBUG_PRINT_INT;
    tpd_flag = 1;
    wake_up_interruptible(&waiter);
    WP1005_PRINT(1, "wp1: ------ EINT ------ \n");
}

static inline int wp1005_write_reg(struct i2c_client *client,char *data, int count)
{
    int ret;

    if(!client->adapter) {
        printk("wp1: !client->adapter = %d\n",__LINE__);
        return ENODEV;
    }

    ret = i2c_master_send(client, data, count);

    if(ret < 0){
        printk("wp1: I2c master send fail!!! in wp1005_write_reg func.\n");
    } else {
        //WP1005_PRINT(1,"Write reg succuess : ret = %d\n",ret);
    }
    return ((ret == count)? 0 : ret );
}
//--------------------------------------------------------------------------------------------------------

struct file *openFile_t(char *path,int flag,int mode)
{
    struct file *fp;

    fp = filp_open(path, flag, 0);
    if (fp)
        return fp;
    else
        return NULL;
}
//--------------------------------------------------------------------------------------------------------
static void file_close(struct file* file) {
    filp_close(file, NULL);
}
//--------------------------------------------------------------------------------------------------------
static int readFile_t(struct file *fp,char *buf,int readlen)
{
    if (fp->f_op && fp->f_op->read)
        return fp->f_op->read(fp,buf,readlen, &fp->f_pos);
    else
        return -1;
}
// ---------------------------------------------------------------------------------------------------------
static void Flash_Erase(void)
{
    u8 set_reg1[2] = {0x34, 0x90}; 	

    u8 set_reg2[2] = {0x34, 0x80};		
    u8 set_reg3[2] = {0x33, 0x06};		
    u8 set_reg4[2] = {0x34, 0x90};		

    u8 set_reg5[2] = {0x34, 0x80};		
    u8 set_reg6[2] = {0x33, 0x60};		
    u8 set_reg7[2] = {0x34, 0x90};		
    u8 set_reg8[2] = {0x39, 0x01};		

    i2c_master_send(i2c_client, set_reg1, 2);
    i2c_master_send(i2c_client, set_reg2, 2);
    i2c_master_send(i2c_client, set_reg3, 2);
    i2c_master_send(i2c_client, set_reg4, 2);
    i2c_master_send(i2c_client, set_reg5, 2);
    i2c_master_send(i2c_client, set_reg6, 2);
    i2c_master_send(i2c_client, set_reg7, 2);

    printk("wp1:[Flash_Erase]: WP1005 Flash mass erase\n");

    msleep(1000);  

    i2c_master_send(i2c_client,set_reg8,2);			

    printk("wp1:%d:%s Finished \n",__LINE__,__func__);
}
// ---------------------------------------------------------------------------------------------------------------------------
static int wp100x_check_ROM_isNotEmpty()
{
    if (tpheader.CAP_2 == CAP2)
    {
        printk("wp1:ROM OK \n");
        return 1;
    }
    else 
    {
        printk("wp1:ROM is empty or WRONG \n");
        return 0;
    }
}
//----------------------------------------------------------------------------------------------------------------------------------------------------
static int wp1005_check_update_inc(void)
{ 
    u16		PID_in_inc = 0;
    u8		FW_VER_in_inc = 0;

    printk("wp1:inc: %x %x %x\n", File_inc[0], File_inc[1], File_inc[2]);
    if(wp100x_check_ROM_isNotEmpty())
    {
        PID_in_inc = (u16)(File_inc[0] << 8) | File_inc[1];
        printk("wp1:PID in inc:%x;in header:%x \n", PID_in_inc, tpheader.P_ID);

        if (PID_in_inc == tpheader.P_ID)
        {
            FW_VER_in_inc = File_inc[2];
            printk("wp1:FW VER in inc:%x;in header:%x \n", FW_VER_in_inc, tpheader.FW_VER);
            if (FW_VER_in_inc > tpheader.FW_VER)
                return 1;
            else
                return 0;
        }
        else
            return 0;
    }
    else
        return 1;

}

// --------------------------------------------------------------------------------------------------------------
static int Flash_Open(void)
{
    int ret = 0;
    // ----------------------------------------------------/
    u8 set1[2]={0x39, 0x01};
    u8 set2[2]={0x34, 0x80};
    u8 set3[2]={0x33, 0xAB};
    u8 set4[2]={0x34, 0x90};
    // ---------- above for disable sleep -----------------/
    u8 unlock_i2c[3] ={0xFB, 0x68, 0x11}; // Unlock MCU I2C to direct-access mode.
    u8 disable_MCU[2] = {0xFF, 0x01}	; // Disable MCU. Stop MCU.
    u8 start_addr[4] = {0x32, 0x00, 0x00, 0x00};
    u8 flash_enable_set[2] = {0x34, 0xB0}; //Wr_flash_en=1 and spim_cs=1 and spim_dual

    ret = wp1005_i2c_txdata(set1, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set1: %d \n", ret);
        return -1;
    }
    ret = wp1005_i2c_txdata(set2, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set2: %d \n", ret);
        return -1;
    }
    ret = wp1005_i2c_txdata(set3, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set3: %d \n", ret);
        return -1;
    }
    ret = wp1005_i2c_txdata(set4, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set4: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(unlock_i2c, 3);
    if (ret < 0)
    {
        printk("wp1:tx error: set unlock_i2c: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(disable_MCU, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set unlock_i2c: %d \n", ret);
        return -1;
    }
    ret = wp1005_i2c_txdata(start_addr, 4);
    //ret = wp1005_write_reg(client, start_addr, 4);
    if (ret < 0)
    {
        printk("wp1:tx error: set unlock_i2c: %d \n", ret);
        return -1;
    }
    ret = wp1005_i2c_txdata(flash_enable_set, 2);
    //ret = wp1005_write_reg(client, flash_enable_set, 2);		
    if (ret < 0)
    {
        printk("wp1:tx error: set unlock_i2c: %d \n", ret);
        return -1;
    }
    //chksum = 0;
    printk("wp1:Flash_Open OK \n");
    return 1;
}
// --------------------------------------------------------------------------------------------------------------
static void Flash_Checksum2(void)
{
    //u8 sum[3] = {0, 0, 0};
    int ret  = 0;
    u8 readCmd = 0x39;
    u8 chsum[3] = {0xFF, 0xFF, 0xFF};
    ret = wp1005_i2c_wr(&readCmd,1,chsum,3);
    if (ret < 0)
        printk("wp1:read checksum fail: %d \n", ret);

    else 
        printk("wp1:chksum: %02x %02x %02x \n", chsum[0], chsum[1], chsum[2]);



}
// --------------------------------------------------------------------------------------------------------------
static int Flash_Checksum(u8* chsum)
{
    //u8 sum[3] = {0, 0, 0};
    int ret  = 0;
    u8 readCmd = 0x39;
    ret = wp1005_i2c_wr(&readCmd,1,chsum,3);
    if (ret < 0)
    {
        printk("wp1:read checksum fail: %d \n", ret);
        return ret;
    }

    //printk("chksum: %02x %02x %02x \n", chsum[0], chsum[1], chsum[2]);
    return ret;

}
// -----------------------------------------------------------------------------------------------------
static int Flash_Close(int binSize, u16 chksum)
{
    //DEBUG_PRINTF("Flash Close\n");
    int result = 0;
    int ret = 0;
    u8 ReadChksum[3] = {0, 0, 0};
    u8 FWLen_Hi = (binSize&0xFF00)>>8;//A15~A8;
    u8 FWLen_Lo = (binSize&0x00FF);//A7~A0	;
    u8 ChkSum_Hi = (chksum&0xFF00)>>8;//A15~A8;
    u8 ChkSum_Lo = (chksum&0x00FF);//A7~A0	;		
    //Set Write Enable Latch(WEL) 

    u8 set_reg1[2] = {0x34, 0x80};
    u8 set_reg2[2] = {0x33, 0x06};		
    u8 set_reg3[2] = {0x34, 0x90};		

    u8 set_reg4[2] = {0x34, 0x80};		
    u8 set_reg5[5] = {0x33, 0x02, 0x00, FWLen_Hi, FWLen_Lo};		
    u8 set_checksum[3] = {0x33, ChkSum_Lo, ChkSum_Hi};		
    u8 set_reg7[2] = {0x34, 0x90};		

    u8 set_reg8[2] = {0x34, 0x10};
    u8 set_reg9[4] = {0x32, 0x00, 0x00, 0x00};	
    u8 set_reg10[2] = {0x39, 0x00}; 

    u8 set_reg11[2] = {0xFF, 0x00};
    u8 set_reg12[3] = {0xFB, 0x68, 0x00};	

    printk("wp1:Flash_Close: Pos:%d ==> 0x%02x%02x ;chksum:0x%04x", binSize, FWLen_Hi, FWLen_Lo, chksum);

    ret = wp1005_i2c_txdata(set_reg1, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg1: %d \n", ret);
        return -1;
    }	

    ret = wp1005_i2c_txdata(set_reg2, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg2: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg3, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg3: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg4, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg4: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg5, 5);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg5: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_checksum, 3);
    if (ret < 0)
    {
        printk("wp1:tx error: set_Chksum: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg7, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg7: %d \n", ret);
        return -1;
    }
    msleep(2); //delay 1 ms [Unit:0.001ms], need to delay 0.4 ~ 0.8 ms


    ret = wp1005_i2c_txdata(set_reg8, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg8: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg9, 4);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg9: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg10, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg10: %d \n", ret);
        return -1;
    }

    msleep(25);	
    result = Flash_Checksum(ReadChksum);
    printk("wp1:Readback Chksum= %02x%02x; Self Cal chksum:%04x \n", ReadChksum[2], ReadChksum[1], chksum);
    msleep(2);

    ret = wp1005_i2c_txdata(set_reg11, 2);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg11: %d \n", ret);
        return -1;
    }

    ret = wp1005_i2c_txdata(set_reg12, 3);
    if (ret < 0)
    {
        printk("wp1:tx error: set_reg12: %d \n", ret);
        return -1;
    }
    msleep(20);

    printk("wp1:Flash_Close OK \n");
    return 1;
}

// --------------------------------------------------------------------------------------------------------------
static int Flash_Write_05(u8 *src, int binSize)	// Jack Max: 256bytes
{
    u8 headOfWriteData = 0x33; 
    int i = 0, ret = 0, pageSize = 7, partRegion = 128;    //pageSize = 7? 128?
    int partSize = pageSize;
    int sendFinished = 0, partFinished = 0;
    u8 set_reg1[2] = {0x34, 0x80}; //Wr_flash_en=1 and spim_cs=0
    u8 set_reg2[2] = {0x33, 0x06}; //Write Enable
    u8 set_reg3[2] = {0x34, 0x90}; //Wr_flash_en=1 and spim_cs=1
    //set to flash page programing mode
    u8 set_reg4[2] = {0x34, 0x80}; //Wr_flash_en=1 and spim_cs=0       	       					
    u8 set_reg5[5] = {0x33, 0x02, 0x00, 0, 0};     //[3]=xadr, [4]=yadr

    u8 set_reg6[2] = {0x34, 0x90}; //Wr_flash_en=1 and spim_cs=1

    u8 data[pageSize+1];
    data[0] = headOfWriteData;

    //printk("Size=%d, data_size:%d \n", binSize, sizeof(data));
    do 
    {
        set_reg5[3] = (sendFinished & 0xFF00)>>8;//xadr:A15~A8  
        set_reg5[4] = (sendFinished & 0x00FF);//yadr:A7~A0

        ret = wp1005_i2c_txdata(set_reg1, 2);
        if (ret < 0)
        {
            printk("wp1: tx error: set_reg1: %d \n", ret);
            return -1;
        }

        ret = wp1005_i2c_txdata(set_reg2, 2);
        if (ret < 0)
        {
            printk("wp1: tx error: set_reg2: %d \n", ret);
            return -1;
        }

        ret = wp1005_i2c_txdata(set_reg3, 2);
        if (ret < 0)
        {
            printk("wp1: tx error: set_reg3: %d \n", ret);
            return -1;
        }

        ret = wp1005_i2c_txdata(set_reg4, 2);
        if (ret < 0)
        {
            printk("wp1: tx error: set_reg4: %d \n", ret);
            return -1;
        }

        ret = wp1005_i2c_txdata(set_reg5, 5);
        if (ret < 0)
        {
            printk("wp1: tx error: set_reg5: %d \n", ret);
            return -1;
        }

        do
        {
            for (i=0; i<pageSize; i++)
                data[i+1] = src[sendFinished + partFinished + i];

            //Set Write Enable Latch(WEL) 		
            ret = wp1005_i2c_txdata(data, (pageSize + 1));
            if (ret < 0)
            {
                printk("wp1: tx error: set data: %d \n", ret);
                return -1;
            }
            partFinished += pageSize;

            if ((partFinished + pageSize) > partRegion)
            {
                pageSize = partRegion - partFinished;			
            }
            //printk("Part: %d %d\n", partFinished, pageSize);
        }while(partFinished != partRegion);

        partFinished = 0;
        pageSize = partSize;
        ret = wp1005_i2c_txdata(set_reg6, 2);
        if (ret < 0)
        {
            printk("wp1: tx error: set_reg6: %d \n", ret);
            return -1;
        }

        sendFinished += partRegion; //pageSize;

        if ((sendFinished + partRegion) > binSize)
        {
            partRegion = binSize - sendFinished;			
        }

        //printk("Finished:%d \n", sendFinished);
        msleep(1); //delay 1 ms [Unit:0.001ms], need to delay 0.4 ~ 0.8 ms
    } while(sendFinished != binSize);

    return 1;
}

// ----------------------------------------------------------------------------------------------------------
static int fw_download_05(int loadMode)   // 1= load bin from SDCARD, 2= load bin from inc
{
    mm_segment_t 	oldfs;
    int binSize = 0;    
    int i = 0, ret = 0; 
    u16 chksum = 0;
  struct file *fp_bin = NULL;

  printk("wp1: in fw_down: %d \n", loadMode);
    //memset(File_inc, 0xFF, sizeof(File_inc));
    // ====================== Make sure where is the bin file ===========================/
  if ((loadMode == 1) || (loadMode == 3))
    {
        memset(File_inc, 0xFF, sizeof(File_inc));
        //initKernelEnv();
        oldfs = get_fs();
        set_fs(KERNEL_DS);

    if (loadMode == 1)
		fp_bin = filp_open(str,O_RDONLY,0);
	else if (loadMode == 3)
	{
		printk("wp1: path: %s \n", ch_buf);
		fp_bin = filp_open(ch_buf,O_RDONLY,0);
	}
	
	printk("wp1: fp_bin=0x%x \n", fp_bin);
	if (IS_ERR(fp_bin))
	{
		printk("wp1: fp_bin ERROR \n");
		return -1;
	}
        binSize = readFile_t(fp_bin,File_inc, sizeof(File_inc));

        filp_close(fp_bin,	NULL);
        set_fs(oldfs);

        if (binSize > 0)
        {
            for (i=0; i<binSize; i++)
                chksum += File_inc[i];	
        }
    }
    else if (loadMode == 2)
    {	
        binSize = sizeof(File_inc) -3 ;
        for (i=3; i<(binSize+3); i++)
        {
            //File_inc[i] = File_inc[3+i];
            chksum += File_inc[i];
        }		
    }
    // ==================================================================================/
    printk("wp1:BinSize=%d; chsum=0x%x \n", binSize, chksum);

    if (binSize > 0)
    {	
        ret = Flash_Open();
        if (ret < 0)
        {
            printk("wp1: Flash Open Fail, update terminated \n");
            return -1;
        }
        Flash_Erase();

    if ((loadMode == 1) || (loadMode == 3))
            ret = Flash_Write_05(File_inc, binSize);	
        else if (loadMode == 2)
            ret = Flash_Write_05(&File_inc[3], binSize);
	else
	{
		printk("ERROR COMMAND in DOWNLOAD!!!!!!! \n");
		return -1;
	}
        if (ret < 0)
        {
            printk("wp1: Flash write Fail, update terminated \n");
            return -1;
        }

        ret = Flash_Close(binSize, chksum);

        if (ret < 0)
        {
            printk("wp1: Flash Close Fail, update terminated \n");
            return -1;
        }
    }
    else
  {
        printk("wp1:Fail to read bin file ! \n");

		return -1;
}

  return 1;
}
// ----------------------------------------------------------------------------------------------------------------
static void i2c_toggle(void)
{
    int ret = 0;
    u8 cmd = 0x03;
    u8 tmp = 0;
    ret = wp1005_i2c_wr(&cmd, 1, &tmp, 1);
    if (ret<0)
        printk("wp1:Send i2c toggle failed:%d \n", ret);
    else
        printk("wp1:Send i2c toggle cmd OK => %d \n", ret);

}
// ---------------------------------------------------------------------------------------------------------------------------
static void sleep_test(void)
{
    int ret = 0;
    u8 cmd = 0x20;
    ret = wp1005_i2c_txdata(&cmd, 1);
    if (ret<0)
        printk("wp1:Send sleep failed:%d \n", ret);
    else
        printk("wp1:Send slepp cmd OK ! \n");

}
// ----------------------------------------------------------------------------------------------------------------
static int Read_ChipID(void)
{
    int ret = 0;
    u8 cmd = 0x2D;
    u8 ChipID[2] = {0x00, 0x00};
    ret = wp1005_i2c_wr(&cmd, 1, ChipID, 2);
    if (ret<0)
        return ret;	
    else
    {
        printk("wp1:ChipID: %x %x \n", ChipID[0], ChipID[1]);
        if ((ChipID[0]==0x10) && (ChipID[1]==0x05))
            ret = 5;
        else
            ret = 4;

        return ret;
    }

}
// ---------------------------------------------------------------------------------------------------------------
static ssize_t show_i2c_rw(struct device *dev, struct device_attribute *attr, char *buf)
{        
    return snprintf(buf,PAGE_SIZE,"%s\n","I2C Read/Write Data is ok");
}

static ssize_t store_i2c_rw(struct device *dev, 
        struct device_attribute *attr, 
        const char *buf, size_t count) 
{ 					
    int UserInput = 0;
    int ret = 0;
    sscanf(buf, "%x", &UserInput);
  char tmpp[10]="abcde";
    WP1005_PRINT(1,"wp1:%d: 0x%x, count:%d \n", UserInput, UserInput, count);

    if (UserInput > 0x100)
    {
        u8 read_reg, read_index= 0;
        read_reg = UserInput >> 8;
        read_index = UserInput & 0xFF;
        WP1005_PRINT(1,"wp1: %x %x \n", read_reg, read_index);
        wp1005_read_register(read_reg, read_index);
    }
    else
    {
        switch(UserInput){
            case 0x1 :
                break;
            case 0x2 :
                wp1005_Mutualmode_Delta();
                break;

            case 0x3 :
                printk("wp1:=== 05 FW UPDATE==== \n");
                mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
                printk("wp1:===  disable IRQ==== \n");
		//UPDATENOSLEEP = 1;
        ret = fw_download_05(1); // 1= load bin from SDCARD, 2= load bin from inc, 3=apk_path
		//UPDATENOSLEEP = 0;
                msleep(1);
		if (ret >0)
		{		
                Prot_Init();
                msleep(1);
                get_header();
                print_header();
                mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);
                printk("wp1:====  enable IRQ==== \n");
		}
		else
			printk("wp1:==== Download FW FAIL ==== \n");
                break;

            case 0x4 :
                wp1005_Mutualmode_Baseline();
                break;
            case 0x5 :
                wp1005_Mutualmode_RawData();
                break;
            case 0x6 :
                wp1005_selfmode_Delta();
                break;
            case 0x7 :
                wp1005_selfmode_RawData();
                break;
            case 0x8 :
                wp1005_selfmode_Baseline();
                break;
            case 0x9 :
                wp1005_setmode_none();
                break;
            case 0x10 :
                //wp1005_unlock_i2c();
                break;
            case 0x11 :
                get_header();
                print_header();
                break;
            case 0x12 :
                //wp1005_unlock_i2c();
                //Flash_Erase();
                break;
            case 0x13 : 
                wp1005_check_update_inc();
                break;

            case 0x14 : 
		printk("wp1:=== DOWNLOAD INC FILE==== \n");
		mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
		printk("wp1:===  disable IRQ ==== \n");
		//UPDATENOSLEEP = 1;
        ret = fw_download_05(2);  // 1= load bin from SDCARD, 2= load bin from inc, 3=apk_path
		//UPDATENOSLEEP = 0;
		msleep(1);
		if (ret > 0)
		{		
			Prot_Init();
			msleep(1);
			get_header();
			print_header();
			mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);
			printk("wp1:====  enable IRQ==== \n");
		}
		else
			printk("wp1:==== Download FW FAIL ==== \n");
                break;	

            case 0x15 : 
                sleep_test();
                break;

            case 0x16 : 
                i2c_toggle();
                msleep(2);

                break;	

            case 0x17 : 
                Flash_Checksum2();
                break;
            case 0x18 : 
                ret = Read_ChipID();			
                break;	
            case 0x19 : 
                ret = Prot_Init();			
                break;
            case 0x20 :  // disable
                mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
                break;
            case 0x21 :  //  enable IRQ
                mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);
                break;

	   case 0x22:
	     printk("wp1: tmp:%s \n", tmpp);
		 memset(tmpp, 0x00, sizeof(tmpp));
		 printk("wp1: after tmp:%s \n", tmpp);
		 break;
		 
	case 0x23:
	     printk("wp1: ic reset \n");
		 wp1005_ic_reset();
		 break;
		 
            default:
                printk("wp1: error\n");
                break;
        }
    }
    return strnlen(buf, PAGE_SIZE); 
}

static ssize_t wp1005_cmd_protocol_show(struct device *dev, struct device_attribute *attr, char *buf)
{
    return snprintf(buf,PAGE_SIZE,"%s\n","1");
}

static ssize_t wp1005_cmd_protocol_store(struct device * dev, struct device_attribute *attr, const char * buf,size_t count)
{
    int UserInput = 0;
    /*Write value to device*/
    sscanf(buf, "%d", &UserInput);

    WP1005_PRINT(1,"wp1: Decimalism :%d\n",UserInput);

    //wp1005_dec_to_binary(UserInput);  
    wp1005_apk_cmd_analysis(UserInput); 

    return strnlen(buf, PAGE_SIZE);
}

static ssize_t show_wp1005_print_msg(struct device *dev, struct device_attribute *attr, char *buf)
{
    return sprintf(buf, "%d \n",1);
}

static ssize_t store_wp1005_print_msg(struct device * dev, struct device_attribute *attr, const char * buf,size_t count)
{   
    int UserInput = 0;
    sscanf(buf, "%x", &UserInput);

    Wp1004PrintLevel = UserInput;
    /*
       switch(UserInput){
       case 0x0  :
       Wp1004PrintLevel = 0;
       break;
       case 0x1  :
       Wp1004PrintLevel = 3;
       break;
       default :
       break;
       } */
    return strnlen(buf, PAGE_SIZE);
}

static ssize_t wp1005_i2c_wr_bytes_show(struct device *dev, struct device_attribute *attr, char *buf)
{
    return sprintf(buf, "%d \n",1);
}

static ssize_t wp1005_i2c_wr_bytes_store(struct device * dev, struct device_attribute *attr, const char * buf,size_t bytes)
{
    // int count=0,power=1;
    int UserInput = 0;
    sscanf(buf, "%d", &UserInput);
    /*  // owen marked
        if((UserInput <= 5) && (UserInput >= 2)){
        while(count < UserInput)
        {
        power += power;
        count++;
        }
        FWUPDATEWRITELEN = power;
        printk("wp1: FWUPDATEWRITELEN : %d : pow : %d\n",FWUPDATEWRITELEN,power);
        } else {
        FWUPDATEWRITELEN = 4;
        printk("wp1: FWUPDATEWRITELEN : %d\n",FWUPDATEWRITELEN);
        }
     */
    return strnlen(buf, PAGE_SIZE);
}

static ssize_t wp1005_data_log_show(struct device *dev, struct device_attribute *attr, char *buf){
    return sprintf(buf, "%d \n",1); //add by jan
}


static ssize_t wp1005_data_log_store(struct device * dev, struct device_attribute *attr, const char * buf,size_t bytes){

    int setType = 0;
    sscanf(buf, "%d", &setType);
    IsDataLogbyDriver = (setType - 90); //Jack add, since APK can not send 0 to driver so all value plus 90 by APK
    if(pLogDataFile != NULL && IsDataLogbyDriver <= 0)//close log file
        wp1005_log_file_close();

    return strnlen(buf, PAGE_SIZE);
} 


//-------------Touch Data--------------------
static ssize_t wp1005_touch_data_store(struct device * dev, struct device_attribute *attr, const char * buf,size_t bytes){

    int UserInput = 0;
    sscanf(buf, "%d", &UserInput); 
    if(UserInput == 1)
        IsReportTouchDataToAPK = 1;
    else
        IsReportTouchDataToAPK = 0;   
    return strnlen(buf, PAGE_SIZE); //add jan 20131216
} 

static ssize_t wp1005_touch_data_show(struct device *dev, struct device_attribute *attr, char *buf){
    return sprintf(buf, "wp1: IsReportTouchDataToAPK = %d \n",IsReportTouchDataToAPK); //add by jan
}

//---------------Key Data-----------------------   
static ssize_t wp1005_key_data_store(struct device * dev, struct device_attribute *attr, const char * buf,size_t bytes){

    int UserInput = 0;
    sscanf(buf, "%d", &UserInput); 
    if(UserInput == 1)
        IsReportKeyDataToAPK = 1;
    else
        IsReportKeyDataToAPK = 0;  
    return strnlen(buf, PAGE_SIZE); //add jan 20131216		
} 

static ssize_t wp1005_key_data_show(struct device *dev, struct device_attribute *attr, char *buf){
    return sprintf(buf, "wp1: IsReportKeyDataToAPK = %d \n",IsReportKeyDataToAPK); //add by jan
}   

//----------------FW Update -----------------------------------------------------------------------------------------------
static ssize_t wp1005_fw_upsate_status_store(struct device * dev, struct device_attribute *attr, const char * buf,size_t bytes){

    int UserInput = 0;
    sscanf(buf, "%d", &UserInput); 
    if(UserInput == 1)
        IsReportFWUpdateStatusToAPK = 1;
    else
        IsReportFWUpdateStatusToAPK = 0;   
    return strnlen(buf, PAGE_SIZE); //add jan 20131216
} 

static ssize_t wp1005_fw_upsate_status_show(struct device *dev, struct device_attribute *attr, char *buf){
    return sprintf(buf, "wp1: IsReportFWUpdateStatusToAPK = %d \n",IsReportFWUpdateStatusToAPK); //add by jan
} 

//------------------------------------------------------------------------------------------------------------------------------
static struct device_attribute winic_i2c_attrs[] = {
    {
        .attr   = {
            .name = "i2c_rw",  
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },
        .show  = show_i2c_rw,  
        .store = store_i2c_rw, 
    },
    {
        .attr   = {
            .name = "print_messages",
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },
        .show  = show_wp1005_print_msg,
        .store = store_wp1005_print_msg,
    },
    {
        .attr   = {
            .name = "cmd_protocol",
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },
        .show  = wp1005_cmd_protocol_show,
        .store = wp1005_cmd_protocol_store,
    },
    /*
       {
       .attr   = {
       .name = "update_fw",
       .mode = S_IRWXU | S_IRWXG | S_IRWXO,
       },
       .show  = wp1005_update_fw_show,
       .store = wp1005_update_fw_store,
       },
     */
    {
        .attr   = {
            .name = "i2c_wr_bytes",
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },
        .show  = wp1005_i2c_wr_bytes_show,
        .store = wp1005_i2c_wr_bytes_store,
    },
    {
        .attr   = {
            .name = "data_log",    //1: log self, 2: log mutual, 3: log dual, 0: log disable
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },  //add by jan
        .show  = wp1005_data_log_show,
        .store = wp1005_data_log_store,
    },
    {
        .attr   = {
            .name = "touch_data",    //1: enable send touch data up to APK, 0: disable
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },  
        .show  = wp1005_touch_data_show,
        .store = wp1005_touch_data_store,
    },
    {
        .attr   = {
            .name = "key_data",    //1: enable send key data up to APK, 0: disable
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },  
        .show  = wp1005_key_data_show,
        .store = wp1005_key_data_store,
    },
    {
        .attr   = {
            .name = "fw_upsate_status",    //1: enable send key data up to APK, 0: disable
            .mode = S_IRWXU | S_IRWXG | S_IRWXO,
        },  
        .show  = wp1005_fw_upsate_status_show,
        .store = wp1005_fw_upsate_status_store,
    },
};

static int winic_i2c_create_attrs(struct device * dev)
{
    int i, rc;

    for (i = 0; i < ARRAY_SIZE(winic_i2c_attrs); i++) {
        rc = device_create_file(dev, &winic_i2c_attrs[i]);
        if (rc)
            goto winic_i2c_attrs_failed;
    }
    goto succeed;

winic_i2c_attrs_failed:
    while (i--)
        device_remove_file(dev, &winic_i2c_attrs[i]);
succeed:
    return rc;
}
//----------------------------------------------------------------------------------------------------------------------------
static int register_wp1005_char_dev(void)
{
    int ret = 0;
    //struct device *dev = &winic_i2c_data_ptr->client->dev;
    struct device *dev = &i2c_client->dev;
    //dev = winic_i2c_data_ptr->i2c_client->dev;
    chardev = cdev_alloc();
    if(chardev == NULL){
        return -1;
    }
    //printk("before dev_id2=%d \n", dev_id2);
    if(alloc_chrdev_region(&dev_id2, 0, 10, "wp1004"))    // Owen attention
    {
        printk(KERN_ALERT"Register char dev error\n");
        return -1;
    } 
    cdev_init(chardev, &wp1005_ioctl_fops);
    if(cdev_add(chardev, dev_id2, 1)){
        printk(KERN_ALERT"Add char dev error!\n");
    }
    //printk("after dev_id2=%d \n", dev_id2);
    winic_i2c_class2 = class_create(THIS_MODULE, "wp");
    if(IS_ERR(winic_i2c_class2)) {
        printk("Err: failed in creating class2. :%d :%s\n",__LINE__,__FUNCTION__);
        return -1;
    }

    // register your own device in sysfs, and this will cause udevd to create corresponding device node 
    dev = device_create(winic_i2c_class2, dev, dev_id2, NULL, "wp1004");	// Owen attention
    ret = winic_i2c_create_attrs(dev);
    printk("wp1:==== winic_i2c_create_attrs(dev) Finished:%d ==== \n", ret);
    return 0;
}
//----------------------------------------------------------------------------------------------------------------------------

static int wp1005_ic_reset(void)				//Jack 20131210 add for re-start ic
{
    //int ret = 0;
    mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
    mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);
    msleep(10);
    mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
    mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
    msleep(10);
    //u8 read_data = 0xFF; 
    //u8 read_cmd[2] = {MSG_GET_REPORTS, 0}; 
    //ret = wp1005_i2c_wr(read_cmd, 2, &read_data, 1);    

    CheckStateBuf[0] = 0;
    Wp1004DebugMode = 0;
    IsDataLogbyDriver = 0;
    IsReportTouchDataToAPK = 0;
    IsReportKeyDataToAPK = 0;
    CurDbgDataType = DATA_ALL_NONE; //Jack 20131220 add to reset current data type

    Prot_Init();
    msleep(1);
    get_header();//Jack 20131214 add to fixed only one finger report issue after system suspend or reset
    msleep(1);
    print_header();
    return 0;
}
//------------------------------------------------------------------------------------------------------------------------------------
static void wp1005_read_register(u8 reg, u8 idx) 
{ 
    int ret = 0;  
    u8 read_cmd[3] = {0x0c, reg, idx}; 
    WP1005_PRINT(1,"wp1:read register cmd: 0x%02x, 0x%02x, 0x%02x \n", read_cmd[0], read_cmd[1], read_cmd[2]); 
    ret = wp1005_i2c_wr(read_cmd, 3, &RegDataBuf[0], 1); 
    if (ret<0) 
        printk("Read Register FAIL ! \n"); 
    else 
        WP1005_PRINT(1,"wp1:Register: reg:0x%02x idx:%d = 0x%02x \n", reg, idx, RegDataBuf[0]); 
} 
//#endif
//------------------------------------------------------------------------------------------------------------------------------------
static void print_header(void)
{
    printk("wp1:Mutual_Node_No:%d,Self_CH_No:%d\n",Mutual_Node_No,Self_CH_No);
    printk("wp1:tpheader.PROT_VER :0x%x\n",tpheader.PROT_VER);
    printk("wp1:tpheader.ID :0x%x\n",tpheader.ID);
    printk("wp1:tpheader.HW_VER :0x%x\n",tpheader.HW_VER);
    printk("wp1:tpheader.FW_VER :0x%x\n",tpheader.FW_VER); 
    printk("wp1:tpheader.SERIAL_NO :0x%x\n",tpheader.SERIAL_NO);
    printk("wp1:tpheader.XL_SIZE(Rx):0x%x (%d)\n",tpheader.XL_SIZE, tpheader.XL_SIZE);
    printk("wp1:tpheader.YR_SIZE(Tx):0x%x (%d)\n",tpheader.YR_SIZE, tpheader.YR_SIZE);
    printk("wp1:tpheader.RES_X:0x%x (%d)\n",tpheader.RES_X, tpheader.RES_X);
    printk("wp1:tpheader.RES_Y:0x%x (%d)\n",tpheader.RES_Y, tpheader.RES_Y);
    //printk("tpheader.SERIAL_NO :0x%08x\n",serial);
    printk("wp1:tpheader.SUPPORT_FINGERS :0x%x\n",tpheader.SUPPORT_FINGERS);
    printk("wp1:tpheader.KEYS_NUM :0x%x\n"	,tpheader.KEYS_NUM);
    printk("wp1:tpheader.MAX_RPT_LEN :0x%x\n",tpheader.MAX_RPT_LEN);
    printk("wp1:tpheader.CAP_1 :0x%x\n",tpheader.CAP_1);
    printk("wp1:tpheader.CAP_2 :0x%x\n",tpheader.CAP_2);
    printk("wp1:tpheader.V_ID:0x%x\n",	tpheader.V_ID);
    printk("wp1:tpheader.P_ID:0x%x\n",	tpheader.P_ID);


}
// --------------------------------------------------------------------------------------------------------------
static int get_header(void)
{
    int i = 0;
    //int ret = 0;

    uint8_t HeaderCmd[2] = {MSG_GET_HEADER, 0x00};

    printk("wp1:  Get_Header() \n");
    for(i = 0; i <(32>>3);i++){
        HeaderCmd[1] = i;
        wp1005_i2c_wr(HeaderCmd, 2, &HeaderBuf[i<<3], 8);
        //printk("i = %d\n",i);
    } 

    for(i = 0; i <32; i++){
        printk("%02x ", HeaderBuf[i]);
    }
    printk("\n");

    save_wp1005_header_data();	

    return 0;
}
// --------------------------------------------------------------------------------------------------------------
static void save_wp1005_header_data(void)
{

    tpheader.PROT_VER 		= HeaderBuf[0];
    tpheader.ID 				= HeaderBuf[1];
    tpheader.HW_VER 			= HeaderBuf[2]; 
    tpheader.FW_VER 			= HeaderBuf[3];
    //P_ID   						= (HeaderBuf[10] << 8) |(HeaderBuf[11]);    //wp1005
    tpheader.P_ID   			= (HeaderBuf[11] << 8) | (HeaderBuf[10]);    //wp1005
    tpheader.V_ID 				= (HeaderBuf[9] << 8) | (HeaderBuf[8]);
    tpheader.SERIAL_NO 			= (HeaderBuf[7] << 24) | (HeaderBuf[6] << 16) | (HeaderBuf[5] << 8) | (HeaderBuf[4]);
    //printk("tpheader.FW_VER :0x%x\n",tpheader.FW_VER);
    tpheader.RES_X 				= (HeaderBuf[13] << 8) |(HeaderBuf[12]);    //wp1005
    tpheader.RES_Y 				= (HeaderBuf[15] << 8) |(HeaderBuf[14]);    //wp1005
    tpheader.XL_SIZE 			= HeaderBuf[16];
    tpheader.YR_SIZE 			= HeaderBuf[17];
    tpheader.SUPPORT_FINGERS 	= HeaderBuf[18];
    tpheader.KEYS_NUM 			= HeaderBuf[19];
    tpheader.MAX_RPT_LEN		= HeaderBuf[20];
    tpheader.CAP_1				= HeaderBuf[21];
    tpheader.CAP_2   			= HeaderBuf[22];

    if (tpheader.XL_SIZE*tpheader.YR_SIZE  < 500)
    {
        Mutual_Node_No = tpheader.XL_SIZE * tpheader.YR_SIZE;
        Self_CH_No = tpheader.XL_SIZE + tpheader.YR_SIZE;

        printk("wp1:== XL * YR == %d * %d == \n", tpheader.XL_SIZE, tpheader.YR_SIZE);

        SelfDataLen[0] = tpheader.XL_SIZE + tpheader.YR_SIZE;
        SelfLen = (tpheader.XL_SIZE + tpheader.YR_SIZE)*2;
        MutualLen1 = tpheader.XL_SIZE * tpheader.YR_SIZE ;
        MutualLen2 = MutualLen1*2;
		printk("WP1198 driver insmod OK; FW_VER=%x, PID=0x%x 20140417\n", tpheader.FW_VER, tpheader.P_ID);
    }
    else
        printk("wp1:Wrong Tx/ Rx Line Number: %d * %d\n", tpheader.XL_SIZE, tpheader.YR_SIZE);


}
//=================================================================================================
#ifdef TPTIMER
static void wp1005_update_inc( struct work_struct *work )
{
    printk("wp1: wp1005_update_inc func.\n");
    wp1005_check_update_inc();
}

static enum hrtimer_restart wp1005_tp_timer(struct hrtimer *handle)
{
    printk("wp1: wp1005_tp_timer func.\n");
    queue_work(wp1005_wq, &wp1005_update.update_inc);
    return HRTIMER_NORESTART;
}
#endif


// ------------------------------------------------------------------------------------------------------------ 
static int Prot_Init(void)
{
    int ret = 0;
    u8 set1[6] = {0x06, 0xFF, 0xA5, 0xB1, 0xC6, 0x54}; 	
    //u8 set2[3] = {0x1F, 0x00, 0x00};
    ret = wp1005_i2c_txdata(set1, 6);
    if (ret < 0)
    {
        printk("wp1:tx error: Init1 Fail: %d \n", ret);
        return -1;
    }

    printk("wp1: Prot init OK \n");
    return ret;
}
// ------------------------------------------------------------------------------------------------------------ 
static int __devinit tpd_probe(struct i2c_client *client, const struct i2c_device_id *id)
{	 
#ifdef JAN_SET_CLASS
    struct device *dev = &client->dev;
#endif	
    char data[8] = {0};
    int retval = TPD_OK;
    //u8 report_rate=0;
    int err=0;
    // int reset_count = 0;
  int ret=0;
    u8 read_cmd[2] = {MSG_GET_REPORTS, 0x00}; 

    printk("wp1: probe:0x%x \n", client->addr);
    //reset_proc:   
    i2c_client = client;

    //hwPowerOn(MT65XX_POWER_LDO_VGP4, VOL_2800, "TP");


#ifdef GPIO_CTP_EN_PIN
    mt_set_gpio_mode(GPIO_CTP_EN_PIN, GPIO_CTP_EN_PIN_M_GPIO);
    mt_set_gpio_dir(GPIO_CTP_EN_PIN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CTP_EN_PIN, GPIO_OUT_ONE);
#endif

#ifdef TPD_POWER_SOURCE_CUSTOM                           
    hwPowerOn(TPD_POWER_SOURCE_CUSTOM, VOL_2800, "TP");    
#endif

#ifdef TPD_POWER_SOURCE_1800
    hwPowerOn(TPD_POWER_SOURCE_1800, VOL_1800, "TP");
#endif

    mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
    mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);  
  msleep(5);
    mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
    mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);
    msleep(10);
    mt_set_gpio_mode(GPIO_CTP_EINT_PIN, GPIO_CTP_EINT_PIN_M_EINT);
    mt_set_gpio_dir(GPIO_CTP_EINT_PIN, GPIO_DIR_IN);
    mt_set_gpio_pull_enable(GPIO_CTP_EINT_PIN, GPIO_PULL_ENABLE);
    mt_set_gpio_pull_select(GPIO_CTP_EINT_PIN, GPIO_PULL_UP);
    // org: enable irq
    //mt65xx_eint_set_sens(CUST_EINT_TOUCH_PANEL_NUM, CUST_EINT_TOUCH_PANEL_SENSITIVE);
    mt_eint_set_hw_debounce(CUST_EINT_TOUCH_PANEL_NUM, CUST_EINT_TOUCH_PANEL_DEBOUNCE_CN);
    mt_eint_registration(CUST_EINT_TOUCH_PANEL_NUM,EINTF_TRIGGER_FALLING,tpd_eint_interrupt_handler, 1); 

    msleep(100);
    mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
    printk("wp1: probe 2\n");
    tpd_flag = 0;   // initial para
    //WP1005Key = 0;  // initial para
    tpd_load_status = 1; 

    ret = register_wp1005_char_dev();
    printk("wp1==== register_wp1005_char_dev() Finished: %d ==== \n", ret);

    //org: ktread here
#ifdef JAN_SET_CLASS
    printk("wp1: probe 4\n");
    winic_i2c_class = class_create(THIS_MODULE, "WINIC_TP_FW");

    if (IS_ERR(winic_i2c_class)) {
        printk(KERN_WARNING "wp1: Can not to create winic tp class : error = %ld\n", PTR_ERR(winic_i2c_class));
        winic_i2c_class = NULL;
    }

    dev = device_create(winic_i2c_class, dev, dev_id, NULL, "WINIC_TP");
    register_wp1005_char_dev();
    winic_i2c_create_attrs(dev);
#endif

    ret = Prot_Init();
    msleep(1);
    get_header();
    print_header();
    msleep(1);

    ret = wp1005_check_update_inc(); 
    if (ret>0)
    {
        //mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
		//UPDATENOSLEEP = 1;
		err = fw_download_05(2);// 1= load bin from SDCARD, 2= load bin from inc, 3=apk_path
		//UPDATENOSLEEP = 0;
        msleep(1);
		if (err > 0)
		{
        Prot_Init();
        msleep(1);
        get_header();
        print_header();
        msleep(1);
    }
		else
			printk("wp1: AUTO DOWN INC FAIL \n");
	}
    thread = kthread_run(touch_event_handler, 0, TPD_DEVICE);
    if (IS_ERR(thread))
    { 
        retval = PTR_ERR(thread);
        TPD_DMESG(TPD_DEVICE "wp1:  failed to create kernel thread: %d\n", retval);
    }

    isChargerOnNow = false;  // initial para
    CheckStateBuf[0] = 0;
    Wp1004DebugMode = 0;
    CurDbgDataType = DATA_ALL_NONE;
    pLogDataFile = NULL;
    mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);
    return 0;   
}

static int __devexit tpd_remove(struct i2c_client *client)
{
    WP1005_PRINT(1,"wp1: wp1005 tpd_remove \n");
    return 0;
}

static int tpd_local_init(void)
{
    if(i2c_add_driver(&tpd_i2c_driver)!=0)
    {
        TPD_DMESG("wp1: File_inc unable to add i2c driver.\n"); 
        return -1;
    }
    if(tpd_load_status == 0) 
    { 
        TPD_DMESG("wp1: wp1005 add error touch panel driver.\n");
        i2c_del_driver(&tpd_i2c_driver);
        return -1;
    }

#ifdef TPD_HAVE_BUTTON     
    tpd_button_setting(TPD_KEY_COUNT, tpd_keys_local, tpd_keys_dim_local);// initialize tpd button data
#endif   

#if (defined(TPD_WARP_START) && defined(TPD_WARP_END))    
    TPD_DO_WARP = 1;
    memcpy(tpd_wb_start, tpd_wb_start_local, TPD_WARP_CNT*4);
    memcpy(tpd_wb_end, tpd_wb_start_local, TPD_WARP_CNT*4);
#endif 

#if (defined(TPD_HAVE_CALIBRATION) && !defined(TPD_CUSTOM_CALIBRATION))
    memcpy(tpd_calmat, tpd_def_calmat_local, 8*4);
    memcpy(tpd_def_calmat, tpd_def_calmat_local, 8*4);	
#endif  
    tpd_type_cap = 1;

    return 0; 
}

static void tpd_resume( struct early_suspend *h )
{

    printk("wp1: resume \n");
    //WP1005_PRINT(1, "wp1: resume \n");
    //KeyType = 0;// initial para
    tpd_flag = 0;   // initial para
    isChargerOnNow = false;   // initial para

        mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM); 
        WP1005_PRINT(1, "wp1: resume: RESET \n");
        mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
        mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
        mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ZERO);  
    msleep(5);  
        mt_set_gpio_mode(GPIO_CTP_RST_PIN, GPIO_CTP_RST_PIN_M_GPIO);
        mt_set_gpio_dir(GPIO_CTP_RST_PIN, GPIO_DIR_OUT);
        mt_set_gpio_out(GPIO_CTP_RST_PIN, GPIO_OUT_ONE);

    msleep(10);
        ///tpd_up(0,0,0);
        wp1005_ts_release();
	WP1005_PRINT(1, "wp1: INIT \n");
        //input_sync(tpd->dev);
        Prot_Init();
        msleep(1);
	WP1005_PRINT(1, "wp1: get header \n");
        get_header();
        msleep(1);
        mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM); 
	wp1005_ac_judge();
	WP1005_PRINT(1, "wp1: resume finished \n");
  

}

static void tpd_suspend( struct early_suspend *h )
{
	printk("wp1: SUSPEND \n");
	mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM); // disable IRQ
	//wp1005_ts_release();
	sleep_test();
    
} 

static struct tpd_driver_t tpd_device_driver = {
    .tpd_device_name = "wp1005",  // WP1005
    .tpd_local_init = tpd_local_init,
    .suspend = tpd_suspend,
    .resume = tpd_resume,
#ifdef TPD_HAVE_BUTTON
    .tpd_have_button = 1,
#else
    .tpd_have_button = 0,
#endif		
};
/* called when loaded into kernel */
static int __init tpd_driver_init(void) 
{
#ifdef TPTIMER
    wp1005_wq = create_singlethread_workqueue("wp1005_wq");
#endif

    i2c_register_board_info(0, &wp1005_i2c_tpd, 1);
    printk("wp1: regist WP1005 finished \n");
    if(tpd_driver_add(&tpd_device_driver) < 0)
        TPD_DMESG("wp1: add WP1005 driver failed\n");

    return 0;
}

/* should never be called */
static void __exit tpd_driver_exit(void) 
{
    /*
#ifdef UPDATEFW
dev_id = MKDEV(major, 0);
unregister_chrdev_region(MKDEV(DP_MAJOR,DP_MINOR),10);
cdev_del(chardev);
device_destroy(winic_i2c_class, dev_id);
class_destroy(winic_i2c_class);
#endif
tpd_driver_remove(&tpd_device_driver);
     */
    printk(KERN_ALERT "%s\n", __FUNCTION__);
    device_destroy(winic_i2c_class2, dev_id2); 
    class_destroy(winic_i2c_class2);
    unregister_chrdev_region(dev_id2,	10);
    cdev_del(chardev);

    tpd_driver_remove(&tpd_device_driver);
}
module_init(tpd_driver_init);
module_exit(tpd_driver_exit);
//-------------------------------------------------------------------------------
#ifdef DEBUGAPK

static ssize_t wp1005_ioctl_write (struct file *filp, const char __user *buff, size_t count, loff_t *pos)
{
    if (count > 259)
        return -EINVAL;
    MsgBuf[count] = '\0';
    return count;
}
//--------------------------------------------------------------------------------------------------------
static ssize_t wp1005_ioctl_read(struct file *filp, char __user *buf, size_t count, loff_t *f_pos)
{
    return (simple_read_from_buffer(buf, count,f_pos,HeaderBuf, 200));
}
//--------------------------------------------------------------------------------------------------------
static int wp1005_ioctl_open(struct inode *inode, struct file *filp)
{
    struct wp1005_ioctl_data *ioctl_data;

    ioctl_data = kmalloc(sizeof(struct wp1005_ioctl_data), GFP_KERNEL);

    if (ioctl_data == NULL)
        return -ENOMEM;

    rwlock_init(&ioctl_data->lock);
    ioctl_data->x        = tpheader.FW_VER;
    ioctl_data->y        = 0xFFFF;
    filp->private_data = ioctl_data;
    return 0;
}
//--------------------------------------------------------------------------------------------------------
static int wp1005_ioctl_close(struct inode *inode, struct file *filp)
{
    //int i;
    if (filp->private_data) {
        kfree(filp->private_data);
        filp->private_data = NULL;
    }
    return 0;
}
//--------------------------------------------------------------------------------------------------------
static long wp1005_ioctl(struct file *filp, unsigned int cmd, unsigned long arg)
{

    struct wp1005_ioctl_data *ioctl_data = filp->private_data;
    int retval = 0;
    int len = 1024;
  int file_name = 0;
  long int ascII[BIN_LENGTH];
    //unsigned char val;
    //uint8_t x;
    struct ioctl_arg data;
  WP1005_PRINT(1, "wp1: Ioctl : %lx:\n",cmd); 
    memset(&data, 0, sizeof(data));
    switch (cmd) {
        case IOCTL_RAWDATA:  //add by jan
            WP1005_PRINT(1,"IOCTL_RAWDATA : report data len(self,mutual)=%d, %d. line=%d \n",self_data_log_len, mutual_data_log_len, __LINE__);                                
            if(copy_to_user((char *)arg,RawBuf, self_data_log_len + mutual_data_log_len)){ //len)){ //Jack 20131205 add
                retval = -EFAULT;
                goto done;
            }                                               
            break;

                case IOCTL_REGREADDATA:  //add by jan
            if(copy_to_user((char *)arg, RegDataBuf, 1)){ //Jack 20131220 add to send register read data to APK. current version read 1 bytes only 
                retval = -EFAULT;
                goto done;
            }                
            break;            
            /*
               case IOCTL_USERDEF1:
               if(copy_to_user((char *)arg,UserDefBuf1, len)){
               retval = -EFAULT;
               goto done;
               }
               break;

               case IOCTL_USERDEF2:
               if(copy_to_user((char *)arg,UserDefBuf2, len)){
               retval = -EFAULT;
               goto done;
               }
               break;
             */
                case IOCTL_BUFGET: //add by jan
            WP1005_PRINT(1,"IOCTL_HEADERBUF : %d LINE\n",__LINE__);
            read_lock(&ioctl_data->lock);
            read_unlock(&ioctl_data->lock);                
            if(copy_to_user((char *)arg,HeaderBuf, len)){
                retval = -EFAULT;
                goto done;
            }
            break;

                case IOCTL_NOISEBUF: //add by jan
            WP1005_PRINT(1,"IOCTL_NOISEBUF : %d LINE\n",__LINE__);
            if(copy_to_user((char *)arg,NoisePtrBuf, NOISE_LEVEL_DATA_LEN)){
                retval = -EFAULT;
                goto done;
            }
            break;
                case IOCTL_CCVALUEBUF: //add by jan
            /*//CCValueFlag == 1;
              Wp1004DebugMode == 1;
            //printk("IOCTL_CCVALUEBUF : %d LINE\n",__LINE__);
            if(copy_to_user((char *)arg,CCValueBuf, len)){
            retval = -EFAULT;
            goto done;
            }*/
            break;

                case IOCTL_CHECKSTATE: //add by jan
            /*CheckStateBuf[0] = 2;
              printk("IOCTL_CHECKSTATE : %d LINE, %d\n",__LINE__,CheckStateBuf[0]);*/
			WP1005_PRINT(1,"wp1: IOCTL_CHECKSTATE : %d LINE, %d\n",__LINE__,CheckStateBuf[0]);
            if(copy_to_user((char *)arg,CheckStateBuf, len)){
                retval = -EFAULT;
                goto done;
            }
            break;
            //case IOCTL_VALSET:
            /*close_cc = arg; // write 0 ;
              wp1005_close_cc_value(close_cc);*/
            //	break;

                case IOCTL_VALSET_NUM:     //add by jan 
            WP1005_PRINT(1,"arg=%ld,IOCTL_VALSET_NUM : %d LINE\n",arg,__LINE__);
            ioctl_num = arg;
            //wp1005_dec_to_binary(ioctl_num);
            wp1005_apk_cmd_analysis(arg);
            break;

            /*case IOCTL_I2CBUFGET :
            //copy_to_user((char *)arg,&I2cBuf[3], I2cBuf[2]);                
            break; */
		case IOCTL_BIN_LENGTH:  //update fw status via APK by jan 20140124    
               // bin_length = arg;
                BIN_LENGTH = arg;
                WP1005_PRINT(1, "wp1: BIN_LENGTH: %d\n",BIN_LENGTH);
                break;
				
        case IOCTL_FILE_NAME_CHAR:  //update fw status via APK by jan 20140124                           
                //strcpy(wp1005_buffer,"");
                     
                APKFWUPDATE = 1;
                     file_name = arg;  
                     //fwcount++;                  
                     ascII[fwcount] = file_name;                     
                     WP1005_PRINT(1, "wp1: APKFWUPDATE=%d; file_name:%d,BIN_LENGTH: %d,ascII:%ld:count=%d\n",
											APKFWUPDATE,file_name,BIN_LENGTH,ascII[fwcount],fwcount) ;
                     wp1005_apk_file_analysis(/*file_name*/ascII[fwcount],fwcount);
                     fwcount++;
                     if(fwcount == BIN_LENGTH)
                           fwcount = 0;
                break;
		
		case IOCTL_GET_FW_UPDATE_STATUE:
                WP1005_PRINT(1,"wp1: IOCTL_GET_FW_UPDATE_STATUE : %d LINE\n",__LINE__);
                if(copy_to_user((char *)arg,ApkFWUpdateStatusData, len)){
                      retval = -EFAULT;
                      goto done;
                }
                break;

                case IOCTL_MUTUALDATALEN ://add by jan
            WP1005_PRINT(1,"%d,%d,IOCTL_MUTUALDATALEN : %d LINE\n",MutualDataLen[0],MutualDataLen[1],__LINE__);
            MutualDataLen[0] = tpheader.XL_SIZE;
            MutualDataLen[1] = tpheader.YR_SIZE;
            MutualDataLen[2] = 2; 
            if(copy_to_user((char *)arg,MutualDataLen, len)){
                retval = -EFAULT;
                goto done;
            }
            break;

                case IOCTL_SELFLDATALEN ://add by jan
            WP1005_PRINT(1,"SelfDataLen=%d, IOCTL_SELFLDATALEN\n",SelfDataLen[0]); // Owen: org: SelfDataLen only
            if(copy_to_user((char *)arg,SelfDataLen, len)){
                retval = -EFAULT;
                goto done;
            }
            break;

	case IOCTL_UPDATEFWBIN : //update fw status via APK by jan 20130724
				printk("wp1:UPDATEFWBIN \n ");
                if(copy_to_user((char *)arg,UpdateFWState, len)){
                      retval = -EFAULT;
                      goto done;
                }
                break;
        case IOCTL_CHECKBINFILE :
				printk("wp1:IOCTL_CHECKBINFILE \n ");
                if(copy_to_user((char *)arg,CheckBinFlag, len)){
                      retval = -EFAULT;
                      goto done;
                }
                break;
				
                case IOCTL_GET_TOUCH_DATA: 
            WP1005_PRINT(1,"ApkFingerData[0] = %d, IOCTL_GET_TOUCH_DATA : %d LINE\n",ApkFingerData[0],__LINE__);
            if(copy_to_user((char *)arg, ApkFingerData, sizeof(s16)*ApkFingerData[0]+1)){
                retval = -EFAULT;
                goto done;
            }
            break;
                case IOCTL_GET_KEY_DATA:
            WP1005_PRINT(1,"IOCTL_GET_KEY_DATA : %d LINE\n",__LINE__);
            if(copy_to_user((char *)arg, ApkKeyData, MAX_SUPPORT_KEY_NO)){
                retval = -EFAULT;
                goto done;
            }
            break;
            /*case IOCTL_GET_FW_UPDATE_STATUE
              WP1005_PRINT(1,"IOCTL_GET_FW_UPDATE_STATUE : %d LINE\n",__LINE__);
              if(copy_to_user((char *)arg,ApkFWUpdateStatusData, ){
              retval = -EFAULT;
              goto done;
              }
              break;*/

                default:
            retval = -ENOTTY;
            break;
            }
done:
            return retval;

    }
    //----------------------------------------------------------------------------------------------
    static int wp1005_get_current_sensing_mode(void){

        if((CurDbgDataType & 0x08) == 0x08)
            CurSensingMode = MUTUAL_MODE;
        else if((CurDbgDataType & 0x01) == 0x01)
            CurSensingMode = SELF_MODE;
        return 0;
    }
    //--------------------------------------------------------------------------------------------------
    //#ifdef WP1005RAWDEBUG
    static int wp1005_Mutualmode_Delta(void)
    {
        uint8_t data[3] = {0x4,0x0,0x38};       
        Wp1004DebugMode = 1;
        WP1005_PRINT(1,"wp1: Before CurDbgDataType=0x%02x\n",CurDbgDataType);
        CurDbgDataType = (CurDbgDataType & MUTUAL_DATA_NONE) | MUTUAL_DATA_DELTA; //Jack 20131204 add to record current data type    
        data[2] = CurDbgDataType;    
        WP1005_PRINT(1,"wp1: CurDbgDataType=0x%02x\n",CurDbgDataType);
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3);
        IsDataLogbyDriver = 1; // owen add
        if(IsDataLogbyDriver == 1)//Jack 20131219 add to handle log file name
            wp1005_log_file_name_handler(CurDbgDataType);
        return 0;      
    }
    //--------------------------------------------------------------------------------------------------
    static int wp1005_Mutualmode_RawData(void)
    {
        uint8_t data[3] = {0x4,0x0,0x18};        
        Wp1004DebugMode = 1;
        WP1005_PRINT(1,"wp1: Before CurDbgDataType=0x%02x\n",CurDbgDataType);
        CurDbgDataType = (CurDbgDataType & MUTUAL_DATA_NONE) | MUTUAL_DATA_RAWDATA; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;
        WP1005_PRINT(1,"wp1: CurDbgDataType=0x%02x\n",CurDbgDataType);
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3); 
        IsDataLogbyDriver = 1; // owen add  
        if(IsDataLogbyDriver == 1)//Jack 20131219 add to handle log file name
            wp1005_log_file_name_handler(CurDbgDataType);
        return 0;
    }
    //--------------------------------------------------------------------------------------------------
    static int wp1005_Mutualmode_Baseline(void)
    {
        uint8_t data[3] = {0x4,0x0,0x28};  
        Wp1004DebugMode = 1;
        CurDbgDataType = (CurDbgDataType & MUTUAL_DATA_NONE) | MUTUAL_DATA_BASELINE; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType; 
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3);  
        IsDataLogbyDriver = 1; // owen add
        if(IsDataLogbyDriver == 1)//Jack 20131219 add to handle log file name
            wp1005_log_file_name_handler(CurDbgDataType);
        return 0;
    }
    //--------------------------------------------------------------------------------------------------
    static int wp1005_Mutualmode_None(void)
    {
        uint8_t data[3] = {0x4,0x0,0xc9};    
        Wp1004DebugMode = 1;
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        CurDbgDataType = CurDbgDataType & MUTUAL_DATA_NONE; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3);
        if(pLogDataFile != NULL && IsDataLogbyDriver == 1)//close log file
            wp1005_log_file_close();
        return 0;
    } 
    //--------------------------------------------------------------------------------------------------
    static int wp1005_selfmode_RawData(void)
    {
        uint8_t data[3] = {0x4,0x0,0x49};   
        Wp1004DebugMode = 1;
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        WP1005_PRINT(1,"wp1: Before CurDbgDataType=0x%02x\n",CurDbgDataType);
        CurDbgDataType = (CurDbgDataType & SELF_DATA_NONE) | SELF_DATA_RAWDATA; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;
        WP1005_PRINT(1,"wp1: CurDbgDataType=0x%02x\n",CurDbgDataType);
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3);  
        if(IsDataLogbyDriver == 1)//Jack 20131219 add to handle log file name
            wp1005_log_file_name_handler(CurDbgDataType);
        return 0;
    }
    //--------------------------------------------------------------------------------------------------
    static int wp1005_selfmode_Baseline(void)
    {
        uint8_t data[3] = {0x4,0x0,0x89};
        Wp1004DebugMode = 1;
        WP1005_PRINT(1,"wp1: Before CurDbgDataType=0x%02x\n",CurDbgDataType);
        CurDbgDataType = (CurDbgDataType & SELF_DATA_NONE) | SELF_DATA_BASELINE; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;
        WP1005_PRINT(1,"wp1: CurDbgDataType=0x%02x\n",CurDbgDataType);
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3);
        if(IsDataLogbyDriver == 1)//Jack 20131219 add to handle log file name
            wp1005_log_file_name_handler(CurDbgDataType);
        return 0;
    }
    //--------------------------------------------------------------------------------------------------
    static int wp1005_selfmode_Delta(void)
    {
        uint8_t data[3] = {0x4,0x0,0xc9};
        Wp1004DebugMode = 1;
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        WP1005_PRINT(1,"wp1: Before CurDbgDataType=0x%02x\n",CurDbgDataType);
        CurDbgDataType = (CurDbgDataType & SELF_DATA_NONE) | SELF_DATA_DELTA; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;
        WP1005_PRINT(1,"wp1: CurDbgDataType=0x%02x\n",CurDbgDataType);
        wp1005_get_current_sensing_mode();
        wp1005_write_reg(i2c_client,data,3);
        if(IsDataLogbyDriver == 1)//Jack 20131219 add to handle log file name
            wp1005_log_file_name_handler(CurDbgDataType);
        return 0;
    }
    //--------------------------------------------------------------------------------------------------
    static int wp1005_selfmode_None(void)
    {
        uint8_t data[3] = {0x4,0x0,0xc9};
        Wp1004DebugMode = 1;    
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        CurDbgDataType = CurDbgDataType & SELF_DATA_NONE; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;
        wp1005_write_reg(i2c_client,data,3);
        if(pLogDataFile != NULL && IsDataLogbyDriver == 1)//close log file
            wp1005_log_file_close();
        return 0;
    } 
    //--------------------------------------------------------------------------------------------------
    static int wp1005_setmode_none(void)
    {
        uint8_t data[3] = {0x4,0x0,0x09};
        //int i;        
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        Wp1004DebugMode = 0;
        IsDataLogbyDriver = 1;  // Owen org: 0
        IsReportTouchDataToAPK = 0;
        IsReportKeyDataToAPK = 0;
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        CurDbgDataType = DATA_ALL_NONE; //Jack 20131204 add to record current data type
        data[2] = CurDbgDataType;    
        wp1005_get_current_sensing_mode();
        WP1005_PRINT(1,"wp1: %d:%s\n",__LINE__,__func__);
        wp1005_write_reg(i2c_client,data,3);
        if(pLogDataFile != NULL && IsDataLogbyDriver == 1)//close log file
            wp1005_log_file_close();
        CheckStateBuf[0] = 0;
        return 0;
    }
    //----------------------------------------------------------------------------------------
    //Jack 20131219 add
    void wp1005_log_file_close(void)
    {

        file_close(pLogDataFile);  
        pLogDataFile = NULL;  
        // set_fs(oldfs_winpower);    // Owen 
        IsDataLogbyDriver = 0;
        Clear_I2C_End(); //Jack 20131218 add to auto clear INT 
        WP1005_PRINT(1,"wp1: [wp1005_log_file_close]: Log File Close \n");
    }
    //----------------------------------------------------------------------------------------
    static void wp1005_log_file_name_handler(u8 DataType){

        char FileDateName[80] = {0}, FileTypeName[80] = {0};        
        struct timeval *tv;
        struct tm *t;
        tv = kmalloc(sizeof(struct timeval), GFP_KERNEL);
        t = kmalloc(sizeof(struct tm), GFP_KERNEL);

        do_gettimeofday(tv);
        time_to_tm(tv->tv_sec, 0, t);

        WP1005_PRINT(1,"wp1: Log File Name: %ld-%d-%d %d:%d:%d \n",
                t->tm_year + 1900,
                t->tm_mon + 1,
                t->tm_mday,
                (t->tm_hour + 8) % 24,
                t->tm_min,
                t->tm_sec);
        sprintf(FileDateName, "%s/%ld_%02d_%02d_%02d%02d%02d",LOG_DATA_FILE_PATH,
                (long int)(t->tm_year + 1900),
                (int)(t->tm_mon + 1),
                (int)t->tm_mday,
                (int)((t->tm_hour + 8) % 24),
                (int)t->tm_min,(int)t->tm_sec);    
        //Self
        if((DataType & SELF_DATA_DELTA) == SELF_DATA_DELTA)
            sprintf(FileTypeName, "SelfDelta");         
        else if((DataType & SELF_DATA_BASELINE)== SELF_DATA_BASELINE)
            sprintf(FileTypeName, "SelfBaseline");  
        else if((DataType & SELF_DATA_RAWDATA) == SELF_DATA_RAWDATA)
            sprintf(FileTypeName, "SelfRawData");                

        //Mutual
        if((DataType & MUTUAL_DATA_DELTA) == MUTUAL_DATA_DELTA)
            sprintf(FileTypeName, "%s_MutualDelta",FileTypeName);                     
        else if((DataType & MUTUAL_DATA_BASELINE) == MUTUAL_DATA_BASELINE)
            sprintf(FileTypeName, "%s_MutualBaseline",FileTypeName);  
        else if((DataType & MUTUAL_DATA_RAWDATA) == MUTUAL_DATA_RAWDATA)
            sprintf(FileTypeName, "%s_MutualRawData",FileTypeName);        

        sprintf(LogDataFileName, "%s_%s.txt",FileDateName, FileTypeName); 

        WP1005_PRINT(1,"wp1: [Data Log Func]: %s ",LogDataFileName);
        pLogDataFile = openFile_t(LogDataFileName, O_CREAT | O_WRONLY, 0);    
        if(pLogDataFile == NULL)
        {  
            printk("wp1: [Data Log Func]: %s, File open fail !!!",LogDataFileName);
            IsDataLogbyDriver = 0;
        }                    	            
    }

    //--------------------------------------------------------------------------------------------------
    static void wp1005_apk_cmd_analysis(unsigned long cmd)
    {
        int i, ret = 0;
        u8 RegAddr = 0x00;
        //uint16_t *noise_ptr = (uint16_t *)NoiseBuf;
        u8 ReadFNSLDataCmd[2] = {MSG_GET_FNSL,0x00};      

        switch (cmd) 
        {
            case APKDOSOMETHING :   // 0001 xxxx xxxx xxxx
                printk("APK DO SOMETHING \n");
                break;

            case SELFRAWDATA:     //8449, 0x2101
                wp1005_selfmode_RawData();    
                break;

            case SELFBASELINE:	  //8450, 0x2102
                wp1005_selfmode_Baseline();
                break;

            case SELFDELTA:		//8451, 0x2103
                wp1005_selfmode_Delta();
                break;

            case SELFNONE:		//8448, 0x2100
                wp1005_selfmode_None();
                break;

            case MUTUALRAWDATA:		//8705, 0x2201
                //printk("cmd=%x \n", cmd);
                wp1005_Mutualmode_RawData();
                break;

            case MUTUALBASELINE:		//8706, 0x2202
                wp1005_Mutualmode_Baseline();
                break;

            case MUTUALDELTA:		//8707, 0x2203
                wp1005_Mutualmode_Delta();
                break;

            case MUTUALNONE:		//8704, 0x2200
                wp1005_Mutualmode_None();
                break;

            case SETTOXDATA:		//	8965, 0x2305
                printk("set to xdata \n");
                break;

            case SETCCVALUE:		//	8964, 0x2304	
                //wp1005_write_cc_value();
                printk("set CC value \n ");
                break;

            case SETTONOISE:		//	8966, 0x2306	
                printk("SET TO NOISE TYPE \n");
                break;

            case SETTOMSG:		//	8967, 0x2307	
                printk("SET TO MSG TYPE \n");
                break;

            case DISABLE_DEBUG_MODE:		//	8972, 0x230C	
                wp1005_setmode_none();

                printk("Disable Debug Mode\n");
                break;

            case CLEAR_I2C:					//	8973, 0x230D	
                Clear_I2C_End();
                printk("== CLEAR I2C == \n");
                break;

            case IC_RESET:		//	8974, 0x230E	
                wp1005_ic_reset();	
                printk("!! RESET WP1005 !! \n");
                break;

            case READ_REGISTER:		//		
                wp1005_read_register(RegAddr, RegReadDataIdx);
                break;

            case READ_NOISE_DATA:		// 9990, 0x2706	
                ret = wp1005_i2c_wr(ReadFNSLDataCmd, 2, NoiseBuf, NOISE_LEVEL_DATA_LEN);

                /*		
                        for (i=0; i<(NOISE_LEVEL_DATA_LEN>>3); i++)
                        {
                        ReadFNSLDataCmd[1] = i;
                        ret = wp1005_i2c_wr(ReadFNSLDataCmd, 2, &NoiseBuf[i<<3], 8);                                                   
                        if (ret < 0){
                        printk("wp1: [%s]: Read FNSL data i2c_rxdata failed: %d, %d\n", __func__, ret, i);
                        break;
                        }		                       			
                        }
                        for(i=0;i<NOISE_LEVEL_DATA_LEN;i++)
                        {                                  
                 *noise_ptr = (*noise_ptr >> 8) | (*noise_ptr << 8);
                 NoisePtrBuf[i] = *noise_ptr;                                                                  
                 WP1005_PRINT(1,"wp1: 1. NoisePtr = 0x%04x\n",NoiseBuf[i]);                                                             
                 noise_ptr++;
                 } 	
                 */			
                break;

            case SET_ISREPORTTOUCHTOAPK:		//	9999, 0x270F
                IsReportTouchDataToAPK ^= 1;
                printk("Type: Touch Point Data. IsReportTouchDataToAPK = %d\n",IsReportTouchDataToAPK);//9999
                break;

            case SET_ISREPORTKEYTOAPK:		//	10000, 0x2710
                IsReportKeyDataToAPK ^= 1;
                printk("Type: Touch Key Data. IsReportKeyDataToAPK = %d\n",IsReportKeyDataToAPK);//10000
                break;

            case APKGETHEADER:	//13066, 0x330A
                WP1005_PRINT(1,"wp1: Type : Header !!!!!\n");//13066
	  Prot_Init();
	  msleep(1);
                get_header();
                break;

            case APK_LOGDATABYDRIVER:	//new:0x3311 13073 ;old:13067, 0x330B
                //printk("cmd=%x; before:%d \n", cmd, IsDataLogbyDriver);
                IsDataLogbyDriver ^= 1; //Jack add, since APK can not send 0 to driver so all value plus 90 by APK
                if((saveLogNow) && (IsDataLogbyDriver <= 0))//close log file
                {	
                    wp1005_log_file_close();
                    printk("----------- close finished ----\n");
                }
                printk("Type: Driver log data. IsDataLogbyDriver = %d \n",IsDataLogbyDriver); //13067
                break;

            case APK_FWUPDATE:    // 13576  0x3508
		printk("wp1: APK FW:  \n ");
		/*
		for(i = 0 ; i < BIN_LENGTH ;i++)
			printk("%c ",ch_buf[i]);
		printk("\n");
		*/
		//strcpy(wp1005_buffer,ch_buf);  // owen at
		//printk("wp1: Open %s file\n",wp1005_buffer); // owen at
		//strcpy(str_apk,ch_buf);
		//printk("wp1: str_apk=%s file\n",str_apk);
		mt_eint_mask(CUST_EINT_TOUCH_PANEL_NUM);
		printk("wp1:===  disable IRQ ==== \n");
		//UPDATENOSLEEP = 1;
		ret = fw_download_05(3); // 1= load bin from SDCARD, 2= load bin from inc, 3=apk_path
		memset(ch_buf, 0x00, sizeof(ch_buf));
		printk("wp1: ch_buf: %s END\n", ch_buf);		
		//UPDATENOSLEEP = 0;
		msleep(1);
		if (ret > 0)
		{
			Prot_Init();
			msleep(1);
			get_header();
			print_header();
			mt_eint_unmask(CUST_EINT_TOUCH_PANEL_NUM);
			printk("wp1:==== enable IRQ ==== \n");
                }
		else
			printk("wp1: DOWNLOAD FW FAIL \n");
                break;


            default:
                printk("WRONG COMMAND: %ld = %lx \n", cmd, cmd);
                break;
        }

    }
    // ---------------------------------------------------------------------------------------------------------------------------
    static int wp1005_read_raw_data()
    {
        int 		ret = 0;
        u8         j = 0;          
        u8         ReadSelfDataCmd[2]      = {I2C_READ_RAW_SELF,           0x00};
        u8         ReadMutualDataCmd[2]    = {I2C_READ_RAW_MUTUAL,         0x00};
        u8         ReadStatusCmd[2]        = {MSG_GET_STATUS,              0x00};

        self_data_log_len = 0;
        mutual_data_log_len = 0;
        /*  //  OWen  marked
            WP1005_PRINT(1, "wp1: +++ wp1005_read_raw_data() +++ \n"); 
            wp1005_i2c_wr(ReadStatusCmd, 2, &ReadBack[0], MSG_GET_STATUS_LEN);  // Owen
        //i2c_smbus_read_i2c_block_data(i2c_client,MSG_GET_STATUS,MSG_GET_STATUS_LEN,ReadBack);
        if((ReadBack[1] & 0x0C) == 0x04)
        CurSensingMode = SELF_TEST_MODE;
        else if((ReadBack[1] & 0x0C) == 0x08)
        CurSensingMode = MUTUAL_MODE;  
         */ //  OWen  marked END
        WP1005_PRINT(1, "wp1: CurSensingMode=0x%x in isr\n", CurSensingMode);  
        WP1005_PRINT(1, "wp1: =====================================================\n");
        //--------- Self Debug Data ---------------//  						
        if((CurDbgDataType & 0xC0) != 0 && (CurDbgDataType & 0x0D) == 0x09)
        {
            WP1005_PRINT(1, "wp1:Self_CH_No=%d\n",Self_CH_No);

            self_data_log_len = Self_CH_No * 2;                         
            for (j=0; j<((self_data_log_len>>3)+1); j++) 
            {
                //ReadSelfDataCmd[1] = j;
                if (j>0)
                    ReadSelfDataCmd[1] = 1;
                ret = wp1005_i2c_wr(ReadSelfDataCmd, 2, &RawBuf[j<<3], ((j+1)<<3)< self_data_log_len ? 8 : (self_data_log_len - (j<<3)));
                WP1005_PRINT(1,"wp1::j=%d,self_len=%d,0x%4x\n",j,self_data_log_len,RawBuf[j]);      
                if (ret < 0) 
                {
                    printk("wp1: %s read_data i2c_rxdata failed: %d, %d\n", __func__, ret, j);
                    return ret;
                }																
            }	
            /*
               for (j=0; j<self_data_log_len; j++)
               {
               if(j==0)
               printk("wp1: ");
               if ((j!=0) && (j%16==0))
               printk("\nwp1");
               printk("%02X ", RawBuf[j]);

               }
               printk("\n wp1:=========%d========== \n", self_data_log_len);
             */
            swap2(self_data_log_len); 
        }




        //------- Mutual Debug Data --------//
        if((CurDbgDataType & 0x30) != 0 && (CurDbgDataType & 0x08) == 0x08)
        {
            WP1005_PRINT(1, "wp1:Mutual_Node_No=%d\n",Mutual_Node_No);
            mutual_data_log_len = Mutual_Node_No * 2;
            for (j=0; j<(Mutual_Node_No>>2); j++)
            {
                if (j>0)
                    ReadMutualDataCmd[1] = 1;
                ret = wp1005_i2c_wr(ReadMutualDataCmd, 2, &RawBuf[(j << 3) + self_data_log_len], 8);
                WP1005_PRINT(1,"wp1::j=%d, pos=%d, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x, 0x%02x\n",
                        j, (j << 3) + self_data_log_len,
                        RawBuf[(j << 3) + self_data_log_len], RawBuf[(j << 3) + self_data_log_len+1],
                        RawBuf[(j << 3) + self_data_log_len+2], RawBuf[(j << 3) + self_data_log_len+3], 
                        RawBuf[(j << 3) + self_data_log_len+4], RawBuf[(j << 3) + self_data_log_len+5], 
                        RawBuf[(j << 3) + self_data_log_len+6], RawBuf[(j << 3) + self_data_log_len+7]);
                if (ret < 0)
                {
                    printk("wp1: %s read_data i2c_rxdata failed: %d, %d\n", __func__, ret, j);
                    return ret;
                }														
            }
            swap2(mutual_data_log_len);
        }     
        WP1005_PRINT(1, "wp1: %d ==============================================\n", IsDataLogbyDriver);  
        WP1005_PRINT(1, "wp1: before checkstate=%d,j=%d\n",j,CheckStateBuf[0]);  					 
        CheckStateBuf[0] = 1;                     
        WP1005_PRINT(1, "wp1: after checkstate=%d,j=%d\n",j,CheckStateBuf[0]); 
        //Driver log function 
        IsDataLogbyDriver = 1;
        if(IsDataLogbyDriver == 1 && CurDbgDataType != DATA_ALL_NONE && pLogDataFile != NULL)
        {                          
            WriteFile(pLogDataFile, RawBuf, self_data_log_len + mutual_data_log_len);  
            //Clear_I2C_End(); //Jack 20131218 add to auto clear INT    // Owen closed
            WP1005_PRINT(1, "wp1: Driver data log len = %d, self_data_log_len = %d, mutual_data_log_len = %d\n",
                    self_data_log_len + mutual_data_log_len,self_data_log_len,mutual_data_log_len);
        } 
        return 1;
    }
    // --------------------------------------------------------------------------------------------------------------------------
    static void swap2(int len)
    {
        int i = 0, j = 0;
        u8 tmp[2] = {0, 0};
        //printk("Before Raw[0]=%x Raw[1]=%x Raw[10]=%x Raw[11]=%x \n", RawBuf[0], RawBuf[1], RawBuf[10], RawBuf[11]);
        for (i=0; i<len; i+=2)
        {
            for (j=0; j<2; j++)
            {
                tmp[j] = RawBuf[i+j];	
            }
            RawBuf[i] = tmp[1];			
            RawBuf[i+1] = tmp[0];		
        }
        //printk("After Raw[0]=%x Raw[1]=%x Raw[10]=%x Raw[11]=%x \n", RawBuf[0], RawBuf[1], RawBuf[10], RawBuf[11]);
        /*
           for (i=0; i<62; i++)
           {
           if (i==0)
           printk("wp1: ");
           if (i!=0 && i%16==0)
           printk("\nwp1: ");
           printk("%02X ", RawBuf[i]);
           }
           printk("\n================================\n");
         */
    }
    // -----------------------------------------------------------------------------------------------
static void wp1005_apk_file_analysis(int cmd,int pos)
{
    WP1005_PRINT(1, "wp1: num=%d\n",cmd);
    ch_buf[pos] = cmd;//ch_ch;
    WP1005_PRINT(1, "wp1: ch_buf_pos=%c\n",ch_buf[pos]);
    
}
// -------------------------------------------------------------------------------------------------------------
#endif

