#!/usr/bin/env python
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
import sys
import h5py # presents HDF5 files as numpy arrays
import os
import numpy
import json
import MaxSimInterface_pb
import retrieveZSimStatLib

if len(sys.argv) < 4:
  print "The script should have at least 3 input parameters! Pass ZSim stat folder as the first parameter. Pass maxine operation modes as the second parameter. Pass characterstic as the third parameter."
  sys.exit( 1)
  
zsim_stat_dir = sys.argv[1]
maxine_op_modes = map(int, sys.argv[2].split(','))
char = sys.argv[3]

jf = open(os.path.join(zsim_stat_dir, './out.cfg'))
js = retrieveZSimStatLib.convert_config_to_json(jf.read()) 
a = json.loads(js)

f = h5py.File(os.path.join(zsim_stat_dir, 'zsim-ev.h5'), 'r')
dset = f['stats']['root']

if char == 'C':
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_cycles, a, dset, maxine_op_modes))

elif char == 'I':
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))

elif char == 'IPC':
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_cycles, a, dset, maxine_op_modes))))


elif char == 'CHLD':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_hits, a, dset, maxine_op_modes, zcn))

elif char == 'CHST':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_hits, a, dset, maxine_op_modes, zcn))

elif char == 'CHLDST':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_hits, a, dset, maxine_op_modes, zcn)) +
         numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_hits, a, dset, maxine_op_modes, zcn)))

elif char == 'CHLDPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_hits, a, dset, maxine_op_modes, zcn)) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CHSTPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_hits, a, dset, maxine_op_modes, zcn)) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CHLDSTPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float((numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_hits, a, dset, maxine_op_modes, zcn)) +
                numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_hits, a, dset, maxine_op_modes, zcn))) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CMLD':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_misses, a, dset, maxine_op_modes, zcn))

elif char == 'CMST':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_misses, a, dset, maxine_op_modes, zcn))

elif char == 'CMLDST':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_misses, a, dset, maxine_op_modes, zcn)) +
         numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_misses, a, dset, maxine_op_modes, zcn)))

elif char == 'CMLDPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_misses, a, dset, maxine_op_modes, zcn)) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CMSTPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_misses, a, dset, maxine_op_modes, zcn)) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CMLDSTPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float((numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_load_misses, a, dset, maxine_op_modes, zcn)) +
                numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_store_misses, a, dset, maxine_op_modes, zcn))) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))


elif char == 'CALD':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_loads, a, dset, maxine_op_modes, zcn))

elif char == 'CAST':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_stores, a, dset, maxine_op_modes, zcn))

elif char == 'CALDST':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_loads, a, dset, maxine_op_modes, zcn)) +
         numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_stores, a, dset, maxine_op_modes, zcn)))

elif char == 'CALDPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_loads, a, dset, maxine_op_modes, zcn)) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CASTPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_stores, a, dset, maxine_op_modes, zcn)) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

elif char == 'CALDSTPKI':
  if len(sys.argv) < 5:
    print "The script should have the 4th cache name parameter for " + char + " characteristic."
    sys.exit(1)
  zcn = sys.argv[4]
  print (float((numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_loads, a, dset, maxine_op_modes, zcn)) +
                numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_arg_sum(retrieveZSimStatLib.cache_stores, a, dset, maxine_op_modes, zcn))) * 1000) /
         float(numpy.sum(retrieveZSimStatLib.for_each_mom_entry_f_sum(retrieveZSimStatLib.core_instrs, a, dset, maxine_op_modes))))

          
else:
  print char + " characteristic is not supported!"
  print "Valid characteristics are:"
  print "  C - cycles"
  print "  I - instructions"
  print "  IPC - instructions per clock"
  print "  C[H|M|A][LD|ST|LDST](PKI) - cache characteristics"
  print "    [..|..] - required alternatives"
  print "    (..|..) - optional alternatives"
  print "    H       - hits"
  print "    M       - misses"
  print "    A       - accesses"
  print "    LD      - loads"
  print "    ST      - stores"
  print "    LDST    - loads and stores"
  print "    PKI     - per kilo instruction"
  
  sys.exit(1)

