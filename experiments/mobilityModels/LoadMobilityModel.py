import os
import sys

MAX_SCENARIOS = 4

class LoadMobilityModel:
    
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
        
    def generate(self, workingDir, outputFile, repeat):
        numScenario = repeat % MAX_SCENARIOS
        mobilityFile = 'mobility-%s-%s-%s-%s-%s-%s-%s-%d.txt' % (str(self.__nNodes), str(self.__simTime), str(self.__maxX), str(self.__maxY), str(self.__minSpeed), str(self.__maxSpeed), str(self.__pauseTime), numScenario)
        relativePath = 'mobilityScenarios/' + mobilityFile
        print 'Using mobility scenario %s' % relativePath
        absolutePath = os.path.abspath(relativePath)
        if not os.path.exists(absolutePath):
            print 'ERROR: Mobility file %s not found' % relativePath
            sys.exit()
        return absolutePath
