#ifndef __FLASH_INFO__
#define __FLASH_INFO__

//Structure declaration for flash info
struct flash_info_header {
	unsigned long long magic; //Flash Info Magic Number : flash_in ASIC
	unsigned long version;
	unsigned long header_size; //Flash Info header Size : 512byte
	unsigned long data_size; //All Flash Info Item Size : 2M - 512byte
	unsigned long item_size; // size of flash info item size : 128byte
	unsigned long index; //max index in flash info region
	unsigned long crc; //crc which protect header
};

struct flash_info {
	unsigned long index; //Sequence number for this flash info item, start from 0;
	unsigned long version;
	unsigned long long op_time; //Time when the operation occur;
	unsigned long long op_pc_mac; //The PC related to operation;
	unsigned long op_source; //The operation from : Flashtool, Recovery, Fastboot
	unsigned long op_type; //The type of opertion
	unsigned char op_object[16];
	unsigned long long op_start_address;
	unsigned long long op_size;
	unsigned long op_region;
	unsigned long flag;
};

struct flash_info_header_cache {
    struct flash_info_header *header;
    int init;
};

//Enum declaration for flash info
enum flash_info_op_source {
	FLASH_TOOL = 0,
	RECOVERY,
	FAST_BOOT,
};

enum flash_info_op_type {
	FLASH_TOOL_ERASE = 0,
	FLASH_TOOL_DOWNLOAD,
	FLASH_TOOL_WRITE_MEMORY,

	FAST_BOOT_ERASE = 10,
	FAST_BOOT_FLASH,
	FAST_BOOT_FORMAT,

	RECOVERY_MOTA_FORMAT_FULL = 20,
	RECOVERY_MOTA_ERASE_FULL,
	RECOVERY_MOTA_UPDATE_PMT_FULL,
	RECOVERY_MOTA_UPDATE_PARTITION_FULL,
	RECOVERY_MOTA_FORMAT_DIFF,
	RECOVERY_MOTA_ERASE_DIFF,
	RECOVERY_MOTA_UPDATE_PMT_DIFF,
	RECOVERY_MOTA_UPDATE_PARTITION_DIFF,
	RECOVERY_FOTA_FORMAT,
	RECOVERY_FOTA_ERASE,
	RECOVERY_FOTA_UPDATE_PMT,
	RECOVERY_FOTA_UPDATE_PARTITION,
};

//IOCTL commands of flash info 
#define TYPE_UPDATE		1

/* Use 'f' as magic number */
#define FLASH_INFO_MAGIC  'm'
#define FLASH_INFO_UPDATE   _IOW(FLASH_INFO_MAGIC, TYPE_UPDATE, struct flash_info)

//include file
#define OP_ON_GOING    0x0
#define OP_DONE 0x1

#endif //FLASH_INFO
