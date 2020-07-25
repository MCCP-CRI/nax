#!/bin/sh
java -cp $(dirname $0)/../lib/*:$(dirname $0)/../user-extensions/* edu.uky.kcr.nax.NaxCommandLineApp "$@"
