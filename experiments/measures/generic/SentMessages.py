from GenericMeasure import GenericMeasure

import Units

class SentMessages(GenericMeasure):
	""" Total number of sent messages of any type """
	
	def __init__(self, period, simulationTime):
		GenericMeasure.__init__(self, r"DEBUG peer.PeerAgentJ  - Peer [0-9]+ sending.*?([0-9]+\,[0-9]+).*?", period, simulationTime, Units.MESSAGES)
			
	def parseLine(self, line):
		self.parseInc(line)
