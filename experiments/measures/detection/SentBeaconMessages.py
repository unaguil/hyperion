from measures.generic.SentXXXMessages import SentXXXMessages

class SentBeaconMessages(SentXXXMessages):
	"""Total number of sent beacon messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'detection.message.BeaconMessage', period, simulationTime)