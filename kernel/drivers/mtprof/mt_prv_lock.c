#include <linux/proc_fs.h>
#include <linux/sched.h>
#include <linux/seq_file.h>
#include <linux/kallsyms.h>
#include <linux/utsname.h>
#include <asm/uaccess.h>
#include <linux/stacktrace.h>
#include <asm/stacktrace.h>

#include <linux/delay.h>
#include <linux/pid.h>
#include <linux/debug_locks.h>
#define SEQ_printf(m, x...)	    \
 do {			    \
    if (m){		    \
	seq_printf(m, x);	\
	printk(x);	    \
    }else		    \
	printk(x);	    \
 } while (0)

#define MT_DEBUG_ENTRY(name) \
static int mt_##name##_show(struct seq_file *m, void *v);\
static int mt_##name##_write(struct file *filp, const char *ubuf, size_t cnt, loff_t *data);\
static int mt_##name##_open(struct inode *inode, struct file *file) \
{ \
    return single_open(file, mt_##name##_show, inode->i_private); \
} \
\
static const struct file_operations mt_##name##_fops = { \
    .open = mt_##name##_open, \
    .write = mt_##name##_write,\
    .read = seq_read, \
    .llseek = seq_lseek, \
    .release = single_release, \
};\
void mt_##name##_switch(int on);

#include <linux/mt_export.h>

/*************/
//sample code 
#if 0
static DEFINE_SPINLOCK(mt_spin_lock);
static DEFINE_SEMAPHORE(mtprof_sem_static);
static struct semaphore* mtprof_sem_dyn;
static void sem_down()
{
    mtprof_sem_dyn = mt_sema_init(1);
    printk("down mtprof sem static...\n");
    down(&mtprof_sem_static);
    printk("down mtprof sem dyn..\n");
    down(mtprof_sem_dyn);
}
static void sem_up(){
    printk("up mtprof sem dyn..\n");
    up(mtprof_sem_dyn);
    printk("up mtprof sem static...\n");
    up(&mtprof_sem_static);
}
#endif


static DEFINE_SPINLOCK(spin_a);
//static DEFINE_SPINLOCK(spin_b);
//static DEFINE_SPINLOCK(spin_c);

static DEFINE_MUTEX(mtx_a);
static DEFINE_MUTEX(mtx_b);
static DEFINE_MUTEX(mtx_c);
MT_DEBUG_ENTRY(pvlk);
static int mt_pvlk_show(struct seq_file *m, void *v)
{
    printk(" debug_locks = %d\n", debug_locks);
    return 0; 
}
static void get_mutex_lock(void)
{
    printk("[get mutex lock1]\n");
    mutex_lock(&mtx_a); 
    mdelay(1000);
}
static void get_mutex_lock2(void)
{
    printk("[get mutex lock2]\n");
    mdelay(1000);
    printk("[get mutex lock2] try to get lock\n");
    mutex_unlock(&mtx_a); 
}
static void get_spin_lock(void)
{
    printk("[get spin lock1]\n");
    spin_lock(&spin_a); 
    printk("[spin lock1] in delay...\n");
    mdelay(20000);
    spin_unlock(&spin_a); 
    printk("[spin lock1] unlock done\n");
}
static void get_spin_lock2(void)
{
    printk("[get spin lock2]\n");
    mdelay(2000);
    printk("[get spin lock2] try to get lock\n");
    spin_lock(&spin_a); 
    spin_unlock(&spin_a); 
    printk("[spin lock2] unlock done\n");
}
static ssize_t mt_pvlk_write(struct file *filp, const char *ubuf,
	size_t cnt, loff_t *data)
{
    char buf[64];
    unsigned long val;
    int ret;
    if (cnt >= sizeof(buf))
        return -EINVAL;

    if (copy_from_user(&buf, ubuf, cnt))
        return -EFAULT;

    buf[cnt] = 0;

    ret = strict_strtoul(buf, 10, &val);
    if (ret < 0)
        return ret;
    if(val == 0){
        debug_locks_off(); 
    }else if(val == 2){
        printk("==== circular lock test=====\n");
        mutex_lock(&mtx_a);
        mutex_lock(&mtx_b);
        mutex_lock(&mtx_c);
        mutex_unlock(&mtx_c);
        mutex_unlock(&mtx_b);
        mutex_unlock(&mtx_a);

        mutex_lock(&mtx_c);
        mutex_lock(&mtx_a);
        mutex_lock(&mtx_b);
        mutex_unlock(&mtx_b);
        mutex_unlock(&mtx_a);
        mutex_unlock(&mtx_c);

    }else if(val == 3){
        debug_locks_off(); 
        printk("======== [3] spin lock debug test ======\n");
        kernel_thread((void *)get_spin_lock, NULL, CLONE_FS|CLONE_FILES);
        kernel_thread((void *)get_spin_lock2, NULL, CLONE_FS|CLONE_FILES);
        printk("======== [3] Done ======\n");
    }else if(val == 4){
        debug_locks_off(); 
        printk("======== [4] mutex lock debug test ======\n");
        kernel_thread((void *)get_mutex_lock, NULL, CLONE_FS|CLONE_FILES);
        kernel_thread((void *)get_mutex_lock2, NULL, CLONE_FS|CLONE_FILES);
        printk("======== [4] Done ======\n");
    }
    printk("[MT prove locking] debug_locks = %d\n", debug_locks);
    return cnt;
}
static int __init init_pvlk_prof(void)
{
    struct proc_dir_entry *pe;
    pe = proc_create("mtprof/pvlk", 0664, NULL, &mt_pvlk_fops);
    if (!pe)
        return -ENOMEM;
    return 0;
}
late_initcall(init_pvlk_prof);
