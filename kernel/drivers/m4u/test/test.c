
#include <linux/version.h>
#include <linux/uaccess.h>
#include <linux/module.h>
#include <linux/miscdevice.h>
#include <linux/fs.h>
#include <linux/platform_device.h>
#include <linux/cdev.h>
#include <linux/interrupt.h>
#include <asm/io.h>
#include <linux/sched.h>
#include <linux/wait.h>
#include <linux/spinlock.h>
#include <linux/delay.h>
#include <linux/earlysuspend.h>
#include <linux/mm.h>
#include <linux/vmalloc.h>
#include <linux/dma-mapping.h>
#include <linux/slab.h>
#include <linux/aee.h>
#include <linux/timer.h>
#include <linux/cache.h>
#include <linux/xlog.h>
#include <linux/mm.h>
#include <linux/pagemap.h>
#include <linux/m4u_profile.h>


#define ktest_func_enter() printk("[ktest] func enter: %s\n", __func__)

static dev_t ktest_dev_num;
#define KTEST_DEV_NAME "ktest"
static struct cdev *ktest_cdev;
static struct miscdevice *ktest_mdev;


typedef struct __kmem__
{
    struct page *page;
    unsigned int size;
    int* va;
}kmem_t;

kmem_t *g_kmem;

typedef struct ktest_priv
{
    struct vm_area_struct *map_vma;
}ktest_priv_t;


static int ktest_open(struct inode *inode, struct file *file)
{
    ktest_priv_t *data;
    
    ktest_func_enter();
    
    data = kmalloc(sizeof(ktest_priv_t), GFP_KERNEL);
    data->map_vma = NULL;

    file->private_data = data;

    return 0;
}

static int ktest_release(struct inode *inode, struct file *file)
{
    ktest_priv_t *data = file->private_data;
    ktest_func_enter();

    if(data)
        kfree(data);

    file->private_data = NULL;
        
    return 0;
}

static int ktest_flush(struct file *file, fl_owner_t id)
{
    ktest_priv_t *data = file->private_data;
    ktest_func_enter();

    printk("ktest inflush: *va=0x%x\n", *(g_kmem->va));
    /*
    if(data && data->map_vma)
    {
        struct vm_area_struct *vma = data->map_vma;
        zap_page_range(vma, vma->vm_start, vma->vm_end-vma->vm_start+1,NULL); 
        data->map_vma = NULL;
    }
    */
    return 0;
}

static long ktest_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
    ktest_func_enter();
    return 0;
}

void ktest_vma_open(struct vm_area_struct * area)
{
    struct file *file = area->vm_file;
    ktest_priv_t *data = file->private_data;
    ktest_func_enter();
    if(data->map_vma != area)
    {
        printk("ktest new vma area create\n");
        //zap_page_range(area, area->vm_start, area->vm_end-area->vm_start+1, NULL);
    }
}

void ktest_vma_close(struct vm_area_struct * area)
{
    struct file *file = area->vm_file;
    ktest_priv_t *data = file->private_data;
    ktest_func_enter();
    if(data->map_vma != area)
    {
        printk("ktest new vma area close\n");
        //zap_page_range(area, area->vm_start, area->vm_end-area->vm_start+1, NULL);
    }
    else
    {
        data->map_vma = NULL;
    }
    
}
int ktest_vma_fault(struct vm_area_struct *vma, struct vm_fault *vmf)
{
    ktest_func_enter();
}


struct vm_operations_struct ktest_vma_ops = 
{
    .open = ktest_vma_open,
    .close = ktest_vma_close,
    .fault = ktest_vma_fault,
};

//notes: mmap will inc(file->refcnt)
static int ktest_mmap(struct file *file, struct vm_area_struct *vma)
{
    ktest_priv_t *data = file->private_data;
    
    ktest_func_enter();

    if(data->map_vma)
    {
        printk("ktest map fail, already mapped!\n");
        return -ENOMEM;
    }
    
    remap_pfn_range(vma, vma->vm_start, 
            __page_to_pfn(g_kmem->page),
            vma->vm_end - vma->vm_start+1,
            /*pgprot_noncached*/(vma->vm_page_prot));
    
    printk("vma_ops_old = 0x%x\n", vma->vm_ops);

    vma->vm_ops = &ktest_vma_ops;
    data->map_vma = vma;
    
    return 0;
}


static const struct file_operations ktest_ops = 
{
    .owner = THIS_MODULE,
    .open = ktest_open,
    .release = ktest_release,
    .flush = ktest_flush,
    .unlocked_ioctl = ktest_ioctl,
    .mmap = ktest_mmap
};


static int __init k_test_init(void)
{
    int ret;
    /*
    alloc_chrdev_region(&ktest_dev_num, 0, 1, KTEST_DEV_NAME);
    ktest_cdev = cdev_alloc();
    ktest_cdev->owner = THIS_MODULE;
    ktest_cdev->ops = &ktest_ops;
    ret = cdev_add(ktest_cdev, ktest_dev_num, 1);
    */
    ktest_mdev = kzalloc(sizeof(struct miscdevice), GFP_KERNEL);
    ktest_mdev->minor = MISC_DYNAMIC_MINOR;
    ktest_mdev->name = "ktest";
    ktest_mdev->fops = &ktest_ops;
    ktest_mdev->parent = NULL;
    ret = misc_register(ktest_mdev);
    if (ret) {
            printk("ktest: failed to register misc device.\n");
            return ERR_PTR(ret);
    }

    g_kmem = kmalloc(sizeof(kmem_t), GFP_KERNEL);
    g_kmem->size = 0x1000;
    g_kmem->page = alloc_page(GFP_KERNEL|__GFP_ZERO);
    g_kmem->va = page_address(g_kmem->page);

    return 0;
}

static void __exit k_test_exit(void)
{
    //cdev_del(ktest_cdev);
    //unregister_chrdev_region(ktest_dev_num, 1);

    if(g_kmem && g_kmem->page)
        __free_page(g_kmem->page);    

    misc_deregister(ktest_mdev);
    /* XXX need to free the heaps and clients ? */
    kfree(ktest_mdev);
}



module_init(k_test_init);
module_exit(k_test_exit);


MODULE_LICENSE("GPL");

