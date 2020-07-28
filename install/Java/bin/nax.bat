@echo off
java -cp %~dp0lib\${project.artifactId}-${project.version}.jar;%~dp0user-jars\* edu.uky.kcr.nax.NaxCommandLineApp %*
