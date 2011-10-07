from measures.generic.ReceivedXXXMessages import ReceivedXXXMessages

class ReceivedACKMessages(ReceivedXXXMessages):
	"""Total number of received ACK messages"""
	
	def __init__(self, period, simulationTime):
		ReceivedXXXMessages.__init__(self, 'peer.message.ACKMessage', period, simulationTime)
	