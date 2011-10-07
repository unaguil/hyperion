from measures.generic.SentXXXMessages import SentXXXMessages

class SentRemoveRouteMessages(SentXXXMessages):
	"""Total number of sent remove routes messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'multicast.search.message.RemoveRouteMessage', period, simulationTime)
		
