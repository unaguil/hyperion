from measures.graphcreation.ForwardedMessages import ForwardedMessages

class SentFCompositionMessages(ForwardedMessages):
	"""Total number of sent collision messages"""
	
	def __init__(self, period, simulationTime):
		ForwardedMessages.__init__(self, 'graphsearch.forward.message.FCompositionMessage', period, simulationTime)
		
