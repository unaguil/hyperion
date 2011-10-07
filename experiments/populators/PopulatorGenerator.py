from ParameterPopulator import *
from CompositionsPopulator import *

class PopulatorGenerator:
    
    def generatePopulator(self, entries):
        
        self.__populator = ''
        
        for entry in entries:
           value = entry.firstChild.data
           key = entry.getAttribute('key')
           
           if key == 'populator':
               self.__populator = value
                       
        if self.__populator == 'ParameterPopulator':
            return ParameterPopulator(entries)
        if self.__populator == 'CompositionsPopulator':
            return CompositionsPopulator(entries)
        
        return None