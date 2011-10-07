from measures.generic.GenericMeasure import GenericMeasure
import measures.generic.Units as Units

class StartedSearches(GenericMeasure):
	"""Total number of started searches"""
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r'DEBUG multicast.search.ParameterSearchImpl  - Peer [0-9]+ started search for parameters \[.*?\].*?([0-9]+\,[0-9]+).*?', period, simulationTime, Units.MESSAGES)
			
	def parseLine(self, line):
		self.parseInc(line)
		
