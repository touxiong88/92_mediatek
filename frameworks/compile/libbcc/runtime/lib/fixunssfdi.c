/* ===-- fixunssfdi.c - Implement __fixunssfdi -----------------------------===
 *
 *                     The LLVM Compiler Infrastructure
 *
 * This file is distributed under the University of Illinois Open Source
 * License. See LICENSE.TXT for details.
 *
 * ===----------------------------------------------------------------------===
 *
 * This file implements __fixunssfdi for the compiler_rt library.
 *
 * ===----------------------------------------------------------------------===
 */

#if !defined(__GNUC__) || __GNUC__ < 4 || (__GNUC__ == 4 && __GNUC_MINOR__ < 7) || defined(ARCH_X86)
// ARM gcc >= 4.7 implements this in libgcc
#include "int_lib.h"

/* Returns: convert a to a unsigned long long, rounding toward zero.
 *          Negative values all become zero.
 */

/* Assumption: float is a IEEE 32 bit floating point type 
 *             du_int is a 64 bit integral type
 *             value in float is representable in du_int or is negative 
 *                 (no range checking performed)
 */

/* seee eeee emmm mmmm mmmm mmmm mmmm mmmm */

du_int
__fixunssfdi(float a)
{
    float_bits fb;
    fb.f = a;
    int e = ((fb.u & 0x7F800000) >> 23) - 127;
    if (e < 0 || (fb.u & 0x80000000))
        return 0;
    du_int r = (fb.u & 0x007FFFFF) | 0x00800000;
    if (e > 23)
        r <<= (e - 23);
    else
        r >>= (23 - e);
    return r;
}
#endif
