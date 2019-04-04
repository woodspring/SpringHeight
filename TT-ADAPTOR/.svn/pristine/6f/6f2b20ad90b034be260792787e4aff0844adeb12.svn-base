#!/bin/sh

. ~/setenv.sh

BASEDIR="$(dirname "$(dirname "$(readlink -f "$0")")")"

MYNAME=SkewRateEngine
PID_FILE=$BASEDIR/bin/$MYNAME.$(hostname).PID

echo "Shutting down $MYNAME ..."
pid=$(cat $PID_FILE);
kill -15 $pid;
sleep 1;
i=1
c=`ps -ef | grep java | grep -v grep | awk '{print $2}' | grep $pid | wc -l`
while [ $i -le 10 -a $c -ne 0 ]
do
		kill -15 $pid;
		sleep 1;
		c=`ps -ef | grep java | grep -v grep | awk '{print $2}' | grep $pid | wc -l`
        i=`expr $i + 1`
done

sleep 1;
c=`ps -ef | grep java | grep -v grep | awk '{print $2}' | grep $pid | wc -l`
if [ $c -ne 0 ]; then
	kill -9 $pid;
fi
echo "$MYNAME shutdown complete."

