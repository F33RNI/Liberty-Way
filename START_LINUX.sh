#!/bin/bash
set echo off
echo Java home: $JAVA_HOME
java -version ""
echo ""
java -Djava.library.path=. -jar "./Liberty-Way.jar" -c ""
read -s -n 1 -p "Press any key to continue . . ."
echo ""