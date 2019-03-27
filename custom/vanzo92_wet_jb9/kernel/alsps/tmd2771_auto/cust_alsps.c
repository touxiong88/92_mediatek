#include <linux/types.h>
#include <mach/mt_pm_ldo.h>
#include <cust_alsps.h>

static struct alsps_hw cust_alsps_hw = {
    .i2c_num    = 2,
    .polling_mode_ps =0,
    .polling_mode_als =1,
    .power_id   = MT65XX_POWER_NONE,    /*LDO is not used*/
    .power_vol  = VOL_DEFAULT,          /*LDO is not used*/
    .i2c_addr   = {0x72, 0x48, 0x78, 0x00},
    /*Lenovo-sw chenlj2 add 2011-06-03,modify parameter below two lines*/
    .als_level  = { 3, 40,  80,   120,   160, 250,  400, 800, 1200,  1600, 2000, 3000, 5000, 10000, 65535},
    .als_value  = { 16, 140, 540,  1000,  1400, 2000, 2500, 3000, 4000, 8000, 10000,  10240, 6000, 9000,  10240, 10240},
    .ps_threshold_high = 700,
    .ps_threshold_low = 500,
    .ps_threshold = 500,
};
struct alsps_hw *TMD2771_get_cust_alsps_hw(void) {
    return &cust_alsps_hw;
}
int TMD2771_CMM_PPCOUNT_VALUE = 0x0B;
int ZOOM_TIME = 4;
int TMD2771_CMM_CONTROL_VALUE = 0x20;
