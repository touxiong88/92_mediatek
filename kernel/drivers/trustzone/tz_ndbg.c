
#include "tz_ndbg.h"

#ifdef CC_ENABLE_NDBG

#include <linux/module.h>
#include <linux/types.h>
#include <linux/slab.h>
#include <linux/delay.h>
#include <linux/kthread.h>
#include <linux/fs.h>
#include <asm/uaccess.h>
#include "tz_cross/trustzone.h"
#include "tz_cross/ta_test.h"
#include "tz_cross/ta_mem.h"
#include "tz_cross/ta_ndbg.h"
#include "trustzone/kree/system.h"
#include "trustzone/kree/mem.h"
#include "kree_int.h"

extern int g_bat_init_flag;

extern int sec_get_random_id(unsigned int *rid);

int battery_meter_get_battery_voltage(void);
int battery_meter_get_charger_voltage(void);
int battery_meter_get_VSense(void);

#define ES_CTR_BASE     (0xF0009000)
#define HRID0   (ES_CTR_BASE + 0x140)
#define HRID1   (ES_CTR_BASE + 0x144)

#define ENTROPY_SIZE    16      // in byte
#define DEVICE_ID_LEN   16      // in byte
#define URANDOM_LEN		8		// in byte

static int get_bat_sense_volt(int times)
{
	if(g_bat_init_flag != 0)
	{
	    return battery_meter_get_battery_voltage();
	}

	return 0;
}

static int get_i_sense_volt(int times)
{
	if(g_bat_init_flag != 0)
	{
	    return battery_meter_get_VSense();
	}

	return 0;
}

static int get_charger_volt(int times)
{
	if(g_bat_init_flag != 0)
	{
	    return battery_meter_get_charger_voltage();
	}
	
	return 0;
}

static int get_urandom_value(uint8_t *pu1Buf, int size)
{
	mm_segment_t old_fs;
	struct file *filp = filp_open("/dev/urandom", O_RDONLY, 0444);
	if( IS_ERR(filp) )
	{
		printk("Failed to open /dev/urandom\n");
		return -1;
	}

	old_fs = get_fs();
	set_fs( KERNEL_DS );
	filp->f_op->read(filp, pu1Buf, size, &(filp->f_pos));
    set_fs(old_fs);

	filp_close(filp, NULL);

	return 0;
}

int entropy_thread(void * arg)
{
    TZ_RESULT ret;
    KREE_SESSION_HANDLE ndbg_session;
    KREE_SESSION_HANDLE mem_session;
    KREE_SHAREDMEM_HANDLE shm_handle;
    KREE_SHAREDMEM_PARAM shm_param;
    MTEEC_PARAM param[4];
    uint32_t *ptr;
    int size = ENTROPY_SIZE + DEVICE_ID_LEN + URANDOM_LEN;
	
    ptr = (uint32_t *)kmalloc(size, GFP_KERNEL);
    memset(ptr, 0, size);

    while(!kthread_should_stop())
    {
        ret = KREE_CreateSession(TZ_TA_NDBG_UUID, &ndbg_session);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("CreateSession error %d\n", ret);
            return 1;
        }

        ret = KREE_CreateSession(TZ_TA_MEM_UUID, &mem_session);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("Create memory session error %d\n", ret);
            return 1;
        }

        shm_param.buffer = ptr;
        shm_param.size = size;
        ret = KREE_RegisterSharedmem(mem_session, &shm_handle, &shm_param);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("KREE_RegisterSharedmem Error: %s\n", TZ_GetErrorString(ret));
            return 1;
        }

        ptr[0] = get_bat_sense_volt(1);
        ptr[1] = get_i_sense_volt(1);
        ptr[2] = get_charger_volt(1);
        ptr[3] = get_charger_volt(1);

        //printk("Voltage Entropy: 0x%x 0x%x 0x%x 0x%x\n", ptr[0], ptr[1], ptr[2], ptr[3]);

        ptr[4] = (*(volatile unsigned int * const)(HRID0));
        ptr[5] = (*(volatile unsigned int * const)(HRID1));


		get_urandom_value((uint8_t *)&ptr[6], sizeof(uint32_t) * 2);

        //printk("Nonce: 0x%x 0x%x 0x%x 0x%x\n", ptr[4], ptr[5], ptr[6], ptr[7]);

        param[0].memref.handle = (uint32_t) shm_handle;
        param[0].memref.offset = 0;
        param[0].memref.size = size/4; // in 32 bits
        param[1].value.a = size/4;

        ret = KREE_TeeServiceCall((KREE_SESSION_HANDLE)ndbg_session,
                TZCMD_NDBG_INIT,
                TZ_ParamTypes3(TZPT_MEMREF_INPUT, TZPT_VALUE_INPUT, TZPT_VALUE_OUTPUT), param);
        //printk ("KREE NDBG Init result = 0x%x\n", param[2].value.a);

        printk ("Start to wait reseed.\n");
        ret = KREE_TeeServiceCall((KREE_SESSION_HANDLE)ndbg_session,
                TZCMD_NDBG_WAIT_RESEED,
                TZ_ParamTypes3(TZPT_MEMREF_INPUT, TZPT_VALUE_INPUT, TZPT_VALUE_OUTPUT), param);
        printk ("OK to send reseed.\n");

        ret = KREE_UnregisterSharedmem(mem_session, shm_handle);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("KREE_UnregisterSharedmem Error: %s\n", TZ_GetErrorString(ret));
            return 1;
        }

        ret = KREE_CloseSession(ndbg_session);
        if (ret != TZ_RESULT_SUCCESS)
            printk("CloseSession error %d\n", ret);

        ret = KREE_CloseSession(mem_session);
        if (ret != TZ_RESULT_SUCCESS)
            printk("Close memory session error %d\n", ret);

    }

    kfree(ptr);

    return 0;
}

#ifdef CC_NDBG_TEST_PROGRAM
int test_random_thread(void * arg)
{
    TZ_RESULT ret;
    KREE_SESSION_HANDLE ndbg_session;
    KREE_SESSION_HANDLE mem_session;
    KREE_SHAREDMEM_HANDLE shm_handle;
    KREE_SHAREDMEM_PARAM shm_param;
    MTEEC_PARAM param[4];
    uint32_t *ptr;
    int size = ENTROPY_SIZE + DEVICE_ID_LEN + URANDOM_LEN;

    ptr = (uint32_t *)kmalloc(size, GFP_KERNEL);
    memset(ptr, 0, size);

    while(!kthread_should_stop())
    {
        ret = KREE_CreateSession(TZ_TA_NDBG_UUID, &ndbg_session);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("CreateSession error %d\n", ret);
            return 1;
        }

        ret = KREE_CreateSession(TZ_TA_MEM_UUID, &mem_session);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("Create memory session error %d\n", ret);
            return 1;
        }

        shm_param.buffer = ptr;
        shm_param.size = size;
        ret = KREE_RegisterSharedmem(mem_session, &shm_handle, &shm_param);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("KREE_RegisterSharedmem Error: %s\n", TZ_GetErrorString(ret));
            return 1;
        }

        param[0].memref.handle = (uint32_t) shm_handle;
        param[0].memref.offset = 0;
        param[0].memref.size = size/4;
        param[1].value.a = size/4;

        ret = KREE_TeeServiceCall((KREE_SESSION_HANDLE)ndbg_session,
                TZCMD_NDBG_RANDOM,
                TZ_ParamTypes3(TZPT_MEMREF_INPUT, TZPT_VALUE_INPUT, TZPT_VALUE_OUTPUT), param);
        printk ("KREE NDBG Random test result = 0x%x\n", param[2].value.a);

        ret = KREE_UnregisterSharedmem(mem_session, shm_handle);
        if (ret != TZ_RESULT_SUCCESS)
        {
            printk("KREE_UnregisterSharedmem Error: %s\n", TZ_GetErrorString(ret));
            return 1;
        }

        ret = KREE_CloseSession(ndbg_session);
        if (ret != TZ_RESULT_SUCCESS)
            printk("CloseSession error %d\n", ret);

        ret = KREE_CloseSession(mem_session);
        if (ret != TZ_RESULT_SUCCESS)
            printk("Close memory session error %d\n", ret);

        ssleep(5);
    }

    kfree(ptr);

    return 0;
}
#endif

void tz_ndbg_init(void)
{
    kthread_run(entropy_thread, NULL, "entropy_thread");
	#ifdef CC_NDBG_TEST_PROGRAM
    kthread_run(test_random_thread, NULL, "test_random_thread");
    #endif
}

#endif

