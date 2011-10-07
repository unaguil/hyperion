from xml.dom.minidom import *
import re
import numpy
import gzip

import periodicValues.PeriodicValues

import periodicValues.Util as Util

import datetime

import generic.Units as Units

DEFAULT_PERIOD = 1.0

def get_class(clazz):
	parts = clazz.split('.')
	module = '.'.join(parts[:-1])
	m = __import__(module)
	for comp in parts[1:]:
		m = getattr(m, comp)
	return m
		
class PeriodicEntry:
	def __init__(self, meanValues, stdValues, period, meanTotal, stdTotal, size):
		self.meanValues = meanValues
		self.stdValues = stdValues
		self.period = period
		self.meanTotal = meanTotal
		self.stdTotal = stdTotal
		self.size = size

class Measures:
	def __init__(self, expConfigFile):
		expConfig = parse(expConfigFile)
				
		measureNodes = expConfig.getElementsByTagName('measure')
		self.__entries = expConfig.getElementsByTagName('entry')
		
		for entry in self.__entries:
			value = entry.firstChild.data
			key = entry.getAttribute("key")
			
			if key == 'finishTime':
				self.__simulationTime = float(value)

		self.__measureData = {}
		
		for measureNode in measureNodes:
			clazz = measureNode.getAttribute('type')
			periodStr = measureNode.getAttribute('period');
							
			if periodStr is not '':
				period = float(periodStr)
			else:
				period = DEFAULT_PERIOD
							
			name = clazz.split('.')[1]
			
			units = self.__getMeasureUnits(clazz, period)
			
			self.__measureData[name] = (clazz, period, units)
		
		self.configurationMeasures = {}
		
		self.__compiledTimePattern = re.compile(r'.*? ([0-9]+\,[0-9]+) Thread-[0-9]+')
		self.__compiledFinishedPattern = re.compile(r'INFO  peer.BasicPeer  - Simulation finished.*?')
		
		self.__simulationFinished = False
		
		self.finishedSimulations = [] 

	def __createMeasure(self, clazz, period):
		className = clazz.split('.')[-1]
		measureClass = get_class('measures.' + clazz + '.' + className)
		
		return measureClass(period, self.__simulationTime)
					
		print 'Invalid measure type: ', value
		return None
	
	def __getMeasureUnits(self, clazz, period):
		measure = self.__createMeasure(clazz, period)
		return measure.getUnits()

	def parseLog(self, outputLog):
		outputFile = gzip.open(outputLog, 'r')
		
		line = outputFile.readline()
		while (line != '' ):
			line.replace( '\n', '' )
		
			time = self.__getLogLineTime(line) 
			
			self.__checkFinished(line)
					
			#Parse line using each measure
			for measure in self.currentMeasures:
				measure.parseLine(line)

			line = outputFile.readline()
			
	def __getLogLineTime(self, line):
		m = self.__compiledTimePattern.match(line)
		if m is not None: 
			return float(m.group(1).replace(',', '.'))
		else:
			return 0.0
		
	def __checkFinished(self, line):
		m = self.__compiledFinishedPattern.match(line)
		if m is not None:
			self.__simulationFinished = True

	def startConfiguration(self, n, name):
		#Create a table for measures values
		self.measures = {}
		self.configurationMeasures[n] = {}
		self.finishedSimulations.append([])
		self.currentConfigurationStatus = self.finishedSimulations[n]
		for name in self.__measureData.keys():
			self.measures[name] = [] 
			
	def startRepeat(self):
		#Create the measure objects 
		self.currentMeasures = []
		for clazz, period, units in self.__measureData.values():
			measure = self.__createMeasure(clazz, period)
			self.currentMeasures.append(measure)
			
		self.__simulationFinished = False
		
	def endRepeat(self):
		#Obtain the value of this repeat and store it
		for measure in self.currentMeasures:
			self.measures[measure.getType()].append((measure.getValues(), measure.getTotalValue()))
			
		self.currentConfigurationStatus.append(self.__simulationFinished)

	def endConfiguration(self, n):
		for measure in self.measures.keys():
			values = self.measures[measure]
			(meanValues, stdValues, meanTotal, stdTotal) = self.__calculateStatistics(values)
			self.configurationMeasures[n][measure] = PeriodicEntry(meanValues, stdValues, self.__measureData[measure][1], meanTotal, stdTotal, len(values))		
				
	def __calculateStatistics(self, valuesArray):
		meanValues = []
		stdValues = []
		
		#get max size
		
		sizes = (len(periodicValues) for periodicValues, totalValues in valuesArray)
		size = max(sizes)
		
		totals = [totalValues for periodicValues, totalValues in valuesArray]
		 
		for i in xrange(size):
			values = []			
			for periodicValues, totalValues in valuesArray:
				values.append(periodicValues.getValue(i))
					
			meanValues.append(numpy.mean(values))	
			stdValues.append(numpy.std(values))
				
		return (meanValues, stdValues, numpy.mean(totals), numpy.std(totals))
	
	def getXMLError(self, tag, type):
		doc = Document()
		
		measuresNode = doc.createElement('measures')
		doc.appendChild(measuresNode)
		
		measuresNode.setAttribute('tag', tag)
		measuresNode.setAttribute('type', type)
		measuresNode.setAttribute('simulationFinished', 'no')
		
		return doc.toprettyxml()
	
	def __allSimulationsFinished(self):
		for configurationStatus in self.finishedSimulations:
			for repeatStatus in configurationStatus:
				if not repeatStatus:
					return False
		return True
												
	def getXMLResults(self, discardTime, experimentTime, tag, type):
		doc = Document()
		
		measuresNode = doc.createElement('measures')
		doc.appendChild(measuresNode)
		
		measuresNode.setAttribute('tag', tag)
		measuresNode.setAttribute('type', type)
		
		#Store date
		measuresNode.setAttribute('creationDate', datetime.datetime.now().strftime('%Y-%m-%d %H:%M'))
		
		configuration = doc.createElement('configuration')
		measuresNode.appendChild(configuration)		
		for entry in self.__entries:
			configuration.appendChild(entry)
		
		if not self.__allSimulationsFinished():
			measuresNode.setAttribute('simulationFinished', 'no')
			
			for configuration, configurationStatus in enumerate(self.finishedSimulations):
				for repeat, repeatStatus in enumerate(configurationStatus):
					if not repeatStatus:
						failedSimulation = doc.createElement('failedSimulation')
						failedSimulation.setAttribute('configuration', str(configuration))
						failedSimulation.setAttribute('repeat', str(repeat))
						measuresNode.appendChild(failedSimulation)
		else:
			for measureName in sorted(self.__measureData.keys()):
				measureNode = doc.createElement('measure')
				measuresNode.appendChild(measureNode)
				measureNode.setAttribute('type', measureName) 
				measureNode.setAttribute('period', str(self.__measureData[measureName][1]))
				units = str(self.__measureData[measureName][2])
				measureNode.setAttribute('units', units)
				for n in range(len(self.configurationMeasures)):
					resultNode = doc.createElement('result')
					entry = self.configurationMeasures[n][measureName]
					resultNode.setAttribute('meanTotal', Units.str_formatter(units, entry.meanTotal))
					resultNode.setAttribute('stdTotal', Units.str_formatter(units, entry.stdTotal))
					resultNode.setAttribute('sampleSize', '%d' % entry.size)
					#write periodic values
					numPeriod = Util.getPeriod(discardTime, entry.period, self.__simulationTime)
					for index, mean in enumerate(entry.meanValues):
						if index >= numPeriod:
							valueNode = doc.createElement('entry')
							valueNode.setAttribute('mean', Units.str_formatter(units, mean))
							valueNode.setAttribute('std', Units.str_formatter(units, entry.stdValues[index]))
							time = (index + 1) * entry.period
							valueNode.setAttribute('time', str(time))
							resultNode.appendChild(valueNode) 
					measureNode.appendChild(resultNode)
	
		return doc.toprettyxml()