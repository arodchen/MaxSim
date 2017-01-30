#!/bin/bash
. ./scripts/executeExitOnFail
set -x
: ${PINPATH?"PINPATH is not set!"}
: ${PROTOBUFPATH?"PROTOBUFPATH is not set!"}
: ${LIBCONFIGPATH?"LIBCONFIGPATH is not set! Hint: export LIBCONFIGPATH=/usr/lib/x86_64-linux-gnu"}
: ${POLARSSLPATH?"POLARSSLPATH is not set! Hint: export POLARSSLPATH=/usr/lib/"}
executeExitOnFail cd zsim
executeExitOnFail scons --r
