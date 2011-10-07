import re
import numpy

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgFoundCompositions:
	"""Average found compositions"""
	
	def __init__(self, period, simulationTime):		
		self.__periodicAvgValues = PeriodicAvgValues(period, simulationTime)
		
		self.__searchPattern = re.compile('DEBUG graphsearch.peer.Peer  - Peer [0-9]+ started composition search (\(.*?\)).*?([0-9]+\,[0-9]+).*?')
		self.__foundPattern = re.compile('DEBUG graphsearch.peer.Peer  - Peer [0-9]+ received composition for search (\(.*?\)).*?([0-9]+\,[0-9]+).*?')
		
		self.__searches = {}
		
	def getType(self):
		return self.__class__.__name__
	
	def getPeriod(self):
		return self.__periodicAvgValues.getPeriod()
	
	def getSimulationTime(self):
		return self.__periodicAvgValues.getSimulationTime()
	
	def getTotalValue(self):
		return self.__periodicAvgValues.getAvgTotal()

	def parseLine(self, line):
		m = self.__searchPattern.match(line)
		if m is not None:
			searchID = m.group(1)
			time = float(m.group(2).replace(',','.'))
			
			self.__searches[searchID] = False
			
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundCompositions(), time)
				
			return
		
		m = self.__foundPattern.match(line)
		if m is not None:
			searchID = m.group(1)
			time = float(m.group(2).replace(',','.'))
			
			self.__searches[searchID] = True
								
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundCompositions(), time)
			
			return 
				
	def __calculateAvgFoundCompositions(self):			
		if len(self.__searches) == 0:
			return 0.0
		
		found = 0
		
		for searchID in self.__searches.keys():
			if self.__searches[searchID]:
				found +=1
				
		return found / len(self.__searches)		
	
	def getValues(self): 
		return self.__periodicAvgValues.getPeriodicValues() 
	
	def getUnits(self):
		return Units.COMPOSITIONS