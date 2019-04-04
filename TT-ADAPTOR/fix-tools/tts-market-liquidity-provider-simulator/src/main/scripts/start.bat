@echo off

set LIB_FOLDER=..\lib
set CONFIG_FOLDER=..\config

set CLASSPATH=%LIB_FOLDER%/*
set CLASSPATH=%CONFIG_FOLDER%;%CLASSPATH%

set MAIN_CLASS=com.tts.market.simulator.MarketSimulatorMain

set VM_ARGUMENT="-Xmx256m -XX:MaxDirectMemorySize=1024m"

set JAVA=java

:run
%JAVA% -cp %CLASSPATH% %MAIN_CLASS% %VM_ARGUMENT% 

:end