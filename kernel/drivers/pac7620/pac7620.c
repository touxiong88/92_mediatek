/********************************************************
2014-10-30	upward compatible new version sensor , sunhouzan
*********************************************************/
#include <linux/module.h>
#include <linux/init.h>
#include <linux/i2c.h>
#include <linux/interrupt.h>
#include <linux/platform_device.h>
#include <linux/input.h>
#include <linux/wakelock.h>
#include <linux/interrupt.h>
#include <linux/workqueue.h>
#include <linux/slab.h>
#include <mach/mt_gpio.h>
#include <linux/gpio.h>
#include "pac7620.h"
#include <linux/mmprofile.h>
#include <linux/device.h>

#include <linux/proc_fs.h>
#include <asm/uaccess.h>
#include <linux/string.h>

#include <mach/mt_pm_ldo.h>
#include <mach/mt_typedefs.h>
#include <mach/mt_boot.h>

#include <cust_eint.h>
#include <linux/jiffies.h>
#include <linux/miscdevice.h>

#include <linux/delay.h>

#include <mach/irqs.h>
#include <mach/eint.h>
#include <cust_eint.h>
#include <linux/hwmsensor.h>
#include <linux/hwmsen_dev.h>
#include <linux/sensors_io.h>
#include <cust_alsps.h>
#include <linux/kthread.h>

//#include <tmg399x.h>
#include <linux/earlysuspend.h>

//i2c-2   ALPS_EINT:GPIO1

#define CURSOR_OPEN  0x6810
#define CURSOR_CLOSE  0x6811
#define CURSOR_SET  0x6812
#define CURSOR_GET  0x6813
#define MOUSE_ORIGN_GET  0x6818//add for factory test

#define CURSOR_MODE  0x6814	//default cursor mode
#define GESTURE_MODE  0x6815
#define GESTURE_GET  0x6816
#define GESTURE_SET  0x6817 


extern int backlight_on;

struct cursors
{
	int x,y;
	int press,cmd;
};
typedef struct cursors CURSOR;
CURSOR coord={0};


typedef struct {
	struct i2c_client	*client;
	struct input_dev *cursor_input_dev;
	int irq;
	bank_e bank;
} pac7620_data_t;



enum {
	// REGISTER 0
	GESTURE_RESERVED,
	GESTURE_RIGHT,
	GESTURE_LEFT,
	GESTURE_UP,	
	GESTURE_DOWN,		
	GESTURE_FORWARD,		
	GESTURE_BACKWARD,	
	GESTURE_CLOCKWISE,		 
	GESTURE_COUNT_CLOCKWISE, 
	//REGISTER 1
	GESTURE_WAVE,	
};
enum {
	GESTURE_MODE_ENUM,	//0
	CURSOR_MODE_ENUM,	//1
};
enum {
	THREAD_STOP,	//0
	THREAD_RUN,	//1
};
enum {
	SENSOR_CLOSE,	//0
	SENSOR_OPEN,	//1
};
enum {
	LIGHT_OFF,	//0
	LIGHT_ON,	//1
};
#ifdef CONFIG_HAS_EARLYSUSPEND
	struct early_suspend pac_early_suspend;
#endif	


struct i2c_adapter 	pac_adap,*pac_adap_p;
struct i2c_client 	pac_client,*pac_client_p;

static  pac7620_data_t pac7620data;

static struct task_struct *thread = NULL;

//int thread_pause = 1;     //线程运行停止flag--------------------------------------------

int thread_flag = THREAD_STOP;
int sensor_state = SENSOR_CLOSE;
int thread_wait =THREAD_STOP;
static DECLARE_WAIT_QUEUE_HEAD(waiter);
static unsigned int pac7620_mode = CURSOR_MODE_ENUM;//0 :gesture mode
static unsigned long gesture_state = GESTURE_RESERVED;

#define PAC7620_DEBUG
#ifdef PAC7620_DEBUG
#define dbmsg(fmt, args ...) printk(KERN_NOTICE "pac7620: [%d]: "fmt"\n", __LINE__,##args)
#else
#define dbmsg(fmt, args ...)  
#endif

static int pac7620_bank_select(bank_e bank);
static int pac7620_i2c_read(u8 reg, u8 *data);
static int pac7620_i2c_write(u8 reg, u8 *data, int len);
static int pac_suspend(void);
static int pac_resume(void);
static int pac7620_i2c_write(u8 reg, u8 *data, int len)
{
	u8  buf[20];
	int rc;
	int ret = 0;
	int i;

	buf[0] = reg;
	if (len >= 20) {
		dbmsg("%s (%d) : FAILED: buffer size is limitted(20) %d\n", __func__, __LINE__, len);
		dev_err(&pac7620data.client->dev, "pac7620_i2c_write FAILED: buffer size is limitted(20)\n");
		return -1;
	}

	for( i=0 ; i<len; i++ ) {
		buf[i+1] = data[i];
	}
 
	rc = i2c_master_send(pac7620data.client, buf, len+1);

	if (rc != len+1) {
		dbmsg("%s (%d) : FAILED: writing to reg 0x%x\n", __func__, __LINE__, reg);

		ret = -1;
	}

	return ret;
}


static int pac7620_i2c_read(u8 reg, u8 *data)
{
	u8  buf[20];
	int rc;		
	
	buf[0] = reg;
	
	rc = i2c_master_send(pac7620data.client, buf, 1);
	if (rc != 1) {
		dbmsg("%s (%d) : FAILED: writing to address 0x%x\n", __func__, __LINE__, reg);
		return -1;
	}	
	
	rc = i2c_master_recv(pac7620data.client, buf, 1);
	if (rc != 1) {
		dbmsg("%s (%d) : FAILED: reading data\n", __func__, __LINE__);
		return -1;
	}	
		
	*data = buf[0] ;		
	return 0;
}

static int pac7620_set_reg(u8 reg, u8 data)
{
	int ret = pac7620_i2c_write(reg, &data, 1);
	
	//dbmsg("%s (%d) : set register , addr = 0x%x, data = 0x%x \n", __func__, __LINE__, reg, data);


	return  ret;
}

static int pac7620_bank_select(bank_e bank)
{
	switch(bank){
		case BANK0:
			pac7620_set_reg(PAC7620_REGITER_BANK_SEL, PAC7620_BANK0);
			break;
		case BANK1:
			pac7620_set_reg(PAC7620_REGITER_BANK_SEL, PAC7620_BANK1);
			break;
		default:
			break;
	}
	
	pac7620data.bank = bank;
	
	return 0;
}


/*
static int pac7620_interrupt_mask(mode_e mode)
{	
	dbmsg("%s (%d) : pac7620 interrupt mask : 0x%x.\n", __func__, __LINE__, mode);

	pac7620_bank_select(BANK0);
	
	if(mode == IRMOTION_ENABLED){
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_0,0xFF);
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_1,0x01);		
	}else if(mode == PROXIMITY_ENABLED){
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_0,0x00);
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_1,0x02);
	}else if(mode == ALL_ENABLED){
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_0,0xFF);
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_1,0x03);
	}else if(mode == ALL_DISABLE){
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_0,0x00);
		pac7620_set_reg(PAC7620_ADDR_GES_PS_DET_MASK_1,0x00);
	}

	pac7620_register_debug();

	return 0;
}
*/

static long _read_addr = 0x01 ;
static ssize_t write_reg_store(struct device *dev, struct device_attribute *attr, const char *buf, size_t size)
{
	char s[256];  
	char *p = s ;
	dbmsg("%s (%d) : write register\n", __func__, __LINE__);

	memcpy(s, buf, size);
	
	*(s+1)='\0';
	*(s+4)='\0';
	*(s+7)='\0';
	
	if(*p == 'w')
	{
		long write_addr, write_data ;

		p += 2;
		if(!kstrtol(p, 16, &write_addr))
		{
			p += 3 ;
			if(!kstrtol(p, 16, &write_data))
			{		
				dbmsg("w 0x%x 0x%x\n", (unsigned int)write_addr, (unsigned int)write_data);
				pac7620_set_reg( (u8)write_addr, (u8)write_data);
			}
		}
	}
	else if(*p == 'r')
	{
		p+=2;
		
		if(!kstrtol(p, 16, &_read_addr))
		{
			u8 data = 0;
			if(!pac7620_i2c_read((u8)_read_addr, &data))
			{
				dbmsg("r 0x%x 0x%x\n", (unsigned int)_read_addr, data);
			}
		}
	}
	return size;
}

static ssize_t read_reg_show(struct device *dev, struct device_attribute *attr, char *buf)
{
	int ret ;
 	char *s= buf;  
 	u8 data = 0;

	dbmsg("%s (%d) : read register\n", __func__, __LINE__);
	ret = pac7620_i2c_read(_read_addr, &data);
	
	if(ret)
		s += sprintf(s,"Error\n");  
	else
  	s += sprintf(s,"Addr 0x%x, Data 0x%x\n",(unsigned int)_read_addr, data);  
	
	return (s - buf);
}

static DEVICE_ATTR(rw_reg, S_IRUGO | S_IWUSR | S_IWGRP, read_reg_show, write_reg_store);

static struct attribute *rw_reg_sysfs_attrs[] = {
	&dev_attr_rw_reg.attr,
	NULL
};

static struct attribute_group rw_reg_attribute_group = {
	.attrs = rw_reg_sysfs_attrs,
};


const unsigned char change_ub_register_array[][2] = {
  {0xEF,0x00},
//{0x48,/*0x3c*/R_AE_Exposire_UB*0.5},   //modified byl   orginal 0x3c  R_AE_Exposire_UB    0x78
//{0x4A,/*0x18*/R_AE_Exposire_LB*0.5},   //modified byl   orginal 0x1e  R_AE_Exposire_LB    0x3c
  {0x48,(unsigned char)((u16)(R_AE_Exposure_UB*0.5) & 0x00FF)},	   //Boy modify @ 2014_1028
  {0x49,(unsigned char)((u16)(R_AE_Exposure_UB*0.5) & 0xFF00)>>8}, //Boy modify @ 2014_1028
  {0x4a,(unsigned char)((u16)(R_AE_Exposure_LB*0.5) & 0x00FF)},	   //Boy modify @ 2014_1028
  {0x4b,(unsigned char)((u16)(R_AE_Exposure_LB*0.5) & 0xFF00)>>8}, //Boy modify @ 2014_1028
};


//Boy modify @ 2014_1031
const unsigned char change_ub_register_array_for_gesture_mode[][2] = {
  {0xEF,0x00},
  {0x48,(unsigned char)((u16)(R_AE_Exposure_UB_for_gesture_mode*0.5) & 0x00FF)},    //Boy modify @ 2014_1031
  {0x49,(unsigned char)((u16)(R_AE_Exposure_UB_for_gesture_mode*0.5) & 0xFF00)>>8}, //Boy modify @ 2014_1031
  {0x4a,(unsigned char)((u16)(R_AE_Exposure_LB_for_gesture_mode*0.5) & 0x00FF)},    //Boy modify @ 2014_1031
  {0x4b,(unsigned char)((u16)(R_AE_Exposure_LB_for_gesture_mode*0.5) & 0xFF00)>>8}, //Boy modify @ 2014_1031
};
//Boy modify @ 2014_1031




static int pac7620_init_reg(void)
{
	//Near_normal_mode_V5_6.15mm_121017 for 940nm
	int i=0;
	u8 data0 = 0, data1 = 0;
	int ret = 0 ;
	
	ret = pac7620_bank_select(BANK0);	//wakeup
	if( ret )
	{
		return -1 ;
	}
	ret = pac7620_bank_select(BANK0);
	if( ret )
	{
		return -2 ;
	}
	ret = pac7620_i2c_read(0, &data0);
	if( ret )
	{
		return -3 ;
	}
	
	ret = pac7620_i2c_read(1, &data1);
	if( ret )
	{
		return -4 ;
	}
	
	dbmsg("%s (%d) : ADDR0 = 0x%x, ADDR1 = 0x%x.\n", __func__, __LINE__, data0, data1);
	if( (data0 != 0x20 ) || (data1 != 0x76) )
	{
		return -5 ;
	}
	if(CURSOR_MODE_ENUM == pac7620_mode)
	{
		for(i = 0; i < INIT_CURSOR_SIZE;i++){
			pac7620_set_reg(init_cursor_array[i][0],init_cursor_array[i][1]);
		}

		//if((ReadSensorReg_PXT(0x7A) & 0x80) == 0x00)//None EPI
		ret = pac7620_i2c_read(0x7A, &data0);
		if( ret )
		{
			return -6 ;
		}
		if((data0 & 0x80) == 0x00)//None EPI	
		{
			dbmsg("%s (%d) : ADDR0 = 0x%x, ADDR1 = 0x%x.\n", __func__, __LINE__, data0, data1);
			for(i = 0; i < 5;i++){ //modify byl 20141030
			pac7620_set_reg(change_ub_register_array[i][0],change_ub_register_array[i][1]);
			}
		}
	}
	else//gesture mode 
	{
		for(i = 0; i < INIT_GESTURE_SIZE;i++){
			pac7620_set_reg(init_gesture_array[i][0],init_gesture_array[i][1]);
		}
		//if((ReadSensorReg_PXT(0x7A) & 0x80) == 0x00)//None EPI
		ret = pac7620_i2c_read(0x7A, &data0);
		if( ret )
		{
			return -7 ;
		}
		if((data0 & 0x80) == 0x00)//None EPI	
		{
			dbmsg("%s (%d) : ADDR0 = 0x%x, ADDR1 = 0x%x.\n", __func__, __LINE__, data0, data1);
			for(i = 0; i < 5;i++){ //modify byl 20141030
			pac7620_set_reg(change_ub_register_array_for_gesture_mode[i][0],change_ub_register_array_for_gesture_mode[i][1]); //Boy modify @ 2014_1031
			}
	    }
	}
	
	dbmsg("ret=%d (%d) : pac7620 initialize register.\n",ret, __LINE__);
	
	return 0;
}

static int pac7620_init_cursor_data(void)
{
	int ret = 0;

	dbmsg("%s (%d) : initialize data\n", __func__, __LINE__);
	
	pac7620data.cursor_input_dev = input_allocate_device();
	
	if (!pac7620data.cursor_input_dev) {
		dbmsg("%s (%d) : could not allocate cursor input device\n", __func__, __LINE__);
		return -ENOMEM;
	}

	input_set_drvdata(pac7620data.cursor_input_dev, &pac7620data);
	
	pac7620data.cursor_input_dev->name = "gesture_cursor_sensor";

		/*********** register input device	**************/
			set_bit(EV_ABS, pac7620data.cursor_input_dev->evbit);
			set_bit(EV_KEY, pac7620data.cursor_input_dev->evbit);
			set_bit(ABS_X, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_Y, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_PRESSURE, pac7620data.cursor_input_dev->absbit);
			set_bit(BTN_TOUCH, pac7620data.cursor_input_dev->keybit);
			set_bit(INPUT_PROP_DIRECT, pac7620data.cursor_input_dev->propbit);
		
			set_bit(ABS_DISTANCE, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_MT_TRACKING_ID, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_MT_TOUCH_MAJOR, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_MT_TOUCH_MINOR, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_MT_POSITION_X, pac7620data.cursor_input_dev->absbit);
			set_bit(ABS_MT_POSITION_Y, pac7620data.cursor_input_dev->absbit);
		
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_DISTANCE, 0, 1, 0, 0);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_MT_POSITION_X, 0, 1080, 0, 0);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_MT_POSITION_Y, 0, 1920, 0, 0);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_MT_TOUCH_MAJOR, 0, 100, 0, 0);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_MT_TOUCH_MINOR, 0, 100, 0, 0);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_X, 0, 1080, 0, 0);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_Y, 0, 1920, 0, 0);
			input_abs_set_res(pac7620data.cursor_input_dev, ABS_X, 1080);
			input_abs_set_res(pac7620data.cursor_input_dev, ABS_Y, 1920);
			input_set_abs_params(pac7620data.cursor_input_dev, ABS_PRESSURE, 0, 255, 0, 0);

	ret = input_register_device(pac7620data.cursor_input_dev);
	if (ret < 0) {
		input_free_device(pac7620data.cursor_input_dev);
		dbmsg("%s (%d) : could not register input device\n", __func__, __LINE__);	
		return ret;
	}
	
	ret = sysfs_create_group(&pac7620data.cursor_input_dev->dev.kobj, &rw_reg_attribute_group);	
	if (ret) {
		dbmsg("%s (%d) : could not create sysfs group\n", __func__, __LINE__);
		return 0;
	}	
	return 0;	
}

#if 1
static void cursor_down( int  x,  int  y,u8 press)
{
	/* Report relative coordinates via the event interface */
    input_report_abs(pac7620data.cursor_input_dev, ABS_MT_TOUCH_MAJOR, 1);//touch area size 

	input_report_key(pac7620data.cursor_input_dev, BTN_TOUCH, 1);
    input_report_abs(pac7620data.cursor_input_dev, ABS_MT_POSITION_X, x);
    input_report_abs(pac7620data.cursor_input_dev, ABS_MT_POSITION_Y, y);
    input_mt_sync(pac7620data.cursor_input_dev);
	input_sync(pac7620data.cursor_input_dev);

	dbmsg("x = %d,y = %d \n",x,y);

}
static void cursor_up(int x, int y, s32 id)
{
	//input_report_abs(pac7620data.cursor_input_dev, ABS_MT_PRESSURE, 100);
    input_report_abs(pac7620data.cursor_input_dev, ABS_MT_TOUCH_MAJOR, 1);//touch area size 

	input_report_key(pac7620data.cursor_input_dev, BTN_TOUCH, 0);
    input_report_abs(pac7620data.cursor_input_dev, ABS_MT_POSITION_X, x);
    input_report_abs(pac7620data.cursor_input_dev, ABS_MT_POSITION_Y, y);
    input_mt_sync(pac7620data.cursor_input_dev);
	input_sync(pac7620data.cursor_input_dev);

	dbmsg("x = %d,y = %d \n",x,y);

	
}
#endif

//#define PAC7620_INT_GIO GPIO_ALS_EINT_PIN
#if 0
irqreturn_t pac7620_irq_thread_fn(int irq, void *data)
{
	int ret,press = 0;
	u8	size_h,size_l,cursor_xl,cursor_yx,cursor_yl;
	int size,temp_x,temp_y,cursor_x,cursor_y;
	unsigned int gesture_state = KEY_RESERVED;

#if 1	
	ret = gpio_get_value(PAC7620_INT_GIO);
	dbmsg("%s (%d) : gpio value = %d\n", __func__, __LINE__, ret);
	
	pac7620_bank_select(BANK0);
	ret = pac7620_i2c_read(PAC7620_ADDR_SIZE_H, &size_h);
	if(!ret)
		dbmsg("%s (%d) : interrupt size_h = 0x%x\n", __func__, __LINE__, size_h);
	else
		goto rtn;				
		
	ret = pac7620_i2c_read(PAC7620_ADDR_SIZE_L, &size_l);
	if(!ret)
		dbmsg("%s (%d) : interrupt size_l = 0x%x\n", __func__, __LINE__, size_l);
	else
		goto rtn;				

	size = size_h << 8 | size_l;
	if(size <= 0)
		goto rtn;	

	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_XL, &cursor_xl);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_xl = 0x%x\n", __func__, __LINE__, cursor_xl);
	else
		goto rtn;				
		
	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_YX, &cursor_yx);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_yx = 0x%x\n", __func__, __LINE__, cursor_yx);
	else
		goto rtn;	
	
	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_YL, &cursor_yl);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_yl = 0x%x\n", __func__, __LINE__, cursor_yl);
	else
		goto rtn;	

	temp_x = ((cursor_yx>>4)|0x0F)<<8|cursor_yl; //MAX3327
	temp_y = ((cursor_yx)|0x0F)<<8|cursor_xl;	 //MAX3327

	cursor_x = temp_x;
	cursor_y = temp_y;
	dbmsg("%s: interrupt temp_x = 0x%x temp_y = 0x%x cursor_x = 0x%x cursor_y = 0x%x\n", __func__, temp_x,temp_y,cursor_x,cursor_y);
	if((cursor_x>1080)||(cursor_x<0))cursor_x = 0;
	if((cursor_y>1920)||(cursor_y<0))cursor_y = 0;
	
#endif	
	if(press)
		cursor_down(temp_x,temp_y,1);
	else
		cursor_up(temp_x,temp_y,0);

rtn:
	dbmsg("%s (%d) \n", __func__, __LINE__);
	return IRQ_HANDLED;
}
#endif

#define SCALE 5771 //(1920/3327)*10000
static int pac7620_cursor_get(void)
{
	int ret;
	u8	size_h,size_l;
	unsigned int size;
	u8	cursor_xl,cursor_yx,cursor_yl;
	unsigned short int temp_x,temp_y;
	unsigned int cursor_x,cursor_y;
	unsigned int scale_temp,read_success = 0;

	pac7620_bank_select(BANK0);
	ret = pac7620_i2c_read(PAC7620_ADDR_SIZE_H, &size_h);
	if(!ret)
		dbmsg("%s (%d) : interrupt size_h = 0x%x\n", __func__, __LINE__, size_h);
	else
		{
			//cursor_up(0,0,0);
			goto no_cursor_data;						
		}				
		
	ret = pac7620_i2c_read(PAC7620_ADDR_SIZE_L, &size_l);
	if(!ret)
		dbmsg("%s (%d) : interrupt size_l = 0x%x\n", __func__, __LINE__, size_l);
	else
		{
			//cursor_up(0,0,0);
			goto no_cursor_data;						
		}			

	size = size_h << 8 | size_l;
	if(size <= 0)
		{
			//cursor_up(0,0,0);
			goto no_cursor_data;						
		}

	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_XL, &cursor_xl);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_xl = 0x%x\n", __func__, __LINE__, cursor_xl);
	else
		{
			//cursor_up(0,0,0);
			goto no_cursor_data;						
		}			
		
	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_YX, &cursor_yx);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_yx = 0x%x\n", __func__, __LINE__, cursor_yx);
	else
		{
			//cursor_up(0,0,0);
			goto no_cursor_data;						
		}	
	
	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_YL, &cursor_yl);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_yl = 0x%x\n", __func__, __LINE__, cursor_yl);
	else
		{
			//cursor_up(0,0,0);
			goto no_cursor_data;						
		}	
					
	temp_x = cursor_yx & 0xf0;
	temp_x = (temp_x <<4)|cursor_yl;//MAX3327

	temp_y = cursor_yx & 0x0f;
	temp_y = (temp_y << 8) | cursor_xl;//MAX3327
	//cut minor axis as android driver x position
	/****************************************************
		  727                 727
		_________________________ (3327)
		|	|				|   |
		|	|				|   |
		|	|				|   |
		|	|	effect		|   | 
		|	|	 area		|   |
		|	|				|   |(3327 * SCALE) = 1920
		|	|				|   |
		|	|				|   |	
		|	|				|   |
		|	|				|   |
		------------------------- (3327)	
		(3327-727-727)*SCALE = 1080
	*****************************************************/
	
	
	if((temp_x > (3327-727))||(temp_x < 727))
		{			
			dbmsg("%s (%d) : interrupt outsize", __func__, __LINE__);
			//goto no_cursor_data;
		}
	
	temp_x = temp_x - 727;
	scale_temp = temp_x*SCALE;
	cursor_x = (int)scale_temp/10000;//mirror x
	
	scale_temp = temp_y*SCALE;
	cursor_y = (int)scale_temp/10000;

	coord.x = cursor_x;
	coord.y = cursor_y;

	read_success =1;
	
	dbmsg("%s (%d) : interrupt cursor_x = %d cursor_y = %d\n", __func__, __LINE__, coord.x,coord.y);
	no_cursor_data:
	if(0 == read_success)
	{
		coord.x = -1;
		coord.y = -1;

		dbmsg("%s (%d) : interrupt cursor get fail", __func__, __LINE__);
	}
	return ret;
}

static int factory_pac7620_cursor_get(void)
{
	int ret;
	u8	size_h,size_l;
	unsigned int size,read_success = 0;
	u8	cursor_xl,cursor_yx,cursor_yl;
	unsigned short int temp_x,temp_y;

	pac7620_bank_select(BANK0);
	ret = pac7620_i2c_read(PAC7620_ADDR_SIZE_H, &size_h);
	if(!ret)
		dbmsg("%s (%d) : interrupt size_h = 0x%x\n", __func__, __LINE__, size_h);
	else
		{
			//cursor_up(0,0,0);
			goto factory_no_cursor_data;						
		}				
		
	ret = pac7620_i2c_read(PAC7620_ADDR_SIZE_L, &size_l);
	if(!ret)
		dbmsg("%s (%d) : interrupt size_l = 0x%x\n", __func__, __LINE__, size_l);
	else
		{
			//cursor_up(0,0,0);
			goto factory_no_cursor_data;						
		}			

	size = size_h << 8 | size_l;
	if(size <= 0)
		{
			//cursor_up(0,0,0);
			goto factory_no_cursor_data;						
		}

	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_XL, &cursor_xl);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_xl = 0x%x\n", __func__, __LINE__, cursor_xl);
	else
		{
			//cursor_up(0,0,0);
			goto factory_no_cursor_data;						
		}			
		
	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_YX, &cursor_yx);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_yx = 0x%x\n", __func__, __LINE__, cursor_yx);
	else
		{
			//cursor_up(0,0,0);
			goto factory_no_cursor_data;						
		}	
	
	ret = pac7620_i2c_read(PAC7620_ADDR_CURSOR_YL, &cursor_yl);
	if(!ret)
		dbmsg("%s (%d) : interrupt cursor_yl = 0x%x\n", __func__, __LINE__, cursor_yl);
	else
		{
			//cursor_up(0,0,0);
			goto factory_no_cursor_data;						
		}	
					
	temp_x = cursor_yx & 0xf0;
	temp_x = (temp_x <<4)|cursor_yl;//MAX3327

	temp_y = cursor_yx & 0x0f;
	temp_y = (temp_y << 8) | cursor_xl;//MAX3327


	coord.x = 3327 - temp_x;
	coord.y = temp_y;

	dbmsg("%s (%d) : interrupt coord.x = %d coord.y  = %d\n", __func__, __LINE__, coord.x ,coord.y);
	
	read_success = 1;
	
	factory_no_cursor_data:
	if(0 == read_success)
	{
		coord.x = -1;
		coord.y = -1;

		dbmsg("%s (%d) : interrupt cursor get fail", __func__, __LINE__);
	}

	return ret;
}


void gesture_get(void)
{	
	int ret;
	
	unsigned char int_flag1,int_flag2;



			pac7620_bank_select(BANK0);
			ret = pac7620_i2c_read(PAC7620_ADDR_GES_PS_DET_FLAG_0, &int_flag1);
			if(!ret)
				dbmsg("%s (%d) : interrupt flag1 = 0x%x\n", __func__, __LINE__, int_flag1);
			else
		goto get_gesture_err;				
				
			ret = pac7620_i2c_read(PAC7620_ADDR_GES_PS_DET_FLAG_1, &int_flag2);
			if(!ret)
				dbmsg("%s (%d) : interrupt flag2 = 0x%x\n", __func__, __LINE__, int_flag2);
			else
		goto get_gesture_err;				


			switch(int_flag1)
			{
				case 	GES_RIGHT_FLAG:
					gesture_state = GESTURE_RIGHT ;
					break;
				case 	GES_LEFT_FLAG:
					gesture_state = GESTURE_LEFT ;
					break;
				case 	GES_UP_FLAG:
					gesture_state = GESTURE_UP ;
					break;
				case 	GES_DOWN_FLAG:
					gesture_state = GESTURE_DOWN ;
					break;
				case 	GES_FORWARD_FLAG:
					gesture_state = GESTURE_FORWARD ;
					break;
				case 	GES_BACKWARD_FLAG:
					gesture_state = GESTURE_BACKWARD ;
					break;
				case 	GES_CLOCKWISE_FLAG:
					gesture_state = GESTURE_CLOCKWISE ;
					break;
				case 	GES_COUNT_CLOCKWISE_FLAG:
					gesture_state = GESTURE_COUNT_CLOCKWISE ;
					break;
				default:
					//gesture_state = GESTURE_RESERVED ;
					break;
			}
			if(GES_WAVE_FLAG == int_flag2)
			{				
					gesture_state = GESTURE_WAVE ;
			}
			
			dbmsg(" : interrupt gesture_state = 0x%lx\n",gesture_state);		
			msleep(50);
			
	get_gesture_err:
	msleep(1);
			

		}	
static int gesture_get_sched(void *unused)
{ 

    struct sched_param param = { .sched_priority = RTPM_PRIO_TPD };
    sched_setscheduler(current, SCHED_RR, &param);

	
	wake_up_interruptible(&waiter);//线程唤醒满足条件 在 1)condition为真的前提下，2) 调用wake_up()。 

    do
    {
        //set_current_state(TASK_INTERRUPTIBLE); 
		//gesture_ctrl();
		//msleep(500);
        wait_event_interruptible(waiter,thread_flag != THREAD_STOP);

        set_current_state(TASK_RUNNING);

		gesture_get();
       //if(thread_flag==THREAD_STOP)do_exit();
        
    }while(!kthread_should_stop());

    return 0;
}

#if 0
static int pac7620_init_interrupt(void)
{

	int result;
		
	result = gpio_request(PAC7620_INT_GIO, "pac7620_int_gpio");
  if (result != 0)
	{
		dbmsg("pac7620 interrupt request failed!\n");
		goto rtn;
	}
		
	result = gpio_direction_input(PAC7620_INT_GIO);
	if (result != 0)
	{
		dbmsg("pac7620 direction failed!\n");
		goto rtn;
	}

	pac7620data.irq = gpio_to_irq(PAC7620_INT_GIO);
	
	result = request_threaded_irq(pac7620data.irq, NULL,
				  pac7620_irq_thread_fn,
				  IRQF_TRIGGER_FALLING, // trugger -> level change
				  "pac7620_irq", &pac7620data);
	
	if (result != 0)
	{
		dbmsg("pac7620 request_irq failed \n");
		goto rtn;
	}	

	enable_irq(pac7620data.irq);	

rtn:
	dbmsg("%s (%d) : gpio %d, irq %d, ret (%d)\n", __func__, __LINE__, PAC7620_INT_GIO, pac7620data.irq , result);
	
	return result;
//#else
	//use mtk way,keep interrupt for gesture
	mt_set_gpio_dir(PAC7620_INT_GIO, GPIO_DIR_IN);
	mt_set_gpio_mode(PAC7620_INT_GIO, GPIO_ALS_EINT_PIN_M_EINT);
	mt_set_gpio_pull_enable(PAC7620_INT_GIO, TRUE);
	mt_set_gpio_pull_select(PAC7620_INT_GIO, GPIO_PULL_UP);

	mt_eint_registration(CUST_EINT_ALS_NUM, CUST_EINTF_TRIGGER_FALLING, pac7620_irq_thread_fn, 0);

	mt_eint_unmask(CUST_EINT_ALS_NUM);



	return 0;	
}
#endif

//static struct kobject *example_kobj;
#ifdef CONFIG_HAS_EARLYSUSPEND
static void pac7620_early_suspend(struct early_suspend *handler)
{
	dbmsg("go to pac7620_early_suspend");
	if(sensor_state == SENSOR_OPEN)
	{
		if(0)//thread_flag == THREAD_RUN)
		{
			kthread_stop(thread);	
		}

		
		{
			// Now we go into suspend mode
			pac7620_set_reg(0xEF,0x01); // Switch to bank 1
			//pac7620_set_reg(0x7E,0x00); // disable SPI, Modify By Angelo, 20130228
			pac7620_set_reg(0x72,0x00); // disable 7620
			
			pac7620_set_reg(0xEF,0x00); // Switch to bank 0
			pac7620_set_reg(0x03,0x00); // disable I2C	
		}
		
	}				
}

static void pac7620_early_resume(struct early_suspend *handler)
{
	u8 data;
	int err;
	
	dbmsg("go to pac7620_early_resume");
	if(sensor_state == SENSOR_OPEN)
	{
		if(0)//thread_flag == THREAD_RUN)
		{
			thread = kthread_run(gesture_get_sched, 0, "mouse_resume");
		    if (IS_ERR(thread))
		    {
		    	err = PTR_ERR(thread);
		       dbmsg("cursor" " failed to create kernel thread: %d \n", err);
		    }
		}
		
		{	
			// Now we go into resume mode	
			// Read ID 3 times to make sure I2C is active
			pac7620_i2c_read(0x00, &data);
			pac7620_i2c_read(0x00, &data);
			pac7620_i2c_read(0x00, &data);
			
			pac7620_set_reg(0xEF,0x01); // Switch to bank 1
			pac7620_set_reg(0x72,0x01); // Enable 7620
			//pac7620_set_reg(0x7E,0x01); // Enable SPI, Modify By Angelo, 20130228	
		}
		
	}
}
#endif

static int pac7620_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id)
{
	int err = 0;
	struct i2c_adapter *adapter;
	dbmsg("%s (%d) : probe module 000 sun\n", __func__, __LINE__);
	
	adapter = to_i2c_adapter(client->dev.parent);

	dbmsg("%s (%d) : probe module\n", __func__, __LINE__);

	 
	
	if (!i2c_check_functionality(adapter, I2C_FUNC_SMBUS_BYTE)) {
		err = -EIO;
		return err;
	}	

	pac7620data.client = client;
/********** 
	err = pac7620_init_reg();
	if (err < 0) {
		return err;
	}
	
**********/ 

	err = pac7620_init_cursor_data(); 
	if (err < 0) {
		return err;
	}

	//err = pac7620_init_interrupt();
	//if (err < 0) {
	//	return err;
	//}

#ifdef CONFIG_HAS_EARLYSUSPEND
	pac_early_suspend.level = EARLY_SUSPEND_LEVEL_BLANK_SCREEN + 1;//bland screen will call
	pac_early_suspend.suspend = pac7620_early_suspend;
	pac_early_suspend.resume = pac7620_early_resume;
	register_early_suspend(&pac_early_suspend);
#endif	
	
//  example_kobj = kobject_create_and_add("pac7620", kernel_kobj);
//  if (!example_kobj)
//       return -ENOMEM;

  /* Create the files associated with this kobject */
//  err = sysfs_create_group(example_kobj, &rw_reg_attribute_group);
//  if (err)
//       kobject_put(example_kobj);
/*#if 0
	thread = kthread_run(cursor_event_handler, 0, "cursor");
    if (IS_ERR(thread))
    {
    	err = PTR_ERR(thread);
        dbmsg("cursor" " failed to create kernel thread: %d \n", err);
    }
#endif
	thread = kthread_run(cursor_event_handler, 0, "mouse");
    if (IS_ERR(thread))
    {
    	err = PTR_ERR(thread);
        dbmsg("cursor" " failed to create kernel thread: %d \n", err);
    }
*/	
	return err;	
}


static int pac7620_i2c_remove(struct i2c_client *client)
{
	pac7620_set_reg(0x03,0x00);

	//free_irq(pac7620data.irq, &pac7620data);
	
	//gpio_free(PAC7620_INT_GIO);

	sysfs_remove_group(&pac7620data.cursor_input_dev->dev.kobj,
			   &rw_reg_attribute_group);
	
	input_unregister_device(pac7620data.cursor_input_dev);
#ifdef CONFIG_HAS_EARLYSUSPEND
	 unregister_early_suspend(&pac_early_suspend);
#endif /* CONFIG_HAS_EARLYSUSPEND */
	return 0;
}

static int pac7620_suspend(struct device *dev)
{
     dbmsg(" ***sun*** pac7620_suspend");
	// Now we go into suspend mode
	pac7620_set_reg(0xEF,0x01); // Switch to bank 1
	//pac7620_set_reg(0x7E,0x00); // disable SPI, Modify By Angelo, 20130228
	pac7620_set_reg(0x72,0x00); // disable 7620
	
	pac7620_set_reg(0xEF,0x00); // Switch to bank 0
	pac7620_set_reg(0x03,0x00); // disable I2C	

	return 0;
}

static int pac7620_resume(struct device *dev)
{
	u8 data;
	dbmsg(" ***sun*** pac7620_resume");
	// Now we go into resume mode
	// Read ID 3 times to make sure I2C is active
	pac7620_i2c_read(0x00, &data);
	pac7620_i2c_read(0x00, &data);
	pac7620_i2c_read(0x00, &data);
	
	pac7620_set_reg(0xEF,0x01); // Switch to bank 1
	pac7620_set_reg(0x72,0x01); // Enable 7620
	//pac7620_set_reg(0x7E,0x01); // Enable SPI, Modify By Angelo, 20130228	

	return 0;
}

/*#if 0//def CONFIG_HAS_EARLYSUSPEND
static void pac7620_early_suspend(struct early_suspend *handler)
{
	dbmsg("go to pac7620_suspend");
	// Now we go into suspend mode
	pac7620_set_reg(0xEF,0x01); // Switch to bank 1
	//pac7620_set_reg(0x7E,0x00); // disable SPI, Modify By Angelo, 20130228
	pac7620_set_reg(0x72,0x00); // disable 7620
	
	pac7620_set_reg(0xEF,0x00); // Switch to bank 0
	pac7620_set_reg(0x03,0x00); // disable I2C	


}

static void pac7620_early_resume(struct early_suspend *handler)
{
	u8 data;
	
	dbmsg("go to pac7620_resume");
	// Now we go into resume mode	
	// Read ID 3 times to make sure I2C is active
	pac7620_i2c_read(0x00, &data);
	pac7620_i2c_read(0x00, &data);
	pac7620_i2c_read(0x00, &data);
	
	pac7620_set_reg(0xEF,0x01); // Switch to bank 1
	pac7620_set_reg(0x72,0x01); // Enable 7620
	//pac7620_set_reg(0x7E,0x01); // Enable SPI, Modify By Angelo, 20130228	

}
#endif
*/
static int pac_suspend(void)
{
	dbmsg(" ***sun*** pac_suspend");
	// Now we go into suspend mode
	pac7620_set_reg(0xEF,0x01); // Switch to bank 1
	//pac7620_set_reg(0x7E,0x00); // disable SPI, Modify By Angelo, 20130228
	pac7620_set_reg(0x72,0x00); // disable 7620
	
	pac7620_set_reg(0xEF,0x00); // Switch to bank 0
	pac7620_set_reg(0x03,0x00); // disable I2C	

	return 0;
}
static int pac_resume(void)
{
	u8 data;
	dbmsg(" ***sun*** pac_resume");
	// Now we go into resume mode
	// Read ID 3 times to make sure I2C is active
	pac7620_i2c_read(0x00, &data);
	pac7620_i2c_read(0x00, &data);
	pac7620_i2c_read(0x00, &data);
	
	pac7620_set_reg(0xEF,0x01); // Switch to bank 1
	pac7620_set_reg(0x72,0x01); // Enable 7620
	//pac7620_set_reg(0x7E,0x01); // Enable SPI, Modify By Angelo, 20130228	

	return 0;
}
static long cursor_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
	int ret = 0;
	int cnt =5;
	//	cmd = (unsigned int)file; //解决与2.3版本兼容问题
	//dbmsg("cmd=0x%x, arg=0x%x \n", cmd, (unsigned int)arg);
	switch ( cmd ) {
		
		case CURSOR_OPEN:
		    dbmsg("CURSOR_OPEN \n");
			sensor_state = SENSOR_OPEN;
			for(cnt;cnt>0;cnt--)
			{
				ret=pac7620_init_reg();	//mode 里面进行初始化?
				
				if(ret<0)
				{	
					dbmsg("CURSOR_OPEN fail ret=%d\n",ret);
					//return ret;
				}
				else 
				{
					dbmsg("CURSOR_OPEN success ret=%d\n",ret);
					break;
				}
			
			}
			break;
		case CURSOR_CLOSE:
		    dbmsg("CURSOR_CLOSE thread_flag=%d\n",thread_flag);
			sensor_state = SENSOR_CLOSE;
			
			if(thread_flag == THREAD_RUN)//切换模式时是否调用 导致重复
			{
				thread_flag = THREAD_STOP;
				kthread_stop(thread);
				dbmsg("kthread_stop by CURSOR_CLOSE \n");
			}			
			pac_suspend();
			break;
		case CURSOR_SET://back 
			dbmsg("CURSOR_SET \n");
			ret = copy_from_user(&coord,(void const *) arg, sizeof(coord));
			if(ret<0)
				return ret;
			cursor_down(coord.x,coord.y,1);
			break;
		case MOUSE_ORIGN_GET://add for factory test
			dbmsg("MOUSE_ORIGN_GET \n");
			factory_pac7620_cursor_get();			
			ret = copy_to_user((void *)arg, &coord, sizeof(coord));
			if(ret<0)
				return ret;
			break;
		case CURSOR_GET:
			dbmsg("CURSOR_GET \n");
			pac7620_cursor_get();
			ret = copy_to_user((void *)arg, &coord, sizeof(coord));
			if(ret<0)
				return ret;
			break;
		case CURSOR_MODE:
			dbmsg("CURSOR_MODE thread_flag=%d\n",thread_flag);
			sensor_state = SENSOR_OPEN;
			if(thread_flag == THREAD_RUN)
			{
				thread_flag = THREAD_STOP;
				kthread_stop(thread);
			}
			pac7620_mode = CURSOR_MODE_ENUM;			
			for(cnt;cnt>0;cnt--)
			{
				ret=pac7620_init_reg();	//mode 里面进行初始化?
				
				if(ret<0)
				{	
					dbmsg("CURSOR_OPEN fail ret=%d\n",ret);
					//return ret;
				}
				else 
				{
					dbmsg("CURSOR_OPEN success ret=%d\n",ret);
					break;
				}
			
			}
			break;
		case GESTURE_MODE:
			dbmsg("GESTURE_MODE thread_flag=%d\n",thread_flag);
			sensor_state = SENSOR_OPEN;
			pac7620_mode = GESTURE_MODE_ENUM;
			for(cnt;cnt>0;cnt--)
			{
				ret=pac7620_init_reg();	//mode 里面进行初始化?
				
				if(ret<0)
				{	
					dbmsg("CURSOR_OPEN fail ret=%d\n",ret);
					//return ret;
				}
				else 
				{
					dbmsg("CURSOR_OPEN success ret=%d\n",ret);
					break;
				}
			
			}
			if(thread_flag != THREAD_RUN)
			{
				thread_flag = THREAD_RUN;
				dbmsg("thread_flag = THREAD_RUN; \n");
				thread = kthread_run(gesture_get_sched, 0, "mouse_gesture");
			    if (IS_ERR(thread))
			    {
			    	ret = PTR_ERR(thread);
			       dbmsg("cursor" " failed to create kernel thread: %d \n", ret);
			    }	
			}
			break;
		case GESTURE_GET:
			dbmsg("GESTURE_GET \n");
			if(copy_to_user((void *)arg, &gesture_state, sizeof(gesture_state)))
			{
				ret = -EFAULT;
				dbmsg("err copy_to_user\n");
				break;
			}		
			gesture_state = 0;
			break;
		case GESTURE_SET:
			dbmsg("GESTURE_SET \n");
			if(copy_from_user(&gesture_state,(void const *) arg, sizeof(gesture_state)))				
			{
				ret = -EFAULT;
				dbmsg("err copy_to_user\n");
				break;
			}	
			break;
		default:
			{
				break;
			}
	}
	return ret;
}

int cursor_open (struct inode *inode, struct file *filp)
{
	dbmsg("cursor open flag: open \n");
	return 0;
}
ssize_t cursor_read(struct file *filp, char __user *buff, size_t count, loff_t *ppos)
{

	return 0;
}
ssize_t cursor_write(struct file *filp, const char __user *buff, size_t count, loff_t *ppos)
{
	return 0;
}

static int cursor_release(struct inode *node, struct file *file)
{
	return 0;
}
static struct file_operations cursor_ops = { 
	.owner   = THIS_MODULE,
	.open    = cursor_open,
	.release = cursor_release,
	.read    = cursor_read,
	.write   = cursor_write,
	.unlocked_ioctl   = cursor_ioctl,	//ioctl
};


static const struct i2c_device_id pac7620_device_id[] = {
	{"pac7620", 0},
	{}
};
static struct i2c_board_info __initdata pac7620=
{
	I2C_BOARD_INFO("pac7620", 0x73),//0x39  0x50 write addr

};

static inline void register_boardinfo(void)
{
	i2c_register_board_info(2, &pac7620, 1);/*静态注册*/
	//pac_client_p = pac7620data.client;
	//pac_adap_p = &pac_adap;
	//pac_client_p = i2c_new_device(pac_adap_p,&pac7620);
	//struct i2c_client * i2c_new_device(struct i2c_adapter *adap, struct i2c_board_info const *info);	
}

MODULE_DEVICE_TABLE(i2c, pac7620_device_id);

static const struct dev_pm_ops pac7620_pm_ops = {
	.suspend = pac7620_suspend,
	.resume = pac7620_resume
};

static struct i2c_driver pac7620_i2c_driver = {
	.driver = {
		   .name = "pac7620",
		   .owner = THIS_MODULE,
		   //.pm = &pac7620_pm_ops
		   },
	.probe = pac7620_i2c_probe,
	.remove = pac7620_i2c_remove,
	.id_table = pac7620_device_id,
};

static struct miscdevice misc = {
       .minor = MISC_DYNAMIC_MINOR,
       .name  = "mouse",
       .fops  = &cursor_ops,
};

static int __init pac7620_init(void)
{
	int ret;
	register_boardinfo();
	
	dbmsg("%s (%d) : init module\n", __func__, __LINE__);
	 
	ret = i2c_add_driver(&pac7620_i2c_driver);
	
	if(ret < 0)
		return ret;

	ret = misc_register(&misc);
	dbmsg("%s i2c_add_driver ret = %d\n", __func__, ret);
	return ret;
	
}

static void __exit pac7620_exit(void)
{
	dbmsg("%s (%d) : exit module\n", __func__, __LINE__);	
	misc_deregister(&misc);
	i2c_del_driver(&pac7620_i2c_driver);
}

module_init(pac7620_init);
module_exit(pac7620_exit);
MODULE_AUTHOR("www.estar.cn");
MODULE_LICENSE("Dual BSD/GPL");
MODULE_VERSION("1.0");
