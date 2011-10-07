import re
import numpy

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgFoundParameters:
	"""Average found parameters. Currently taxonomy is not supported"""
	
	def __init__(self, period, simulationTime):		
		self.__periodicAvgValues = PeriodicAvgValues(period, simulationTime)
		
		self.__searchPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) started search for parameters (\[.*?\]).*?([0-9]+\,[0-9]+).*?')
		self.__foundPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) found parameters (\[.*?\]) in node [0-9]+.*?([0-9]+\,[0-9]+).*?')
		self.__lostPattern = re.compile('DEBUG multicast.search.ParameterSearchImpl  - Peer ([0-9]+) lost parameters {(.*?)} in node [0-9]+ ([0-9]+\,[0-9]+).*?')
		
		self.__parametersAdded = re.compile('DEBUG dissemination.newProtocol.ptable.ParameterTable  - Peer [0-9]+ added local parameters: (\[.*?\]) ([0-9]+\,[0-9]+).*?')
		self.__parametersRemoved = re.compile('DEBUG dissemination.newProtocol.ptable.ParameterTable  - Peer [0-9]+ removed local parameters: (\[.*?\]) ([0-9]+\,[0-9]+).*?')
		
		self.__currentParameters = {}
		
		self.__currentSearches = {}
		
	def getType(self):
		return self.__class__.__name__
	
	def getPeriod(self):
		return self.__periodicAvgValues.getPeriod()
	
	def getSimulationTime(self):
		return self.__periodicAvgValues.getSimulationTime()
	
	def getTotalValue(self):
		return self.__periodicAvgValues.getAvgTotal()
	
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
						
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundParameters(), time)
						
			return 
		
		m = self.__parametersRemoved.match(line) 
		if m is not None:
			parameters = self.__getParameters(m.group(1))
			time = float(m.group(2).replace(',','.'))
			
			for parameter in parameters:
				self.__checkParameter(parameter, self.__currentParameters)
				self.__currentParameters[parameter].remove(parameter)
				
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundParameters(), time)
				
			return
		
		m = self.__searchPattern.match(line)
		if m is not None:
			peer = m.group(1)
			parameters = self.__getParameters(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			if not peer in self.__currentSearches:
				self.__currentSearches[peer] = {}
				
			for parameter in parameters:
				if not parameter in self.__currentSearches[peer]:
					self.__currentSearches[peer][parameter] = 0
			
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundParameters(), time)
				
			return
		
		m = self.__foundPattern.match(line)
		if m is not None:
			peer = m.group(1)
			parameters = self.__getParameters(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			for parameter in parameters:
				self.__currentSearches[peer][parameter] += 1
								
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundParameters(), time)
			
			return 
		
		m = self.__lostPattern.match(line)
		if m is not None:
			peer = m.group(1)
			parameters = self.__getLostParameters(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			for parameter in parameters:
				self.__currentSearches[peer][parameter] -= 1
				
			self.__periodicAvgValues.addValue(self.__calculateAvgFoundParameters(), time)
			
			return 
		
	def __getLostParameters(self, str):
		parameters = []
		strings = str.split(', (')
		for s in strings:
			values = s.split('=')[1] 
			parameters.extend(self.__getParameters(values))
		return parameters
				
	def __calculateAvgFoundParameters(self):			
		avgRatios = []

		for peer in self.__currentSearches.keys():
			ratios = []
			for parameter in self.__currentSearches[peer].keys():
				if not parameter in self.__currentParameters:
					ratios.append(0.0)
				else:
					availables = self.__currentParameters[parameter]	
					found = self.__currentSearches[peer][parameter]
					ratios.append(found / float(availables))
					
			if len(ratios) == 0:
				avgRatios.append(0.0)
			else:
				avgRatios.append(numpy.mean(ratios))
			
		if len(avgRatios) == 0:
			return 0.0
		else:
			return numpy.mean(avgRatios)		
			
	def __checkParameter(self, parameter, list):
		if not parameter in list:
			list[parameter] = 0 
	
	def getValues(self): 
		return self.__periodicAvgValues.getPeriodicValues() 
	
	def getUnits(self):
		return Units.PARAMETERS