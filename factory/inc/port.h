/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */ 

#ifndef _FM_PORT_H_
#define _FM_PORT_H_

#ifdef __cplusplus
extern "C" {
#endif
 
int COM_Init (int *fd_atcmd, int *fd_uart, int *hUsbComPort);
int COM_DeInit (int *fd_atcmd, int *fd_uart, int *hUsbComPort);




#ifdef __cplusplus
}
#endif


#endif
