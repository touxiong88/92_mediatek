#define LOG_TAG "Fence"

#include <sync/sync.h>
#include <ui/Fence.h>
#include <unistd.h>

#include <cutils/xlog.h>

#ifndef EMULATOR_SUPPORT
#include "aee.h"
#endif

// ---------------------------------------------------------------------------

const char* findKeyWord(const char* msg) {
    android::String8 keyword("timeline_");

    if (strstr(msg, "SurfaceFlinger")) {
        keyword.append("SurfaceFlinger");
    } else if (strstr(msg, "ovl_timeline")) {
        keyword.append("ovl_timeline");
    } else if (strstr(msg, "mali")) {
        keyword.append("mali");
    }

    return keyword.string();
}

// ---------------------------------------------------------------------------

namespace android {

void Fence::dump(int fd, const char* logname, unsigned int warningTimeout) {
    if (-1 == fd) return;

    String8 aee_msg = String8::format("%s: fence %d didn't signal in %u ms",
        logname, fd, warningTimeout);

    struct sync_fence_info_data *info = sync_fence_info(fd);
    if (info) {
        struct sync_pt_info *pt_info = NULL;
        // status: active(0) signaled(1) error(<0)
        XLOGI("fence(%s) status(%d)", info->name, info->status);

        // iterate active/error sync points
        while ((pt_info = sync_pt_info(info, pt_info))) {
            if (NULL != pt_info && pt_info->status <= 0) {
                int ts_sec = pt_info->timestamp_ns / 1000000000LL;
                int ts_usec = (pt_info->timestamp_ns % 1000000000LL) / 1000LL;

                // sepecial line for CR dispatch
                String8 msg = String8::format("CRDISPATCH_KEY:%s\n",
                    findKeyWord(pt_info->obj_name));

                msg.appendFormat(
                    "sync point: timeline(%s) drv(%s) status(%d) sync_drv(%u) timestamp(%d.%06d)",
                    pt_info->obj_name, pt_info->driver_name, pt_info->status,
                    *(uint32_t *)pt_info->driver_data, ts_sec, ts_usec);
                XLOGI(msg.string());
                aee_msg.appendFormat("\n%s", msg.string());
            }
        }
        sync_fence_info_free(info);
    }

#ifndef EMULATOR_SUPPORT
    // raise aee warning
    aee_system_warning(LOG_TAG, NULL, DB_OPT_FTRACE | DB_OPT_DUMMY_DUMP, aee_msg.string());
#endif
}

} // namespace android
