from measures.generic.SentXXXMessages import SentXXXMessages

class SentACKMessages(SentXXXMessages):
	"""Total number of sent ACK messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'peer.message.ACKMessage', period, simulationTime)