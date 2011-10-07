from measures.generic.SentXXXMessages import SentXXXMessages

class SentBundleMessages(SentXXXMessages):
	"""Total number of sent bundle messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'message.BundleMessage', period, simulationTime)