#include <sys/mman.h>
#include <dlfcn.h>
#include <cutils/log.h>
#include <cutils/atomic.h>
#include <hardware/hardware.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
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
#include <utils/Log.h>
#include <sys/time.h>


#include "data_rgb888_720x1280_golden.h"
#include "data_argb_720x1280_golden.h"
#include "data_rgb888_720x1280_golden2.h"
#include "data_argb_720x1280_golden2.h"
#include "data_rgb565_720x1280_golden.h"
#include "data_argb_720x1280_golden3.h"
#include "data_yuv420_p_720x1280_golden.h"

#include "data_rgb888_720x1280.h"
#include "data_rgb565_480x800.h"
#include "data_argb_64x64.h"
#include "data_uyvy_64x64.h"

//#pragma GCC optimize ("O0")
//#ifdef LOG_TAG
//#undef LOG_TAG
//#endif
//#define LOG_TAG "ION_TEST"
//#define LogPrint(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, ## __VA_ARGS__)
#define OUTPUT_BYTE_NUM 40
//#define BINNING_ANDROID_LOG
#ifdef BINNING_ANDROID_LOG
#define LOGV ALOGD
#else
#define LOGV printf
#endif

#define ion_carveout_heap_test


//unsigned int bufsize=1024*1024*8+256;
typedef enum
{
    RGB888   = 0,
    ARGB8888  = 1,
    RGB565 = 2,
    UYVY = 3,
    YUV420_P =4
} SourceType;

struct fb_var_screeninfo vinfo;
struct fb_fix_screeninfo finfo;
volatile char* inputBuffer;
volatile char* outputBuffer;
static	 const char * inbitsream = "/sdcard/tempin.bin";
static	 const char * outbitsream = "/sdcard/tempout.bin";
static	 const char * srcbitsream = "/sdcard/tempsrc.bin";



void dump_bitsream(char *inbuf,int size,char *location) 
{

    FILE *fp = fopen(location, "ab");

    if (fp)
    {
        fwrite(inbuf, 1,size, fp);
        fclose(fp);
    }
	else
		LOGV("open file fail!\n");
}

unsigned int verify_bitsream(char* golden_addr,char *outbuf,int lcm_width,
	                  int lcm_height,int wdma_Bpp,int format)
{
	int i=0;
	int j=0;
	int n=0;
	unsigned int diff_count=0;
	/*
	if(format == YUV420_P)
	{
		dump_bitsream(outbuf,lcm_height*lcm_width*wdma_Bpp*3/2,outbitsream);
	}
	else	
	{
		dump_bitsream(outbuf,lcm_height*lcm_width*wdma_Bpp,outbitsream);
	}*/	
#if 0
	for (i=0; i<lcm_height*lcm_width*wdma_Bpp; i+=wdma_Bpp)
	{
		if(*(volatile unsigned int*)(golden_addr+i) != *(volatile unsigned int*)(outbuf+i))
		{
		   LOGV("display ut compare error, input=0x%08x, output=0x%08x, i = %d!!\n", *(volatile unsigned int*)(golden_addr+i), *(volatile unsigned int*)(outbuf+i), i);
		   result =0;
		}
	}
	LOGV("Per Pixel for verify_bitsream \n");
#else
	if(golden_addr!=NULL && outbuf!= NULL)
	{
		if(format == YUV420_P)
		{
			char* u_addr = outbuf+lcm_width*lcm_height;
			char* v_addr = outbuf+lcm_width*lcm_height*5/4;
			char* golden_addr_u = golden_addr+lcm_width*lcm_height;
			char* golden_addr_v = golden_addr+lcm_width*lcm_height*5/4;
			unsigned int t=0;
			unsigned int size = lcm_width*lcm_height*wdma_Bpp;
			n=0;
			for(t=0;t<size;t++)
			{
				if( *(outbuf+t)!=*(golden_addr+t))
				{
					diff_count++;
					n++;
					if(n<=OUTPUT_BYTE_NUM)
						printf("t=%d, gold=0x%x, real=0x%x \n", t, *(golden_addr+t), *(outbuf+t));
				}
			}
			n=0;
			for(t=0;t<size/4;t++)
			{
				if(*(u_addr+t) != *(golden_addr_u+t) )
				{
					diff_count++;
					n++;
					if(n<=OUTPUT_BYTE_NUM)
						printf("t=%d, gold=0x%x, real=0x%x \n", t, *(golden_addr_u+t), *(u_addr+t));
				}
			}
			n=0;
			for(t=0;t<size/4;t++)
			{
				if( *(v_addr+t) != *(golden_addr_v+t) )
				{
					diff_count++;
					n++;
					if(n<=OUTPUT_BYTE_NUM)
						printf("t=%d, gold=0x%x, real=0x%x \n", t, *(golden_addr_v+t), *(v_addr+t));
				}
			}	
		}
		else
		{
			for (i=0; i<lcm_height; i++)
			{
				for(j=0;j<lcm_width*wdma_Bpp;j++)
				{
					if(*(golden_addr+i*wdma_Bpp+j) != *(outbuf+i*wdma_Bpp+j))
					{
						n++;
						if(n<=OUTPUT_BYTE_NUM)
							LOGV("display ut compare error, n=%d,golden=0x%x, output=0x%x\n",i*wdma_Bpp+j, *(golden_addr+i*wdma_Bpp+j), *(outbuf+i*wdma_Bpp+j));
						diff_count++;
					}
				}
			}
		}	
		//LOGV("Per Byte for verify_bitsream \n");
	}
#endif

	return diff_count;
}

int test_ddp_slt(char* src_addr,int src_width,int src_height,int ovl_fmt,
	               int wdma_out_fmt,int wdma_w,int wdma_h,char* golden_addr)
{
    int i=0;
    int ion_fd=0;
    int display_fd=0;
	int share_fd=0;
    struct ion_handle* handle=NULL;
    struct fb_overlay_layer fb_layer;
	unsigned int x_virtual = 0;
    unsigned int  fbsize = 0;
	unsigned int wdma_Bpp=4;
	unsigned int ovl_Bpp=4;
	unsigned int diff_cnt = 0;
	
	display_fd = open("/dev/graphics/fb0", O_RDONLY);
	if(display_fd < 0)
	{
		LOGV("Cannot open fb0 device\n");
		return 0;
	}

    if (ioctl(display_fd, FBIOGET_VSCREENINFO, &vinfo) < 0) 
    {
        LOGV("ioctl FBIOGET_VSCREENINFO failed\n");
        return 0;
    }

    if (ioctl(display_fd, FBIOGET_FSCREENINFO, &finfo) < 0) 
    {
        LOGV("ioctl FBIOGET_FSCREENINFO failed\n");
        return 0;
    }
	
    fbsize = vinfo.xres * vinfo.yres * 4;

    ion_fd = ion_open();
    if (ion_fd < 0)
    {
        LOGV("Cannot open ion device.\n");
        return 0;
    }
    if (ion_alloc_mm(ion_fd, fbsize, 4, 0, &handle))
    {
        LOGV("IOCTL[ION_IOC_ALLOC] failed!\n");
        return 0;
    }

    if (ion_share(ion_fd, handle, &share_fd))
    {
        LOGV("IOCTL[ION_IOC_SHARE] failed!\n");
        return 0;
    }

    inputBuffer = ion_mmap(ion_fd, NULL, fbsize, PROT_READ|PROT_WRITE, MAP_SHARED, share_fd, 0);
    LOGV("ion_map: inputBuffer = 0x%x\n", inputBuffer);
    if (inputBuffer==NULL)
    {
        LOGV("Cannot map ion buffer.\n");
        return 0;
    }

// config input buffer
#if 0
    for (i=0; i<fbsize; i+=4)
    {
        *(volatile unsigned int*)(inputBuffer+i) = (0xff000000 + i);
    }
#else
    
    memset((void*)inputBuffer,0,fbsize);
	if(ovl_fmt==ARGB8888)
	{
		ovl_Bpp = 4;
	}else if(ovl_fmt==RGB888){
		ovl_Bpp = 3;
	}else if(ovl_fmt==RGB565 ||ovl_fmt==UYVY){
		ovl_Bpp = 2;
	}

	for(i=0;i<src_height;i++)
	{
	   memcpy((void*)(inputBuffer+vinfo.xres*ovl_Bpp*i), (void*)(src_addr+src_width*ovl_Bpp*i), src_width*ovl_Bpp);
	}
	//dump_bitsream(src_addr,src_height*src_width*ovl_Bpp,srcbitsream);
	//dump_bitsream(inputBuffer,fbsize,inbitsream); 
#endif
    
    struct ion_mm_data mm_data;
    mm_data.mm_cmd = ION_MM_CONFIG_BUFFER;
    mm_data.config_buffer_param.handle = handle;
    mm_data.config_buffer_param.eModuleID = 1;
    mm_data.config_buffer_param.security = 0;
    mm_data.config_buffer_param.coherent = 1;
    if (ion_custom_ioctl(ion_fd, ION_CMD_MULTIMEDIA, &mm_data))
    {
        LOGV("IOCTL[ION_IOC_CUSTOM] Config Buffer failed!\n");
        return 0;
    }
    struct ion_sys_data sys_data;
    sys_data.sys_cmd = ION_SYS_GET_PHYS;
    sys_data.get_phys_param.handle = handle;
    if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
    {
        LOGV("IOCTL[ION_IOC_CUSTOM] Get Phys failed!\n");
        return 0;
    }
    LOGV("Physical address=0x%08X len=0x%X\n", sys_data.get_phys_param.phy_addr, sys_data.get_phys_param.len);

	sys_data.sys_cmd = ION_SYS_CACHE_SYNC;
	sys_data.cache_sync_param.handle = handle;


	sys_data.cache_sync_param.sync_type = ION_CACHE_INVALID_BY_RANGE;
	if (ion_custom_ioctl(ion_fd, ION_CMD_SYSTEM, &sys_data))
	{
	   printf("IOCTL[ION_IOC_CUSTOM] Cache sync failed!\n");
	   return 0;
	}

	memset(&fb_layer, 0, sizeof(fb_layer));
	fb_layer.layer_id = 3;
	fb_layer.layer_enable = 1;
	fb_layer.src_base_addr = inputBuffer;
	fb_layer.src_phy_addr = sys_data.get_phys_param.phy_addr;
	if(ovl_fmt==ARGB8888)
	{
		fb_layer.src_fmt = MTK_FB_FORMAT_ARGB8888;
	}else if(ovl_fmt==RGB888){
		fb_layer.src_fmt = MTK_FB_FORMAT_RGB888;
	}else if(ovl_fmt==RGB565){
		fb_layer.src_fmt = MTK_FB_FORMAT_RGB565;
	}else if(ovl_fmt==UYVY){
		fb_layer.src_fmt = MTK_FB_FORMAT_UYVY;
	}
	
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
			LOGV("ioctl to set overlay layer fail\n");
			return 0;
		}
		sleep(2);
	}

	// begin screen capture and compare
	outputBuffer = malloc(vinfo.xres * vinfo.yres * 4);
	if(outputBuffer == NULL)
	{
		LOGV("malloc outputbuffer failed\n");
		return 0;
	}
    memset(outputBuffer,0,vinfo.xres * vinfo.yres * 4);
#if 0	
	if(ioctl(display_fd, MTKFB_CAPTURE_FRAMEBUFFER, &outputBuffer) < 0)
	{
		LOGV("ioctl to capture screen fail\n");
		return 0;
	}	
#else
	struct fb_slt_catpure s_config;
	memset(&s_config, 0, sizeof(s_config));

    if(wdma_out_fmt==RGB888)
    {
		s_config.format= MTK_FB_FORMAT_RGB888;
		wdma_Bpp =3;
    }
	else if(wdma_out_fmt == ARGB8888)
	{
		s_config.format= MTK_FB_FORMAT_ARGB8888;
		wdma_Bpp =4;
	}
	else if(wdma_out_fmt == RGB565)
	{
		s_config.format= MTK_FB_FORMAT_RGB565;
		wdma_Bpp=2;
	}
	else if(wdma_out_fmt == YUV420_P)
	{
		s_config.format= MTK_FB_FORMAT_YUV420_P;
		wdma_Bpp=1;
	}
	else{
		s_config.format= MTK_FB_FORMAT_ARGB8888;
		wdma_Bpp =4;
	}
	s_config.wdma_width = wdma_w;
	s_config.wdma_height = wdma_h;
	s_config.outputBuffer = outputBuffer;
		
	LOGV("format 0x%x outputbuf 0x%x wdma_width %d wdma_height %d wdma_Bpp %d\n",s_config.format,s_config.outputBuffer,s_config.wdma_width,s_config.wdma_height,wdma_Bpp);
	if(ioctl(display_fd, MTKFB_SLT_AUTO_CAPTURE, &s_config) < 0)
	{
		LOGV("ioctl to capture screen fail\n");
		return 0;
	}	
#endif

	diff_cnt = verify_bitsream(golden_addr,outputBuffer,wdma_w,wdma_h,wdma_Bpp,wdma_out_fmt);

	fb_layer.layer_id = 3;
	fb_layer.layer_enable = 0;
	
	if(ioctl(display_fd, MTKFB_SET_OVERLAY_LAYER, &fb_layer) < 0)
	{
		LOGV("ioctl to set overlay layer fail\n");
		return 0;
	}

	sleep(1);

    ion_munmap(ion_fd, inputBuffer, fbsize);

    if (ion_free(ion_fd, handle))
    {
        LOGV("IOCTL[ION_IOC_FREE] failed!\n");
        return 0;
    }
    ion_close(ion_fd);
	
	if(outputBuffer!=NULL)
	{
		free(outputBuffer);
	}
	LOGV("Display test done(ovl=%d,wdma_out_fmt=%d,wdma_w=%d,wdma_h=%d error rate %d%% \n)",ovl_fmt,wdma_out_fmt,wdma_w,wdma_h,diff_cnt/(wdma_w*wdma_h*wdma_Bpp)*100);

   	if(diff_cnt!=0)
		return 0;
   	else
   		return 1;
}

   
int main(int argc, char **argv)
{  
    int result = 0;
	int total_result=1;
	printf("Binning SLT DDP Test Start!!!\n");
	struct timeval tvStart,tvEnd;
	gettimeofday (&tvStart,NULL);
	long long starttime=0,endtime=0;
	
	starttime=(int64_t)tvStart.tv_sec * 1000000ll + tvStart.tv_usec;
	
	printf("Binning SLT DDP Test Case1!!!\n");
	//result = test_ddp_slt(data_argb_64x64,64,64,ARGB8888,ARGB8888,720,1280,data_argb_720x1280_golden);
	result = test_ddp_slt(data_uyvy_64x64,64,64,UYVY,ARGB8888,720,1280,data_argb_720x1280_golden3);
	total_result&=result;

	printf("Case1 Result=%d\nBinning SLT DDP Test Case2!!!\n",result);
	result = test_ddp_slt(data_argb_64x64,64,64,ARGB8888,YUV420_P,720,1280,data_yuv420_p_720x1280_golden);	
	total_result&=result;
	
	printf("Case2 Result=%d\nBinning SLT DDP Test Case3!!!\n",result);
	result = test_ddp_slt(data_argb_64x64,64,64,ARGB8888,RGB888,720,1280,data_rgb888_720x1280_golden);
	total_result&=result;


	printf("Case3 Result=%d\nBinning SLT DDP Test Case4!!!\n",result);
	result = test_ddp_slt(data_rgb565_480x800,480,800,RGB565,RGB888,720,1280,data_rgb888_720x1280_golden2);
	total_result&=result;
	
	printf("Case4 Result=%d\nBinning SLT DDP Test Case5!!!\n",result);
	result = test_ddp_slt(data_rgb888_720x1280,720,1280,RGB888,RGB565,720,1280,data_rgb565_720x1280_golden);	
	total_result&=result;

	printf("Case5 Result=%d\nBinning SLT DDP Test Case6!!!\n",result);
	result = test_ddp_slt(data_rgb888_720x1280,720,1280,RGB888,ARGB8888,720,1280,data_argb_720x1280_golden2);
	total_result&=result;
	printf("Case6 Result=%d\n",result);
	
	gettimeofday (&tvEnd,NULL);
	endtime=(int64_t)tvEnd.tv_sec * 1000000ll + tvEnd.tv_usec;
	LOGV("All Case consume Timediff=%lld,endtime=%lldus,starttime=%lldus",endtime-starttime,endtime,starttime); 

	if(total_result)
	{
		printf("Binning SLT DDP Test Success!!!\n");
	}
	else
	{
		printf("Binning SLT DDP Test Failure!!!\n");
	}
	printf("Binning SLT DDP Test End!!!\n");
	return result;
}
