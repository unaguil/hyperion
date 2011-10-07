from measures.generic.SentXXXMessages import SentXXXMessages

class SentDisconnectServicesMessages(SentXXXMessages):
	"""Total number of sent disconnect services messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'graphcreation.collisionbased.message.DisconnectServicesMessage', period, simulationTime)
		
