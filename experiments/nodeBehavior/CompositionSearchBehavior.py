import random

from CommonSearchBehavior import CommonSearchBehavior

class CompositionSearchBehavior(CommonSearchBehavior):
    """
        Creates the behavior of the nodes for multicast experimentation.
    """
    def __init__(self, entries, nodePopulator):
        CommonSearchBehavior.__init__(self, entries, nodePopulator, 'Composition search behavior')
        
        self.__searchedParameters = {}
        
    def perform(self, time, oFile):
        node = random.randrange(self.getNNodes())
        compositions = self.getNodePopulator().getCompositions()
        if len(compositions) > 0:
            compositionNum = random.randrange(len(compositions))
            oFile.write('$ns_ at %f "$agents(%d) agentj composeService %d\"\n' % (time, node, compositionNum))
        
    def printInfo(self):
        print '* Using a total of %d different compositions' % len(self.getNodePopulator().getCompositions())        