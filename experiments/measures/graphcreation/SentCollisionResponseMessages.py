from measures.generic.SentXXXMessages import SentXXXMessages

class SentCollisionResponseMessages(SentXXXMessages):
	"""Total number of sent collision response messages"""
	
	def __init__(self, period, simulationTime):
		SentXXXMessages.__init__(self, 'graphcreation.collisionbased.message.CollisionResponseMessage', period, simulationTime)
		
