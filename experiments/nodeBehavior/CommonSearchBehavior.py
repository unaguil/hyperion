import math

from time import time

import util.TimeFormatter as TimeFormatter

class CommonSearchBehavior:
    """
        Defines the common behavior for periodic actions generation.
    """
    
    def __init__(self, entries, nodePopulator, behaviorName):
        for entry in entries:
            value = entry.firstChild.data
            key = entry.getAttribute("key")
            
            if key == "searchFreq":
                self.__searchFreq = float(value)
                
            if key == "finishTime":
                self.__finishTime = float(value)
                
            if key == "nNodes":
                self.__nNodes = int(value)
                
            if key == 'timeRange':
                self.__timeRange = eval(value)
                    
        self.__nodePopulator = nodePopulator
        
        self.__behaviorName = behaviorName
        
        if self.__searchFreq == 0:
            self.__searchPeriod = 0.0
        else:
            self.__searchPeriod = 1 / self.__searchFreq
            
    def getNNodes(self):
        return self.__nNodes
    
    def getNodePopulator(self):
        return self.__nodePopulator
    
    def getBehaviorName(self):
        return self.__behaviorName

    def __getTimeRange(self):
        init, end = self.__timeRange
        
        if init == 'START':
            init = 0.5
            
        if end == 'END' or end >= self.__finishTime:
            end = self.__finishTime
            
        return (init, end)
    
    def generate(self, workingDir, oFile):             
        
        init, end = self.__getTimeRange()
        
        print ''   
        print '************* %s ****************' % self.__behaviorName
        print '* Nodes: %d' % self.__nNodes
        print '* Frequency: %.3f searches/s' % self.__searchFreq
        print '* Generated: %d searches' % math.ceil(self.__searchFreq * (self.__getTimeRange()[1] - self.__getTimeRange()[0]))
        print '* Time range: [%s, %s] s' % (self.__getTimeRange()[0], self.__getTimeRange()[1])
        self.printInfo()

        startTime = time()
        
        if self.__searchPeriod > 0.0:            
            #Create search events each specified period of time during specified time range        
            currentTime = init
            while (currentTime < end):
                #perform action
                self.perform(currentTime, oFile)
                
                #Increment time
                currentTime += self.__searchPeriod
                
        print '* Behavior generation time: %s ' % TimeFormatter.formatTime(time() - startTime) 
        print '**********************************************************'        
                
    def perform(self, time, oFile):
        pass 
    
    def printInfo(self):
        pass
