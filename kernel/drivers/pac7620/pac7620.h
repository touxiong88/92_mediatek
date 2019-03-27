

// REGISTER DESCRIPTION
#define PAC7620_VAL(val, maskbit)		( val << maskbit )
#define PAC7620_ADDR_BASE				0x00

// REGISTER BANK SELECT
#define PAC7620_REGITER_BANK_SEL		(PAC7620_ADDR_BASE + 0xEF)	//W

// REGISTER BANK 0
#define PAC7620_ADDR_SUSPEND_CMD		(PAC7620_ADDR_BASE + 0x3)	//W
#define PAC7620_ADDR_GES_PS_DET_MASK_0		(PAC7620_ADDR_BASE + 0x41)	//RW
#define PAC7620_ADDR_GES_PS_DET_MASK_1		(PAC7620_ADDR_BASE + 0x42)	//RW
#define PAC7620_ADDR_GES_PS_DET_FLAG_0		(PAC7620_ADDR_BASE + 0x43)	//R
#define PAC7620_ADDR_GES_PS_DET_FLAG_1		(PAC7620_ADDR_BASE + 0x44)	//R
#define PAC7620_ADDR_STATE_INDICATOR	(PAC7620_ADDR_BASE + 0x45)	//R
#define PAC7620_ADDR_PS_HIGH_THRESHOLD	(PAC7620_ADDR_BASE + 0x69)	//RW
#define PAC7620_ADDR_PS_LOW_THRESHOLD	(PAC7620_ADDR_BASE + 0x6A)	//RW
#define PAC7620_ADDR_PS_APPROACH_STATE	(PAC7620_ADDR_BASE + 0x6B)	//R
#define PAC7620_ADDR_PS_RAW_DATA		(PAC7620_ADDR_BASE + 0x6C)	//R

#define PAC7620_ADDR_SIZE_H		(PAC7620_ADDR_BASE +  0XD7)	//RW
#define PAC7620_ADDR_SIZE_L		(PAC7620_ADDR_BASE +  0XD8)	//RW
#define PAC7620_ADDR_CURSOR_XL	(PAC7620_ADDR_BASE + 0XD3)	//RW
#define PAC7620_ADDR_CURSOR_YX	(PAC7620_ADDR_BASE + 0XD4)	//RW
#define PAC7620_ADDR_CURSOR_YL	(PAC7620_ADDR_BASE + 0XD5)	//RW


// REGISTER BANK 1
#define PAC7620_ADDR_PS_GAIN			(PAC7620_ADDR_BASE + 0x44)	//RW
#define PAC7620_ADDR_IDLE_S1_STEP_0		(PAC7620_ADDR_BASE + 0x67)	//RW
#define PAC7620_ADDR_IDLE_S1_STEP_1		(PAC7620_ADDR_BASE + 0x68)	//RW
#define PAC7620_ADDR_IDLE_S2_STEP_0		(PAC7620_ADDR_BASE + 0x69)	//RW
#define PAC7620_ADDR_IDLE_S2_STEP_1		(PAC7620_ADDR_BASE + 0x6A)	//RW
#define PAC7620_ADDR_OP_TO_S1_STEP_0	(PAC7620_ADDR_BASE + 0x6B)	//RW
#define PAC7620_ADDR_OP_TO_S1_STEP_1	(PAC7620_ADDR_BASE + 0x6C)	//RW
#define PAC7620_ADDR_OP_TO_S2_STEP_0	(PAC7620_ADDR_BASE + 0x6D)	//RW
#define PAC7620_ADDR_OP_TO_S2_STEP_1	(PAC7620_ADDR_BASE + 0x6E)	//RW
#define PAC7620_ADDR_OPERATION_ENABLE	(PAC7620_ADDR_BASE + 0x72)	//RW

// PAC7620_REGITER_BANK_SEL
#define PAC7620_BANK0		PAC7620_VAL(0,0)
#define PAC7620_BANK1	PAC7620_VAL(1,0)

// PAC7620_ADDR_SUSPEND_CMD
#define PAC7620_I2C_WAKEUP	PAC7620_VAL(1,0)
#define PAC7620_I2C_SUSPEND	PAC7620_VAL(0,0)

// PAC7620_ADDR_OPERATION_ENABLE
#define PAC7620_ENABLE		PAC7620_VAL(1,0)
#define PAC7620_DISABLE		PAC7620_VAL(0,0)

typedef enum {
	BANK0 = 0,
	BANK1,		
} bank_e;

enum {
	// REGISTER 0
	GES_RIGHT_FLAG			 = BIT(0),
	GES_LEFT_FLAG			 = BIT(1),
	GES_UP_FLAG				 = BIT(2),
	GES_DOWN_FLAG			 = BIT(3),
	GES_FORWARD_FLAG		 = BIT(4),
	GES_BACKWARD_FLAG		 = BIT(5),
	GES_CLOCKWISE_FLAG		 = BIT(6),
	GES_COUNT_CLOCKWISE_FLAG = BIT(7),
	//REGISTER 1
	GES_WAVE_FLAG		= BIT(0),	
};
//typedef unsigned short u16;

//cursor mode  Angelo Modify   we only set the register we need
#define R_AE_Exposure_UB    60
#define R_AE_Exposure_LB    (R_AE_Exposure_UB*0.5)

unsigned char init_cursor_array[][2] = {	// Initial cursor
	
    {0xEF,0x00},
	{0x37,0x07},
	{0x38,0x17},
	{0x39,0x06},
	{0x42,0x01},
	{0x46,0x2D},
	{0x47,0x0F},
	{0x48,(unsigned char)((u16)R_AE_Exposure_UB & 0x00FF)},	 //Boy modify @ 2014_1028
    {0x49,(unsigned char)((u16)(R_AE_Exposure_UB & 0xFF00)>>8)},	 //Boy modify @ 2014_1028
    {0x4a,(unsigned char)((u16)R_AE_Exposure_LB & 0x00FF)},	 //Boy modify @ 2014_1028
    {0x4b,(unsigned char)(((u16)R_AE_Exposure_LB & 0xFF00)>>8)},	 //Boy modify @ 2014_1028
	{0x4C,0x20},
	{0x51,0x10},
	{0x5E,0x10},
	{0x60,0x27},
	{0x80,0x42},
	{0x81,0x44},
	{0x82,0x04},
	{0x8B,0x01},
	{0x90,0x06},
	{0x95,0x0A},
	{0x96,0x0C},
	{0x97,0x05},
	{0x9A,0x14},
	{0x9C,0x3F},
	{0xA5,0x19},
	{0xCC,0x19},
	{0xCD,0x0B},
	{0xCE,0x13},
	{0xCF,0x64},
	{0xD0,0x21},
	{0xEF,0x01},
	{0x02,0x0F},
	{0x03,0x10},
	{0x04,0x02},
	{0x25,0x01},
	{0x27,0x39},
	{0x28,0x7F},
	{0x29,0x08},
	{0x3E,0xFF},
	{0x5E,0x3D},
	{0x65,0x96},
	{0x67,0x97},
	{0x69,0xCD},
	{0x6A,0x01},
	{0x6D,0x2C},
	{0x6E,0x01},
	{0x72,0x01},
	{0x73,0x35},
	{0x74,0x00},
	{0x77,0x01},
	
	{0xEF,0x01},
	{0x74,0x03},//R_Control_Mode = cursor mode
	{0xEF,0x00},
	{0x37,0x02},//R_CursorClampLeft[4:0]  modified byl original 0x07
	{0x38,0x1C},//R_CursorClampRight[4:0] modified byl original 0x17
	
	{0x39,0x02},//R_CursorClampUp[4:0]		modified byl original 0x06
	{0x3a,0x1C},//R_CursorClampDown[4:0]	modified byl original 0x12
	{0x8c,0x37},//R_PositionResolution[2:0]
  	{0xef,0x00},
};



#define R_AE_Exposure_UB_for_gesture_mode    60						//Boy modify @ 2014_1031
#define R_AE_Exposure_LB_for_gesture_mode    (R_AE_Exposure_UB_for_gesture_mode*0.5)	//Boy modify @ 2014_1031

//gesture mode Boy modify at 2014_0724
unsigned char init_gesture_array[][2] = {	// Initial Gesture mode
	{0xEF,0x00},
	{0x37,0x07},
	{0x38,0x17},
	{0x39,0x06},
	{0x42,0x01},
	{0x46,0x2D},
	{0x47,0x0F},
	//{0x48,0x3C}, //Boy modify @ 2014_1031
    	//{0x49,0x00}, //Boy modify @ 2014_1031
    	//{0x4a,0x1e}, //Boy modify @ 2014_1031
    	//{0x4b,0x00}, //Boy modify @ 2014_1031
    	{0x48,(unsigned char)((u16)R_AE_Exposure_UB_for_gesture_mode & 0x00FF)},	//Boy modify @ 2014_1031
   	{0x49,(unsigned char)((u16)(R_AE_Exposure_UB_for_gesture_mode & 0xFF00)>>8)},	//Boy modify @ 2014_1031
    	{0x4a,(unsigned char)((u16)R_AE_Exposure_LB_for_gesture_mode & 0x00FF)},	//Boy modify @ 2014_1031
    	{0x4b,(unsigned char)(((u16)R_AE_Exposure_LB_for_gesture_mode & 0xFF00)>>8)},	//Boy modify @ 2014_1031
	{0x4C,0x20},
	{0x51,0x10},
	{0x5E,0x10},
	{0x60,0x27},
	{0x80,0x42},
	{0x81,0x44},
	{0x82,0x04},
	{0x8B,0x01},
	{0x90,0x06},
	{0x95,0x0A},
	{0x96,0x0C},
	{0x97,0x05},
	{0x9A,0x14},
	{0x9C,0x3F},
	{0xA5,0x19},
	{0xCC,0x19},
	{0xCD,0x0B},
	{0xCE,0x13},
	{0xCF,0x64},
	{0xD0,0x21},
	{0xEF,0x01},
	{0x02,0x0F},
	{0x03,0x10},
	{0x04,0x02},
	{0x25,0x01},
	{0x27,0x39},
	{0x28,0x7F},
	{0x29,0x08},
	{0x3E,0xFF},
	{0x5E,0x3D},
	{0x65,0x96},
	{0x67,0x97},
	{0x69,0xCD},
	{0x6A,0x01},
	{0x6D,0x2C},
	{0x6E,0x01},
	{0x72,0x01},
	{0x73,0x35},
	{0x74,0x00},
	{0x77,0x01},
	{0xEF,0x00},
};


#define INIT_CURSOR_SIZE (sizeof(init_cursor_array)/sizeof(init_cursor_array[0]))
#define INIT_GESTURE_SIZE (sizeof(init_gesture_array)/sizeof(init_gesture_array[0]))

