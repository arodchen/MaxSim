MaxSim
======

Description
-----------
**MaxSim** is a simulation platform based on the Maxine VM, the ZSim simulator, and the McPAT modeling framework.
MaxSim is able to simulate fast and accurately managed workloads running on top of Maxine VM.
It features pointer tagging, low-intrusive microarchitectural profiling based on tagged pointers and modeling of complex software changes via address-space morphing. You can find more details in the paper:

*"MaxSim: A Simulation Platform for Managed Applications",
Andrey Rodchenko, Christos Kotselidis, Andy Nisbet, Antoniu Pop, and Mikel Lujan, ISPASS 2017 (Best Paper Award)*. [[paper]](https://www.researchgate.net/publication/316473850_MaxSim_A_Simulation_Platform_for_Managed_Applications) [[slides]](http://apt.cs.manchester.ac.uk/people/arodchenko/MaxSim_A_Simulation_Platform_for_Managed_Applications_Slides.pdf)

##### Acknowledgements
This work is partially supported by EPSRC grants PAMELA EP/K008730/1,
DOME EP/J016330/1, and EU Horizon 2020 ACTiCLOUD 732366 grant. 
A. Rodchenko is funded by a Microsoft Research PhD Scholarship,
A. Pop is funded by a Royal Academy of Engineering Research Fellowship,
and M. Lujan is funded by a Royal Society University Research Fellowship.


Dependencies
------------
`Maxine VM dependencies, ZSim dependencies, McPAT dependencies, protobuf-2.6.1, timelimit`

Environment Variables
---------------------
`Maxine VM, ZSim, McPAT environment variables`

`PROTOBUFPATH=<protobuf-2.6.1 install path>`

`MCPATPATH=<McPAT 1.0 bin path>`

Usage
-----
##### Building, Cleaning, Style Checking, and Setting Kernel Parameters
`./scripts/generateMaxSimInterface.sh`             - generates MaxSim interface.

`./scripts/setZSimKernelParameters.sh`             - sets ZSim kernel parameters (requires sudo).

`./scripts/buildMaxine<Debug|Product>.sh`          - builds Maxine VM (and re-generates MaxSim interface).

`./scripts/buildImageC1X<Debug|Product>.sh`        - builds Maxine VM image.

`./scripts/buildZSim<Debug|Product>.sh`            - builds ZSim (and re-generates MaxSim interface).

`./scripts/buildMaxSim<Debug|Product>.sh`          - builds MaxSim (does all mentioned above).

`./scripts/cleanMaxine.sh`                         - cleans Maxine VM.

`./scripts/cleanZSim.sh`                           - cleans ZSim.

`./scripts/cleanMaxSim.sh`                         - cleans Maxine VM and ZSim.

`./scripts/checkStyle.sh`                          - checks style in Maxine VM.

##### Running DaCapo-9.12-bach Benchmarks
Command:
```shell
./scripts/runMaxSimDacapo.sh <output directory> <ZSim template configuration> <number of runs>
```
Arguments:
```
    <output directory> - existing output directory where results are stored
                         (overwrites existing results in the directory)
    <ZSim template configuration> - ZSim template configuration in which COMMAND_TEMPLATE is
                                    replaced by actual command to be executed by ZSim 
                                    (e.g. ./zsim/tests/*.tmpl)
    <number of runs> - number of runs of each benchmark
    EXTRA_MAXINE_FLAGS - environment variable used to pass extra flags to Maxine VM
```

##### MaxSim Interface and Configuration
MaxSim interface is defined using Protocol Buffers 2.6.1 in the following file: `./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto`

Default values of `message MaxSimConfig` define a build-time MaxSim configuration.

`isMaxSimEnabled` indicates whether Maxine and ZSim are configured to work in tandem (`true`) or separately (`false`).

`pointerTaggingType` indicates a type of active pointer tagging. Three types of pointer tagging are available:
`NO_TAGGING` - native x86-64 tagging (sign-extension of the 47th bit),
`CLASS_ID_TAGGING` - objects tagging by IDs of their classes,
`ALLOC_SITE_ID_TAGGING` - objects tagging by IDs of allocation sites.

`layoutScaleFactor` and `layoutScaleRefFactor` are parameters of two bijections of the address space morphing scheme described in the paper. `layoutScaleFactor` is the second parameter of f<sub>e</sub> and the first parameter of f<sub>c</sub> bijection. `layoutScaleRefFactor` is the first parameter of f<sub>e</sub> bijection.

##### ZSim MaxSim-Related Configuration Parameters
`pointerTagging` simulation parameter indicates whether pointer tagging simulation is enabled in ZSim. 

`MAProfCacheGroupId` compact ID can be assigned to a cache. When MaxSim profiling is active, the event related to a specific cache will be aggregated in the corresponding MAProfCacheGroup. `MAProfCacheGroupNames` parameter is associated with caches, and it defines names of MAProfCacheGroups delimited by `|` symbol (e.g. ./zsim/tests/*.tmpl).

NOTE: When working in tandem with Maxine VM `startFastForwarded` Maxine VM process parameter should be set to `true`. Exiting fast forwarding should be performed explicitly in Maxine VM.

##### MaxineVM MaxSim-Related Flags
`-XX:-MaxSimEnterFFOnVMExit`            - makes MaxSim enter fast forwarding mode on VM exit (default: `false`).

`-XX:-MaxSimExitFFOnVMEnter`            - makes MaxSim exit fast forwarding mode on VM enter (default: `false`).

`-XX:MaxSimMaxineInfoFileName=<value>`  - MaxSim Maxine information file name (default: maxine-info.db).

`-XX:-MaxSimPrintProfileOnVMExit`       - makes MaxSim to print profiling information on VM exit (default: `false`).

`-XX:-MaxSimProfiling`                  - enables MaxSim profiling (default: `false`).

`-XX:MaxSimZSimProfileFileName=<value>` - MaxSim ZSim profile file name (default: zsim-prof.db).

`-XX:-TraceMaxSimTagging`               - traces MaxSim tagging.

`-XX:MaxSimDataTransDB=<value>`         - MaxSim data transformation database for address space morphing.

NOTE: All flags related to profiling have effect only when `pointerTaggingType [default = CLASS_ID_TAGGING]` or
`pointerTaggingType [default = ALLOC_SITE_ID_TAGGING]`. `-XX:MaxSimDataTransDB=` accepts `DataTransDB` message with `DataTransInfo`s having `FieldOffsetRemapPair`s representing m<sub>e</sub> reordering map described in the paper.
`-XX:MaxSimDataTransDB=` has effect only when `pointerTaggingType [default = CLASS_ID_TAGGING]`.

##### Controlling Simulation by Managed Applications
MaxSim simulation can be controlled by managed applications by setting `MaxSim.Command` property (via a call to `System.setProperty("MaxSim.Command", <value>)`) the following values:

`"ROI_BEGIN()"` Exits fast-forwarding mode and starts simulation of a region of interest.

`"ROI_END()"`   Enters fast-forwarding mode and stops simulation of a region of interest.

`"PRINT_PROFILE_TO_FILE(<file name>)"` Prints profile to a file with a specified name.

`"RESET_PROFILE_COLLECTION()"` Resets profile collection.

NOTE: All commands related to profiling have effect only when `pointerTaggingType [default = CLASS_ID_TAGGING]` or
`pointerTaggingType [default = ALLOC_SITE_ID_TAGGING]` and `-XX:+MaxSimProfiling` flag is passed to Maxine VM.

##### Printing Profiling Information in the Textual Format
Command:
```shell
cd maxine
../graal/mxtool/mx maxsimprofprint <flags>
```
Flags:
```
    -MaxineInfoDB=<arg>
        Location of the file containing Maxine information database.
    -ZSimProfileDB=<arg>
        Location of the file containing ZSim profile database.
    -help[=true|false, t|f, y|n] (default: false)
        Show help message and exit.
    -o=<arg> (default: maxsim-prof.txt)
        Output file name.
```

##### Retrieving Statistics Collected by ZSim
Command:
```shell
./scripts/retrieveZSimStat.py <ZSim stat dir> <Maxine VM oper modes> <characteristic> (<cahe name>)
```
Arguments:
```
    <ZSim stat dir> - directory containing ZSim stat files (zsim-ev.h5)
    <Maxine VM oper modes> - comma-separated numerical list of Maxine VM operation modes
                             for which statistics is retrieved. Operation modes are
                             listed in MaxineVMOperationMode in MaxSimInterface.proto
    <characteristic> - retrieved characteristic. Supported characteristics are:
                       C - cycles
                       I - intructions
                       IPC - intructions per clock
                       C[H|M|A][LD|ST|LDST](PKI) - cache characteristics
                           [..|..] - required alternatives
                           (..|..) - optional alternatives
                           H       - hits
                           M       - misses
                           A       - accesses
                           LD      - loads
                           ST      - stores
                           LDST    - loads and stores
                           PKI     - per kilo instruction
    <cahe name> - cache name required only for cache characteristics listed avove
```
NOTE: The parts of this script were obtained from [the ZSim-NVMain simulator](https://github.com/AXLEproject/axle-zsim-nvmain).

##### Modeling Power and Energy Using McPAT
Command:
```shell
./scripts/runMcPAT.py
```
Flags:
```
    [-h (help)]
    [-z <zsim-stat-dir>] - directory containing ZSim stat files (zsim-ev.h5)
    [-e <maxine-op-modes>] - comma-separated numerical list of Maxine VM operation modes
                             for which statistics is retrieved. Operation modes are
                             listed in MaxineVMOperationMode in MaxSimInterface.proto
    [-d <resultsdir (default: .)>]
    [-t <type: total|dynamic|static|peak|peakdynamic|area>]
    [-o <output-file (power{.png,.txt,.py})>]
```
NOTE: The parts of this script were obtained from [the Sniper simulator](http://snipersim.org/w/The_Sniper_Multi-Core_Simulator) under [MIT License](http://snipersim.org/w/License).

Recipes
-------
Profiles simple `./maxine/com.oracle.max.tests/src/test/output/HelloWorld.java` application using `4C` ZSim configuration (the configuration description is in the paper):
```shell
# Changes pointerTaggingType default type to CLASS_ID_TAGGING
sed -i 's/pointerTaggingType = 2 \[default = NO_TAGGING/pointerTaggingType = 2 \[default = CLASS_ID_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
# Builds MaxSim
./scripts/buildMaxSimProduct.sh
# Simulates HelloWorld application and produces ZSim profile and Maxine information files (zsim-prof.db and maxine-info.db).
./zsim/build/release/zsim ./zsim/tests/Nehalem-4C_MaxineHelloWorld.cfg
# Prints profile to maxsim-prof.txt
pushd maxine
../graal/mxtool/mx maxsimprofprint -MaxineInfoDB=../maxine-info.db -ZSimProfileDB=../zsim-prof.db -o=../maxsim-prof.txt
popd
# Changes back pointerTaggingType to NO_TAGGING
sed -i 's/pointerTaggingType = 2 \[default = CLASS_ID_TAGGING/pointerTaggingType = 2 \[default = NO_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
```
Profiles simple `./maxine/com.oracle.max.tests/src/test/output/MaxSimSingleLinkedList.java` application using `1CQ` ZSim configuration (the configuration description is in the paper):
```shell
# Changes pointerTaggingType default type to CLASS_ID_TAGGING
sed -i 's/pointerTaggingType = 2 \[default = NO_TAGGING/pointerTaggingType = 2 \[default = CLASS_ID_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
# Builds MaxSim
./scripts/buildMaxSimProduct.sh
# Simulates MaxSimSingleLinkedList application and produces three ZSim profile and one Maxine information files.
./zsim/build/release/zsim ./zsim/tests/Nehalem-1CQ_MaxSimSingleLinkedList.cfg
# Change back pointerTaggingType to NO_TAGGING
sed -i 's/pointerTaggingType = 2 \[default = CLASS_ID_TAGGING/pointerTaggingType = 2 \[default = NO_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
```
Characterizes and profiles `DaCapo-9.12-bach` using `1CQ` ZSim configuration (the configuration description is in the paper):
```shell
sed -i 's/pointerTaggingType = 2 \[default = NO_TAGGING/pointerTaggingType = 2 \[default = ALLOC_SITE_ID_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
mkdir dacapo_characterization
EXTRA_MAXINE_FLAGS="-XX:+MaxSimProfiling -XX:+MaxSimPrintProfileOnVMExit" ./scripts/runMaxSimDacapo.sh dacapo_characterization ./zsim/tests/Nehalem-1CQ.tmpl 1
sed -i 's/pointerTaggingType = 2 \[default = ALLOC_SITE_ID_TAGGING/pointerTaggingType = 2 \[default = NO_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
```
Retrieves L3 Cache Misses per Kilo Instruction:
```shell
./scripts/retrieveZSimStat.py ./dacapo_characterization/zsim/DaCapo-9.12-bach_eclipse_product_0 1,2 CMLDPKI l3
```
Models energy spent in the Garbage Collection (GC) part of the workload:
```shell
./scripts/runMcPAT.py -z ./dacapo_characterization/zsim/DaCapo-9.12-bach_eclipse_product_0 -e 2
```
Models energy spent in the non-GC part of the workload:
```shell
./scripts/runMcPAT.py -z ./dacapo_characterization/zsim/DaCapo-9.12-bach_eclipse_product_0 -e 1
```
Profiles simple `./maxine/com.oracle.max.tests/src/test/output/HelloWorld.java` application using `1CQ` ZSim configuration, modeling compressed object pointers and `String` objects' fields reordering using `./misc/DataTrans/HelloWorldCompPointDataTrans.db` as described in the example shown in Figure 7 in the paper:
```shell
# Changes pointerTaggingType default type to CLASS_ID_TAGGING and layoutScaleFactor to 2
sed -i 's/pointerTaggingType = 2 \[default = NO_TAGGING/pointerTaggingType = 2 \[default = CLASS_ID_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
sed -i 's/layoutScaleFactor = 3 \[default = 1/layoutScaleFactor = 3 \[default = 2/' maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
# Builds MaxSim
./scripts/buildMaxSimProduct.sh
# Simulates HelloWorld application and produces ZSim profile and Maxine information files (zsim-prof.db and maxine-info.db).
./zsim/build/release/zsim ./zsim/tests/Nehalem-1CQ_MaxineHelloWorldCompPointDataTrans.cfg
# Prints profile to maxsim-prof.txt
pushd maxine
../graal/mxtool/mx maxsimprofprint -MaxineInfoDB=../maxine-info.db -ZSimProfileDB=../zsim-prof.db -o=../maxsim-prof.txt
popd
# Change back pointerTaggingType to NO_TAGGING and layoutScaleFactor to 1
sed -i 's/pointerTaggingType = 2 \[default = CLASS_ID_TAGGING/pointerTaggingType = 2 \[default = NO_TAGGING/' ./maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
sed -i 's/layoutScaleFactor = 3 \[default = 2/layoutScaleFactor = 3 \[default = 1/' maxine/com.oracle.max.vm/src/com/sun/max/vm/maxsim/MaxSimInterface.proto
```
