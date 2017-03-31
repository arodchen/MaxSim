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
. ./scripts/executeExitOnFail
set -x
executeExitOnFail cd zsim
executeExitOnFail rm -rf build
