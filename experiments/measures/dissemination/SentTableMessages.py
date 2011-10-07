from measures.generic.SentXXXMessages import SentXXXMessages

class SentTableMessages(SentXXXMessages):
	"""Total number of sent table messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'dissemination.newProtocol.message.TableMessage', period, simulationTime)
		
