from measures.generic.SentXXXMessages import SentXXXMessages

class SentSearchMessages(SentXXXMessages):
	"""Total number of sent search messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'multicast.search.message.SearchMessage', period, simulationTime)
		
