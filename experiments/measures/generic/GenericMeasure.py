import re
from measures.periodicValues.PeriodicValues import *

class GenericMeasure:
    def __init__(self, pattern, period, simulationTime, units):        
        self.__periodicValues = PeriodicValues(0, period, simulationTime)
        
        self.__prog = re.compile(pattern)
        
        self.__units = units
        
    def getType(self):
        return self.__class__.__name__
    
    def getPeriod(self):
        return self.__periodicValues.getPeriod()
    
    def getSimulationTime(self):
        return self.__periodicValues.getSimulationTime()
    
    def getTotalValue(self):
        return self.__periodicValues.getTotal()
    
    def incValue(self, time, simulationTime):
        self.__periodicValues.incValue(time, self.getPeriod(), simulationTime)
        
    def parseInc(self, line):
        m = self.__prog.match(line)
        if m is not None:
            time  = float(m.group(1).replace(',','.'))
            self.incValue(time, self.getSimulationTime())
    
    def getValues(self): 
        return self.__periodicValues
    
    def getUnits(self):
        return self.__units