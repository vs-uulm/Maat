#!/usr/bin/python3

import json
import os
from functools import reduce

inDir = "with_gt"
graphDir = "graphs"

detectorNames = ["de.uulm.vs.autodetect.posverif.ART"]#, "de.uulm.vs.autodetect.posverif.SAW"]

#parameter that is being studied (useful for detectors with >1 parameter)
thresholdName = "THRESHOLD"

simulationList = [f for f in os.listdir(inDir) if os.path.isfile(os.path.join(inDir, f)) and f.endswith('.json')]

#result is an array of name, params, result triplet; params is an array of key-value tuples
#we want detector name -> {threshold : (FP,FN,TP,TN))} for different thresholds
def mapToResult(obj):
    results = obj['results']
    mapResult = {}
    for item in results:
        (name, pars, res) = item
    
        if name in detectorNames:
            if not name in mapResult:
                mapResult[name]={}
    
            threshold = None
    
            for par in pars:
                (key, value) = par
                if key == thresholdName:
                    threshold = value
            if not thresholdName:
                #this detector does not have the specified parameter, skip it
                continue
            resArray = None
            if res == "FP":
                resArray = (1,0,0,0)
            elif res == "FN":
                resArray = (0,1,0,0)
            elif res == "TP":
                resArray = (0,0,1,0)
            elif res == "TN":
                resArray = (0,0,0,1)
            else:
                print("Warning, unexpected result",item,name)
            mapResult[name][threshold] = resArray
    #endfor -- result item
    return mapResult

def accumulate(acc, item):
    #this assumes that every detector and every threshold occurs for every message
    #TODO insert (0,0,0,0) for missing stuff..?)
    result = {}
    for key in acc:
        result[key]={}
        for thld in acc[key]:
            result[key][thld] = (acc[key][thld][0] + item[key][thld][0],
                                 acc[key][thld][1] + item[key][thld][1],
                                 acc[key][thld][2] + item[key][thld][2],
                                 acc[key][thld][3] + item[key][thld][3])
    return result

def precisionAndRecall(data):
    (FP, FN, TP, TN) = data
    positive = FP + TP
    relevant = TP + FN
    precision = TP/positive
    recall = TP/relevant

    if precision < 0.0 or precision > 1.0:
        print("Warning, bad precision")

    if recall < 0.0 or recall > 1.0:
        print("Warning, bad recall")

    return (precision, recall)

accumulatedResultSet = []
for sim in simulationList:
    print("working on",sim)

    inFileName = os.path.join(inDir, sim)
    simResultSet = []
    with open(inFileName, 'r') as inFile:
        
        #map
        for line in inFile:
            obj = json.loads(line)

            mapRes = mapToResult(obj)

            simResultSet.append(mapRes)
    #reduce
    simAccumulate = reduce(accumulate, simResultSet)
    
    #reduces to {NAME -> {thld -> (FP,FN,TP,TN)}}
    for name in simAccumulate:
        if name == "de.uulm.vs.autodetect.posverif.ART":
            print(simAccumulate)
        for thld in simAccumulate[name]:
            simAccumulate[name][thld] = precisionAndRecall(simAccumulate[name][thld])
        if name == "de.uulm.vs.autodetect.posverif.ART":
            print(simAccumulate)
    
    accumulatedResultSet.append(simAccumulate)

import matplotlib.pyplot as plt



for detector in detectorNames:
    print('creating graph for',detector)
    (fig, axes) = plt.subplots()
    axes.set_xlabel("precision")
    axes.set_ylabel("recall")
    axes.set_xlim([0,1])
    axes.set_ylim([0,1])
    dataset = []
    index = []
    for threshold in accumulatedResultSet[0][detector]:
        (precision, recall) = (0,0)
        for item in accumulatedResultSet:
            precision += item[detector][threshold][0]
            recall += item[detector][threshold][1]
        dataset.append([precision/len(accumulatedResultSet), recall/len(accumulatedResultSet)])
        index.append(threshold)
        #TODO stddev?
    print(dataset)
    axes.scatter(list(map(lambda a: a[0], dataset)), list(map(lambda a:a[1], dataset)))

    c = 0
    for item in index:
        axes.annotate(str(item), xy = (dataset[c]), xycoords='data', xytext=(0.1,0.1))
        c+=1

    plt.savefig(os.path.join(graphDir, detector + ".png"), format='png')
    plt.close()

