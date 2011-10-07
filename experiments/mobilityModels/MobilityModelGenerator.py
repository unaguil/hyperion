from RandomWaypoint import *
from LoadMobilityModel import *

class MobilityModelGenerator:
    
    def generateModel(self, entries):
        self.__mobilityModel = ''
        
        for entry in entries:
            value = entry.firstChild.data
            key = entry.getAttribute("key")
            
            if key == "mobilityModel":
                self.__mobilityModel = value
                
        if self.__mobilityModel == 'RandomWaypoint':
            return RandomWaypoint(entries)
        if self.__mobilityModel == 'LoadMobilityModel':
            return LoadMobilityModel(entries)
        return None