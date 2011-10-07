#!/usr/bin/python

import sys
import os
import shutil
import subprocess
import math

from optparse import OptionParser

from time import time

from ConfigGenerator import *
from measures.Measures import *
from ScriptGenerator import *

import util.TimeFormatter as TimeFormatter

MAX_TRIES = 3

class RepeatRunner:
	def __init__(self, tempDir, config, workingDir, scriptGenerator, repeat):
		self.__tempDir = tempDir
		self.__config = config
		self.__configDir = workingDir
		self.__scriptGenerator = scriptGenerator
		self.__repeat = repeat
		
		self.__try = 1
		
	def __execute(self):		
		#Create temporal directory and prepare configuration files
		if os.path.isdir(self.__tempDir):
			shutil.rmtree(self.__tempDir)
		
		os.mkdir(self.__tempDir)
		
		self.__prepareSimulationFiles(self.__tempDir, self.__config, self.__configDir)	
		
		#Create script using ScriptGenerator
		if self.__scriptGenerator.getDiscardTime() >= self.__scriptGenerator.getSimulationTime():
			print 'Discard time %.2f should be smaller than simulation time %.2f ' % (self.__scriptGenerator.getDiscardTime(), self.__scriptGenerator.getSimulationTime())
			sys.exit()
						
		self.__scriptGenerator.generate('Script.tcl', self.__tempDir, self.__repeat)
		
		#Run script
		simStartTime = time()
		out = open(self.__tempDir + '/out', 'w')
		err = open(self.__tempDir + '/err', 'w')
		
		print ''
		print '* Launching simulation script on directory %s try %d' % (self.__tempDir, self.__try)
		sys.stdout.flush()
		
		p = subprocess.Popen(['ns', 'Script.tcl'], stdout=out, stderr=err, cwd=self.__tempDir)
		result = p.wait()
		out.close()
		err.close()
		
		if result != 0:
			print 'ERROR: There was a problem during simulation execution on directory %s' % self.__tempDir
			sys.stdout.flush()
			return False
		
		print '* NS-2 simulation on directory %s running time: %s' % (self.__tempDir, TimeFormatter.formatTime(time() - simStartTime))
		print ''
		sys.stdout.flush()
		
		self.__compressOutputLog()
		
		return True
		
	def run(self):
		success = False
		while self.__try <= MAX_TRIES and not success:		
			success = self.__execute()
			self.__try += 1
			
		if not success:
			print 'ERROR: Max tries for simulation execution reached'
			return False
		else:
			return True
			
	def __compressOutputLog(self):  
		startTime = time()
		print '* Compressing output log file %s' % self.__getPlainLog()
		p = subprocess.Popen(['gzip', self.__getPlainLog()])
		result = p.wait()
		
		if result != 0:
			print 'ERROR: There was a problem compressing file %s' % self.__getPlainLog()
			sys.stdout.flush()
			return False
		
		print '* Compressed log obtained in %s time: %s' % (self.getOutputLog(), TimeFormatter.formatTime(time() - startTime)) 
		print ''
		return True
	
	def __getPlainLog(self):
		return self.__tempDir + '/output.log'
	
	def getOutputLog(self):
		return self.__tempDir + '/output.log.gz'
	
	def __prepareSimulationFiles(self, tempDir, config, workingDir):
		configFile = open(tempDir + '/Configuration.xml', 'w')
		configFile.write(config)
		configFile.close()
		
		shutil.copy2('./common/WCommon.tcl', tempDir + '/WCommon.tcl')
		
		shutil.copy2('log4j.properties', tempDir + '/log4j.properties')
		
		#Copy all files contained in input directory to temporal directory
		for file in os.listdir(workingDir):
			srcPath = workingDir + '/' + file
			dstPath = tempDir + '/' + file
			if not os.path.isdir(srcPath):
				shutil.copy2(srcPath, dstPath)

class Experiment:
	def __init__(self, configDir, inputFile, outputFile, debug, workingDir, processing):
		self.__configDir = configDir
		self.__outputFile = outputFile
		self.__inputFile = self.__configDir + '/' + inputFile
		self.__debug = debug
		self.__workingDir = workingDir
		
		if not processing and os.path.isdir(self.__workingDir):
			shutil.rmtree(self.__workingDir)
		
		if not processing:	
			os.mkdir(self.__workingDir)
		
			shutil.copy2(self.__inputFile, self.__workingDir + '/')
			
	def __saveTimestamp(self, dir):
		oFile = open(dir + '/launched.txt', 'w')
		oFile.write("Experiment started on: %s " % datetime.datetime.now().strftime('%Y-%m-%d %H:%M'))
		oFile.close()

	def __run(self):		
		configGenerator = ConfigGenerator(self.__inputFile)
		
		measures = Measures(self.__inputFile)
		
		initTime = time()
		
		error = False

		configurationCounter = 0		
		
		self.__saveTimestamp(self.__workingDir)
		
		while configGenerator.hasNext():
			configurationDir = self.__workingDir + '/configuration-' + str(configurationCounter)
			
			os.mkdir(configurationDir)
			
			startTime = time()
			
			repeat = configGenerator.getRepeat()
			
			print ''
			print "* Running experiment configuration %d with %d repetition(s)" % (configurationCounter + 1, repeat)
			print ''
			sys.stdout.flush() 
			#Get current execution configuration
			config = configGenerator.next()
			measures.startConfiguration(configurationCounter, configGenerator.getType())
			
			scriptGenerator = ScriptGenerator(config, self.__inputFile)
			
			repeatRunners = []
			
			for counter in range(repeat):
				
				tempDir = configurationDir + '/repeat-' + str(counter)
				
				print ''
				print '* Running repeat %d of %d' % (counter + 1, repeat)
				sys.stdout.flush()
				r = RepeatRunner(tempDir, config, self.__configDir, scriptGenerator, counter)
				repeatRunners.append(r)
				if not r.run():
					error = True
					break
				
			if error:
				break 

			print '* Parsing output log files'
			sys.stdout.flush()
			outputLogs = [r.getOutputLog() for r in repeatRunners]
			
			self.__processRepeat(measures, outputLogs)

			measures.endConfiguration(configurationCounter)
			
			configurationCounter = configurationCounter + 1
			
			print '* Configuration execution time: %s' % TimeFormatter.formatTime(time() - startTime)
			print ''
			sys.stdout.flush()
			
		experimentTime = time() - initTime
			
		if error:
			results = measures.getXMLError(configGenerator.getTag(), configGenerator.getType())
			print 'Experiment finished with errors. Total time: %s' % TimeFormatter.formatTime(experimentTime)
			print '' 
			sys.stdout.flush()
		else:
			results =  measures.getXMLResults(scriptGenerator.getDiscardTime(), experimentTime, configGenerator.getTag(), configGenerator.getType())
			print 'Experiment finished. Total time: %s' % TimeFormatter.formatTime(experimentTime)
			print '' 
			sys.stdout.flush()
			
		#Save to output file
		self.__outputFile.write(results)
			
		#Removing running directory if not must be maintained
		if not self.__debug:
			shutil.rmtree(self.__workingDir)	
		
	def __processRepeat(self, measures, outputLogs):
		for outputLog in outputLogs:
			print 'Output log %s file size %s' % (outputLog, self.__sizeof_fmt(os.path.getsize(outputLog)))
			sys.stdout.flush()
			#Measure results
			logProcessStartTime = time()
			measures.startRepeat()
			measures.parseLog(outputLog)
			measures.endRepeat()
							
			print 'Output log %s parsing time: %s' % (outputLog, TimeFormatter.formatTime(time() - logProcessStartTime))
			print ''
			sys.stdout.flush()
		
	def __sizeof_fmt(self, num):
	    for x in ['bytes','KB','MB','GB','TB']:
	        if num < 1024.0:
	            return "%3.1f%s" % (num, x)
	        num /= 1024.0		
	        
	def __processOutput(self):
		configGenerator = ConfigGenerator(self.__inputFile)
		
		measures = Measures(self.__inputFile)
		
		initTime = time()

		configurationCounter = 0		
		while configGenerator.hasNext():
			configurationDir = self.__workingDir + '/configuration-' + str(configurationCounter)
			
			startTime = time()
			
			repeat = configGenerator.getRepeat()
			
			print ''
			print ''
			print "* Processing experiment configuration %d output with %d repetition(s)" % (configurationCounter + 1, repeat)
			sys.stdout.flush() 
			#Get current execution configuration
			config = configGenerator.next()
			measures.startConfiguration(configurationCounter, configGenerator.getType())
			
			scriptGenerator = ScriptGenerator(config, self.__inputFile)

			print ''
			print 'Parsing output log files'
			sys.stdout.flush()
			outputLogs = []
			
			for i in range(repeat):
				outputLog = configurationDir + '/repeat-' + str(i) + '/output.log.gz'
				outputLogs.append(outputLog)
			
			self.__processRepeat(measures, outputLogs)

			measures.endConfiguration(configurationCounter)
			
			configurationCounter = configurationCounter + 1
			
			print '* Configuration processing time: %s' % TimeFormatter.formatTime(time() - startTime)
			print ''
			sys.stdout.flush()

		experimentTime = time() - initTime

		print 'Experiment output processing finished. Total time: %s' % TimeFormatter.formatTime(experimentTime)
		print '' 
		
		results =  measures.getXMLResults(scriptGenerator.getDiscardTime(), experimentTime, configGenerator.getTag(), configGenerator.getType())
		
		#Save to output file
		self.__outputFile.write(results)
		
	def perform(self, processing):
		if not processing:
			self.__run()
		else:
			self.__processOutput()
		
def __runExperiment(outputFile, configDir, inputFile, debug, workingDir, processing): 
	if outputFile is not sys.stdout:
		oFile = open(outputFile, 'w');
		e = Experiment(configDir, inputFile, oFile, debug, workingDir, processing)
		e.perform(processing)
		oFile.close()
	else:
		e = Experiment(configDir, inputFile, sys.stdout, debug, workingDir, processing)
		e.perform(processing)
		
def main():
	
	print '****************************************************'
	print '            Experimentation environment               '
	print ''
	print '****************************************************'
	
	parser = OptionParser()
	parser.add_option("-c", "--configDir", dest="configDir", help="experiment configuration directory")
	parser.add_option("-f", "--file", dest="inputFile", help="experiment configuration file")
	parser.add_option("-o", "--output", dest="outputFile", help="experiment report output file", default=sys.stdout)
	parser.add_option("-w", "--workingDir", dest="workingDir", help="directory to store simulation results", default='/tmp/experiment')
	parser.add_option("-d", "--debug", dest="debug", help="debug mode", default=False)
	parser.add_option("-p", "--processDir", dest="processDir", help="process output directory")
	
	(options, args) = parser.parse_args()
	
	if not options.processDir is None:
		inputFile = None
		for file in os.listdir(options.processDir):
			if not os.path.isdir(file) and '.xml' in file:
				inputFile = file
		
		if inputFile is None:
			print 'ERROR: Experiment config file not found in output directory'
			sys.exit()
			
		__runExperiment(options.outputFile, options.processDir, inputFile, True, options.processDir, True)
	else:
		if options.configDir is None or options.inputFile and None:
			parser.print_usage()
		else:
			inputFilePath = options.configDir + '/' + options.inputFile
			if inputFilePath is options.outputFile:
				parser.error("ERROR: Input and output files cannot be the same") 
			
			__runExperiment(options.outputFile, options.configDir, options.inputFile, options.debug, options.workingDir, False)

if __name__ == '__main__':
    main()
