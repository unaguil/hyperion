from measures.generic.SentXXXMessages import SentXXXMessages

class SentRemovedServicesMessages(SentXXXMessages):
	"""Total number of sent inhibed messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'graphcreation.collisionbased.message.RemovedServicesMessage', period, simulationTime)
		
