from measures.generic.SentXXXMessages import SentXXXMessages

class SentCollisionMessages(SentXXXMessages):
	"""Total number of sent collision messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'graphcreation.collisionbased.message.CollisionMessage', period, simulationTime)
		
