from measures.generic.SentXXXMessages import SentXXXMessages

class SentSearchResponseMessages(SentXXXMessages):
	"""Total number of sent search response messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'multicast.search.message.SearchResponseMessage', period, simulationTime)
		
