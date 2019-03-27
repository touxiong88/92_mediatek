
#define LOG_TAG "GraphicBufferExtra"
#define ATRACE_TAG ATRACE_TAG_GRAPHICS

#include <stdint.h>
#include <errno.h>

#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/Trace.h>

#include <cutils/xlog.h>

#include <hardware/gralloc.h>
#include <hardware/gralloc_extra.h>

#include <ui/GraphicBufferExtra.h>

namespace android {
// ---------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE( GraphicBufferExtra )

GraphicBufferExtra::GraphicBufferExtra()
    : mExtraDev(0)
{
    hw_module_t const* module;
    int err = hw_get_module(GRALLOC_HARDWARE_MODULE_ID, &module);
	
    ALOGE_IF(err, "FATAL: can't find the %s module", GRALLOC_HARDWARE_MODULE_ID);
    if (err == 0) 
    {
        gralloc_extra_open(module, &mExtraDev);
    }
}

GraphicBufferExtra::~GraphicBufferExtra()
{
	if (mExtraDev)
	{
		gralloc_extra_close(mExtraDev);
	}
}

// ---------------------------------------------------------------------------
}; // namespace android
