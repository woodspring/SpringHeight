#!/bin/sh

newLpName=$1;

newLpNameLowercase=${newLpName,,}

newLpNameUppercase=${newLpName^^}

echo Making LP simulator for $newLpNameUppercase 
TICKTRADE_HOME=/apps/ticktrade

mkdir -p $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-$newLpNameLowercase/bin/logs
cp start.sh $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-$newLpNameLowercase/bin/.
cp stop.sh $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-$newLpNameLowercase/bin/.

sed -i "s@-DAPP_NAME=MDS@-DAPP_NAME=MDS$newLpNameUppercase -DoperateAs=$newLpNameUppercase@" $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-$newLpNameLowercase/bin/start.sh

cd $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-$newLpNameLowercase
ln -s $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-generic/lib
ln -s $TICKTRADE_HOME/tts-market-liquidity-provider-simulator-generic/config
