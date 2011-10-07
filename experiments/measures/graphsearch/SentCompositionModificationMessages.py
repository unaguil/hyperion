from measures.graphcreation.ForwardedMessages import ForwardedMessages

class SentCompositionModificationMessages(ForwardedMessages):
	"""Total number of sent composition modification messages"""
	
	def __init__(self, period, simulationTime):
		ForwardedMessages.__init__(self, 'graphsearch.forward.message.CompositionModificationMessage', period, simulationTime)
		
