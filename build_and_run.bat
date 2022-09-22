echo off
set version=%1
set serverPath="C:\Users\Simon\Documents\Minecraft Server\%version%"
copy target\autoregion-0.6.2.jar %serverPath%\plugins\
copy target\autoregion-0.6.2.jar release\autoregion-0.6.2.jar
cd %serverPath%
.\start.bat
