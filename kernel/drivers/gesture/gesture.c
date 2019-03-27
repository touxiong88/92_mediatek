#include <linux/module.h>               // For module specific items
#include <linux/moduleparam.h>          // For new moduleparam's
#include <linux/types.h>                // For standard types (like size_t)
#include <linux/errno.h>                // For the -ENODEV/... values 
#include <linux/kernel.h>               // For dbmsg/panic/... 
#include <linux/fs.h>                   // For file operations 
#include <linux/ioport.h>               // For io-port access 
#include <linux/platform_device.h>      // For platform_driver framework 
#include <linux/init.h>                 // For __init/__exit/... 
#include <linux/uaccess.h>              // For copy_to_user/put_user/... 
#include <linux/io.h>                   // For inb/outb/... 
#include <linux/gpio.h>  								// GPIO
#include <linux/device.h>  
#include <linux/cdev.h>  
#include <linux/slab.h>               	//kamlloc  

#include <linux/string.h>
#include <linux/mutex.h>
#include <linux/kmod.h>
#include <linux/mm.h>
#include <asm/system.h>

#include <linux/hrtimer.h>
#include <linux/err.h>
#include <linux/spinlock.h>

#include <linux/jiffies.h>
#include <linux/timer.h>



#include <mach/mt_pwm.h>  
#include <mach/mt_typedefs.h>
//#include <mach/mt_clock_manager.h>


#include <mach/mt_reg_base.h>
#include <mach/mt_boot.h>
#include <mtk_kpd.h>		/* custom file */
#include <mach/mt_gpio.h>
#include <linux/kthread.h>
#include <cust_alsps.h>


typedef unsigned int u32;
typedef unsigned long long u64;
typedef  int s32;
typedef  long long s64;


#define CURSOR_SET_DATA  0x5810
#define CURSOR_GET_DATA  0x5811

#define MAX_X	(1080)	//3327 4200
#define MAX_Y	(1920)	//3327 6300
//#include <mach/irqs.h>
//#include <mach/eint.h>
//#include <mach/mt_gpio.h>
/**************gpio_config_start*********************/
//#include <linux/gpio.h>

struct estar_gesture_t
{
	struct cdev cdev; 
	struct class * estar_class;
	dev_t devno;
	int device_major;
};
struct estar_gesture_t *estar_gesturep; /* 设备结构体指针*/ 
struct input_dev  *pixart_input_dev = 0;

static struct task_struct *thread = NULL;

struct cursors
{
	s32 x1,y1;
	u8	press,cmd;
//	s32    x2,y2;
//	s32    x3,y3;
//	s32    x4,y4;
};
typedef struct cursors CURSOR;
static CURSOR coord={0};

struct packets
{
	u8 	start_h,start_l;/*接收包起始码 正确为 0xff 0xff*/
	u8  y_h,y_l;		/*Y轴高低共12bit有效数据*/ 
	u8  x_h,x_l;		/*x轴高低共12bit有效数据*/ 
	u8	press,cmd;		/*按键按下标志，预留命令*/
	u8	end_h,end_l;	/*接收包尾码 正确为 0xfe 0xfe*/
};						/**/
typedef struct packets PACKET;

static PACKET recv={0};//,*recv_p;


static int cursor_halt = 0;/*updata flag*/
static int cursor_flag = 0;/*interrupt flag*/
static int up_flag = 1;/*up flag*/
static s32 pre_x,pre_y;
static s32 delta_x,delta_y;

#define GESTURE_DEBUG
#ifdef GESTURE_DEBUG
#define dbmsg(fmt, args ...) printk(KERN_NOTICE "gesture: %s[%d]: "fmt"\n", __FUNCTION__, __LINE__,##args)
#else
#define dbmsg(fmt, args ...)  
#endif

//static int gesture_major = 252;   // gesture_major = 0;
static int  gesture_major = 0;

//static int cursor_event_handler(void *unused);



static void cursor_down(s32 x, s32 y,u8 press)
{
	/* Report relative coordinates via the event interface */

	//input_report_abs(pixart_input_dev, ABS_MT_PRESSURE, 100);
    input_report_abs(pixart_input_dev, ABS_MT_TOUCH_MAJOR, 1);//touch area size 
	input_report_key(pixart_input_dev, BTN_TOUCH, press);	//BTN_LEFT
	//input_report_key(pixart_input_dev, BTN_TOOL_PEN, 1);
    input_report_abs(pixart_input_dev, ABS_MT_POSITION_X, x);
    input_report_abs(pixart_input_dev, ABS_MT_POSITION_Y, y);
    input_mt_sync(pixart_input_dev);
	input_sync(pixart_input_dev);

	dbmsg("down x=%d ,y = %d press = %d\n",x,y,press);
}
static void cursor_up(s32 x, s32 y,u8 press)
{
	/* Report relative coordinates via the event interface */

	//input_report_abs(pixart_input_dev, ABS_DISTANCE, 0);
    input_report_abs(pixart_input_dev, ABS_MT_TOUCH_MAJOR, 1);//touch area size 
	input_report_key(pixart_input_dev, BTN_TOUCH, 0);	//BTN_LEFT
	//input_report_key(pixart_input_dev, BTN_TOOL_PEN, 1);
    input_report_abs(pixart_input_dev, ABS_MT_POSITION_X, x);
    input_report_abs(pixart_input_dev, ABS_MT_POSITION_Y, y);
    input_mt_sync(pixart_input_dev);
	input_sync(pixart_input_dev);


	dbmsg("down x=%d ,y = %d press = %d\n",x,y,press);

}	

static void pen_up(s32 x, s32 y,u8 press)
{
	/* Report relative coordinates via the event interface */

	//input_report_abs(pixart_input_dev, ABS_DISTANCE, 0);
    input_report_abs(pixart_input_dev, ABS_MT_TOUCH_MAJOR, 1);//touch area size 
	input_report_key(pixart_input_dev, BTN_TOUCH, 1);	//BTN_LEFT
	//input_report_key(pixart_input_dev, BTN_TOOL_PEN, 1);
    input_report_abs(pixart_input_dev, ABS_MT_POSITION_X, x);
    input_report_abs(pixart_input_dev, ABS_MT_POSITION_Y, y);
    input_mt_sync(pixart_input_dev);
	input_sync(pixart_input_dev);


	dbmsg("down x=%d ,y = %d press = %d\n",x,y,press);

}	


/*Coordination mapping*/
static int cursor_calibrate_driver(PACKET *recv_p)
{
	short int tmp_x,tmp_y;
	
	if((0xff == recv_p->start_h )&&(0xfe == recv_p->end_h))
		{

			
			if(recv_p->press == 0x55)//down
				{
					coord.press =1;
					up_flag = 0;// not up flag
				}
			else if(recv_p->press == 0x80)//up
				{
					coord.press =0; //return 0;//up don't deal coord					
				}
			tmp_x= (recv_p->x_h<<8)|recv_p->x_l;
			tmp_y= (recv_p->y_h<<8)|recv_p->y_l;						   	

			coord.x1 = 1080-tmp_x; //s16 ->s32
			coord.y1 = tmp_y; //s16 ->s32
			
			//coord.x1 = -coord.x1;//mirror
			
			if((coord.x1 >= 1080))	coord.x1 = 1079;
			if((coord.y1 >= 1920))	coord.y1 = 1919;

			delta_x = coord.x1 - pre_x ;
			delta_y = coord.y1 - pre_y ;	
			

			
			dbmsg("press = 0x%x,tmp_x= 0x%x tmp_y= 0x%x; x = %d y = %d \n",recv_p->press ,tmp_x,tmp_y,coord.x1 ,coord.y1);
			return 0;
		}
	else
		{
			return -1;
		}
}

static int cursor_event_handler(void *unused)
{	
	 int x = 2,y = 2;
	 struct sched_param param = { .sched_priority = RTPM_PRIO_TPD };
	 sched_setscheduler(current, SCHED_RR, &param);
	//cursor_down(500,1000,1);
	 do
    {
        //set_current_state(TASK_INTERRUPTIBLE);			
		
        //if (cursor_halt)
        {
            //cursor_halt = 0;                    
			 while (!cursor_flag)
	        {
	            //cursor_halt = 0;
	            msleep(20);
	        }
	        //wait_event_interruptible(waiter, cursor_flag != 0);
			//cursor_flag = 0;

			if(++x > 1080) x = 5;
			if(++y > 1920) y = 5;
			//cnt++;
			/*
			if(cnt <= 500)
				{
					x=y= 2;
				}
			else if(cnt <= 1000)
				{
					x=y= -2;
				}
			else
				{
					cnt = 0;
				}
				*/
			//if(0 == x) cursor_up(0,0,0);
			pen_up(x,y,0);
			//dbmsg("x1 = %d,y1= %d \n",x,y);
			//cursor_down(coord.x1,coord.y1,coord.press);
			msleep(1);
			//cursor_up(0,0,0);
			msleep(50);
		}
		if(cursor_halt)
			kthread_stop(thread);  
	 }
	 while(!kthread_should_stop());
	return 0;
}

//extern   void tpd_up(int x, int y,int *count); //声明要调用的函数  

static long gesture_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
	int ret,err,i,cnt;
	int positive_delta_x,positive_delta_y;//正数positive
	//	cmd = (unsigned int)file; //解决与2.3版本兼容问题
	//dbmsg("cmd=0x%x, arg=0x%x \n", cmd, (unsigned int)arg);
	switch ( cmd ) {
		
		case CURSOR_SET_DATA:
			ret = copy_from_user(&recv,(void const *) arg, sizeof(recv));
			cursor_halt = 1;
			cursor_flag = 0;
			//dbmsg("ret=0x%x \n",ret);
			ret = cursor_calibrate_driver(&recv);
			if(0 == ret)
			{
					if(coord.press)	
					{
						if(0)//(delta_x>5)||(delta_x<-5)||(delta_y>5)||(delta_y<-5))//补点
						{
							if(delta_x < 0)
								positive_delta_x = -delta_x;
							else	
								positive_delta_x = delta_x;
								
							if(delta_y < 0)
								positive_delta_y = -delta_y;
							else
								positive_delta_y = delta_y;
							
							positive_delta_x>positive_delta_y?(cnt = positive_delta_x/5):(cnt = positive_delta_y/5);
					
					
							for (i=1;i<=cnt;i++)
							{									
								cursor_down(pre_x+(delta_x*i/cnt),pre_y+(delta_y*i/cnt),coord.press);
							}					
							
						}									
						
						cursor_down(coord.x1,coord.y1,coord.press);
				
						pre_x = coord.x1;
						pre_y = coord.y1;
					}
					else
					{
							//if(up_flag) //already up
								//break;
							//else
							{
								//cursor_up(0,0,0);
								cursor_up(coord.x1,coord.y1,coord.press);
								up_flag = 1;//already up
							}
					}
			}

			//cursor_up(0,0,0);
			break;
		case CURSOR_GET_DATA:	//用于测试自动画线
			cursor_halt = 0;
			cursor_flag = 1;/*run auto paint*/

			thread = kthread_run(cursor_event_handler, 0, "gesture");
			cursor_up(0,0,0);
		    if (IS_ERR(thread))
		    {
		    	err = PTR_ERR(thread);
		        dbmsg("GESTURE" " failed to create kernel thread: %d \n", err);
		    }
			break;
			
		default:
			{
				break;
			}
	}
	return 0;
}

int gesture_open (struct inode *inode, struct file *filp)
{
	dbmsg("gesture open flag: open \n");
	return 0;
}
ssize_t gesture_read(struct file *filp, char __user *buff, size_t count, loff_t *ppos)
{

	return 0;
}
ssize_t gesture_write(struct file *filp, const char __user *buff, size_t count, loff_t *ppos)
{
	return 0;
}

static int gesture_release(struct inode *node, struct file *file)
{
	return 0;
}
static struct file_operations gesture_remap_ops = { 
	.owner   = THIS_MODULE,
	.open    = gesture_open,
	.release = gesture_release,
	.read    = gesture_read,
	.write   = gesture_write,
	.unlocked_ioctl   = gesture_ioctl,	//ioctl
};
static void gesture_setup_cdev(struct cdev *dev, int minor,
		struct file_operations *fops)
{
	int err;
	cdev_init(dev, fops);																	
	dev->owner = THIS_MODULE;
	dev->ops = fops;
	err = cdev_add (dev, estar_gesturep->devno, 1);												
	if (err)
		dbmsg (KERN_NOTICE "Error %d adding gesture %d", err, minor);
}

static int __init gesture_init(void)
{
	int result;
	dbmsg("gesture init");
	estar_gesturep = 	kmalloc(sizeof(struct estar_gesture_t), GFP_KERNEL);
	if(NULL == estar_gesturep)
	{
		dbmsg("NO Memory!");
		return -1;
	}
	memset(estar_gesturep, 0, sizeof(struct estar_gesture_t));
	estar_gesturep->devno = MKDEV(gesture_major, 0);										

	result = alloc_chrdev_region(&estar_gesturep->devno, 0, 1, "gesture");	
	estar_gesturep->device_major= MAJOR(estar_gesturep->devno);														

	if (result < 0) {																			
		dbmsg(KERN_WARNING "gesture: unable to get major %d\n", estar_gesturep->device_major);
		return result;
	}
	if (estar_gesturep->device_major== 0)																	
		estar_gesturep->device_major= result;

	gesture_setup_cdev(&estar_gesturep->cdev, 0, &gesture_remap_ops);		

	estar_gesturep->estar_class= class_create(THIS_MODULE, "gesture");					
	device_create(estar_gesturep->estar_class, NULL, estar_gesturep->devno, NULL, "gesture");																
				
   /* allocate input device */
  	pixart_input_dev = input_allocate_device();
	if (!pixart_input_dev) {
	dbmsg("Bad input_alloc_device()\n");
	}
   /* Announce that the virtual mouse will generate
	relative coordinates */

	/*********** register input device	**************/
		set_bit(EV_ABS, pixart_input_dev->evbit);
		set_bit(EV_KEY, pixart_input_dev->evbit);
		set_bit(ABS_X, pixart_input_dev->absbit);
		set_bit(ABS_Y, pixart_input_dev->absbit);
		//set_bit(ABS_Z, pixart_input_dev->absbit);
		set_bit(ABS_PRESSURE, pixart_input_dev->absbit);
		set_bit(BTN_TOUCH, pixart_input_dev->keybit);//BTN_TOOL_PEN
		set_bit(BTN_TOOL_PEN, pixart_input_dev->keybit);
		set_bit(INPUT_PROP_DIRECT, pixart_input_dev->propbit);
	
		set_bit(ABS_DISTANCE, pixart_input_dev->absbit);
		set_bit(ABS_MT_TRACKING_ID, pixart_input_dev->absbit);
		set_bit(ABS_MT_TOUCH_MAJOR, pixart_input_dev->absbit);
		set_bit(ABS_MT_TOUCH_MINOR, pixart_input_dev->absbit);
		set_bit(ABS_MT_POSITION_X, pixart_input_dev->absbit);
		set_bit(ABS_MT_POSITION_Y, pixart_input_dev->absbit);
	
		input_set_abs_params(pixart_input_dev, ABS_DISTANCE, 0, 100, 0, 0);
		input_set_abs_params(pixart_input_dev, ABS_MT_POSITION_X, 0, 1080, 0, 0);
		input_set_abs_params(pixart_input_dev, ABS_MT_POSITION_Y, 0, 1920, 0, 0);
		input_set_abs_params(pixart_input_dev, ABS_MT_TOUCH_MAJOR, 0, 100, 0, 0);
		input_set_abs_params(pixart_input_dev, ABS_MT_TOUCH_MINOR, 0, 100, 0, 0);
		input_set_abs_params(pixart_input_dev, ABS_X, 0, 1080, 0, 0);
		input_set_abs_params(pixart_input_dev, ABS_Y, 0, 1920, 0, 0);
		input_abs_set_res(pixart_input_dev, ABS_X, 1080);
		input_abs_set_res(pixart_input_dev, ABS_Y, 1920);
		input_set_abs_params(pixart_input_dev, ABS_PRESSURE, 0, 255, 0, 0);
		
		/* Register with the input subsystem */
		result = input_register_device(pixart_input_dev);

		if (result < 0) {																			
		dbmsg( "Virtual Mouse Driver Initialized fail\n");
		return result;
		}
		
	return 0;
}
static void __exit gesture_cleanup(void)
{
	/* Unregister from the input subsystem */
	input_unregister_device(pixart_input_dev);
	device_destroy(estar_gesturep->estar_class,estar_gesturep->devno);				
	class_destroy(estar_gesturep->estar_class);
	cdev_del(&estar_gesturep->cdev);																	
	unregister_chrdev_region(estar_gesturep->devno, 1);		
	dbmsg("gesture device uninstalled\n");
}

module_init(gesture_init);
module_exit(gesture_cleanup);										
MODULE_AUTHOR("www.estar.cn");
MODULE_LICENSE("Dual BSD/GPL");
MODULE_VERSION("1.0");
