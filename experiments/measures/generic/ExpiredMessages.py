from GenericMeasure import GenericMeasure

import Units

class ExpiredMessages(GenericMeasure):
	"""Total number of messages whose broadcasting expired when using the reliable broadcast functionality"""
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r"DEBUG peer.ReliableBroadcast  - Peer [0-9]+ failed reliable broadcast .*? ([0-9]+\,[0-9]+).*?", period, simulationTime, Units.MESSAGES)
	
	def parseLine(self, line):
		self.parseInc(line)
		
	
