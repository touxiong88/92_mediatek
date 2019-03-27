#include <linux/kernel.h>	/* printk() */
#include <linux/module.h>
#include <linux/types.h>	/* size_t */
#include <linux/slab.h>		/* kmalloc() */
#include <linux/cdev.h>
#include <asm/uaccess.h>   /*set_fs get_fs mm_segment_t*/
#include <linux/flash_info.h>
#include "partition_define.h"
#include <linux/mmc/sd_misc.h>
#include <linux/mutex.h>
#include <linux/time.h>
#include <linux/timex.h>
#include <linux/rtc.h>
#include <linux/crc32.h>

#define USING_XLOG

#ifdef USING_XLOG 
#include <linux/xlog.h>

#define TAG     "FLASH_INFO"

#define flash_err(fmt, args...)       \
    xlog_printk(ANDROID_LOG_ERROR, TAG, fmt, ##args)
#define flash_info(fmt, args...)      \
    xlog_printk(ANDROID_LOG_INFO, TAG, fmt, ##args)

#else

#define TAG     "[FLASH_INFO]"

#define flash_err(fmt, args...)       \
    printk(KERN_ERR TAG);           \
    printk(KERN_CONT fmt, ##args) 
#define flash_info(fmt, args...)      \
    printk(KERN_NOTICE TAG);        \
    printk(KERN_CONT fmt, ##args)

#endif

#define FLASH_INFO_VERSION_SIZE 4

static struct mutex flash_info_lock;
static struct flash_info_header_cache header_cache;
static char flash_info_version[FLASH_INFO_VERSION_SIZE] = "1.0";
static unsigned long long emmc_total_size = 0;
static unsigned long long emmc_user_size = 0;
extern int eMMC_rw_x(loff_t addr,u32 *buffer, int host_num, int iswrite, u32 totalsize, int transtype, Region part);

#define FLASH_INFO_REGION_SIZE     (0x200000)
#define FLASH_INFO_REGION_OFFSET   (0x300000)
#define BLOCK_SIZE 512
#define FLASH_INFO_SIZE 128
#define FLASH_INFO_HEADER_SIZE 512
#define FLASH_INFO_MAGIC_NUM    0x6E695F6873616C66

struct flash_info_region;
struct flash_info_region_ops {
    int (*read_blocks)(struct flash_info_region *region, unsigned int start_blk, unsigned int blk_no, void *blks_buf);
    int (*write_blocks)(struct flash_info_region *region, unsigned int start_blk, unsigned int blk_no, void *blks_buf);
    int (*verify_area)(struct flash_info_region *region, unsigned int start_blk, unsigned int blk_no);
    int (*init)(struct flash_info_region *region);
};

struct flash_info_region {
    unsigned long long base_addr;
    unsigned int size;
    unsigned int init_done;
    struct flash_info_region_ops *ops;
};

struct time_pack {
    unsigned long long second:8;
    unsigned long long minute:8;
    unsigned long long hour:8;
    unsigned long long day:8;
    unsigned long long month:8;
    unsigned long long year:8;
};

static int flash_info_region_verify(struct flash_info_region *region, unsigned int start_blk, unsigned int blk_no)
{
    unsigned int offset = start_blk * BLOCK_SIZE;
    unsigned int size = blk_no * BLOCK_SIZE;

    if ((offset + size) > region->size || offset < 0) {
        flash_err("[%s]:verify region fail.\n", __func__);
        return -1;
    }
    return 0;
}

int flash_info_region_read_blocks(struct flash_info_region *region, unsigned int start_blk, unsigned int blk_no, void *blks_buf)
{
    int err;

#ifdef MTK_NEW_COMBO_EMMC_SUPPORT
    err = eMMC_rw_x(region->base_addr + start_blk * BLOCK_SIZE, (unsigned int *)blks_buf, 0, 0, blk_no * BLOCK_SIZE, 1, EMMC_PART_USER); 
#else
    err = eMMC_rw_x(region->base_addr + start_blk * BLOCK_SIZE, (unsigned int *)blks_buf, 0, 0, blk_no * BLOCK_SIZE, 1, USER); 
#endif
    if(err)
        flash_err("[%s]:base_addr = %llu, start_blk = %u, blk_no = %u fail", __func__, region->base_addr, start_blk, blk_no);
    return err;
}

int flash_info_region_write_blocks(struct flash_info_region *region, unsigned int start_blk, unsigned int blk_no, void *blks_buf)
{
    int err;

#ifdef MTK_NEW_COMBO_EMMC_SUPPORT
    err = eMMC_rw_x(region->base_addr + start_blk * BLOCK_SIZE, (unsigned int *)blks_buf, 0, 1, blk_no * BLOCK_SIZE, 1, EMMC_PART_USER); 
#else
    err = eMMC_rw_x(region->base_addr + start_blk * BLOCK_SIZE, (unsigned int *)blks_buf, 0, 1, blk_no * BLOCK_SIZE, 1, USER); 
#endif
    if(err)
        flash_err("[%s]:base_addr = %llu, start_blk = %u, blk_no = %u fail", __func__, region->base_addr, start_blk, blk_no);
    return err;
}

static void flash_info_region_init(struct flash_info_region *region)
{
    struct storage_info s_info = {0};

    if (!region->init_done) {
        BUG_ON(!msdc_get_info(EMMC_CARD_BOOT, EMMC_CAPACITY, &s_info));
        BUG_ON(!msdc_get_info(EMMC_CARD_BOOT, EMMC_USER_CAPACITY, &s_info));

        emmc_user_size = s_info.emmc_user_capacity * BLOCK_SIZE;
        emmc_total_size = s_info.emmc_capacity * BLOCK_SIZE;
        flash_info("[%s]:emmc_total_size = 0x%llx, user_region_size = 0x%llx\n", __func__,
                emmc_total_size, emmc_user_size);

#ifdef MTK_NEW_COMBO_EMMC_SUPPORT
        region->base_addr = emmc_user_size - FLASH_INFO_REGION_OFFSET;
#else
        region->base_addr = emmc_total_size - FLASH_INFO_REGION_OFFSET - MBR_START_ADDRESS_BYTE;
#endif
        region->init_done = 1;
    }
}
static struct flash_info_region_ops flash_info_ops = {
    .read_blocks = flash_info_region_read_blocks,
    .write_blocks = flash_info_region_write_blocks,
    .verify_area = flash_info_region_verify,
    .init = flash_info_region_init,
};

static struct flash_info_region fi_region = {
    .size = FLASH_INFO_REGION_SIZE,
    .init_done = 0,
    .ops = &flash_info_ops,
};

static int flash_info_read_header(struct flash_info_header_cache *cache)
{
    int err;
    void *blks_buf;

    if (!fi_region.init_done) {
        fi_region.ops->init(&fi_region);
    }

    if (cache->init)
        return 0;

    blks_buf = kzalloc(FLASH_INFO_HEADER_SIZE, GFP_KERNEL);
    if (!blks_buf) {
        err = -ENOMEM;
        flash_err("[%s]:alloc cache for header block fail\n", __func__);
        goto fail_kzalloc;
    }

    err = fi_region.ops->read_blocks(&fi_region, 0, FLASH_INFO_HEADER_SIZE / BLOCK_SIZE, blks_buf);
    if(err) {
        flash_err("[%s]:read header block fail\n", __func__);
        goto out;
    }

    memcpy(cache->header, blks_buf, sizeof(struct flash_info_header));
    cache->init = 1;

out:
    kfree(blks_buf);
fail_kzalloc:
    return err;
}

static int flash_info_write_header(struct flash_info_header_cache *cache)
{
    int err;
    void *blks_buf;

    if (!fi_region.init_done) {
        fi_region.ops->init(&fi_region);
    }

    blks_buf = kzalloc(FLASH_INFO_HEADER_SIZE, GFP_KERNEL);
    if (!blks_buf) {
        err = -ENOMEM;
        flash_err("[%s]:alloc cache for header block fail\n", __func__);
        goto fail_kzalloc;
    }

    memcpy(blks_buf, cache->header, sizeof(struct flash_info_header));
    err = fi_region.ops->write_blocks(&fi_region, 0, FLASH_INFO_HEADER_SIZE / BLOCK_SIZE, blks_buf);
    if(err) {
        flash_err("[%s]:write header block fail\n", __func__);
        goto out;
    }
out:
    kfree(blks_buf);
fail_kzalloc:
    return err;
}

static int flash_info_header_init(struct flash_info_header_cache *cache)
{
    cache->header->magic = FLASH_INFO_MAGIC_NUM;
    memcpy(&cache->header->version, flash_info_version, FLASH_INFO_VERSION_SIZE);
    cache->header->header_size = FLASH_INFO_HEADER_SIZE;
    cache->header->data_size =  FLASH_INFO_REGION_SIZE - FLASH_INFO_HEADER_SIZE;
    cache->header->item_size = FLASH_INFO_SIZE;
    cache->header->index = 0;
    cache->header->crc = 0;
}

static int flash_info_verify_header(struct flash_info_header_cache *cache)
{
    unsigned int origcrc, crc;
    if (cache->header->magic != FLASH_INFO_MAGIC_NUM)
        return -1;
    origcrc = cache->header->crc;
    cache->header->crc = 0;
    crc = crc32(0L, cache->header, sizeof(struct flash_info_header));
    /*
     * It CRC check error, we just print warning message now.
     */
    if (crc != origcrc) {
        flash_err("[%s]:CRC for header is wrong.\n", __func__);
    }
    return 0;
}

static void flash_info_pack(struct flash_info_header_cache *cache, struct flash_info *info)
{
    struct timex txc;
    struct rtc_time tm;
    struct time_pack tpk;
    unsigned char year, month, day, hour, minute, second;

    do_gettimeofday(&(txc.time));
    rtc_time_to_tm(txc.time.tv_sec, &tm);
    flash_info("[%s]:current kernel time : %d-%d-%d-%d-%d-%d\n", __func__, tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
            tm.tm_hour, tm.tm_min, tm.tm_sec);
    tpk.year = (unsigned char)(tm.tm_year % 100);
    tpk.month = (unsigned char)(tm.tm_mon + 1);
    tpk.day = (unsigned char)(tm.tm_mday);
    tpk.hour = (unsigned char)(tm.tm_hour);
    tpk.minute = (unsigned char)(tm.tm_min);
    tpk.second = (unsigned char)(tm.tm_sec);

    info->index = cache->header->index;
    memcpy(&(info->version), flash_info_version, FLASH_INFO_VERSION_SIZE);
    memcpy(&(info->op_time), &tpk, sizeof(unsigned long long));

    return;
}

static int flash_info_append(struct flash_info_header_cache *cache, struct flash_info *info)
{
    int err;
    unsigned int start_blk;
    unsigned int blk_no;
    unsigned int data_block_num;
    unsigned int data_start_block;
    unsigned int info_off;
    void *blks_buf;

    if (!fi_region.init_done) {
        fi_region.ops->init(&fi_region);
    }
    
    data_start_block = cache->header->header_size / BLOCK_SIZE;
    data_block_num = cache->header->data_size / BLOCK_SIZE;

    start_blk = data_start_block + ((cache->header->index * cache->header->item_size) / BLOCK_SIZE) % data_block_num ;
    blk_no = (cache->header->item_size + BLOCK_SIZE - 1) / BLOCK_SIZE;
    info_off = (cache->header->index * cache->header->item_size) % BLOCK_SIZE;

    blks_buf = kzalloc(blk_no * BLOCK_SIZE, GFP_KERNEL);
    if (!blks_buf) {
        err = -ENOMEM;
        flash_err("[%s]:alloc cache for header block fail\n", __func__);
        goto fail_kzalloc;
    }

    err = fi_region.ops->verify_area(&fi_region, start_blk, blk_no);
    if(err) {
        flash_err("[%s]:exceed flash info region range\n", __func__);
        goto out;
    }

    err = fi_region.ops->read_blocks(&fi_region, start_blk, blk_no, blks_buf);
    if(err) {
        flash_err("[%s]:read target block for append flash info fail\n", __func__);
        goto out;
    }

    memcpy(blks_buf + info_off, info, sizeof(struct flash_info));

    err = fi_region.ops->write_blocks(&fi_region, start_blk, blk_no, blks_buf);
    if(err) {
        flash_err("[%s]:write target for append flash info fail\n", __func__);
        goto out;
    }
out:
    kfree(blks_buf);
fail_kzalloc:
    return err;
}

static int __flash_info_update(struct flash_info *info)
{
    int err;

    /*
    * 1) Read back and verify verify header. If the header is vaild, 
    *    update index and write header to emmc. Otherwse, initial header.
    */

    err = flash_info_read_header(&header_cache);
    if (err) {
        flash_err("[%s]:read header fail\n", __func__);
        goto out;
    }

    err = flash_info_verify_header(&header_cache);
    if (err) {
        flash_err("[%s]:verify header fail, header will be initial\n", __func__);
        flash_info_header_init(&header_cache);
    } else {
        header_cache.header->index++;
    }
    header_cache.header->crc = 0;
    header_cache.header->crc = crc32(0L, header_cache.header, sizeof(struct flash_info_header));

    err = flash_info_write_header(&header_cache);
    if (err) {
        flash_err("[%s]:read header fail\n", __func__);
        goto out;
    }

    /*
     * 2) Update some item in flash info. like time/index/version
     */
    flash_info_pack(&header_cache, info);

    /*
     * 3) Add a new flash info to region. If the region is full,  
     *    then replace the oldest order.
     */

    err = flash_info_append(&header_cache, info);
    if (err) {
        flash_err("[%s]:append a new flash info fail.\n", __func__);
        goto out;
    }

    flash_info("[%s]:append flash info, index = %d\n", __func__, header_cache.header->index);
out:
    return err;
}

static int flash_info_update(void __user *arg)
{
    int err;
    struct flash_info *info;
    
    info = kmalloc(sizeof(struct flash_info), GFP_KERNEL);
    if (!info) {
        err = -ENOMEM;
        flash_err("[%s]: allocate info fail\n", __func__);
        goto fail_malloc;
    }
    if (copy_from_user(info, arg, sizeof(struct flash_info))) {
        err = -EFAULT;
        goto out;
    }

    mutex_lock(&flash_info_lock);
    err = __flash_info_update(info);
    mutex_unlock(&flash_info_lock);
out:
    kfree(info);

fail_malloc:
    return err;
}

static long flash_info_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
    long err;
    void __user *argp = (void __user *)arg;

    switch (cmd)
    {
        case FLASH_INFO_UPDATE:
            err = flash_info_update(argp);
            break;
        default:
            err = -EINVAL;
    }
    return err;
}

static unsigned int major;
static struct class *flash_info_class;
static struct cdev *flash_info_cdev;
static struct file_operations flash_info_cdev_ops = {
    .owner = THIS_MODULE,
    .unlocked_ioctl = flash_info_ioctl,
};

static void create_flash_info_cdev(void)
{
    int err;
    dev_t devno;
    struct device *flash_info_dev;

    err = alloc_chrdev_region(&devno, 0, 1, "flash_info");
    if (err) {
        flash_err("[%s]fail to alloc devno\n", __func__);
        goto fail_alloc_devno;
    }
    
    major = MAJOR(devno);

    flash_info_cdev = cdev_alloc();
    if (!flash_info_cdev) {
        flash_err("[%s]fail to alloc cdev\n", __func__);
        goto fail_alloc_cdev;
    }

    flash_info_cdev->owner = THIS_MODULE;
    flash_info_cdev->ops = &flash_info_cdev_ops;

    err = cdev_add(flash_info_cdev, devno, 1);
    if (err) {
        flash_err("[%s]fail to add cdev\n", __func__);
        goto fail_add_cdev;
    }

    flash_info_class = class_create(THIS_MODULE, "flash_info");
    if (IS_ERR(flash_info_class)) {
        flash_err("[%s]fail to create class flash_info\n", __func__);
        goto fail_create_class;
    }
    
    flash_info_dev = device_create(flash_info_class, NULL, devno, NULL, "flash_info");
    if (IS_ERR(flash_info_dev)) {
        flash_err("[%s]fail to create class flash_info\n", __func__);
        goto fail_create_device;
    }

    return;

fail_create_device:
    class_destroy(flash_info_class);
fail_create_class:
fail_add_cdev:
    cdev_del(flash_info_cdev);
fail_alloc_cdev:
    unregister_chrdev_region(devno, 1);
fail_alloc_devno:
    return;
}

static void remove_flash_info_cdev(void)
{
    device_destroy(flash_info_class, MKDEV(major, 0));
    class_destroy(flash_info_class);
    cdev_del(flash_info_cdev);
    unregister_chrdev_region(MKDEV(major, 0), 1);
}

static int alloc_header_cache(void)
{
    int err;
    header_cache.header = kzalloc(sizeof(struct flash_info_header), GFP_KERNEL);
    if (!header_cache.header) {
        err = -ENOMEM;
        flash_err("[%s] allocate cache for header fail", __func__);
    }
    header_cache.init = 0;
    return err;
}

static void free_header_cache(void)
{
    if (header_cache.header)
        kfree(header_cache.header);
}

static int __init flash_info_init(void)
{
    create_flash_info_cdev();
    mutex_init(&flash_info_lock);
    alloc_header_cache();
    return 0;
}

static void __exit flash_info_exit(void)
{
    remove_flash_info_cdev();
    free_header_cache();
}

module_init(flash_info_init);
module_exit(flash_info_exit);

MODULE_LICENSE("GPL");
MODULE_DESCRIPTION("MediaTek Flash Info Logger Driver");
MODULE_AUTHOR("jian.Lin <Jian.lin@mediatek.com>");
