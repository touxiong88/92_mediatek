#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include "camera_custom_nvram.h"
#include "camera_custom_sensor.h"
#include "image_sensor.h"
#include "kd_imgsensor_define.h"
#include "camera_AE_PLineTable_gc5004mipiraw.h"
#include "camera_info_gc5004mipiraw.h"
#include "camera_custom_AEPlinetable.h"
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
        63225,    // i4R_AVG
        12686,    // i4R_STD
        91900,    // i4B_AVG
        25068,    // i4B_STD
        {  // i4P00[9]
            5735000, -2735000, -440000, -892500, 3430000, 22500, 112500, -3142500, 5585000
        },
        {  // i4P10[9]
            1244269, -1323385, 79116, -99715, -83282, 180755, 5538, 268015, -268454
        },
        {  // i4P01[9]
            722094, -934777, 212682, -184706, -265630, 456332, -35070, -506136, 549457
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
            1632,    // u4MinGain, 1024 base = 1x
            7086,    // u4MaxGain, 16x
            52,    // u4MiniISOGain, ISOxx  
            128,    // u4GainStepUnit, 1x/8 
            20000,    // u4PreExpUnit 
            30,    // u4PreMaxFrameRate
            20000,    // u4VideoExpUnit  
            30,    // u4VideoMaxFrameRate 
            1024,    // u4Video2PreRatio, 1024 base = 1x 
            50000,    // u4CapExpUnit 
            30,    // u4CapMaxFrameRate
            1024,    // u4Cap2PreRatio, 1024 base = 1x
            28,    // u4LensFno, Fno = 2.8
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
            FALSE,    // bEnableCaptureThres
            FALSE,    // bEnableVideoThres
            FALSE,    // bEnableStrobeThres
            47,    // u4AETarget
            47,    // u4StrobeAETarget
            40,    // u4InitIndex
            4,    // u4BackLightWeight
            32,    // u4HistStretchWeight
            4,    // u4AntiOverExpWeight
            4,    // u4BlackLightStrengthIndex
            2,    // u4HistStretchStrengthIndex
            2,    // u4AntiOverExpStrengthIndex
            2,    // u4TimeLPFStrengthIndex
            {1, 3, 5, 7, 8},    // u4LPFConvergeTable[AE_CCT_STRENGTH_NUM] 
            90,    // u4InDoorEV = 9.0, 10 base 
            2,    // i4BVOffset delta BV = value/10 
            64,    // u4PreviewFlareOffset
            64,    // u4CaptureFlareOffset
            5,    // u4CaptureFlareThres
            64,    // u4VideoFlareOffset
            5,    // u4VideoFlareThres
            64,    // u4StrobeFlareOffset
            5,    // u4StrobeFlareThres
            50,    // u4PrvMaxFlareThres
            0,    // u4PrvMinFlareThres
            50,    // u4VideoMaxFlareThres
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
                754,    // i4R
                512,    // i4G
                547    // i4B
            }
        },
        // Original XY coordinate of AWB light source
        {
           // Strobe
            {
                -445,    // i4X
                -241    // i4Y
            },
            // Horizon
            {
                -445,    // i4X
                -241    // i4Y
            },
            // A
            {
                -320,    // i4X
                -238    // i4Y
            },
            // TL84
            {
                -151,    // i4X
                -253    // i4Y
            },
            // CWF
            {
                -75,    // i4X
                -344    // i4Y
            },
            // DNP
            {
                -37,    // i4X
                -159    // i4Y
            },
            // D65
            {
                119,    // i4X
                -167    // i4Y
            },
            // DF
            {
                119,    // i4X
                -167    // i4Y
            }
        },
        // Rotated XY coordinate of AWB light source
        {
            // Strobe
            {
                -477,    // i4X
                -169    // i4Y
            },
            // Horizon
            {
                -477,    // i4X
                -169    // i4Y
            },
            // A
            {
                -353,    // i4X
                -185    // i4Y
            },
            // TL84
            {
                -189,    // i4X
                -226    // i4Y
            },
            // CWF
            {
                -128,    // i4X
                -328    // i4Y
            },
            // DNP
            {
                -61,    // i4X
                -151    // i4Y
            },
            // D65
            {
                92,    // i4X
                -184    // i4Y
            },
            // DF
            {
                92,    // i4X
                -184    // i4Y
            }
        },
        // AWB gain of AWB light source
        {
            // Strobe 
            {
                512,    // i4R
                675,    // i4G
                1708    // i4B
            },
            // Horizon 
            {
                512,    // i4R
                675,    // i4G
                1708    // i4B
            },
            // A 
            {
                512,    // i4R
                573,    // i4G
                1219    // i4B
            },
            // TL84 
            {
                587,    // i4R
                512,    // i4G
                884    // i4B
            },
            // CWF 
            {
                737,    // i4R
                512,    // i4G
                903    // i4B
            },
            // DNP 
            {
                604,    // i4R
                512,    // i4G
                668    // i4B
            },
            // D65 
            {
                754,    // i4R
                512,    // i4G
                547    // i4B
            },
            // DF 
            {
                754,    // i4R
                512,    // i4G
                547    // i4B
            }
        },
        // Rotation matrix parameter
        {
            9,    // i4RotationAngle
            253,    // i4Cos
            40    // i4Sin
        },
        // Daylight locus parameter
        {
            -176,    // i4SlopeNumerator
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
            -300,    // i4RightBound
            -889,    // i4LeftBound
            -127,    // i4UpperBound
            -227    // i4LowerBound
            },
            // Warm fluorescent
            {
            -300,    // i4RightBound
            -889,    // i4LeftBound
            -227,    // i4UpperBound
            -347    // i4LowerBound
            },
            // Fluorescent
            {
            -115,    // i4RightBound
            -300,    // i4LeftBound
            -115,    // i4UpperBound
            -277    // i4LowerBound
            },
            // CWF
            {
            -115,    // i4RightBound
            -300,    // i4LeftBound
            -277,    // i4UpperBound
            -378    // i4LowerBound
            },
            // Daylight
            {
            117,    // i4RightBound
            -115,    // i4LeftBound
            -104,    // i4UpperBound
            -340    // i4LowerBound
            },
            // Shade
            {
            477,    // i4RightBound
            117,    // i4LeftBound
            -104,    // i4UpperBound
            -264    // i4LowerBound
            },
            // Daylight Fluorescent
            {
            0,    // i4RightBound
            0,    // i4LeftBound
            0,    // i4UpperBound
            0    // i4LowerBound
            }
        },
        // PWB light area
        {
            // Reference area
            {
            477,    // i4RightBound
            -889,    // i4LeftBound
            0,    // i4UpperBound
            -378    // i4LowerBound
            },
            // Daylight
            {
            142,    // i4RightBound
            -115,    // i4LeftBound
            -104,    // i4UpperBound
            -340    // i4LowerBound
            },
            // Cloudy daylight
            {
            242,    // i4RightBound
            67,    // i4LeftBound
            -104,    // i4UpperBound
            -340    // i4LowerBound
            },
            // Shade
            {
            342,    // i4RightBound
            67,    // i4LeftBound
            -104,    // i4UpperBound
            -340    // i4LowerBound
            },
            // Twilight
            {
            -115,    // i4RightBound
            -275,    // i4LeftBound
            -104,    // i4UpperBound
            -340    // i4LowerBound
            },
            // Fluorescent
            {
            142,    // i4RightBound
            -289,    // i4LeftBound
            -134,    // i4UpperBound
            -378    // i4LowerBound
            },
            // Warm fluorescent
            {
            -253,    // i4RightBound
            -453,    // i4LeftBound
            -134,    // i4UpperBound
            -378    // i4LowerBound
            },
            // Incandescent
            {
            -253,    // i4RightBound
            -453,    // i4LeftBound
            -104,    // i4UpperBound
            -340    // i4LowerBound
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
            733,    // i4R
            512,    // i4G
            644    // i4B
            },
            // Cloudy daylight
            {
            859,    // i4R
            512,    // i4G
            517    // i4B
            },
            // Shade
            {
            909,    // i4R
            512,    // i4G
            479    // i4B
            },
            // Twilight
            {
            580,    // i4R
            512,    // i4G
            889    // i4B
            },
            // Fluorescent
            {
            701,    // i4R
            512,    // i4G
            765    // i4B
            },
            // Warm fluorescent
            {
            512,    // i4R
            512,    // i4G
            1179    // i4B
            },
            // Incandescent
            {
            485,    // i4R
            512,    // i4G
            1135    // i4B
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
            6632    // i4OffsetThr
            },
            // Warm fluorescent	
            {
            0,    // i4SliderValue
            6412    // i4OffsetThr
            },
            // Shade
            {
            0,    // i4SliderValue
            1345    // i4OffsetThr
            },
            // Daylight WB gain
            {
            636,    // i4R
            512,    // i4G
            692    // i4B
            },
            // Preference gain: strobe
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: tungsten
            {
            512,    // i4R
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
            512,    // i4R
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
            512,    // i4R
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
                -569,    // i4RotatedXCoordinate[0]
                -445,    // i4RotatedXCoordinate[1]
                -281,    // i4RotatedXCoordinate[2]
                -153,    // i4RotatedXCoordinate[3]
                0    // i4RotatedXCoordinate[4]
            }
        }
    },
    {0}
};

#include INCLUDE_FILENAME_ISP_LSC_PARAM
//};  //  namespace


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
                                             sizeof(AE_PLINETABLE_T)};

    if (CameraDataType > CAMERA_DATA_AE_PLINETABLE || NULL == pDataBuf || (size < dataSize[CameraDataType]))
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
        default:
            break;
    }
    return 0;
}}; // NSFeature


