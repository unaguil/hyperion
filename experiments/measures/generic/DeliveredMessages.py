from GenericMeasure import GenericMeasure
import Units

class DeliveredMessages(GenericMeasure):
	"""Total number of messages which were correctly delivered using reliable broadcast functionality"""
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r"DEBUG peer.ReliableBroadcast  - Peer [0-9]+ delivered message.*? ([0-9]+\,[0-9]+).*?", period, simulationTime, Units.MESSAGES)
		
	def parseLine(self, line):
		self.parseInc(line)
		
		
