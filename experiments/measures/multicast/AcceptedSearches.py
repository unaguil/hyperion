from measures.generic.GenericMeasure import GenericMeasure
import measures.generic.Units as Units

class AcceptedSearches(GenericMeasure):
	"""Total number of accepted searches"""
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r'DEBUG multicast.search.ParameterSearchImpl  - Peer [0-9]+ accepted multicast.search.message.SearchMessage.*?([0-9]+\,[0-9]+).*?', period, simulationTime, Units.MESSAGES)
		
	def parseLine(self, line):
		self.parseInc(line)
		
