#if !defined(__MRDUMP_H__)
#define __MRDUMP_H__

#define MRDUMP_CB_ADDR 0x81F00000
#define MRDUMP_CB_SIZE 0x1000

#define MRDUMP_CPU_MAX 16

struct mrdump_control_block;

#if defined(CONFIG_MTK_AEE_MRDUMP)

void mrdump_reserve_memory(void);

void mrdump_platform_init(struct mrdump_control_block *mrdump_cb, void (*mrdump_hw_enable_cb)(bool enabled));

#else

static inline void mrdump_reserve_memory(void) {}

static inline void mrdump_platform_init(struct mrdump_control_block *cblock, void (*hw_enable)(bool enabled)) {}

#endif

#endif
