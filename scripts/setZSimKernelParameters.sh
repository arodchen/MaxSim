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

if [[ $UID != 0 ]]; then
    echo "Superuser privilege is required to run this script!"
    echo "Example: sudo $0 $*"
    exit 1
fi
set -x
sysctl -w kernel.randomize_va_space=0
sysctl -w kernel.yama.ptrace_scope=0
sysctl -w kernel.shmmax=1073741824
