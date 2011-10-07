import math
import types
import random
import os
from time import time

import util.TimeFormatter as TimeFormatter

from xml.dom.minidom import *

from Service import Service
from Taxonomy import Taxonomy

class CompositionsPopulator:
    """Provides functions to create compositions and services for different nodes using a set of restrictions
       It also includes a function to generate the XML configuration for each node in the format used in simulations.
    """
    def __init__(self, entries):
        for entry in entries:
            value = entry.firstChild.data
            key = entry.getAttribute("key")
            
            if key == "nCompositions":
                self.__nCompositions = int(value)
            if key == "compositionIO":
                self.__compositionIO = eval(value)
            #if key == "sDistribution":
            #    self.__sDistribution = float(value)
            #all compositions are valid
            self.__sDistribution = 1.0
            if key == "dDistribution":
                self.__dDistribution = eval(value)
            if key == "width":
                self.__width = eval(value)
            if key == 'nNodes':
                self.__nNodes = int(value)
                
            self.__serviceNameGenerator = self.__nameGenerator()
            self.__parameterNameGenerator = self.__nameGenerator()
    
    def generate(self, workingDir):
        self.__workingDir = workingDir
        self.__servicesDir = self.__workingDir + '/services'
        self.__solutionsDir = self.__servicesDir + '/solutions/'
                    
        print ''
        print '************* Composition populator ****************'
        
        startTime = time()
        self.__generate()
        print '* Compositions generation time: %s ' % TimeFormatter.formatTime(time() - startTime)

        print '****************************************************'
    
    def __generate(self):        
        compositionsWithSolutions = int(self.__nCompositions * self.__sDistribution)
        compositionsWithoutSolutions = self.__nCompositions - compositionsWithSolutions
         
        print '* Nodes: %d' % self.__nNodes   
        print '* Total compositions: %d' % self.__nCompositions
        print '* Solution distribution: %s -> (%d, %d)' % (self.__sDistribution, compositionsWithSolutions, compositionsWithoutSolutions)
        print '* CompositionIO: %s' % str(self.__compositionIO)
        print '* Depth distribution: %s' % str(self.__dDistribution)
        print '* Solution width: %s' % str(self.__width)
        
        self.__createServiceDirectory()
        
        self.__checkRange(self.__compositionIO, 'compositionIO')
        self.__checkRange(self.__width, 'width')
    
        dDistributionTable = self.__getDistributionTable(compositionsWithSolutions, self.__dDistribution, 'dDistribution')
        
        self.__compositions = []
        services = []
        
        for numCompositions, maxDepth in dDistributionTable:
            for i in xrange(numCompositions):
                composition, compositionGraph = self.__createComposition(maxDepth, False)
                self.__compositions.append(composition)
                
                self.__generateXMLCompositionGraph(composition, compositionGraph)
                
                for compositionServices in compositionGraph.values():
                    services += compositionServices
        
        for i in xrange(compositionsWithoutSolutions):
            composition, compositionServices = self.__createComposition(5, True)
            self.__compositions.append(composition)
            services += compositionServices

	print '* Generated services: %d' % len(services)
        
        #distribute services among nodes
        nodes = {}
        for service in services:
            node = random.randrange(self.__nNodes)
            if not node in nodes:
                nodes[node] = []
            nodes[node].append(service)
        
        taxonomy = Taxonomy('TaxonomyRootElement')
        
        self.__generateXMLNodeConfigurations(self.__compositions, nodes, taxonomy)
        
    def getCompositions(self):
        return self.__compositions
        
    def __createComposition(self, maxDepth, invalid):
        serviceName = "Composition-" + self.__serviceNameGenerator.next()
        composition = Service(serviceName)
        
        for i in xrange(random.randrange(self.__compositionIO[0], self.__compositionIO[1])):
            composition.addInput(self.__parameterNameGenerator.next())
            
        currentOutputs = self.__getInitOutputs(composition)
    
        services = {}
        compositionOutputs = self.__createGraph(currentOutputs, services, 0, maxDepth, invalid)
        for output in compositionOutputs:
            composition.addOutput(self.__getParamID(output))
    
        return composition, services
    
    def __createGraph(self, currentOutputs, services, currentDepth, maxDepth, invalid):
        #create next services
        nextServices = []
        for i in xrange(random.randrange(self.__width[0], self.__width[1])):
            nextService = Service('Service-' + self.__serviceNameGenerator.next())
            selectedInputs = self.__selectInputs(currentOutputs)
            for input in selectedInputs:
                nextService.addInput(self.__getParamID(input))
                
            for i in xrange(random.randrange(self.__compositionIO[0], self.__compositionIO[1])):
                nextService.addOutput(self.__parameterNameGenerator.next())
            
            nextServices.append(nextService)
            
        currentDepth += 1
            
        if invalid and self.__stopGeneration(currentDepth, maxDepth):
            randomOutputs = []
            for i in xrange(random.randrange(self.__compositionIO[0], self.__compositionIO[1])):
                randomOutputs.append(self.__parameterNameGenerator.next())
            return randomOutputs
            
        services[currentDepth] = []
        services[currentDepth] += nextServices
        if currentDepth < maxDepth:
            currentOutputs = self.__getOutputs(nextServices)
            return self.__createGraph(currentOutputs, services, currentDepth, maxDepth, invalid)
        else:
            return self.__getOutputs(nextServices)    
        
    def __stopGeneration(self, currentDepth, maxDepth):
        #stop probability increases with depth        
        if currentDepth >= maxDepth:
            return True
        
        value = random.randrange(maxDepth - currentDepth)
        return value == 0
    
    def __getOutputs(self, services):
        outputs = []
        for service in services:
            outputs += service.getOutputs()
        return outputs
    
    def __selectInputs(self, outputs):
        numInputs = random.randrange(len(outputs)) + 1
        #select numInputs outputs
        selectedOutputs = []
        for i in xrange(numInputs):
            index = random.randrange(numInputs)
            selectedOutputs.append(outputs[index])
            
        selectedOutputs = set(selectedOutputs)
        
        return self.__convertToInputs(selectedOutputs)
    
    def __getInitOutputs(self, composition):
        return self.__convertToOutputs(composition.getInputs())
    
    def __getGoalInputs(self, composition):
        return self.__convertToInputs(composition.getOutputs())
    
    def __getParamID(self, p):
        return p[p.find('-') + 1:]
    
    def __convertToOutputs(self, parameters):
        outputs = []
        for p in parameters:
            output = 'O-' + p[p.find('-') + 1:]
            outputs.append(output)
        return outputs
    
    def __convertToInputs(self, parameters):
        outputs = []
        for p in parameters:
            output = 'I-' + p[p.find('-') + 1:]
            outputs.append(output)
        return outputs
        
    def __nameGenerator(self):
        currentName = 0
        while True:
            yield '%d' % currentName
            currentName += 1
    
    def __checkRange(self, range, str):
        if range[0] < 1 or range[1] < 1:
            raise Exception('Range ' + str + ' should be positive and greater or equal than 1')
        
    def __getTotalSolutions(self, sDistributionTable):
        totalSolutions = 0;
        for compositions, solutions in sDistributionTable:
            totalSolutions += compositions * solutions
        return totalSolutions
    
    def __checkDistribution(self, distribution, str):
        total = round(sum([percentage for percentage, value in distribution]), 2)
        
        if not total == 1.0:
            raise Exception('Distribution ' + str + ' percentages do not add to 1.0. Obtained %f instead' % total)
        
    def __getDistributionTable(self, elements, distribution, str):
        self.__checkDistribution(distribution, str)
           
        totalElements = 0
        
        distributionTable = []
        
        for index, (percentage, values) in enumerate(distribution):
            calculatedElements = int(round(elements * percentage))
            if index == len(distribution) - 1:
                calculatedElements = elements - totalElements
            distributionTable.append((calculatedElements, values))
            totalElements += calculatedElements
            
        return distributionTable
    
    def __createServiceDirectory(self):
        print '* Creating %s directory ' % self.__servicesDir
        os.mkdir(self.__servicesDir)
        os.mkdir(self.__solutionsDir)
    
    def __generateXMLCompositionGraph(self, composition, compositionGraph):                                        
        doc = Document()
        compositionGraphElement = doc.createElement('compositionGraph')
        doc.appendChild(compositionGraphElement);
        for layer in sorted(compositionGraph.keys()):
            layerElement = doc.createElement("layer")
            layerElement.setAttribute('depth', str(layer))
            compositionGraphElement.appendChild(layerElement)
            for service in compositionGraph[layer]:
                serviceElement = doc.createElement('service')
                serviceElement.setAttribute('name', service.getID())
                layerElement.appendChild(serviceElement)
            
        filePath =  self.__solutionsDir + '/' + composition.getID() + '.xml' 
        oFile = open(filePath, 'w')
        oFile.write(doc.toprettyxml())
        oFile.close();
    
    def __generateXMLNodeConfigurations(self, compositions, nodes, taxonomy):                
        taxonomyFilePath = self.__workingDir + '/taxonomy.xml'
        oFile = open(taxonomyFilePath, 'w')
        xmlTaxonomy = taxonomy.createXMLDocument() 
        oFile.write(xmlTaxonomy.toprettyxml())
        oFile.close()
        
        self.__writeCompositions(compositions)
        
        self.__writeNodeServices(nodes)
        
    def __writeNodeServices(self, nodes):
        for node in nodes.keys():
            servicesFilePath = self.__servicesDir + '/Services' + str(node) + '.xml'
            oFile = open(servicesFilePath, 'w')
            doc = self.__createServicesXMLDocument(nodes[node])
            oFile.write(doc.toprettyxml())
            oFile.close()
    
    def __createServicesXMLDocument(self, services):
        doc = Document()
        
        servicesElement = doc.createElement('services')
        doc.appendChild(servicesElement)
        for service in services:
            service.addXMLService(servicesElement, doc)
            
        return doc
        
    def __writeCompositions(self, compositions):
        compositionsFilePath = self.__servicesDir + '/Services.xml'
        oFile = open(compositionsFilePath, 'w')
        doc = self.__createServicesXMLDocument(compositions)
        oFile.write(doc.toprettyxml())
        oFile.close()
