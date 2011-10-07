import Util

class PeriodicValues:
    def __init__(self, default, period, simulationTime):
        self.__period = period
        self.__simulationTime = simulationTime
        
        periods = Util.getPeriods(period, simulationTime) 
        
        self.__values = []
        
        for index in xrange(periods):
            self.__values.append(default)
        
    def getValue(self, index):
        return self.__values[index]
        
    def setValue(self, index, value):
        self.__values[index] = value 
        
    def getValues(self):
        return self.__values
                            
    def incValue(self, time, period, simulationTime):
        index = Util.getPeriod(time, period, simulationTime)
        if index is not None:
            self.__values[index] += 1
            
    def __len__(self):
        return len(self.__values) 
    
    def getTotal(self):
        return sum(self.__values)
    
    def getPeriod(self):
        return self.__period
    
    def getSimulationTime(self):
        return self.__simulationTime;