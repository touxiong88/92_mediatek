#ifndef __DRM_PLAYREADY_DECRYPTOR_H__
#define __DRM_PLAYREADY_DECRYPTOR_H__

#ifdef USE_DRM_FRAMEWORK	
class DrmManagerClient;
#else
#include <stdlib.h>
#include <strings.h>

#endif

namespace android {


#ifndef USE_DRM_FRAMEWORK	
typedef struct
{   
    void * pdrmAppContext;
    void * pdrmDecryptContext;	
    char * pOpaqueBuffer;
    int       u4OpaqueBufferSize;	
    char * pbRevocationBuffer ;	
}DRM_PLAYREADY_SESSION;
#endif

typedef struct AIVPLATEXT_IVDATA {
    unsigned long long qwInitializationVector;
    unsigned long long qwBlockOffset;
    unsigned long  bByteOffset;
} AIVPLATEXT_IVDATA;

typedef struct AIVPLAY_ENCINFO{
    char                                  role[100];	
    unsigned int                   dataSize;                              //total enc buffer size
    unsigned int                   segNum;                               //trunk number	
    AIVPLATEXT_IVDATA    iv[10];                      //IV data for each trunk
    unsigned int                   offset[10];               //pointer to an integer array, each element describe clear data size
    unsigned int                   length[10];              //pointer to an integer array, each element describe enc data size
    unsigned int                   dstHandle;              //true : dstData is a handle; false : dstData is a buffer;
}AIVPLAY_ENCINFO;


class DrmPlayreadyDecryptor {

public:
    DrmPlayreadyDecryptor();
    virtual ~DrmPlayreadyDecryptor();

    status_t Initialize(unsigned int nDataSize, unsigned char * data);
    status_t Decrypt( unsigned int  encData, unsigned int  decData, AIVPLAY_ENCINFO *encInfo);
    status_t Terminate();
protected:


#ifdef USE_DRM_FRAMEWORK		
    DrmManagerClient * mDrmManagerClient;
    sp<DecryptHandle> mDecryptHandle;
#else
    DRM_PLAYREADY_SESSION * m_pDrmPlayreadySession;

#endif


};

}
#endif

