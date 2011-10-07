import re
import numpy

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgCompositionSearchTime:
	"""Average found composition time"""
	
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
		return self.__periodicAvgValues.getAvgTotal(False)
	
	def parseLine(self, line):
		m = self.__searchPattern.match(line)
		if m is not None:
			searchID = m.group(1)
			time = float(m.group(2).replace(',','.'))
			
			self.__searches[searchID] = time
				
			return
		
		m = self.__foundPattern.match(line)
		if m is not None:
			searchID = m.group(1)
			time = float(m.group(2).replace(',','.'))
			
			if searchID in self.__searches:
				searchTime = self.__searches[searchID]
				elapsedTime = time - searchTime
				self.__periodicAvgValues.addValue(elapsedTime, searchTime) 
				del self.__searches[searchID]
				
			return
		
	def getValues(self): 
		return self.__periodicAvgValues.getPeriodicValues(False) 
	
	def getUnits(self):
		return Units.SECONDS