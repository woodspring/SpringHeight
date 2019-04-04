#!/bin/sh

. ~/setenv.sh

MYNAME=MarketSimulator
PID_FILE=$MYNAME.PID
LIB_FOLDER=../lib
CONFIG_FOLDER=../config

CLASSPATH="$LIB_FOLDER/*"
CLASSPATH="$CONFIG_FOLDER:$CLASSPATH"

MAIN_CLASS=com.tts.mlp.MarketLiquidityProviderSimMain


ADD_ARGUMENT="-DAPP_NAME=MDS"
ADD_ARGUMENT="$ADD_ARGUMENT -DSIMULATOR.NO_LOGGING=TRUE "
ADD_ARGUMENT="$ADD_ARGUMENT -Dspring.profiles.active=$TTS_ENV,$TTS_MESSAGING_PLATFORM"
ADD_ARGUMENT="$ADD_ARGUMENT -Dlogback.configurationFile=$CONFIG_FOLDER/log-resources/logback.xml"
#ADD_ARGUMENT="$ADD_ARGUMENT -DpathToForwardCurveData=<PATH TO FILE CONTAINING FORWARD POINT DATA>
VM_ARGUMENT="-Xmx2048m -XX:MaxDirectMemorySize=2048m "
PROGRAM_ARGUMENT=""

CURRENT_DATE=$(date +%Y%m%d);
CURRENT_SEQ=$(ls -ld logs.before$CURRENT_DATE* 2> /dev/null | wc -l)

#clean up
mv logs logs.before$CURRENT_DATE-$CURRENT_SEQ 2>  /dev/null
mv nohup.out logs.before$CURRENT_DATE-$CURRENT_SEQ/nohup.out 2> /dev/null

nohup $JAVA_HOME/bin/java -cp $CLASSPATH $VM_ARGUMENT $ADD_ARGUMENT $MAIN_CLASS $PROGRAM_ARGUMENT 2>&1 &
echo $! > $PID_FILE

echo Start $MYNAME
