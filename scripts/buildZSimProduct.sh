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

. ./scripts/executeExitOnFail
set -x
: ${PINPATH?"PINPATH is not set!"}
: ${PROTOBUFPATH?"PROTOBUFPATH is not set!"}
: ${LIBCONFIGPATH?"LIBCONFIGPATH is not set! Hint: export LIBCONFIGPATH=/usr/lib/x86_64-linux-gnu"}
: ${POLARSSLPATH?"POLARSSLPATH is not set! Hint: export POLARSSLPATH=/usr/lib/"}
executeExitOnFail cd zsim
executeExitOnFail scons --r
