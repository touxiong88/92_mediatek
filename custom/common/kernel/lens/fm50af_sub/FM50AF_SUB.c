/*
 * MD218A voice coil motor driver
 *
 *
 */

#include <linux/i2c.h>
#include <linux/delay.h>
#include <linux/platform_device.h>
#include <linux/cdev.h>
#include <linux/uaccess.h>
#include <linux/fs.h>
#include <asm/atomic.h>
#include "FM50AF_SUB.h"
#include "../camera/kd_camera_hw.h"

#define LENS_I2C_BUSNUM 1
static struct i2c_board_info __initdata kd_lens_dev={ I2C_BOARD_INFO("FM50AF_SUB", 0x18)};


#define FM50AF_SUB_DRVNAME "FM50AF_SUB"
#define FM50AF_SUB_VCM_WRITE_ID           0x18

#define FM50AF_SUB_DEBUG
#ifdef FM50AF_SUB_DEBUG
#define FM50AF_SUBDB printk
#else
#define FM50AF_SUBDB(x,...)
#endif

static spinlock_t g_FM50AF_SUB_SpinLock;

static struct i2c_client * g_pstFM50AF_SUB_I2Cclient = NULL;

static dev_t g_FM50AF_SUB_devno;
static struct cdev * g_pFM50AF_SUB_CharDrv = NULL;
static struct class *actuator_class = NULL;

static int  g_s4FM50AF_SUB_Opened = 0;
static long g_i4MotorStatus = 0;
static long g_i4Dir = 0;
static unsigned long g_u4FM50AF_SUB_INF = 0;
static unsigned long g_u4FM50AF_SUB_MACRO = 1023;
static unsigned long g_u4TargetPosition = 0;
static unsigned long g_u4CurrPosition   = 0;

static int g_sr = 3;

#if 0
extern s32 mt_set_gpio_mode(u32 u4Pin, u32 u4Mode);
extern s32 mt_set_gpio_out(u32 u4Pin, u32 u4PinOut);
extern s32 mt_set_gpio_dir(u32 u4Pin, u32 u4Dir);
#endif

static int s4FM50AF_SUB_ReadReg(unsigned short * a_pu2Result)
{
    int  i4RetValue = 0;
    char pBuff[2];

    i4RetValue = i2c_master_recv(g_pstFM50AF_SUB_I2Cclient, pBuff , 2);

    if (i4RetValue < 0) 
    {
        FM50AF_SUBDB("[FM50AF_SUB] I2C read failed!! \n");
        return -1;
    }

    *a_pu2Result = (((u16)pBuff[0]) << 4) + (pBuff[1] >> 4);

    return 0;
}

static int s4FM50AF_SUB_WriteReg(u16 a_u2Data)
{
    int  i4RetValue = 0;

    char puSendCmd[2] = {(char)(a_u2Data >> 4) , (char)(((a_u2Data & 0xF) << 4)+g_sr)};

    //FM50AF_SUBDB("[FM50AF_SUB] g_sr %d, write %d \n", g_sr, a_u2Data);
    g_pstFM50AF_SUB_I2Cclient->ext_flag |= I2C_A_FILTER_MSG;
    i4RetValue = i2c_master_send(g_pstFM50AF_SUB_I2Cclient, puSendCmd, 2);
	
    if (i4RetValue < 0) 
    {
        FM50AF_SUBDB("[FM50AF_SUB] I2C send failed!! \n");
        return -1;
    }

    return 0;
}

inline static int getFM50AF_SUBInfo(__user stFM50AF_SUB_MotorInfo * pstMotorInfo)
{
    stFM50AF_SUB_MotorInfo stMotorInfo;
    stMotorInfo.u4MacroPosition   = g_u4FM50AF_SUB_MACRO;
    stMotorInfo.u4InfPosition     = g_u4FM50AF_SUB_INF;
    stMotorInfo.u4CurrentPosition = g_u4CurrPosition;
    stMotorInfo.bIsSupportSR      = TRUE;

	if (g_i4MotorStatus == 1)	{stMotorInfo.bIsMotorMoving = 1;}
	else						{stMotorInfo.bIsMotorMoving = 0;}

	if (g_s4FM50AF_SUB_Opened >= 1)	{stMotorInfo.bIsMotorOpen = 1;}
	else						{stMotorInfo.bIsMotorOpen = 0;}

    if(copy_to_user(pstMotorInfo , &stMotorInfo , sizeof(stFM50AF_SUB_MotorInfo)))
    {
        FM50AF_SUBDB("[FM50AF_SUB] copy to user failed when getting motor information \n");
    }

    return 0;
}

#ifdef LensdrvCM3
inline static int getFM50AF_SUBMETA(__user stFM50AF_SUB_MotorMETAInfo * pstMotorMETAInfo)
{
    stFM50AF_SUB_MotorMETAInfo stMotorMETAInfo;
    stMotorMETAInfo.Aperture=2.8;      //fn
	stMotorMETAInfo.Facing=1;   
	stMotorMETAInfo.FilterDensity=1;   //X
	stMotorMETAInfo.FocalDistance=1.0;  //diopters
	stMotorMETAInfo.FocalLength=34.0;  //mm
	stMotorMETAInfo.FocusRange=1.0;    //diopters
	stMotorMETAInfo.InfoAvalibleApertures=2.8;
	stMotorMETAInfo.InfoAvalibleFilterDensity=1;
	stMotorMETAInfo.InfoAvalibleFocalLength=34.0;
	stMotorMETAInfo.InfoAvalibleHypeDistance=1.0;
	stMotorMETAInfo.InfoAvalibleMinFocusDistance=1.0;
	stMotorMETAInfo.InfoAvalibleOptStabilization=0;
	stMotorMETAInfo.OpticalAxisAng[0]=0.0;
	stMotorMETAInfo.OpticalAxisAng[1]=0.0;
	stMotorMETAInfo.Position[0]=0.0;
	stMotorMETAInfo.Position[1]=0.0;
	stMotorMETAInfo.Position[2]=0.0;
	stMotorMETAInfo.State=0;
	stMotorMETAInfo.u4OIS_Mode=0;
	
	if(copy_to_user(pstMotorMETAInfo , &stMotorMETAInfo , sizeof(stFM50AF_SUB_MotorMETAInfo)))
	{
		FM50AF_SUBDB("[FM50AF_SUB] copy to user failed when getting motor information \n");
	}

    return 0;
}
#endif

inline static int moveFM50AF_SUB(unsigned long a_u4Position)
{
    int ret = 0;
    
    if((a_u4Position > g_u4FM50AF_SUB_MACRO) || (a_u4Position < g_u4FM50AF_SUB_INF))
    {
        FM50AF_SUBDB("[FM50AF_SUB] out of range \n");
        return -EINVAL;
    }

    if (g_s4FM50AF_SUB_Opened == 1)
    {
        unsigned short InitPos;
        ret = s4FM50AF_SUB_ReadReg(&InitPos);
	    
        if(ret == 0)
        {
            FM50AF_SUBDB("[FM50AF_SUB] Init Pos %6d \n", InitPos);
			
			spin_lock(&g_FM50AF_SUB_SpinLock);
            g_u4CurrPosition = (unsigned long)InitPos;
			spin_unlock(&g_FM50AF_SUB_SpinLock);
			
        }
        else
        {	
			spin_lock(&g_FM50AF_SUB_SpinLock);
            g_u4CurrPosition = 0;
			spin_unlock(&g_FM50AF_SUB_SpinLock);
        }

		spin_lock(&g_FM50AF_SUB_SpinLock);
        g_s4FM50AF_SUB_Opened = 2;
        spin_unlock(&g_FM50AF_SUB_SpinLock);
    }

    if (g_u4CurrPosition < a_u4Position)
    {
        spin_lock(&g_FM50AF_SUB_SpinLock);	
        g_i4Dir = 1;
        spin_unlock(&g_FM50AF_SUB_SpinLock);	
    }
    else if (g_u4CurrPosition > a_u4Position)
    {
        spin_lock(&g_FM50AF_SUB_SpinLock);	
        g_i4Dir = -1;
        spin_unlock(&g_FM50AF_SUB_SpinLock);			
    }
    else										{return 0;}

    spin_lock(&g_FM50AF_SUB_SpinLock);    
    g_u4TargetPosition = a_u4Position;
    spin_unlock(&g_FM50AF_SUB_SpinLock);	

    //FM50AF_SUBDB("[FM50AF_SUB] move [curr] %d [target] %d\n", g_u4CurrPosition, g_u4TargetPosition);

            spin_lock(&g_FM50AF_SUB_SpinLock);
            g_sr = 3;
            g_i4MotorStatus = 0;
            spin_unlock(&g_FM50AF_SUB_SpinLock);	
		
            if(s4FM50AF_SUB_WriteReg((unsigned short)g_u4TargetPosition) == 0)
            {
                spin_lock(&g_FM50AF_SUB_SpinLock);		
                g_u4CurrPosition = (unsigned long)g_u4TargetPosition;
                spin_unlock(&g_FM50AF_SUB_SpinLock);				
            }
            else
            {
                FM50AF_SUBDB("[FM50AF_SUB] set I2C failed when moving the motor \n");			
                spin_lock(&g_FM50AF_SUB_SpinLock);
                g_i4MotorStatus = -1;
                spin_unlock(&g_FM50AF_SUB_SpinLock);				
            }

    return 0;
}

inline static int setFM50AF_SUBInf(unsigned long a_u4Position)
{
    spin_lock(&g_FM50AF_SUB_SpinLock);
    g_u4FM50AF_SUB_INF = a_u4Position;
    spin_unlock(&g_FM50AF_SUB_SpinLock);	
    return 0;
}

inline static int setFM50AF_SUBMacro(unsigned long a_u4Position)
{
    spin_lock(&g_FM50AF_SUB_SpinLock);
    g_u4FM50AF_SUB_MACRO = a_u4Position;
    spin_unlock(&g_FM50AF_SUB_SpinLock);	
    return 0;	
}

////////////////////////////////////////////////////////////////
static long FM50AF_SUB_Ioctl(
struct file * a_pstFile,
unsigned int a_u4Command,
unsigned long a_u4Param)
{
    long i4RetValue = 0;

    switch(a_u4Command)
    {
        case FM50AF_SUBIOC_G_MOTORINFO :
            i4RetValue = getFM50AF_SUBInfo((__user stFM50AF_SUB_MotorInfo *)(a_u4Param));
        break;
		#ifdef LensdrvCM3
        case FM50AF_SUBIOC_G_MOTORMETAINFO :
            i4RetValue = getFM50AF_SUBMETA((__user stFM50AF_SUB_MotorMETAInfo *)(a_u4Param));
        break;
		#endif
        case FM50AF_SUBIOC_T_MOVETO :
            i4RetValue = moveFM50AF_SUB(a_u4Param);
        break;
 
        case FM50AF_SUBIOC_T_SETINFPOS :
            i4RetValue = setFM50AF_SUBInf(a_u4Param);
        break;

        case FM50AF_SUBIOC_T_SETMACROPOS :
            i4RetValue = setFM50AF_SUBMacro(a_u4Param);
        break;
		
        default :
      	    FM50AF_SUBDB("[FM50AF_SUB] No CMD \n");
            i4RetValue = -EPERM;
        break;
    }

    return i4RetValue;
}

//Main jobs:
// 1.check for device-specified errors, device not ready.
// 2.Initialize the device if it is opened for the first time.
// 3.Update f_op pointer.
// 4.Fill data structures into private_data
//CAM_RESET
static int FM50AF_SUB_Open(struct inode * a_pstInode, struct file * a_pstFile)
{
    FM50AF_SUBDB("[FM50AF_SUB] FM50AF_SUB_Open - Start\n");


    if(g_s4FM50AF_SUB_Opened)
    {
        FM50AF_SUBDB("[FM50AF_SUB] the device is opened \n");
        return -EBUSY;
    }
	
    spin_lock(&g_FM50AF_SUB_SpinLock);
    g_s4FM50AF_SUB_Opened = 1;
    spin_unlock(&g_FM50AF_SUB_SpinLock);

    FM50AF_SUBDB("[FM50AF_SUB] FM50AF_SUB_Open - End\n");

    return 0;
}

//Main jobs:
// 1.Deallocate anything that "open" allocated in private_data.
// 2.Shut down the device on last close.
// 3.Only called once on last time.
// Q1 : Try release multiple times.
static int FM50AF_SUB_Release(struct inode * a_pstInode, struct file * a_pstFile)
{
    FM50AF_SUBDB("[FM50AF_SUB] FM50AF_SUB_Release - Start\n");

    if (g_s4FM50AF_SUB_Opened)
    {
        FM50AF_SUBDB("[FM50AF_SUB] feee \n");
        g_sr = 5;
	    s4FM50AF_SUB_WriteReg(200);
        msleep(10);
	    s4FM50AF_SUB_WriteReg(100);
        msleep(10);
            	            	    	    
        spin_lock(&g_FM50AF_SUB_SpinLock);
        g_s4FM50AF_SUB_Opened = 0;
        spin_unlock(&g_FM50AF_SUB_SpinLock);

    }

    FM50AF_SUBDB("[FM50AF_SUB] FM50AF_SUB_Release - End\n");

    return 0;
}

static const struct file_operations g_stFM50AF_SUB_fops = 
{
    .owner = THIS_MODULE,
    .open = FM50AF_SUB_Open,
    .release = FM50AF_SUB_Release,
    .unlocked_ioctl = FM50AF_SUB_Ioctl
};

inline static int Register_FM50AF_SUB_CharDrv(void)
{
    struct device* vcm_device = NULL;

    FM50AF_SUBDB("[FM50AF_SUB] Register_FM50AF_SUB_CharDrv - Start\n");

    //Allocate char driver no.
    if( alloc_chrdev_region(&g_FM50AF_SUB_devno, 0, 1,FM50AF_SUB_DRVNAME) )
    {
        FM50AF_SUBDB("[FM50AF_SUB] Allocate device no failed\n");

        return -EAGAIN;
    }

    //Allocate driver
    g_pFM50AF_SUB_CharDrv = cdev_alloc();

    if(NULL == g_pFM50AF_SUB_CharDrv)
    {
        unregister_chrdev_region(g_FM50AF_SUB_devno, 1);

        FM50AF_SUBDB("[FM50AF_SUB] Allocate mem for kobject failed\n");

        return -ENOMEM;
    }

    //Attatch file operation.
    cdev_init(g_pFM50AF_SUB_CharDrv, &g_stFM50AF_SUB_fops);

    g_pFM50AF_SUB_CharDrv->owner = THIS_MODULE;

    //Add to system
    if(cdev_add(g_pFM50AF_SUB_CharDrv, g_FM50AF_SUB_devno, 1))
    {
        FM50AF_SUBDB("[FM50AF_SUB] Attatch file operation failed\n");

        unregister_chrdev_region(g_FM50AF_SUB_devno, 1);

        return -EAGAIN;
    }

    actuator_class = class_create(THIS_MODULE, "actuatordrv0");
    if (IS_ERR(actuator_class)) {
        int ret = PTR_ERR(actuator_class);
        FM50AF_SUBDB("Unable to create class, err = %d\n", ret);
        return ret;            
    }

    vcm_device = device_create(actuator_class, NULL, g_FM50AF_SUB_devno, NULL, FM50AF_SUB_DRVNAME);

    if(NULL == vcm_device)
    {
        return -EIO;
    }
    
    FM50AF_SUBDB("[FM50AF_SUB] Register_FM50AF_SUB_CharDrv - End\n");    
    return 0;
}

inline static void Unregister_FM50AF_SUB_CharDrv(void)
{
    FM50AF_SUBDB("[FM50AF_SUB] Unregister_FM50AF_SUB_CharDrv - Start\n");

    //Release char driver
    cdev_del(g_pFM50AF_SUB_CharDrv);

    unregister_chrdev_region(g_FM50AF_SUB_devno, 1);
    
    device_destroy(actuator_class, g_FM50AF_SUB_devno);

    class_destroy(actuator_class);

    FM50AF_SUBDB("[FM50AF_SUB] Unregister_FM50AF_SUB_CharDrv - End\n");    
}

//////////////////////////////////////////////////////////////////////

static int FM50AF_SUB_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id);
static int FM50AF_SUB_i2c_remove(struct i2c_client *client);
static const struct i2c_device_id FM50AF_SUB_i2c_id[] = {{FM50AF_SUB_DRVNAME,0},{}};   
struct i2c_driver FM50AF_SUB_i2c_driver = {                       
    .probe = FM50AF_SUB_i2c_probe,                                   
    .remove = FM50AF_SUB_i2c_remove,                           
    .driver.name = FM50AF_SUB_DRVNAME,                 
    .id_table = FM50AF_SUB_i2c_id,                             
};  

#if 0 
static int FM50AF_SUB_i2c_detect(struct i2c_client *client, int kind, struct i2c_board_info *info) {         
    strcpy(info->type, FM50AF_SUB_DRVNAME);                                                         
    return 0;                                                                                       
}      
#endif 
static int FM50AF_SUB_i2c_remove(struct i2c_client *client) {
    return 0;
}

/* Kirby: add new-style driver {*/
static int FM50AF_SUB_i2c_probe(struct i2c_client *client, const struct i2c_device_id *id)
{
    int i4RetValue = 0;

    FM50AF_SUBDB("[FM50AF_SUB] FM50AF_SUB_i2c_probe\n");

    /* Kirby: add new-style driver { */
    g_pstFM50AF_SUB_I2Cclient = client;
    
    g_pstFM50AF_SUB_I2Cclient->addr = g_pstFM50AF_SUB_I2Cclient->addr >> 1;
    
    //Register char driver
    i4RetValue = Register_FM50AF_SUB_CharDrv();

    if(i4RetValue){

        FM50AF_SUBDB("[FM50AF_SUB] register char device failed!\n");

        return i4RetValue;
    }

    spin_lock_init(&g_FM50AF_SUB_SpinLock);

    FM50AF_SUBDB("[FM50AF_SUB] Attached!! \n");

    return 0;
}

static int FM50AF_SUB_probe(struct platform_device *pdev)
{
    return i2c_add_driver(&FM50AF_SUB_i2c_driver);
}

static int FM50AF_SUB_remove(struct platform_device *pdev)
{
    i2c_del_driver(&FM50AF_SUB_i2c_driver);
    return 0;
}

static int FM50AF_SUB_suspend(struct platform_device *pdev, pm_message_t mesg)
{
    return 0;
}

static int FM50AF_SUB_resume(struct platform_device *pdev)
{
    return 0;
}

// platform structure
static struct platform_driver g_stFM50AF_SUB_Driver = {
    .probe		= FM50AF_SUB_probe,
    .remove	= FM50AF_SUB_remove,
    .suspend	= FM50AF_SUB_suspend,
    .resume	= FM50AF_SUB_resume,
    .driver		= {
        .name	= "lens_actuator0",
        .owner	= THIS_MODULE,
    }
};

static int __init FM50AF_SUB_i2C_init(void)
{
    i2c_register_board_info(LENS_I2C_BUSNUM, &kd_lens_dev, 1);
	
    if(platform_driver_register(&g_stFM50AF_SUB_Driver)){
        FM50AF_SUBDB("failed to register FM50AF_SUB driver\n");
        return -ENODEV;
    }

    return 0;
}

static void __exit FM50AF_SUB_i2C_exit(void)
{
	platform_driver_unregister(&g_stFM50AF_SUB_Driver);
}

module_init(FM50AF_SUB_i2C_init);
module_exit(FM50AF_SUB_i2C_exit);

MODULE_DESCRIPTION("FM50AF_SUB lens module driver");
MODULE_AUTHOR("KY Chen <ky.chen@Mediatek.com>");
MODULE_LICENSE("GPL");


