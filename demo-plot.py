#!/usr/bin/python3

import subprocess
import matplotlib.pyplot as plt
import sys
import time

if len(sys.argv) < 2 or "\"" in sys.argv[1]:
    print("Usage: demo-plot.py <filename>")
    print("filename may not contain quotes (\")!")
    sys.exit(1)

#note: this could be a security risk, sys.argv[1] is not checked...
cmd = subprocess.Popen('java -jar bin/libs/AutoDetect-0.1.jar "'+sys.argv[1] +'" "./testdata/GPS-0.csv"', shell=True, stdout=subprocess.PIPE)

#fig = plt.figure()

plt.ion()
fig = plt.figure()

x = []
data1 = []
data2 = []
data3 = []
ln1, ln2, ln3 = plt.plot(x, data1, 'g^', x, data2, 'r*', x, data3, 'b+')
plt.xlim([0,45])
plt.ylim([0,1])
plt.xlabel("time (s)")
plt.ylabel("evaluation")
plt.pause(0.1)
for line in cmd.stdout:
    line = line.decode(sys.stdout.encoding)
    line = line.strip()
    if "INFO" in line: #ignore info level debug output
        continue
    t, id, fusion, ART, MGT = line.split(',')
    if not "19" in id or line[-1] == ',':
        continue
    print(line)
    x.append(float(t))
    data1.append(float(fusion))
    data2.append(float(ART))
    data3.append(float(MGT))
    plt.plot(x, data1, 'g^', x, data2, 'r*', x, data3, 'b+')
    plt.pause(0.01)
    time.sleep(0.01)
