import math
import numpy

from PeriodicValues import PeriodicValues

import Util

class PeriodicAvgValues:
    def __init__(self, period, simulationTime):
        self.__period = period
        self.__simulationTime = simulationTime   
        
        periods = int(math.ceil(simulationTime / period))
        
        self.__values = []
        
        for index in xrange(periods):
            self.__values.append([])
        
    def __getAvgValue(self, index, initialValue, initialTime, finalTime): 
        if len(self.__values[index]) == 0:
            return initialValue, initialValue
        else:
            newValues = []
            newValues.append((initialValue, initialTime))
            newValues.extend(self.__values[index])
            
            values = []
            weights = []            
            for index, (value, time) in enumerate(newValues):
                if index == len(newValues) - 1:
                    duration = finalTime - time
                else: 
                    value, nextTime = newValues[index + 1]
                    duration = nextTime - time
                    
                values.append(value)
                weights.append(duration)    
                                            
            return numpy.average(values, weights=weights), values[-1]
        
    def getPeriodicValues(self, maintainLastValue=True):
        periodicValues = PeriodicValues(0, self.getPeriod(), self.getSimulationTime())
                        
        initialValue = 0.0
        for index, value in enumerate(self.__values):
            initialTime = index * self.__period   
            
            avgValue, newInitialValue = self.__getAvgValue(index, initialValue, initialTime, self.__simulationTime)
            
            if newInitialValue is not None:
                initialValue = newInitialValue
                
            if not maintainLastValue:
                initialValue = 0.0
                
            periodicValues.setValue(index, avgValue)
                    
        return periodicValues  
            
    def addValue(self, value, time):        
        index = Util.getPeriod(time, self.getPeriod(), self.getSimulationTime())
        if index is not None:
            self.__values[index].append((value, time))
            
    def __len__(self):
        return len(self.__values) 
    
    def getAvgTotal(self, maintainLastValue=True):
        if not maintainLastValue:
            values = [v for v in self.getPeriodicValues(maintainLastValue).getValues() if v > 0.0]
        else:
            values = self.getPeriodicValues().getValues()
            
        return numpy.mean(values)
    
    def getPeriod(self):
        return self.__period
    
    def getSimulationTime(self):
        return self.__simulationTime
    