#!/usr/bin/python3

import json

import os

inDir = "input"
outDir = "with_gt"

simulationList = [f for f in os.listdir(inDir) if os.path.isdir(os.path.join(inDir, f))]
print("list of simulation results:",simulationList)

for simName in simulationList:
    print("starting processing of",simName)
    GT = {}
    #messageID-indexed ground truth dictionary
    with open(os.path.join(inDir, simName, 'GroundTruthJSONlog.json'),'r') as GTFile:
        for line in GTFile:
            GTObject = json.loads(line)
            GT[GTObject["messageID"]] = GTObject
    
    #parse vehicleIDs from file names assuming results-XXX.json as structure (default)
    vehicleList = [int(f[8:-5]) for f in os.listdir(os.path.join(inDir,simName)) if os.path.isfile(os.path.join(inDir,simName,f)) and f.startswith('results-') and f.endswith('.json')]
    print(os.listdir(os.path.join(inDir,simName)))
    print(vehicleList)

    for vehicle in sorted(vehicleList):
        print("in",simName," -- processing vehicle",vehicle)
        inFileName = os.path.join(inDir, simName, "results-" + str(vehicle) + ".json")
        outFileName = os.path.join(outDir, simName + "-with-GT.json")
        with open(inFileName, 'r') as inFile, open(outFileName, 'a') as outFile:
            for line in inFile:
                inObj = json.loads(line)
                #inObj["trust"]
                #inObj["time"]
                #inObj["messageID"]
                #inObj["senderID"]
                inObj["receiverID"] = vehicle

                matchingGT = GT[inObj["messageID"]]

                newResults = []
                for item in inObj["results"]:
                    if item.startswith("Identity::"):
                        continue
                    (detectorName, params) = item[:-1].split('{')
                    paramKVs = []
                    pars = params.split(', ')
                    for kv in pars:
                        (key, value) = kv.split('=')
                        paramKVs.append((key, float(value)))
                    res = inObj["results"][item]
                    if matchingGT["attackerType"] == 0:
                        #not an attack
                        if res > 0.5:
                            #detection says legitimate -- true negative
                            res = 'TN'
                        else:
                            #detection says attack -- false positive
                            res = 'FP'
                    else:
                        if res > 0.5:
                            #detection says legitimate -- false negative
                            res = 'FN'
                        else:
                            #detection says attack -- true positive
                            res = 'TP'
                    newResults.append((detectorName, paramKVs, res))
                inObj["results"] = newResults

                outFile.write(json.dumps(inObj))
                outFile.write('\n') #automatically converts to newline, see documentation of .write
