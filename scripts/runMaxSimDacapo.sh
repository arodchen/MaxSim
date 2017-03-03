#!/bin/bash
# Copyright 2017 Andrey Rodchenko, School of Computer Science, The University of Manchester
# 
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#                                                                          
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program. If not, see <http://www.gnu.org/licenses/>.
#
if [ -z "$1" ] ; then
    echo Output directory is not specified! Pass output directory as the first argument.
    exit 1 
fi
if [ ! -d "$1" ]; then
    echo Output directory $1 does not exist.
    exit 1 
fi
if [ -z "$2" ] ; then
    echo Simulator template configuration is not specified! Pass simulator template configuration as the second argument.
    exit 1 
fi
if [ ! -f "$2" ]; then
    echo Simulator template configuration $2 does not exist.
    exit 1 
fi
if [ -z "$3" ] ; then
    echo Number of executions is not specified! Pass number of executions as the third argument.
    exit 1 
fi
. ./scripts/executeExitOnFail
. ./scripts/approximateLocks
set -x

SCRIPT_NAME=$(basename "$0")
OUTPUT_DIR=$(readlink -f $1)
SIMULATOR_TMPL_CFG=$(readlink -f $2)
SIMULATOR_TMPL_CFG_BASENAME=$(basename "$2")
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

executeExitOnFail ./scripts/buildMaxSimProduct.sh

executeExitOnFail pushd ./graal/lib
if [ ! -f ./$BENCHMARK_JAR ]; then
    executeExitOnFail wget "http://downloads.sourceforge.net/project/dacapobench/9.12-bach/$BENCHMARK_JAR"
fi
CP_JAR_FLAGS=" -cp ../misc/DaCapoCallbacks:../graal/lib/$BENCHMARK_JAR Harness -c MaxSimCallback"
executeExitOnFail popd

executeExitOnFail pushd ./maxine

removeDeadLock $SCRIPT_NAME

TESTS_NUM=${#TESTS[@]}
LAST_J=$((TESTS_NUM-1))
LAST_I=$((EXECS_NUM-1))
for i in $(seq 0 $LAST_I) ; do
    for j in $(seq 0 $LAST_J) ; do

        executeExitOnFail rm -rf heartbeat out.cfg zsim-cmp.h5 zsim-ev.h5 zsim.h5 zsim.log.0 zsim.out

        ZSIM_COMMAND="../maxine/com.oracle.max.vm.native/generated/linux/maxvm $EXTRA_MAXINE_FLAGS -XX:-InlineTLAB -Xss1M -Xms2G -Xmx2G -XX:ReservedBaselineCodeCacheSize=384M $CP_JAR_FLAGS ${TESTS[j]} -n${ITERS[j]}"
        SIMULATOR_TEST_CFG="${SIMULATOR_TMPL_CFG_BASENAME/.tmpl/_${TESTS[j]}.cfg}"
        executeExitOnFail cp $SIMULATOR_TMPL_CFG ${SIMULATOR_TEST_CFG}
        executeExitOnFail sed -i s\|COMMAND_TEMPLATE\|"$ZSIM_COMMAND"\|g ${SIMULATOR_TEST_CFG}
        TEST_STDERR_FILE=$OUTPUT_DIR/DaCapo-9.12-bach_${TESTS[j]}_product_$i.out
        executeExitOnFail touch $TEST_STDERR_FILE

        acquireLock ${TESTS[j]}
        TAKE=0
        while !(grep -q PASSED $TEST_STDERR_FILE) && (($TAKE < $TEST_TAKES_NUM)); do
            timelimit -T $TEST_TIMELIMIT -t $WARN_TIMELIMIT ../zsim/build/release/zsim $SIMULATOR_TEST_CFG 2>$TEST_STDERR_FILE
            TAKE=$[$TAKE+1]
        done
        releaseLock ${TESTS[j]}

        ZSIM_TEST_OUT_DIR=$OUTPUT_DIR/zsim/DaCapo-9.12-bach_${TESTS[j]}_product_$i/
        executeExitOnFail mkdir -p $ZSIM_TEST_OUT_DIR
        executeExitOnFail mv $SIMULATOR_TEST_CFG out.cfg zsim-cmp.h5 zsim-ev.h5 zsim.h5 zsim.log.0 zsim.out heartbeat $ZSIM_TEST_OUT_DIR
    done
done

executeExitOnFail popd

