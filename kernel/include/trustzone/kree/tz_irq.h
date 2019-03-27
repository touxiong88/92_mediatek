/*
 * IRQ/FIQ for TrustZone
 */

#ifndef __KREE_TZ_IRQ_H__
#define __KREE_TZ_IRQ_H__

#ifdef CONFIG_MTK_TEE_SUPPORT

void kree_irq_init(void);
int kree_set_fiq(int irq, unsigned long irq_flags);
void kree_enable_fiq(int irq);
void kree_disable_fiq(int irq);
unsigned int kree_fiq_get_intack(void);
void kree_fiq_eoi(unsigned int iar);
int kree_raise_softfiq(unsigned int mask, unsigned int irq);

#else

#define kree_set_fiq(irq, irq_flags)     -1
#define kree_enable_fiq(irq)
#define kree_disable_fiq(irq)

#endif /* CONFIG_MTK_TEE_SUPPORT */

#endif /* __KREE_TZ_IRQ_H__ */
