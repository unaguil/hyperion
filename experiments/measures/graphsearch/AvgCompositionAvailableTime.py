import re
import numpy

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgCompositionAvailableTime:
	"""Average time compositions are available"""
	
	def __init__(self, period, simulationTime):		
		self.__periodicAvgValues = PeriodicAvgValues(period, simulationTime)
		
		self.__foundPattern = re.compile('DEBUG graphsearch.peer.Peer  - Peer [0-9]+ received composition for search (\(.*?\)).*?([0-9]+\,[0-9]+).*?')
		self.__lostPattern = re.compile('DEBUG graphsearch.peer.Peer  - Peer [0-9]+ received invalid composition for search (\(.*?\)).*?([0-9]+\,[0-9]+).*?')
		
		self.__searches = {}
		
		self.__defaultValuesAdded = False
		
	def getType(self):
		return self.__class__.__name__
	
	def getPeriod(self):
		return self.__periodicAvgValues.getPeriod()
	
	def getSimulationTime(self):	
		return self.__periodicAvgValues.getSimulationTime()
	
	def getTotalValue(self):
		self.__addDefaultValues()
		return self.__periodicAvgValues.getAvgTotal(False)
	
	def parseLine(self, line):
		m = self.__foundPattern.match(line)
		if m is not None:
			searchID = m.group(1)
			time = float(m.group(2).replace(',','.'))
			
			if not searchID in self.__searches: 
				self.__searches[searchID] = time
		
			return
		
		m = self.__lostPattern.match(line)
		if m is not None:
			searchID = m.group(1)
			time = float(m.group(2).replace(',','.'))			
			
			if searchID in self.__searches:
				foundTime = self.__searches[searchID]
				elapsedTime = time - foundTime
				self.__periodicAvgValues.addValue(elapsedTime, foundTime) 
				del self.__searches[searchID]
			
			return
		
	def __addDefaultValues(self):
		if not self.__defaultValuesAdded:
			for searchID in self.__searches.keys():
				foundTime = self.__searches[searchID]
				elapsedTime = self.getSimulationTime() - foundTime
				self.__periodicAvgValues.addValue(elapsedTime, foundTime)
							
			self.__defaultValuesAdded = True
				
	def getValues(self): 
		self.__addDefaultValues()
		return self.__periodicAvgValues.getPeriodicValues(False) 
	
	def getUnits(self):
		return Units.SECONDS