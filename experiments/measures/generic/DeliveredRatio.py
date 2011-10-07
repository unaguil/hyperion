from GenericMeasure import GenericMeasure
from ReliableBroadcastedMessages import ReliableBroadcastedMessages
from DeliveredMessages import DeliveredMessages

from measures.periodicValues.PeriodicValues import *

import Units

class DeliveredRatio(GenericMeasure):
	"""The ratio of messages which were correctly delivered calculated using the sum of delivered and expired messages as the total number of messages"""	
	
	def __init__(self, period, simulationTime):	
		GenericMeasure.__init__(self, '', period, simulationTime, Units.RATIO)
		
		self.__period = period
		self.__simulationTime = simulationTime
		
		self.__broadcastedMessages = ReliableBroadcastedMessages(period, simulationTime)
		self.__deliveredMessages = DeliveredMessages(period, simulationTime) 
	
	def parseLine(self, line):
		self.__broadcastedMessages.parseLine(line)
		self.__deliveredMessages.parseLine(line)
			
	def getTotalValue(self):
		totalBroadcastedMessages = self.__broadcastedMessages.getTotalValue()
		if totalBroadcastedMessages == 0:
			return 0
		else:
			return self.__deliveredMessages.getTotalValue() / float(totalBroadcastedMessages)
		
	def getValues(self):
		values = PeriodicValues(0, self.__period, self.__simulationTime)
		
		broadcastedMessagesArray = self.__broadcastedMessages.getValues()
		deliveredMessagesArray = self.__deliveredMessages.getValues()
		
		for index in xrange(len(broadcastedMessagesArray)):
			broadcasted = broadcastedMessagesArray.getValue(index)
			delivered = deliveredMessagesArray.getValue(index)
			if broadcasted == 0:
				values.setValue(index, 0);
			else:
				values.setValue(index, delivered / float(broadcasted))
			 
		return values