/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/

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

#define FRAME_WIDTH  (800)
#define FRAME_HEIGHT (1280)

#define LCM_ID  (0x21)

#define GPIO_LCD_RST_EN      (GPIO112 | 0x80000000)
#define GPIO_POWER_EN        (GPIO82 | 0x80000000)
#define GPIO_AVEE_EN        (GPIO12 | 0x80000000)
#define GPIO_AVDD_EN        (GPIO13 | 0x80000000)
// ---------------------------------------------------------------------------
//  Local Variables
// ---------------------------------------------------------------------------

static LCM_UTIL_FUNCS lcm_util = { 0 };

#define SET_RESET_PIN(v)    (lcm_util.set_reset_pin((v)))

#define UDELAY(n) (lcm_util.udelay(n))
#define MDELAY(n) (lcm_util.mdelay(n))
#define REGFLAG_DELAY                                       0XFFE
#define REGFLAG_END_OF_TABLE                                0xFFF   // END OF REGISTERS MARKER

// ---------------------------------------------------------------------------
//  Local Functions
// ---------------------------------------------------------------------------

#define dsi_set_cmdq_V2(cmd, count, ppara, force_update)	(lcm_util.dsi_set_cmdq_V2(cmd, count, ppara, force_update))
#define dsi_set_cmdq(pdata, queue_size, force_update)		(lcm_util.dsi_set_cmdq(pdata, queue_size, force_update))
#define wrtie_cmd(cmd)										(lcm_util.dsi_write_cmd(cmd))
#define write_regs(addr, pdata, byte_nums)					(lcm_util.dsi_write_regs(addr, pdata, byte_nums))
#define read_reg(cmd)										(lcm_util.dsi_dcs_read_lcm_reg(cmd))
#define read_reg_v2(cmd, buffer, buffer_size)   			(lcm_util.dsi_dcs_read_lcm_reg_v2(cmd, buffer, buffer_size))

#define dsi_lcm_set_gpio_out(pin, out)										lcm_util.set_gpio_out(pin, out)
#define dsi_lcm_set_gpio_mode(pin, mode)									lcm_util.set_gpio_mode(pin, mode)
#define dsi_lcm_set_gpio_dir(pin, dir)										lcm_util.set_gpio_dir(pin, dir)
#define dsi_lcm_set_gpio_pull_enable(pin, en)								lcm_util.set_gpio_pull_enable(pin, en)

#define   LCM_DSI_CMD_MODE							0

struct LCM_setting_table {
    unsigned cmd;
    unsigned char count;
    unsigned char para_list[64];
};

static struct LCM_setting_table lcm_initialization_setting[] = {
	{0xFF,4,{0xAA,0x55,0xA5,0x80}},//========== Internal setting ==========
	{REGFLAG_DELAY, 120, {}},

	{0x6F,2,{0x11,0x00}},// MIPI related Timing Setting
	{0xF7,2,{0x20,0x00}},

	{0x6F,1,{0x06}},//  Improve ESD option
	{0xF7,1,{0xA0}},
	{0x6F,1,{0x19}},
	{0xF7,1,{0x12}},

	{0x6F,1,{0x08}},// Vcom floating
	{0xFA,1,{0x40}},
	{0x6F,1,{0x11}},
	{0xF3,1,{0x01}},

	{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},//========== page0 relative ==========
	{0xC8,1,{0x80}},

	{0xB1,2,{0x6C,0x01}},// Set WXGA resolution

	{0xB6,1,{0x08}},// Set source output hold time

	{0x6F,1,{0x02}},//EQ control function
	{0xB8,1,{0x08}},

	{0xBB,2,{0x54,0x54}},// Set bias current for GOP and SOP

	{0xBC,2,{0x05,0x05}},// Inversion setting 

	{0xC7,1,{0x01}},// zigzag setting

	{0xBD,5,{0x02,0xB0,0x0C,0x0A,0x00}},// DSP Timing Settings update for BIST

	{0xF0,5,{0x55,0xAA,0x52,0x08,0x01}},//========== page1 relative ==========

	{0xB0,2,{0x05,0x05}},// Setting AVDD, AVEE clamp
	{0xB1,2,{0x05,0x05}},

	{0xBC,2,{0x3A,0x01}},// VGMP, VGMN, VGSP, VGSN setting
	{0xBD,2,{0x3E,0x01}},

	{0xCA,1,{0x00}},// gate signal control

	{0xC0,1,{0x04}},// power IC control

	{0xB2,1,{0x00}},// VCL SET -2.5V

	{0xBE,1,{0x80}},// VCOM = -1.888V

	{0xB3,2,{0x19,0x19}},// Setting VGH=15V, VGL=-11V
	{0xB4,2,{0x12,0x12}},

	{0xB9,2,{0x24,0x24}},// power control for VGH, VGL
	{0xBA,2,{0x14,0x14}},

	{0xF0,5,{0x55,0xAA,0x52,0x08,0x02}},//========== page2 relative ==========

	{0xEE,1,{0x01}},//gamma setting
	{0xEF,4,{0x09,0x06,0x15,0x18}},//Gradient Control for Gamma Voltage

	{0xB0,6,{0x00,0x00,0x00,0x08,0x00,0x17}},
	{0x6F,1,{0x06}},
	{0xB0,6,{0x00,0x25,0x00,0x30,0x00,0x45}},
	{0x6F,1,{0x0C}},
	{0xB0,4,{0x00,0x56,0x00,0x7A}},                                                                                                                                            
	{0xB1,6,{0x00,0xA3,0x00,0xE7,0x01,0x20}},
	{0x6F,1,{0x06}},
	{0xB1,6,{0x01,0x7A,0x01,0xC2,0x01,0xC5}},
	{0x6F,1,{0x0C}},
	{0xB1,4,{0x02,0x06,0x02,0x5F}},
	{0xB2,6,{0x02,0x92,0x02,0xD0,0x02,0xFC}},
	{0x6F,1,{0x06}},
	{0xB2,6,{0x03,0x35,0x03,0x5D,0x03,0x8B}},
	{0x6F,1,{0x0C}},
	{0xB2,4,{0x03,0xA2,0x03,0xBF}},
	{0xB3,4,{0x03,0xD2,0x03,0xFF}},

	//========== GOA relative ==========
	{0xF0,5,{0x55,0xAA,0x52,0x08,0x06}},// PAGE6 : GOUT Mapping, VGLO select
	{0xB0,2,{0x00,0x17}},
	{0xB1,2,{0x16,0x15}},
	{0xB2,2,{0x14,0x13}},
	{0xB3,2,{0x12,0x11}},
	{0xB4,2,{0x10,0x2D}},
	{0xB5,2,{0x01,0x08}},
	{0xB6,2,{0x09,0x31}},
	{0xB7,2,{0x31,0x31}},
	{0xB8,2,{0x31,0x31}},
	{0xB9,2,{0x31,0x31}},
	{0xBA,2,{0x31,0x31}},
	{0xBB,2,{0x31,0x31}},
	{0xBC,2,{0x31,0x31}},
	{0xBD,2,{0x31,0x09}},
	{0xBE,2,{0x08,0x01}},
	{0xBF,2,{0x2D,0x10}},
	{0xC0,2,{0x11,0x12}},
	{0xC1,2,{0x13,0x14}},
	{0xC2,2,{0x15,0x16}},
	{0xC3,2,{0x17,0x00}},
	{0xE5,2,{0x31,0x31}},
	{0xC4,2,{0x00,0x17}},
	{0xC5,2,{0x16,0x15}},
	{0xC6,2,{0x14,0x13}},
	{0xC7,2,{0x12,0x11}},
	{0xC8,2,{0x10,0x2D}},
	{0xC9,2,{0x01,0x08}},
	{0xCA,2,{0x09,0x31}},
	{0xCB,2,{0x31,0x31}},
	{0xCC,2,{0x31,0x31}},
	{0xCD,2,{0x31,0x31}},
	{0xCE,2,{0x31,0x31}},
	{0xCF,2,{0x31,0x31}},
	{0xD0,2,{0x31,0x31}},
	{0xD1,2,{0x31,0x09}},
	{0xD2,2,{0x08,0x01}},
	{0xD3,2,{0x2D,0x10}},
	{0xD4,2,{0x11,0x12}},
	{0xD5,2,{0x13,0x14}},
	{0xD6,2,{0x15,0x16}},
	{0xD7,2,{0x17,0x00}},
	{0xE6,2,{0x31,0x31}},
	{0xD8,5,{0x00,0x00,0x00,0x00,0x00}},//VGL level select
	{0xD9,5,{0x00,0x00,0x00,0x00,0x00}},
	{0xE7,1,{0x00}},

	// PAGE3 :
	{0xF0,5,{0x55,0xAA,0x52,0x08,0x03}},//gate timing control
	{0xB0,2,{0x20,0x00}},
	{0xB1,2,{0x20,0x00}},
	{0xB2,5,{0x05,0x00,0x42,0x00,0x00}},
	{0xB6,5,{0x05,0x00,0x42,0x00,0x00}},
	{0xBA,5,{0x53,0x00,0x42,0x00,0x00}},
	{0xBB,5,{0x53,0x00,0x42,0x00,0x00}},
	{0xC4,1,{0x40}},

	// gate CLK EQ
	// gate STV EQ

	// PAGE5 :
	{0xF0,5,{0x55,0xAA,0x52,0x08,0x05}},
	{0xB0,2,{0x17,0x06}},
	{0xB8,1,{0x00}},
	{0xBD,5,{0x03,0x01,0x01,0x00,0x01}},
	{0xB1,2,{0x17,0x06}},
	{0xB9,2,{0x00,0x01}},
	{0xB2,2,{0x17,0x06}},
	{0xBA,2,{0x00,0x01}},
	{0xB3,2,{0x17,0x06}},
	{0xBB,2,{0x0A,0x00}},
	{0xB4,2,{0x17,0x06}},
	{0xB5,2,{0x17,0x06}},
	{0xB6,2,{0x14,0x03}},
	{0xB7,2,{0x00,0x00}},
	{0xBC,2,{0x02,0x01}},
	{0xC0,1,{0x05}},
	{0xC4,1,{0xA5}},
	{0xC8,2,{0x03,0x30}},
	{0xC9,2,{0x03,0x51}},
	{0xD1,5,{0x00,0x05,0x03,0x00,0x00}},
	{0xD2,5,{0x00,0x05,0x09,0x00,0x00}},
	{0xE5,1,{0x02}},
	{0xE6,1,{0x02}},
	{0xE7,1,{0x02}},
	{0xE9,1,{0x02}},
	{0xED,1,{0x33}},

	{0x11,1,{0x00}},
	{REGFLAG_DELAY, 120, {}},
	{0x29,1,{0x00}},
	{REGFLAG_DELAY, 120, {}},
	//	{0xF0,5,{0x55,0xAA,0x52,0x08,0x00}},
	//	{0xEE,4,{0x87,0x78,0x02,0x40}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
static struct LCM_setting_table lcm_sleep_out_setting[] = {
	// Sleep Out
	{0x11, 1, {0x00}},
	{REGFLAG_DELAY, 120, {}},
	// Display ON
	{0x29, 1, {0x00}},
	{REGFLAG_DELAY, 20, {}},
	{REGFLAG_END_OF_TABLE, 0x00, {}}
};
static struct LCM_setting_table lcm_deep_sleep_mode_in_setting[] = {
	// Display off sequence
	{0x28, 1, {0x00}},
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
		}
	}
}
// ---------------------------------------------------------------------------
//  LCM Driver Implementations
// ---------------------------------------------------------------------------

static void lcm_set_util_funcs(const LCM_UTIL_FUNCS * util)
{
	memcpy(&lcm_util, util, sizeof(LCM_UTIL_FUNCS));
}

static void lcm_get_params(LCM_PARAMS * params)
{
	memset(params, 0, sizeof(LCM_PARAMS));

	params->type = LCM_TYPE_DSI;

	params->width = FRAME_WIDTH;
	params->height = FRAME_HEIGHT;

#if (LCM_DSI_CMD_MODE)
	params->dsi.mode = CMD_MODE;
#else
	params->dsi.mode = BURST_VDO_MODE;	//SYNC_PULSE_VDO_MODE;//BURST_VDO_MODE;
#endif
	// DSI
	/* Command mode setting */
	//1 Three lane or Four lane
	params->dsi.LANE_NUM = LCM_FOUR_LANE;
	params->dsi.data_format.color_order = LCM_COLOR_ORDER_RGB;
	params->dsi.data_format.trans_seq   = LCM_DSI_TRANS_SEQ_MSB_FIRST;
	params->dsi.data_format.padding     = LCM_DSI_PADDING_ON_LSB;                                                                                                         
	params->dsi.data_format.format      = LCM_DSI_FORMAT_RGB888;

	params->dsi.packet_size = 256;
	params->dsi.intermediat_buffer_num = 0;
	// Video mode setting
	params->dsi.PS = LCM_PACKED_PS_24BIT_RGB888;
	params->dsi.word_count = 800*3;

    params->dsi.vertical_sync_active		= 10;
    params->dsi.vertical_backporch		    = 10;
    params->dsi.vertical_frontporch 		= 10;
    params->dsi.vertical_active_line		= FRAME_HEIGHT; 

    params->dsi.horizontal_sync_active		= 10;//Jim
    params->dsi.horizontal_backporch		= 200;
    params->dsi.horizontal_frontporch		= 200;
    params->dsi.horizontal_active_pixel 	= FRAME_WIDTH;

	//params->dsi.LPX=8;

	// Bit rate calculation
	//1 Every lane speed
	//params->dsi.pll_div1 = 0;	// div1=0,1,2,3;div1_real=1,2,4,4 ----0: 546Mbps  1:273Mbps
	//params->dsi.pll_div2 = 1;	// div2=0,1,2,3;div1_real=1,2,4,4
	//params->dsi.fbk_div = 17;	// fref=26MHz, fvco=fref*(fbk_div+1)*2/(div1_real*div2_real)
	//params->dsi.cont_clock = 1;//samsung
	params->dsi.PLL_CLOCK = 300;
}
static void lcm_init(void)
{
	unsigned int data_array[16];

	mt_set_gpio_mode(GPIO_POWER_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_POWER_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_POWER_EN, GPIO_OUT_ONE);
	mt_set_gpio_mode(GPIO_AVEE_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_AVEE_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVEE_EN, GPIO_OUT_ONE);
	mt_set_gpio_mode(GPIO_AVDD_EN, GPIO_MODE_00);                                                                                                                         
	mt_set_gpio_dir(GPIO_AVDD_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVDD_EN, GPIO_OUT_ONE);

#ifdef BUILD_LK
	upmu_set_rg_vgp3_en(0);                                                                                                                                               
	MDELAY(10);
	upmu_set_rg_vgp3_vosel(3);
	upmu_set_rg_vgp3_en(1);
	isl98607_set_rg_output_vol(0x10, 0x10, 0x10);
#else
	hwPowerOn(MT6322_POWER_LDO_VGP3, VOL_1800, "LCM");
#endif

	MDELAY(100);
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

void lcm_suspend(void)
{
	push_table(lcm_deep_sleep_mode_in_setting, sizeof(lcm_deep_sleep_mode_in_setting) / sizeof(struct LCM_setting_table), 1);	//wqtao. enable
#ifdef BUILD_LK
	upmu_set_rg_vgp3_en(0);
#else
	hwPowerDown(MT6322_POWER_LDO_VGP3, "LCM");
#endif
	mt_set_gpio_out(GPIO_POWER_EN, GPIO_OUT_ZERO);
	mt_set_gpio_out(GPIO_AVEE_EN, GPIO_OUT_ZERO);
	mt_set_gpio_out(GPIO_AVDD_EN, GPIO_OUT_ZERO);
}

static void lcm_resume(void)
{
	//push_table(lcm_sleep_out_setting, sizeof(lcm_sleep_out_setting) / sizeof(struct LCM_setting_table), 1);	//wqtao. enable
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

	unsigned char x0_MSB = ((x0 >> 8) & 0xFF);
	unsigned char x0_LSB = (x0 & 0xFF);
	unsigned char x1_MSB = ((x1 >> 8) & 0xFF);
	unsigned char x1_LSB = (x1 & 0xFF);
	unsigned char y0_MSB = ((y0 >> 8) & 0xFF);
	unsigned char y0_LSB = (y0 & 0xFF);
	unsigned char y1_MSB = ((y1 >> 8) & 0xFF);
	unsigned char y1_LSB = (y1 & 0xFF);

	unsigned int data_array[16];

	data_array[0] = 0x00053902;
	data_array[1] =
	    (x1_MSB << 24) | (x0_LSB << 16) | (x0_MSB << 8) | 0x2a;
	data_array[2] = (x1_LSB);
	dsi_set_cmdq(data_array, 3, 1);

	data_array[0] = 0x00053902;
	data_array[1] =
	    (y1_MSB << 24) | (y0_LSB << 16) | (y0_MSB << 8) | 0x2b;
	data_array[2] = (y1_LSB);
	dsi_set_cmdq(data_array, 3, 1);

	data_array[0] = 0x002c3909;
	dsi_set_cmdq(data_array, 1, 0);

}
#endif

static unsigned int lcm_compare_id(void)
{
	unsigned int id=0;
	unsigned char buffer[3];
	unsigned int array[16];
	unsigned int data_array[16];
	mt_set_gpio_mode(GPIO_POWER_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_POWER_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_POWER_EN, GPIO_OUT_ONE);
	mt_set_gpio_mode(GPIO_AVEE_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_AVEE_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVEE_EN, GPIO_OUT_ONE);
	mt_set_gpio_mode(GPIO_AVDD_EN, GPIO_MODE_00);                                                                                                                         
	mt_set_gpio_dir(GPIO_AVDD_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_AVDD_EN, GPIO_OUT_ONE);

#ifdef BUILD_LK
	upmu_set_rg_vgp3_en(0);                                                                                                                                               
	MDELAY(10);
	upmu_set_rg_vgp3_vosel(3);
	upmu_set_rg_vgp3_en(1);
	isl98607_set_rg_output_vol(0x10, 0x10, 0x10);
#else
	hwPowerOn(MT6322_POWER_LDO_VGP3, VOL_1800, "LCM");
#endif

	MDELAY(100);
	mt_set_gpio_mode(GPIO_LCD_RST_EN, GPIO_MODE_00);
	mt_set_gpio_dir(GPIO_LCD_RST_EN, GPIO_DIR_OUT);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(10);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ZERO);
	MDELAY(50);
	mt_set_gpio_out(GPIO_LCD_RST_EN, GPIO_OUT_ONE);
	MDELAY(50);

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

LCM_DRIVER nt35521_wxga_cmi_lcm_drv = {
	.name = "nt35521_cmi_wxga",
	.set_util_funcs = lcm_set_util_funcs,
	.get_params = lcm_get_params,
	.init = lcm_init,
	.suspend = lcm_suspend,
	.resume = lcm_resume,
	.compare_id  = lcm_compare_id,
//	.esd_check = lcm_esd_check,
//	.esd_recover = lcm_esd_recover,
#if (LCM_DSI_CMD_MODE)
	.update = lcm_update,
#endif
};
