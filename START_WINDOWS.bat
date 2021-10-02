@echo off
echo Java home: %JAVA_HOME%
java -Djava.library.path=. -jar Liberty-Way.jar
pause