from GenericMeasure import GenericMeasure

import Units

class ReceivedXXXMessages(GenericMeasure):
	def __init__(self, messageClass, period, simulationTime):	
		GenericMeasure.__init__(self, r"DEBUG .*?  - Peer .*? received " + messageClass + ".*?([0-9]+\,[0-9]+).*?", period, simulationTime, Units.MESSAGES)
	
	def parseLine(self, line):
		self.parseInc(line)
					