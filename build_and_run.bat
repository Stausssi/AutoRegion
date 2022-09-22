echo off
set version=%1
set serverPath="C:\Users\Simon\Documents\Minecraft Server\%version%"
copy target\autoregion-0.6.2.jar %serverPath%\plugins\
cd %serverPath%
.\start.bat
