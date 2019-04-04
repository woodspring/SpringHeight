#!/bin/sh

. ~/setenv.sh

BASEDIR="$(dirname "$(dirname "$(readlink -f "$0")")")"

MYNAME=FSSMarketAdapter
PID_FILE=$BASEDIR/bin/$MYNAME.$(hostname).PID

echo "Starting up $MYNAME ..."

LIB_FOLDER=$BASEDIR/lib
CONFIG_FOLDER=$BASEDIR/config
LOGS_FOLDER=$BASEDIR/bin/logs

TIBRV_LIB_FOLDER=$TIBRV_HOME/lib
EMS_LIB_FOLDER=$EMS_HOME/lib
SOLACE_LIB_FOLDER=$SOLACE_HOME/lib

TIBRV_JAR=$TIBRV_LIB_FOLDER/tibrvnative.jar
EMS_JAR=$(echo $EMS_LIB_FOLDER/t*.jar | tr ' ' ':')
SOLACE_JAR=$(echo $SOLACE_LIB_FOLDER/s*.jar | tr ' ' ':')

CLASSPATH="$CONFIG_FOLDER:$LIB_FOLDER/*"

MAIN_CLASS=com.tts.fa.TtsFssAdapterMain

#customize Reuters args
USER_HOME_DIR=/home/ticktrade
mkdir -p $USER_HOME_DIR/.java/.systemPrefs
mkdir -p $USER_HOME_DIR/.java/.userPrefs
chmod -R 755 $USER_HOME_DIR/.java
ADDITIONAL_JAVA_OPTS="-Djava.util.prefs.systemRoot=$USER_HOME_DIR/.java -Djava.util.prefs.userRoot=$USER_HOME_DIR/.java/.userPrefs "

#RESTART COUNTER LOGIC 
RESTART_COUNT=-1
RESTART_COUNTER_FILE=$BASEDIR/bin/_DONT_DELETE_RESTART_COUNT.dat
CURRENT_YEAR_WEEK=`date +%Y%W`
ls $RESTART_COUNTER_FILE 2>&1 > /dev/null
if [ "$?" -ne 0 ]; then
	RESTART_COUNT=0
else
	RESTART_FILE_CONTENT=`cat $RESTART_COUNTER_FILE`
	RESTART_WEEK=`echo $RESTART_FILE_CONTENT | awk '{split($0, a, ":");print a[1]}'`
	if [ $RESTART_WEEK -eq $CURRENT_YEAR_WEEK ]; then
		RESTART_COUNT=`echo $RESTART_FILE_CONTENT | awk '{split($0, a, ":");print a[2]}'`
	else
		RESTART_COUNT=0
	fi
fi
NEW_COUNT=`expr $RESTART_COUNT + 1`
echo $CURRENT_YEAR_WEEK:$NEW_COUNT > $RESTART_COUNTER_FILE


#Use -DfileBasedForwardApp=true to revert to previous MAS method where it sourced the forward points directly from a file 
ADD_ARGUMENT="-DAPP_NAME=FA "
ADD_ARGUMENT="$ADD_ARGUMENT -DRESTART.COUNT=$RESTART_COUNT"
ADD_ARGUMENT="$ADD_ARGUMENT -Dspring.profiles.active=$TTS_ENV,$TTS_MESSAGING_PLATFORM"
ADD_ARGUMENT="$ADD_ARGUMENT -Dlogback.configurationFile=$CONFIG_FOLDER/log-resources/logback.xml"
#ADD_ARGUMENT="$ADD_ARGUMENT -Dcom.sun.management.jmxremote.port=3335 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
#VM_ARGUMENT="-XX:+CMSClassUnloadingEnabled -Xms512M -Xmx512M -XX:+AggressiveHeap -XX:MaxPermSize=256M"
#VM_ARGUMENT="-XX:+CMSClassUnloadingEnabled -Xms512M -Xmx1024M -XX:+AggressiveHeap -XX:MaxMetaspaceSize=256M -XX:+UseParallelOldGC"
VM_ARGUMENT="-XX:+CMSClassUnloadingEnabled -Xms512M -Xmx1024M -XX:MaxMetaspaceSize=512M  -XX:+UseParNewGC -XX:+UseConcMarkSweepGC "
PROGRAM_ARGUMENT=""


CURRENT_DATE=$(date +%Y%m%d);
CURRENT_SEQ=$(ls -ld $LOGS_FOLDER/logs.before$CURRENT_DATE* 2> /dev/null | wc -l)
CURRENT_SESLOGSEQ=$(ls -ld $LOGS_FOLDER/sessionLogs.before$CURRENT_DATE* 2> /dev/null | wc -l)

#clean up
#mv sessionLogs $LOGS_FOLDER/sessionLogs.before$CURRENT_DATE-$CURRENT_SESLOGSEQ 2>  /dev/null
mv $LOGS_FOLDER/current $LOGS_FOLDER/logs.before$CURRENT_DATE-$CURRENT_SEQ 2>  /dev/null
mv nohup.out $LOGS_FOLDER/logs.before$CURRENT_DATE-$CURRENT_SEQ/nohup.out 2> /dev/null

mkdir $LOGS_FOLDER/current

nohup $JAVA_HOME/bin/java -cp $CLASSPATH $ADD_ARGUMENT $ADDITIONAL_JAVA_OPTS $VM_ARGUMENT $MAIN_CLASS $PROGRAM_ARGUMENT > $LOGS_FOLDER/current/logs.nohup.out 2>&1 &
echo $! > $PID_FILE

echo "$MYNAME startup complete."

