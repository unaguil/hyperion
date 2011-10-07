from measures.generic.ReceivedXXXMessages import ReceivedXXXMessages

class ReceivedBeaconMessages(ReceivedXXXMessages):
	"""Total number of received beacon messages"""
	
	def __init__(self, period, simulationTime):
		ReceivedXXXMessages.__init__(self, 'detection.message.BeaconMessage', period, simulationTime)
	