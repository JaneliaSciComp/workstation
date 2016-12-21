@ECHO OFF
if "%~2"=="" (
    echo USAGE %0 OldVersion NewVersion
    goto :eof
)
REM   Setting to base directory...
cd %~dp0 
java -cp DataBrowserModule/build/classes org.janelia.it.workstation.browser.util.VersionNumberIncrementer %1 %2

:eof