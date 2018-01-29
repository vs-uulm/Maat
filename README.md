Getting subjective logic code through submodules:
```sh
git submodule init
git submodule update
```
You'll need to do the `init` for any new clone of the project.

# Getting Started

Maat is currently primarily designed to take input from simulations run in the Veins package, which simulates all the necessary communication between vehicles. Maat is only responsible for the misbehavior detection internal to a vehicle; however, it is possible to give Maat other input by supplying appropriately designed JSON input. Refer to the documentation for more information.

Because Maat and VEINS are decoupled, you can use existing output, and setting up VEINS is only needed when you wish to generate custom simulation output (e.g., analyze new types of packets or different payload).

## Setting up simulations (OPTIONAL)

Install the VEINS package, e.g., using [this tutorial](http://veins.car2x.org/tutorial/); however, you should use Maat's copy of VEINS instead of the current version (4.6), which includes several enhancements that take care of data output.

This means installing [OMNeT++](https://omnetpp.org/omnetpp) in version 5.1.1; compile it using `./configure && make`. If you don't have Qt, compilation will fail and tell you that this is the case; if this happens, disable the corresponding options in the file `configure.user` (set variables `WITH_QTENV`, `PREFER_QTENV`, `WITH_OSG`, `WITH_OSGEARTH` to no): this will give you the older Tk-based GUI (but everything else will remain the same).

**Note:** make sure that the OMNeT++ bin directory (`..../omnetpp-5.1.1/bin`) is in your `PATH`, and the lib directory (`..../omnetpp-5.1.1/lib`) is in your `LD_LIBRARY_PATH`. I recommend adding this to your `.bashrc`.

Install [SUMO](http://sumo.dlr.de/wiki/Simulation_of_Urban_MObility_-_Wiki) in version 0.30.0; either use the repository of your operating system (make sure the version is correct!), or compile it manually. For some Ubuntu versions there are known issues with dependencies (see the VEINS tutorial for details and a fix). 

**Note:** make sure that `sumo` is accessible through your `PATH` (use `which sumo` to check). I recommend adding this to your `.bashrc` if you compiled your own version of SUMO.

Install [VEINS-Maat](https://gitlab-vs.informatik.uni-ulm.de/rens.vanderheijden/veins/): check out the `attacker_simulation` branch and compile the code with `./configure && make`. VEINS-Maat also includes the [LuST](https://github.com/lcodeca/LuSTScenario/wiki) scenario, which provides realistic vehicle movement data based on the city of Luxembourg.

Test that your setup works by going to `veins-maat/examples/veins` and typing `./run`. This should start the OMNeT++ interface; use the play button on top to start the simulation.

## Setting up Maat

..
