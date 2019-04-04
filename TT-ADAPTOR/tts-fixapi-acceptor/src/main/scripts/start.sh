#!/bin/sh

. ~/setenv.sh

MYNAME=FIXAcceptorMain
PID_FILE=$MYNAME.${hostname}.PID
LIB_FOLDER=../lib
CONFIG_FOLDER=../config
TIBRV_LIB_FOLDER=$TIBRV_HOME/lib
EMS_LIB_FOLDER=$EMS_HOME/lib
SOLACE_LIB_FOLDER=$SOLACE_HOME/lib

TIBRV_JAR=$TIBRV_LIB_FOLDER/tibrvnative.jar
EMS_JAR=$(echo $EMS_LIB_FOLDER/t*.jar | tr ' ' ':')
SOLACE_JAR=$(echo $SOLACE_LIB_FOLDER/s*.jar | tr ' ' ':')

CLASSPATH="$CONFIG_FOLDER:$LIB_FOLDER/*"

MAIN_CLASS=com.tts.fixapi.FIXAcceptorMain

ADD_ARGUMENT="-DAPP_NAME=FIXACCEPTOR"
ADD_ARGUMENT="$ADD_ARGUMENT -Dspring.profiles.active=$TTS_ENV,$TTS_MESSAGING_PLATFORM"
ADD_ARGUMENT="$ADD_ARGUMENT -Dlogback.configurationFile=../config/log-resources/logback.xml"
VM_ARGUMENT=""

[ ! -d logs  ] && mkdir logs

CURRENT_DATE=$(date +%Y%m%d);
CURRENT_SEQ=$(ls -ld logs/logs.before$CURRENT_DATE* 2> /dev/null | wc -l)

#clean up
mv logs/current logs/logs.before$CURRENT_DATE-$CURRENT_SEQ 2>  /dev/null
mkdir logs/current

nohup $JAVA_HOME/bin/java -cp $CLASSPATH $ADD_ARGUMENT $MAIN_CLASS $VM_ARGUMENT >logs/current/logs.nohup.out &
echo $! > $PID_FILE

echo Start $MYNAME



