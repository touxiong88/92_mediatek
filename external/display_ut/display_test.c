#include <sys/mman.h>
#include <dlfcn.h>
#include <cutils/log.h>
#include <cutils/atomic.h>
#include <hardware/hardware.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <string.h>
#include <stdlib.h>
#include <sched.h>
#include <sys/resource.h>
#include <linux/fb.h>
#include <wchar.h>
#include <pthread.h>
#include <linux/mmprofile.h>
#include <linux/ion.h>
#include <linux/ion_drv.h>
#include <ion/ion.h>
#include <unistd.h>
#include "mtkfb.h"
#include "mtkfb_info.h"

//#pragma GCC optimize ("O0")
#ifdef LOG_TAG
#undef LOG_TAG
#endif
//#define LOG_TAG "ION_TEST"
//#define LogPrint(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, ## __VA_ARGS__)

#define ion_carveout_heap_test

//unsigned int bufsize=1024*1024*8+256;

struct fb_var_screeninfo vinfo;
struct fb_fix_screeninfo finfo;
volatile char* inputBuffer;
volatile char* outputBuffer;

int main(int argc, char **argv)
{
    int i;
    int ion_fd;
    int display_fd;
    struct ion_handle* handle;
    int share_fd;
    struct fb_overlay_layer fb_layer;
	unsigned int x_virtual = 0;
    unsigned int  fbsize = 0;

	display_fd = open("/dev/graphics/fb0", O_RDONLY);
	if(display_fd < 0)
	{
		printf("Cannot open fb0 device\n");
		return 0;
	}

    if (ioctl(display_fd, FBIOGET_VSCREENINFO, &vinfo) < 0) 
    {
        printf("ioctl FBIOGET_VSCREENINFO failed\n");
        return 0;
    }

    if (ioctl(display_fd, FBIOGET_FSCREENINFO, &finfo) < 0) 
    {
        printf("ioctl FBIOGET_FSCREENINFO failed\n");
        return 0;
    }
	
    fbsize = vinfo.xres * vinfo.yres * 4;

    ion_fd = ion_open();
    if (ion_fd < 0)
    {
        printf("Cannot open ion device.\n");
        return 0;
    }
    if (ion_alloc_mm(ion_fd, fbsize, 4, 0, &handle))
    {
        printf("IOCTL[ION_IOC_ALLOC] failed!\n");
        return 0;
    }

    if (ion_share(ion_fd, handle, &share_fd))
    {
        printf("IOCTL[ION_IOC_SHARE] failed!\n");
        return 0;
    }

    inputBuffer = ion_mmap(ion_fd, NULL, fbsize, PROT_READ|PROT_WRITE, MAP_SHARED, share_fd, 0);
    printf("ion_map: inputBuffer = 0x%x\n", inputBuffer);
    if (!inputBuffer)
    {
        printf("Cannot map ion buffer.\n");
        return 0;
    }


    for (i=0; i<fbsize; i+=4)
    {
        *(volatile unsigned int*)(inputBuffer+i) = (0xff000000 + i);
    }

  
    
        struct ion_mm_data mm_data;
        mm_data.mm_cmd = ION_MM_CONFIG_BUFFER;
        mm_data.config_buffer_param.handle = handle;
        mm_data.config_buffer_param.eModuleID = 1;
        mm_data.config_buffer_param.security = 0;
        mm_data.config_buffer_param.coherent = 1;
        if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &mm_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Config Buffer failed!\n");
            return 0;
        }
        struct ion_sys_data sys_data;
        sys_data.sys_cmd = ION_SYS_GET_PHYS;
        sys_data.get_phys_param.handle = handle;
        if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
        {
            printf("IOCTL[ION_IOC_CUSTOM] Get Phys failed!\n");
            return 0;
        }
        printf("Physical address=0x%08X len=0x%X\n", sys_data.get_phys_param.phy_addr, sys_data.get_phys_param.len);

	memset(&fb_layer, 0, sizeof(fb_layer));
	fb_layer.layer_id = 3;
	fb_layer.layer_enable = 1;
	fb_layer.src_base_addr = inputBuffer;
	fb_layer.src_phy_addr = sys_data.get_phys_param.phy_addr;
	fb_layer.src_fmt = MTK_FB_FORMAT_ARGB8888;
	fb_layer.src_pitch = vinfo.xres;
	fb_layer.src_offset_x = 0; 
	fb_layer.src_offset_y = 0;
	fb_layer.src_width = vinfo.xres;
	fb_layer.src_height = vinfo.yres;
		
	fb_layer.tgt_offset_x = 0;
	fb_layer.tgt_offset_y = 0;
	fb_layer.tgt_width = vinfo.xres;
	fb_layer.tgt_height = vinfo.yres;

	{
		if(ioctl(display_fd, MTKFB_SET_OVERLAY_LAYER, &fb_layer) < 0)
		{
			printf("ioctl to set overlay layer fail\n");
			return 0;
		}
		sleep(1);
		
	}

	// begin screen capture and compare
	outputBuffer = malloc(vinfo.xres * vinfo.yres * 4);
	if(outputBuffer == NULL)
	{
		printf("malloc outputbuffer failed\n");
		return 0;
	}

	
	if(ioctl(display_fd, MTKFB_CAPTURE_FRAMEBUFFER, &outputBuffer) < 0)
	{
		printf("ioctl to capture screen fail\n");
		return 0;
	}

	for (i=0; i<fbsize; i+=4)
	{
	    if(*(volatile unsigned int*)(inputBuffer+i) != *(volatile unsigned int*)(outputBuffer+i))
	    {
	 	   printf("display ut compare error, input=0x%08x, output=0x%08x, i = %d!!\n", *(volatile unsigned int*)(inputBuffer+i), *(volatile unsigned int*)(outputBuffer+i), i);
	    }
	}
	if(i == fbsize)
		printf("display ut finished, inputbuffer is the same as output buffer\n");

	fb_layer.layer_id = 3;
	fb_layer.layer_enable = 0;
	if(ioctl(display_fd, MTKFB_SET_OVERLAY_LAYER, &fb_layer) < 0)
	{
		printf("ioctl to set overlay layer fail\n");
		return 0;
	}


    ion_munmap(ion_fd, inputBuffer, 0x1000);

    if (ion_free(ion_fd, handle))
    {
        printf("IOCTL[ION_IOC_FREE] failed!\n");
        return 0;
    }
    ion_close(ion_fd);
    printf("Display test done!\n");
    return 0;
}
