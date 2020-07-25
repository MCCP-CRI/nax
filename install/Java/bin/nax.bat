@echo off
java -cp %~dp0..\lib\*;%~dp0..\user-extensions\* edu.uky.kcr.nax.NaxCommandLineApp %*
