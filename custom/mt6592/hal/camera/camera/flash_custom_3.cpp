#include "camera_custom_nvram.h"
#include "camera_custom_types.h"

#include "camera_custom_AEPlinetable.h"
#include "camera_custom_nvram.h"

#include <cutils/xlog.h>
#include "flash_feature.h"
#include "flash_param.h"
#include "flash_tuning_custom.h"
#include <kd_camera_feature.h>
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
int cust_getPrecapAfMode()
{
    XLOGD("cust_getPrecapAfMode");
//	cust_getPrecapAfMode£º
//    e_PrecapAf_None,   //²»×öprecapture AF
//    e_PrecapAf_BeforePreflash, //×öprecapture AFÔÚpreflashÇ°
//    e_PrecapAf_AfterPreflash,  //×öprecapture AFÔÚpreflashºó
    
    //return e_PrecapAf_AfterPreflash;
  	//return  e_PrecapAf_BeforePreflash;
    return e_PrecapAf_None;
}


int cust_isNeedDoPrecapAF_v2(int isFocused, int flashMode, int afLampMode, int isBvLowerTriger)
{
    XLOGD("cust_isNeedDoPrecapAF_v2: isFocused=%d flashMode=%d afLampMode=%d isBvLowerTriger=%d",isFocused, flashMode, afLampMode, isBvLowerTriger);
    if(flashMode == LIB3A_FLASH_MODE_FORCE_ON)	//¿ªÉÁ¹âµÆÊ±Ô¤ÉÁ¶Ô½¹
    {
        return 1;
    }
    else if(flashMode == LIB3A_FLASH_MODE_FORCE_OFF)//¹ØÉÁ¹âµÆÊ±²»Ô¤ÉÁ¶Ô½¹
    {
        return 0;
    }
    else
    {
	    if(isBvLowerTriger==1)
	        return 1;
	    else
	        return 0;
    }
}

