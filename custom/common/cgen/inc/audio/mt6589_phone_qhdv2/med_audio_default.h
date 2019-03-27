#ifndef MED_AUDIO_CUSTOM_H
#define MED_AUDIO_CUSTOM_H
// normal mode parameters ------------------------
#define NORMAL_SPEECH_OUTPUT_FIR_COEFF \
    -432,   384,  -431,   303,  -565,\
    9,  -438,   201,  -382,   336,\
    -640,   382,  -780,  1304,  -420,\
    1663,   998,  1746, -1657,  4298,\
    -1038, 23197, 23197, -1038,  4298,\
    -1657,  1746,   998,  1663,  -420,\
    1304,  -780,   382,  -640,   336,\
    -382,   201,  -438,     9,  -565,\
    303,  -431,   384,  -432,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// headset mode parameters ------------------------
#define HEADSET_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// handfree mode parameters ------------------------
#define HANDFREE_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// VoIP_BT mode parameters ------------------------
#define VOIPBT_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// VoIP_NORMAL mode parameters ------------------------
#define VOIPNORMAL_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// VoIP_Handfree mode parameters ------------------------
#define VOIPHANDFREE_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// AUX1 mode parameters ------------------------
#define AUX1_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
// AUX2 mode parameters ------------------------
#define AUX2_SPEECH_OUTPUT_FIR_COEFF \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
#define SPEECH_OUTPUT_MED_FIR_COEFF \
    NORMAL_SPEECH_OUTPUT_FIR_COEFF,\
    HEADSET_SPEECH_OUTPUT_FIR_COEFF ,\
    HANDFREE_SPEECH_OUTPUT_FIR_COEFF ,\
    VOIPBT_SPEECH_OUTPUT_FIR_COEFF,\
    VOIPNORMAL_SPEECH_OUTPUT_FIR_COEFF,\
    VOIPHANDFREE_SPEECH_OUTPUT_FIR_COEFF,\
    AUX1_SPEECH_OUTPUT_FIR_COEFF,\
    AUX2_SPEECH_OUTPUT_FIR_COEFF
#define SPEECH_INPUT_MED_FIR_COEFF\
    -919,   967,  -897,   895,  -717,\
    586,  -730,   923,  -902,   982,\
    -1155,  1300, -1387,  1549, -1681,\
    1978, -2754,  3878, -4616,  4995,\
    -7156, 23197, 23197, -7156,  4995,\
    -4616,  3878, -2754,  1978, -1681,\
    1549, -1387,  1300, -1155,   982,\
    -902,   923,  -730,   586,  -717,\
    895,  -897,   967,  -919,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    -306,   313,  -352,   349,  -207,\
    241,  -314,   207,  -324,   499,\
    -202,   241,  -727,   656,  -670,\
    1154,  -685,   544, -1866,   975,\
    -290, 20675, 20675,  -290,   975,\
    -1866,   544,  -685,  1154,  -670,\
    656,  -727,   241,  -202,   499,\
    -324,   207,  -314,   241,  -207,\
    349,  -352,   313,  -306,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    \
    32767,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0
#define FIR_output_index\
    0,     0,     0,     0,     0,     0,     0,     0
#define FIR_input_index\
    0,     0,     0,     0,     0,     0,     0,     0
#define MED_SPEECH_NORMAL_MODE_PARA \
    96,   253, 16388,    31, 57351,    31,   400,    64,\
    80,   229,   611,     0, 20488,     0,     0,  8192
#define MED_SPEECH_EARPHONE_MODE_PARA \
    0,   189, 10756,    31, 57351,    31,   400,    64,\
    80,   229,   611,     0, 20488,     0,     0,     0
#define MED_SPEECH_BT_EARPHONE_MODE_PARA \
    0,   253, 10756,    31, 53255,    31,   400,     0,\
    80,   229,   611,     0, 53256,     0,     0,    86
#define MED_SPEECH_LOUDSPK_MODE_PARA \
    96,   224,  5256,    31, 57351, 24607,   400,   132,\
    84,   229,   611,     0, 20488,     0,     0,     0
#define MED_SPEECH_CARKIT_MODE_PARA \
    96,   224,  5256,    31, 57351, 24607,   400,   132,\
    84,  4325,   611,     0, 20488,     0,     0,     0
#define MED_SPEECH_BT_CORDLESS_MODE_PARA \
    0,     0,     0,     0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,     0,     0,     0
#define MED_SPEECH_AUX1_MODE_PARA \
    0,     0,     0,     0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,     0,     0,     0
#define MED_SPEECH_AUX2_MODE_PARA \
    0,     0,     0,     0,     0,     0,     0,     0,\
    0,     0,     0,     0,     0,     0,     0,     0
#endif