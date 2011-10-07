from GenericMeasure import GenericMeasure

import Units

class SentXXXMessages(GenericMeasure):
	def __init__(self, messageClass, period, simulationTime):
		self.__messageClass = messageClass
		
		GenericMeasure.__init__(self, r"DEBUG .*?  - Peer .*? sending " + self.__messageClass + ".*? ([0-9]+\,[0-9]+).*?", period, simulationTime, Units.MESSAGES)
			
	def parseLine(self, line):
		self.parseInc(line)
		
