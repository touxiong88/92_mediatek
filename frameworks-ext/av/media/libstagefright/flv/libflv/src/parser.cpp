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

#include <stdlib.h>
#include <stdio.h>
#include "parser.h"
#include "byteio.h"


#define FLV_DEBUG_LOGE(x, ...)   ALOGE(" "x,  ##__VA_ARGS__)
#define FLV_DEBUG_LOGD(x, ...)   ALOGD(" "x,  ##__VA_ARGS__)
#define FLV_DEBUG_LOGV(x, ...)   //ALOGV(" "x,  ##__VA_ARGS__)
#define FLV_DEBUG_LOGM(x, ...)  // ALOGV(" "x,  ##__VA_ARGS__)

flvParser::flvParser(void* source, flv_io_read_func_ptr read, 
                        flv_io_write_func_ptr write, 
                        flv_io_seek_func_ptr seek)

{
    flv_iostream_str iostream;
    iostream.read = read;
    iostream.write = write;
    iostream.seek = seek;
    iostream.source = source;

    mError = FLV_OK;
    mfile =NULL;
    mSeekTable = NULL;
#ifdef FLV_DIRECT_SEEK_SUPPORT
    bUpdateSeekTable = true;
#endif
    //Initialize asf parser library 
    mfile = flv_open_file(&iostream);
    if (!mfile)
    {
            FLV_DEBUG_LOGE("flvParser:Error failed to Initialize FLV parser");
    }  
    FLV_DEBUG_LOGV("flvParser:mfile=0x%08x\n",mfile);
        
}

flvParser::~flvParser()
{
    flv_close();
}

flv_file_str* flvParser::flv_open_file(flv_iostream_str *iostream)
{
    flv_file_str *flvfile = NULL;
    flv_seek_table * pTable=NULL;
        
    if(!iostream){
        FLV_DEBUG_LOGE("flv_open_file: error1, iostreamis NULL\n");
        return NULL;
    }
    flvfile = (flv_file_str *)calloc(1, sizeof(flv_file_str)); //malloc + set0
    if (!flvfile){
        FLV_DEBUG_LOGE("flv_open_file: error2, alloc mem fail\n");
        return NULL;
    }
    FLV_DEBUG_LOGM("[memory] alloc 0x%08x\n",flvfile);    

    flvfile->mIoStream.read = iostream->read;
    flvfile->mIoStream.write = iostream->write;
    flvfile->mIoStream.seek = iostream->seek;
    flvfile->mIoStream.source = iostream->source;

    flvfile->mMeta = (flv_meta_str *)calloc(1, sizeof(flv_meta_str));
    if(!flvfile->mMeta){
        FLV_DEBUG_LOGM("[memory] free 0x%08x\n",flvfile);    
        free(flvfile);    
        flvfile=NULL;    
        FLV_DEBUG_LOGE("flv_open_file: error3, alloc mem fail\n");
        return NULL;
    }
    flvfile->mMeta->audio_codec_id = 0xFF;
    flvfile->mMeta->video_codec_id = 0xFF;
    FLV_DEBUG_LOGM("[memory] alloc 0x%08x\n",flvfile->mMeta);  


    mSeekTable= (flv_seek_table *)calloc(1, sizeof(flv_seek_table));
    if(!mSeekTable){
        FLV_DEBUG_LOGM("[memory] free 0x%08x\n",flvfile->mMeta);    
        free(flvfile->mMeta);
        flvfile->mMeta = NULL;
        FLV_DEBUG_LOGM("[memory] free 0x%08x\n",flvfile);    
        free(flvfile);
        flvfile=NULL;    
        FLV_DEBUG_LOGE("flv_open_file: error4, alloc mem fail\n");
        return NULL;
    }
    FLV_DEBUG_LOGM("[memory] alloc 0x%08x\n",mSeekTable);   

    pTable = (flv_seek_table *)(mSeekTable);

    pTable->pEntry = (flv_seek_table_entry*)calloc(FLV_SEEK_ENTRY_MAX_ENTRIES, sizeof(flv_seek_table_entry));
    pTable->LastTime = 0;
    pTable->MaxEntries= FLV_SEEK_ENTRY_MAX_ENTRIES;
    pTable->SetEntries = 0;
    pTable->TimeGranularity = FLV_SEEK_MAX_TIME_GRANULARITY;//ms
    pTable->RangeTime = 0;//the time range this table covers 
    

    mError = FLV_OK;
    return flvfile;
}


void flvParser::flv_close()
{
    if (mfile)
    {
       if(mfile->mMeta){
            if(mfile->mMeta->filepositions)
            {
                FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",mfile->mMeta->filepositions);    
                free(mfile->mMeta->filepositions);
                mfile->mMeta->filepositions = NULL;
            }
            if(mfile->mMeta->times)
            {
                FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",mfile->mMeta->times);    
                free(mfile->mMeta->times);
                mfile->mMeta->times = NULL;
            }
            FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",mfile->mMeta);    
            free(mfile->mMeta);    
            mfile->mMeta =NULL;
       }
       
       if(mSeekTable){
            if(mSeekTable->pEntry){
                FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",mfile->mMeta);    
                free(mSeekTable->pEntry);  
                mSeekTable->pEntry = NULL;
            }
            FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",mSeekTable);    
            free(mSeekTable); 
            mSeekTable = NULL;
       }
       
       FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",mfile);    
       free(mfile);  
       mfile = NULL;
    }
}

FLV_ERROR_TYPE flvParser::IsflvFile()
{
    char TAG[4];
    int32_t tmp;
    const char string[4]= "FLV";

    mfile->mIoStream.seek(mfile->mIoStream.source, 0, FLV_SEEK_FROM_SET);
    
    tmp = flv_byteio_read((uint8_t*)TAG,3,&(mfile->mIoStream));
    if(tmp < 3)
    {
        FLV_DEBUG_LOGE("flv_parse_header: error read file,tmp=%d\n",tmp);
        return FLV_FILE_READ_ERR;
    }     
    TAG[3] = '\0';

    mfile->mIoStream.seek(mfile->mIoStream.source, 0, FLV_SEEK_FROM_SET);

    tmp = strncmp(TAG, string, 3); 

    if(tmp==0)
    {
        FLV_DEBUG_LOGD("IsflvFile: this is an FLV file\n");
        return FLV_OK;
    }
    FLV_DEBUG_LOGD("IsflvFile:not FLV file, TAG=%s\n",TAG);
    return FLV_ERROR;  

    
}

FLV_ERROR_TYPE flvParser::ParseflvFile()
{
    FLV_ERROR_TYPE ret;
    flv_tag_header_info tag_header;
    
    if(!mfile)
    {
	    return FLV_ERROR;
    }

    mfile->file_hdr_position = 0;    
    
    ret = flv_parse_header();
    if(ret!=FLV_OK)
    {
        return ret;
    } 
    mfile->meta_tag_position = mfile->cur_file_offset;
    FLV_DEBUG_LOGD("ParseflvFile: flv_parse_header done:cur_file_offset=0x%llx\n",mfile->cur_file_offset); 
    
    ret = flv_parse_script();
    if(ret!=FLV_OK)
    {
        return ret;
    }
    mfile->data_tag_position = mfile->cur_file_offset;
    FLV_DEBUG_LOGD("ParseflvFile: flv_parse_script done:cur_file_offset=0x%llx\n",mfile->cur_file_offset); 

    ret = flv_setup_seektable();
    if(ret!=FLV_OK)
    {
        return ret;
    }
    FLV_DEBUG_LOGD("ParseflvFile: flv_setup_seektable done:cur_file_offset=0x%llx\n",mfile->cur_file_offset); 

    return FLV_OK;
    
}


FLV_ERROR_TYPE flvParser::flv_setup_seektable()
{
    uint32_t max_point, min_point,i ,u4SamplingCnt = 1, u4EntryIndx = 0;
    if(mfile->mMeta->filepositions && mfile->mMeta->times)
    {
        mfile->hasSeekTable = 1;
        mSeekTable->MaxEntries = mfile->mMeta->timescnt;
        mSeekTable->SetEntries = mSeekTable->MaxEntries;

        if(mSeekTable->MaxEntries > FLV_SEEK_ENTRY_MAX_ENTRIES)
        {
            u4SamplingCnt = (uint32_t)(mSeekTable->MaxEntries/FLV_SEEK_ENTRY_MAX_ENTRIES) + 1;

            for(i=0;i<mSeekTable->MaxEntries;i+=u4SamplingCnt)
            {
                mSeekTable->pEntry[u4EntryIndx].ulTime = mfile->mMeta->times[i]*1000;//s->ms
                mSeekTable->pEntry[u4EntryIndx++].ulOffset = mfile->mMeta->filepositions[i];
            }
            mSeekTable->MaxEntries = u4EntryIndx;
            mSeekTable->SetEntries = mSeekTable->MaxEntries;
        }
        else
        {
            for(i=0;i<mSeekTable->MaxEntries;i++)
            {
                mSeekTable->pEntry[i].ulTime = mfile->mMeta->times[i]*1000;//s->ms
                mSeekTable->pEntry[i].ulOffset = mfile->mMeta->filepositions[i];
            }
        }
        FLV_DEBUG_LOGD("flv_setup_seektable 1: seek MaxEntries=%d(limit %d)\n",mSeekTable->MaxEntries, FLV_SEEK_ENTRY_MAX_ENTRIES);
        FLV_DEBUG_LOGD("flv_setup_seektable 1: seek TimeGranularity=%lld ms\n",mSeekTable->TimeGranularity);
        FLV_DEBUG_LOGD("flv_setup_seektable 1: seek SetEntries=%d\n",mSeekTable->SetEntries);

        return FLV_OK;

    }
    //update seek table info
    mfile->hasSeekTable = 0;
    mSeekTable->TimeGranularity = FLV_SEEK_MAX_TIME_GRANULARITY;//ms

    min_point = mfile->duration/FLV_SEEK_MAX_TIME_GRANULARITY;
    max_point = mfile->duration/FLV_SEEK_MIN_TIME_GRANULARITY;

    if(mfile->duration == 0)
    {
       // min_point = FLV_SEEK_ENTRY_MAX_ENTRIES>>2;
        max_point = FLV_SEEK_ENTRY_MAX_ENTRIES;
    }
    
    if(FLV_SEEK_ENTRY_MAX_ENTRIES >= max_point)
    {
        mSeekTable->MaxEntries = max_point;
        mSeekTable->TimeGranularity = FLV_SEEK_MIN_TIME_GRANULARITY;            
    }
    else if(FLV_SEEK_ENTRY_MAX_ENTRIES < max_point && FLV_SEEK_ENTRY_MAX_ENTRIES > min_point)
    {
        mSeekTable->MaxEntries = FLV_SEEK_ENTRY_MAX_ENTRIES;
        mSeekTable->TimeGranularity = mfile->duration / FLV_SEEK_ENTRY_MAX_ENTRIES; 
    }
    else if(FLV_SEEK_ENTRY_MAX_ENTRIES <= min_point)
    {
        mSeekTable->MaxEntries = FLV_SEEK_ENTRY_MAX_ENTRIES;
        mSeekTable->TimeGranularity = mfile->duration / FLV_SEEK_ENTRY_MAX_ENTRIES; 
    }

    mSeekTable->SetEntries = 0;
    mSeekTable->RangeTime = 0;
    mSeekTable->LastTime =0;

    FLV_DEBUG_LOGD("flv_setup_seektable 2: seek MaxEntries=%d\n",mSeekTable->MaxEntries);
    FLV_DEBUG_LOGD("flv_setup_seektable 2: seek TimeGranularity=%lld ms\n",mSeekTable->TimeGranularity);
    FLV_DEBUG_LOGD("flv_setup_seektable 2: seek SetEntries=%d \n",mSeekTable->SetEntries);   

    
    return FLV_OK;
}


    

FLV_ERROR_TYPE flvParser::flv_parse_script()
{
    flv_tag_str* pMeta=NULL;
    FLV_ERROR_TYPE ret;
    int32_t tmp;
    
    pMeta = (flv_tag_str *)calloc(1, sizeof(flv_tag_str)); //malloc + set0
    if(!pMeta)
    {
        FLV_DEBUG_LOGE("flv_parse_script: error1,calloc failed \n"); 
        return FLV_ERR_NO_MEMORY; 
    }
    FLV_DEBUG_LOGD("[memory]flv_parse_script: Alloc 0x%08x \n",pMeta); 
    
    while(1)
    {
        ret = flv_read_tag_header(&(pMeta->tag_header)) ;//FLV_TAG_HEADER_SIZE
        if(ret != FLV_OK)
        {
            FLV_DEBUG_LOGE("flv_parse_script: error2\n"); 
            ret = FLV_FILE_READ_ERR; 
            break;
        }
        if(pMeta->tag_header.tag_type!=FLV_TAG_TYPE_META)
        {
            FLV_DEBUG_LOGE("flv_parse_script:this not a script,tag_type is %d,cur_file_offset=0x%llx\n",
                                      pMeta->tag_header.tag_type,mfile->cur_file_offset); 
            mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset,FLV_SEEK_FROM_SET);
            ret = FLV_OK; 
            break;
        }

        //FLV_TAG_TYPE_META 
        if(mfile->hasMeta ==0)
        {        
            pMeta->tag_data = (uint8_t*)malloc(pMeta->tag_header.tag_data_size);
            if(!pMeta->tag_data)
            {
                FLV_DEBUG_LOGE("flv_parse_script: error3,calloc failed \n"); 
                return FLV_ERR_NO_MEMORY; 
            }
            FLV_DEBUG_LOGD("[memory]flv_parse_script: Alloc tag_data 0x%08x \n",pMeta->tag_data); 

            tmp = flv_byteio_read(pMeta->tag_data,pMeta->tag_header.tag_data_size, &mfile->mIoStream); //TAG_DATA size
            if(tmp < 0)
            {
                return FLV_FILE_READ_ERR; 
            }  
            if(tmp < (int)(pMeta->tag_header.tag_data_size))
            {
                ret = FLV_FILE_READ_ERR; 
                FLV_DEBUG_LOGE("flv_parse_script: error4,read failed \n"); 
                break;
            }                
            
            ret = flv_parse_onMetaData(pMeta,mfile->mMeta);

            if(pMeta->tag_data)
            {
                FLV_DEBUG_LOGD("[memory]flv_parse_script: Free 0x%08x \n",pMeta->tag_data); 
                free(pMeta->tag_data);
                pMeta->tag_data =NULL;
            }
        }
        else if(mfile->hasMeta ==1)
        {
           mfile->mIoStream.seek(mfile->mIoStream.source, pMeta->tag_header.tag_data_size,FLV_SEEK_FROM_CUR);
           FLV_DEBUG_LOGD("flv_parse_script: has parserd Meta, not parser this script isze=%d\n",pMeta->tag_header.tag_data_size); 
        }       

		//verify tag size
		uint8_t data[FLV_TAG_PREV_SIZE];
		tmp = flv_byteio_read(data,FLV_TAG_PREV_SIZE, &mfile->mIoStream); 
		
		if(tmp < FLV_TAG_PREV_SIZE)
		{		 
			FLV_DEBUG_LOGE("flv_parse_script(prev_tag): error read file,tmp=%d\n",tmp); 
			return FLV_FILE_READ_ERR;	 
		}  
		pMeta->tag_header.prv_tag_size = flv_byteio_get_4byte(data);
		if(pMeta->tag_header.prv_tag_size != pMeta->tag_header.tag_data_size + FLV_TAG_HEADER_SIZE)
		{
			FLV_DEBUG_LOGE("flv_parse_script tag size err, file offset=0xllx\n", mfile->cur_file_offset); 
		}
		//-- verify tag size end
        
        mfile->cur_file_offset = mfile->cur_file_offset + pMeta->tag_header.tag_data_size 
                                + FLV_TAG_HEADER_SIZE + FLV_TAG_PREV_SIZE;        

    }
    
    if(pMeta->tag_data)
    {
        FLV_DEBUG_LOGD("[memory]flv_parse_script: Free 0x%08x \n",pMeta->tag_data); 
        free(pMeta->tag_data);
        pMeta->tag_data =NULL;
    }
    if(pMeta)
    {
        FLV_DEBUG_LOGD("[memory]flv_parse_script: Free 0x%08x \n",pMeta);
        free(pMeta);
        pMeta = NULL;
     }
        
    if(ret != FLV_OK)
    {
        FLV_DEBUG_LOGE("flv_parse_script: error5\n"); 
        return FLV_ERROR; 
    }
    mfile->duration =(uint64_t) mfile->mMeta->duration;   //ms
    mfile->file_size = (uint64_t)mfile->mMeta->file_size;

    //FLV_DEBUG_LOGD("flv_parse_script:  mfile->cur_file_offset=%lld \n", mfile->cur_file_offset);
   FLV_DEBUG_LOGD("flv_parse_script:  mfile->duration=%lld ,mfile->file_size=%lld\n", mfile->duration,mfile->file_size);

    return FLV_OK;
      
}

FLV_ERROR_TYPE flvParser::flv_parse_amf_obj(
                                   uint8_t* amf_data,uint32_t amf_data_len,uint32_t* offset,char* key,
                                   flv_meta_str* metaInfo,uint32_t depth)
{
    flv_iostream_str* pIostream = &(mfile->mIoStream);
    FLV_AMF_V0_TYPE  amf_type;
    
    char strVal[256];
    uint32_t strsize=0;
    double numVal;
    uint16_t date_offset=0;

    uint32_t array_size=0;

    numVal = 0.0;

    amf_type = (FLV_AMF_V0_TYPE)flv_byteio_get_byte(amf_data+(*offset));
    *offset = *offset+1;

    
    FLV_DEBUG_LOGD("FLV_AMF: amf_type =%d,depth=%d,key=%s,*offset=%d,amf_data_len=%d\n",amf_type,depth,key,*offset,amf_data_len);

    switch(amf_type) 
    {
        case FLV_AMF_V0_TYPE_NUMBER:
        {
            //key= filepostions && depth ==3  
            //key= times && depth ==3  
            numVal = flv_amf_number2double(flv_byteio_get_8byte(amf_data+(*offset))); 
            (*offset) = (*offset)+8;
            FLV_DEBUG_LOGV("FLV_AMF: NUMBE value =%f\n",numVal);  
            break;
        }
        case FLV_AMF_V0_TYPE_BOOL:
        {
            numVal = flv_byteio_get_byte(amf_data+(*offset));
            //LOGD("FLV_AMF: NUMBE value =%d\n",numVal);  
            (*offset) = (*offset)+1;
            break;
        }
        case FLV_AMF_V0_TYPE_STRING:
        {
            strsize = flv_byteio_get_2byte(amf_data+(*offset));
            (*offset) = (*offset)+2;            
            flv_byteio_get_string((uint8_t*)strVal, strsize+1,amf_data+(*offset));
            (*offset) = (*offset)+strsize; 
            //LOGD("FLV_AMF: strVal value =%s\n",strVal);  
            break;
        }
        case FLV_AMF_V0_TYPE_OBJECT: 
        {
            //depth == 1, key =keyframes
            uint32_t end;
            FLV_DEBUG_LOGD("FLV_AMF:  ========START BJECT key =%s ========\n",key);  
            //if(depth==1 && key && (FLV_AMF_V0_TYPE_OBJECT == amf_type && (0 == strncmp(key, "keyframes", 9))))
            if(depth==1 && key && (FLV_AMF_V0_TYPE_OBJECT == amf_type && (0 == strcmp(key, "keyframes"))))
            {
                FLV_DEBUG_LOGD("AMF: file has seek table info\n");
            }

            while(*offset <  amf_data_len)
            {
              FLV_DEBUG_LOGD("FLV_AMF: IN OBJECT: key =%s,*offset=%d\n",strVal,*offset); 
              strsize = flv_byteio_get_2byte(amf_data+(*offset));
              (*offset) = (*offset)+2;
              if(strsize == 0)
              {
                 end = flv_byteio_get_byte(amf_data+(*offset));
                 (*offset) = (*offset)+1;
                 if(end == 9) 
                 {
                     FLV_DEBUG_LOGD(" FLV_AMF:  ========EXIT OBJECT key =%s ===*offset=%d=====\n",key,*offset); 
                     break;
                 }
              }
              flv_byteio_get_string((uint8_t*)strVal, strsize+1,amf_data+(*offset));
              (*offset) = (*offset)+strsize;  

             // FLV_DEBUG_LOGD("FLV_AMF: IN OBJECT: key =%s,*offset=%d\n",strVal,*offset); 

              flv_parse_amf_obj(amf_data,amf_data_len,offset,strVal,metaInfo,depth+1);             
            }                 
            break;
        }
        case FLV_AMF_V0_TYPE_MOVIECLIP:
        case FLV_AMF_V0_TYPE_NULL:
        case FLV_AMF_V0_TYPE_UNDEFINED:
        case FLV_AMF_V0_TYPE_UNSUPPORTED:
        {          
            break;
        }//not handle
        case FLV_AMF_V0_TYPE_LONG_STRING:
        {          
            strsize = flv_byteio_get_4byte(amf_data+(*offset));
            (*offset) = (*offset)+4;            
            flv_byteio_get_string((uint8_t*)strVal, strsize+1,amf_data+(*offset));
            (*offset) = (*offset)+strsize; 
           // LOGD("FLV_AMF: strVal value =%s\n",strVal);
            break;
        } 
        case FLV_AMF_V0_TYPE_DATE:
        {      
            numVal = flv_amf_number2double(flv_byteio_get_8byte(amf_data+(*offset))); 
            (*offset) = (*offset)+8;
            //LOGD("FLV_AMF: date NUMBE value =%f\n",numVal); 
            date_offset = flv_byteio_get_2byte(amf_data+(*offset)); 
            (*offset) = (*offset)+2;
           // LOGD("FLV_AMF: date INT value =%d\n",date_offset); 
            break;
        }
        case FLV_AMF_V0_TYPE_MIXED_ARRAY:
        {          
            //depth == 0
            uint32_t end;
            array_size = flv_byteio_get_4byte(amf_data+(*offset));
            (*offset) = (*offset)+4; //array_size
            FLV_DEBUG_LOGD("FLV_AMF:  ========START MIXED_ARRAY size =%d ========\n",array_size);
            while(*offset <  amf_data_len)
            {
              FLV_DEBUG_LOGD("FLV_AMF: IN MIXED_ARRAY key =%s,*offset=%d\n",strVal,*offset); 
              strsize = flv_byteio_get_2byte(amf_data+(*offset));
              (*offset) = (*offset)+2;
              if(strsize == 0)
              {
                 end = flv_byteio_get_byte(amf_data+(*offset));
                 (*offset) = (*offset)+1;
                 if(end == 9) 
                 {
                     FLV_DEBUG_LOGD(" FLV_AMF:  ========EXIT MIXED_ARRAY key =%s ===*offset=%d=====\n",key,*offset); 
                     break;
                 }
              }
              flv_byteio_get_string((uint8_t*)strVal, strsize+1,amf_data+(*offset));//add '\0' 
              (*offset) = (*offset)+strsize;  
              
             // FLV_DEBUG_LOGD("FLV_AMF: IN MIXED_ARRAY key =%s,*offset=%d\n",strVal,*offset); 

              flv_parse_amf_obj(amf_data,amf_data_len,offset,strVal,metaInfo,depth+1);    //depth ==1    
            }          
            
            break;
        }          
            
        case FLV_AMF_V0_TYPE_ARRAY: 
        {          
            //key= filepostion && depth ==2  
            
            uint32_t i;
            array_size = flv_byteio_get_4byte(amf_data+(*offset));
            (*offset) = (*offset)+4;
            FLV_DEBUG_LOGD("FLV_AMF: strict array size =%d\n",array_size);

            //if(depth==2 && key  && (0 == strncmp(key, "filepositions", 13)) && FLV_AMF_V0_TYPE_ARRAY == amf_type)
            if(depth==2 && key  && (0 == strcmp(key, "filepositions")) && FLV_AMF_V0_TYPE_ARRAY == amf_type)
            {
                metaInfo->fileposcnt = array_size;
                FLV_DEBUG_LOGD("AMF: file has seek filepositions %lld\n",metaInfo->fileposcnt );
                metaInfo->filepositions = (uint64_t*)calloc(metaInfo->fileposcnt,sizeof(uint64_t));
                FLV_DEBUG_LOGD("[memory]AMF: alloc mem 0x%08x\n",metaInfo->filepositions );
            }

            //else if(depth==2 && key  && (0 == strncmp(key, "times", 5)) && FLV_AMF_V0_TYPE_ARRAY == amf_type)
            else if(depth==2 && key  && (0 == strcmp(key, "times")) && FLV_AMF_V0_TYPE_ARRAY == amf_type)
            {
                metaInfo->timescnt = array_size;
                FLV_DEBUG_LOGD("AMF: file has seek times %lld\n",metaInfo->timescnt );
                metaInfo->times = (uint64_t*)calloc(metaInfo->timescnt,sizeof(uint64_t));
                FLV_DEBUG_LOGD("[memory]AMF: alloc mem 0x%08x\n",metaInfo->times );//ms
            }
            
            for(i = 0;i < array_size; i++)
            {
                flv_parse_amf_obj(amf_data,amf_data_len,offset,key,metaInfo,depth+1);       
            }       
            
            break;
        }
        default: //unsupported type, we couldn't skip
            return FLV_ERROR;
    }
   
    

    //FLV_DEBUG_LOGD("FLV_AMF: set value: depth =%d, key=%s, amf_type =%d\n",depth,key,amf_type);
    if(depth==1 && key && (FLV_AMF_V0_TYPE_NUMBER == amf_type || FLV_AMF_V0_TYPE_BOOL == amf_type ))
    {
        //if(0 == strncmp(key, "duration", 8))
        if(0 == strcmp(key, "duration"))
        {
           metaInfo->duration = numVal*1000;//s->ms
        }
        //else if(0 == strncmp(key, "width", 5))
        else if(0 == strcmp(key, "width"))
        {
           metaInfo->width = numVal;
        }
        //else if(0 == strncmp(key, "height", 6))
        else if(0 == strcmp(key, "height"))
        {
           metaInfo->height = numVal;
        }
        //else if(0 == strncmp(key, "videodatarate", 13))
		else if(0 == strcmp(key, "videodatarate"))
        {
           metaInfo->video_data_rate = numVal;
        }
        //else if(0 == strncmp(key, "framerate", 9))
        else if(0 == strcmp(key, "framerate"))
        {
           metaInfo->frame_rate = numVal;
        }
        //else if(0 == strncmp(key, "videocodecid", 12))
        else if(0 == strcmp(key, "videocodecid"))
        {
            
           metaInfo->video_codec_id = numVal;
        }
        //else if(0 == strncmp(key, "audiosamplerate", 15))
        else if(0 == strcmp(key, "audiosamplerate"))
        {
           metaInfo->audio_sample_rate = numVal;
        }
        //else if(0 == strncmp(key, "audiosamplesize", 15))
        else if(0 == strcmp(key, "audiosamplesize"))
        {
           metaInfo->audio_sample_size = numVal;
        }
        //else if(0 == strncmp(key, "stereo", 6))
        else if(0 == strcmp(key, "stereo"))
        {
           metaInfo->stereo = numVal;
        }
        //else if(0 == strncmp(key, "audiocodecid", 12))
        else if(0 == strcmp(key, "audiocodecid"))
        {
           metaInfo->audio_codec_id = numVal;
        }
        //else if(0 == strncmp(key, "filesize", 8))
        else if(0 == strcmp(key, "filesize"))
        {
           metaInfo->file_size = numVal;
        }
        //else if(0 == strncmp(key, "lasttimestamp", 13))
        else if(0 == strcmp(key, "lasttimestamp"))
        {
           metaInfo->last_time_ts = numVal;
        }
        //else if(0 == strncmp(key, "lastkeyframetimestamp", 21))
        else if(0 == strcmp(key, "lastkeyframetimestamp"))
        {
           metaInfo->last_keyframe_ts = numVal;
        }
        //else if(0 == strncmp(key, "audiodelay", 10))
        else if(0 == strcmp(key, "audiodelay"))
        {
           metaInfo->audio_delay = numVal;
        }
        //else if(0 == strncmp(key, "canSeekToEnd", 12))
        else if(0 == strcmp(key, "canSeekToEnd"))
        {
           metaInfo->can_seek_to_end = (bool)numVal;
        }
        //else if(0 == strncmp(key, "audiodatarate", 13))
        else if(0 == strcmp(key, "audiodatarate"))
        {
           metaInfo->audio_data_rate = numVal;
        }
        
    }
    //else if(depth==3 && key  && (0 == strncmp(key, "filepositions", 13)) && FLV_AMF_V0_TYPE_NUMBER == amf_type)
    else if(depth==3 && key  && (0 == strcmp(key, "filepositions")) && FLV_AMF_V0_TYPE_NUMBER == amf_type)
    {
         metaInfo->filepositions[metaInfo->fileposidx++] = (uint64_t)numVal;
    }
    //else if (depth==3 && key  && (0 == strncmp(key, "times", 5)) && FLV_AMF_V0_TYPE_NUMBER == amf_type)
    else if (depth==3 && key  && (0 == strcmp(key, "times")) && FLV_AMF_V0_TYPE_NUMBER == amf_type)
    {
         metaInfo->times[metaInfo->timesidx++] = (uint64_t)numVal;  //s
    }
    return FLV_OK;

}

FLV_ERROR_TYPE flvParser::flv_parse_onMetaData(flv_tag_str* pMeta_tag,flv_meta_str* metaInfo)
{
    FLV_AMF_V0_TYPE type;
    uint32_t offset=0;
    char buffer[11]; //"onMetaData". 
    FLV_ERROR_TYPE ret;

    type = (FLV_AMF_V0_TYPE)flv_byteio_get_byte(pMeta_tag->tag_data);
    offset = offset+1;

    offset =offset +2 ; //string size

    flv_byteio_get_string((uint8_t*)buffer, sizeof(buffer),pMeta_tag->tag_data+ offset);
    
    if(type != FLV_AMF_V0_TYPE_STRING || 0!=strncmp(buffer, "onMetaData",10))
    {
        FLV_DEBUG_LOGE("flv_parse_meta_amf: error1 type=%d,%s\n",type,buffer); 
        return FLV_ERROR;
    }
    offset = offset + 10;
    
    //parse the second object (we want a mixed array)
    ret = flv_parse_amf_obj(pMeta_tag->tag_data ,pMeta_tag->tag_header.tag_data_size ,&offset,"NULL", metaInfo,0) ;
    if(ret == FLV_OK)
    {
        mfile->hasMeta = 1;
     
        FLV_DEBUG_LOGV("flv_parse_onMetaData:metaInfo:audio_codec_id=%f,video_codec_id=%f\n",metaInfo->audio_codec_id,metaInfo->video_codec_id);
        FLV_DEBUG_LOGV("flv_parse_onMetaData:metaInfo:duration=%f,file_size=%f\n",metaInfo->duration,metaInfo->file_size);
        FLV_DEBUG_LOGV("flv_parse_onMetaData:metaInfo:width=%f,height=%f\n",metaInfo->width,metaInfo->height);
        FLV_DEBUG_LOGV("flv_parse_onMetaData:metaInfo:frame_rate=%f,can_seek_to_end=%d\n",metaInfo->frame_rate,metaInfo->can_seek_to_end);
    
    }
    return ret;
}


FLV_ERROR_TYPE flvParser::flv_read_tag_header(flv_tag_header_info* tag_header)
{
    uint8_t data[FLV_TAG_HEADER_SIZE];
    
    uint32_t read_size = FLV_TAG_HEADER_SIZE;
    
    int32_t tmp;     
    
    tmp = flv_byteio_read(data,read_size, &mfile->mIoStream); 
    
    if(tmp < read_size)
    {        
        FLV_DEBUG_LOGE("flv_read_tag_header: error read file,tmp=%d\n",tmp); 
        return FLV_FILE_READ_ERR;    
    }    

    
    //tag_header->prv_tag_size = flv_byteio_get_4byte(data);
    tag_header->tag_type = flv_byteio_get_byte(data);
    tag_header->tag_data_size = flv_byteio_get_3byte(data+1);
    tag_header->tag_ts = flv_byteio_get_3byte(data+4);
    tag_header->tag_ts = flv_byteio_get_byte(data+7)<<24 | tag_header->tag_ts;
    tag_header->streamId = flv_byteio_get_3byte(data+8);
    
    FLV_DEBUG_LOGV("flv_read_tag_header:prv_tag_size=%d,tag_type=%d,tag_data_size=%d,tag_ts=%d\n",
        tag_header->prv_tag_size,tag_header->tag_type, 
        tag_header->tag_data_size,tag_header->tag_ts);

    return FLV_OK;

}


FLV_ERROR_TYPE flvParser::flv_parse_header()
{
    uint8_t data[FLV_FILE_DEADER_SIZE]; //9
    uint32_t read_size = FLV_FILE_DEADER_SIZE;
    int32_t tmp;
    tmp = flv_byteio_read(data,read_size,&mfile->mIoStream);
    if(tmp < read_size)
    {
        FLV_DEBUG_LOGE("flv_parse_header: error read file,tmp=%d\n",tmp);
        return FLV_FILE_READ_ERR;
    }

    mfile->version  = flv_byteio_get_byte(data+3) & 0x000000FF;
    mfile->hasVideo = flv_byteio_get_byte(data+4) & FLV_HAS_VIDEO_BITMASK;
    mfile->hasAudio =( flv_byteio_get_byte(data+4) & FLV_HAS_AUDIO_BITMASK )>> 2;
    mfile->header_size = flv_byteio_get_4byte(data+5);

    // read first prev_tag_size, should be 0 and useless
    tmp = flv_byteio_read(data,FLV_TAG_PREV_SIZE, &mfile->mIoStream); 
    if(tmp < FLV_TAG_PREV_SIZE)
    {        
        FLV_DEBUG_LOGE("flv_parse_header(prev tag): error read file,tmp=%d\n",tmp); 
        return FLV_FILE_READ_ERR;    
    }  

    mfile->cur_file_offset = mfile->header_size + FLV_TAG_PREV_SIZE;        

    FLV_DEBUG_LOGD("flv_parse_header: version=%d,hasVideo=%d, hasAudio=%d,header size =%d\n",
                            mfile->version, mfile->hasVideo,mfile->hasAudio,mfile->header_size);

    tmp = mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset,FLV_SEEK_FROM_SET);

    

    return FLV_OK;        
    
}

flv_tag_str* flvParser::flv_tag_create()
{
    flv_tag_str* tag;
    uint8_t* ptr=NULL;
    
    tag = (flv_tag_str*)calloc(1,sizeof(flv_tag_str));
    if (!tag)
    {
        FLV_DEBUG_LOGE("flv_tag_create: error1, alloc mem fail\n");
        return NULL;
    }
    FLV_DEBUG_LOGM("[memory] alloc 0x%08x\n",tag); 
    
    tag->tag_data = NULL;
    ptr = (uint8_t*)calloc(1,FLV_BS_BUFFER_SIZE);//1000K
    if (!ptr)
    {
        FLV_DEBUG_LOGE("flv_tag_create: error2, alloc mem fail\n");
        return NULL;
    }
    FLV_DEBUG_LOGM("[memory] alloc 0x%08x\n",ptr); 
    tag->tag_data = ptr;
    
    return tag;

}
        
void flvParser::flv_tag_destroy(flv_tag_str *tag)
{
    if (tag)
    {
        if(tag->tag_data)
        {
            FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",tag->tag_data);    
            free(tag->tag_data);
            tag->tag_data =NULL;
            
        }
        FLV_DEBUG_LOGM("[memory] Free 0x%08x\n",tag); 
        free(tag);    
        tag = NULL;
    }    
}


FLV_ERROR_TYPE flvParser::flv_read_a_tag(flv_tag_str *tag)
{
    FLV_ERROR_TYPE ret;
    uint8_t* ptr=NULL;
    FLV_DEBUG_LOGV("flv_read_a_tag IN: file offset=0x%llx\n",mfile->cur_file_offset); 
    
    if(!tag)
    {
        FLV_DEBUG_LOGE("flv_read_a_tag: error input is NULL\n");
        return FLV_ERROR;  
    }

READ_TAG_HEADER:

    ret = flv_read_tag_header(&(tag->tag_header));
    if(ret != FLV_OK)
    {
        FLV_DEBUG_LOGE("flv_read_a_tag: error,read header is NULL\n");
        return ret;  
    }

    if(tag->tag_header.tag_data_size > FLV_BS_BUFFER_SIZE )
    {
        ptr = (uint8_t*)realloc(tag->tag_data,tag->tag_header.tag_data_size);
        if (!ptr)
        {
            FLV_DEBUG_LOGE("flv_read_a_tag: error, alloc mem fail\n");
            return FLV_ERROR;  
        }
        FLV_DEBUG_LOGM("[memory] free 0x%08x\n",tag->tag_data); 
        FLV_DEBUG_LOGM("[memory] alloc 0x%08x\n",ptr); 
        tag->tag_data = ptr;      
    }

    //READ DATA
    int32_t tmp;     
    
    tmp = flv_byteio_read(tag->tag_data,tag->tag_header.tag_data_size, &mfile->mIoStream); 
	if(tmp < 0)
    {        
        FLV_DEBUG_LOGE("flv_read_a_tag: error read file,tmp=%d\n",tmp); 
        return FLV_FILE_READ_ERR;    
    } 
    
    if(tmp < ((int)(tag->tag_header.tag_data_size)))
    {        
        FLV_DEBUG_LOGE("flv_read_a_tag: error read file,tmp=%d\n",tmp); 
        return FLV_FILE_READ_ERR;    
    }  


    //read prev tag size
    uint8_t data[FLV_TAG_PREV_SIZE];
    tmp = flv_byteio_read(data,FLV_TAG_PREV_SIZE, &mfile->mIoStream); 
    
    if(tmp < FLV_TAG_PREV_SIZE)
    {        
        FLV_DEBUG_LOGE("flv_read_a_tag(prev_tag): error read file,tmp=%d\n",tmp); 
        return FLV_FILE_READ_ERR;    
    }  

    //verify tag size
    tag->tag_header.prv_tag_size = flv_byteio_get_4byte(data);
    if((tag->tag_header.prv_tag_size != tag->tag_header.tag_data_size + FLV_TAG_HEADER_SIZE)&&
		((flv_get_videocodecid() == FLV_VIDEO_CODEC_ID_AVC)
		 ||(flv_get_videocodecid() == FLV_VIDEO_CODEC_ID_HEVC)
		 ||(flv_get_videocodecid() == FLV_VIDEO_CODEC_ID_HEVC_XL)
		 ||(flv_get_videocodecid() == FLV_VIDEO_CODEC_ID_HEVC_PPS)))
    {
        FLV_DEBUG_LOGE("flv tag size err, file offset=0x%llx\n", mfile->cur_file_offset); 
        //find next I frame
        #ifdef FLV_DIRECT_SEEK_SUPPORT

        uint64_t u8FileOffset = 0, u8SearchOffset = 0, u8SearchOffsetAcc = 0;
        uint8_t *pu1DataBuf = (uint8_t*)malloc(FLV_BS_BUFFER_SIZE*sizeof(uint8_t)); 
		
        //update file offset
        u8FileOffset = mfile->cur_file_offset + FLV_TAG_HEADER_SIZE + FLV_TAG_PREV_SIZE + tag->tag_header.tag_data_size ;
        while(pu1DataBuf && mfile->hasVideo && u8SearchOffsetAcc<0x1400000)	//search 20MB
        {
            tmp = flv_byteio_read(pu1DataBuf, FLV_BS_BUFFER_SIZE, &mfile->mIoStream); 
			if(tmp < 0)
            {
                FLV_DEBUG_LOGE("flv_read_a_tag(findNextI): read to file end\n"); 
                free(pu1DataBuf);
                pu1DataBuf = NULL;
                return FLV_FILE_READ_ERR;
            }
            u8SearchOffset = (uint64_t)flv_search_tag_pattern(&pu1DataBuf, tmp);
            if (u8SearchOffset < tmp)
            {
                mfile->cur_file_offset = u8FileOffset + u8SearchOffsetAcc + u8SearchOffset + FLV_TAG_PREV_SIZE;
                mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
                free(pu1DataBuf);
                pu1DataBuf = NULL;
                goto READ_TAG_HEADER;
            }
            else if(tmp < FLV_BS_BUFFER_SIZE)
            {
                FLV_DEBUG_LOGE("flv_read_a_tag(findNextI): read to file end\n"); 
                free(pu1DataBuf);
                pu1DataBuf = NULL;
                return FLV_FILE_READ_ERR;
            }
            else
            {
                u8SearchOffsetAcc += FLV_BS_BUFFER_SIZE;
            }
        }
        if(pu1DataBuf)
        {
            free(pu1DataBuf);
            pu1DataBuf = NULL;
            //can't find I frame, recovery to original position			
            mfile->mIoStream.seek(mfile->mIoStream.source, u8FileOffset, FLV_SEEK_FROM_SET);
        }
        else
        {
            FLV_DEBUG_LOGE("flv_read_a_tag(alloc fail)\n"); 
        }
        #endif
    }
	//-- verify tag size end

    tag->tag_data_offset = 0;//put tag data from 0 offset of tag data buffer
    //update seek table
    flv_update_seek_table(tag);

    //update file offset
    mfile->cur_file_offset = mfile->cur_file_offset + FLV_TAG_HEADER_SIZE + FLV_TAG_PREV_SIZE + tag->tag_header.tag_data_size ;

    FLV_DEBUG_LOGV("flv_read_a_tag OUT: file offset=0x%llx\n",mfile->cur_file_offset); 
    return FLV_OK;
}


void flvParser::flv_dump_seektable()
{
    uint32_t i;
    flv_seek_table_entry* pEntry=(flv_seek_table_entry*) mSeekTable->pEntry;
    FLV_DEBUG_LOGD("-----flv_dump_seektable---\n");       
    for(i =0;i< mSeekTable->SetEntries;i++)
    {
        FLV_DEBUG_LOGD("-----entry %d  ts=%lld  offset=%lld---\n",i,pEntry[i].ulTime,pEntry[i].ulOffset);
    }
}
int64_t flvParser::flv_seek_to_msec(int64_t msec)
{
    int64_t newTs;
    uint32_t i;
    FLV_DEBUG_LOGD("flv_seek_to_msec: seekto %lld ms",msec);

    if(msec<=0)//seek to start 
    {
        mfile->cur_file_offset  = mfile->data_tag_position ;
        mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
    #ifdef FLV_DIRECT_SEEK_SUPPORT
	bUpdateSeekTable = true;
    #endif
        FLV_DEBUG_LOGD("flv_seek_to_msec: seet to 0, mfile->cur_file_offset=0x%08x\n",mfile->cur_file_offset);
        return 0;
    }
    
    else if(mfile->hasSeekTable)
    {
        FLV_DEBUG_LOGD("flv_seek_to_msec: hasSeekTable path\n");
        flv_seek_table_entry* pEntry=(flv_seek_table_entry*) mSeekTable->pEntry;
    #ifdef FLV_DIRECT_SEEK_SUPPORT
	bUpdateSeekTable = true;
    #endif

        //flv_dump_seektable();
           
        for(i =0;i< mSeekTable->MaxEntries;i++)
        {
            FLV_DEBUG_LOGV("flv_seek_to_msec1: msec =%lld pEntry[%d].ulTime=%lld, pEntry[%d].ulTime=%lld \n",msec,i,pEntry[i].ulTime , i+1,pEntry[i+1].ulTime);
            if(msec >= pEntry[i].ulTime && msec<pEntry[i+1].ulTime)
                break;
        }

        if(i < mSeekTable->MaxEntries)
        {
            newTs =  pEntry[i].ulTime;
            mfile->cur_file_offset = pEntry[i].ulOffset; //tag start offset
            mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
            FLV_DEBUG_LOGD("flv_seek_to_msec: return 1 %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
            return newTs;
           
        }
        else
        {
            newTs =  pEntry[mSeekTable->MaxEntries-1].ulTime;
            mfile->cur_file_offset = pEntry[mSeekTable->MaxEntries-1].ulOffset; //tag start offset
            mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
            FLV_DEBUG_LOGD("flv_seek_to_msec: return 2 %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
            return newTs;
        }
    }
    else if(mfile->hasVideo)
    {
        FLV_DEBUG_LOGD("flv_seek_to_msec: NOT hasSeekTable path\n");
        flv_seek_table_entry* pEntry= (flv_seek_table_entry*)mSeekTable->pEntry;

        //1. search from the exist table
        for( i =0;i< mSeekTable->SetEntries;i++)
        {
            FLV_DEBUG_LOGV("flv_seek_to_msec2: msec =%lld pEntry[%d].ulTime=%lld, pEntry[%d].ulTime=%lld \n",msec,i,pEntry[i].ulTime , i+1,pEntry[i+1].ulTime);
            if(msec >= pEntry[i].ulTime && msec<pEntry[i+1].ulTime)//concern : the ts gap between 2 entry should > 1/fps
                break;
        }
        if(i < mSeekTable->SetEntries)
        {
        #ifdef FLV_DIRECT_SEEK_SUPPORT
	    bUpdateSeekTable = true;
        #endif
            newTs =  pEntry[i].ulTime;
            mfile->cur_file_offset = pEntry[i].ulOffset; //tag start offset
            mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
            FLV_DEBUG_LOGD("flv_seek_to_msec: return 3 %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
            return newTs;
           
        }
        //2.can not find in exist seek table
        else 
        {
            //2.1. all table entry has set up
            if(mSeekTable->SetEntries == mSeekTable->MaxEntries)
            {
            #ifdef FLV_DIRECT_SEEK_SUPPORT
        	bUpdateSeekTable = true;
            #endif
                newTs =  pEntry[mSeekTable->MaxEntries-1].ulTime;
                mfile->cur_file_offset = pEntry[mSeekTable->MaxEntries-1].ulOffset; //tag start offset
                mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
                FLV_DEBUG_LOGD("flv_seek_to_msec: return 4 %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
                return newTs;
            }
            //2.2. still has empty entry
            else
            {
                flv_tag_str  tag;
                FLV_ERROR_TYPE ret = FLV_ERROR;
                uint64_t ts = 0;

	    #ifdef FLV_DIRECT_SEEK_SUPPORT
                // jump to latest seek point first
		if ((mSeekTable->SetEntries != 0))// && (mfile->cur_file_offset < pEntry[mSeekTable->SetEntries-1].ulOffset))
		{
		    mfile->cur_file_offset = pEntry[mSeekTable->SetEntries-1].ulOffset; //tag start offset
                    mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
		    ts = pEntry[mSeekTable->SetEntries-1].ulTime;
                    FLV_DEBUG_LOGD("flv_seek_to_msec: jump to offset=0x%llx first",mfile->cur_file_offset);
		}
		else
		{
		    ts = 0;
                    mfile->cur_file_offset = mfile->data_tag_position;
                    mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
			
		}
            
		FLV_DEBUG_LOGD("flv_seek_to_msec: time diff = 0x%llx",(msec - ts));
	        // use direct seek method
		if ((msec - ts) > FLV_DIRECT_SEEK_THD)
		{
		    bUpdateSeekTable = false;
		    ret = flv_direct_seek_to_msec(msec, ts, &newTs);
		    if (ret == FLV_OK)
		    {
		        bUpdateSeekTable = false;
			return newTs;
		    }
		}

	        if (ret != FLV_OK)
		{
		bUpdateSeekTable = true;
		// jump to latest seek point first
		if ((mSeekTable->SetEntries != 0))// && (mfile->cur_file_offset < pEntry[mSeekTable->SetEntries-1].ulOffset))
		{
		    mfile->cur_file_offset = pEntry[mSeekTable->SetEntries-1].ulOffset; //tag start offset
                    mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
		    ts = pEntry[mSeekTable->SetEntries-1].ulTime;
                    FLV_DEBUG_LOGD("flv_seek_to_msec: jump to offset=0x%llx first",mfile->cur_file_offset);
		}
		else
		{
                    mfile->cur_file_offset = mfile->data_tag_position;
                    mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
			
		}
	    #endif
                tag.tag_data = (uint8_t*)malloc(FLV_BS_BUFFER_SIZE*sizeof(uint8_t));                
                FLV_DEBUG_LOGM("[memory]flv_seek_to_msec alloc 0x%08x\n",ptr);
                while(1)
                {
                    ret = flv_read_a_tag(&tag);
                    if(ret ==FLV_OK)
                    {
                        ts = pEntry[mSeekTable->SetEntries-1].ulTime;                 
                        if(ts >= msec  )//find tsg when searching
                        {
                            FLV_DEBUG_LOGD("flv_seek_to_msec: find the tag: ts= %lld, entry=%d\n",ts,mSeekTable->SetEntries);
                            break;
                        }
                    }
                    else if(ret == FLV_FILE_READ_ERR)//can not find
                    {
                        FLV_DEBUG_LOGE("flv_seek_to_msec:EOS!!\n");
                        //flv_dump_seektable();
                        break;
                    }
                    else if(ret == FLV_ERROR)//can not find
                    {
                        FLV_DEBUG_LOGE("flv_seek_to_msec:Error!!\n");
                        break;
                    }
                    
                }

                if(tag.tag_data)
                {
                    FLV_DEBUG_LOGM("[memory]flv_seek_to_msec free 0x%08x\n",tag->tag_data); 
                    free(tag.tag_data);
                    tag.tag_data=NULL;
                }
                
                //flv_dump_seektable();
                if(mSeekTable->SetEntries==0 && ret!=FLV_OK)//has no data
                {
                    newTs =  0;
                    mfile->cur_file_offset = mfile->data_tag_position; //tag start offset
                    mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
                    FLV_DEBUG_LOGE("flv_seek_to_msec: return 5:entry %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
                }
                else
                {   
                    //re-search table after setup more
                    for( i =0;i< mSeekTable->SetEntries;i++)
                    {
                        FLV_DEBUG_LOGV("flv_seek_to_msec 6: msec =%lld pEntry[%d].ulTime=%lld, pEntry[%d].ulTime=%lld \n",msec,i,pEntry[i].ulTime , i+1,pEntry[i+1].ulTime);
                        if(msec >= pEntry[i].ulTime && msec<pEntry[i+1].ulTime)//concern : the ts gap between 2 entry should > 1/fps
                            break;
                    }
                    if(i < mSeekTable->SetEntries)
                    {
                        newTs =  pEntry[i].ulTime;
                        mfile->cur_file_offset = pEntry[i].ulOffset; //tag start offset
                        mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
                        FLV_DEBUG_LOGD("flv_seek_to_msec: return 7 %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
                       
                    }
                    else
                    {
                        newTs =  pEntry[mSeekTable->SetEntries-1].ulTime;
                        mfile->cur_file_offset = pEntry[mSeekTable->SetEntries-1].ulOffset; //tag start offset
                        mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
                        FLV_DEBUG_LOGD("flv_seek_to_msec: return 8 entry %d newTs=%lld, offset=0x%llx",i,newTs,mfile->cur_file_offset);
                    }     
                }                         
               
                return newTs;
	    #ifdef FLV_DIRECT_SEEK_SUPPORT
		}
	    #endif
            }           
        }
    }
    // 3. has nothing ,seek to start position
    else 
    {
        mfile->cur_file_offset  = mfile->data_tag_position ;
        mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
        FLV_DEBUG_LOGD("flv_seek_to_msec: set to 0, mfile->cur_file_offset=0x%08x\n",mfile->cur_file_offset);
        return 0;  
    }
    FLV_DEBUG_LOGD("flv_seek_to_msec: return 9 %d newTs=%lld, offset=%lld",i,newTs,mfile->cur_file_offset);
    return msec;
}
      

#ifdef FLV_DIRECT_SEEK_SUPPORT
FLV_ERROR_TYPE flvParser::flv_direct_seek_to_msec(int64_t Trgmsec, int64_t Currmsec, int64_t *Retmsec)
{
    uint32_t cnt = 0;
	int32_t read_size = 0;
    uint64_t file_offset = 0;
    uint64_t file_step = 0;
    uint64_t tag_ts = 0, ts_diff=0;
    uint32_t buf_offset = 0;
    uint8_t* tag_data;
    uint64_t fwd_offset = 0, beh_offset = 0, beh_ts = 0;
    uint64_t seek_diff_thd;
    bool forward = true;
    bool find = false;

    if ((mfile->duration == 0) || (mfile->file_size == 0))
    {
        FLV_DEBUG_LOGD("flv_direct_seek_to_msec : duration or file_size is zero\n"); 
        return FLV_ERROR;  
    }

    tag_data = (uint8_t*)malloc(FLV_BS_BUFFER_SIZE*sizeof(uint8_t)); 
    file_offset = mfile->cur_file_offset + ((Trgmsec - Currmsec - FLV_DIRECT_SEEK_THD)*(mfile->file_size)/mfile->duration);
    file_step = ((FLV_DIRECT_SEEK_THD*(mfile->file_size)/2)/mfile->duration);
    seek_diff_thd = mfile->duration /mSeekTable->MaxEntries;

    while (1)
    {
	mfile->mIoStream.seek(mfile->mIoStream.source, file_offset, FLV_SEEK_FROM_SET);
	FLV_DEBUG_LOGD("flv_direct_seek_to_msec : jump to 0x%llx/0x%llx/0x%llx\n",file_offset, beh_offset, fwd_offset); 
	while(1) 
	{
    	    read_size = flv_byteio_read(tag_data,FLV_BS_BUFFER_SIZE, &mfile->mIoStream); 
		if(read_size < 0)
		{
		    return FLV_FILE_READ_ERR;
		}
	    buf_offset = flv_search_tag_pattern(&tag_data, read_size);
	    if (buf_offset < read_size)
	    {
	        tag_ts = flv_byteio_get_3byte(tag_data+buf_offset+8);
                tag_ts = flv_byteio_get_byte(tag_data+buf_offset+11)<<24 | tag_ts;
		if (tag_ts > Trgmsec)
		{
		    FLV_DEBUG_LOGD("flv_direct_seek_to_msec : find time forward = %lld\n",tag_ts); 
		    forward = false;
		    ts_diff = tag_ts - Trgmsec;
		    if ((file_offset < fwd_offset) || (fwd_offset == 0))
		    {
		        fwd_offset = file_offset;
		    }
		}
		else
		{
		    FLV_DEBUG_LOGD("flv_direct_seek_to_msec : find time behind = %lld\n",tag_ts);
		    ts_diff = Trgmsec - tag_ts;
		    if (ts_diff < seek_diff_thd)
		    {
		        find = true;
		    }
		    if ((file_offset + buf_offset) > beh_offset)
		    {
		        beh_offset = file_offset + buf_offset;
			beh_ts = tag_ts;
		    }
		    forward = true;
		}
	        break;
	    }
	    else if(read_size < FLV_BS_BUFFER_SIZE)
	    {
	        FLV_DEBUG_LOGD("flv_direct_seek_to_msec : read to file end\n"); 
	        forward = false;
		ts_diff = mfile->duration - Trgmsec;
		if ((file_offset < fwd_offset) || (fwd_offset == 0))
		{
		    fwd_offset = file_offset;
		}
	        break;
	    }
	    else
	    {
	        file_offset += FLV_BS_BUFFER_SIZE;
	    }
	}
        if ((find) || (cnt >= SEARCH_THD))
        {
            if(find)
            {
                mfile->cur_file_offset = beh_offset + FLV_TAG_PREV_SIZE;
                *Retmsec = beh_ts;
                //find = true;
            }
            break;
        }
	if ((fwd_offset != 0) && (beh_offset != 0))
	{
	    file_offset = (fwd_offset + beh_offset)/2;
	}
	else
	{
      	    if (forward)
      	    {
      	        if ((file_offset+file_step) < mfile->file_size)
                    file_offset  += file_step;
		else
		   file_offset = (file_offset + mfile->file_size)/2;
      	    }
            else
            {
                if (file_offset > file_step)
        	    file_offset  -= file_step;
		else
		    file_offset = file_offset/2;
            }
	}
	cnt++;
    }
    if(tag_data)
    {
        FLV_DEBUG_LOGM("[memory]flv_direct_seek_to_msec free 0x%08x\n",tag_data); 
        free(tag_data);
        tag_data=NULL;
    }
    FLV_DEBUG_LOGD("flv_direct_seek_to_msec : final offset = 0x%llx\n",mfile->cur_file_offset);
    mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);
    return find ? FLV_OK : FLV_ERROR;
}


uint32_t flvParser::flv_search_tag_pattern(uint8_t **data, uint32_t size)
{
    uint8_t *start = *data;
    uint8_t zerocnt = 0;
    uint32_t offset = size, i=0;
    //char pattern0[8] = {0x00, 0x00, 0x00, 0x17, 0x00, 0x00, 0x00, 0x00};
    //char pattern1[8] = {0x00, 0x00, 0x00, 0x17, 0x01, 0x??, 0x??, 0x??};

    if (0 == size)
    {
    	FLV_DEBUG_LOGD("flv_search_tag_pattern : size=0\n"); 
        return 0;
    }
    do
    {
        do
	{
            if (start[i]  == 0x00)
            {
	        zerocnt++;
            }
	    else
	    {
	        zerocnt = 0;
	    }
	    i++;
        }while(((zerocnt < 3) || (start[i] == 0x00)) && (i < (size - 5)));

	if ((start[i] == 0x17) &&
        (((start[i+1] == 0x00) && (start[i+2] == 0x00) && (start[i+3] == 0x00) && (start[i+4] == 0x00))||
        (start[i+1] == 0x01)))

	{
	    if ((i >= 15) && ((start[i - 11]&0x1F) == 0x09))
	    {
	        offset = i - 15;
	        break;
	    }
	}
    }while(i < (size - 8));

    FLV_DEBUG_LOGD("flv_search_tag_pattern : find offset = 0x%x\n",offset); 
    return offset;
}
#endif
	
                
uint32_t flvParser::flv_search_video_tag_pattern(uint8_t *data, uint32_t size)
{
    uint8_t *start = data;
    uint8_t zerocnt = 0;
    uint32_t offset = 0, i=0, tag_data_size=0, prv_tag_size=0;
    //char pattern0[8] = {0x00, 0x00, 0x00, 0x17, 0x00, 0x00, 0x00, 0x00};
    //char pattern1[8] = {0x00, 0x00, 0x00, 0x17, 0x01, 0x??, 0x??, 0x??};

    FLV_DEBUG_LOGV("flv_search_video_tag_pattern : start=%d\n", start[0]); 
    if (0 == size)
    {
    	FLV_DEBUG_LOGD("flv_search_video_tag_pattern : size=0\n"); 
        return 0;
    }
    do
    {
        do
	{
            if (start[i]  == 0x00)
            {
	        zerocnt++;
            }
	    else
	    {
	        zerocnt = 0;
	    }
	    i++;
        }while(((zerocnt < 3) || (start[i] == 0x00)) && (i < (size - 5)));

       //FLV_DEBUG_LOGD("flv_search_video_tag_pattern : zerocnt=%d, start[i]=%d\n", zerocnt, start[i]); 

	if ((zerocnt >= 3) && 
		((start[i] & 0xF0) >= 0x10) && ((start[i] & 0xF0) <= 0x50) && //Frame Type
		((start[i] & 0x0F) >= 0x02) && ((start[i] & 0x0F) <= 0x07) && //CodecID
	    ((((start[i] & 0x0F) != 0x07) && ((start[i] & 0x0F) != 0x012)) || //neither AVC and HEVC video
	    (((start[i+1] == 0x00) && (start[i+2] == 0x00) && (start[i+3] == 0x00) && (start[i+4] == 0x00)) || //AVC video with sequence header
	    (start[i+1] == 0x01))))//AVC video with NALU
	{
	    //FLV_DEBUG_LOGD("flv_search_video_tag_pattern : i=%d, zerocnt=%d, start[i]=%d\n", i, zerocnt, start[i]); 
	    if ((i >= 15) && ((start[i - 11]&0x1F) == 0x09)) //TagType=video
	    {
	    	//check if DataSize header match prev_data_size
	    	tag_data_size = flv_byteio_get_3byte(&start[i-10]);
	        if((i + tag_data_size + FLV_TAG_HEADER_SIZE) < size)
	        {        
	            prv_tag_size = flv_byteio_get_4byte(&start[i + tag_data_size]);
				//FLV_DEBUG_LOGD("tag_data_size 0x%x, prv_tag_size 0x%x\n", tag_data_size, prv_tag_size); 
	            if(prv_tag_size == tag_data_size + FLV_TAG_HEADER_SIZE)
	            {
	                offset = i;
	                break;
	            }
	        }
	    }
	}
    }while(i < (size - 8));

    FLV_DEBUG_LOGD("flv_search_video_tag_pattern : find offset = 0x%x\n",offset); 
    return offset;
}


uint32_t flvParser::flv_search_audio_tag_pattern(uint8_t *data, uint32_t size)
{
    uint8_t *start = data;
    uint8_t zerocnt = 0;
    uint32_t offset = 0, i=0, tag_data_size=0, prv_tag_size=0;
    //char pattern0[8] = {0x00, 0x00, 0x00, 0xF?};

    FLV_DEBUG_LOGV("flv_search_audio_tag_pattern : start=%d\n", start[0]); 
    if (0 == size)
    {
    	FLV_DEBUG_LOGD("flv_search_audio_tag_pattern : size=0\n"); 
        return 0;
    }
    do
    {
        do
	{
            if (start[i]  == 0x00)
            {
	        zerocnt++;
            }
	    else
	    {
	        zerocnt = 0;
	    }
	    i++;
        }while(((zerocnt < 3) || (start[i] == 0x00)) && (i < (size - 5)));

       //FLV_DEBUG_LOGD("flv_search_audio_tag_pattern : zerocnt=%d, start[i]=%d\n", zerocnt, start[i]); 

	   if ( (zerocnt >= 3) && 
			((start[i] & 0xF0) != 0x07) && //sound format
			((start[i] & 0xF0) != 0x08) && 
			((start[i] & 0xF0) != 0x0e) && 
			((start[i] & 0xF0) != 0x0f)
		   )
	   {
		   //FLV_DEBUG_LOGD("flv_search_audio_tag_pattern : i=%d, zerocnt=%d, start[i]=%d\n", i, zerocnt, start[i]); 
		   if ((i >= 15) && ((start[i - 11]&0x1F) == 0x08)) //TagType=audio
		   {
			   //check if DataSize header match prev_data_size
			   tag_data_size = flv_byteio_get_3byte(&start[i-10]);
			   if((i + tag_data_size + FLV_TAG_HEADER_SIZE) < size)
			   {		
				   prv_tag_size = flv_byteio_get_4byte(&start[i + tag_data_size]);
				   //FLV_DEBUG_LOGD("tag_data_size 0x%x, prv_tag_size 0x%x\n", tag_data_size, prv_tag_size); 
				   if(prv_tag_size == tag_data_size + FLV_TAG_HEADER_SIZE)
				   {
					   offset = i;
					   break;
				   }
			   }
		   }
	   }

    }while(i < (size - 8));

    FLV_DEBUG_LOGD("flv_search_audio_tag_pattern : find offset = 0x%x\n",offset); 
    return offset;
}

                
uint8_t flvParser::flv_get_stream_count()
{
       return 1;
}
        
       
bool flvParser::flv_is_seekable()
{
    if(!mfile)
    {
        FLV_DEBUG_LOGE("flv_is_seekable: !mfile ,can not seek\n"); 
        return false;
    }

    else if(mfile->hasSeekTable)
    {
        return true;
        FLV_DEBUG_LOGD("flv_is_seekable: hasSeekTable can seek\n"); 
    }
    else if(mfile->hasVideo)
    {
        
        FLV_DEBUG_LOGD("flv_is_seekable: hasVideo tag ,can seek\n"); 
        return true;
    }
    else 
    {
        FLV_DEBUG_LOGD("flv_is_seekable: !hasVideo && !hasSeekTabletag ,can not seek\n"); 
        return false;    
    }
    return false;
    
}
        
uint64_t flvParser::flv_get_file_size()
{
    if(mfile){
         return (uint64_t)mfile->file_size;
    }
    FLV_DEBUG_LOGD("flv_get_file_size: error return 0\n"); 
    return 0;
}

void flvParser::flv_set_file_size(uint64_t file_size)
{
	uint64_t u8VideoDuration = 0, u8AudioDuration = 0;
	
    if(mfile && ((0 == mfile->file_size) || (file_size != mfile->file_size))){
    	  if (file_size != mfile->file_size)
    	  {
    	      uint64_t file_offset = 0, timestamp = 0;
             uint8_t* tag_data;
             uint32_t buf_offset = 0, total_offset = 0;
			 int32_t read_size = 0;
    	      FLV_DEBUG_LOGD("flv_set_file_size: file size mismatch,meta/real 0x%x/0x%x\n", mfile->file_size, file_size); 
    	      tag_data = (uint8_t*)malloc(FLV_BS_BUFFER_SIZE*sizeof(uint8_t)); 
             file_offset = (file_size > FLV_BS_BUFFER_SIZE) ? (file_size - FLV_BS_BUFFER_SIZE) : mfile->cur_file_offset;

			 //find video tag
             if(mfile->hasVideo)
             {
                 mfile->mIoStream.seek(mfile->mIoStream.source, file_offset, FLV_SEEK_FROM_SET);
                 read_size = flv_byteio_read(tag_data,FLV_BS_BUFFER_SIZE, &mfile->mIoStream); 
				 if(read_size < 0)
				 {
				 	ALOGE("[Video]read size out of range, force to return");
					if(tag_data)
					{
					    free(tag_data);
					    tag_data = NULL;
					}
				 	return;
				 }
                 FLV_DEBUG_LOGD("flv_set_file_size: start = %d\n", tag_data[0]); 
                 do 
                 {
                     buf_offset = flv_search_video_tag_pattern(&tag_data[total_offset], (read_size-total_offset));
                     total_offset += buf_offset;
                     FLV_DEBUG_LOGV("flv_set_file_size: video total_offset = %d\n", total_offset); 
                 }while((0 != buf_offset) && (total_offset < read_size));


                 if ((0 != total_offset) && (total_offset >= 7))
                 {
        	         u8VideoDuration = flv_byteio_get_3byte(&tag_data[total_offset-7]);
        	         u8VideoDuration |= (flv_byteio_get_byte(&tag_data[total_offset-4]) << 24);
                 }
             }
			 //find audio tag
             if(mfile->hasAudio)
             {
			     buf_offset = 0;
			     total_offset = 0;
                 mfile->mIoStream.seek(mfile->mIoStream.source, file_offset, FLV_SEEK_FROM_SET);
                 read_size = flv_byteio_read(tag_data,FLV_BS_BUFFER_SIZE, &mfile->mIoStream); 
				 if(read_size < 0)
				 {
				 	ALOGE("[Audio]read size out of range, force to return");
					if(tag_data)
					{
					    free(tag_data);
					    tag_data = NULL;
					}
				 	return;
				 }
                 FLV_DEBUG_LOGD("flv_set_file_size: start = %d\n", tag_data[0]); 
                 do 
                 {
                     buf_offset = flv_search_audio_tag_pattern(&tag_data[total_offset], (read_size-total_offset));
                     total_offset += buf_offset;
                     FLV_DEBUG_LOGV("flv_set_file_size: audio total_offset = %d\n", total_offset); 
                 }while((0 != buf_offset) && (total_offset < read_size));


                 if ((0 != total_offset) && (total_offset >= 7))
                 {
        	         u8AudioDuration = flv_byteio_get_3byte(&tag_data[total_offset-7]);
        	         u8AudioDuration |= (flv_byteio_get_byte(&tag_data[total_offset-4]) << 24);
                 }
             }

			 //recorvery current position
             mfile->mIoStream.seek(mfile->mIoStream.source, mfile->cur_file_offset, FLV_SEEK_FROM_SET);

             //update duration
			 if(u8VideoDuration > 0 || u8AudioDuration > 0)
             {
                 FLV_DEBUG_LOGD("flv_set_file_size: old duration/new vid/aud = %lld/%lld/%lld\n", mfile->duration, u8VideoDuration, u8AudioDuration); 
                     mfile->duration = (u8VideoDuration > u8AudioDuration) ? u8VideoDuration : u8AudioDuration;
                 flv_setup_seektable();
             }

    	      if(tag_data)
             {
                 FLV_DEBUG_LOGM("[memory]flv_set_file_size free 0x%08x\n",tag_data); 
                 free(tag_data);
                 tag_data=NULL;
             }
    	  }
         mfile->file_size = file_size;
    }
    FLV_DEBUG_LOGD("flv_set_file_size: %lld\n", file_size); 
    return;
}
        
uint64_t flvParser::flv_get_creation_date()
{
    return 1;
}
        
uint64_t flvParser::flv_get_duration()
{
    if(mfile){
        //FLV_DEBUG_LOGD("flv_get_duration: error return 0\n"); 
        return mfile->duration;
    }
    FLV_DEBUG_LOGD("flv_get_duration: error return 0\n"); 
    return 0;
}
        
uint32_t flvParser::flv_get_max_bitrate()
{
    return 1;
}


bool flvParser::flv_has_video()
{
    if(mfile){
        return mfile->hasVideo;
    }
    FLV_DEBUG_LOGD("flv_has_video: error return false\n");
    return false;
}


bool flvParser::flv_has_audio()
{
    if(mfile){
        return mfile->hasAudio;
    }
     FLV_DEBUG_LOGD("flv_has_audio: error return false\n");
    return false;
}


FLV_VIDEO_CODEC_ID flvParser::flv_get_videocodecid()
{
    if(mfile && mfile->mMeta){
       // FLV_DEBUG_LOGD("flv_get_videocodecid:%f\n",mfile->mMeta->video_codec_id);
        return (FLV_VIDEO_CODEC_ID)(uint32_t)(mfile->mMeta->video_codec_id);
    }
    FLV_DEBUG_LOGD("flv_get_videocodecid: error return FLV_VIDEO_CODEC_ID_UNKHNOWN\n");
    return FLV_VIDEO_CODEC_ID_UNKHNOWN;
}


FLV_AUDIO_CODEC_ID flvParser::flv_get_audiocodecid()
{
    if(mfile && mfile->mMeta){
        //FLV_DEBUG_LOGD("flv_get_audiocodecid:%f\n",mfile->mMeta->audio_codec_id);
        return (FLV_AUDIO_CODEC_ID)(uint32_t)(mfile->mMeta->audio_codec_id);
    }
    FLV_DEBUG_LOGD("flv_get_audiocodecid: error return FLV_AUDIO_CODEC_ID_UNKHNOWN\n");
    return FLV_AUDIO_CODEC_ID_UNKHNOWN;
}

void flvParser::flv_get_resolution(uint32_t* width,uint32_t* height)
{
    if(mfile && mfile->mMeta)
    {
        *width  = (uint32_t)mfile->mMeta->width;
        *height = (uint32_t)mfile->mMeta->height;
    }  
    else
    {
        FLV_DEBUG_LOGD("flv_get_resolution: error return 0\n");
        *width  = 0;
        *height = 0;
    }
}

flv_meta_str* flvParser::flv_get_meta()
{
    if(mfile && mfile->hasMeta)
    {
        return mfile->mMeta;
    }  
    else
    {
        FLV_DEBUG_LOGD("flv_get_meta: error return 0\n");
        return NULL;
    }
}


FLV_ERROR_TYPE flvParser::flv_search_all_seek_tables(int64_t seekTimeMs,
                    int64_t foundTimeMs,uint64_t foundVideoTagPos)
{
		return FLV_OK;
}

int64_t flvParser::flv_update_seek_table(flv_tag_str* cur_tag)
{
    flv_seek_table* table;
    flv_seek_table_entry* pEntry;
    uint8_t check_byte;
    uint32_t i;
    uint32_t seek_cnt;
    uint64_t tag_ts;
    
    table = mSeekTable;

    if(mfile->hasSeekTable)
    {
        FLV_DEBUG_LOGV("flv_update_seek_table: have ssektable already,not update\n");
        return 1;
    }

    if(mSeekTable->SetEntries == mSeekTable->MaxEntries)//set all
    {
        FLV_DEBUG_LOGV("flv_update_seek_table: don't update table as entries full\n");
        return -1;
    }

#ifdef FLV_DIRECT_SEEK_SUPPORT
    if (!bUpdateSeekTable)
    {
        FLV_DEBUG_LOGV("flv_update_seek_table: don't update table because bUpdateSeekTable is false\n");
        return -1;
    }
#endif
    
    if(!cur_tag ||  cur_tag->tag_header.tag_type != FLV_TAG_TYPE_VIDEO)
    {
        FLV_DEBUG_LOGV("flv_update_seek_table: don't update table,as not video tag\n");
        return -1;
    }

    check_byte = *(uint8_t*)(cur_tag->tag_data);
    tag_ts = cur_tag->tag_header.tag_ts; 
    
    if((check_byte & FLV_VIDEO_FRAME_TYPE_BITMASK) != FLV_VIDEO_FRAME_TYPE_KEY)
    {
        FLV_DEBUG_LOGV("flv_update_seek_table: don't update table,as not video key tag\n");
        return -1;
    }     
    
    if(mSeekTable->SetEntries > 0 && tag_ts < (mSeekTable->RangeTime + mSeekTable->TimeGranularity))
    {
        FLV_DEBUG_LOGV("flv_update_seek_table:don't update table as cur ts=%lld < gap %lld\n",tag_ts,(mSeekTable->RangeTime + mSeekTable->TimeGranularity));
        return -1;
    }
       
    pEntry = &(mSeekTable->pEntry[mSeekTable->SetEntries]);
    pEntry->ulTime   = tag_ts;
    pEntry->ulOffset = mfile->cur_file_offset;  // as our read tag : 1st read lastsize
    mSeekTable->RangeTime = tag_ts ;
    FLV_DEBUG_LOGM("flv_update_seek_table: update %d entry,ts=%lld,Offset=0x%llx\n",mSeekTable->SetEntries,pEntry->ulTime,pEntry->ulOffset);
    mSeekTable->SetEntries++;
    return tag_ts;
   
}






























