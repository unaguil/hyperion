from measures.generic.ReceivedXXXMessages import ReceivedXXXMessages
 
class ReceivedTableMessages(ReceivedXXXMessages):
	"""Total number of received table messages"""
	
	def __init__(self, period, simulationTime):
		ReceivedXXXMessages.__init__(self, 'dissemination.newProtocol.message.TableMessage', period, simulationTime)
	
		
	