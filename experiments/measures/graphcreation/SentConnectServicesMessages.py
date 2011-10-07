from measures.generic.SentXXXMessages import SentXXXMessages

class SentConnectServicesMessages(SentXXXMessages):
	"""Total number of sent connect services messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'graphcreation.collisionbased.message.ConnectServicesMessage', period, simulationTime)
		
