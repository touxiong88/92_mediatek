#include <platform/mt_typedefs.h>
#include <platform/mt_reg_base.h>
#include <platform/mt_i2c.h>
#include <printf.h>

/**********************************************************
  *
  *   [I2C Function For Read/Write isl98607]
  *
  *********************************************************/
static int isl98607_set_cmd_write(kal_uint8 cmd, int writeData)
{
	char cmd_buf[2]={0x00, 0x00};
	U32 ret_code = I2C_OK;

	struct mt_i2c_t i2c;
	cmd_buf[0] = cmd;
	cmd_buf[1] = writeData;

	i2c.id = I2C1;
	i2c.addr = 0x29;
	i2c.mode = ST_MODE;
	i2c.speed = 200;
	i2c.st_rs = I2C_TRANS_REPEATED_START;
	i2c.is_push_pull_enable = 0;
	i2c.is_clk_ext_disable = 0;
	i2c.delay_len = 0;
	i2c.is_dma_enabled = 0;

	ret_code = i2c_write(&i2c, &cmd_buf[0], 2);	 // set register command

	if (ret_code != I2C_OK)
	{
		printf("isl98607_set_cmd_write error!!!\n");
		return ret_code;
	}
	return ret_code;
}

int isl98607_set_cmd_read(kal_uint8 cmd, int *returnData)
{
	char cmd_buf[2]={0x00, 0x00};
	U32 ret_code = I2C_OK;
	int readData = 0;

	struct mt_i2c_t i2c;
	cmd_buf[0] = cmd;

	i2c.id = I2C1;
	i2c.addr = 0x29;
	i2c.mode = ST_MODE;
	i2c.speed = 200;
	i2c.st_rs = I2C_TRANS_REPEATED_START;
	i2c.is_push_pull_enable = 0;
	i2c.is_clk_ext_disable = 0;
	i2c.delay_len = 0;
	i2c.is_dma_enabled = 0;

	ret_code = i2c_write_read(&i2c, &cmd_buf[0], 1, 1);  // set register command

	if (ret_code != I2C_OK)
		return 0;

	readData = cmd_buf[0];
	*returnData = readData;
		printf("buffer read = [%X]\n", readData);
	return 1;
}

int isl98607_set_rg_output_vol(char vbst, char vn, char vp)
{
	isl98607_set_cmd_write(0x06,vbst);
	isl98607_set_cmd_write(0x08,vn);
	isl98607_set_cmd_write(0x09,vp);
	return 1;
}
