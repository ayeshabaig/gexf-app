package edu.umuc.swen670.gexf.internal.model;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cytoscape.model.CyNetwork;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GEXF13Parser extends GEXFParserBase {

	public GEXF13Parser(Document doc, XMLStreamReader xmlReader, CyNetwork cyNetwork, String version) {
		super(doc, xmlReader, cyNetwork, version);
	}

	@Override
	public void ParseStream() throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, XMLStreamException {
		
		String defaultEdgeType = "";
		String mode = "";
		
		while(_xmlReader.hasNext()) {
			int event = _xmlReader.next();

			switch(event) {
			case XMLStreamConstants.START_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFMeta.META)) {
					ParseMeta();
					break;
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFGraph.GRAPH)) {
					List<String> graphAttributes = GetElementAttributes();
					
					defaultEdgeType = graphAttributes.contains(GEXFGraph.DEFAULTEDGETYPE) ? _xmlReader.getAttributeValue(null, GEXFGraph.DEFAULTEDGETYPE).trim() : EdgeTypes.UNDIRECTED;
					mode = graphAttributes.contains(GEXFGraph.MODE) ? _xmlReader.getAttributeValue(null, GEXFGraph.MODE).trim() : GEXFGraph.STATIC;
				}
			}
		}

		_attNodeMapping = ParseAttributeHeader("node");
		
		_attEdgeMapping = ParseAttributeHeader("edge");
		_cyNetwork.getDefaultEdgeTable().createColumn(GEXFEdge.EDGETYPE, String.class, true);
		_cyNetwork.getDefaultEdgeTable().createColumn(GEXFEdge.WEIGHT, Double.class, true);


		XPath xPath =  XPathFactory.newInstance().newXPath();
		String expression = "/gexf/graph/nodes/node";
		NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(_doc, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node xNode = nodeList.item(i);

			ParseNode(xNode, expression);
		}

		ParseEdges(defaultEdgeType);
	}
	
	@Override
	protected <T> List<T> ParseArray(String array, Class<T> type) throws IOException {
		List<T> list = new ArrayList<T>();
		
		StringReader reader = new StringReader(array + ' '); //pad the string
		
		int r;
		char c;
		while((r=reader.read()) != -1) {
			c = (char)r;
			
			if(c=='[' || c=='(' || c==']' || c==')' || c==',' || c==' ' || c=='\t' || c=='\r' || c=='\n') {
				//keep processing
			}
			else if(c=='"' || c=='\'') {
				String literal = "";
				
				do {
					literal = literal + c;
					
					c = (char)reader.read();
				}while(c!='"' && c!='\'');
				
				list.add(GenericParse(literal, type));
				reader.skip(-1);
			}
			else {
				String value = "";
				
				do {
					value = value + c;
					
					c = (char)reader.read();
				}while(c!=']' && c!=')' && c!=',' && c!=' ' && c!='\t' && c!='\r' && c!='\n');
				
				if(value.equals("null")) {
					list.add(null);
				}
				else {
					list.add(GenericParse(value, type));
				}
				
				reader.skip(-1);
			}
		}

		return list;
	}

	@Override
	protected Boolean IsDirected(String direction) {
		if(direction.equalsIgnoreCase(EdgeTypes.DIRECTED)) {
			return true;
		}
		else if(direction.equalsIgnoreCase(EdgeTypes.UNDIRECTED)) {
			return false;
		}
		else if (direction.equalsIgnoreCase(EdgeTypes.MUTUAL)) {
			return true;
		}
		else {
			throw new IllegalArgumentException(direction);
		}
	}

	@Override
	protected Boolean IsBiDirectional(String direction) {
		if (direction.equalsIgnoreCase(EdgeTypes.MUTUAL)) {
			return true;
		}
		else {
			return false;
		}
	}

}
