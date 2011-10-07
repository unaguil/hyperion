from measures.generic.GenericMeasure import GenericMeasure
import measures.generic.Units as Units

class StartedCompositions(GenericMeasure):
	"""Total number of started searches"""
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r'DEBUG graphsearch.peer.Peer  - Peer [0-9]+ started composition search \(.*?\).*?([0-9]+\,[0-9]+).*?', period, simulationTime, Units.COMPOSITIONS)
			
	def parseLine(self, line):
		self.parseInc(line)
		
