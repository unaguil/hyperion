#!/usr/bin/python

import subprocess
import os
import sys

nodes = 70
finishTime = 100.0
gridW = 265
gridH = 1058

minSpeed = 0.001
maxSpeed = 5.0
pauseTime = 10.0

command = './setdest'
parameters = '-v 2 -n %s -s 1 -m %s -M %s -t %s -P 1 -p %s -x %s -y %s' % (nodes, minSpeed, maxSpeed, finishTime, pauseTime, gridW, gridH)

paramList = [command]
                
for e in parameters.split('-'):
    paramList.append('-' + e)


running = []

scenarios = [0,1,2,3]

print 'Generating scenarios ', scenarios
print '\tNodes:', nodes
print '\tFinishTime:', finishTime
print '\tWidth', gridW
print '\tHeight', gridH
print '\tMinSpeed', minSpeed
print '\tMaxSpeed', maxSpeed
print '\tPauseTime', pauseTime

for i in scenarios:
    file = 'mobility-%s-%s-%s-%s-%s-%s-%s-%d.txt' % (str(nodes), str(finishTime), str(gridW), str(gridH), str(minSpeed), str(maxSpeed), str(pauseTime), i)
    outputFile = open(file, 'w') 
    print 'Output file %s' % file
    sys.stdout.flush()

    p = subprocess.Popen(paramList, stdout=outputFile)
    
    running.append((p, outputFile))
    

for num, (p, outputFile) in enumerate(running):
    result = p.wait()
    outputFile.close()
        
    if result != 0:
        print "ERROR: There was a problem during mobility scenario %d generation" % (num + 1)
    else:
        print 'Scenario %d correctly generated' % (num + 1)
        
    sys.stdout.flush()

