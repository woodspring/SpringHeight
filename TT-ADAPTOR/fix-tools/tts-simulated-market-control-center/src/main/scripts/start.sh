#!/bin/sh

. ~/setenv.sh

BASEDIR="$(dirname "$(dirname "$(readlink -f "$0")")")"

MYNAME=MarketSimulatorControlCenter
PID_FILE=$BASEDIR/bin/$MYNAME.$(hostname).PID

echo "Starting up $MYNAME ..."

LIB_FOLDER=$BASEDIR/lib
CONFIG_FOLDER=$BASEDIR/config
LOGS_FOLDER=$BASEDIR/bin/logs

CLASSPATH="$LIB_FOLDER/*"
CLASSPATH="$CONFIG_FOLDER:$CLASSPATH"

MAIN_CLASS=com.tts.mlp.MarketControlCenterMain



ADD_ARGUMENT="-DAPP_NAME=MDS"
ADD_ARGUMENT="$ADD_ARGUMENT -DSIMULATOR.NO_LOGGING=TRUE"
ADD_ARGUMENT="$ADD_ARGUMENT -Dspring.profiles.active=$TTS_ENV,$TTS_MESSAGING_PLATFORM"
ADD_ARGUMENT="$ADD_ARGUMENT -Dlogback.configurationFile=$CONFIG_FOLDER/log-resources/logback.xml"
#ADD_ARGUMENT="$ADD_ARGUMENT -DpathToForwardCurveData=<PATH TO FILE CONTAINING FORWARD POINT DATA>
VM_ARGUMENT="-Xmx2048m -XX:MaxDirectMemorySize=2048m "
PROGRAM_ARGUMENT=""

CURRENT_DATE=$(date +%Y%m%d);
CURRENT_SEQ=$(ls -ld $LOGS_FOLDER/logs.before$CURRENT_DATE* 2> /dev/null | wc -l)

#clean up
mv $LOGS_FOLDER/current $LOGS_FOLDER/logs.before$CURRENT_DATE-$CURRENT_SEQ 2>  /dev/null
mv nohup.out logs.before$CURRENT_DATE-$CURRENT_SEQ/nohup.out 2> /dev/null
mkdir $LOGS_FOLDER/current

nohup $JAVA_HOME/bin/java -cp $CLASSPATH $ADD_ARGUMENT $VM_ARGUMENT $MAIN_CLASS $PROGRAM_ARGUMENT > $LOGS_FOLDER/current/logs.nohup.out 2>&1 &
echo $! > $PID_FILE

echo "$MYNAME startup complete."
