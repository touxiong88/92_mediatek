#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include "camera_custom_nvram.h"
#include "camera_custom_sensor.h"
#include "image_sensor.h"
#include "kd_imgsensor_define.h"
#include "camera_AE_PLineTable_ov8865mipiraw.h"
#include "camera_info_ov8865mipiraw.h"
#include "camera_custom_AEPlinetable.h"
#include "camera_custom_tsf_tbl.h"
const NVRAM_CAMERA_ISP_PARAM_STRUCT CAMERA_ISP_DEFAULT_VALUE =
{{
    //Version
    Version: NVRAM_CAMERA_PARA_FILE_VERSION,
    //SensorId
    SensorId: SENSOR_ID,
    ISPComm:{
        {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        }
    },
    ISPPca:{
        #include INCLUDE_FILENAME_ISP_PCA_PARAM
    },
    ISPRegs:{
        #include INCLUDE_FILENAME_ISP_REGS_PARAM
        },
    ISPMfbMixer:{{
        {//00: MFB mixer for ISO 100
            0x00000000, 0x00000000
        },
        {//01: MFB mixer for ISO 200
            0x00000000, 0x00000000
        },
        {//02: MFB mixer for ISO 400
            0x00000000, 0x00000000
        },
        {//03: MFB mixer for ISO 800
            0x00000000, 0x00000000
        },
        {//04: MFB mixer for ISO 1600
            0x00000000, 0x00000000
        },
        {//05: MFB mixer for ISO 2400
            0x00000000, 0x00000000
        },
        {//06: MFB mixer for ISO 3200
            0x00000000, 0x00000000
        }
    }},
    ISPCcmPoly22:{
        78400,    // i4R_AVG
        18738,    // i4R_STD
        108175,    // i4B_AVG
        27711,    // i4B_STD
        {  // i4P00[9]
            5137500, -2637500, 55000, -835000, 3657500, -265000, -70000, -2452500, 5085000
        },
        {  // i4P10[9]
            140976, -382660, 220395, 68987, -427491, 360463, 124189, 746200, -886423
        },
        {  // i4P01[9]
            59379, -245097, 174726, -152703, -429646, 580764, -54364, -147431, 192934
        },
        {  // i4P20[9]
            0, 0, 0, 0, 0, 0, 0, 0, 0
        },
        {  // i4P11[9]
            0, 0, 0, 0, 0, 0, 0, 0, 0
        },
        {  // i4P02[9]
            0, 0, 0, 0, 0, 0, 0, 0, 0
        }
    }
}};

const NVRAM_CAMERA_3A_STRUCT CAMERA_3A_NVRAM_DEFAULT_VALUE =
{
    NVRAM_CAMERA_3A_FILE_VERSION, // u4Version
    SENSOR_ID, // SensorId

    // AE NVRAM
    {
        // rDevicesInfo
        {
            1144,    // u4MinGain, 1024 base = 1x
            8192,    // u4MaxGain, 16x
            80,    // u4MiniISOGain, ISOxx  
            128,    // u4GainStepUnit, 1x/8 
            27,    // u4PreExpUnit 
            31,    // u4PreMaxFrameRate
            18,    // u4VideoExpUnit  
            31,    // u4VideoMaxFrameRate 
            1024,    // u4Video2PreRatio, 1024 base = 1x 
            14,    // u4CapExpUnit 
            30,    // u4CapMaxFrameRate
            1024,    // u4Cap2PreRatio, 1024 base = 1x
            22,    // u4LensFno, Fno = 2.8
            350    // u4FocusLength_100x
        },
        // rHistConfig
        {
            2,    // u4HistHighThres
            40,    // u4HistLowThres
            2,    // u4MostBrightRatio
            1,    // u4MostDarkRatio
            160,    // u4CentralHighBound
            20,    // u4CentralLowBound
            {240, 230, 220, 210, 200},    // u4OverExpThres[AE_CCT_STRENGTH_NUM] 
            {86, 108, 128, 148, 170},    // u4HistStretchThres[AE_CCT_STRENGTH_NUM] 
            {18, 22, 26, 30, 34}    // u4BlackLightThres[AE_CCT_STRENGTH_NUM] 
        },
        // rCCTConfig
        {
            TRUE,    // bEnableBlackLight
            TRUE,    // bEnableHistStretch
            FALSE,    // bEnableAntiOverExposure
            TRUE,    // bEnableTimeLPF
            TRUE,    // bEnableCaptureThres
            TRUE,    // bEnableVideoThres
            TRUE,    // bEnableStrobeThres
            47,    // u4AETarget
            47,    // u4StrobeAETarget
            50,    // u4InitIndex
            4,    // u4BackLightWeight
            32,    // u4HistStretchWeight
            4,    // u4AntiOverExpWeight
            2,    // u4BlackLightStrengthIndex
            2,    // u4HistStretchStrengthIndex
            2,    // u4AntiOverExpStrengthIndex
            2,    // u4TimeLPFStrengthIndex
            {1, 3, 5, 7, 8},    // u4LPFConvergeTable[AE_CCT_STRENGTH_NUM] 
            90,    // u4InDoorEV = 9.0, 10 base 
            -10,    // i4BVOffset delta BV = value/10 
            4,    // u4PreviewFlareOffset
            4,    // u4CaptureFlareOffset
            5,    // u4CaptureFlareThres
            4,    // u4VideoFlareOffset
            5,    // u4VideoFlareThres
            2,    // u4StrobeFlareOffset
            2,    // u4StrobeFlareThres
            8,    // u4PrvMaxFlareThres
            0,    // u4PrvMinFlareThres
            8,    // u4VideoMaxFlareThres
            0,    // u4VideoMinFlareThres
            18,    // u4FlatnessThres    // 10 base for flatness condition.
            75    // u4FlatnessStrength
        }
    },
    // AWB NVRAM
    {
        // AWB calibration data
        {
            // rUnitGain (unit gain: 1.0 = 512)
            {
                0,    // i4R
                0,    // i4G
                0    // i4B
            },
            // rGoldenGain (golden sample gain: 1.0 = 512)
            {
                0,    // i4R
                0,    // i4G
                0    // i4B
            },
            // rTuningUnitGain (Tuning sample unit gain: 1.0 = 512)
            {
                0,    // i4R
                0,    // i4G
                0    // i4B
            },
            // rD65Gain (D65 WB gain: 1.0 = 512)
            {
                1084,    // i4R
                512,    // i4G
                702    // i4B
            }
        },
        // Original XY coordinate of AWB light source
        {
           // Strobe
            {
                123,    // i4X
                -475    // i4Y
            },
            // Horizon
            {
                -401,    // i4X
                -439    // i4Y
            },
            // A
            {
                -271,    // i4X
                -441    // i4Y
            },
            // TL84
            {
                -152,    // i4X
                -432    // i4Y
            },
            // CWF
            {
                -79,    // i4X
                -500    // i4Y
            },
            // DNP
            {
                -14,    // i4X
                -433    // i4Y
            },
            // D65
            {
                160,    // i4X
                -394    // i4Y
            },
            // DF
            {
                121,    // i4X
                -475    // i4Y
            }
        },
        // Rotated XY coordinate of AWB light source
        {
            // Strobe
            {
                82,    // i4X
                -484    // i4Y
            },
            // Horizon
            {
                -437,    // i4X
                -403    // i4Y
            },
            // A
            {
                -308,    // i4X
                -416    // i4Y
            },
            // TL84
            {
                -189,    // i4X
                -417    // i4Y
            },
            // CWF
            {
                -122,    // i4X
                -491    // i4Y
            },
            // DNP
            {
                -51,    // i4X
                -430    // i4Y
            },
            // D65
            {
                126,    // i4X
                -406    // i4Y
            },
            // DF
            {
                80,    // i4X
                -484    // i4Y
            }
        },
        // AWB gain of AWB light source
        {
            // Strobe 
            {
                1149,    // i4R
                512,    // i4G
                825    // i4B
            },
            // Horizon 
            {
                539,    // i4R
                512,    // i4G
                1596    // i4B
            },
            // A 
            {
                644,    // i4R
                512,    // i4G
                1342    // i4B
            },
            // TL84 
            {
                748,    // i4R
                512,    // i4G
                1129    // i4B
            },
            // CWF 
            {
                906,    // i4R
                512,    // i4G
                1122    // i4B
            },
            // DNP 
            {
                903,    // i4R
                512,    // i4G
                938    // i4B
            },
            // D65 
            {
                1084,    // i4R
                512,    // i4G
                702    // i4B
            },
            // DF 
            {
                1147,    // i4R
                512,    // i4G
                826    // i4B
            }
        },
        // Rotation matrix parameter
        {
            5,    // i4RotationAngle
            255,    // i4Cos
            22    // i4Sin
        },
        // Daylight locus parameter
        {
            -155,    // i4SlopeNumerator
            128    // i4SlopeDenominator
        },
        // AWB light area
        {
            // Strobe:FIXME
            {
            0,    // i4RightBound
            0,    // i4LeftBound
            0,    // i4UpperBound
            0    // i4LowerBound
            },
            // Tungsten
            {
            -239,    // i4RightBound
            -889,    // i4LeftBound
            -324,    // i4UpperBound
            -459    // i4LowerBound
            },
            // Warm fluorescent
            {
            -239,    // i4RightBound
            -889,    // i4LeftBound
            -459,    // i4UpperBound
            -579    // i4LowerBound
            },
            // Fluorescent
            {
            -101,    // i4RightBound
            -239,    // i4LeftBound
            -342,    // i4UpperBound
            -444    // i4LowerBound
            },
            // CWF
            {
            -101,    // i4RightBound
            -239,    // i4LeftBound
            -444,    // i4UpperBound
            -541    // i4LowerBound
            },
            // Daylight
            {
            151,    // i4RightBound
            -101,    // i4LeftBound
            -326,    // i4UpperBound
            -440    // i4LowerBound
            },
            // Shade
            {
            511,    // i4RightBound
            151,    // i4LeftBound
            -326,    // i4UpperBound
            -486    // i4LowerBound
            },
            // Daylight Fluorescent
            {
            151,    // i4RightBound
            -101,    // i4LeftBound
            -440,    // i4UpperBound
            -580    // i4LowerBound
            }
        },
        // PWB light area
        {
            // Reference area
            {
            511,    // i4RightBound
            -889,    // i4LeftBound
            0,    // i4UpperBound
            -580    // i4LowerBound
            },
            // Daylight
            {
            176,    // i4RightBound
            -101,    // i4LeftBound
            -326,    // i4UpperBound
            -440    // i4LowerBound
            },
            // Cloudy daylight
            {
            276,    // i4RightBound
            101,    // i4LeftBound
            -326,    // i4UpperBound
            -440    // i4LowerBound
            },
            // Shade
            {
            376,    // i4RightBound
            101,    // i4LeftBound
            -326,    // i4UpperBound
            -440    // i4LowerBound
            },
            // Twilight
            {
            -101,    // i4RightBound
            -261,    // i4LeftBound
            -326,    // i4UpperBound
            -440    // i4LowerBound
            },
            // Fluorescent
            {
            176,    // i4RightBound
            -289,    // i4LeftBound
            -356,    // i4UpperBound
            -541    // i4LowerBound
            },
            // Warm fluorescent
            {
            -208,    // i4RightBound
            -408,    // i4LeftBound
            -356,    // i4UpperBound
            -541    // i4LowerBound
            },
            // Incandescent
            {
            -208,    // i4RightBound
            -408,    // i4LeftBound
            -326,    // i4UpperBound
            -440    // i4LowerBound
            },
            // Gray World
            {
            5000,    // i4RightBound
            -5000,    // i4LeftBound
            5000,    // i4UpperBound
            -5000    // i4LowerBound
            }
        },
        // PWB default gain	
        {
            // Daylight
            {
            940,    // i4R
            512,    // i4G
            777    // i4B
            },
            // Cloudy daylight
            {
            1132,    // i4R
            512,    // i4G
            623    // i4B
            },
            // Shade
            {
            1204,    // i4R
            512,    // i4G
            579    // i4B
            },
            // Twilight
            {
            718,    // i4R
            512,    // i4G
            1070    // i4B
            },
            // Fluorescent
            {
            921,    // i4R
            512,    // i4G
            967    // i4B
            },
            // Warm fluorescent
            {
            676,    // i4R
            512,    // i4G
            1398    // i4B
            },
            // Incandescent
            {
            614,    // i4R
            512,    // i4G
            1289    // i4B
            },
            // Gray World
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            }
        },
        // AWB preference color	
        {
            // Tungsten
            {
            0,    // i4SliderValue
            6569    // i4OffsetThr
            },
            // Warm fluorescent	
            {
            0,    // i4SliderValue
            5995    // i4OffsetThr
            },
            // Shade
            {
            0,    // i4SliderValue
            1342    // i4OffsetThr
            },
            // Daylight WB gain
            {
            872,    // i4R
            512,    // i4G
            910    // i4B
            },
            // Preference gain: strobe
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: tungsten
            {
            500,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: warm fluorescent
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: fluorescent
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: CWF
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: daylight
            {
            516,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: shade
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: daylight fluorescent
            {
            490,    // i4R
            512,    // i4G
            512    // i4B
            }
        },
        {// CCT estimation
            {// CCT
                2300,    // i4CCT[0]
                2850,    // i4CCT[1]
                4100,    // i4CCT[2]
                5100,    // i4CCT[3]
                6500    // i4CCT[4]
            },
            {// Rotated X coordinate
                -563,    // i4RotatedXCoordinate[0]
                -434,    // i4RotatedXCoordinate[1]
                -315,    // i4RotatedXCoordinate[2]
                -177,    // i4RotatedXCoordinate[3]
                0    // i4RotatedXCoordinate[4]
            }
        }
    },
    {0}
};

#include INCLUDE_FILENAME_ISP_LSC_PARAM
//};  //  namespace

const CAMERA_TSF_TBL_STRUCT CAMERA_TSF_DEFAULT_VALUE =
{
    #include "camera_tsf_para_ov8865mipiraw.h"
    #include "camera_tsf_data_ov8865mipiraw.h"
};


typedef NSFeature::RAWSensorInfo<SENSOR_ID> SensorInfoSingleton_T;


namespace NSFeature {
template <>
UINT32
SensorInfoSingleton_T::
impGetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, VOID*const pDataBuf, UINT32 const size) const
{
    UINT32 dataSize[CAMERA_DATA_TYPE_NUM] = {sizeof(NVRAM_CAMERA_ISP_PARAM_STRUCT),
                                             sizeof(NVRAM_CAMERA_3A_STRUCT),
                                             sizeof(NVRAM_CAMERA_SHADING_STRUCT),
                                             sizeof(NVRAM_LENS_PARA_STRUCT),
                                             sizeof(AE_PLINETABLE_T),
                                             0,
                                             sizeof(CAMERA_TSF_TBL_STRUCT)};

    if (CameraDataType > CAMERA_DATA_TSF_TABLE || NULL == pDataBuf || (size < dataSize[CameraDataType]))
    {
        return 1;
    }

    switch(CameraDataType)
    {
        case CAMERA_NVRAM_DATA_ISP:
            memcpy(pDataBuf,&CAMERA_ISP_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_ISP_PARAM_STRUCT));
            break;
        case CAMERA_NVRAM_DATA_3A:
            memcpy(pDataBuf,&CAMERA_3A_NVRAM_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_3A_STRUCT));
            break;
        case CAMERA_NVRAM_DATA_SHADING:
            memcpy(pDataBuf,&CAMERA_SHADING_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_SHADING_STRUCT));
            break;
        case CAMERA_DATA_AE_PLINETABLE:
            memcpy(pDataBuf,&g_PlineTableMapping,sizeof(AE_PLINETABLE_T));
            break;
        case CAMERA_DATA_TSF_TABLE:
            memcpy(pDataBuf,&CAMERA_TSF_DEFAULT_VALUE,sizeof(CAMERA_TSF_TBL_STRUCT));
            break;
        default:
            break;
    }
    return 0;
}}; // NSFeature


