#!/usr/bin/python

import matplotlib.pyplot as plt
import numpy
import re
import math

import os

import sys

from optparse import OptionParser

from xml.dom.minidom import *

from matplotlib.backends.backend_pdf import *

def __relStd(mean, std):
	return 100 * std / mean

class PeriodicResult:
	def __init__(self, tag, period, values, meanTotal, stdTotal, sampleSize):
		self.__tag = float(tag)
		self.__period = period
		self.__values = values
		self.__meanTotal = meanTotal
		self.__stdTotal = stdTotal
		self.__sampleSize = sampleSize
	
	def getTag(self):
		return self.__tag		
	
	def getPeriod(self):
		return self.__period
		
	def getRelStdValues(self):
		relStdValues = []
		for mean, std, time in self.__values:
			if mean == 0:
				relStdValues.append(0)
			else: 
				relStdValues.append(100 * std / mean)
		return relStdValues
	
	def getMeanValues(self):
		return [mean for mean, std, time in self.__values]
	
	def getStdValues(self):
		return [std for mean, std, time in self.__values]
	
	def getTimeValues(self):
		return [time for mean, std, time in self.__values]
	
	def getMeanTotal(self):
		return self.__meanTotal

	def getStdTotal(self):
		return self.__stdTotal
	
	def getSampleSize(self):
		return self.__sampleSize
	
	def getMeanTotal(self):
		return self.__meanTotal
	
	def geStdTotal(self):
		return self.__stdTotal

class Graph:
	def __init__(self, files):
		self.__measures = {}
		
		for file in files:
			try:
				parser = parse(file)
				
				measures = parser.documentElement
				tag = measures.getAttribute('tag')
				type = measures.getAttribute('type')
				
				simulationFinished = measures.getAttribute('simulationFinished')
				
				if simulationFinished == 'no':
					print 'ERROR: There are error in %s output file' % file
					sys.exit()			
				
				measureNodes = parser.getElementsByTagName('measure')
				for measureNode in measureNodes:
					mType = measureNode.getAttribute('type')
					period = measureNode.getAttribute('period')
					units = measureNode.getAttribute('units')
					resultNodes = measureNode.getElementsByTagName('result') 
					
					if not mType in self.__measures:
						self.__measures[mType] = (type, units, [])
					
					for resultNode in resultNodes:
						sampleSize = int(resultNode.getAttribute('sampleSize'))
						meanTotal = float(resultNode.getAttribute('meanTotal'))
						stdTotal = float(resultNode.getAttribute('stdTotal'))
						
						entryNodes = resultNode.getElementsByTagName('entry')
						
						values = []
						for entryNode in entryNodes:
							mean = float(entryNode.getAttribute('mean'))
							std = float(entryNode.getAttribute('std'))
							time = float(entryNode.getAttribute('time'))
							values.append((mean, std, time))
							
						result = PeriodicResult(tag, float(period), values, meanTotal, stdTotal, sampleSize)
						
						expectedType = self.__measures[mType][0]
						if not type == expectedType:
							print 'ERROR: Trying to merge incompatible types'
							sys.exit()
						
						self.__measures[mType][2].append(result)
			except Exception:
				print 'ERROR: Parser error processing file %s' % file
				sys.exit()
	
	def getMeasures(self):
		return self.__measures.keys()
	
	def __checkUnits(self, measures, measureTypes):
		values = []
		
		for measureType in measureTypes:
			name, units, results = measures[measureType]
			values.append(units)
			
		values = set(values)
		
		if len(values) > 1:
			print 'ERROR: Trying to plot measures with different units'
			sys.exit()
			
	def __checkMeasureTypes(self, measures, measureTypes):
		for measureType in measureTypes:
			if not measureType in measures:
				print 'Unknown measure %s' % measureType
				sys.exit()
	
	def plotTotal(self, measureTypes, xLabel=None, yLabel=None, xmin=None, xmax=None, ymin=None, ymax=None):
		self.__checkMeasureTypes(self.__measures, measureTypes)			
		self.__checkUnits(self.__measures, measureTypes)
							
		plotValues = {}
		
		lines = []
		labels = []						

		for measureType in measureTypes:			
			x = []
			y = []
		
			stdValues = []
			relStdValues = []
		
			name, units, results = self.__measures[measureType] 
			for result in results:
				x.append(result.getTag())
				y.append(result.getMeanTotal())
				stdValues.append(result.getStdTotal())
				
				if result.getMeanTotal() == 0:
					relStdValues.append(0)
				else:
					relStdValues.append(100 * result.getStdTotal() / result.getMeanTotal())
				
			#print 'Plotting %s measure type' % measureType
			#print "X: ", x
			#print "Y: ", y
			
			if not xLabel is None:
				plt.xlabel(xLabel)
			else:
				plt.xlabel(name)
				
			label = self.__formatType(measureType)
		
			self.__printTotalInfo(y, stdValues, relStdValues, label, '')
		
			line = plt.plot(x, y, label=label)
			
			lines.append(line)
			labels.append(label)
		
		if not yLabel is None:
			plt.ylabel(yLabel)
		else:
			plt.ylabel(units)
		
		self.__setAxis(plt, xmin, xmax, ymin, ymax) 
		
		plt.figlegend(lines, labels, 'upper right')
		
	def finishPlotting(self, plt, fName, format='DISPLAY'):
		if format == 'DISPLAY':
			plt.show()
		else:
			plt.savefig(fName, format=format)
		
	def __formatType(self, measureType):
		words = re.findall('[A-Z][^A-Z]*', measureType)
		for num, word in enumerate(words):
			if num > 0:
				result = result + ' ' + word.lower()
			else:
				result = word
		return result
		
	def __setAxis(self, plt, xmin, xmax, ymin, ymax):
		axis = list(plt.axis())
		if xmin is not None and xmax is not None:
			axis[0] = float(xmin)
			axis[1] = float(xmax)
			
		if ymin is not None and ymax is not None:
			axis[2] = float(ymin)
			axis[3] = float(ymax)
			
		plt.axis(axis)
		
	def plotPeriodic(self, measureTypes, yLabel=None, xmin=None, xmax=None, ymin=None, ymax=None):
		self.__checkMeasureTypes(self.__measures, measureTypes)			
		self.__checkUnits(self.__measures, measureTypes)
						
		plotValues = {}

		plt.xlabel('time [s]')
		
		lines = []
		labels = []
			
		plt.grid(True)
		for measureType in measureTypes:			
			if not measureType in self.__measures:
				print 'ERROR: Unknown measure %s' % measureType
				sys.exit()
			else:			
				name, units, results = self.__measures[measureType]
				for result in results: 
					x = result.getTimeValues()
					y = result.getMeanValues()
					relStdValues = result.getRelStdValues()
					maxRelStd = max(relStdValues) 
					
					#print "X: ", x
					#print "Y: ", y
			
					formattedType = self.__formatType(measureType)
			
					label = '%s (%s = %d)' % (formattedType, name, result.getTag())
					
					self.__printPeriodicInfo(y, x, result.getStdValues(), relStdValues, label, '')
			
					line = plt.plot(x, y)
					lines.append(line)
					
					labels.append(label)
		
		if not yLabel is None:
			plt.ylabel(yLabel)
		else:
			plt.ylabel(units)
			
		self.__setAxis(plt, xmin, xmax, ymin, ymax)
		
		plt.figlegend(lines, labels, 'upper right')  
		
	def printSummary(self):
		for measureType in sorted(self.__measures):
			name, units, results = self.__measures[measureType]
			print 'Measure: %s [%s]' % (measureType, units)
			tagValues = {}
			for result in results:
				tagValues[result.getTag()] = result	
			
			for tag in sorted(tagValues.keys()):
				result = tagValues[tag]
				print ' *%s: %s mean: %.2f std: %2.f' % (name, tag, result.getMeanTotal(), result.getStdTotal())
				
	def plotAll(self, format, onePDF):
		if onePDF:
			rowsPerPage = 1
			columnsPerPage = 1
			plotsPerPage = rowsPerPage * columnsPerPage
			pp = PdfPages('allPlots.pdf')
			
			for index, measure in enumerate(sorted(self.__measures)):
				
				plotNumber = index % plotsPerPage
				if plotNumber == 0:
					plt.clf() 
					plt.figure(index / plotsPerPage + 1)
					 
				plt.subplot(rowsPerPage, columnsPerPage, plotNumber + 1)
				self.plotPeriodic([measure])
				
				if plotNumber + 1 == plotsPerPage:
					plt.savefig(pp, format='pdf')
				
			pp.close()
		else: 
			for measure in sorted(self.__measures):
				fName = measure + '.' + format
				plt.clf()
				self.plotPeriodic([measure])
				self.finishPlotting(plt, fName, format) 
				
	def __printPeriodicInfo(self, data, times, stdValues, relStdValues, label, units):
		print ''
		print 'Plotting periodic data label=\'%s\':' % label
		print '\t*Size: %d elements' % len(data)
		print '\t*Time range: [%s,%s] s' % (min(times), max(times))
		print '\t*Max std: %.2f %s' % (max(stdValues), units)
		print '\t*Max relative std: %.2f%%' % max(relStdValues)
		print '\t*Mean relative std: %.2f%%' % numpy.mean(relStdValues)
		print '\t*Std of relative std: %.2f%%' % numpy.std(relStdValues)
		print ''
		
	def __printTotalInfo(self, data, stdValues, relStdValues, label, units):
		print ''
		print 'Plotting total data label=\'%s\':' % label
		print '\t*Size: %d elements' % len(data)
		print '\t*Max std: %.2f %s' % (max(stdValues), units)		
		print '\t*Max relative std: %.2f%%' % max(relStdValues)
		print '\t*Mean relative std: %.2f%%' % numpy.mean(relStdValues)
		print '\t*Std of relative std: %.2f%%' % numpy.std(relStdValues)
		print ''

def getXMLFiles(path):
	xmlFiles = []
	for file in os.listdir(path):
		filePath = path + '/' + file
		if not os.path.isdir(filePath) and '.xml' in file:
			xmlFiles.append(filePath)
	
	return xmlFiles

def main():
	parser = OptionParser()
	parser.add_option("-i", "--input", dest="inputFile", help="experiment report used as input")
	parser.add_option("-m", "--merge", dest="directory", help="directory containing the result files to merge")
	parser.add_option("-p", "--periodic", dest="periodic", help="plots a periodic graph (default is total graph)", action="store_true", default=False)
	parser.add_option("-s", "--summary", dest="summary", help="prints the summary of the parsed files", action="store_true", default=False)
	parser.add_option("-a", "--all", dest="all", help="plots all measures each one in a separate image", action="store_true", default=False)
	parser.add_option("-f", "--format", dest="format", help='output format for plotted image', default='DISPLAY')
	parser.add_option("-o", "--onePDF", dest="onePDF", help='write plots to a multiple pages PDF', action="store_true", default=False)
	parser.add_option("-w", "--writeFile", dest="writeFile", help="plotting output file name", default=None)
	
	parser.add_option("--xLabel", dest="xLabel", help="sets the label for X axis")
	parser.add_option("--yLabel", dest="yLabel", help="sets the label for Y axis")
	parser.add_option("--xmin", dest="xmin", help="sets the min value for X axis")
	parser.add_option("--xmax", dest="xmax", help="sets the max value for X axis")
	parser.add_option("--ymin", dest="ymin", help="sets the min value for Y axis")
	parser.add_option("--ymax", dest="ymax", help="sets the max value for Y axis")
	
	(options, args) = parser.parse_args()
	
	if options.inputFile is None and options.directory is None:
		parser.print_usage()
	else:
		files = []
		if not options.directory is None:
			files = getXMLFiles(options.directory)
		else:
			files.append(options.inputFile)
			
		graph = Graph(files)
		
		if options.all: 
			graph.plotAll(options.format, options.onePDF)
			sys.exit()
			
		if options.summary:
			graph.printSummary()
			sys.exit()
		
		if len(args) == 0:
			if not options.summary:
				print 'Supported measures: %s' % graph.getMeasures()
			else:
				graph.printSummary()
		else:  					
			if not options.periodic:
				#Plot selected measure types				
				graph.plotTotal(args, options.xLabel, options.yLabel, options.xmin, options.xmax, options.ymin, options.ymax)
				graph.finishPlotting(plt, options.writeFile, options.format)
			else: 
				graph.plotPeriodic(args, options.yLabel, options.xmin, options.xmax, options.ymin, options.ymax)
				graph.finishPlotting(plt, options.writeFile, options.format)

if __name__ == '__main__':
	 main()

