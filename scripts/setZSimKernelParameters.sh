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
if [[ $UID != 0 ]]; then
    echo "Superuser privilege is required to run this script!"
    echo "Example: sudo $0 $*"
    exit 1
fi
set -x
sysctl -w kernel.randomize_va_space=0
sysctl -w kernel.yama.ptrace_scope=0
sysctl -w kernel.shmmax=1073741824
