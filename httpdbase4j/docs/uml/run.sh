rm uml.png
java -jar /usr/local/UMLGraph-4.8/lib/UmlGraph.jar -private -nodefontname Courier -nodefontclassname Courier  uml.java 
dot -Tpng -ouml.png graph.dot

java -jar /usr/local/UMLGraph-4.8/lib/UmlGraph.jar -private -nodefontname Courier -nodefontclassname Courier  templ.java
dot -Tpng -otempl.png graph.dot
