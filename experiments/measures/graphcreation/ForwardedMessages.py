from measures.generic.GenericMeasure import GenericMeasure

import measures.generic.Units as Units
 
class ForwardedMessages(GenericMeasure):
	def __init__(self, messageClass, period, simulationTime):
		self.__messageClass = messageClass
		
		GenericMeasure.__init__(self, r"DEBUG .*?  - Peer .*? forwarding " + self.__messageClass + " ([0-9]+\,[0-9]+).*?", period, simulationTime, Units.MESSAGES)
			
	def parseLine(self, line):
		self.parseInc(line)
		
