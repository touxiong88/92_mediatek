#ifdef BUILD_LK
#else
#include <linux/string.h>
#if defined(BUILD_UBOOT)
#include <asm/arch/mt_gpio.h>
#else
#include <mach/mt_gpio.h>
#endif
#endif
#include "lcm_drv.h"

#ifdef BUILD_LK
	#include <platform/mt_gpio.h>
	#include <platform/mt_pmic.h>
#elif defined(BUILD_UBOOT)
	#include <asm/arch/mt_gpio.h>
#else
	#include <mach/mt_gpio.h>
	#include <linux/xlog.h>
	#include <mach/mt_pm_ldo.h>
#endif

// ---------------------------------------------------------------------------
//  Local Constants
// ---------------------------------------------------------------------------

#define FRAME_WIDTH  (720)
#define FRAME_HEIGHT (1280)

#define LCM_ID  (0x21)

#ifndef TRUE
    #define TRUE 1
#endif

#ifndef FALSE
    #define FALSE 0
#endif
#define GPIO_LCD_RST_EN      (GPIO112 | 0x80000000)

static unsigned int lcm_esd_test = FALSE;      ///only for ESD test

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))
#define REGFLAG_DELAY             							0xAB
#define REGFLAG_END_OF_TABLE      							0x100 //AA   // END OF REGISTERS MARKER

struct LCM_setting_table {
    unsigned char cmd;
    unsigned char count;
    unsigned char para_list[64];
};

// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	        lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)										lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)					lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg											lcm_util.dsi_read_reg()
#define read_reg_v2(cmd, buffer, buffer_size)				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)

#define   LCM_DSI_CMD_MODE							0

static void push_table(struct LCM_setting_table *table, unsigned int count, unsigned char force_update)
{
        unsigned int i;

    for(i = 0; i < count; i++) {
                
        unsigned cmd;
        cmd = table[i].cmd;
                
        switch (cmd) {
                        
            case REGFLAG_DELAY :
                MDELAY(table[i].count);
                break;
                                
            case REGFLAG_END_OF_TABLE :
                break;
                                
            default:
                                dsi_set_cmdq_V2(cmd, table[i].count, table[i].para_list, force_update);
			break;
        }
    }
        
}


// ---------------------------------------------------------------------------
//  LCM Driver Implementations
// ---------------------------------------------------------------------------

static void lcm_set_util_funcs(const LCM_UTIL_FUNCS *util)
{
    memcpy(&lcm_util, util, sizeof(LCM_UTIL_FUNCS));
}


static void lcm_get_params(LCM_PARAMS *params)
{
	memset(params, 0, sizeof(LCM_PARAMS));

	params->type   = LCM_TYPE_DSI;

	params->width  = FRAME_WIDTH;
	params->height = FRAME_HEIGHT;

	params->dbi.te_mode                     = LCM_DBI_TE_MODE_VSYNC_ONLY;
	params->dbi.te_edge_polarity            = LCM_POLARITY_RISING;

	params->dsi.mode   = BURST_VDO_MODE;

	// DSI
	/* Command mode setting */
	//1 Three lane or Four lane
	params->dsi.LANE_NUM				= LCM_FOUR_LANE;
	//The following defined the fomat for data coming from LCD engine.
	params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
	params->dsi.data_format.trans_seq	= LCM_DSI_TRANS_SEQ_MSB_FIRST;
	params->dsi.data_format.padding 	= LCM_DSI_PADDING_ON_LSB;
	params->dsi.data_format.format	  = LCM_DSI_FORMAT_RGB888;

	// Video mode setting
	params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

	params->dsi.vertical_sync_active				= 2;// 3    2
	params->dsi.vertical_backporch					= 10;// 20   1
	params->dsi.vertical_frontporch					= 20; // 1  12
	params->dsi.vertical_active_line				= FRAME_HEIGHT;

	params->dsi.horizontal_sync_active				= 60;// 50  2
	params->dsi.horizontal_backporch				= 100;
	params->dsi.horizontal_frontporch				= 100;
	params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

    //params->dsi.LPX=8; 

	// Bit rate calculation
	//1 Every lane speed
	params->dsi.pll_div1=0;		// div1=0,1,2,3;div1_real=1,2,4,4 ----0: 546Mbps  1:273Mbps
	params->dsi.pll_div2=1;		// div2=0,1,2,3;div1_real=1,2,4,4
	params->dsi.fbk_div =17;    // fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)
}

static struct LCM_setting_table lcm_initialization_setting[] = {


 {0xff,  4,  {0xaa,0x55,0xa5,0x80}},
	{0x6f,  2,  {0x11,0x00}},
	{0xf7,  2,  {0x20,0x00}},
    {0x6f,  1,  {0x11}},
    {0xf3,  1,  {0x01}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x00}},
	{0xbd,  5,  {0x01,0xa0,0x0c,0x08,0x01}},
	{0x6f,  1,  {0x02}},
	{0xb8,  1,  {0x0c}},
	{0xbb,  2,  {0x11,0x11}},
	{0xbc,  2,  {0x00,0x00}},
	{0xb6,  1,  {0x01}},
	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x01}},
	{0xb0,  2,  {0x09,0x09}},
	{0xb1,  2,  {0x09,0x09}},
	{0xbc,  2,  {0x68,0x01}},
	{0xbd,  2,  {0x68,0x01}},
	{0xca,  1,  {0x00}},
	{0xc0,  1,  {0x04}},
	{0xb5,  2,  {0x03,0x03}},
	{0xbe,  1,  {0x68}},//5b
	{0xb3,  2,  {0x28,0x28}},
	{0xb4,  2,  {0x0f,0x0f}},
	{0xb9,  2,  {0x34,0x34}}, //53
	{0xba,  2,  {0x15,0x15}},
	
	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x02}},
	{0xee,  1,  {0x01}},

	{0xb0,  16, {0x00,0x00,0x00,0x58,0x00,0x88,0x00,0xa7,0x00,0xc0,0x00,0xe8,0x01,0x08,0x01,0x3c}},
	{0xb1,  16, {0x01,0x63,0x01,0xa4,0x01,0xd6,0x02,0x26,0x02,0x67,0x02,0x69,0x02,0xa5,0x02,0xe3}},
	{0xb2,  16, {0x03,0x09,0x03,0x3a,0x03,0x5e,0x03,0x8a,0x03,0xa5,0x03,0xc4,0x03,0xd8,0x03,0xed}},
	{0xb3,  4,  {0x03,0xf7,0x03,0xfb}},

	{0x6f,  1,  {0x02}},
	{0xf7,  1,  {0x47}},

	{0x6f,  1,  {0x0a}},
	{0xf7,  1,  {0x02}},

	{0x6f,  1,  {0x17}},
	{0xf4,  1,  {0x70}},

	{0x6f,  1,  {0x11}},
	{0xf3,  1,  {0x01}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x06}},
	{0xb0,  2,  {0x12,0x10}},
	{0xb1,  2,  {0x18,0x16}},
	{0xb2,  2,  {0x00,0x02}},
	{0xb3,  2,  {0x31,0x31}},
	{0xb4,  2,  {0x31,0x31}},
	{0xb5,  2,  {0x31,0x31}},
	{0xb6,  2,  {0x31,0x31}},
	{0xb7,  2,  {0x31,0x31}},
	{0xb8,  2,  {0x31,0x08}},
	{0xb9,  2,  {0x2d,0x2e}},       //0x2e,0x2d
	{0xba,  2,  {0x2e,0x2d}},       //0x2d,0x2e
	{0xbb,  2,  {0x09,0x31}},
	{0xbc,  2,  {0x31,0x31}},
	{0xbd,  2,  {0x31,0x31}},
	{0xbe,  2,  {0x31,0x31}},
	{0xbf,  2,  {0x31,0x31}},
	{0xc0,  2,  {0x31,0x31}},
	{0xc1,  2,  {0x03,0x01}},
	{0xc2,  2,  {0x17,0x19}},
	{0xc3,  2,  {0x11,0x13}},
	{0xe5,  2,  {0x31,0x31}},

	{0xc4,  2,  {0x17,0x19}},
	{0xc5,  2,  {0x11,0x13}},
	{0xc6,  2,  {0x03,0x01}},
	{0xc7,  2,  {0x31,0x31}},
	{0xc8,  2,  {0x31,0x31}},
	{0xc9,  2,  {0x31,0x31}},
	{0xca,  2,  {0x31,0x31}},
	{0xcb,  2,  {0x31,0x31}},
	{0xcc,  2,  {0x31,0x09}},
	{0xcd,  2,  {0x2e,0x2d}},     //0x2d,0x2e
	{0xce,  2,  {0x2d,0x2e}},     //2e,2d 
	{0xcf,  2,  {0x08,0x31}},
	{0xd0,  2,  {0x31,0x31}},
	{0xd1,  2,  {0x31,0x31}},
	{0xd2,  2,  {0x31,0x31}},
	{0xd3,  2,  {0x31,0x31}},
	{0xd4,  2,  {0x31,0x31}},
	{0xd5,  2,  {0x00,0x02}},
	{0xd6,  2,  {0x12,0x10}},
	{0xd7,  2,  {0x18,0x16}},

	{0xd8,  5,  {0x00,0x00,0x00,0x00,0x00}},
	{0xd9,  5,  {0x00,0x00,0x00,0x00,0x00}},
	{0xe7,  1,  {0x00}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
	{0xed,  1,  {0x30}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x03}},
	{0xb1,  2,  {0x00,0x00}},
	{0xb0,  2,  {0x20,0x00}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
	{0xe5,  1,  {0x00}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
	{0xb0,  2,  {0x17,0x06}},
	{0xb8,  1,  {0x00}},

        {0xbd,  5,  {0x03,0x03,0x01,0x00,0x03}},
	{0xb1,  2,  {0x17,0x06}},
	{0xb9,  2,  {0x00,0x03}},
	{0xb2,  2,  {0x17,0x06}},
	{0xba,  2,  {0x00,0x00}},
	{0xb3,  2,  {0x17,0x06}},
	{0xbb,  2,  {0x00,0x00}},
	{0xb4,  2,  {0x17,0x06}},
	{0xb5,  2,  {0x17,0x06}},
	{0xb6,  2,  {0x17,0x06}},
	{0xb7,  2,  {0x17,0x06}},
	{0xbc,  2,  {0x00,0x03}},
	{0xe5,  1,  {0x06}},
	{0xe6,  1,  {0x06}},
	{0xe7,  1,  {0x06}},
	{0xe8,  1,  {0x06}},
	{0xe9,  1,  {0x06}},
	{0xea,  1,  {0x06}},
	{0xeb,  1,  {0x06}},
	{0xec,  1,  {0x06}},
	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
	{0xc0,  1,  {0x0b}},
	{0xc1,  1,  {0x09}},
	{0xc2,  1,  {0x0b}},
	{0xc3,  1,  {0x09}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x03}},
	{0xb2,  5,  {0x05,0x00,0x00,0x00,0x90}},
	{0xb3,  5,  {0x05,0x00,0x00,0x00,0x90}},
	{0xb4,  5,  {0x05,0x00,0x00,0x00,0x90}},
	{0xb5,  5,  {0x05,0x00,0x00,0x00,0x90}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
	{0xc4,  1,  {0x10}},
	{0xc5,  1,  {0x10}},
	{0xc6,  1,  {0x10}},
	{0xc7,  1,  {0x10}},
	
	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x03}},
        {0xb6,  5,  {0x05,0x00,0x00,0x00,0x00}},       //0x05,0x00,0x00,0x00,0x90
        {0xb7,  5,  {0x05,0x00,0x00,0x00,0x00}},
        {0xb8,  5,  {0x05,0x00,0x00,0x00,0x00}},
        {0xb9,  5,  {0x05,0x00,0x00,0x00,0x00}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
	{0xc8,  2,  {0x07,0x20}},
	{0xc9,  2,  {0x03,0x20}},
	{0xca,  2,  {0x07,0x00}},
	{0xcb,  2,  {0x03,0x00}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x03}},
        {0xba,  5,  {0x44,0x00,0x00,0x01,0x20}},    //0x44,0x00,0x00,0x00,0x90
        {0xbb,  5,  {0x44,0x00,0x00,0x01,0x20}},
        {0xbc,  5,  {0x44,0x00,0x00,0x01,0x20}},
        {0xbd,  5,  {0x44,0x00,0x00,0x01,0x20}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
        {0xd1,  5,  {0x00,0x05,0x00,0x07,0x10}},
        {0xd2,  5,  {0x00,0x05,0x04,0x07,0x10}},
        {0xd3,  5,  {0x00,0x00,0x0a,0x07,0x10}},
        {0xd4,  5,  {0x00,0x00,0x0a,0x07,0x10}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x05}},
        {0xd0,  7,  {0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
        {0xd5,  11, {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
        {0xd6,  11, {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
        {0xd7,  11, {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00}},
        {0xd8,  5,  {0x00,0x00,0x00,0x00,0x00}},

	{0xf0,  5,  {0x55,0xaa,0x52,0x08,0x03}},
	{0xc4,  1,  {0x60}},
	{0xc5,  1,  {0x40}},
	{0xc6,  1,  {0x60}},
	{0xc7,  1,  {0x40}},

	{0x6f,  1,  {0x01}},
	{0xf9,  1,  {0x46}},

	{0x11, 1,   {0x00}},
	{REGFLAG_DELAY, 120, {}},

	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
	// Note
	// Strongly recommend not to set Sleep out / Display On here. That will cause messed frame to be shown as later the backlight is on.


	// Setting ending by predefined flag
	
};


static void lcm_init(void)
{
#ifdef BUILD_LK
    upmu_set_rg_vgp2_en(0);
    MDELAY(10);
    upmu_set_rg_vgp2_vosel(6);
    upmu_set_rg_vgp2_en(1);
#else
    hwPowerDown(MT6322_POWER_LDO_VGP2, "LCM");
    MDELAY(10);
    hwPowerOn(MT6322_POWER_LDO_VGP2, VOL_3300, "LCM");
#endif
	mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(50);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(150);
	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_suspend(void)
{
    unsigned int data_array[16];

	data_array[0]=0x00280500; // Display Off
	dsi_set_cmdq(data_array, 1, 1);

	data_array[0] = 0x00100500; // Sleep In
	dsi_set_cmdq(data_array, 1, 1);

	MDELAY(120);
}


static void lcm_resume(void)
{
	lcm_init();
}
         
#if (LCM_DSI_CMD_MODE)
static void lcm_update(unsigned int x, unsigned int y,
                       unsigned int width, unsigned int height)
{
    unsigned int x0 = x;
    unsigned int y0 = y;
    unsigned int x1 = x0 + width - 1;
    unsigned int y1 = y0 + height - 1;

    unsigned char x0_MSB = ((x0>>8)&0xFF);
    unsigned char x0_LSB = (x0&0xFF);
    unsigned char x1_MSB = ((x1>>8)&0xFF);
    unsigned char x1_LSB = (x1&0xFF);
    unsigned char y0_MSB = ((y0>>8)&0xFF);
    unsigned char y0_LSB = (y0&0xFF);
    unsigned char y1_MSB = ((y1>>8)&0xFF);
    unsigned char y1_LSB = (y1&0xFF);

    unsigned int data_array[16];

    data_array[0]= 0x00053902;
    data_array[1]= (x1_MSB<<24)|(x0_LSB<<16)|(x0_MSB<<8)|0x2a;
    data_array[2]= (x1_LSB);
    dsi_set_cmdq(data_array, 3, 1);

    data_array[0]= 0x00053902;
    data_array[1]= (y1_MSB<<24)|(y0_LSB<<16)|(y0_MSB<<8)|0x2b;
    data_array[2]= (y1_LSB);
    dsi_set_cmdq(data_array, 3, 1);

    data_array[0]= 0x002c3909;
    dsi_set_cmdq(data_array, 1, 0);

}
#endif


static unsigned int lcm_compare_id(void)
{
	unsigned int id=0;
	unsigned char buffer[3];
	unsigned int array[16];
	unsigned int data_array[16];
#ifdef BUILD_LK
    upmu_set_rg_vgp2_en(0);
    MDELAY(10);
    upmu_set_rg_vgp2_vosel(6);
    upmu_set_rg_vgp2_en(1);
#else
    hwPowerDown(MT6322_POWER_LDO_VGP2, "LCM");
    MDELAY(10);
    hwPowerOn(MT6322_POWER_LDO_VGP2, VOL_3300, "LCM");
#endif
	mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(50);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(150);

    data_array[0] = 0x00063902;
    data_array[1] = 0x52AA55F0;  
    data_array[2] = 0x00000108;                
    dsi_set_cmdq(&data_array, 3, 1); 

  	array[0] = 0x00033700;// read id return two byte,version and id
  	dsi_set_cmdq(array, 1, 1);
  	
  	read_reg_v2(0xC5, buffer, 3);
  	id = buffer[1]; //we only need ID
    #ifdef BUILD_LK
	printf("%s, nt35521 id = 0x%08x buffer[0]=0x%08x,buffer[1]=0x%08x,buffer[2]=0x%08x\n",
		   __func__, id,buffer[0],buffer[1],buffer[2]);
    #else
	printk("%s, nt35521 id = 0x%08x buffer[0]=0x%08x,buffer[1]=0x%08x,buffer[2]=0x%08x\n",
		   __func__, id,buffer[0],buffer[1],buffer[2]);
    #endif

    if(id == LCM_ID)
    	return 1;
    else
        return 0;
}


int err_count = 0;

static unsigned int lcm_esd_check(void)
{
#ifndef BUILD_LK
    unsigned char buffer[8] = {0};
    unsigned int array[4];
    int i =0;

    array[0] = 0x00013700;    
    dsi_set_cmdq(array, 1,1);
    read_reg_v2(0x0A, buffer,8);

    printk( "nt35521_JDI lcm_esd_check: buffer[0] = %d,buffer[1] = %d,buffer[2] = %d,buffer[3] = %d,buffer[4] = %d,buffer[5] = %d,buffer[6] = %d,buffer[7] = %d\n",buffer[0],buffer[1],buffer[2],buffer[3],buffer[4],buffer[5],buffer[6],buffer[7]);

    if((buffer[0] != 0x9C))/*LCD work status error,need re-initalize*/
    {
        printk( "nt35521_JDI lcm_esd_check buffer[0] = %d\n",buffer[0]);
        return TRUE;
    }
    else
    {
        if(buffer[3] != 0x02) //error data type is 0x02
        {
             return FALSE;
        }
        else
        {
             if((buffer[4] != 0) || (buffer[5] != 0x80))
             {
                  err_count++;
             }
             else
             {
                  err_count = 0;
             }             
             if(err_count >=2 )
             {
                 err_count = 0;
                 printk( "nt35521_JDI lcm_esd_check buffer[4] = %d , buffer[5] = %d\n",buffer[4],buffer[5]);
                 return TRUE;
             }
        }
        return FALSE;
    }
#endif
	
}

static unsigned int lcm_esd_recover(void)
{
    lcm_init();
    lcm_resume();

    return TRUE;
}



LCM_DRIVER nt35521_tm50_llong_hd_lcm_drv = 
{
    .name			= "nt35521_tm50_llong_hd",
    .set_util_funcs = lcm_set_util_funcs,
    .get_params     = lcm_get_params,
    .init           = lcm_init,
    .suspend        = lcm_suspend,
    .resume         = lcm_resume,
    .compare_id     = lcm_compare_id,
    .esd_check = lcm_esd_check,
    .esd_recover = lcm_esd_recover,
#if (LCM_DSI_CMD_MODE)
    .update         = lcm_update,
#endif
    };

