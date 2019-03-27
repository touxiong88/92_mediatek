#define PWON_CHECK_INC       	1// from Jan, Check FW VER when PW ON
#define TOUCH_NUM				10

#define APKDOSOMETHING 			0x1FFF// 0001 xxxx xxxx xxxx
#define SELFRAWDATA        		0x2101  //8449, 
#define SELFBASELINE	  		0x2102
#define SELFDELTA           	0x2103
#define SELFNONE            	0x2100
#define MUTUALRAWDATA       	0x2201
#define MUTUALBASELINE			0x2202
#define MUTUALDELTA				0x2203
#define MUTUALNONE		    	0x2200
#define SETTOXDATA		    	0x2305
#define SETCCVALUE				0x2304	
#define SETTONOISE				0x2306	
#define SETTOMSG				0x2307	
#define DISABLE_DEBUG_MODE		0x230C	
#define CLEAR_I2C				0x230D	
#define IC_RESET				0x230E	
#define READ_REGISTER			0x9999   // ?		
#define READ_NOISE_DATA			0x2706				
#define SET_ISREPORTTOUCHTOAPK	0x270F
#define SET_ISREPORTKEYTOAPK	0x2710
#define APKGETHEADER			0x330A
#define APK_LOGDATABYDRIVER		0x3311     //13073
#define	APK_FWUPDATE			0x3508		//13576

#define IOC_MAGIC 					'\x66' 
#define IOCTL_FILE_NAME_CHAR 		_IOR(0xDC, 0x80, int)
#define IOCTL_BIN_LENGTH     		_IOR(0xDC, 0x79, int)
#define IOCTL_GET_FW_UPDATE_STATUE  _IOR(0xDC, 0xA2, int) //struct wp1004_ioctl_fw_update_status)

#define IOCTL_REGREADDATA   		_IOR(0xDC, 0x81, int)
#define IOCTL_CCVALUEBUF    		_IOR(0xDC, 0x82, int)
#define IOCTL_CHECKSTATE    		_IOR(0xDC, 0x83, int)
#define IOCTL_NOISEBUF      		_IOR(0xDC, 0x84, int)
#define IOCTL_VALSET_NUM    		_IOR(0xDC, 0x85, int)
//#define IOCTL_DONOTUPDATE   		_IOR(0xDC, 0x86, int)
#define IOCTL_CHECKBINFILE  		_IOR(0xDC, 0x87, int)
#define IOCTL_SELFLDATALEN  		_IOR(0xDC, 0x88, int)
#define IOCTL_MUTUALDATALEN 		_IOR(0xDC, 0x89, int)
#define IOCTL_I2CFLAG       		_IOR(0xDC, 0x90, struct ioctl_arg)
//#define IOCTL_VALSET        		_IOW(0xDC, 0x91, struct ioctl_arg)//int)
#define IOCTL_VALGET        		_IOR(0xDC, 0x92, struct ioctl_arg)
#define IOCTL_BUFGET        		_IOR(0xDC, 0x93, int)
#define IOCTL_RAWDATA       		_IOR(0xDC, 0x94, int)
//#define IOCTL_I2CBUFGET     		_IOR(0xDC, 0x95, int)
//#define IOCTL_USERDEF1      		_IOR(0xDC, 0x96, int)
//#define IOCTL_USERDEF2      		_IOR(0xDC, 0x98, int)
#define IOCTL_UPDATEFWBIN   		_IOR(0xDC, 0x99, int)
#define IOCTL_GET_TOUCH_DATA 		_IOR(0xDC, 0xA0, int) //struct wp1005_ioctl_finger_data)
#define IOCTL_GET_KEY_DATA   		_IOR(0xDC, 0xA1, int) //struct wp1005_ioctl_key_data)

#define I2C_END_LEN          		2
#define MSG_GET_FNSL         		0x0A
#define SELF_MODE            		0x01
#define I2C_READ_RAW_SELF    		0x10
#define I2C_READ_RAW_MUTUAL  		0x11
#define MUTUAL_MODE          		0x10
#define SELF_TEST_MODE       		0x11
//Data Type
#define DATA_ALL_NONE        		0x09
#define SELF_DATA_NONE       		0x39
#define SELF_DATA_RAWDATA    		0x49
#define SELF_DATA_BASELINE   		0x89
#define SELF_DATA_DELTA      		0xC9
#define MUTUAL_DATA_NONE     		0xC9
#define MUTUAL_DATA_RAWDATA  		0x19
#define MUTUAL_DATA_BASELINE 		0x29
#define MUTUAL_DATA_DELTA    		0x39
#define NOISE_LEVEL_DATA_LEN 		128
#define MSG_GET_STATUS            	0x2
#define MSG_GET_STATUS_LEN        	3
#define MAX_SUPPORT_FINGER_NO      	16 //Jack 20131214 add
#define MAX_SUPPORT_KEY_NO         	8 //Jack 20131214 add
#define MAX_REG_DATA_LEN           	64 //Jack 20131220 add to set maximum register read data len
#define LOG_DATA_FILE_PATH       	"/storage/sdcard0" //Jack 20131219 add for assign log data file path

#define UPDATEFW

//#ifdef UPDATEFW
//#define DP_MINOR MINOR(dev_id) 
//#define DP_MAJOR MAJOR(dev_id)
//#define FLASHREADLEN              	32
#define ROUNDOFFERROR             	31
#define WAIT_TIMEOUT              	80
#define CAP2                      	0xc4
#define HWVER_HIGH_BIT            	0x80
#define HID_MAX_PACKET_SIZE_EP2   	64
#define MAX_TOTAL_CHANNELS        	48
#define MAX_MUTUAL_NODES          	512
#define MSG_GET_HEADER            	0x1
#define MSG_GET_REPORTS           	0x3
#define HW_ADDRESS                	32029  //0x7d1d 
#define FW_ADDRESS                	32030  //0x7d1e
#define PID_ADDRESS_H             	32039  //0x7d27
#define PID_ADDRESS_L             	32040  //0x7d28 , for D2 FWBIN
#define FWPATH                    	"/sdcard/WP1004_51.bin"
#define WP1005_PRINT(level, x...) 		do { if(Wp1004PrintLevel == (level)) printk(KERN_INFO "[wp1] " x); } while(0)
#define WP1005_KEY_NUMBER 				3

//#define TPD_CLOSE_POWER_IN_SLEEP

#define TPD_OK 							0
//#define IsFingerReport(xx)        	(xx & 0x80)
//#define TPD_RESET_ISSUE_WORKAROUND
//#define TPD_MAX_RESET_COUNT 			3

//#define MAX_TRANSACTION_LENGTH  		8
//#define GTP_ADDR_LENGTH             	2
//#define I2C_MASTER_CLOCK       		150

#define TOUCH_IOC_MAGIC 				'A'
//#define DRIVER_VER  					"M5"

#define TPD_GET_VELOCITY_CUSTOM_X 		_IO(TOUCH_IOC_MAGIC,0)
#define TPD_GET_VELOCITY_CUSTOM_Y 		_IO(TOUCH_IOC_MAGIC,1)

struct wp1005_ioctl_data {
        unsigned char val;
        uint32_t x,
                 y,
                 z,
                 delta, 
                 baseline,
                 rawdata;               
        struct  i2c_client *client;
        struct  input_dev *input;
        rwlock_t lock;
};

/*
typedef struct
{
    uint16_t x_pos;
    uint16_t y_pos;
    uint16_t z_force;
    uint8_t  id;
    uint8_t  is_down;
}tFingerInfo;

struct wp1005_ioctl_finger_data {
    tFingerInfo TouchData[MAX_SUPPORT_FINGER_NO];
    uint8_t CurTouchFingerNo;
};

struct wp1005_ioctl_key_data {
    unsigned char KeyData[MAX_SUPPORT_KEY_NO];    
};

struct wp1005_ioctl_fw_update_status {
    uint16_t    CheckSum;
    uint16_t    CurProgress;
};*/

struct ioctl_arg {
        unsigned int reg;
        unsigned int val;
        unsigned char i2c_buf[258];
        uint64_t *pointer;
        uint16_t  x,
                  y,
                  z,
                  wp_i2c_flag;
};

typedef struct
{
    uint8_t PROT_VER;
	uint8_t ID;
	uint8_t HW_VER;
	uint8_t FW_VER;
	uint32_t SERIAL_NO;
	uint16_t V_ID;
	uint16_t P_ID;
	uint16_t RES_X;
	uint16_t RES_Y;
	uint8_t XL_SIZE;
	uint8_t YR_SIZE;
	uint8_t SUPPORT_FINGERS;
	uint8_t KEYS_NUM;
	uint8_t MAX_RPT_LEN;
	uint8_t CAP_1;
	uint8_t CAP_2;
}tProtocolHeader;

/*
typedef struct
{
    uint8_t  tp_reports[HID_MAX_PACKET_SIZE_EP2-3];
    uint16_t tp_mutual_cyc_id;
    uint16_t tp_self_raw[MAX_TOTAL_CHANNELS];
    uint16_t tp_mutual_raw[MAX_MUTUAL_NODES];
    uint8_t  tp_status[5];
    uint8_t  datamod[5];
} tParameters;
*/

struct winic_i2c_data {
        int           irq;
        char          phys[32];
        unsigned char skip_packet;
        struct i2c_client *client;
	struct work_struct get_work;
        struct input_dev *input_dev;
        struct input_dev *dev;
        struct hrtimer timer;
        struct early_suspend early_suspend; 
};
