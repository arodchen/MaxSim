MaxSim
======

Description
-----------
**MaxSim** is a simulation platform based on the Maxine VM, the ZSim simulator, and the McPAT modeling framework.
MaxSim is able to simulate fast and accurately managed workloads running on top of Maxine VM.
It features pointer tagging, low-intrusive microarchitectural profiling based on tagged pointers and modeling of complex software changes via address-space morphing. You can find more details in the paper:

*"MaxSim: A Simulation Platform for Managed Applications",
Andrey Rodchenko, Christos Kotselidis, Andy Nisbet, Antoniu Pop, and Mikel Lujan, ISPASS 2017*.

##### Acknowledgements
This work is partially supported by EPSRC grants PAMELA EP/K008730/1,
DOME EP/J016330/1, and EU Horizon 2020 ACTiCLOUD 732366 grant. 
A. Rodchenko is funded by a Microsoft Research PhD Scholarship,
A. Pop is funded by a Royal Academy of Engineering Research Fellowship,
and M. Lujan is funded by a Royal Society University Research Fellowship.


Dependencies
------------
`Maxine VM dependencies, ZSim dependencies, McPAT dependencies, protobuf-2.6.1, timelimit`

Environment variables
---------------------
`Maxine VM, ZSim, McPAT enviroment variables`

`PROTOBUFPATH=<protobuf-2.6.1 install path>`

`MCPATPATH=<McPAT 1.0 bin path>`

Usage
-----
##### Building, Cleaning and Setting Kernel Parameters
`./scripts/generateMaxSimInterface.sh`             - generates MaxSim interface

`./scripts/setZSimKernelParameters.sh`             - sets ZSim kernel parameters (requires sudo)

`./scripts/buildMaxine<Debug|Product>.sh`          - builds Maxine VM (and re-generates MaxSim interface)

`./scripts/buildImageC1X<Debug|Product>.sh`        - builds Maxine VM image

`./scripts/buildZSim<Debug|Product>.sh`            - builds ZSim (and re-generates MaxSim interface)

`./scripts/buildMaxSim<Debug|Product>.sh`          - builds MaxSim (does all mentioned above)

`./scripts/cleanMaxine.sh `                        - cleans Maxine VM

`./scripts/cleanZSim.sh `                          - cleans ZSim

`./scripts/cleanMaxSim.sh `                        - cleans Maxine VM and ZSim

##### Running DaCapo-9.12-bach Benchmarks
```
./scripts/runMaxSimDacapo.sh <output directory> <ZSim template configuration> <number of runs>
```

```
    <output directory> - existing output directory where results are stored
                         (overwrites existing results in the directory)
    <ZSim template configuration> - ZSim template configuration in which COMMAND_TEMPLATE is
                                    replaced by actual command to be executed by ZSim 
                                    (e.g. ./zsim/tests/*.tmpl)
    <number of runs> - number of runs of each benchmark
```
