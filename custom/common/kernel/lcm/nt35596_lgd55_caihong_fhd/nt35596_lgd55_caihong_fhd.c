#ifndef BUILD_LK
#include <linux/string.h>
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

#define FRAME_WIDTH  (1080)
#define FRAME_HEIGHT (1920)

#define LCM_ID_NT35596 (0x96)
#define GPIO_LCD_RST_EN      (GPIO112 | 0x80000000)
// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))


// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	        lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)		lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)										lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)					lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg(cmd)											lcm_util.dsi_dcs_read_lcm_reg(cmd)
#define read_reg_v2(cmd, buffer, buffer_size)   				lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)

#define dsi_lcm_set_gpio_out(pin, out)										lcm_util.set_gpio_out(pin, out)
#define dsi_lcm_set_gpio_mode(pin, mode)									lcm_util.set_gpio_mode(pin, mode)
#define dsi_lcm_set_gpio_dir(pin, dir)										lcm_util.set_gpio_dir(pin, dir)
#define dsi_lcm_set_gpio_pull_enable(pin, en)								lcm_util.set_gpio_pull_enable(pin, en)

#define   LCM_DSI_CMD_MODE							0


static void TC358768_DCS_write_1A_1P(unsigned char cmd, unsigned char para)
{
        unsigned int data_array[16];
        //unsigned char buffer;
        data_array[0] =(0x00023902);
        data_array[1] =(0x00000000| (para<<8)|(cmd));
        dsi_set_cmdq(data_array, 2, 1);

	//MDELAY(1);
}

#define TC358768_DCS_write_1A_0P(cmd)							data_array[0]=(0x00000500 | (cmd<<16)); \
																dsi_set_cmdq(data_array, 1, 1);

static void init_lcm_registers(void)
{
		unsigned int data_array[16];
		//unsigned char buffer[8];

//CMD2 P4		       
TC358768_DCS_write_1A_1P(0xFF,0x05);
TC358768_DCS_write_1A_1P(0xFF,0x05);
TC358768_DCS_write_1A_1P(0xFB,0x01);
TC358768_DCS_write_1A_1P(0xC5,0x31);

TC358768_DCS_write_1A_1P(0x90, 0x00);

//FP/BP
TC358768_DCS_write_1A_1P(0x93, 0x04);
TC358768_DCS_write_1A_1P(0x94, 0x04);

TC358768_DCS_write_1A_1P(0x9B, 0x0F);

TC358768_DCS_write_1A_1P(0x91, 0x44);
TC358768_DCS_write_1A_1P(0x92, 0x79);

//PIN MAP
TC358768_DCS_write_1A_1P(0x00,0x00);
TC358768_DCS_write_1A_1P(0x01,0x00);
TC358768_DCS_write_1A_1P(0x02,0x00);
TC358768_DCS_write_1A_1P(0x03,0x00);
TC358768_DCS_write_1A_1P(0x04,0x00);
TC358768_DCS_write_1A_1P(0x05,0x00);
TC358768_DCS_write_1A_1P(0x06,0x00);
TC358768_DCS_write_1A_1P(0x07,0x00);
TC358768_DCS_write_1A_1P(0x08,0x00);
TC358768_DCS_write_1A_1P(0x09,0x00);
TC358768_DCS_write_1A_1P(0x0A,0x04);
TC358768_DCS_write_1A_1P(0x0B,0x03);
TC358768_DCS_write_1A_1P(0x0C,0x01);
TC358768_DCS_write_1A_1P(0x0D,0x13);
TC358768_DCS_write_1A_1P(0x0E,0x15);
TC358768_DCS_write_1A_1P(0x0F,0x17);

TC358768_DCS_write_1A_1P(0x10,0x00);
TC358768_DCS_write_1A_1P(0x11,0x00);
TC358768_DCS_write_1A_1P(0x12,0x00);
TC358768_DCS_write_1A_1P(0x13,0x00);
TC358768_DCS_write_1A_1P(0x14,0x00);
TC358768_DCS_write_1A_1P(0x15,0x00);
TC358768_DCS_write_1A_1P(0x16,0x00);
TC358768_DCS_write_1A_1P(0x17,0x00);
TC358768_DCS_write_1A_1P(0x18,0x00);
TC358768_DCS_write_1A_1P(0x19,0x00);
TC358768_DCS_write_1A_1P(0x1A,0x04);
TC358768_DCS_write_1A_1P(0x1B,0x03);
TC358768_DCS_write_1A_1P(0x1C,0x01);
TC358768_DCS_write_1A_1P(0x1D,0x13);
TC358768_DCS_write_1A_1P(0x1E,0x15);
TC358768_DCS_write_1A_1P(0x1F,0x17);

//STV		       
TC358768_DCS_write_1A_1P(0x20, 0x00);
TC358768_DCS_write_1A_1P(0x21, 0x00);
TC358768_DCS_write_1A_1P(0x22, 0x09);
TC358768_DCS_write_1A_1P(0x23, 0x05);
TC358768_DCS_write_1A_1P(0x24, 0x05);
TC358768_DCS_write_1A_1P(0x25, 0x6D);

//GCK1,2		       
TC358768_DCS_write_1A_1P(0x2F, 0x01);
TC358768_DCS_write_1A_1P(0x30, 0x01);
TC358768_DCS_write_1A_1P(0x31, 0x00);
TC358768_DCS_write_1A_1P(0x32, 0x32);
TC358768_DCS_write_1A_1P(0x33, 0x01);
TC358768_DCS_write_1A_1P(0x34, 0x03);
TC358768_DCS_write_1A_1P(0x35, 0x68);
TC358768_DCS_write_1A_1P(0x36, 0x00);
TC358768_DCS_write_1A_1P(0x37, 0x2D);
TC358768_DCS_write_1A_1P(0x38, 0x08);

//U2D,D2U		       
TC358768_DCS_write_1A_1P(0x29, 0x58);
TC358768_DCS_write_1A_1P(0x2A, 0x16);
TC358768_DCS_write_1A_1P(0x2B, 0x0A);

TC358768_DCS_write_1A_1P(0x5B, 0x00);
TC358768_DCS_write_1A_1P(0x5F, 0x75);
TC358768_DCS_write_1A_1P(0x63, 0x00);
TC358768_DCS_write_1A_1P(0x67, 0x04);
TC358768_DCS_write_1A_1P(0x6C, 0x00);

TC358768_DCS_write_1A_1P(0x5C, 0x2C);
TC358768_DCS_write_1A_1P(0x60, 0x75);
TC358768_DCS_write_1A_1P(0x64, 0x00);
TC358768_DCS_write_1A_1P(0x68, 0x04);
TC358768_DCS_write_1A_1P(0x6C, 0x10);
TC358768_DCS_write_1A_1P(0x5D, 0x00);
TC358768_DCS_write_1A_1P(0x61, 0x75);
TC358768_DCS_write_1A_1P(0x65, 0x00);
TC358768_DCS_write_1A_1P(0x69, 0x04);
TC358768_DCS_write_1A_1P(0x6C, 0x00);

TC358768_DCS_write_1A_1P(0x6C, 0x10);

TC358768_DCS_write_1A_1P(0x7A, 0x01);
//MUX dummy
TC358768_DCS_write_1A_1P(0x7B, 0x80);
//TC358768_DCS_write_1A_1P(0x7B, 0x91);
TC358768_DCS_write_1A_1P(0x7C, 0xD8);
TC358768_DCS_write_1A_1P(0x7D, 0x60);
TC358768_DCS_write_1A_1P(0x7E, 0x07);
TC358768_DCS_write_1A_1P(0x7F, 0x17);
TC358768_DCS_write_1A_1P(0x80, 0x00);
TC358768_DCS_write_1A_1P(0x81, 0x05);
TC358768_DCS_write_1A_1P(0x82, 0x02);
TC358768_DCS_write_1A_1P(0x83, 0x00);
TC358768_DCS_write_1A_1P(0x84, 0x05);
TC358768_DCS_write_1A_1P(0x85, 0x05);
TC358768_DCS_write_1A_1P(0x86, 0x1B);
TC358768_DCS_write_1A_1P(0x87, 0x1B);
TC358768_DCS_write_1A_1P(0x88, 0x1B);
TC358768_DCS_write_1A_1P(0x89, 0x1B);
TC358768_DCS_write_1A_1P(0x8A, 0x33);
TC358768_DCS_write_1A_1P(0x8B, 0x00);
TC358768_DCS_write_1A_1P(0x8C, 0x00);

TC358768_DCS_write_1A_1P(0x73, 0xD0);
TC358768_DCS_write_1A_1P(0x74, 0x0D);
TC358768_DCS_write_1A_1P(0x75, 0x03);
TC358768_DCS_write_1A_1P(0x76, 0x16);

TC358768_DCS_write_1A_1P(0x99, 0x33);
TC358768_DCS_write_1A_1P(0x98, 0x00);

//*******************************************************************
//Power Related
TC358768_DCS_write_1A_1P(0xFF, 0x01);

//Normal Black Panel
TC358768_DCS_write_1A_1P(0x00, 0x01);

//PUMP VGH=2xAVDD, VGL=2AVEE
TC358768_DCS_write_1A_1P(0x05, 0x50);

//EN_PRE_REG
TC358768_DCS_write_1A_1P(0x06, 0x4A);

//Enable VGHO/VGLO clamp function, and Set VGHO=10V, VGLO=-10V
TC358768_DCS_write_1A_1P(0x14, 0xA8);
TC358768_DCS_write_1A_1P(0x07, 0xB2);

//Regulator Enable, VGHO/VGLO setting, VGHO=9V, VGLO=-9V
TC358768_DCS_write_1A_1P(0x0E, 0xB5);
TC358768_DCS_write_1A_1P(0x0F, 0xB8);

//Gamma Voltage Setting GVDDP=3.9V, GVDDN=-3.9V
TC358768_DCS_write_1A_1P(0x0B, 0x69);
TC358768_DCS_write_1A_1P(0x0C, 0x69);

//VCOM setting, VCOM=-0.2V
TC358768_DCS_write_1A_1P(0x11, 0x13);
TC358768_DCS_write_1A_1P(0x12, 0x13);

//AVDDR/AVEER Setting (+/- 4.8V)
TC358768_DCS_write_1A_1P(0x08, 0x0C);
TC358768_DCS_write_1A_1P(0x15, 0x13);
TC358768_DCS_write_1A_1P(0x16, 0x13);

//ISOP
//TC358768_DCS_write_1A_1P(0x6D, 0x22);

//Gate EQ
//TC358768_DCS_write_1A_1P(0x58, 0x82);
//TC358768_DCS_write_1A_1P(0x59, 0x02);
//TC358768_DCS_write_1A_1P(0x5A, 0x02);
//TC358768_DCS_write_1A_1P(0x5B, 0x02);
//TC358768_DCS_write_1A_1P(0x5C, 0x82);
//TC358768_DCS_write_1A_1P(0x5D, 0x82);
//TC358768_DCS_write_1A_1P(0x5E, 0x02);
//TC358768_DCS_write_1A_1P(0x5F, 0x02);

//TC358768_DCS_write_1A_1P(0x58, 0x02);
//TC358768_DCS_write_1A_1P(0x5C, 0x02);
//TC358768_DCS_write_1A_1P(0x5D, 0x02);

//*******************************************************************

//CMD1
TC358768_DCS_write_1A_1P(0xFF, 0x00);

TC358768_DCS_write_1A_1P(0xBA, 0x03);//Setting MIPI 4 Lane

//VBP=8,VFP=8
TC358768_DCS_write_1A_1P(0xD3, 0x12);//VBP
TC358768_DCS_write_1A_1P(0xD4, 0x08);//VFP
MDELAY(5);

TC358768_DCS_write_1A_0P(0x11);
//Delayus(150000);
MDELAY(120);
TC358768_DCS_write_1A_1P(0xFF,0x00);
//TC358768_DCS_write_1A_1P(0x34,0x00);
TC358768_DCS_write_1A_1P(0x35,0x00);
//TC358768_DCS_write_1A_1P(0x36,0x08);
TC358768_DCS_write_1A_0P(0x29);
MDELAY(10);

#if 0//ndef BUILD_LK
	read_reg_v2(0xDA, &buffer[0], 1);
	read_reg_v2(0xDB, &buffer[1], 1);
	read_reg_v2(0xDC, &buffer[2], 1);
	read_reg_v2(0xF4, &buffer[3], 1);

	printk("%s, ID = (0x%02x, 0x%02x, 0x%02x, 0x%02x)\n", __func__, buffer[0], buffer[1], buffer[2], buffer[3]);
#endif

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

		// enable tearing-free
		params->dbi.te_mode 				= LCM_DBI_TE_MODE_VSYNC_ONLY;
		params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

        #if (LCM_DSI_CMD_MODE)
		params->dsi.mode   = CMD_MODE;
        #else
		params->dsi.mode   = BURST_VDO_MODE;
        #endif

		// DSI
		/* Command mode setting */
		//1 Three lane or Four lane
		params->dsi.LANE_NUM				= LCM_FOUR_LANE;
		//The following defined the fomat for data coming from LCD engine.
		params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
		params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
		params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
		params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

		// Highly depends on LCD driver capability.
		// Not support in MT6573
		params->dsi.packet_size=256;

		// Video mode setting
		params->dsi.intermediat_buffer_num = 0;//because DSI/DPI HW design change, this parameters should be 0 when video mode in MT658X; or memory leakage

		params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;
//		params->dsi.word_count=720*3;


		params->dsi.vertical_sync_active				= 4;
		params->dsi.vertical_backporch					= 8;
		params->dsi.vertical_frontporch					= 8;
		params->dsi.vertical_active_line				= FRAME_HEIGHT;

		params->dsi.horizontal_sync_active				= 4;
		params->dsi.horizontal_backporch				= 118;
		params->dsi.horizontal_frontporch				= 118;
		params->dsi.horizontal_active_pixel				= FRAME_WIDTH;

		// Bit rate calculation
		//1 Every lane speed
		params->dsi.pll_div1=0;		// div1=0,1,2,3;div1_real=1,2,4,4 ----0: 546Mbps  1:273Mbps
		params->dsi.pll_div2=0;		// div2=0,1,2,3;div1_real=1,2,4,4
		params->dsi.fbk_div =16;    // fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)

}

static void lcm_init(void)
{
#ifdef BUILD_LK
    upmu_set_rg_vgp2_vosel(6);
    upmu_set_rg_vgp2_en(1);
#else
    hwPowerOn(MT6322_POWER_LDO_VGP2, VOL_3300, "LCM");
#endif
    mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
    mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
    MDELAY(10);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
    MDELAY(10);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
    MDELAY(120);
	init_lcm_registers();
#ifdef BUILD_LK
	printf("lcm_init nt35596\n");
#else
	printk("lcm_init nt35596\n");
#endif
}

static void lcm_suspend(void)
{
	unsigned int data_array[16];
	//unsigned char buffer[2];

#if 0//ndef BUILD_LK
	data_array[0] = 0x00013700;// read id return two byte,version and id
	dsi_set_cmdq(data_array, 1, 1);

	read_reg_v2(0xFE, buffer, 1);
	printk("%s, kernel nt35596 horse debug: nt35596 id = 0x%08x\n", __func__, buffer[0]);
#endif

	data_array[0]=0x00280500; // Display Off
	dsi_set_cmdq(data_array, 1, 1);
    MDELAY(20);

	data_array[0] = 0x00100500; // Sleep In
	dsi_set_cmdq(data_array, 1, 1);
    MDELAY(120);

    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
    MDELAY(50);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
    MDELAY(50);
}


static void lcm_resume(void)
{
	//unsigned int data_array[16];
	//unsigned char buffer[2];

	lcm_init();

#if 0//ndef BUILD_LK
	data_array[0] = 0x00013700;// read id return two byte,version and id
	dsi_set_cmdq(data_array, 1, 1);

	read_reg_v2(0xFE, buffer, 1);
	printk("%s, kernel nt35596 horse debug: nt35596 id = 0x%08x\n", __func__, buffer[0]);
#endif

	//TC358768_DCS_write_1A_0P(0x11); // Sleep Out
	//MDELAY(150);

	//TC358768_DCS_write_1A_0P(0x29); // Display On

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

	data_array[0]= 0x00290508; //HW bug, so need send one HS packet
	dsi_set_cmdq(data_array, 1, 1);

	data_array[0]= 0x002c3909;
	dsi_set_cmdq(data_array, 1, 0);

}
#endif
#if 1
static unsigned int lcm_compare_id(void)
{
	unsigned int id=0;
	unsigned int id0, id1, id2, id3;
	unsigned char buffer[4];
	unsigned int array[16];

#ifdef BUILD_LK
    upmu_set_rg_vgp2_vosel(6);
    upmu_set_rg_vgp2_en(1);
#else
    hwPowerOn(MT6322_POWER_LDO_VGP2, VOL_3300, "LCM");
#endif
    mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
    mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
    MDELAY(10);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
    MDELAY(10);
    mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
    MDELAY(120);

	array[0] = 0x00023700;// read id return two byte,version and id
	dsi_set_cmdq(array, 1, 1);

	read_reg_v2(0xF4, buffer, 4);
	id = buffer[0]; //we only need ID
	id0 = buffer[0];
	id1 = buffer[1];
	id2 = buffer[2];
	id3 = buffer[3];	
#ifdef BUILD_LK
	printf("%s, LK nt35596 debug: nt35596 id = 0x%08x, id0 = 0x%08x, id1 = 0x%08x, id2 = 0x%08x, id3 = 0x%08x\n", __func__, id, id0, id1, id2, id3);
#else
	printk("%s, kernel nt35596 horse debug: nt35596 id = 0x%08x, id0 = 0x%08x, id1 = 0x%08x, id2 = 0x%08x, id3 = 0x%08x\n", __func__, id, id0, id1, id2, id3);
#endif

    if(id == LCM_ID_NT35596)
    	return 1;
    else
        return 0;
}
#endif

LCM_DRIVER nt35596_lgd55_caihong_fhd_lcm_drv =
{
    .name			= "nt35596_lgd55_caihong_fhd",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	.compare_id     = lcm_compare_id,
#if (LCM_DSI_CMD_MODE)
    .update         = lcm_update,
#endif
    };
