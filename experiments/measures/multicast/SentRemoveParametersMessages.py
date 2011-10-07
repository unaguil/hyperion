from measures.generic.SentXXXMessages import SentXXXMessages

class SentRemoveParametersMessages(SentXXXMessages):
	"""Total number of sent remove parameters messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'multicast.search.message.RemoveParametersMessage', period, simulationTime)
		
