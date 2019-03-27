#! /usr/bin/env python

# generate the animation hash name table xml file for cache files.

import sys
import os
import hashlib
import string

#return the md5sum string for a file
def md5sum(filename):
    m = hashlib.md5()
    m.update(open(filename, 'r').read())
    return m.hexdigest()


#return the main filename
def removeExt(filename):
    return string.split(filename, ".")[0]

#constant strings
fileExt = ".json"
prompt = "Generating xml file from *{0} files..."
fileBegin ='<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
fileEnd ='</resources>'
appPath = os.path.dirname(sys.argv[0])
jsonPath = appPath + "/res/raw"


print prompt.format(fileExt)
#if len(sys.argv) != 2:
#    exit()

xmlFileName = sys.argv[1]
#open file
try:
   xmlFile = open(xmlFileName, "w");
except:
   print "failed to open " + xmlFileName
   exit()
#write header
xmlFile.write(fileBegin)

#write body
files = os.listdir(jsonPath)
m = hashlib.md5()
lineTemplate = '    <string name="{0}" translatable="false">{1}</string>\n'
for f in files:
    if string.find(string.lower(f), fileExt) == -1: continue
    line =  lineTemplate.format(removeExt(f), md5sum(jsonPath + '/' + f))
    xmlFile.write(line)

xmlFile.write(fileEnd)
xmlFile.close()

