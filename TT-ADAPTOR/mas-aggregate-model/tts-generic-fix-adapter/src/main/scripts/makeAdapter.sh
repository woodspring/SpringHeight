#!/bin/sh

newLpName=$1;

newLpNameLowercase=${newLpName,,}

newLpNameUppercase=${newLpName^^}

echo Making LP adapter for $newLpNameUppercase 
TICKTRADE_HOME=/apps/ticktrade

mkdir -p $TICKTRADE_HOME/tts-$newLpNameLowercase-fix-adapter/bin/logs
cp start.sh $TICKTRADE_HOME/tts-$newLpNameLowercase-fix-adapter/bin/.
cp stop.sh $TICKTRADE_HOME/tts-$newLpNameLowercase-fix-adapter/bin/.

sed -i "s@-DAPP_NAME=FA@-DAPP_NAME=FA$newLpNameUppercase -DoperateAs=$newLpNameUppercase@" $TICKTRADE_HOME/tts-$newLpNameLowercase-fix-adapter/bin/start.sh

cd $TICKTRADE_HOME/tts-$newLpNameLowercase-fix-adapter
ln -s $TICKTRADE_HOME/tts-generic-fix-adapter/lib
ln -s $TICKTRADE_HOME/tts-generic-fix-adapter/config
