from measures.generic.SentXXXMessages import SentXXXMessages

class SentForwardMessages(SentXXXMessages):
	"""Total number of sent forward messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'graphcreation.collisionbased.message.ForwardMessage', period, simulationTime)
		
