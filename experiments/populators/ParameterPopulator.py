import math
import types
import random
import os

from xml.dom.minidom import *

from Taxonomy import Taxonomy

class IncorrectSolution(Exception):
    def __init__(self):
        pass

class ParameterPopulator:
    """Provides functions to populate a the list of parameters for a set of nodes
       It also includes a function to generate the XML configuration for each node in the format used in simulations.
    """
    def __init__(self, entries):
        for entry in entries:
            value = entry.firstChild.data
            key = entry.getAttribute("key")
            
            if key == "nNodes":
                self.__nNodes = int(value)
            if key == "nDistribution":
                self.__nDistribution = eval(value)
            if key == "gDistribution":
                self.__gDistribution = eval(value)
            if key == "tDistribution":
                self.__tDistribution = eval(value)  
    
    def generate(self, workingDir):            
        print ''
        print '************* Parameter populator ****************'
        parametersTable, taxonomy = self.__generate(self.__nNodes, self.__nDistribution, self.__gDistribution, self.__tDistribution)    
        
        self.__generateXMLNodeConfigurations(workingDir, parametersTable, taxonomy)

        print '**************************************************'
        
    def getParameters(self):
        return self.__parameters
    
    def __generate(self, nNodes, nDistribution, gDistribution, tDistribution):    
        nDistributionTable = self.__getNDistributionTable(nNodes, nDistribution)
        
        print '* ParameterPopulator: Node distribution: %s' % str(nDistribution)
        #print 'Node distribution table: %s' % str(nDistributionTable)
        
        totalNodes = sum((nodes for nodes, parameters in nDistributionTable))
        if not totalNodes == nNodes:
            raise Exception('Node distribution incorrectly calculated')   
        
        totalParameters = sum((nodes * parameters for nodes, parameters in nDistributionTable))
        print '* ParameterPopulator: Total parameters (distinct or not): %d' % totalParameters
        
        gDistributionTable = self.__getGDistributionTable(totalParameters, gDistribution)
        
        print '* ParameterPopulator: Group distribution: %s' % str(gDistribution) 
        #print 'Group distribution table: %s' %str(gDistributionTable)
        
        sumParameters = sum(gDistributionTable)
        if not sumParameters == totalParameters:
            raise Exception('Group distribution incorrectly calculated')
        
        if not len(tDistribution) == len(gDistributionTable):
            raise Exception('Taxonomy distribution size is not correct. Expected %d', len(gDistributionTable));
        
        tDistributionTable = self.__getTDistributionTable(gDistributionTable, tDistribution)
        
        print '* ParameterPopulator: Taxonomy distribution: %s' % str(tDistribution)
        #print 'Taxonomy distribution tables: %s' % str(tDistributionTable)
        
        correctSolution = False
        while not correctSolution:
            try:
                parameters, taxonomy = self.__createParameters(gDistributionTable, tDistributionTable)
                
                self.__parameters = set(parameters)
        
                #print 'Parameters %s' % str(parameters)
        
                if not len(parameters) == totalParameters:
                    raise Exception('%d parameters generated. %d expected' % (len(parameters, totalParameters)))
                
                parametersTable =  self.__createParametersTable(nDistributionTable, parameters)
                correctSolution = True
            except IncorrectSolution:
                print '* ParameterPopulator: Incorrect distribution generated. Trying another one'
                
        
        #print 'Parameters table: %s' % str(parametersTable)
        
        parametersTable = self.__randomizeTable(parametersTable)
        
        return parametersTable, taxonomy
    
    def __randomizeTable(self, parametersTable):
        randomizedTable = {}
        
        nodes = range(self.__nNodes)
        
        for node in parametersTable.keys():
            index = random.randrange(len(nodes))
            newNode = nodes[index]
            nodes.remove(newNode)
            randomizedTable[newNode] = parametersTable[node]
        
        return randomizedTable
        
    def __createParametersTable(self, nDistributionTable, parameters):
        parametersTable = {}
        
        nodeIndex = 0
        for nodes, numParameters in nDistributionTable:
            for node in xrange(nodeIndex, nodes + nodeIndex):
                selectedParameters = self.__selectDistinctParameters(numParameters, parameters)
                parametersTable[node] = selectedParameters
                nodeIndex += 1
                                
        return parametersTable
            
    def __selectDistinctParameters(self, num, parameters):
        selectedParameters = []        
        
        if len(set(parameters)) < num:
            raise IncorrectSolution()
        
        while not len(selectedParameters) == num:
            index = random.randrange(0, len(parameters))        
            p = parameters[index]
            if not p in selectedParameters:
                selectedParameters.append(p)
                
        for p in selectedParameters:
            parameters.remove(p) 
        
        return selectedParameters
        
    def __parameterGenerator(self):
        currentParameter = 0
        while True:
            yield '%d' % currentParameter
            currentParameter += 1
        
    def __createParameters(self, gDistributionTable, tDistributionTable):
        generatedParameters = []
        
        generator = self.__parameterGenerator()
        
        taxonomy = Taxonomy('TaxonomyRootElement')
        
        for parameters, tElements in zip(gDistributionTable, tDistributionTable):
            if tElements == 'UNIQUE':
                for i in xrange(parameters):
                    p = generator.next()
                    generatedParameters.append(p)
                    taxonomy.getRoot().addChild(p)
            else:
                parent = taxonomy.getRoot()
                for i in xrange(len(tElements)):
                    p = generator.next()
                    parent = parent.addChild(p)
                    for j in xrange(tElements[i]):
                        generatedParameters.append(p)
                        
        return generatedParameters, taxonomy
        
    def __getTDistributionTable(self, gDistributionTable, tDistribution):
        tDistributionTable = []
        
        for parameters, gDistribution in zip(gDistributionTable, tDistribution):
            if gDistribution == 'UNIQUE':
                tDistributionTable.append('UNIQUE')
            else:
                table = self.__getGDistributionTable(parameters, gDistribution)
                tDistributionTable.append(table)
            
        return tDistributionTable
        
    def __getGDistributionTable(self, totalParameters, gDistribution):
        self.__checkGDistribution(gDistribution)
           
        sumParameters = 0
        
        gDistributionTable = []
        
        for index, percentage in enumerate(gDistribution):
            parameters = int(round(totalParameters * percentage))
            if index == len(gDistribution) - 1:
                parameters = totalParameters - sumParameters
            gDistributionTable.append(parameters)
            sumParameters += parameters
            
        return gDistributionTable
        
    def __getNDistributionTable(self, nNodes, nDistribution):
        self.__checkNDistribution(nDistribution)
           
        totalNodes = 0
        
        nDistributionTable = []
        
        for index, (percentage, parameters) in enumerate(nDistribution):
            nodes = int(round(nNodes * percentage))
            if index == len(nDistribution) - 1:
                nodes = nNodes - totalNodes
            nDistributionTable.append((nodes, parameters))
            totalNodes += nodes
            
        return nDistributionTable
    
    def __checkGDistribution(self, gDistribution):
        total = round(sum([percentage for percentage in gDistribution]), 2)
        
        if not total == 1.0:
            raise Exception('Group distribution percentages do not add to 1.0. Obtained %f instead' % total)
        
    def __checkNDistribution(self, nDistribution):
        total = round(sum([percentage for percentage, parameters in nDistribution]), 2)
        
        if not total == 1.0:
            raise Exception('Node distribution percentages do not add to 1.0. Obtained %f instead' % total) 
            
    def __generateXML(self, node, parameters, outputFilePath):
        doc = Document()
        
        parameterList = doc.createElement('parameterlist')
        doc.appendChild(parameterList)          
        
        for p in parameters:
            parameter = doc.createElement('parameter')
            parameter.setAttribute('id', 'I-' + p)
            parameterList.appendChild(parameter)
        
        oFile = open(outputFilePath, 'w')
        oFile.write(doc.toprettyxml())
        oFile.close()
            
    def __generateXMLNodeConfigurations(self, workingDir, parametersTable, taxonomy):
        """Generates parameter configuration XML files for each node included in the parameters table"""
        
        dir = workingDir + '/parameters'
        
        print '* ParameterPopulator: Creating %s directory ' % dir
        os.mkdir(dir)
        
        taxonomyFilePath = workingDir + '/taxonomy.xml'
        oFile = open(taxonomyFilePath, 'w')
        xmlTaxonomy = taxonomy.createXMLDocument() 
        oFile.write(xmlTaxonomy.toprettyxml())
        oFile.close()
        
        for node in parametersTable.keys():
            if len(parametersTable[node]) > 0:
                filePath = dir + '/Parameters' + str(node) + '.xml'
                print '* ParameterPopulator: Generating file %s' % filePath
                parameters = parametersTable[node]
                self.__generateXML(node, set(parametersTable[node]), filePath)
    
        print '* ParameterPopulator: Generation finished'