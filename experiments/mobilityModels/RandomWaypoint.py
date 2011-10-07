import sys
import subprocess
from time import time

import util.TimeFormatter as TimeFormatter

class RandomWaypoint:
    
    def __init__(self, entries):
        for entry in entries:
            value = entry.firstChild.data
            key = entry.getAttribute("key")
            
            if key == "nNodes":
                self.__nNodes = value
            if key == "finishTime":
                self.__simTime = value
            if key == "gridW":
                self.__maxX = value
            if key == "gridH":
                self.__maxY = value
            if key == "minSpeed":
                self.__minSpeed = value
            if key == "maxSpeed":
                self.__maxSpeed = value
            if key == "pauseTime":
                self.__pauseTime = value
        
    def generate(self, workingDir, file, repeat):
        command = 'setdest'
        parameters = '-v 2 -n %s -s 1 -m %s -M %s -t %s -P 1 -p %s -x %s -y %s' % (self.__nNodes, self.__minSpeed, self.__maxSpeed, self.__simTime, self.__pauseTime, self.__maxX, self.__maxY)
        
        cmd = command + ' ' + parameters
        
        print ''
        print '************* Random waypoint ****************'
        print '* Nodes: %s' % self.__nNodes
        print '* Minimum speed: %s m/s' % self.__minSpeed
        print '* Maximum speed: %s m/s' % self.__maxSpeed
        print '* Simulation time: %s s' % self.__simTime
        print '* Pause time: %s s' % self.__pauseTime
        print '* Max X: %s m' % self.__maxX
        print '* Max Y: %s m' % self.__maxY
        
        print '* Generated using external command: %s' % cmd
        
        paramList = [command]
                
        for e in parameters.split('-'):
            paramList.append('-' + e)
        
        startTime = time()
        
        oFilePath = workingDir + '/' + file
        outputFile = open(oFilePath, 'w')
        p = subprocess.Popen(paramList, stdout=outputFile)
        result = p.wait()
        outputFile.close()
        
        if result != 0:
            print "ERROR: There was a problem during mobility model generation"
            sys.exit()

        print '* Model generation time: %s' % TimeFormatter.formatTime(time() - startTime)
        
        print '**********************************************'
        
        return file 