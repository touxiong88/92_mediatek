typedef enum FM36_EFFECT_MODE_TAG
{
	fm36_STATE_SLEEP=0,
	fm36_STATE_EFFECT_8K_HSNS_ENABLE,				//handset mode
	fm36_STATE_EFFECT_8K_HFNS_ENABLE,				//handfree mode
	fm36_STATE_EFFECT_8K_HSWexin_ENABLE,			//QQ  weixin Record mode
	fm36_STATE_EFFECT_8K_HFFFP_ENABLE,				//Con-Call mode
	fm36_STATE_EFFECT_8K_HSRecord_ENABLE,			//FFP Record mode
	fm36_STATE_EFFECT_8K_Record_General_ENABLE,     //General Record mode
}FM36_EFFECT_MODETAG;
typedef struct {
	int regaddr;
	int regdata;
} fm36_reg_struct;

/**************************************

  narrow band handset with noise suppression

 ***************************************/
fm36_reg_struct FM36_8K_HSNSon_para[]=
{
	{0x2301, 0x0000},
	{0x22F8, 0x8000},
	{0x2305, 0x0000},
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2269, 0x1F40},
	{0x226A, 0xC65D},
	{0x2268, 0x1F40},
	{0x22FD, 0x00DE},
	{0x2288, 0x0000},
	{0x22B9, 0x1006},
	{0x2303, 0x0700},
	{0x2304, 0x4232},
	{0x230C, 0x00B0},
	{0x2310, 0x0001},
	{0x236E, 0x0000},
	{0x236F, 0x0001},
	{0x2375, 0x7333},
	{0x2376, 0x6666},
	{0x2377, 0x599A},
	{0x237E, 0x0001},
	{0x237F, 0x4000},
	{0x2380, 0x2800},
	{0x2381, 0x0005},
	{0x2382, 0x0060},
	{0x2383, 0x7FFF},
	{0x2388, 0x3000},
	{0x2389, 0x2400},
	{0x238B, 0x1800},
	{0x238C, 0x1800},
	{0x238F, 0x2000},
	{0x239B, 0x0002},
	{0x239C, 0x0A00},
	{0x239F, 0x0001},
	{0x2295, 0x0000},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2299, 0x7FFF},
	{0x22B2, 0x0000},
	{0x2282, 0x0000},
	{0x2283, 0x0001},
	{0x22E5, 0x00A0},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2274, 0x0000},
	{0x22FA, 0x007F},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x2278, 0xFCFC},
	{0x232F, 0x0100},
	{0x232C, 0x0000},
	{0x2298, 0x0000},
	{0x22BA, 0x1006},
	{0x22FC, 0x6000},
	{0x232B, 0x0030},
	{0x22EE, 0x8000},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2211},
	{0x2396, 0x1000},
	{0x2397, 0x0FFF},
	{0x2398, 0xFFFF},
	{0x23A1, 0x0000},
	{0x22FB, 0x0000}
};

/**************************************

  narrow band hand-free with noise suppression

 ***************************************/
fm36_reg_struct FM36_8K_HFNSon_para[]=
{
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x1F40},
	{0x2269, 0x1F40},
	{0x226A, 0xC65D},
	{0x2288, 0x0000},
	{0x2295, 0x0000},
	{0x22B9, 0x1006},
	{0x22F8, 0x8001},
	{0x22FD, 0x00DE},
	{0x2301, 0x0000},
	{0x2304, 0xC232},
	{0x2305, 0x0000},
	{0x230C, 0x0200},
	{0x2310, 0x0001},
	{0x237E, 0x0001},
	{0x239B, 0x0002},
	{0x239C, 0x0A00},
	{0x239F, 0x0001},
	{0x22BA, 0x1006},
	{0x2278, 0x0404},
	{0x2303, 0x0F00},
	{0x232C, 0x0000},
	{0x232F, 0x0200},
	{0x2362, 0x1000},
	{0x236B, 0x0000},
	{0x236C, 0x0078},
	{0x236D, 0x0000},
	{0x236E, 0x0001},
	{0x2371, 0x000F},
	{0x2373, 0x0000},
	{0x2374, 0x7FFF},
	{0x2375, 0x599A},
	{0x2376, 0x47AE},
	{0x2377, 0x3958},
	{0x2379, 0x2AAB},
	{0x237A, 0x4000},
	{0x237C, 0x0040},
	{0x2388, 0x3800},
	{0x2389, 0x3800},
	{0x238A, 0x3800},
	{0x238B, 0x2000},
	{0x238C, 0x2000},
	{0x23A2, 0x0800},
	{0x23AE, 0x7FFF},
	{0x23AF, 0x7FFF},
	{0x23B0, 0x7FFF},
	{0x23B1, 0x7FFF},
	{0x23B2, 0x7FFF},
	{0x23B3, 0x7FFF},
	{0x23B4, 0x7FFF},
	{0x23B5, 0x7FFF},
	{0x23B6, 0x7FFF},
	{0x23B7, 0x7FFF},
	{0x23B8, 0x7FFF},
	{0x23B9, 0x7FFF},
	{0x23BA, 0x7FFF},
	{0x23BB, 0x7FFF},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2298, 0x7FFF},
	{0x2299, 0x0000},
	{0x22F1, 0xD000},
	{0x22B2, 0x0000},
	{0x2282, 0x0001},
	{0x2283, 0x0000},
	{0x22E5, 0x00A0},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2274, 0x0002},
	{0x22FA, 0x007F},
	{0x23AD, 0x7FFF},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x22FC, 0x6000},
	{0x232B, 0x0050},
	{0x22EE, 0x8000},
	{0x2309, 0x0400},
	{0x23A1, 0x0000},
	{0x2355, 0x2000},
	{0x2356, 0x2000},
	{0x2357, 0x2000},
	{0x2358, 0x2000},
	{0x2359, 0x2000},
	{0x235A, 0x2000},
	{0x235B, 0x2000},
	{0x235C, 0x2000},
	{0x235D, 0x2000},
	{0x235E, 0x2000},
	{0x235F, 0x2000},
	{0x2361, 0x2000},
	{0x23A8, 0x0800},
	{0x23A9, 0x1000},
	{0x23AA, 0x0200},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2212},
	{0x2396, 0x2334},
	{0x2397, 0x4FFF},
	{0x2398, 0xFFFF},
	{0x23AB, 0x4000},
	{0x22FB, 0x0000}
};


/**************************************

  wide band handset used for ΢�� and VR

 ***************************************/

fm36_reg_struct FM36_Wexin_HSNSon_para[]=
{
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x3E80},
	{0x2269, 0x3E80},
	{0x226A, 0xC65D},
	{0x2288, 0x0000},
	{0x2295, 0x0000},
	{0x22B9, 0x1006},
	{0x22F8, 0x8003},
	{0x22FD, 0x00DE},
	{0x2301, 0x0002},
	{0x2304, 0xC232},
	{0x2305, 0x0000},
	{0x230C, 0x0600},
	{0x2310, 0x0001},
	{0x237E, 0x0001},
	{0x239B, 0x0002},
	{0x239C, 0x0A00},
	{0x239F, 0x0001},
	{0x22BA, 0x1006},
	{0x2278, 0x0404},
	{0x2303, 0x0F00},
	{0x232C, 0x0000},
	{0x232F, 0x0200},
	{0x2362, 0x1000},
	{0x236B, 0x0000},
	{0x236C, 0x0078},
	{0x236D, 0x0000},
	{0x236E, 0x0001},
	{0x2371, 0x000F},
	{0x2373, 0x0000},
	{0x2374, 0x7FFF},
	{0x2375, 0x599A},
	{0x2376, 0x47AE},
	{0x2377, 0x3958},
	{0x2379, 0x2AAB},
	{0x237A, 0x4000},
	{0x237C, 0x0040},
	{0x2388, 0x3800},
	{0x2389, 0x3800},
	{0x238A, 0x3800},
	{0x238B, 0x2000},
	{0x238C, 0x2000},
	{0x23A2, 0x0800},
	{0x23AE, 0x7FFF},
	{0x23AF, 0x7FFF},
	{0x23B0, 0x7FFF},
	{0x23B1, 0x7FFF},
	{0x23B2, 0x7FFF},
	{0x23B3, 0x7FFF},
	{0x23B4, 0x7FFF},
	{0x23B5, 0x7FFF},
	{0x23B6, 0x7FFF},
	{0x23B7, 0x7FFF},
	{0x23B8, 0x7FFF},
	{0x23B9, 0x7FFF},
	{0x23BA, 0x7FFF},
	{0x23BB, 0x7FFF},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2298, 0x3FFF},
	{0x2299, 0x3FFF},
	{0x22F1, 0xD000},
	{0x22B2, 0x0000},
	{0x2282, 0x0001},
	{0x2283, 0x0000},
	{0x22E5, 0x00A0},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2274, 0x0002},
	{0x22FA, 0x007F},
	{0x23AD, 0x7FFF},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x22FC, 0x6000},
	{0x232B, 0x0050},
	{0x22EE, 0x8000},
	{0x2309, 0x0800},
	{0x23A1, 0x0000},
	{0x2355, 0x2000},
	{0x2356, 0x2000},
	{0x2357, 0x2000},
	{0x2358, 0x2000},
	{0x2359, 0x2000},
	{0x235A, 0x2000},
	{0x235B, 0x2000},
	{0x235C, 0x2000},
	{0x235D, 0x2000},
	{0x235E, 0x2000},
	{0x235F, 0x2000},
	{0x2361, 0x2000},
	{0x23A8, 0x4000},
	{0x23A9, 0x2000},
	{0x23AA, 0x0200},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2212},
	{0x2396, 0x2334},
	{0x2397, 0x4444},
	{0x2398, 0x4444},
	{0x23AB, 0x4000},
	{0x22FB, 0x0000}
};

/**************************************

  wide band con-call BFNSon for Recorder

 ***************************************/
fm36_reg_struct FM36_Recoder_Con_call_para[]=
{
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x3E80},
	{0x2269, 0x3E80},
	{0x226A, 0xC65D},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2274, 0x0000},
	{0x2278, 0xF4F4},
	{0x2282, 0x0000},
	{0x2283, 0x0001},
	{0x2288, 0x0000},
	{0x2295, 0x0000},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2298, 0x7FFF},
	{0x2299, 0x0000},
	{0x22B2, 0x0000},
	{0x22B9, 0x1006},
	{0x22BA, 0x1006},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x22E5, 0x00A0},
	{0x22EE, 0x8000},
	{0x22F1, 0xD000},
	{0x22F8, 0x8003},
	{0x22FA, 0x007F},
	{0x22FC, 0x6000},
	{0x22FD, 0x00DE},
	{0x2301, 0x0002},
	{0x2303, 0x0600},
	{0x2304, 0xC032},
	{0x2305, 0x0000},
	{0x2309, 0x0000},
	{0x230C, 0x0200},
	{0x2310, 0x0001},
	{0x236E, 0x0001},
	{0x2371, 0x000F},
	{0x2373, 0x0000},
	{0x2374, 0x7FFF},
	{0x2375, 0x599A},
	{0x2376, 0x47AE},
	{0x2377, 0x3958},
	{0x2379, 0x2AAB},
	{0x237A, 0x4000},
	{0x237C, 0x0010},
	{0x237E, 0x0001},
	{0x2388, 0x7FFF},
	{0x2389, 0x1000},
	{0x238A, 0x0200},
	{0x238B, 0x1000},
	{0x238C, 0x1000},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2222},
	{0x2396, 0x2233},
	{0x2397, 0x4444},
	{0x2398, 0x4444},
	{0x239B, 0x0000},
	{0x239C, 0x0800},
	{0x239F, 0x0001},
	{0x23A1, 0x0000},
	{0x23A2, 0x0400},
	{0x23A8, 0x4000},
	{0x23A9, 0x1000},
	{0x23AA, 0x0200},
	{0x23AB, 0x4000},
	{0x22FB, 0x0000}
};

/**************************************

  wide band Far Field Pickup for Recorder

 ***************************************/
fm36_reg_struct FM36_Record_FFP_para[]=
{
	{0x2264, 0x01A0},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x3E80},
	{0x2269, 0x3E80},
	{0x226A, 0xC65D},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2278, 0xF4F4},
	{0x2282, 0x0000},
	{0x2283, 0x0001},
	{0x2288, 0x0000},
	{0x22B9, 0x1025},
	{0x22BA, 0x1026},
	{0x22E5, 0x00A0},
	{0x22E8, 0x0000},
	{0x22E9, 0x0001},
	{0x22EA, 0x0001},
	{0x22EE, 0x8000},
	{0x22F2, 0x004C},
	{0x22F8, 0x8004},
	{0x22FA, 0x007F},
	{0x22FD, 0x00C2},
	{0x2301, 0x0002},
	{0x2303, 0x0200},
	{0x2304, 0x8232},
	{0x2305, 0x0000},
	{0x230C, 0x0200},
	{0x230D, 0x0200},
	{0x236E, 0x0001},
	{0x2386, 0x0500},
	{0x2387, 0x0200},
	{0x238B, 0x1000},
	{0x238C, 0x1000},
	{0x2391, 0x7E80},
	{0x239D, 0x0000},
	{0x239E, 0x7FFF},
	{0x23A4, 0x2000},
	{0x23A5, 0x2800},
	{0x23A6, 0x0300},
	{0x23A7, 0x7C00},
	{0x23A8, 0x4000},
	{0x23A9, 0x2800},
	{0x23AA, 0x0300},
	{0x23AB, 0x7C00},
	{0x23C4, 0x1000},
	{0x23C5, 0x7E80},
	{0x23C7, 0x0500},
	{0x23C8, 0x0200},
	{0x22FB, 0x0000}
};

/**************************************

  wide band General NS&FFP off for Recorder

 ***************************************/
fm36_reg_struct FM36_Record_General_para[]=
{
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x3E80},
	{0x2269, 0x3E80},
	{0x226A, 0xC65D},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2274, 0x0000},
	{0x2278, 0xF4F4},
	{0x2282, 0x0000},
	{0x2283, 0x0001},
	{0x2288, 0x0000},
	{0x2295, 0x0000},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2298, 0x7FFF},
	{0x2299, 0x0000},
	{0x22B2, 0x0000},
	{0x22B9, 0x1006},
	{0x22BA, 0x1006},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x22E5, 0x00A0},
	{0x22EE, 0x8000},
	{0x22F1, 0xD000},
	{0x22F8, 0x8003},
	{0x22FA, 0x007F},
	{0x22FC, 0x6000},
	{0x22FD, 0x00DE},
	{0x2301, 0x0002},
	{0x2303, 0x8000},
	{0x2304, 0x4032},
	{0x2305, 0x0000},
	{0x2309, 0x0000},
	{0x230C, 0x0290},
	{0x2310, 0x0001},
	{0x236E, 0x0001},
	{0x2371, 0x000F},
	{0x2373, 0x0000},
	{0x2374, 0x7FFF},
	{0x2375, 0x599A},
	{0x2376, 0x47AE},
	{0x2377, 0x3958},
	{0x2379, 0x2AAB},
	{0x237A, 0x4000},
	{0x237C, 0x0010},
	{0x237E, 0x0001},
	{0x2388, 0x7FFF},
	{0x2389, 0x1000},
	{0x238A, 0x0200},
	{0x238B, 0x1000},
	{0x238C, 0x1000},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2222},
	{0x2396, 0x2233},
	{0x2397, 0x4444},
	{0x2398, 0x4444},
	{0x239B, 0x0000},
	{0x239C, 0x0800},
	{0x239F, 0x0001},
	{0x23A1, 0x0000},
	{0x23A2, 0x0400},
	{0x23A8, 0x4000},
	{0x23A9, 0x2000},
	{0x23AA, 0x0300},
	{0x22FB, 0x0000}
};


/**************************************

  Test mode for MIC0

 ***************************************/
fm36_reg_struct FM36_Test_MIC0_para[]=
{
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x3E80},
	{0x2269, 0x3E80},
	{0x226A, 0xC65D},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2278, 0xF800},
	{0x2282, 0x0000},
	{0x2283, 0x0001},
	{0x2288, 0x0000},
	{0x2295, 0x0000},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2298, 0x0000},
	{0x2299, 0x7FFF},
	{0x22B2, 0x0000},
	{0x22B9, 0x1006},
	{0x22BA, 0x1006},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x22E5, 0x00A0},
	{0x22EE, 0x8000},
	{0x22F1, 0xD000},
	{0x22F8, 0x8005},
	{0x22FA, 0x007F},
	{0x22FC, 0x6000},
	{0x22FD, 0x00DE},
	{0x2301, 0x0002},
	{0x2302, 0x0001},
	{0x2303, 0x0700},
	{0x2304, 0x4232},
	{0x2305, 0x0000},
	{0x2309, 0x0800},
	{0x230C, 0x0080},
	{0x2310, 0x0001},
	{0x232B, 0x0030},
	{0x232C, 0x0000},
	{0x232F, 0x0200},
	{0x2362, 0x1000},
	{0x236B, 0x0000},
	{0x236C, 0x00B4},
	{0x236D, 0x0000},
	{0x236E, 0x0001},
	{0x2375, 0x599A},
	{0x2376, 0x47AE},
	{0x2377, 0x3958},
	{0x237C, 0x0040},
	{0x237E, 0x0001},
	{0x2388, 0x3800},
	{0x2389, 0x3800},
	{0x238A, 0x3800},
	{0x238B, 0x2000},
	{0x238C, 0x2000},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2212},
	{0x2396, 0x2334},
	{0x2397, 0x4FFF},
	{0x2398, 0xFFFF},
	{0x239B, 0x0002},
	{0x239C, 0x0A00},
	{0x239F, 0x0001},
	{0x23A1, 0x4000},
	{0x22FB, 0x0000}
};


/**************************************

  Test mode for MIC1

 ***************************************/
fm36_reg_struct FM36_Test_MIC1_para[]=
{
	{0x2264, 0x0180},
	{0x2266, 0x0033},
	{0x2267, 0x3E80},
	{0x2268, 0x3E80},
	{0x2269, 0x3E80},
	{0x226A, 0xC65D},
	{0x226C, 0x007D},
	{0x226D, 0x0000},
	{0x226E, 0x001A},
	{0x2278, 0xF800},
	{0x2282, 0x0001},
	{0x2283, 0x0000},
	{0x2288, 0x0000},
	{0x2295, 0x0000},
	{0x2296, 0x0002},
	{0x2297, 0x0003},
	{0x2298, 0x0000},
	{0x2299, 0x7FFF},
	{0x22B2, 0x0000},
	{0x22B9, 0x1006},
	{0x22BA, 0x1006},
	{0x22D7, 0x0000},
	{0x22D8, 0x0001},
	{0x22E5, 0x00A0},
	{0x22EE, 0x8000},
	{0x22F1, 0xD000},
	{0x22F8, 0x8005},
	{0x22FA, 0x007F},
	{0x22FC, 0x6000},
	{0x22FD, 0x00DE},
	{0x2301, 0x0002},
	{0x2302, 0x0001},
	{0x2303, 0x0700},
	{0x2304, 0x4232},
	{0x2305, 0x0000},
	{0x2309, 0x0800},
	{0x230C, 0x0080},
	{0x2310, 0x0001},
	{0x232B, 0x0030},
	{0x232C, 0x0000},
	{0x232F, 0x0200},
	{0x2362, 0x1000},
	{0x236B, 0x0000},
	{0x236C, 0x00B4},
	{0x236D, 0x0000},
	{0x236E, 0x0001},
	{0x2375, 0x599A},
	{0x2376, 0x47AE},
	{0x2377, 0x3958},
	{0x237C, 0x0040},
	{0x237E, 0x0001},
	{0x2388, 0x3800},
	{0x2389, 0x3800},
	{0x238A, 0x3800},
	{0x238B, 0x2000},
	{0x238C, 0x2000},
	{0x2393, 0x6655},
	{0x2394, 0x4433},
	{0x2395, 0x2212},
	{0x2396, 0x2334},
	{0x2397, 0x4FFF},
	{0x2398, 0xFFFF},
	{0x239B, 0x0002},
	{0x239C, 0x0A00},
	{0x239F, 0x0001},
	{0x23A1, 0x4000},
	{0x22FB, 0x0000}
};
