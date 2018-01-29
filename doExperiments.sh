#!/bin/bash

gradle jar
scp "bin/libs/Maat-1.0.jar" bwunicluster.scc.kit.edu:maat/Maat.jar
scp "detectors.xml" bwunicluster.scc.kit.edu:maat/detectors.xml
scp "job.moab" bwunicluster.scc.kit.edu:maat/job.moab
scp "jobscript.sh" bwunicluster.scc.kit.edu:maat/jobscript.sh
scp -r "testdata/testsWithAttacker" bwunicluster.scc.kit.edu:maat/
ssh -t bwunicluster.scc.kit.edu 'cd maat && module load devel/java_jdk && chmod +x jobscript.sh && ./jobscript.sh testsWithAttacker'
