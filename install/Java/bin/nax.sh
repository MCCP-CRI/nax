#!/bin/sh
java -cp $(dirname $0)/lib/${project.artifactId}-${project.version}.jar:$(dirname $0)/user-jars/* edu.uky.kcr.nax.NaxCommandLineApp "$@"
