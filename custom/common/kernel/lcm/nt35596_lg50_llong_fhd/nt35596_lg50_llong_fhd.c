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

TC358768_DCS_write_1A_1P(0xff,0xee);
TC358768_DCS_write_1A_1P(0xfb,0x01);
TC358768_DCS_write_1A_1P(0x18,0x40);
MDELAY(10);
TC358768_DCS_write_1A_1P(0x18,0x00);
MDELAY(20);
	
TC358768_DCS_write_1A_1P(0xff,0x05);
TC358768_DCS_write_1A_1P(0xfb,0x01);
TC358768_DCS_write_1A_1P(0xc5,0x31);
TC358768_DCS_write_1A_1P(0xff,0x01);
TC358768_DCS_write_1A_1P(0xfb,0x01);
TC358768_DCS_write_1A_1P(0x00,0x01);
TC358768_DCS_write_1A_1P(0x01,0x55);
TC358768_DCS_write_1A_1P(0x02,0x45);
TC358768_DCS_write_1A_1P(0x03,0x55);
TC358768_DCS_write_1A_1P(0x05,0x50);
TC358768_DCS_write_1A_1P(0x14,0xa8);
TC358768_DCS_write_1A_1P(0x07,0xb2);
TC358768_DCS_write_1A_1P(0x08,0x0c);
TC358768_DCS_write_1A_1P(0x0b,0x88);
TC358768_DCS_write_1A_1P(0x0c,0x88);
TC358768_DCS_write_1A_1P(0x0e,0xbf);
TC358768_DCS_write_1A_1P(0x0f,0xc2);
TC358768_DCS_write_1A_1P(0x11,0x1f);
TC358768_DCS_write_1A_1P(0x12,0x1f);
TC358768_DCS_write_1A_1P(0x13,0x03);
TC358768_DCS_write_1A_1P(0x06,0x0a);
TC358768_DCS_write_1A_1P(0x15,0x95);
TC358768_DCS_write_1A_1P(0x16,0x95);
TC358768_DCS_write_1A_1P(0x1b,0x39);
TC358768_DCS_write_1A_1P(0x1c,0x39);
TC358768_DCS_write_1A_1P(0x1d,0x47);


//TC358768_DCS_write_1A_1P(0x44,0xc1);
//TC358768_DCS_write_1A_1P(0x45,0x86);
//TC358768_DCS_write_1A_1P(0x46,0xc4);

TC358768_DCS_write_1A_1P(0x58,0x05);
TC358768_DCS_write_1A_1P(0x59,0x05);
TC358768_DCS_write_1A_1P(0x5a,0x05);
TC358768_DCS_write_1A_1P(0x5b,0x05);
TC358768_DCS_write_1A_1P(0x5c,0x00);
TC358768_DCS_write_1A_1P(0x5d,0x00);
TC358768_DCS_write_1A_1P(0x5e,0x00);
TC358768_DCS_write_1A_1P(0x5f,0x00);
TC358768_DCS_write_1A_1P(0x6d,0x44);
TC358768_DCS_write_1A_1P(0x75,0x00);
TC358768_DCS_write_1A_1P(0x76,0x00);
TC358768_DCS_write_1A_1P(0x77,0x00);
TC358768_DCS_write_1A_1P(0x78,0x10);
TC358768_DCS_write_1A_1P(0x79,0x00);
TC358768_DCS_write_1A_1P(0x7a,0x24);
TC358768_DCS_write_1A_1P(0x7b,0x00);
TC358768_DCS_write_1A_1P(0x7c,0x38);
TC358768_DCS_write_1A_1P(0x7d,0x00);
TC358768_DCS_write_1A_1P(0x7e,0x4b);
TC358768_DCS_write_1A_1P(0x7f,0x00);
TC358768_DCS_write_1A_1P(0x80,0x5c);
TC358768_DCS_write_1A_1P(0x81,0x00);
TC358768_DCS_write_1A_1P(0x82,0x68);
TC358768_DCS_write_1A_1P(0x83,0x00);
TC358768_DCS_write_1A_1P(0x84,0x75);
TC358768_DCS_write_1A_1P(0x85,0x00);
TC358768_DCS_write_1A_1P(0x86,0x83);
TC358768_DCS_write_1A_1P(0x87,0x00);
TC358768_DCS_write_1A_1P(0x88,0xac);
TC358768_DCS_write_1A_1P(0x89,0x00);
TC358768_DCS_write_1A_1P(0x8a,0xd0);
TC358768_DCS_write_1A_1P(0x8b,0x01);
TC358768_DCS_write_1A_1P(0x8c,0x0a);
TC358768_DCS_write_1A_1P(0x8d,0x01);
TC358768_DCS_write_1A_1P(0x8e,0x3c);
TC358768_DCS_write_1A_1P(0x8f,0x01);
TC358768_DCS_write_1A_1P(0x90,0x90);
TC358768_DCS_write_1A_1P(0x91,0x01);
TC358768_DCS_write_1A_1P(0x92,0xd9);
TC358768_DCS_write_1A_1P(0x93,0x01);
TC358768_DCS_write_1A_1P(0x94,0xdb);
TC358768_DCS_write_1A_1P(0x95,0x02);
TC358768_DCS_write_1A_1P(0x96,0x1d);
TC358768_DCS_write_1A_1P(0x97,0x02);
TC358768_DCS_write_1A_1P(0x98,0x6d);
TC358768_DCS_write_1A_1P(0x99,0x02);
TC358768_DCS_write_1A_1P(0x9a,0x9f);
TC358768_DCS_write_1A_1P(0x9b,0x02);
TC358768_DCS_write_1A_1P(0x9c,0xf5);
TC358768_DCS_write_1A_1P(0x9d,0x03);
TC358768_DCS_write_1A_1P(0x9e,0x1f);
TC358768_DCS_write_1A_1P(0x9f,0x03);
TC358768_DCS_write_1A_1P(0xa0,0x58);
TC358768_DCS_write_1A_1P(0xa2,0x03);
TC358768_DCS_write_1A_1P(0xa3,0x64);
TC358768_DCS_write_1A_1P(0xa4,0x03);
TC358768_DCS_write_1A_1P(0xa5,0x75);
TC358768_DCS_write_1A_1P(0xa6,0x03);
TC358768_DCS_write_1A_1P(0xa7,0x89);
TC358768_DCS_write_1A_1P(0xa9,0x03);
TC358768_DCS_write_1A_1P(0xaa,0x99);
TC358768_DCS_write_1A_1P(0xab,0x03);
TC358768_DCS_write_1A_1P(0xac,0xac);
TC358768_DCS_write_1A_1P(0xad,0x03);
TC358768_DCS_write_1A_1P(0xae,0xc3);
TC358768_DCS_write_1A_1P(0xaf,0x03);
TC358768_DCS_write_1A_1P(0xb0,0xdd);
TC358768_DCS_write_1A_1P(0xb1,0x03);
TC358768_DCS_write_1A_1P(0xb2,0xe8);
TC358768_DCS_write_1A_1P(0xb3,0x00);
TC358768_DCS_write_1A_1P(0xb4,0x00);
TC358768_DCS_write_1A_1P(0xb5,0x00);
TC358768_DCS_write_1A_1P(0xb6,0x10);
TC358768_DCS_write_1A_1P(0xb7,0x00);
TC358768_DCS_write_1A_1P(0xb8,0x24);
TC358768_DCS_write_1A_1P(0xb9,0x00);
TC358768_DCS_write_1A_1P(0xba,0x38);
TC358768_DCS_write_1A_1P(0xbb,0x00);
TC358768_DCS_write_1A_1P(0xbc,0x4b);
TC358768_DCS_write_1A_1P(0xbd,0x00);
TC358768_DCS_write_1A_1P(0xbe,0x5c);
TC358768_DCS_write_1A_1P(0xbf,0x00);
TC358768_DCS_write_1A_1P(0xc0,0x68);
TC358768_DCS_write_1A_1P(0xc1,0x00);
TC358768_DCS_write_1A_1P(0xc2,0x75);
TC358768_DCS_write_1A_1P(0xc3,0x00);
TC358768_DCS_write_1A_1P(0xc4,0x83);
TC358768_DCS_write_1A_1P(0xc5,0x00);
TC358768_DCS_write_1A_1P(0xc6,0xac);
TC358768_DCS_write_1A_1P(0xc7,0x00);
TC358768_DCS_write_1A_1P(0xc8,0xd0);
TC358768_DCS_write_1A_1P(0xc9,0x01);
TC358768_DCS_write_1A_1P(0xca,0x0a);
TC358768_DCS_write_1A_1P(0xcb,0x01);
TC358768_DCS_write_1A_1P(0xcc,0x3c);
TC358768_DCS_write_1A_1P(0xcd,0x01);
TC358768_DCS_write_1A_1P(0xce,0x90);
TC358768_DCS_write_1A_1P(0xcf,0x01);
TC358768_DCS_write_1A_1P(0xd0,0xd9);
TC358768_DCS_write_1A_1P(0xd1,0x01);
TC358768_DCS_write_1A_1P(0xd2,0xdb);
TC358768_DCS_write_1A_1P(0xd3,0x02);
TC358768_DCS_write_1A_1P(0xd4,0x1d);
TC358768_DCS_write_1A_1P(0xd5,0x02);
TC358768_DCS_write_1A_1P(0xd6,0x6d);
TC358768_DCS_write_1A_1P(0xd7,0x02);
TC358768_DCS_write_1A_1P(0xd8,0x9f);
TC358768_DCS_write_1A_1P(0xd9,0x02);
TC358768_DCS_write_1A_1P(0xda,0xf5);
TC358768_DCS_write_1A_1P(0xdb,0x03);
TC358768_DCS_write_1A_1P(0xdc,0x1f);
TC358768_DCS_write_1A_1P(0xdd,0x03);
TC358768_DCS_write_1A_1P(0xde,0x58);
TC358768_DCS_write_1A_1P(0xdf,0x03);
TC358768_DCS_write_1A_1P(0xe0,0x64);
TC358768_DCS_write_1A_1P(0xe1,0x03);
TC358768_DCS_write_1A_1P(0xe2,0x75);
TC358768_DCS_write_1A_1P(0xe3,0x03);
TC358768_DCS_write_1A_1P(0xe4,0x89);
TC358768_DCS_write_1A_1P(0xe5,0x03);
TC358768_DCS_write_1A_1P(0xe6,0x99);
TC358768_DCS_write_1A_1P(0xe7,0x03);
TC358768_DCS_write_1A_1P(0xe8,0xac);
TC358768_DCS_write_1A_1P(0xe9,0x03);
TC358768_DCS_write_1A_1P(0xea,0xc3);
TC358768_DCS_write_1A_1P(0xeb,0x03);
TC358768_DCS_write_1A_1P(0xec,0xdd);
TC358768_DCS_write_1A_1P(0xed,0x03);
TC358768_DCS_write_1A_1P(0xee,0xe8);
TC358768_DCS_write_1A_1P(0xef,0x00);
TC358768_DCS_write_1A_1P(0xf0,0x00);
TC358768_DCS_write_1A_1P(0xf1,0x00);
TC358768_DCS_write_1A_1P(0xf2,0x10);
TC358768_DCS_write_1A_1P(0xf3,0x00);
TC358768_DCS_write_1A_1P(0xf4,0x24);
TC358768_DCS_write_1A_1P(0xf5,0x00);
TC358768_DCS_write_1A_1P(0xf6,0x38);
TC358768_DCS_write_1A_1P(0xf7,0x00);
TC358768_DCS_write_1A_1P(0xf8,0x4b);
TC358768_DCS_write_1A_1P(0xf9,0x00);
TC358768_DCS_write_1A_1P(0xfa,0x5c);
TC358768_DCS_write_1A_1P(0xff,0x02);
TC358768_DCS_write_1A_1P(0xfb,0x01);
TC358768_DCS_write_1A_1P(0x00,0x00);
TC358768_DCS_write_1A_1P(0x01,0x68);
TC358768_DCS_write_1A_1P(0x02,0x00);
TC358768_DCS_write_1A_1P(0x03,0x75);
TC358768_DCS_write_1A_1P(0x04,0x00);
TC358768_DCS_write_1A_1P(0x05,0x83);
TC358768_DCS_write_1A_1P(0x06,0x00);
TC358768_DCS_write_1A_1P(0x07,0xac);
TC358768_DCS_write_1A_1P(0x08,0x00);
TC358768_DCS_write_1A_1P(0x09,0xd0);
TC358768_DCS_write_1A_1P(0x0a,0x01);
TC358768_DCS_write_1A_1P(0x0b,0x0a);
TC358768_DCS_write_1A_1P(0x0c,0x01);
TC358768_DCS_write_1A_1P(0x0d,0x3c);
TC358768_DCS_write_1A_1P(0x0e,0x01);
TC358768_DCS_write_1A_1P(0x0f,0x90);
TC358768_DCS_write_1A_1P(0x10,0x01);
TC358768_DCS_write_1A_1P(0x11,0xd9);
TC358768_DCS_write_1A_1P(0x12,0x01);
TC358768_DCS_write_1A_1P(0x13,0xdb);
TC358768_DCS_write_1A_1P(0x14,0x02);
TC358768_DCS_write_1A_1P(0x15,0x1d);
TC358768_DCS_write_1A_1P(0x16,0x02);
TC358768_DCS_write_1A_1P(0x17,0x6d);
TC358768_DCS_write_1A_1P(0x18,0x02);
TC358768_DCS_write_1A_1P(0x19,0x9f);
TC358768_DCS_write_1A_1P(0x1a,0x02);
TC358768_DCS_write_1A_1P(0x1b,0xf5);
TC358768_DCS_write_1A_1P(0x1c,0x03);
TC358768_DCS_write_1A_1P(0x1d,0x1f);
TC358768_DCS_write_1A_1P(0x1e,0x03);
TC358768_DCS_write_1A_1P(0x1f,0x58);
TC358768_DCS_write_1A_1P(0x20,0x03);
TC358768_DCS_write_1A_1P(0x21,0x64);
TC358768_DCS_write_1A_1P(0x22,0x03);
TC358768_DCS_write_1A_1P(0x23,0x75);
TC358768_DCS_write_1A_1P(0x24,0x03);
TC358768_DCS_write_1A_1P(0x25,0x89);
TC358768_DCS_write_1A_1P(0x26,0x03);
TC358768_DCS_write_1A_1P(0x27,0x99);
TC358768_DCS_write_1A_1P(0x28,0x03);
TC358768_DCS_write_1A_1P(0x29,0xac);
TC358768_DCS_write_1A_1P(0x2a,0x03);
TC358768_DCS_write_1A_1P(0x2b,0xc3);
TC358768_DCS_write_1A_1P(0x2d,0x03);
TC358768_DCS_write_1A_1P(0x2f,0xdd);
TC358768_DCS_write_1A_1P(0x30,0x03);
TC358768_DCS_write_1A_1P(0x31,0xe8);
TC358768_DCS_write_1A_1P(0x32,0x00);
TC358768_DCS_write_1A_1P(0x33,0x00);
TC358768_DCS_write_1A_1P(0x34,0x00);
TC358768_DCS_write_1A_1P(0x35,0x10);
TC358768_DCS_write_1A_1P(0x36,0x00);
TC358768_DCS_write_1A_1P(0x37,0x24);
TC358768_DCS_write_1A_1P(0x38,0x00);
TC358768_DCS_write_1A_1P(0x39,0x38);
TC358768_DCS_write_1A_1P(0x3a,0x00);
TC358768_DCS_write_1A_1P(0x3b,0x4b);
TC358768_DCS_write_1A_1P(0x3d,0x00);
TC358768_DCS_write_1A_1P(0x3f,0x5c);
TC358768_DCS_write_1A_1P(0x40,0x00);
TC358768_DCS_write_1A_1P(0x41,0x68);
TC358768_DCS_write_1A_1P(0x42,0x00);
TC358768_DCS_write_1A_1P(0x43,0x75);
TC358768_DCS_write_1A_1P(0x44,0x00);
TC358768_DCS_write_1A_1P(0x45,0x83);
TC358768_DCS_write_1A_1P(0x46,0x00);
TC358768_DCS_write_1A_1P(0x47,0xac);
TC358768_DCS_write_1A_1P(0x48,0x00);
TC358768_DCS_write_1A_1P(0x49,0xd0);
TC358768_DCS_write_1A_1P(0x4a,0x01);
TC358768_DCS_write_1A_1P(0x4b,0x0a);
TC358768_DCS_write_1A_1P(0x4c,0x01);
TC358768_DCS_write_1A_1P(0x4d,0x3c);
TC358768_DCS_write_1A_1P(0x4e,0x01);
TC358768_DCS_write_1A_1P(0x4f,0x90);
TC358768_DCS_write_1A_1P(0x50,0x01);
TC358768_DCS_write_1A_1P(0x51,0xd9);
TC358768_DCS_write_1A_1P(0x52,0x01);
TC358768_DCS_write_1A_1P(0x53,0xdb);
TC358768_DCS_write_1A_1P(0x54,0x02);
TC358768_DCS_write_1A_1P(0x55,0x1d);
TC358768_DCS_write_1A_1P(0x56,0x02);
TC358768_DCS_write_1A_1P(0x58,0x6d);
TC358768_DCS_write_1A_1P(0x59,0x02);
TC358768_DCS_write_1A_1P(0x5a,0x9f);
TC358768_DCS_write_1A_1P(0x5b,0x02);
TC358768_DCS_write_1A_1P(0x5c,0xf5);
TC358768_DCS_write_1A_1P(0x5d,0x03);
TC358768_DCS_write_1A_1P(0x5e,0x1f);
TC358768_DCS_write_1A_1P(0x5f,0x03);
TC358768_DCS_write_1A_1P(0x60,0x58);
TC358768_DCS_write_1A_1P(0x61,0x03);
TC358768_DCS_write_1A_1P(0x62,0x64);
TC358768_DCS_write_1A_1P(0x63,0x03);
TC358768_DCS_write_1A_1P(0x64,0x75);
TC358768_DCS_write_1A_1P(0x65,0x03);
TC358768_DCS_write_1A_1P(0x66,0x89);
TC358768_DCS_write_1A_1P(0x67,0x03);
TC358768_DCS_write_1A_1P(0x68,0x99);
TC358768_DCS_write_1A_1P(0x69,0x03);
TC358768_DCS_write_1A_1P(0x6a,0xac);
TC358768_DCS_write_1A_1P(0x6b,0x03);
TC358768_DCS_write_1A_1P(0x6c,0xc3);
TC358768_DCS_write_1A_1P(0x6d,0x03);
TC358768_DCS_write_1A_1P(0x6e,0xdd);
TC358768_DCS_write_1A_1P(0x6f,0x03);
TC358768_DCS_write_1A_1P(0x70,0xe8);
TC358768_DCS_write_1A_1P(0x71,0x00);
TC358768_DCS_write_1A_1P(0x72,0x00);
TC358768_DCS_write_1A_1P(0x73,0x00);
TC358768_DCS_write_1A_1P(0x74,0x10);
TC358768_DCS_write_1A_1P(0x75,0x00);
TC358768_DCS_write_1A_1P(0x76,0x24);
TC358768_DCS_write_1A_1P(0x77,0x00);
TC358768_DCS_write_1A_1P(0x78,0x38);
TC358768_DCS_write_1A_1P(0x79,0x00);
TC358768_DCS_write_1A_1P(0x7a,0x4b);
TC358768_DCS_write_1A_1P(0x7b,0x00);
TC358768_DCS_write_1A_1P(0x7c,0x5c);
TC358768_DCS_write_1A_1P(0x7d,0x00);
TC358768_DCS_write_1A_1P(0x7e,0x68);
TC358768_DCS_write_1A_1P(0x7f,0x00);
TC358768_DCS_write_1A_1P(0x80,0x75);
TC358768_DCS_write_1A_1P(0x81,0x00);
TC358768_DCS_write_1A_1P(0x82,0x83);
TC358768_DCS_write_1A_1P(0x83,0x00);
TC358768_DCS_write_1A_1P(0x84,0xac);
TC358768_DCS_write_1A_1P(0x85,0x00);
TC358768_DCS_write_1A_1P(0x86,0xd0);
TC358768_DCS_write_1A_1P(0x87,0x01);
TC358768_DCS_write_1A_1P(0x88,0x0a);
TC358768_DCS_write_1A_1P(0x89,0x01);
TC358768_DCS_write_1A_1P(0x8a,0x3c);
TC358768_DCS_write_1A_1P(0x8b,0x01);
TC358768_DCS_write_1A_1P(0x8c,0x90);
TC358768_DCS_write_1A_1P(0x8d,0x01);
TC358768_DCS_write_1A_1P(0x8e,0xd9);
TC358768_DCS_write_1A_1P(0x8f,0x01);
TC358768_DCS_write_1A_1P(0x90,0xdb);
TC358768_DCS_write_1A_1P(0x91,0x02);
TC358768_DCS_write_1A_1P(0x92,0x1d);
TC358768_DCS_write_1A_1P(0x93,0x02);
TC358768_DCS_write_1A_1P(0x94,0x6d);
TC358768_DCS_write_1A_1P(0x95,0x02);
TC358768_DCS_write_1A_1P(0x96,0x9f);
TC358768_DCS_write_1A_1P(0x97,0x02);
TC358768_DCS_write_1A_1P(0x98,0xf5);
TC358768_DCS_write_1A_1P(0x99,0x03);
TC358768_DCS_write_1A_1P(0x9a,0x1f);
TC358768_DCS_write_1A_1P(0x9b,0x03);
TC358768_DCS_write_1A_1P(0x9c,0x58);
TC358768_DCS_write_1A_1P(0x9d,0x03);
TC358768_DCS_write_1A_1P(0x9e,0x64);
TC358768_DCS_write_1A_1P(0x9f,0x03);
TC358768_DCS_write_1A_1P(0xa0,0x75);
TC358768_DCS_write_1A_1P(0xa2,0x03);
TC358768_DCS_write_1A_1P(0xa3,0x89);
TC358768_DCS_write_1A_1P(0xa4,0x03);
TC358768_DCS_write_1A_1P(0xa5,0x99);
TC358768_DCS_write_1A_1P(0xa6,0x03);
TC358768_DCS_write_1A_1P(0xa7,0xac);
TC358768_DCS_write_1A_1P(0xa9,0x03);
TC358768_DCS_write_1A_1P(0xaa,0xc3);
TC358768_DCS_write_1A_1P(0xab,0x03);
TC358768_DCS_write_1A_1P(0xac,0xdd);
TC358768_DCS_write_1A_1P(0xad,0x03);
TC358768_DCS_write_1A_1P(0xae,0xe8);
TC358768_DCS_write_1A_1P(0xaf,0x00);
TC358768_DCS_write_1A_1P(0xb0,0x00);
TC358768_DCS_write_1A_1P(0xb1,0x00);
TC358768_DCS_write_1A_1P(0xb2,0x10);
TC358768_DCS_write_1A_1P(0xb3,0x00);
TC358768_DCS_write_1A_1P(0xb4,0x24);
TC358768_DCS_write_1A_1P(0xb5,0x00);
TC358768_DCS_write_1A_1P(0xb6,0x38);
TC358768_DCS_write_1A_1P(0xb7,0x00);
TC358768_DCS_write_1A_1P(0xb8,0x4b);
TC358768_DCS_write_1A_1P(0xb9,0x00);
TC358768_DCS_write_1A_1P(0xba,0x5c);
TC358768_DCS_write_1A_1P(0xbb,0x00);
TC358768_DCS_write_1A_1P(0xbc,0x68);
TC358768_DCS_write_1A_1P(0xbd,0x00);
TC358768_DCS_write_1A_1P(0xbe,0x75);
TC358768_DCS_write_1A_1P(0xbf,0x00);
TC358768_DCS_write_1A_1P(0xc0,0x83);
TC358768_DCS_write_1A_1P(0xc1,0x00);
TC358768_DCS_write_1A_1P(0xc2,0xac);
TC358768_DCS_write_1A_1P(0xc3,0x00);
TC358768_DCS_write_1A_1P(0xc4,0xd0);
TC358768_DCS_write_1A_1P(0xc5,0x01);
TC358768_DCS_write_1A_1P(0xc6,0x0a);
TC358768_DCS_write_1A_1P(0xc7,0x01);
TC358768_DCS_write_1A_1P(0xc8,0x3c);
TC358768_DCS_write_1A_1P(0xc9,0x01);
TC358768_DCS_write_1A_1P(0xca,0x90);
TC358768_DCS_write_1A_1P(0xcb,0x01);
TC358768_DCS_write_1A_1P(0xcc,0xd9);
TC358768_DCS_write_1A_1P(0xcd,0x01);
TC358768_DCS_write_1A_1P(0xce,0xdb);
TC358768_DCS_write_1A_1P(0xcf,0x02);
TC358768_DCS_write_1A_1P(0xd0,0x1d);
TC358768_DCS_write_1A_1P(0xd1,0x02);
TC358768_DCS_write_1A_1P(0xd2,0x6d);
TC358768_DCS_write_1A_1P(0xd3,0x02);
TC358768_DCS_write_1A_1P(0xd4,0x9f);
TC358768_DCS_write_1A_1P(0xd5,0x02);
TC358768_DCS_write_1A_1P(0xd6,0xf5);
TC358768_DCS_write_1A_1P(0xd7,0x03);
TC358768_DCS_write_1A_1P(0xd8,0x1f);
TC358768_DCS_write_1A_1P(0xd9,0x03);
TC358768_DCS_write_1A_1P(0xda,0x58);
TC358768_DCS_write_1A_1P(0xdb,0x03);
TC358768_DCS_write_1A_1P(0xdc,0x64);
TC358768_DCS_write_1A_1P(0xdd,0x03);
TC358768_DCS_write_1A_1P(0xde,0x75);
TC358768_DCS_write_1A_1P(0xdf,0x03);
TC358768_DCS_write_1A_1P(0xe0,0x89);
TC358768_DCS_write_1A_1P(0xe1,0x03);
TC358768_DCS_write_1A_1P(0xe2,0x99);
TC358768_DCS_write_1A_1P(0xe3,0x03);
TC358768_DCS_write_1A_1P(0xe4,0xac);
TC358768_DCS_write_1A_1P(0xe5,0x03);
TC358768_DCS_write_1A_1P(0xe6,0xc3);
TC358768_DCS_write_1A_1P(0xe7,0x03);
TC358768_DCS_write_1A_1P(0xe8,0xdd);
TC358768_DCS_write_1A_1P(0xe9,0x03);
TC358768_DCS_write_1A_1P(0xea,0xe8);
TC358768_DCS_write_1A_1P(0xff,0x05);
TC358768_DCS_write_1A_1P(0xfb,0x01);
TC358768_DCS_write_1A_1P(0x00,0x0f);
TC358768_DCS_write_1A_1P(0x01,0x00);
TC358768_DCS_write_1A_1P(0x02,0x00);
TC358768_DCS_write_1A_1P(0x03,0x00);
TC358768_DCS_write_1A_1P(0x04,0x0b);
TC358768_DCS_write_1A_1P(0x05,0x0c);
TC358768_DCS_write_1A_1P(0x06,0x00);
TC358768_DCS_write_1A_1P(0x07,0x00);
TC358768_DCS_write_1A_1P(0x08,0x00);
TC358768_DCS_write_1A_1P(0x09,0x00);
TC358768_DCS_write_1A_1P(0x0a,0x03);
TC358768_DCS_write_1A_1P(0x0b,0x04);
TC358768_DCS_write_1A_1P(0x0c,0x01);
TC358768_DCS_write_1A_1P(0x0d,0x13);
TC358768_DCS_write_1A_1P(0x0e,0x15);
TC358768_DCS_write_1A_1P(0x0f,0x17);
TC358768_DCS_write_1A_1P(0x10,0x0f);
TC358768_DCS_write_1A_1P(0x11,0x00);
TC358768_DCS_write_1A_1P(0x12,0x00);
TC358768_DCS_write_1A_1P(0x13,0x00);
TC358768_DCS_write_1A_1P(0x14,0x0b);
TC358768_DCS_write_1A_1P(0x15,0x0c);
TC358768_DCS_write_1A_1P(0x16,0x00);
TC358768_DCS_write_1A_1P(0x17,0x00);
TC358768_DCS_write_1A_1P(0x18,0x00);
TC358768_DCS_write_1A_1P(0x19,0x00);
TC358768_DCS_write_1A_1P(0x1a,0x03);
TC358768_DCS_write_1A_1P(0x1b,0x04);
TC358768_DCS_write_1A_1P(0x1c,0x01);
TC358768_DCS_write_1A_1P(0x1d,0x13);
TC358768_DCS_write_1A_1P(0x1e,0x15);
TC358768_DCS_write_1A_1P(0x1f,0x17);
TC358768_DCS_write_1A_1P(0x20,0x09);
TC358768_DCS_write_1A_1P(0x21,0x01);
TC358768_DCS_write_1A_1P(0x22,0x00);
TC358768_DCS_write_1A_1P(0x23,0x00);
TC358768_DCS_write_1A_1P(0x24,0x00);
TC358768_DCS_write_1A_1P(0x25,0xed);
TC358768_DCS_write_1A_1P(0x29,0x58);
TC358768_DCS_write_1A_1P(0x2a,0x16);
TC358768_DCS_write_1A_1P(0x2b,0x05);
TC358768_DCS_write_1A_1P(0x2f,0x02);
TC358768_DCS_write_1A_1P(0x30,0x04);
TC358768_DCS_write_1A_1P(0x31,0x49);
TC358768_DCS_write_1A_1P(0x32,0x23);
TC358768_DCS_write_1A_1P(0x33,0x01);
TC358768_DCS_write_1A_1P(0x34,0x00);
TC358768_DCS_write_1A_1P(0x35,0x69);
TC358768_DCS_write_1A_1P(0x36,0x00);
TC358768_DCS_write_1A_1P(0x37,0x2d);
TC358768_DCS_write_1A_1P(0x38,0x18);
TC358768_DCS_write_1A_1P(0x5b,0x00);
TC358768_DCS_write_1A_1P(0x5f,0x75);
TC358768_DCS_write_1A_1P(0x63,0x00);
TC358768_DCS_write_1A_1P(0x67,0x04);
TC358768_DCS_write_1A_1P(0x6c,0x00);
TC358768_DCS_write_1A_1P(0x90,0x00);
TC358768_DCS_write_1A_1P(0x74,0x10);
TC358768_DCS_write_1A_1P(0x75,0x19);
TC358768_DCS_write_1A_1P(0x76,0x06);
TC358768_DCS_write_1A_1P(0x77,0x03);
TC358768_DCS_write_1A_1P(0x78,0x00);
TC358768_DCS_write_1A_1P(0x79,0x00);
TC358768_DCS_write_1A_1P(0x7b,0x80);
TC358768_DCS_write_1A_1P(0x7c,0xd8);
TC358768_DCS_write_1A_1P(0x7d,0x60);
TC358768_DCS_write_1A_1P(0x7e,0x10);
TC358768_DCS_write_1A_1P(0x7f,0x19);
TC358768_DCS_write_1A_1P(0x80,0x00);
TC358768_DCS_write_1A_1P(0x81,0x06);
TC358768_DCS_write_1A_1P(0x82,0x03);
TC358768_DCS_write_1A_1P(0x83,0x00);
TC358768_DCS_write_1A_1P(0x84,0x03);
TC358768_DCS_write_1A_1P(0x85,0x07);
TC358768_DCS_write_1A_1P(0x86,0x1b);
TC358768_DCS_write_1A_1P(0x87,0x39);
TC358768_DCS_write_1A_1P(0x88,0x1b);
TC358768_DCS_write_1A_1P(0x89,0x39);
TC358768_DCS_write_1A_1P(0x8a,0x33);//33
//TC358768_DCS_write_1A_1P(0xb5,0x20);
TC358768_DCS_write_1A_1P(0x8c,0x01);
TC358768_DCS_write_1A_1P(0x91,0x4c);
TC358768_DCS_write_1A_1P(0x92,0x79);
TC358768_DCS_write_1A_1P(0x93,0x04);
TC358768_DCS_write_1A_1P(0x94,0x04);
TC358768_DCS_write_1A_1P(0x95,0xe4);
TC358768_DCS_write_1A_1P(0x98,0x00);
TC358768_DCS_write_1A_1P(0x99,0x33);
TC358768_DCS_write_1A_1P(0x9b,0x0f);//0f
TC358768_DCS_write_1A_1P(0xa4,0x0f);
TC358768_DCS_write_1A_1P(0x9d,0xb0);
/*TC358768_DCS_write_1A_1P(0xc4,0x24);
TC358768_DCS_write_1A_1P(0xc5,0x30);
TC358768_DCS_write_1A_1P(0xc6,0x00);*/
TC358768_DCS_write_1A_1P(0xff,0x23);
TC358768_DCS_write_1A_1P(0x08,0x04);
TC358768_DCS_write_1A_1P(0xfb,0x01);
TC358768_DCS_write_1A_1P(0xff,0x00);

TC358768_DCS_write_1A_1P(0xBA, 0x03);//Setting MIPI 4 Lane

//VBP=8,VFP=8
TC358768_DCS_write_1A_1P(0xd3,0x12);
TC358768_DCS_write_1A_1P(0xd4,0x08);//VFP

MDELAY(5);


TC358768_DCS_write_1A_0P(0x11);

MDELAY(120);

TC358768_DCS_write_1A_0P(0x29);

/*
TC358768_DCS_write_1A_1P(0xff,0x05);
MDELAY(10);
TC358768_DCS_write_1A_1P(0xec,0x01);
//MDELAY(10);*/

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
		params->dsi.fbk_div =14;    // fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)

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

LCM_DRIVER nt35596_lg50_llong_fhd_lcm_drv =
{
    .name			= "nt35596_lg50_llong_fhd",
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
