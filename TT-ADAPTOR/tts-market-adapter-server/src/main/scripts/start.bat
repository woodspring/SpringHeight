@echo off

setlocal enableextensions disabledelayedexpansion

rem Where to find java information in registry
set "javaKey=HKLM\SOFTWARE\JavaSoft\Java Runtime Environment"

rem Get current java version
set "javaVersion="
for /f "tokens=3" %%v in ('reg query "%javaKey%" /v "CurrentVersion" 2^>nul') do set "javaVersion=%%v"

rem Test if a java version has been found
if not defined javaVersion (
                echo Java version not found
                goto end
)

rem Get java home for current java version
set "javaDir="
for /f "tokens=2,*" %%d in ('reg query "%javaKey%\%javaVersion%" /v "JavaHome" 2^>nul') do set "javaDir=%%e"

if not defined javaDir (
                echo Java directory not found
                goto end
) else (
                echo JAVA_HOME : %javaDir%
)

set TIBRV_HOME=C:\tibco\tibrv\8.4
set JAVA_HOME=%javaDir%
set JAVA="%JAVA_HOME%"\bin\java
set LIB_FOLDER=..\lib
set CONFIG_FOLDER=..\config
set TIBRV_LIB_FOLDER=%TIBRV_HOME%\lib


set CLASSPATH=%LIB_FOLDER%\*;%TIBRV_LIB_FOLDER%\tibrvnative.jar
set CLASSPATH=%CONFIG_FOLDER%;%CLASSPATH%

set MAIN_CLASS=com.tts.mas.TtsMarketAdapterServerMain

set VM_ARGUMENT=-DAPP_NAME=MDE -DRESTART.COUNT=1 -Dspring.profiles.active=remoteDev4,rv -Dtts_env=remoteDev4 -Dtts_messaging_platform=rv -XX:+CMSClassUnloadingEnabled -Xms512M -Xmx1024M -XX:MaxMetaspaceSize=512M  -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -Dlog4j.configurationFile=%CONFIG_FOLDER%\log-resources\log4j2-dev.xml


:run
%JAVA% %VM_ARGUMENT% -cp %CLASSPATH%  %MAIN_CLASS% 

:end
