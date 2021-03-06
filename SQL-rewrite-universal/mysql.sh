#!/bin/sh  
  
  
# Copyright (c) 2010 codingstandards. All rights reserved.  
# file: mysql.sh  
# description: Bash中操作MySQL数据库  
# license: LGPL  
# author: codingstandards  
# email: codingstandards@gmail.com  
# version: 1.0  
# date: 2010.02.28  
  
  
# MySQL中导入导出数据时，使用CSV格式时的命令行参数  
# 在导出数据时使用：select ... from ... [where ...] into outfile '/tmp/data.csv' $MYSQL_CSV_FORMAT;  
# 在导入数据时使用：load data infile '/tmp/data.csv' into table ... $MYSQL_CSV_FORMAT;  
# CSV标准文档：RFC 4180  
MYSQL_CSV_FORMAT="fields terminated by ',' optionally enclosed by '\"' escaped by '\"' lines terminated by '\r\n'"  