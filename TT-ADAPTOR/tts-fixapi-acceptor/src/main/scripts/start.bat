@echo off

set LIB_FOLDER=..\lib
set CONFIG_FOLDER=..\config
set TIBRV_LIB_FOLDER=%TIBRV_HOME%\lib
set EMS_LIB_FOLDER=%EMS_HOME%\lib


set CLASSPATH=%LIB_FOLDER%/*;%TIBRV_LIB_FOLDER%/*;%EMS_LIB_FOLDER%/*
set CLASSPATH=%CONFIG_FOLDER%;%CLASSPATH%

set MAIN_CLASS=com.tts.fixapi.FIXAcceptorMain

set ADD_ARGUMENT="-Dlog4j.configurationFile=../config/log-resources/log4j2.xml -DAPP_NAME=FIXACCEPTOR "
set VM_ARGUMENT=""

set JAVA=java

:run
%JAVA% -cp %CLASSPATH% %ADD_ARGUMENT% %MAIN_CLASS% %VM_ARGUMENT% 

:end