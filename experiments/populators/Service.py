from xml.dom.minidom import *

class Service:
    
    def __init__(self, id):
        self.__id = id
        self.__inputs = []
        self.__outputs = []
        
    def getID(self):
        return self.__id
        
    def addInput(self, parameter):
        self.__inputs.append('I-' + parameter)
    
    def addOutput(self, parameter):
        self.__outputs.append('O-' + parameter)
        
    def getInputs(self):
        return self.__inputs
    
    def getOutputs(self):
        return self.__outputs
        
    def addXMLService(self, services, doc):
        service = doc.createElement('service')
        service.setAttribute('id', self.__id)
        services.appendChild(service)
        
        parameters = []
        parameters += self.__inputs
        parameters += self.__outputs
        
        for p in parameters:
            parameter = doc.createElement('parameter')
            parameter.setAttribute('id', p)
            service.appendChild(parameter)
        
        