#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include "camera_custom_nvram.h"
#include "camera_custom_sensor.h"
#include "image_sensor.h"
#include "kd_imgsensor_define.h"
#include "camera_AE_PLineTable_ov13850trulymipiraw.h"
#include "camera_info_ov13850trulymipiraw.h"
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
        78875,    // i4R_AVG
        16692,    // i4R_STD
        95425,    // i4B_AVG
        21021,    // i4B_STD
        {  // i4P00[9]
            4867500, -2427500, 115000, -735000, 3722500, -422500, -245000, -2665000, 5472500
        },
        {  // i4P10[9]
            2091280, -2280425, 198646, -57842, -57141, 145460, 481302, 679760, -1160979
        },
        {  // i4P01[9]
            1752034, -1958168, 219898, -291397, -190448, 513170, 119507, -285270, 161461
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
            7936,    // u4MaxGain, 16x
            53,    // u4MiniISOGain, ISOxx  
            32,    // u4GainStepUnit, 1x/8 
            13,    // u4PreExpUnit 
            30,    // u4PreMaxFrameRate
            13,    // u4VideoExpUnit  
            30,    // u4VideoMaxFrameRate 
            1024,    // u4Video2PreRatio, 1024 base = 1x 
            13,    // u4CapExpUnit 
            24,    // u4CapMaxFrameRate
            1024,    // u4Cap2PreRatio, 1024 base = 1x
            20,    // u4LensFno, Fno = 2.8
            350    // u4FocusLength_100x
        },
        // rHistConfig
        {
            4,    // u4HistHighThres
            40,    // u4HistLowThres
            2,    // u4MostBrightRatio
            1,    // u4MostDarkRatio
            160,    // u4CentralHighBound
            20,    // u4CentralLowBound
            {240, 230, 220, 210, 200},    // u4OverExpThres[AE_CCT_STRENGTH_NUM] 
            {82, 108, 128, 148, 170},    // u4HistStretchThres[AE_CCT_STRENGTH_NUM] 
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
            0,    // u4StrobeAETarget
            50,    // u4InitIndex
            4,    // u4BackLightWeight
            32,    // u4HistStretchWeight
            4,    // u4AntiOverExpWeight
            0,    // u4BlackLightStrengthIndex
            0,    // u4HistStretchStrengthIndex
            2,    // u4AntiOverExpStrengthIndex
            2,    // u4TimeLPFStrengthIndex
            {1, 3, 5, 7, 8},    // u4LPFConvergeTable[AE_CCT_STRENGTH_NUM] 
            90,    // u4InDoorEV = 9.0, 10 base 
            -4,    // i4BVOffset delta BV = value/10 
            80,    // u4PreviewFlareOffset
            80,    // u4CaptureFlareOffset
            4,    // u4CaptureFlareThres
            80,    // u4VideoFlareOffset
            4,    // u4VideoFlareThres
            32,    // u4StrobeFlareOffset
            2,    // u4StrobeFlareThres
            180,    // u4PrvMaxFlareThres
            0,    // u4PrvMinFlareThres
            180,    // u4VideoMaxFlareThres
            0,    // u4VideoMinFlareThres
            18,    // u4FlatnessThres    // 10 base for flatness condition.
            55    // u4FlatnessStrength
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
                964,    // i4R
                512,    // i4G
                681    // i4B
            }
        },
        // Original XY coordinate of AWB light source
        {
           // Strobe
            {
                128,    // i4X
                -339    // i4Y
            },
            // Horizon
            {
                -385,    // i4X
                -324    // i4Y
            },
            // A
            {
                -286,    // i4X
                -342    // i4Y
            },
            // TL84
            {
                -87,    // i4X
                -361    // i4Y
            },
            // CWF
            {
                -96,    // i4X
                -422    // i4Y
            },
            // DNP
            {
                -29,    // i4X
                -377    // i4Y
            },
            // D65
            {
                128,    // i4X
                -339    // i4Y
            },
            // DF
            {
                0,    // i4X
                0    // i4Y
            }
        },
        // Rotated XY coordinate of AWB light source
        {
            // Strobe
            {
                128,    // i4X
                -339    // i4Y
            },
            // Horizon
            {
                -385,    // i4X
                -324    // i4Y
            },
            // A
            {
                -286,    // i4X
                -342    // i4Y
            },
            // TL84
            {
                -87,    // i4X
                -361    // i4Y
            },
            // CWF
            {
                -96,    // i4X
                -422    // i4Y
            },
            // DNP
            {
                -29,    // i4X
                -377    // i4Y
            },
            // D65
            {
                128,    // i4X
                -339    // i4Y
            },
            // DF
            {
                0,    // i4X
                0    // i4Y
            }
        },
        // AWB gain of AWB light source
        {
            // Strobe 
            {
                964,    // i4R
                512,    // i4G
                681    // i4B
            },
            // Horizon 
            {
                512,    // i4R
                556,    // i4G
                1452    // i4B
            },
            // A 
            {
                552,    // i4R
                512,    // i4G
                1199    // i4B
            },
            // TL84 
            {
                742,    // i4R
                512,    // i4G
                939    // i4B
            },
            // CWF 
            {
                796,    // i4R
                512,    // i4G
                1032    // i4B
            },
            // DNP 
            {
                820,    // i4R
                512,    // i4G
                887    // i4B
            },
            // D65 
            {
                964,    // i4R
                512,    // i4G
                681    // i4B
            },
            // DF 
            {
                512,    // i4R
                512,    // i4G
                512    // i4B
            }
        },
        // Rotation matrix parameter
        {
            0,    // i4RotationAngle
            256,    // i4Cos
            0    // i4Sin
        },
        // Daylight locus parameter
        {
            -127,    // i4SlopeNumerator
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
            -137,    // i4RightBound
            -787,    // i4LeftBound
            -283,    // i4UpperBound
            -383    // i4LowerBound
            },
            // Warm fluorescent
            {
            -137,    // i4RightBound
            -787,    // i4LeftBound
            -383,    // i4UpperBound
            -503    // i4LowerBound
            },
            // Fluorescent
            {
            -79,    // i4RightBound
            -137,    // i4LeftBound
            -271,    // i4UpperBound
            -391    // i4LowerBound
            },
            // CWF
            {
            -79,    // i4RightBound
            -137,    // i4LeftBound
            -391,    // i4UpperBound
            -472    // i4LowerBound
            },
            // Daylight
            {
            153,    // i4RightBound
            -79,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
            },
            // Shade
            {
            513,    // i4RightBound
            153,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
            },
            // Daylight Fluorescent
            {
            153,    // i4RightBound
            -79,    // i4LeftBound
            -419,    // i4UpperBound
            -503    // i4LowerBound
            }
        },
        // PWB light area
        {
            // Reference area
            {
            513,    // i4RightBound
            -787,    // i4LeftBound
            0,    // i4UpperBound
            -503    // i4LowerBound
            },
            // Daylight
            {
            178,    // i4RightBound
            -79,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
            },
            // Cloudy daylight
            {
            278,    // i4RightBound
            103,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
            },
            // Shade
            {
            378,    // i4RightBound
            103,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
            },
            // Twilight
            {
            -79,    // i4RightBound
            -239,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
            },
            // Fluorescent
            {
            178,    // i4RightBound
            -196,    // i4LeftBound
            -289,    // i4UpperBound
            -472    // i4LowerBound
            },
            // Warm fluorescent
            {
            -186,    // i4RightBound
            -386,    // i4LeftBound
            -289,    // i4UpperBound
            -472    // i4LowerBound
            },
            // Incandescent
            {
            -186,    // i4RightBound
            -386,    // i4LeftBound
            -259,    // i4UpperBound
            -419    // i4LowerBound
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
            866,    // i4R
            512,    // i4G
            758    // i4B
            },
            // Cloudy daylight
            {
            1049,    // i4R
            512,    // i4G
            626    // i4B
            },
            // Shade
            {
            1122,    // i4R
            512,    // i4G
            585    // i4B
            },
            // Twilight
            {
            653,    // i4R
            512,    // i4G
            1005    // i4B
            },
            // Fluorescent
            {
            847,    // i4R
            512,    // i4G
            868    // i4B
            },
            // Warm fluorescent
            {
            582,    // i4R
            512,    // i4G
            1262    // i4B
            },
            // Incandescent
            {
            550,    // i4R
            512,    // i4G
            1193    // i4B
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
            6993    // i4OffsetThr
            },
            // Warm fluorescent	
            {
            0,    // i4SliderValue
            4610    // i4OffsetThr
            },
            // Shade
            {
            0,    // i4SliderValue
            1341    // i4OffsetThr
            },
            // Daylight WB gain
            {
            779,    // i4R
            512,    // i4G
            843    // i4B
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
                -513,    // i4RotatedXCoordinate[0]
                -414,    // i4RotatedXCoordinate[1]
                -215,    // i4RotatedXCoordinate[2]
                -157,    // i4RotatedXCoordinate[3]
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
    #include INCLUDE_FILENAME_TSF_PARA
    #include INCLUDE_FILENAME_TSF_DATA
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


