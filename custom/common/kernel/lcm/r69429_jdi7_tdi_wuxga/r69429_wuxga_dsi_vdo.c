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

#define FRAME_WIDTH  (1200)
#define FRAME_HEIGHT (1920)

#define GPIO_LCD_RST_EN      (GPIO112 | 0x80000000)
#define GPIO_POWER_EN        (GPIO82 | 0x80000000)
#define GPIO_DCDC_EN        (GPIO106 | 0x80000000)		//ic_power
#define GPIO_LED_EN        (GPIO107 | 0x80000000)		//led_power
// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = {0};

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))

#define REGFLAG_DELAY             							0XFE
#define REGFLAG_END_OF_TABLE      							0x100   // END OF REGISTERS MARKER

// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	        lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update)
#define dsi_set_cmdq(pdata, queue_size, force_update)				lcm_util.dsi_set_cmdq(pdata, queue_size, force_update)
#define wrtie_cmd(cmd)												lcm_util.dsi_write_cmd(cmd)
#define write_regs(addr, pdata, byte_nums)							lcm_util.dsi_write_regs(addr, pdata, byte_nums)
#define read_reg(cmd)												lcm_util.dsi_dcs_read_lcm_reg(cmd)
#define read_reg_v2(cmd, buffer, buffer_size)   					lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size)

#define   LCM_DSI_CMD_MODE							0

struct LCM_setting_table {
	unsigned cmd;
	unsigned char count;
	unsigned char para_list[64];
};

static struct LCM_setting_table lcm_initialization_setting[] = {
	{0x11, 0, {0x0}},
	{REGFLAG_DELAY, 250, {}},
	{0xB0,	1, {0x04}},
	{0xB3, 6, {0x14, 0x0, 0x0, 0x0, 0x0, 0x0}},
	{0xB6, 2, {0x3A, 0xC3}},

	//{0x51,1,{0xe6}},
	{0x53,1,{0x2c}},
	{0x3a,1,{0x77}},
	{0x2a,4,{0x00,0x00,0x04,0xaf}},
	{0x2b,4,{0x00,0x00,0x07,0x7f}},

	{0x29, 0, {0x0}},
	{REGFLAG_DELAY, 100, {}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
static struct LCM_setting_table lcm_sleep_out_setting[] = {
	{0x11, 0, {0x00}},
	{REGFLAG_DELAY, 120, {}},
	{0x29, 0, {0x00}},
	{REGFLAG_DELAY, 20, {}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
static struct LCM_setting_table lcm_sleep_mode_in_setting[] = {
	{0x28, 0, {0x00}},
	{REGFLAG_DELAY, 20, {}},
	{0x10, 0, {0x00}},
	{REGFLAG_DELAY, 120, {}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
static void init_lcm_registers(void)
{
	unsigned int data_array[16];

	data_array[0] = 0x00010500;
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(10);

	data_array[0]=0x00022902;
	data_array[1]=0x000004b0;//b0
	dsi_set_cmdq(data_array, 2, 1);

	data_array[0] = 0x00062902;  //interface setting
	data_array[1] = 0x000814B3;  //5 paras  04-->14
	data_array[2] = 0x00000022;
	dsi_set_cmdq(data_array, 3, 1);

	data_array[0] = 0x00022902;  //interface ID setting
	data_array[1] = 0x00000CB4;
	dsi_set_cmdq(data_array, 2, 1);

	data_array[0] = 0x00032902;  //DSI control
	data_array[1] = 0x00D33AB6;     //D3
	dsi_set_cmdq(data_array, 2, 1);

	data_array[0]=0x00023902;
	data_array[1]=0x0000e651;
	dsi_set_cmdq(data_array, 2, 1);

	data_array[0]=0x00023902;
	data_array[1]=0x00002c53;
	dsi_set_cmdq(data_array, 2, 1);

	data_array[0] = 0x00110500;  //exit sleep mode
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(120);
	data_array[0] = 0x00290500;  //set display on
	dsi_set_cmdq(data_array, 1, 1);
	MDELAY(120);

}
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

	// enable tearing-free
	params->dbi.te_mode 				= LCM_DBI_TE_MODE_VSYNC_ONLY;
	params->dbi.te_edge_polarity		= LCM_POLARITY_RISING;

#if (LCM_DSI_CMD_MODE)
	params->dsi.mode   					= CMD_MODE;
#else
	params->dsi.mode   = BURST_VDO_MODE;//SYNC_PULSE_VDO_MODE; //BURST_VDO_MODE;  //82: SYNC_EVENT_VDO_MODE
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
	params->dsi.word_count=FRAME_WIDTH*3;
	params->dsi.vertical_sync_active				= 2;	// 3    2
	params->dsi.vertical_backporch					= 6;//5;//100;	// 20   1
	params->dsi.vertical_frontporch					= 8;//10;//100; 	// 1  12
	params->dsi.vertical_active_line				= FRAME_HEIGHT;

	params->dsi.horizontal_sync_active				= 12;//10;	// 50  2
	params->dsi.horizontal_backporch				= 60;//120;//100;
	params->dsi.horizontal_frontporch				= 120;//85;//100;
	params->dsi.horizontal_active_pixel				= FRAME_WIDTH;
	params->dsi.pll_select=0;	//0: MIPI_PLL; 1: LVDS_PLL
	params->dsi.PLL_CLOCK=450;

}
static void lcm_init(void)
{
#ifdef BUILD_LK
	upmu_set_rg_vgp3_en(0);
	MDELAY(10);
	upmu_set_rg_vgp3_vosel(3);
	upmu_set_rg_vgp3_en(1);
#else
	hwPowerOn(MT6322_POWER_LDO_VGP3, VOL_1800, "LCM");
#endif
	MDELAY(10);
	mt_set_gpio_mode(GPIO_POWER_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_POWER_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_POWER_EN, GPIO_OUT_ONE);
	MDELAY(20);
	mt_set_gpio_mode(GPIO_DCDC_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_DCDC_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_DCDC_EN, GPIO_OUT_ONE);
	MDELAY(20);
	mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(10);
	mt_set_gpio_out(GPIO_DCDC_EN, GPIO_OUT_ZERO);
	MDELAY(20);
	mt_set_gpio_out(GPIO_DCDC_EN, GPIO_OUT_ONE);
	MDELAY(20);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(50);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(120);
	push_table(lcm_initialization_setting, sizeof(lcm_initialization_setting) / sizeof(struct LCM_setting_table), 1);
	MDELAY(20);
	mt_set_gpio_mode(GPIO_LED_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LED_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LED_EN, GPIO_OUT_ONE);
}

int lcm_backlight_control(unsigned int level)
{
	unsigned int light_level=0;
	light_level = (level >> 2);
	dsi_set_cmdq_V2(0x51, 1, &light_level, 1);
}
static void lcm_suspend(void)
{
	push_table(lcm_sleep_mode_in_setting, sizeof(lcm_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);
#ifdef BUILD_LK
	upmu_set_rg_vgp3_en(0);
#else
	hwPowerDown(MT6322_POWER_LDO_VGP3, "LCM");
#endif
	mt_set_gpio_out(GPIO_POWER_EN, GPIO_OUT_ZERO);
	mt_set_gpio_out(GPIO_DCDC_EN, GPIO_OUT_ZERO);
	mt_set_gpio_out(GPIO_LED_EN, GPIO_OUT_ZERO);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
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

	data_array[0]= 0x00052902;
	data_array[1]= (x1_MSB<<24)|(x0_LSB<<16)|(x0_MSB<<8)|0x2a;
	data_array[2]= (x1_LSB);
	dsi_set_cmdq(data_array, 3, 1);

	data_array[0]= 0x00052902;
	data_array[1]= (y1_MSB<<24)|(y0_LSB<<16)|(y0_MSB<<8)|0x2b;
	data_array[2]= (y1_LSB);
	dsi_set_cmdq(data_array, 3, 1);

	data_array[0]= 0x00290508; //HW bug, so need send one HS packet
	dsi_set_cmdq(data_array, 1, 1);

	data_array[0]= 0x002c3909;  //0x002c3909;
	dsi_set_cmdq(data_array, 1, 0);

}
#endif
#if 0
static unsigned int lcm_compare_id(void)
{
	unsigned int id=0;
	unsigned char buffer[2];
	unsigned int array[16];

	SET_RESET_PIN(1);
	SET_RESET_PIN(0);
	MDELAY(1);

	SET_RESET_PIN(1);
	MDELAY(20);

	array[0] = 0x00023700;// read id return two byte,version and id
	dsi_set_cmdq(array, 1, 1);

	read_reg_v2(0xF4, buffer, 2);
	id = buffer[0]; //we only need ID
#ifdef BUILD_LK
	printf("%s, LK nt35590 debug: nt35590 id = 0x%08x\n", __func__, id);
#else
	printk("%s, kernel nt35590 horse debug: nt35590 id = 0x%08x\n", __func__, id);
#endif

	if(id == LCM_ID_NT35590)
		return 1;
	else
		return 0;
}
#endif

LCM_DRIVER r69429_wuxga_dsi_vdo_lcm_drv =
{
	.name			= "r69429_wuxga_dsi_vdo",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params     = lcm_get_params,
	.init           = lcm_init,
	.suspend        = lcm_suspend,
	.resume         = lcm_resume,
	//.compare_id     = lcm_compare_id,
#if (LCM_DSI_CMD_MODE)
	.update         = lcm_update,
#endif
};
