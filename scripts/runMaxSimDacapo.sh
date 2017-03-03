#!/bin/bash
# Copyright 2017 Andrey Rodchenko, School of Computer Science, The University of Manchester
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ -z "$1" ] ; then
    echo Output directory is not specified! Pass output directory as the first argument.
    exit 1 
fi
if [ -z "$2" ] ; then
    echo Simulator template configuration is not specified! Pass simulator template configuration which should be located in ./zsim/tests/ as the second argument.
    exit 1 
fi
if [ ! -f ./zsim/tests/$2 ]; then
    echo Simulator template configuration $2 does not exist. It should be located in ./zsim/tests/.
    exit 1 
fi
if [ -z "$3" ] ; then
    echo Number of executions is not specified! Pass number of executions as the third argument.
    exit 1 
fi
. ./scripts/executeExitOnFail
. ./scripts/approximateLocks
set -x

SCRIPT_NAME=`basename "$0"`
OUTPUT_DIR=$(readlink -f $1)
SIMULATOR_TMPL_CFG=$2
EXECS_NUM=$3

TEST_TAKES_NUM=3
TEST_TIMELIMIT=43200
WARN_TIMELIMIT=$[$TEST_TIMELIMIT+1]

BENCHMARK_JAR="dacapo-9.12-bach.jar"
TESTS=(avrora batik eclipse fop h2 jython luindex lusearch pmd sunflow tomcat tradebeans tradesoap xalan)
ITERS=(13     20    16      30  8  13     10      8        13  15      15     13         15        18   )

# Only single instance (mutually) of these tests can be run on the same machine 
SINGLE_INSTANCE_TESTS=(eclipse tomcat tradebeans tradesoap)
SINGLE_INSTANCE_LOCK_DIR=/var/lock/dacapo-9.12-bach-single-instance-tests-lock

executeExitOnFail mkdir $OUTPUT_DIR

executeExitOnFail ./scripts/buildMaxSimProduct.sh

executeExitOnFail pushd ./graal/lib
if [ ! -f ./$BENCHMARK_JAR ]; then
    executeExitOnFail wget "http://downloads.sourceforge.net/project/dacapobench/9.12-bach/$BENCHMARK_JAR"
fi
CP_JAR_FLAGS=" -cp ../misc/DaCapoCallbacks:../graal/lib/$BENCHMARK_JAR Harness -c MaxSimCallback"
executeExitOnFail popd

executeExitOnFail pushd ./maxine

removeDeadLock $SINGLE_INSTANCE_LOCK_DIR $SCRIPT_NAME

TESTS_NUM=${#TESTS[@]}
LAST_J=$((TESTS_NUM-1))
LAST_I=$((EXECS_NUM-1))
for i in $(seq 0 $LAST_I) ; do
    for j in $(seq 0 $LAST_J) ; do

        executeExitOnFail rm -rf heartbeat out.cfg zsim-cmp.h5 zsim-ev.h5 zsim.h5 zsim.log.0 zsim.out

        ZSIM_COMMAND="../maxine/com.oracle.max.vm.native/generated/linux/maxvm $EXTRA_MAXINE_FLAGS -XX:-InlineTLAB -Xss1M -Xms2G -Xmx2G -XX:ReservedBaselineCodeCacheSize=384M $CP_JAR_FLAGS ${TESTS[j]} -n${ITERS[j]}"
        SIMULATOR_TEST_CFG="${SIMULATOR_TMPL_CFG/.tmpl/_${TESTS[j]}.cfg}"
        executeExitOnFail cp ../zsim/tests/$SIMULATOR_TMPL_CFG ${SIMULATOR_TEST_CFG}
        executeExitOnFail sed -i s\|COMMAND_TEMPLATE\|"$ZSIM_COMMAND"\|g ${SIMULATOR_TEST_CFG}
        TEST_STDERR_FILE=$OUTPUT_DIR/DaCapo-9.12-bach_${TESTS[j]}_product_$i.out
        executeExitOnFail touch $TEST_STDERR_FILE

        acquireLock ${TESTS[j]} $SINGLE_INSTANCE_TESTS $SINGLE_INSTANCE_LOCK_DIR
        TAKE=0
        while !(grep -q PASSED $TEST_STDERR_FILE) && (($TAKE < $TEST_TAKES_NUM)); do
            timelimit -T $TEST_TIMELIMIT -t $WARN_TIMELIMIT ../zsim/build/release/zsim $SIMULATOR_TEST_CFG 2>$TEST_STDERR_FILE
            TAKE=$[$TAKE+1]
        done
        releaseLock ${TESTS[j]} $SINGLE_INSTANCE_TESTS $SINGLE_INSTANCE_LOCK_DIR

        ZSIM_TEST_OUT_DIR=$OUTPUT_DIR/zsim/DaCapo-9.12-bach_${TESTS[j]}_product_$i/
        executeExitOnFail mkdir -p $ZSIM_TEST_OUT_DIR
        executeExitOnFail mv $SIMULATOR_TEST_CFG out.cfg zsim-cmp.h5 zsim-ev.h5 zsim.h5 zsim.log.0 zsim.out heartbeat $ZSIM_TEST_OUT_DIR
    done
done

executeExitOnFail popd

