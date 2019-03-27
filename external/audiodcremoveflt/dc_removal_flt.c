/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*****************************************************************************
 *
 * Filename:
 * ---------
 * Dc_Remove_fit.c
 *
 * Project:
 * --------
 * SWIP
 *
 * Description:
 * ------------
 * DC_Remove_fit implementation
 *
 * Author:
 * -------
 * Chipeng Chang
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision$
 * $Modtime$
 * $Log$
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include "dc_removal_flt.h"

typedef struct
{
    // information of input data
    unsigned int mChannels;
    unsigned int mSampleRate;
    unsigned int mWorkingMode;
    // 1 : left channel
    long acc_x1;
    long prev_x1;
    long prev_y1;
    // 2 : right channel
    long acc_x2;
    long prev_x2;
    long prev_y2;
    double pole;
    uint32_t A_Coefficient;
} DCRemove_Filter;

static short clamp16(int sample)
{
    if ((sample >> 15) ^ (sample >> 31))
    {
        sample = 0x7FFF ^ (sample >> 31);
    }
    return sample;
}

static long clamp24(long long sample)
{
    if(sample > 2147483647)
    {
        sample = 2147483647;
    }
    else if(sample < -2147483647)
    {
        sample = -2147483647;
    }
    return (long)sample;
}

static short clamp(int input)
{
    if (input > 32767)
    {
        input = 32767;
    }
    else if (input < -32768)
    {
        input = -32768;
    }
    return input;
}

void Init_Pole_para(DCRemove_Filter *pHandle)
{
    double Q=0.0;
    Q=32768.0;	// Q1.15

    // pole value will effect filter frequency response
    switch (pHandle->mWorkingMode)
    {
        case 0:
            pHandle->pole = 0.95;
            pHandle->A_Coefficient = (int)(Q*(1.0 - pHandle->pole));
            break;
        case 1:
            pHandle->pole = 0.97;
            pHandle->A_Coefficient = (int)(Q*(1.0 - pHandle->pole));
            break;
        case 2:
            pHandle->pole = 0.9999;
            pHandle->A_Coefficient = (int)(Q*(1.0 - pHandle->pole));
            break;
        default:
            pHandle->pole = 0.9999;
            pHandle->A_Coefficient = (int)(Q*(1.0 - pHandle->pole));
            break;
    }
}


//============================================================//
// opem DCRomve_Handle
// pHandle:             input, Handle of current DCRemove_Handle.
// iuChannel:           input, Channel if input process
// iuSampleRate         input, SampleRate of input.
// iuWorkingMode        input, working mode of filter.
//============================================================//
DCRemove_Handle *DCR_Open(unsigned int iuChannel,
                          unsigned int iuSampleRate,
                          unsigned int iuWorkingMode)
{
    DCRemove_Filter *pDCR_Handle = NULL;
    pDCR_Handle = (DCRemove_Filter *)malloc(sizeof(DCRemove_Filter));
    if (pDCR_Handle == NULL)
    {
        return NULL;
    }
    pDCR_Handle->mChannels = iuChannel;
    pDCR_Handle->mSampleRate = iuSampleRate;
    pDCR_Handle->mWorkingMode = iuWorkingMode;
    pDCR_Handle->prev_x1 = 0;
    pDCR_Handle->prev_y1 = 0;
    pDCR_Handle->prev_x2 = 0;
    pDCR_Handle->prev_y2 = 0;
    pDCR_Handle->acc_x1 = 0;
    pDCR_Handle->acc_x2 = 0;
    Init_Pole_para(pDCR_Handle);
    return pDCR_Handle;
}

//============================================================//
// process for buffer
// pHandle:             input,
//                      Handle of current DCRemove_Handle.
// InputBuffer          input , input buffer
// InputLength          input , input length(byte)
//                      output, output of  buffer left.
// OutputBuffer         input , pointer to output buffer
// OutputLength         input , length of output buffer
//                      output, output data length in bytes
// return               total data out in bytes
//============================================================//
unsigned int  DCR_Process(DCRemove_Handle *pHandle,
                          short *InputBuffer,
                          unsigned int *InputLength,
                          short *OutputBuffer,
                          unsigned int *OutputLength
                         )
{
    DCRemove_Filter *pDCR_Handle = NULL;
    int Conumebytes = 0;
    int Length = 0;
    if (pHandle == NULL)
    {
        return 0;
    }
    pDCR_Handle = (DCRemove_Handle *)pHandle;
    if (pDCR_Handle->mChannels == 1)
    {
        int acc = pDCR_Handle->acc_x1, prev_x = pDCR_Handle->prev_x1, prev_y = pDCR_Handle->prev_y1, A_Coeffient = pDCR_Handle->A_Coefficient;
        Conumebytes = (*InputLength > *OutputLength) ? *OutputLength : *InputLength;
        for (Length = 0; Length < (Conumebytes >> 1); Length++)
        {
                acc   -= prev_x;
                prev_x = ((int)*(InputBuffer+Length))<<15;
                acc   += prev_x; 
                acc   -= A_Coeffient * prev_y;
                prev_y = acc>>15;         // quantization happens here 
                *(OutputBuffer+Length)   = (short)clamp16(prev_y); // acc has y[n] in upper 17 bits and -e[n] in lower 15 bits 
        }
        pDCR_Handle->acc_x1 = acc;
        pDCR_Handle->prev_x1 = prev_x;
        pDCR_Handle->prev_y1 = prev_y;
        *InputLength -= Conumebytes;
        *OutputLength = Conumebytes;
    }
    else if (pDCR_Handle->mChannels == 2)
    {        
        int acc1 = pDCR_Handle->acc_x1, prev_x1 = pDCR_Handle->prev_x1, prev_y1 = pDCR_Handle->prev_y1,A_Coeffient = pDCR_Handle->A_Coefficient; 
        int acc2 = pDCR_Handle->acc_x2, prev_x2 = pDCR_Handle->prev_x2, prev_y2 = pDCR_Handle->prev_y2;
        Conumebytes = (*InputLength > *OutputLength)?*OutputLength:*InputLength;        
        for ( Length = 0; Length < (Conumebytes >> 1); Length+=2 )
        {
                // deal with 2 channel
                acc1   -= prev_x1;
                prev_x1 = ((int)*(InputBuffer+Length))<<15;
                acc1   += prev_x1; 
                acc1   -= A_Coeffient * prev_y1;
                prev_y1 = acc1>>15;         // quantization happens here 
                *(OutputBuffer+Length)   = (short)clamp16(prev_y1); // acc has y[n] in upper 17 bits and -e[n] in lower 15 bits         

                acc2   -= prev_x2;
                prev_x2 = ((int)*(InputBuffer+Length+1))<<15;
                acc2   += prev_x2; 
                acc2   -= A_Coeffient * prev_y2;
                prev_y2 = acc2>>15;         // quantization happens here 
                *(OutputBuffer+Length+1)   = (short)clamp16(prev_y2); // acc has y[n] in upper 17 bits and -e[n] in lower 15 bits 

                // store value
                pDCR_Handle->acc_x1 = acc1;
                pDCR_Handle->prev_x1 = prev_x1;
                pDCR_Handle->prev_y1 = prev_y1;
                pDCR_Handle->acc_x2 = acc2;
                pDCR_Handle->prev_x2 = prev_x2;
                pDCR_Handle->prev_y2 = prev_y2;
        }

        // modify output data.
        *InputLength -= Conumebytes;
        *OutputLength = Conumebytes;
    }
    else{

    }
    return Conumebytes;
}

unsigned int  DCR_Process_24(DCRemove_Handle *pHandle,
         long *InputBuffer,
         unsigned int *InputLength,
         long *OutputBuffer,
         unsigned int *OutputLength
         )
{

    DCRemove_Filter *pDCR_Handle = NULL;
    int Conumebytes = 0;
    int Length =0;
    if(pHandle == NULL)
        return 0;    
    pDCR_Handle = (DCRemove_Handle*)pHandle;

    if(pDCR_Handle->mChannels == 1)
    {               
        long long acc = pDCR_Handle->acc_x1;
        long prev_x = pDCR_Handle->prev_x1, prev_y = pDCR_Handle->prev_y1, A_Coeffient = pDCR_Handle->A_Coefficient; 
        Conumebytes = (*InputLength > *OutputLength)?*OutputLength:*InputLength; 
        long *Addr_In, *Addr_Out; 
        int loop_times;
        long  temp;
        Addr_In =  InputBuffer+Length;
        Addr_Out = OutputBuffer+Length;	Conumebytes = (*InputLength > *OutputLength)?*OutputLength:*InputLength;
        loop_times = Conumebytes >> 2;	// sample count = Conumebytes/sizeof(int32) = Conumebytes/4            
        while(loop_times) 
        {
            acc   -= prev_x;
            // Parameter is Q1.31
            prev_x = (*Addr_In++);           // Q1.31
            acc   += prev_x;					// Q1.31
            //acc   -= 0.0001 * prev_y;       // Q0*Q1.31
            temp=(0.0001*prev_y);
            if((acc-temp)<=-2147483647)
            	acc=-2147483647;
            else if ( (acc-temp)>=2147483647)
            	acc=2147483647;
            else
            	acc=acc-temp;
            prev_y = acc;
            *Addr_Out++   = clamp24(prev_y);

            loop_times--;
        }
        pDCR_Handle->acc_x1 = acc;
        pDCR_Handle->prev_x1 = prev_x;
        pDCR_Handle->prev_y1 = prev_y;
        *InputLength -= Conumebytes;
        *OutputLength = Conumebytes;
    }
    else if(pDCR_Handle->mChannels == 2)
    {
        long long acc1 = pDCR_Handle->acc_x1;
        long prev_x1 = pDCR_Handle->prev_x1, prev_y1 = pDCR_Handle->prev_y1, A_Coeffient = pDCR_Handle->A_Coefficient; 
        long long acc2 = pDCR_Handle->acc_x2;
        long prev_x2 = pDCR_Handle->prev_x2, prev_y2 = pDCR_Handle->prev_y2;
        long *Addr_In, *Addr_Out; 
        int loop_times;
        long  temp;
        Addr_In =  InputBuffer+Length;
        Addr_Out = OutputBuffer+Length;	
        Conumebytes = (*InputLength > *OutputLength)?*OutputLength:*InputLength;
        loop_times = (Conumebytes>>3);	// sample count = Conumebytes/sizeof(int32) = Conumebytes/4, +stereo, thus">>3"
        while(loop_times) 
        {
            // deal with 2 channel
            acc1   -= prev_x1;
            // Parameter is Q1.31
            prev_x1 = (*Addr_In++);	            // Q1.31
            acc1   += prev_x1;	                // Q1.31
            temp=(0.0001*prev_y1);
            if((acc1-temp)<=-2147483647)
            	acc1=-2147483647;
            else if ((acc1-temp)>=2147483647)
            	acc1=2147483647;
            else
            	acc1=acc1-temp;
            prev_y1 = acc1;
            *Addr_Out++   = clamp24(prev_y1);

            acc2   -= prev_x2;
            // Parameter is Q1.31
            prev_x2 = (*Addr_In++);              // Q1.31
            acc2   += prev_x2;	                // Q1.31
            //acc2   -= 0.0001 * prev_y2;         // Q0*Q1.31
            temp=(0.0001*prev_y2);
            if((acc2-temp)<=-2147483647)
            	acc2=-2147483647;
            else if ((acc2-temp)>=2147483647)
            	acc2=2147483647;
            else
            	acc2=acc2-temp;
            prev_y2 = acc2;
            *Addr_Out++   = clamp24(prev_y2);

            loop_times--;
        }
        // store value
        pDCR_Handle->acc_x1 = acc1;
        pDCR_Handle->prev_x1 = prev_x1;
        pDCR_Handle->prev_y1 = prev_y1;
        pDCR_Handle->acc_x2 = acc2;
        pDCR_Handle->prev_x2 = prev_x2;
        pDCR_Handle->prev_y2 = prev_y2;
        // modify output data.
        *InputLength -= Conumebytes;
        *OutputLength = Conumebytes;
    }
    else
    {

    }

    return Conumebytes;
}

/*----------------------------------------------------------------------*/
/* Close the process                                                    */
/*----------------------------------------------------------------------*/
void DCR_Close(DCRemove_Handle *pHandle)
{
    DCRemove_Filter *pDCR_Handle = NULL;
    if (pHandle == NULL)
    {
        return;
    }
    pDCR_Handle = (DCRemove_Handle *)pHandle;
    free(pDCR_Handle);
    pDCR_Handle = NULL;
}

//============================================================//
// Reconfig DCR_Handle
// pHandle:             input,
//                      Handle of current DCRemove_Handle.
// pHandle:             input, Handle of current DCRemove_Handle.
// iuChannel:           input, Channel if input process
// iuSampleRate         input, SampleRate of input.
// iuWorkingMode        input, working mode of filter.
//============================================================//
DCRemove_Handle *DCR_ReConfig(DCRemove_Handle *pHandle, unsigned int iuChannel,
                              unsigned int iuSampleRate,
                              unsigned int iuWorkingMode)
{
    DCRemove_Filter *pDCR_Handle = NULL;
    if (pHandle == NULL)
    {
        return NULL;
    }
    pDCR_Handle = (DCRemove_Handle *)pHandle;
    pDCR_Handle->mChannels = iuChannel;
    pDCR_Handle->mSampleRate = iuSampleRate;
    pDCR_Handle->mWorkingMode = iuWorkingMode;
    pDCR_Handle->prev_x1 = 0;
    pDCR_Handle->prev_y1 = 0;
    pDCR_Handle->prev_x2 = 0;
    pDCR_Handle->prev_y2 = 0;
    pDCR_Handle->acc_x1 = 0;
    pDCR_Handle->acc_x2 = 0;
    Init_Pole_para(pDCR_Handle);
    return pDCR_Handle;
}

