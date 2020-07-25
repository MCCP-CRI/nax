#!/bin/sh
$(dirname $0)/../jvm/bin/java -cp $(dirname $0)/../lib/*:$(dirname $0)/../user-extensions/* edu.uky.kcr.nax.NaxCommandLineApp "$@"
