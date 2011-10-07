import re

from measures.periodicValues.PeriodicAvgValues import PeriodicAvgValues

import measures.generic.Units as Units

class AvgNeighbors:
	"""Average neighbors for a node"""
	
	def __init__(self, period, simulationTime):		
		self.__periodicAvgValues = PeriodicAvgValues(period, simulationTime)
		
		self.__appearPattern = re.compile('DEBUG detection.beaconDetector.BeaconDetector  - Peer ([0-9]+) has new neighbors: (\[.*?\]) ([0-9]+\,[0-9]+).*?')
		self.__disappearPattern = re.compile('DEBUG detection.beaconDetector.BeaconDetector  - Peer ([0-9]+) has lost neighbors: (\[.*?\]) ([0-9]+\,[0-9]+).*?')
		
		self.__currentNeighbors = {}
		
	def getType(self):
		return self.__class__.__name__
	
	def getPeriod(self):
		return self.__periodicAvgValues.getPeriod()
	
	def getSimulationTime(self):
		return self.__periodicAvgValues.getSimulationTime()
	
	def getTotalValue(self):
		return self.__periodicAvgValues.getAvgTotal()

	def parseLine(self, line):
		m = self.__appearPattern.match(line)
		if m is not None:
			peer = m.group(1)
			neighbors = eval(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			self.__checkPeer(peer)
			
			self.__currentNeighbors[peer] += neighbors
			
			self.__periodicAvgValues.addValue(len(self.__currentNeighbors[peer]), time)
						
			return 
		
		m = self.__disappearPattern.match(line) 
		if m is not None:
			peer = m.group(1)
			neighbors = eval(m.group(2))
			time = float(m.group(3).replace(',','.'))
			
			self.__checkPeer(peer)
			
			for neighbor in neighbors:
				self.__currentNeighbors[peer].remove(neighbor)
				
			self.__periodicAvgValues.addValue(len(self.__currentNeighbors[peer]), time)
			
	def __checkPeer(self, peer):
		if not peer in self.__currentNeighbors:
			self.__currentNeighbors[peer] = []
	
	def getValues(self): 
		return self.__periodicAvgValues.getPeriodicValues() 
	
	def getUnits(self):
		return Units.NEIGHBORS