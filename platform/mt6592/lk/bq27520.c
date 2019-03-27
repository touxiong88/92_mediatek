#include <platform/mt_typedefs.h>
#include <platform/mt_reg_base.h>
#include <platform/mt_i2c.h>
#include <platform/bq27520.h>
#include <printf.h>

#define  driver_debug	0
/**********************************************************
  *
  *   [I2C Slave Setting]
  *
  *********************************************************/
#define bq27520_SLAVE_ADDR_WRITE   0xAA
#define bq27520_SLAVE_ADDR_READ    0xAB

int bq27520_set_cmd_write_ROM(unsigned int addr,kal_uint8 cmd, int WriteData)
{
	char cmd_buf[2]={0x00,0x00};
    U32 ret_code = I2C_OK;
    int readData = 0;

    struct mt_i2c_t i2c;

    cmd_buf[0] = cmd;
	cmd_buf[1] = WriteData & 0xFF;

    i2c.id = I2C1;
	i2c.addr = addr>>1;
	i2c.mode = ST_MODE;
	i2c.speed = 200;
    i2c.st_rs = I2C_TRANS_REPEATED_START;
    i2c.is_push_pull_enable = 0;
	i2c.is_clk_ext_disable = 0;
	i2c.delay_len = 0;
	i2c.is_dma_enabled = 0;
	if(driver_debug)
	{
		printf("bq27520_set_cmd_write_ROM addr=0x%x,cmd=0x%x,data=0x%x\n",i2c.addr,cmd_buf[0],cmd_buf[1]);
	}
    ret_code = i2c_write(&i2c, &cmd_buf[0], 2);	 // set register command

	if (ret_code != I2C_OK)
	{
		printf("zlog bq27520_set_cmd_write_ROM error\n");
    	return 0;
	}
    return 1;

}
int bq27520_set_cmd_read_ROM(unsigned int addr,kal_uint8 cmd,int *returnData)
{
	char cmd_buf[2]={0x00, 0x00};
    U32 ret_code = I2C_OK;
    int readData = 0;

    struct mt_i2c_t i2c;

    cmd_buf[0] = cmd;

    i2c.id = I2C1;
	i2c.addr = addr>>1;
	i2c.mode = ST_MODE;
	i2c.speed = 100;
    i2c.st_rs = I2C_TRANS_REPEATED_START;
    i2c.is_push_pull_enable = 0;
	i2c.is_clk_ext_disable = 0;
	i2c.delay_len = 0;
	i2c.is_dma_enabled = 0;
    ret_code = i2c_write_read(&i2c, &cmd_buf[0], 1,	1);	 // set register command
	if(driver_debug)
	{
		printf("bq27520_set_cmd_read_ROM---readData=%d\n",cmd_buf[0]);
	}
	if (ret_code != I2C_OK)
   	{
		printf("zlog bq27520_set_cmd_read_ROM error\n");
    	return 0;
	}

    readData = cmd_buf[0];
    *returnData = readData;

    return 1;
}
int bq27520_set_cmd_read(kal_uint8 cmd, int *returnData)
{
	char cmd_buf[2]={0x00, 0x00};
    U32 ret_code = I2C_OK;
    int readData = 0;

    struct mt_i2c_t i2c;

    cmd_buf[0] = cmd;

    i2c.id = I2C1;
	i2c.addr = 0x55;
	i2c.mode = ST_MODE;
	i2c.speed = 100;
    i2c.st_rs = I2C_TRANS_REPEATED_START;
    i2c.is_push_pull_enable = 0;
	i2c.is_clk_ext_disable = 0;
	i2c.delay_len = 0;
	i2c.is_dma_enabled = 0;

    ret_code = i2c_write_read(&i2c, &cmd_buf[0], 1,	2);	 // set register command

	if (ret_code != I2C_OK)
    return 0;

    readData = (cmd_buf[1] << 8) | cmd_buf[0];
    *returnData = readData;
	if(driver_debug)
	{
    	printf("buffer read = [%X]\n", readData);
	}

    return 1;
}

int bq27520_set_cmd_write(kal_uint8 cmd, int WriteData)
{
	char cmd_buf[3]={0x00,0x00,0x00};
    U32 ret_code = I2C_OK;
    int readData = 0;

    struct mt_i2c_t i2c;

    cmd_buf[0] = cmd;
	cmd_buf[1] = WriteData & 0xFF;
	cmd_buf[2] = (WriteData >> 8) & 0xFF;

    i2c.id = I2C1;
	i2c.addr = 0x55;
	i2c.mode = ST_MODE;
	i2c.speed = 100;
    i2c.st_rs = I2C_TRANS_REPEATED_START;
    i2c.is_push_pull_enable = 0;
	i2c.is_clk_ext_disable = 0;
	i2c.delay_len = 0;
	i2c.is_dma_enabled = 0;

    ret_code = i2c_write(&i2c, &cmd_buf[0], 3);	 // set register command

	if (ret_code != I2C_OK)
    return 0;

    return 1;

}
int bq27520_FuelGauge_init(void)
{
	int size=0;
	int returndata=0;
	int i = 0;
	for(i=0;i<5;i++)
	{
		bq27520_set_cmd_write_ROM(0xaa,0x00,0x00);
		bq27520_set_cmd_write_ROM(0xaa,0x01,0x00);
		bq27520_set_cmd_read(0x00, &returndata);
		if(returndata&0x04)
		{
			printf("zlog RUP_DIS is high\n");
			bq27520_set_cmd_write_ROM(0xaa,0x00,0x21);
			bq27520_set_cmd_write_ROM(0xaa,0x01,0x00);
		}
		else
		{
			break;
		}
	}
    return 0;
}
