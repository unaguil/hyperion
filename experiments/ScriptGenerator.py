from xml.dom.minidom import *
import math

from mobilityModels.MobilityModelGenerator import *
from populators.PopulatorGenerator import *
from nodeBehavior.NodeBehaviorGenerator import *

class ScriptGenerator:
	def __init__(self, config, expConfigFile):
		scriptConfig = parseString(config)
		self.__entries = scriptConfig.getElementsByTagName("entry")
		
		mobilityModel = None
		minSpeed = None
		maxSpeed = None
		pauseTime = None
		
		#Default transmission range (100 meters)
		self.__transmissionRange = 100
		
		for entry in self.__entries:
			value = entry.firstChild.data
			key = entry.getAttribute("key")
			if key == "nNodes":
				self.__nNodes = int(value)
			if key == "finishTime":
				self.__finishTime = float(value)
			if key == "gridW":
				self.__gridW = float(value)
			if key == "gridH":
				self.__gridH = float(value)
			if key == "javaAgent":
				self.__javaAgent = value
			if key == "transmissionRange":
				self.__transmissionRange = float(value)
			if key == "discardTime":
				self.__discardTime = float(value)
	
		#Print scenario parameters
		simulationArea = self.getGridW() * self.getGridH()
		nDensity = self.getNNodes() / simulationArea
		nCoverage = math.pi * self.getTransmissionRange()**2
		
		footprint = (nCoverage / simulationArea) * 100.0
		if footprint > 100.0:
			footprint = 100.0
		 
		maximumPath = math.sqrt(self.getGridW()**2 + self.getGridH()**2)
		networkDiameter = maximumPath / self.getTransmissionRange()
		neighborCount = nCoverage * nDensity
		if neighborCount > self.getNNodes() - 1:
			neighborCount = self.getNNodes() - 1;
		
		print '**************** Scenario parameters **********************'
		print '* Simulation time: %.2f s' % self.getSimulationTime()
		print '* Initial discarded time: %.2f s' % self.getDiscardTime()
		print '* Simulation area: %.2f m x %.2f m = %.2f m^2' % (self.getGridW(), self.getGridH(), simulationArea)
		print '* Number of nodes: %d' % self.getNNodes()
		print '* Node density: %.5f nodes/m^2' % nDensity
		print '* Transmission range: %.2f m' % self.getTransmissionRange() 
		print '* Node coverage: %.2f m^2' % nCoverage
		print '* Footprint: %.2f %%' % footprint
		print '* Maximum path: %.2f m' % maximumPath
		print '* Network diameter: %.2f hops' % networkDiameter
		print '* Neighbor count: %.2f neighbors/node' % neighborCount
		print '***********************************************************'
		
		expConfig = parse(expConfigFile)
		codeTag = expConfig.getElementsByTagName("code")
		if len(codeTag) > 0:
			self.__codeFile = codeTag[0].getAttribute("codeFile")
		else:
			self.__codeFile = ''
		
	def generate(self, fileName, workingDir, repeat):
		#Use mobility model generator to create mobility model using script configuration
		mobilityModelGenerator = MobilityModelGenerator()
		mobilityModel = mobilityModelGenerator.generateModel(self.__entries) 
		
		#Create populator which includes population
		populatorGenerator = PopulatorGenerator()
		nodePopulator = populatorGenerator.generatePopulator(self.__entries)
		
		#Create node behavior generator
		behaviorGenerator = NodeBehaviorGenerator()
		nodeBehavior = behaviorGenerator.generateNodeBehavior(self.__entries, nodePopulator)
		
		if nodePopulator is not None:
			nodePopulator.generate(workingDir)
		
		file = open(workingDir + '/' + fileName, "w")
		file.write("source WCommon.tcl\n")
		file.write("\n")
		file.write("set nNodes " + str(self.__nNodes) + "\n")
		file.write("set finishTime " + str(self.__finishTime) + "\n")
		file.write("set gridW " + str(self.__gridW) + "\n")
		file.write("set gridH " + str(self.__gridH) + "\n")
		file.write("set javaAgent " + self.__javaAgent + "\n")
		file.write("\n")
		file.write("set ns_		[new Simulator]\n")
		file.write("set tracefd     [open simple.tr w]\n")
		file.write("$ns_ trace-all $tracefd\n")
		file.write("proc do_something {agents_ nodes_ god_} {\n")
		file.write("\tglobal ns_ finishTime\n")
		file.write("\tupvar $agents_ agents\n")
		file.write("\tupvar $nodes_ node_\n")
		file.write("\n")
		
		if self.__codeFile is not '':
			#Write code
			codeFile = open(workingDir + '/' + self.__codeFile, 'r')
			code = codeFile.read()
			file.write(code)
		
		if mobilityModel is not None:
			file.write('\n')
			mobilityFilePath = mobilityModel.generate(workingDir, 'mobility-scenario.txt', repeat)
			file.write('source ' + mobilityFilePath + '\n')
			
		if nodeBehavior is not None:
			file.write('\n')
			oFile = open(workingDir + '/nodeBehavior.txt', 'w')
			nodeBehavior.generate(workingDir, oFile)
			oFile.close()
			file.write('source nodeBehavior.txt\n')
		
		file.write("}\n")
		file.write("\n")
		file.write("wireless_simulation_extended $nNodes $finishTime $javaAgent true $gridW $gridH 100 OFF\n")
		file.close()
			
	def getGridW(self):
		return self.__gridW
	
	def getGridH(self):
		return self.__gridH
	
	def getNNodes(self):
		return self.__nNodes
	
	def getTransmissionRange(self):
		return self.__transmissionRange
	
	def getSimulationTime(self):
		return self.__finishTime
	
	def getDiscardTime(self):
		return self.__discardTime