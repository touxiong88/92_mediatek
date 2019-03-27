#ifndef MTK_GRALLOC_EXTRA_GRAPHIC_BUFFER_EXTRA_H
#define MTK_GRALLOC_EXTRA_GRAPHIC_BUFFER_EXTRA_H

#include <stdint.h>
#include <sys/types.h>

#include <system/window.h>

#include <utils/Singleton.h>
#include <utils/RefBase.h>

struct extra_device_t;
struct gralloc_buffer_info_t;

namespace android {
// ---------------------------------------------------------------------------

class GraphicBufferExtra : public Singleton<GraphicBufferExtra>
{
public:
    static inline GraphicBufferExtra& get() { return getInstance(); }

    int getIonFd(buffer_handle_t handle, int *idx, int *num);

	int getBufInfo(buffer_handle_t handle, gralloc_buffer_info_t* bufInfo);

	int getSecureBuffer(buffer_handle_t handle, int *type, int *hBuffer);
	
	int setBufParameter(buffer_handle_t handle, int mask, int value);
    
	int getMVA(buffer_handle_t handle, void ** mvaddr);

private:
    friend class Singleton<GraphicBufferExtra>;
	
    GraphicBufferExtra();
	
	~GraphicBufferExtra();

    extra_device_t *mExtraDev;
	
};

// ---------------------------------------------------------------------------
}; // namespace android

#endif // MTK_GRALLOC_EXTRA_GRAPHIC_BUFFER_EXTRA_H
