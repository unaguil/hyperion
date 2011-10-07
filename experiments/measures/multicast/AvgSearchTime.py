import re
import numpy

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgSearchTime:
	"""Average found parameters. Currently taxonomy is not supported"""
	
	def __init__(self, period, simulationTime):		
		self.__periodicAvgValues = PeriodicAvgValues(period, simulationTime)
		
		self.__searchPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) started search for parameters (\[.*?\]) searchID \((S:[0-9]+ ID:[0-9]+)\) ([0-9]+\,[0-9]+).*?')
		self.__foundPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) found parameters (\[.*?\]) in node [0-9]+ searchID \((S:[0-9]+ ID:[0-9]+)\) ([0-9]+\,[0-9]+).*?')
		self.__lostPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) lost parameters {(.*?)} in node [0-9]+ ([0-9]+\,[0-9]+).*?')
		
		self.__parametersAdded = re.compile('DEBUG dissemination.newProtocol.ptable.ParameterTable  - Peer [0-9]+ added local parameters: (\[.*?\]) ([0-9]+\,[0-9]+).*?')
		
		self.__currentParameters = {}
		
		self.__currentSearches = {}
		
		self.__expectedRatio = 1.0
		
	def getType(self):
		return self.__class__.__name__
	
	def getPeriod(self):
		return self.__periodicAvgValues.getPeriod()
	
	def getSimulationTime(self):
		return self.__periodicAvgValues.getSimulationTime()
	
	def getTotalValue(self):
		return self.__periodicAvgValues.getAvgTotal(False)
	
	def __getParameters(self, str):
		return [str.strip() for str in str[1:-1].split(',')]

	def parseLine(self, line):
		m = self.__parametersAdded.match(line)
		if m is not None:
			parameters = self.__getParameters(m.group(1))
			time = float(m.group(2).replace(',','.'))
			
			for parameter in parameters:
				self.__checkParameter(parameter, self.__currentParameters)
				self.__currentParameters[parameter] += 1
						
			return 
		
		m = self.__searchPattern.match(line)
		if m is not None:
			peer = m.group(1)
			parameters = self.__getParameters(m.group(2))
			searchID = m.group(3)
			time = float(m.group(4).replace(',','.'))
			
			if not peer in self.__currentSearches:
				self.__currentSearches[peer] = {}
				
			self.__currentSearches[peer][searchID] = (time, {})
				
			for parameter in parameters:
				self.__currentSearches[peer][searchID][1][parameter] = 0
				
			return
		
		m = self.__foundPattern.match(line)
		if m is not None:
			peer = m.group(1)
			parameters = self.__getParameters(m.group(2))
			searchID = m.group(3)
			time = float(m.group(4).replace(',','.'))
			
			if searchID in self.__currentSearches[peer]:				
				for parameter in parameters:
					self.__currentSearches[peer][searchID][1][parameter] += 1
					
				foundRatio = self.__calculateAvgFoundParametersRatio(self.__currentSearches[peer][searchID][1])
				
				if foundRatio >= self.__expectedRatio:
					searchTime = self.__currentSearches[peer][searchID][0]
					elapsedTime = (time - searchTime) * 1000
					self.__periodicAvgValues.addValue(elapsedTime, searchTime) 
					del self.__currentSearches[peer][searchID]
				
			return
		
		m = self.__lostPattern.match(line)
		if m is not None:
			peer = m.group(1)
			lostParams = self.__getLostParameters(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			lostParameters = self.__getLostParameters(lostParams)
			
			for searchID, parameters in lostParameters:
				if searchID in self.__currentSearches[peer]:
					for parameter in parameters:
						self.__currentSearches[peer][searchID][1][parameter] -= 1
			
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
	
	def getValues(self): 
		return self.__periodicAvgValues.getPeriodicValues(False) 
	
	def getUnits(self):
		return Units.MILLIS