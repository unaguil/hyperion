from xml.dom.minidom import *

class Element:
    def __init__(self, id):
        self.__id = id
        self.__childs = []
        
    def getID(self):
        return self.__id
        
    def addChild(self, child):
        element = Element(child)
        self.__childs.append(element)
        return element
        
    def childs(self):
        return self.__childs

class Taxonomy:
    def __init__(self, root):
        self.__root = Element(root)

    def getRoot(self):
        return self.__root
    
    
    def __recursiveWrite(self, currentElement, docRoot, doc):
        element = doc.createElement('element');
        element.setAttribute('id', currentElement.getID());
        docRoot.appendChild(element);
        
        for child in currentElement.childs():
            eChild = doc.createElement('child');
            eChild.setAttribute('id', child.getID());
            element.appendChild(eChild);
        
        for child in currentElement.childs():
            self.__recursiveWrite(child, docRoot, doc)
            
    def createXMLDocument(self):
        doc = Document()
        
        docRoot = doc.createElement('taxonomy');
        docRoot.setAttribute('root', self.getRoot().getID());
        doc.appendChild(docRoot);
        
        self.__recursiveWrite(self.getRoot(), docRoot, doc)
        
        return doc 
        
    
