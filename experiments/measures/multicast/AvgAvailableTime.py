import re
import numpy

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgAvailableTime:
	"""Average time parameters are available. Currently taxonomy is not supported"""
	
	def __init__(self, period, simulationTime):		
		self.__periodicAvgValues = PeriodicAvgValues(period, simulationTime)
		
		self.__foundPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) found parameters (\[.*?\]) in node ([0-9]+) searchID \((S:[0-9]+ ID:[0-9]+)\) ([0-9]+\,[0-9]+).*?')
		self.__lostPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) lost parameters {(.*?)} in node [0-9]+ ([0-9]+\,[0-9]+).*?')
		
		self.__foundParameters = {}
		
		self.__defaultValuesAdded = False;
		
	def getType(self):
		return self.__class__.__name__
	
	def getPeriod(self):
		return self.__periodicAvgValues.getPeriod()
	
	def getSimulationTime(self):	
		return self.__periodicAvgValues.getSimulationTime()
	
	def getTotalValue(self):
		self.__addDefaultValues()
		return self.__periodicAvgValues.getAvgTotal(False)
	
	def __getParameters(self, str):
		return [str.strip() for str in str[1:-1].split(',')]

	def parseLine(self, line):
		m = self.__foundPattern.match(line)
		if m is not None:
			peer = m.group(1)
			parameters = self.__getParameters(m.group(2))
			remotePeer = m.group(3)
			searchID = m.group(4)
			time = float(m.group(5).replace(',','.'))
			
			if not peer in self.__foundParameters:
				self.__foundParameters[peer] = {}
				
			if not searchID in self.__foundParameters[peer]:
				self.__foundParameters[peer][searchID] = {}
				
			if not remotePeer in self.__foundParameters[peer][searchID]:
				self.__foundParameters[peer][searchID][remotePeer] = {}
							
			for parameter in parameters:
				self.__foundParameters[peer][searchID][remotePeer][parameter] = time
				
			return
		
		m = self.__lostPattern.match(line)
		if m is not None:
			peer = m.group(1)
			lostParams = self.__getLostParameters(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			lostParameters = self.__getLostParameters(lostParams)
			
			for searchID, parameters in lostParameters:
				foundTime = self.__foundParameters[peer][searchID][remotePeer][parameter]
				elapsedTime = time - foundTime
				self.__periodicAvgValues.addValue(elapsedTime, foundTime) 
				del self.__foundParameters[peer][searchID][remotePeer][parameter]
			
			return
		
	def __getLostParameters(self, str):
		lostParameters = []
		strings = str.split(', (')
		for s in strings:
			searchID, values = s.split('=')
			searchID = ''.join([c for c in searchID if c not in ('(', ')')]) 
			lostParameters.add((searchID, self.__getParameters(values)))
		return lostParameters
		
	def __calculateAvgFoundParametersRatio(self, foundParameters):
		ratios = [] 		 
		for parameter in foundParameters.keys():
			found = foundParameters[parameter]
			ratio = self.__currentParameters[parameter] / found
			ratios.append(ratio)
			
		return numpy.mean(ratios)
			
	def __checkParameter(self, parameter, list):
		if not parameter in list:
			list[parameter] = 0
			
	def __addDefaultValues(self):
		if not self.__defaultValuesAdded:
			for peer in self.__foundParameters:
				for searchID in self.__foundParameters[peer]:
					for remotePeer in self.__foundParameters[peer][searchID]:
						for parameter in self.__foundParameters[peer][searchID][remotePeer]:
							foundTime = self.__foundParameters[peer][searchID][remotePeer][parameter]
							elapsedTime = self.getSimulationTime() - foundTime
							self.__periodicAvgValues.addValue(elapsedTime, foundTime)
							
			self.__defaultValuesAdded = True
				
	def getValues(self): 
		self.__addDefaultValues()
		return self.__periodicAvgValues.getPeriodicValues(False) 
	
	def getUnits(self):
		return Units.MILLIS