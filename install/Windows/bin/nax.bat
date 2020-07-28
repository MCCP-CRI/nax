@echo off
%~dp0jvm\bin\java -cp %~dp0lib\${project.artifactId}-${project.version}.jar;%~dp0user-jars\* edu.uky.kcr.nax.NaxCommandLineApp %*
