from measures.generic.GenericMeasure import GenericMeasure
import measures.generic.Units as Units

class AcceptedSearchResponses(GenericMeasure):
	"""Total number of accepted search responses"""
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r'DEBUG multicast.search.ParameterSearchImpl  - Peer [0-9]+ found parameters \[.*?\] in node [0-9]+.*?([0-9]+\,[0-9]+).*?', period, simulationTime, Units.MESSAGES)
			
	def parseLine(self, line):
		self.parseInc(line)
		
