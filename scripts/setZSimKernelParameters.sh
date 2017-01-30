#!/bin/bash
if [[ $UID != 0 ]]; then
    echo "Superuser privilege is required to run this script!"
    echo "Example: sudo $0 $*"
    exit 1
fi
set -x
sysctl -w kernel.randomize_va_space=0
sysctl -w kernel.yama.ptrace_scope=0
sysctl -w kernel.shmmax=1073741824
