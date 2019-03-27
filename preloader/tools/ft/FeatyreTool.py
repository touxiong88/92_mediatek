#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import sys
import os
import struct
import re

keys_v1 = ([ "CFG_BOOT_DEV","CFG_FPGA_PLATFORM", "CFG_EVB_PLATFORM", "CFG_APWDT_DISABLE", "CFG_MDWDT_DISABLE",
"CFG_BATTERY_DETECT","CFG_UART_TOOL_HANDSHAKE","CFG_USB_TOOL_HANDSHAKE","CFG_USB_DOWNLOAD","CFG_PMT_SUPPORT","CFG_LOG_BAUDRATE",
"CFG_META_BAUDRATE","CFG_UART_LOG","CFG_UART_META","CFG_EMERGENCY_DL_SUPPORT","CFG_EMERGENCY_DL_TIMEOUT_MS","CFG_MMC_ADDR_TRANS",
"CFG_SWITCH_USB_UART_SUPPORT" ,"CFG_SWITCH_FORCE_USB" ,"CFG_SWITCH_FORCE_UART" ])
pattern = "a6b3c487"
versionDict = {"v1":keys_v1}

def encode(hexfile):
	print "\n[Feature Tool]"
	print "==========================="
	valueList = []
	keys = []
	i = 0
	#remove hexfile first
	if os.path.exists(hexfile):
		#print "DEBUG: remove hexfile"
		os.remove(hexfile)
	#check version
	env = os.getenv("CFG_FEATURE_ENCODE")
	if(env):
		#print env
		if(versionDict.has_key(env)):
			keys = versionDict[env]
			version = env
		else:
			print "no support version, please check default.mak ->CFG_FEATURE_ENCODE"
			sys.exit(0)
	else:
		print "No support feature cat tool"
		sys.exit(0)

	for item in keys:
		env = os.getenv(item)
		if(env):
			if( env.isdigit() ) :
				valueList.append(int(env))
			else :
				valueList.append(env.encode("hex")) 
			i=i+1
			print item + "="+ str (valueList[len(valueList)-1])
		else:
			print item +" is not define"	
	#Encode into file
	f= open(hexfile, 'w+')
	f.write(pattern+"_"+version+"@")
	for item in valueList:	
		if( type(item) is int):
			bin_value= "!"+struct.pack('I', item )
		else:
			bin_value =  item
		f.write(bin_value + "@")
	f.write(pattern+"@")
	f.close()
	#padding hex to 4 byte alignment
	filesize= os.stat(hexfile).st_size
	#print filesize
	f = open(hexfile, 'a')
	padding = filesize % 4
	if(padding !=0):
		padding = 4-padding
		for x in range(padding):
			f.write("\x00")
	f.close()
	print "tools/ft/FeatureTool pass!!!\n"
	
	
	
	
def decode(binfile):
	#parse bin
	f = open(binfile)
	fin = f.read()
	found = re.search("a6b3c487.*a6b3c487",fin)
	if(found):
		#print found.group(0)
		fin = found.group(0).split("@")	
		i=0
		keys=[]
		#get version
		version = fin[0].split("_")[1]
		if(versionDict.has_key(version)):
			keys = versionDict[version]
		else:
			print "not support this version"
			sys.exit(0)
		
		#print feature
		for item in fin:
			if(item != "" and item.find(pattern) == -1):
				if( item[0] == "!" ):					
					ans= struct.unpack('I',item[1:])[0]
				else:
					ans=item.decode("hex")
				print keys[i] +" ="+str(ans)
				i=i+1
	else:
		print "pattern not found"

def main():
	if(len(sys.argv)!=3):
		print "usage: python FeatureTool.py [encode/decode] [preloader_xxx.bin]"
	elif cmp(sys.argv[1],"decode") == 0:
		if(sys.argv[2]):
			decode(sys.argv[2])
	elif cmp(sys.argv[1],"encode") == 0:
		if(sys.argv[2]):
			encode(sys.argv[2])
	else:
		print "usage: python FeatureTool.py [encode/decode] [preloader_xxx.bin]"

if __name__ == '__main__':
	main()

# vim:set nu et ts=4 sw=4 cino=>4: