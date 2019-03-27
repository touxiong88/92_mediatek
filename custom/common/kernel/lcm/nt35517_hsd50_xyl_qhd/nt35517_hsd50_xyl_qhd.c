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

#define FRAME_WIDTH  (540)
#define FRAME_HEIGHT (960)

#define REGFLAG_DELAY             							0xAB
#define REGFLAG_END_OF_TABLE      							0xFB   // END OF REGISTERS MARKER

#define GPIO_LCD_RST_EN      (GPIO112 | 0x80000000)
#define GPIO_AVEE_EN        (GPIO119 | 0x80000000)
#define GPIO_AVDD_EN        (GPIO118 | 0x80000000)

// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))

#define LCM_ID       (0x5517)
#define LCM_ID1       (0xC1)
#define LCM_ID2       (0x80)

// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	(lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update))
#define dsi_set_cmdq(pdata, queue_size, force_update)		(lcm_util.dsi_set_cmdq(pdata, queue_size, force_update))
#define wrtie_cmd(cmd)										(lcm_util.dsi_write_cmd(cmd))
#define write_regs(addr, pdata, byte_nums)					(lcm_util.dsi_write_regs(addr, pdata, byte_nums))
#define read_reg(cmd)										(lcm_util.dsi_dcs_read_lcm_reg(cmd))
#define read_reg_v2(cmd, buffer, buffer_size)   			(lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size))

#define dsi_lcm_set_gpio_out(pin, out)						(lcm_util.set_gpio_out(pin, out))
#define dsi_lcm_set_gpio_mode(pin, mode)					(lcm_util.set_gpio_mode(pin, mode))
#define dsi_lcm_set_gpio_dir(pin, dir)						(lcm_util.set_gpio_dir(pin, dir))
#define dsi_lcm_set_gpio_pull_enable(pin, en)				(lcm_util.set_gpio_pull_enable(pin, en))

struct LCM_setting_table {
    unsigned char cmd;
    unsigned char count;
    unsigned char para_list[64];
};
static struct LCM_setting_table lcm_compare_id_setting[] = {
    // Display off sequence
    {0xf0,  5,      {0x55,0xaa,0x52,0x08,0x01}},
    {REGFLAG_DELAY, 10, {}},
    {REGFLAG_END_OF_TABLE, 0x00, {}}
};

static struct LCM_setting_table lcm_sleep_out_setting[] = {
    // Sleep Out
    {0x11, 1, {0x00}},
    {REGFLAG_DELAY, 120, {}},

    // Display ON
    {0x29, 1, {0x00}},
    {REGFLAG_END_OF_TABLE, 0x00, {}}
};

static struct LCM_setting_table lcm_initialization_setting[] = {
{0xF0, 5, {0x55,0xAA,0x52,0x08,0x00}},

{0xB3, 1, {0x80}},

//BackwardScanCTB=CRL=1
{0xB1, 2, {0xFC,0x00}},

//Sourceholdtime
{0xB6, 1, {0x03}},

//GateEQcontrol
{0xB7, 2, {0x72,0x72}},

//SourceEQcontrol(Mode2)
{0xB8, 4, {0x01,0x06,0x05,0x04}},

//BiasCurrent
{0xBB, 1, {0x55}},

//Inversion
{0xBC, 3, {0x02,0x02,0x02}},

{0xBD, 5, {0x01,0x4E,0x10,0x20,0x01}},

//DisplayTiming:Dual8-phase4-overlap
{0xC9, 6, {0x61,0x06,0x0D,0x17,0x17,0x00}},

//*************************************
//SelectCMD2,Page1
//*************************************
{0xF0, 5, {0x55,0xAA,0x52,0x08,0x01}},

//AVDD:5.3V
{0xB0, 3, {0x0C,0x0C,0x0C}},

//AVEE:-5.3V
{0xB1, 3, {0x0C,0x0C,0x0C}},

//VCL:-3.5V
{0xB2, 3, {0x02,0x02,0x02}},

//VGH:15.0V
{0xB3, 3, {0x10,0x10,0x10}},

//VGLX:-10.0V
{0xB4, 3, {0x06,0x06,0x06}},

//AVDD:3xVDDB
{0xB6, 3, {0x44,0x44,0x44}},

//AVEE:-2xVDDB
{0xB7, 3, {0x34,0x34,0x34}},

//VCL:-2xVDDB
{0xB8, 3, {0x34,0x34,0x34}},

//VGH:2xAVDD-AVEE
{0xB9, 3, {0x34,0x34,0x34}},

//VGLX:AVEE-AVDD
{0xBA, 3, {0x14,0x14,0x14}},

//SetVGMP=4.9V/VGSP=0VXBE

{0xBC, 3, {0x00,0x98,0x00}},

//SetVGMN=-4.9V/VGSN=0V
{0xBD, 3, {0x00,0x98,0x00}},

//VMSEL0:0xBE;1:0xBF
//{0xC1, 1, {0x01}},

//SetVCOM_offset
{0xBE, 1, {0x4E}},//60

//Pump:0x00orPFM:0x50control
{0xC2, 1, {0x00}},

//GammaGradientControl
{0xD0, 4, {0x0F,0x0F,0x10,0x10}},


//R(+)MCRcmd
{0xD1, 16, {0x00,0x00,0x00,0x20,0x00,0x4F,0x00,0x71,0x00,0x8B,0x00,0xB2,0x00,0xD0,0x01,0x03}},
{0xD2, 16, {0x01,0x2A,0x01,0x66,0x01,0x95,0x01,0xDF,0x02,0x1C,0x02,0x1D,0x02,0x58,0x02,0x9A}},
{0xD3, 16, {0x02,0xC4,0x03,0x01,0x03,0x28,0x03,0x6A,0x03,0x8C,0x03,0xA6,0x03,0xC0,0x03,0xD2}},
{0xD4, 4, {0x03,0xE6,0x03,0xFF}},

//G(+)MCRcmd
{0xD5, 16, {0x00,0x00,0x00,0x20,0x00,0x4F,0x00,0x71,0x00,0x8B,0x00,0xB2,0x00,0xD0,0x01,0x03}},
{0xD6, 16, {0x01,0x2A,0x01,0x66,0x01,0x95,0x01,0xDF,0x02,0x1C,0x02,0x1D,0x02,0x58,0x02,0x9A}},
{0xD7, 16, {0x02,0xC4,0x03,0x01,0x03,0x28,0x03,0x6A,0x03,0x8C,0x03,0xA6,0x03,0xC0,0x03,0xD2}},
{0xD8, 4, {0x03,0xE6,0x03,0xFF}},

//B(+)MCRcmd
{0xD9, 16, {0x00,0x00,0x00,0x20,0x00,0x4F,0x00,0x71,0x00,0x8B,0x00,0xB2,0x00,0xD0,0x01,0x03}},
{0xDD, 16, {0x01,0x2A,0x01,0x66,0x01,0x95,0x01,0xDF,0x02,0x1C,0x02,0x1D,0x02,0x58,0x02,0x9A}},
{0xDE, 16, {0x02,0xC4,0x03,0x01,0x03,0x28,0x03,0x6A,0x03,0x8C,0x03,0xA6,0x03,0xC0,0x03,0xD2}},
{0xDF, 4, {0x03,0xE6,0x03,0xFF}},

//R(-)MCRcmd
{0xE0, 16, {0x00,0x00,0x00,0x20,0x00,0x4F,0x00,0x71,0x00,0x8B,0x00,0xB2,0x00,0xD0,0x01,0x03}},
{0xE1, 16, {0x01,0x2A,0x01,0x66,0x01,0x95,0x01,0xDF,0x02,0x1C,0x02,0x1D,0x02,0x58,0x02,0x9A}},
{0xE2, 16, {0x02,0xC4,0x03,0x01,0x03,0x28,0x03,0x6A,0x03,0x8C,0x03,0xA6,0x03,0xC0,0x03,0xD2}},
{0xE3, 4, {0x03,0xE6,0x03,0xFF}},

//G(-)MCRcmd
{0xE4, 16, {0x00,0x00,0x00,0x20,0x00,0x4F,0x00,0x71,0x00,0x8B,0x00,0xB2,0x00,0xD0,0x01,0x03}},
{0xE5, 16, {0x01,0x2A,0x01,0x66,0x01,0x95,0x01,0xDF,0x02,0x1C,0x02,0x1D,0x02,0x58,0x02,0x9A}},
{0xE6, 16, {0x02,0xC4,0x03,0x01,0x03,0x28,0x03,0x6A,0x03,0x8C,0x03,0xA6,0x03,0xC0,0x03,0xD2}},
{0xE7, 4, {0x03,0xE6,0x03,0xFF}},

//B(-)MCRcmd
{0xE8, 16, {0x00,0x00,0x00,0x20,0x00,0x4F,0x00,0x71,0x00,0x8B,0x00,0xB2,0x00,0xD0,0x01,0x03}},
{0xE9, 16, {0x01,0x2A,0x01,0x66,0x01,0x95,0x01,0xDF,0x02,0x1C,0x02,0x1D,0x02,0x58,0x02,0x9A}},
{0xEA, 16, {0x02,0xC4,0x03,0x01,0x03,0x28,0x03,0x6A,0x03,0x8C,0x03,0xA6,0x03,0xC0,0x03,0xD2}},
{0xEB, 4, {0x03,0xE6,0x03,0xFF}},

//*************************************
//SelectCMD3Enable
//*************************************
{0xFF, 4, {0xAA,0x55,0x25,0x01}},
{0xF3, 7, {0x02,0x03,0x07,0x44,0x88,0xD1,0x0D}},
//*************************************
//TEOn
//*************************************
{0x35, 1, {0x00}},

    //*************************************
    // Sleep Out
    //*************************************
    {0x11,1,{0x00}},

    {REGFLAG_DELAY, 150, {}},
    //*************************************
    // Display On
    //*************************************
    {0x29, 1 ,{0x00}},
    {REGFLAG_END_OF_TABLE, 0x00, {}}
};

static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
    // Display off sequence
    {0x28, 1, {0x00}},
    {REGFLAG_DELAY, 10, {}},

    // Sleep Mode On
    {0x10, 1, {0x00}},
    {REGFLAG_DELAY, 120, {}},

    {REGFLAG_END_OF_TABLE, 0x00, {}}
};


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
 /*
                if (cmd != 0xFF && cmd != 0x2C && cmd != 0x3C) {
                    //#if defined(BUILD_UBOOT)
                    //	printf("[DISP] - uboot - REG_R(0x%x) = 0x%x. \n", cmd, table[i].para_list[0]);
                    //#endif
                    while(read_reg(cmd) != table[i].para_list[0]);		
                }
*/
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
	params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
    params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
    params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;
    params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

    params->width  = FRAME_WIDTH;
    params->height = FRAME_HEIGHT;

    // enable tearing-free
    params->dbi.te_mode 			= LCM_DBI_TE_MODE_VSYNC_ONLY;
    params->dbi.te_edge_polarity		= LCM_POLARITY_FALLING;

    //params->dsi.mode   = SYNC_PULSE_VDO_MODE;
    params->dsi.mode   = SYNC_EVENT_VDO_MODE;

    // DSI
    /* Command mode setting */
    params->dsi.LANE_NUM				= LCM_TWO_LANE;

    //The following defined the fomat for data coming from LCD engine.


    params->dsi.intermediat_buffer_num = 2;//because DSI/DPI HW design change, this parameters should be 0 when video mode in MT658X; or memory leakage

    params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

    params->dsi.word_count=540*3;	//DSI CMD mode need set these two bellow params, different to 6577
    params->dsi.vertical_active_line=960;
    params->dsi.compatibility_for_nvk = 0;		// this parameter would be set to 1 if DriverIC is NTK's and when force match DSI clock for NTK's
    	params->dsi.PS=LCM_PACKED_PS_24BIT_RGB888;

	params->dsi.vertical_sync_active				= 3;//4
	params->dsi.vertical_backporch					= 13;//16
	params->dsi.vertical_frontporch					= 12;//15
	params->dsi.vertical_active_line				= FRAME_HEIGHT;
	params->dsi.horizontal_sync_active				= 10;//10
	params->dsi.horizontal_backporch				= 50;//64
	params->dsi.horizontal_frontporch				= 50;//64
	//params->dsi.horizontal_blanking_pixel		       = 60;
	params->dsi.horizontal_active_pixel		       = FRAME_WIDTH;
	// Bit rate calculation
	params->dsi.pll_div1=1;
	params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
	params->dsi.fbk_div=16;
    // Bit rate calculation
/*
#ifdef CONFIG_MT6589_FPGA
    params->dsi.pll_div1=2;		// div1=0,1,2,3;div1_real=1,2,4,4
    params->dsi.pll_div2=2;		// div2=0,1,2,3;div2_real=1,2,4,4
    params->dsi.fbk_div =8;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)
#else
    params->dsi.pll_div1=1;		// div1=0,1,2,3;div1_real=1,2,4,4
    params->dsi.pll_div2=0;		// div2=0,1,2,3;div2_real=1,2,4,4
    params->dsi.fbk_div =14;		// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)		
#endif
*/
}


static void lcm_init(void)
{
	mt_set_gpio_mode(GPIO_AVEE_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_AVEE_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVEE_EN, GPIO_OUT_ONE);
	mt_set_gpio_mode(GPIO_AVDD_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_AVDD_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVDD_EN, GPIO_OUT_ONE);

#ifdef BUILD_LK
    upmu_set_rg_vgp2_vosel(6);
    upmu_set_rg_vgp2_en(1);
	isl98607_set_rg_output_vol(0x04, 0x04, 0x04);
#else
    hwPowerOn(MT6322_POWER_LDO_VGP2, VOL_2800, "LCM");
#endif

	mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(120);

    //init_lcm_registers();
    push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
}


static void lcm_suspend(void)
{
    unsigned int data_array[16];

    data_array[0] = 0x00100500;
    dsi_set_cmdq(data_array, 1, 1);
    MDELAY(120);

    data_array[0] = 0x00280500;
    dsi_set_cmdq(data_array, 1, 1);
    MDELAY(10);

    data_array[0] = 0x014F1500;
    dsi_set_cmdq(data_array, 1, 1);
    MDELAY(40);
#if 1
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(50);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(50);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(120);

#ifdef BUILD_LK
	upmu_set_rg_vgp2_en(0);
#else
	hwPowerDown(MT6322_POWER_LDO_VGP2, "LCM");
#endif
	mt_set_gpio_out(GPIO_AVEE_EN, GPIO_OUT_ZERO);
	mt_set_gpio_out(GPIO_AVDD_EN, GPIO_OUT_ZERO);
#endif
}


static void lcm_resume(void)
{
#ifndef BUILD_LK
	lcm_init();
#endif
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

static void lcm_setbacklight(unsigned int level)
{
    unsigned int data_array[16];


#if defined(BUILD_LK)
    printf("%s, %d\n", __func__, level);
#elif defined(BUILD_UBOOT)
    printf("%s, %d\n", __func__, level);
#else
    printk("lcm_setbacklight = %d\n", level);
#endif

    if(level > 255)
        level = 255;

    data_array[0]= 0x00023902;
    data_array[1] =(0x51|(level<<8));
    dsi_set_cmdq(data_array, 2, 1);
}


static void lcm_setpwm(unsigned int divider)
{
    // TBD
}


static unsigned int lcm_getpwm(unsigned int divider)
{
    // ref freq = 15MHz, B0h setting 0x80, so 80.6% * freq is pwm_clk;
    // pwm_clk / 255 / 2(lcm_setpwm() 6th params) = pwm_duration = 23706
    unsigned int pwm_clk = 23706 / (1<<divider);	


    return pwm_clk;
}


static unsigned int lcm_compare_id(void)
{
	unsigned int id = 0;
	unsigned char buffer[5];
	unsigned int array[5];

	mt_set_gpio_mode(GPIO_AVEE_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_AVEE_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVEE_EN, GPIO_OUT_ONE);
	mt_set_gpio_mode(GPIO_AVDD_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_AVDD_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVDD_EN, GPIO_OUT_ONE);

#ifdef BUILD_LK
    upmu_set_rg_vgp2_vosel(6);
    upmu_set_rg_vgp2_en(1);
	isl98607_set_rg_output_vol(0x04, 0x04, 0x04);
#else
	hwPowerOn(MT6322_POWER_LDO_VGP2, VOL_2800, "LCM");
#endif
	mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(100);

	push_table(lcm_compare_id_setting, sizeof(lcm_compare_id_setting) / sizeof(struct LCM_setting_table), 1);

	array[0] = 0x00033700;// read id return two byte,version and id
	dsi_set_cmdq(array, 1, 1);
	read_reg_v2(0xc5, buffer, 3);
	id = ((buffer[0] << 8) | buffer[1]); //we only need ID
#if defined(BUILD_LK)
	printf("%s, id1 = 0x%x,id2 = 0x%x,id3 = 0x%x\n", __func__, buffer[0],buffer[1],buffer[2]);
#endif
	//printk("%s, id1 = 0x%x,id2 = 0x%x,id3 = 0x%x\n", __func__, buffer[0],buffer[1],buffer[2]);
	return (LCM_ID== id)?1:0;

}


LCM_DRIVER nt35517_hsd50_xyl_qhd_lcm_drv =
{
	.name		= "nt35517_hsd50_xyl_qhd",
    .set_util_funcs = lcm_set_util_funcs,
    .get_params     = lcm_get_params,
    .init           = lcm_init,
    .suspend        = lcm_suspend,
    .resume         = lcm_resume,
    .set_backlight	= lcm_setbacklight,
    //.set_pwm        = lcm_setpwm,
    //.get_pwm        = lcm_getpwm,
    .compare_id    = lcm_compare_id,
    //.update         = lcm_update
};
