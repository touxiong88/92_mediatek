#include <utils/Log.h>
#include <fcntl.h>
#include <math.h>

#include "camera_custom_nvram.h"
#include "camera_custom_sensor.h"
#include "image_sensor.h"
#include "kd_imgsensor_define.h"
#include "camera_AE_PLineTable_ov13850mipiraw.h"
#include "camera_info_ov13850mipiraw.h"
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
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
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
        71700,    // i4R_AVG
        16311,    // i4R_STD
        94000,    // i4B_AVG
        21567,    // i4B_STD
        {  // i4P00[9]
            4457500, -2097500, 200000, -755000, 3712500, -382500, -137500, -2175000, 4872500
        },
        {  // i4P10[9]
            1626216, -1601087, -47072, 57512, 139648, -162621, -10885, -645230, 656115
        },
        {  // i4P01[9]
            1172228, -1118183, -70569, -105731, -270, 138937, -147535, -1302505, 1450041
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
            11,    // u4PreExpUnit 
            30,    // u4PreMaxFrameRate
            11,    // u4VideoExpUnit  
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
            44,    // u4AETarget
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
                931,    // i4R
                512,    // i4G
                651    // i4B
            }
        },
        // Original XY coordinate of AWB light source
        {
           // Strobe
            {
                -5,    // i4X
                -364    // i4Y
            },
            // Horizon
            {
                -385,    // i4X
                -329    // i4Y
            },
            // A
            {
                -265,    // i4X
                -354    // i4Y
            },
            // TL84
            {
                -176,    // i4X
                -282    // i4Y
            },
            // CWF
            {
                -110,    // i4X
                -367    // i4Y
            },
            // DNP
            {
                -47,    // i4X
                -344    // i4Y
            },
            // D65
            {
                132,    // i4X
                -309    // i4Y
            },
            // DF
            {
                43,    // i4X
                -375    // i4Y
            }
        },
        // Rotated XY coordinate of AWB light source
        {
            // Strobe
            {
                -18,    // i4X
                -364    // i4Y
            },
            // Horizon
            {
                -397,    // i4X
                -315    // i4Y
            },
            // A
            {
                -277,    // i4X
                -345    // i4Y
            },
            // TL84
            {
                -186,    // i4X
                -276    // i4Y
            },
            // CWF
            {
                -123,    // i4X
                -363    // i4Y
            },
            // DNP
            {
                -59,    // i4X
                -342    // i4Y
            },
            // D65
            {
                121,    // i4X
                -314    // i4Y
            },
            // DF
            {
                30,    // i4X
                -377    // i4Y
            }
        },
        // AWB gain of AWB light source
        {
            // Strobe 
            {
                833,    // i4R
                512,    // i4G
                844    // i4B
            },
            // Horizon 
            {
                512,    // i4R
                552,    // i4G
                1451    // i4B
            },
            // A 
            {
                577,    // i4R
                512,    // i4G
                1183    // i4B
            },
            // TL84 
            {
                591,    // i4R
                512,    // i4G
                951    // i4B
            },
            // CWF 
            {
                724,    // i4R
                512,    // i4G
                977    // i4B
            },
            // DNP 
            {
                765,    // i4R
                512,    // i4G
                868    // i4B
            },
            // D65 
            {
                931,    // i4R
                512,    // i4G
                651    // i4B
            },
            // DF 
            {
                901,    // i4R
                512,    // i4G
                803    // i4B
            }
        },
        // Rotation matrix parameter
        {
            2,    // i4RotationAngle
            256,    // i4Cos
            9    // i4Sin
        },
        // Daylight locus parameter
        {
            -136,    // i4SlopeNumerator
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
            -231,    // i4RightBound
            -881,    // i4LeftBound
            -220,    // i4UpperBound
            -380    // i4LowerBound
            },
            // Warm fluorescent
            {
            -231,    // i4RightBound
            -881,    // i4LeftBound
            -380,    // i4UpperBound
            -500    // i4LowerBound
            },
            // Fluorescent
            {
            -80,    // i4RightBound
            -231,    // i4LeftBound
            -211,    // i4UpperBound
            -319    // i4LowerBound
            },
            // CWF
            {
            -80,    // i4RightBound
            -231,    // i4LeftBound
            -319,    // i4UpperBound
            -453    // i4LowerBound
            },
            // Daylight
            {
            146,    // i4RightBound
            -80,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
            },
            // Shade
            {
            506,    // i4RightBound
            146,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
            },
            // Daylight Fluorescent
            {
            146,    // i4RightBound
            -80,    // i4LeftBound
            -394,    // i4UpperBound
            -470    // i4LowerBound
            }
        },
        // PWB light area
        {
            // Reference area
            {
            506,    // i4RightBound
            -881,    // i4LeftBound
            0,    // i4UpperBound
            -500    // i4LowerBound
            },
            // Daylight
            {
            171,    // i4RightBound
            -80,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
            },
            // Cloudy daylight
            {
            271,    // i4RightBound
            96,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
            },
            // Shade
            {
            371,    // i4RightBound
            96,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
            },
            // Twilight
            {
            -80,    // i4RightBound
            -240,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
            },
            // Fluorescent
            {
            171,    // i4RightBound
            -286,    // i4LeftBound
            -226,    // i4UpperBound
            -413    // i4LowerBound
            },
            // Warm fluorescent
            {
            -177,    // i4RightBound
            -377,    // i4LeftBound
            -226,    // i4UpperBound
            -413    // i4LowerBound
            },
            // Incandescent
            {
            -177,    // i4RightBound
            -377,    // i4LeftBound
            -234,    // i4UpperBound
            -394    // i4LowerBound
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
            843,    // i4R
            512,    // i4G
            724    // i4B
            },
            // Cloudy daylight
            {
            1010,    // i4R
            512,    // i4G
            597    // i4B
            },
            // Shade
            {
            1078,    // i4R
            512,    // i4G
            556    // i4B
            },
            // Twilight
            {
            645,    // i4R
            512,    // i4G
            965    // i4B
            },
            // Fluorescent
            {
            743,    // i4R
            512,    // i4G
            842    // i4B
            },
            // Warm fluorescent
            {
            558,    // i4R
            512,    // i4G
            1145    // i4B
            },
            // Incandescent
            {
            554,    // i4R
            512,    // i4G
            1136    // i4B
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
            5844    // i4OffsetThr
            },
            // Warm fluorescent	
            {
            0,    // i4SliderValue
            5799    // i4OffsetThr
            },
            // Shade
            {
            0,    // i4SliderValue
            1341    // i4OffsetThr
            },
            // Daylight WB gain
            {
            736,    // i4R
            512,    // i4G
            838    // i4B
            },
            // Preference gain: strobe
            {
            512,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: tungsten
            {
            503,    // i4R
            512,    // i4G
            513    // i4B
            },
            // Preference gain: warm fluorescent
            {
            503,    // i4R
            512,    // i4G
            513    // i4B
            },
            // Preference gain: fluorescent
            {
            504,    // i4R
            512,    // i4G
            512    // i4B
            },
            // Preference gain: CWF
            {
            503,    // i4R
            512,    // i4G
            518    // i4B
            },
            // Preference gain: daylight
            {
            512,    // i4R
            512,    // i4G
            510    // i4B
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
                -518,    // i4RotatedXCoordinate[0]
                -398,    // i4RotatedXCoordinate[1]
                -307,    // i4RotatedXCoordinate[2]
                -180,    // i4RotatedXCoordinate[3]
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
    #include "camera_tsf_para_ov13850mipiraw.h"
    #include "camera_tsf_data_ov13850mipiraw.h"
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


