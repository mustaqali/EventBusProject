EventBus Build Instruction for Unix & Windows:

-----------------UNIX------------------------------------

#------ build_and_test_all.sh ---------------------
# Use the following script to build all source and test and run the junit test

#  make sure java1.8 is set for your env before build
#export JAVA_HOME=c:/java/java1.8 

export BASEDIR=.
export LIB=$BASEDIR/lib
export SRC=$BASEDIR/src/com/EventBus
export TEST=$BASEDIR/test/com/EventBus

export SRC_OUT=$BASEDIR/out/production/EventBusProject
export TEST_OUT=$BASEDIR/out/test/EventBusProject

export $CLASSPATH=LIB/junit-4.12.jar:$LIB/hamcrest-core-1.3.jar

export $CLASSPATH=$CLASSPATH:$SRC_OUT:$TEST_OUT

#build src and test files
javac -d  SRC_OUT  $SRC/*.java
javac -d  TEST_OUT   -cp $CLASSPATH  $TEST/*.java

#Run junit tests
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.SyncEventBusBasicTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.SuperBulkTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.AsyncBusBasicTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.AsyncBusCacheLatestEventTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.AsyncBusFilterEventTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.AsyncBusMultiThreadTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.AsyncBusThreadHashDistributionTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.SyncEventBusReentrantTest
java -cp $CLASSPATH  org.junit.runner.JUnitCore   com.EventBus.AsyncBusCallbackReentrantTest

-------

#-------- build_and_run_example.sh -----
# Use this script to run a sample Event Bus pub/sub client & test---------

#Make sure java1.8 is set as per your env before build
#export JAVA_HOME=C:/java/jdk1.8
export path=$JAVA_HOME/bin

export BASEDIR=.
export CLASSPATH=$BASEDIR/out/production/EventBusProject

javac -cp $CLASSPATH EventBusExample.java
java  -cp  $CLASSPATH;./ EventBusExample




-------------------------------------WINDOWS: ---------------------------------------------------------------------------------------

set CLASSPATH=%LIB%\junit-4.12.jar;%LIB%\hamcrest-core-1.3.jar

set CLASSPATH=%CLASSPATH%;%SRC_OUT%;%TEST_OUT%

javac -d  %SRC_OUT%   %SRC%\*.java
javac -d  %TEST_OUT%    -cp %CLASSPATH%   %TEST%\*.java


java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.SyncEventBusBasicTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.SuperBulkTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.AsyncBusBasicTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.AsyncBusCacheLatestEventTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.AsyncBusFilterEventTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.AsyncBusMultiThreadTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.AsyncBusThreadHashDistributionTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.SyncEventBusReentrantTest
java -cp %CLASSPATH%   org.junit.runner.JUnitCore   com.EventBus.AsyncBusCallbackReentrantTest

---------



REM-------- build_and_run_example.bat -----
REM  Use this script to run a sample Event Bus pub/sub client & test---------

REM make sure java1.8 is set as per your env before build
set JAVA_HOME=C:\java\jdk1.8
set path=%JAVA_HOME%\bin

set BASEDIR=.
set CLASSPATH=%BASEDIR%\out\production\EventBusProject

javac -cp %CLASSPATH% EventBusExample.java
java  -cp  %CLASSPATH%;.\ EventBusExample
