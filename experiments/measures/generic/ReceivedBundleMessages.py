from measures.generic.ReceivedXXXMessages import ReceivedXXXMessages

class ReceivedBundleMessages(ReceivedXXXMessages):
	"""Total number of received bundle messages"""
	
	def __init__(self, period, simulationTime):
		ReceivedXXXMessages.__init__(self, 'message.BundleMessage', period, simulationTime)
	