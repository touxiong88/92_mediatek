/*************************************************************************************************
imx135_otp-eeprom.c
---------------------------------------------------------
OTP Application file From Truly for imx135
2013.11.26_wgs
---------------------------------------------------------
NOTE:
The modification is appended to initialization of image sensor. 
After sensor initialization, use the function , and get the id value.
bool otp_wb_update()
and
bool otp_lenc_update(), 
then the calibration of AWB and LSC will be applied. 
After finishing the OTP written, we will provide you the golden_rg and golden_bg settings.
**************************************************************************************************/

#include <linux/videodev2.h>
#include <linux/i2c.h>
#include <linux/platform_device.h>
#include <linux/delay.h>  
#include <linux/cdev.h>
#include <linux/uaccess.h>
#include <linux/fs.h>
#include <asm/atomic.h>
#include <linux/slab.h>


#include "kd_camera_hw.h"
#include "kd_imgsensor.h"
#include "kd_imgsensor_define.h"
#include "kd_imgsensor_errcode.h"

#include "imx135mipiraw_Sensor.h"
#include "imx135mipiraw_Camera_Sensor_para.h"
#include "imx135mipiraw_CameraCustomized.h"

//#include "imx135_otp.h"

extern int iWriteReg(u16 a_u2Addr , u32 a_u4Data , u32 a_u4Bytes , u16 i2cId);

kal_uint16 IMX135MIPI_read_cmos_sensor(kal_uint32 addr);



#define imx135_wordwrite_cmos_sensor(addr, para) iWriteReg((u16) addr , (u32) para , 2, IMX135MIPI_WRITE_ID)
#define imx135_bytewrite_cmos_sensor(addr, para) iWriteReg((u16) addr , (u32) para , 1, IMX135MIPI_WRITE_ID)




//#define unsigned short             unsigned short
//#define BYTE               unsigned char
#define Sleep(ms) mdelay(ms)

#define O_FILM_ID           0x07

enum LENS
{
	LARGEN_LENS = 1,
	KT_LENS,
	KM_LENS,
	GENIUS_LENS,
	SUNNY_LENS,
	OTHER_LENS,
};
enum DRIVER_IC
{
	DONGWOOK = 1,
	ADI,
	ASM,
	ROHM,
	OTHER_DRIVER,
};
enum VCM
{
	TDK = 1,
	MISTUMIS,
	SIKAO,
	MWT,
	ALPS,
	OTHER_VCM,
};
#define VALID_OTP          0x01

#define GAIN_DEFAULT       0x0100
#define GAIN_GREEN1_ADDR   0x020E
#define GAIN_BLUE_ADDR     0x0212
#define GAIN_RED_ADDR      0x0210
#define GAIN_GREEN2_ADDR   0x0214

unsigned short golden_r_Gr;
unsigned short golden_Gb_Gr;
unsigned short golden_b_Gr;

unsigned short current_r_Gr;
unsigned short current_Gb_Gr;
unsigned short current_b_Gr;


kal_uint32 r_ratio;
kal_uint32 b_ratio;


//kal_uint32	golden_r = 0, golden_gr = 0, golden_gb = 0, golden_b = 0;
//kal_uint32	current_r = 0, current_gr = 0, current_gb = 0, current_b = 0;
/*************************************************************************************************
* Function    :  get_wb_id_flag
* Description :  get wb_id otp WRITTEN_FLAG  
* Return      :  [unsigned char], if 0x01 , this type has valid otp data, otherwise, invalid otp data
**************************************************************************************************/
unsigned char get_wb_id_flag()
{
	unsigned char temp = 0;
	unsigned char validflag = 0;
	unsigned char flag = 0;
	
	imx135_bytewrite_cmos_sensor(0x3b02, 0x00);  //
	imx135_bytewrite_cmos_sensor(0x3b00, 0x01);  //
	temp = IMX135MIPI_read_cmos_sensor(0x3b34);   //
	printk("----o-film get_wb_id_flag() 0x3b34 temp=%x\n",temp);
	
	validflag =(temp>>6);
	if(validflag==0x01)
		{
			flag =validflag;
			return flag;
		}
		
	temp = IMX135MIPI_read_cmos_sensor(0x3b3B);   //
	printk("----o-film get_wb_id_flag() 0x3b3B temp=%x\n",temp);
	
	validflag =(temp>>6);
	if(validflag==0x01)
		{
			flag =validflag;
			return flag;
		}
		
	flag =validflag;
	printk("WB error:0x%02x",flag);
	return flag;
}



/*************************************************************************************************
* Function    :  get_otp_module_id
* Description :  get otp MID value 
* Return      :  [unsigned char] 0 : OTP data fail 
                 other value : module ID data , TRULY ID is 0x0002            
**************************************************************************************************/
unsigned char get_otp_module_id()
{
	
	unsigned char module_id = 0;
	unsigned char temp = 0;
	unsigned char flag = 0;
	
	//imx135_bytewrite_cmos_sensor(0x3b02, 0x00);  //3b02里写0x00
	//imx135_bytewrite_cmos_sensor(0x3b00, 0x01);  //3b00里写0x01
	temp = IMX135MIPI_read_cmos_sensor(0x3b34);  //读3b34的bit 5~bit 0   O-Film =0x07
	printk("----o-film group1 get_otp_module_id() temp=%x\n",temp);

	flag=(temp>>6);
	module_id =(temp&0x3f);
	if(flag==0x01)
		{
			return module_id;
		}
		
	temp = IMX135MIPI_read_cmos_sensor(0x3b3B);   //
	printk("----o-film group2 get_otp_module_id() temp=%x\n",temp);
	
	flag =(temp>>6);
	module_id =(temp&0x3f);
	if(flag==0x01)
		{
			return module_id;
		}
		
	module_id=0;
	printk("OTP_Module ID eeror: 0x%02x.\n",module_id);
	return module_id;
}







/*************************************************************************************************
* Function    :  get_lsc_flag
* Description :  get LSC WRITTEN_FLAG  
* Return      :  [unsigned char], if 0x01 , this type has valid lsc data, otherwise, invalid otp data
**************************************************************************************************/
unsigned char get_lsc_flag()
{
	unsigned char flag = 0;
	
	imx135_bytewrite_cmos_sensor(0x3b02, 0x01);  //3b02 write page
	imx135_bytewrite_cmos_sensor(0x3b00, 0x01);  //3b00里写0x01
	flag= IMX135MIPI_read_cmos_sensor(0x3b04);  //读3b34的bit 5~bit 0   O-Film =0x07
	printk("-----o-film get_lsc_flag() flag=%x\n",flag);
	
	flag = ((flag >> 6) & 0x03);
	
	printk("LSC Flag:0x%02x",flag );
	return flag;
}

/*************************************************************************************************
* Function    :  otp_lenc_update
* Description :  Update lens correction 
* Return      :  [bool] 0 : OTP data fail 
                        1 : otp_lenc update success            
**************************************************************************************************/
bool otp_lenc_update()
{
	unsigned char lsc_flag = 0;
	unsigned int  i =0;
	int j = 0,k=0,n=0,temp=0,index =0;

	unsigned char lsc_golden_data[504]={0x02,0x04,0x01,0xc1,0x01,0xe3,0x01,0xa5,0x01,0xc2,0x01,0x88,0x01,0x9a,0x01,0x72,0x01,0x8a,0x01,0x69,0x01,0x95,0x01,0x6e,0x01,0xb4,0x01,0x80,0x01,0xd0,0x01,
0x8e,0x01,0xed,0x01,0xa7,0x01,0xf3,0x01,0xa7,0x01,0xc1,0x01,0x87,0x01,0x84,0x01,0x5d,0x01,0x58,0x01,0x3f,0x01,0x45,0x01,0x34,0x01,0x51,0x01,0x3e,0x01,0x7b,0x01,
0x59,0x01,0xad,0x01,0x75,0x01,0xd5,0x01,0x8d,0x01,0xe1,0x01,0x9c,0x01,0xa2,0x01,0x71,0x01,0x5b,0x01,0x41,0x01,0x23,0x01,0x18,0x01,0x10,0x01,0x0c,0x01,0x20,0x01,
0x18,0x01,0x4f,0x01,0x39,0x01,0x8d,0x01,0x60,0x01,0xc1,0x01,0x83,0x01,0xd7,0x01,0x9a,0x01,0x95,0x01,0x6a,0x01,0x49,0x01,0x34,0x01,0x12,0x01,0x0c,0x01,0x00,0x01,
0x00,0x01,0x0e,0x01,0x0a,0x01,0x3e,0x01,0x2d,0x01,0x7e,0x01,0x59,0x01,0xb7,0x01,0x7d,0x01,0xe1,0x01,0x9c,0x01,0x9f,0x01,0x6e,0x01,0x5a,0x01,0x41,0x01,0x24,0x01,
0x1a,0x01,0x10,0x01,0x0b,0x01,0x1e,0x01,0x15,0x01,0x4f,0x01,0x38,0x01,0x88,0x01,0x5e,0x01,0xc1,0x01,0x82,0x01,0xeb,0x01,0xa5,0x01,0xc1,0x01,0x84,0x01,0x85,0x01,
0x5c,0x01,0x57,0x01,0x3e,0x01,0x43,0x01,0x32,0x01,0x50,0x01,0x3a,0x01,0x77,0x01,0x53,0x01,0xa7,0x01,0x71,0x01,0xcc,0x01,0x88,0x01,0xfa,0x01,0xb6,0x01,0xe0,0x01,
0x9c,0x01,0xba,0x01,0x85,0x01,0x94,0x01,0x6e,0x01,0x87,0x01,0x64,0x01,0x8e,0x01,0x68,0x01,0xae,0x01,0x77,0x01,0xc5,0x01,0x89,0x01,0xe0,0x01,0x9a,0x01,0xc1,0x01,
0xb1,0x01,0xa1,0x01,0x95,0x01,0x84,0x01,0x7d,0x01,0x6e,0x01,0x67,0x01,0x62,0x01,0x5c,0x01,0x69,0x01,0x61,0x01,0x79,0x01,0x70,0x01,0x8f,0x01,0x7f,0x01,0xa7,0x01,
0x93,0x01,0xab,0x01,0x96,0x01,0x88,0x01,0x78,0x01,0x5d,0x01,0x54,0x01,0x3e,0x01,0x38,0x01,0x30,0x01,0x2e,0x01,0x3b,0x01,0x35,0x01,0x57,0x01,0x4b,0x01,0x76,0x01,
0x64,0x01,0x93,0x01,0x7d,0x01,0xa2,0x01,0x90,0x01,0x73,0x01,0x65,0x01,0x41,0x01,0x3b,0x01,0x18,0x01,0x16,0x01,0x0b,0x01,0x0a,0x01,0x17,0x01,0x15,0x01,0x3a,0x01,
0x31,0x01,0x65,0x01,0x54,0x01,0x87,0x01,0x70,0x01,0xa0,0x01,0x91,0x01,0x6d,0x01,0x60,0x01,0x37,0x01,0x31,0x01,0x0e,0x01,0x0b,0x01,0x00,0x01,0x00,0x01,0x09,0x01,
0x08,0x01,0x30,0x01,0x27,0x01,0x5e,0x01,0x4d,0x01,0x84,0x01,0x6e,0x01,0xa4,0x01,0x90,0x01,0x75,0x01,0x69,0x01,0x44,0x01,0x3d,0x01,0x1b,0x01,0x1a,0x01,0x0d,0x01,
0x0b,0x01,0x18,0x01,0x14,0x01,0x3c,0x01,0x31,0x01,0x63,0x01,0x50,0x01,0x8a,0x01,0x74,0x01,0xad,0x01,0x98,0x01,0x88,0x01,0x7b,0x01,0x60,0x01,0x54,0x01,0x3f,0x01,
0x38,0x01,0x34,0x01,0x2c,0x01,0x3c,0x01,0x33,0x01,0x56,0x01,0x49,0x01,0x75,0x01,0x63,0x01,0x8f,0x01,0x79,0x01,0xbc,0x01,0xa6,0x01,0xa3,0x01,0x94,0x01,0x85,0x01,
0x79,0x01,0x70,0x01,0x62,0x01,0x66,0x01,0x58,0x01,0x69,0x01,0x5c,0x01,0x7a,0x01,0x6a,0x01,0x8e,0x01,0x78,0x01,0xa4,0x01,0x8a,}; //o-film 给出
	
	unsigned char lsc_data[505] = {0};
	lsc_flag = get_lsc_flag();
	if(lsc_flag != VALID_OTP)
	{
		printk("No LSC data or LSC data is invalid!!!\n");
		for(k=0;k<504;k++)
		{
			lsc_data[k+1]=lsc_golden_data[k];
		}
		//return 0;
	}
	else{
			//printk("******please notice here why read 0x03 while(253)*****\n");
			for(k = 0x01; k < 0x09; k++ )
			{   
				imx135_bytewrite_cmos_sensor(0x3b02, k);   // IMX135MIPI_WRITE_ID    ? 0x21  chose page 
				imx135_bytewrite_cmos_sensor(0x3b00, 0x01);
		
				do
				{ 	
				   temp = IMX135MIPI_read_cmos_sensor( 0x3b01);
				   msleep(10);
				   n++; 
				}while( (n < 30) && ( (temp&0x01) == 0 ) );
				
				if(k<8)
					{
						for (j=0; j<64; j++)
							{ 
								index =(k-1)*64+j;
								lsc_data[index]=IMX135MIPI_read_cmos_sensor(0x3b04+j);  
							} 
						
					}
					else
						{
							for (j=0; j<57; j++)
							{ 
								index =(k-1)*64+j;
								lsc_data[index]=IMX135MIPI_read_cmos_sensor(0x3b04+j);  
							} 
						}
			}
			
		}
	
	for(i=0;i<504;i++) //LSC SIZE is 504 BYTES
	{
		imx135_bytewrite_cmos_sensor(0x4800+i, lsc_data[i+1]);  //rlk_zhanxw_read_sensor
	}

	imx135_bytewrite_cmos_sensor(0x4500, 0x1F);
	imx135_bytewrite_cmos_sensor(0x0700, 0x01);
	imx135_bytewrite_cmos_sensor(0x3A63, 0x01);

	printk("Update lsc finished\n");
	
	return 1;
}

/*************************************************************************************************
* Function    :  wb_gain_set
* Description :  Set WB ratio to register gain setting  512x
* Parameters  :  [int] r_ratio : R ratio data compared with golden module R
                       b_ratio : B ratio data compared with golden module B
* Return      :  [bool] 0 : set wb fail 
                        1 : WB set success            
**************************************************************************************************/

bool wb_gain_set()
{
	unsigned short R_GAIN;
	unsigned short B_GAIN;
	unsigned short Gr_GAIN;
	unsigned short Gb_GAIN;
	unsigned short G_GAIN;
		
	if(!r_ratio || !b_ratio)
	{
		printk("OTP WB ratio Data Err!\n");
		return 0;
	}

	if(r_ratio >= 512 )
	{
		if(b_ratio>=512) 
		{
			R_GAIN = (unsigned short)(GAIN_DEFAULT * r_ratio / 512);						
			G_GAIN = GAIN_DEFAULT;
			B_GAIN = (unsigned short)(GAIN_DEFAULT * b_ratio / 512);
		}
		else
		{
			R_GAIN =  (unsigned short)(GAIN_DEFAULT*r_ratio / b_ratio );
			G_GAIN = (unsigned short)(GAIN_DEFAULT*512 / b_ratio );
			B_GAIN = GAIN_DEFAULT; 
		}
	}
	else                      
	{
		if(b_ratio >= 512)
		{
			R_GAIN = GAIN_DEFAULT;
			G_GAIN = (unsigned short)(GAIN_DEFAULT*512 /r_ratio);		
			B_GAIN =  (unsigned short)(GAIN_DEFAULT*b_ratio / r_ratio );
		} 
		else 
		{
			Gr_GAIN = (unsigned short)(GAIN_DEFAULT*512/ r_ratio );						
			Gb_GAIN = (unsigned short)(GAIN_DEFAULT*512/b_ratio );						
			if(Gr_GAIN >= Gb_GAIN)						
			{						
				R_GAIN = GAIN_DEFAULT;						
				G_GAIN = (unsigned short)(GAIN_DEFAULT *512/ r_ratio );						
				B_GAIN =  (unsigned short)(GAIN_DEFAULT*b_ratio / r_ratio );						

			} 
			else
			{						
				R_GAIN =  (unsigned short)(GAIN_DEFAULT*r_ratio  / b_ratio);						
				G_GAIN = (unsigned short)(GAIN_DEFAULT*512 / b_ratio );						
				B_GAIN = GAIN_DEFAULT;
			}
		}        
	}

	//printk("OTP_golden_r=%d,golden_gr=%d,golden_gb=%d,golden_b=%d \n",golden_r,golden_gr,golden_gb,golden_b);
	//printk("OTP_current_r=%d,current_gr=%d,current_gb=%d,current_b=%d \n",current_r,current_gr,current_gb,current_b);
	//printk("OTP_r_ratio=%d,b_ratio=%d \n",r_ratio,b_ratio);

	imx135_wordwrite_cmos_sensor(GAIN_RED_ADDR, R_GAIN);		
	imx135_wordwrite_cmos_sensor(GAIN_BLUE_ADDR, B_GAIN);     
	imx135_wordwrite_cmos_sensor(GAIN_GREEN1_ADDR, G_GAIN); //Green 1 default gain 1x		
	imx135_wordwrite_cmos_sensor(GAIN_GREEN2_ADDR, G_GAIN); //Green 2 default gain 1x

	printk("OTP WB Update Finished! \n");
	return 1;
}

/*************************************************************************************************
* Function    :  get_otp_wb
* Description :  Get WB data        
Typical WB module number：25、26
Corner WB module number：14、8
Shading Typical:34、28
Shading CornerTotal:02、05 
**************************************************************************************************/
bool get_otp_wb()
{
	unsigned char temph = 0;
	unsigned char templ = 0;
	unsigned char flag = 0;
	golden_r_Gr = 619, golden_b_Gr = 679, golden_Gb_Gr = 1026;
	current_r_Gr = 0, current_b_Gr = 0, current_Gb_Gr = 0;
	
	
	flag = (0x03&(IMX135MIPI_read_cmos_sensor(0x3b34)>>6)); 
	printk("----ofilm get_otp_wb() group1 flag=%x\n",flag);
	
	if(flag = 0x10)
	{
		temph = IMX135MIPI_read_cmos_sensor(0x3b35);  
		templ = IMX135MIPI_read_cmos_sensor(0x3b36); 
		current_r_Gr  = (unsigned short)templ + (((unsigned short)temph)  << 8);
		printk("---o-film group1 get_otp_wb() current_r_Gr=%x\n",current_r_Gr);
	
		temph = IMX135MIPI_read_cmos_sensor(0x3b37);  
		templ = IMX135MIPI_read_cmos_sensor(0x3b38); 
		current_b_Gr  = (unsigned short)templ + (((unsigned short)temph ) << 8);
		printk("---o-film group1 get_otp_wb() current_b_Gr=%x\n",current_b_Gr);
	
		temph = IMX135MIPI_read_cmos_sensor(0x3b39);  
		templ = IMX135MIPI_read_cmos_sensor(0x3b3a); 
		current_Gb_Gr  = (unsigned short)templ + (((unsigned short)temph ) << 8);	
		printk("---o-film group1 get_otp_wb() current_Gb_Gr=%x\n",current_Gb_Gr);
		
		return 1;
	}
	
	flag = 0;
	flag =(0x03&(IMX135MIPI_read_cmos_sensor(0x3b3B)>>6)); 
	printk("----ofilm get_otp_wb() group2 flag=%x\n",flag);
	
	if(flag = 0x10)
	{
		temph = IMX135MIPI_read_cmos_sensor(0x3b3c);  
		templ = IMX135MIPI_read_cmos_sensor(0x3b3d); 
		current_r_Gr  = (unsigned short)templ + (((unsigned short)temph)  << 8);
		printk("---o-film group2 get_otp_wb() current_r_Gr=%x\n",current_r_Gr);
	
		temph = IMX135MIPI_read_cmos_sensor(0x3b3e);  
		templ = IMX135MIPI_read_cmos_sensor(0x3b3f); 
		current_b_Gr  = (unsigned short)templ + (((unsigned short)temph ) << 8);
		printk("---o-film group2 get_otp_wb() current_b_Gr=%x\n",current_b_Gr);
	
		temph = IMX135MIPI_read_cmos_sensor(0x3b40);  
		templ = IMX135MIPI_read_cmos_sensor(0x3b41); 
		current_Gb_Gr  = (unsigned short)templ + (((unsigned short)temph ) << 8);	
		printk("---o-film group2 get_otp_wb() current_Gb_Gr=%x\n",current_Gb_Gr);
		
		return 1;
	}

	current_r_Gr =golden_r_Gr;
	current_b_Gr =golden_b_Gr;
	current_Gb_Gr=golden_Gb_Gr;
	
	return 1;
}


/*************************************************************************************************
* Function    :  otp_wb_update
* Description :  Update WB correction 
* Return      :  [bool] 0 : OTP data fail 
                        1 : otp_WB update success            
**************************************************************************************************/
bool otp_wb_update()
{
	//unsigned short golden_g_Gr;
	//unsigned short current_g_Gr;
	unsigned short awb_rgb_gain[3] ={0,0,0};
	unsigned short R_Gain=0,B_Gain=0,Gr_Gain=0,Gb_Gain=0;
	
	unsigned short i=0,minindex =0 ,min =0;


	if(!get_otp_wb())  // get wb data from otp
	{
		printk("Get OTP WB data Err!\n");
		return 0;
	}

	awb_rgb_gain[0] = golden_r_Gr*1024/current_r_Gr;
	awb_rgb_gain[1] = golden_b_Gr*1024/current_b_Gr;
	awb_rgb_gain[2] = golden_Gb_Gr*1024/current_Gb_Gr;
	
	minindex =0;
	minindex =awb_rgb_gain[0];
	for(i=0;i<3;i++)
	{
		if(min <awb_rgb_gain[i])
			{
				min =awb_rgb_gain[i];
				minindex =i;	
			}	
	}

	if(min<1024)
		{
			switch(minindex)
			{
				case 0://R gain 最少
					R_Gain =0x0100;	
					Gr_Gain =R_Gain*current_r_Gr/golden_r_Gr;
					B_Gain =Gr_Gain*golden_b_Gr/current_b_Gr;
					Gb_Gain =Gr_Gain*golden_Gb_Gr/current_Gb_Gr;
					break;
				case 1://B gain 最少
					B_Gain =0x0100;	
					Gr_Gain =B_Gain*current_b_Gr/golden_b_Gr;
					R_Gain =Gr_Gain*golden_r_Gr/current_r_Gr;
					Gb_Gain =Gr_Gain*golden_Gb_Gr/current_Gb_Gr;
					
					break;
				case 2://Gb gain 最少
					Gb_Gain =0x0100;	
					Gr_Gain =Gb_Gain*current_Gb_Gr/golden_Gb_Gr;
					R_Gain =Gr_Gain*golden_r_Gr/current_r_Gr;
					B_Gain =Gr_Gain*golden_b_Gr/current_b_Gr;
					
					break;
			}	
		}else
			{// Gr gain 最少
					Gr_Gain =0x0100;	
					Gb_Gain =Gr_Gain*golden_Gb_Gr/current_Gb_Gr;
					R_Gain =Gr_Gain*golden_r_Gr/current_r_Gr;
					B_Gain =Gr_Gain*golden_b_Gr/current_b_Gr;
				
			}

	imx135_wordwrite_cmos_sensor(GAIN_RED_ADDR, R_Gain);		
	imx135_wordwrite_cmos_sensor(GAIN_BLUE_ADDR, B_Gain);     
	imx135_wordwrite_cmos_sensor(GAIN_GREEN1_ADDR, Gr_Gain); 	
	imx135_wordwrite_cmos_sensor(GAIN_GREEN2_ADDR, Gb_Gain); 

	return 1;
}

/*************************************************************************************************
* Function    :  otp_update()
* Description :  update otp data from otp , it otp data is valid, 
                 it include get ID and WB update function  
* Return      :  [bool] 0 : update fail
                        1 : update success
**************************************************************************************************/
kal_bool otp_update()
{

	unsigned char FLG  = 0x00;
	unsigned char MID = 0x00;
	
		FLG = get_wb_id_flag();
		if(FLG != VALID_OTP)
		{
			printk(" No WB data or WB data is invalid!!!\n");
			return KAL_FALSE;
		}
		
		MID =   get_otp_module_id();
		if(MID != O_FILM_ID) //Select 
		{
			printk("No Truly Module !!!!\n");
			return KAL_FALSE;
		}
		
		if (!otp_wb_update())
		{
				printk("Update wb Err\n");
				return KAL_FALSE;
		}
	
	if(!otp_lenc_update())
	{
		printk("Update LSC Err\n");
		return KAL_FALSE;
	}

	return KAL_TRUE;	
}
