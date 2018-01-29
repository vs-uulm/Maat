#!/usr/bin/python3

import re
import csv

outputFile = "mmsDetection.txt"

skip = 4

trainingModels = []

output = {}

modelRegex = re.compile(r'.*?for (.*?) up to (.*)')
anomStateRegex = re.compile(r'ALERT: ANOMALOUS STATE: (0.[0-9]+).*?-->(.*)')
newStateRegex = re.compile(r'ALERT: ANOMALOUS STATE: .*?-->(.*)')
anomTransRegex = re.compile(r'ALERT: ANOMALOUS TRANSITION: (0.[0-9]+).*?-->(.*?) GOTO (.*)')
newTransRegex = re.compile(r'ALERT: ANOMALOUS TRANSITION: .*?-->(.*?) GOTO (.*)')

currentModel = None
currentTimeStamp = None

with open(outputFile, 'r') as f:
    for line in f:
        if skip > 0:
            skip-=1
            continue

        #first: training models
        if not trainingModels:
            trainingModels = line[len('Training models: '):].split(', ')[:-1]
            skip+=1 #followed by 'Started...'
            continue

        matchobj = None #reset to be safe..
        if line.startswith('Detection scores for'):
            (modelName, timeStampString) = modelRegex.match(line.strip()).groups()
            if not modelName in output:
                output[modelName]={}
            if not timeStampString in output[modelName]:
                output[modelName][timeStampString] = []
            #update current model
            currentModel = modelName
            currentTimeStamp = timeStampString
            continue
        elif line.startswith('ALERT: '):
            if not currentModel or not currentTimeStamp:
                print('Error, invalid input file')
                exit()
            if line.startswith('ALERT: ANOMALOUS STATE: new state'):
                matchobj = newStateRegex.match(line)
                if matchobj:
                    stateName = matchobj.groups()[0].strip()
                    output[currentModel][currentTimeStamp].append([1, stateName])
                else:
                    print('Error parsing: %s'%(line))
            elif line.startswith('ALERT: ANOMALOUS STATE:'):
                try:
                    matchobj = anomStateRegex.match(line)
                    if matchobj:
                        (stateProb, stateName) = matchobj.groups()
                        stateName = stateName.strip()
                        output[currentModel][currentTimeStamp].append([stateProb, stateName])
                    else:
                        print('Error parsing: %s'%(line))
                except ValueError:
                    print('could not parse %s'%(line))
            elif line.startswith('ALERT: ANOMALOUS TRANSITION: new transition'):
                matchobj = newTransRegex.match(line)
                if matchobj:
                    (src, dest) = matchobj.groups()
                    src = src.strip()
                    dest = dest.strip()
                    output[currentModel][currentTimeStamp].append([1, (src, dest)])
                else:
                    print('Error parsing: %s'%(line))
            elif line.startswith('ALERT: ANOMALOUS TRANSITION:'):
                try:
                    matchobj = anomTransRegex.match(line)
                    if matchobj:
                        (transProb, src, dest) = matchobj.groups()
                        src = src.strip()
                        dest = dest.strip()
                        output[currentModel][currentTimeStamp].append([transProb, (src, dest)])
                    else:
                        print('Error parsing: %s'%(line))
                except ValueError:
                    print('could not parse %s'%(line))
            else:
                print('Error, invalid input file')
        else:
            print('could not parse: %s'%(line))

with open('data.csv', 'w', newline='') as f:
    # newline='' is correct; see documentation of csv:
    # https://docs.python.org/3/library/csv.html#id3
    writer = csv.writer(f, delimiter=',', quotechar='"')
    for model in output:
        for timeStamp in output[model]:
            for anomaly in output[model][timeStamp]:
                #note: this writes transitions as a pair/tuple in one field.
                writer.writerow([timeStamp, model, anomaly[0], anomaly[1]])
