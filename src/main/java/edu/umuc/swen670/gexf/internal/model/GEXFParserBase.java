package edu.umuc.swen670.gexf.internal.model;

import java.awt.Color;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;

import edu.umuc.swen670.gexf.internal.io.DelayedVizProp;

abstract class GEXFParserBase {

	protected XMLStreamReader _xmlReader = null;
	protected CyNetwork _cyNetwork = null;
	protected String _version = "";
	protected CyGroupFactory _cyGroupFactory = null;
	
	protected Hashtable<String, Long> _idMapping = new Hashtable<String, Long>();
	protected AttributeMapping _attNodeMapping = null;
	protected AttributeMapping _attEdgeMapping = null;
	
	protected List<DelayedVizProp> _vizProps = new ArrayList<DelayedVizProp>();
	
	public GEXFParserBase(XMLStreamReader xmlReader, CyNetwork cyNetwork, String version, CyGroupFactory cyGroupFactory) {
		_xmlReader = xmlReader;
		_cyNetwork = cyNetwork;
		_version = version;
		_cyGroupFactory = cyGroupFactory;
	}
	
	public abstract List<DelayedVizProp> ParseStream() throws IOException, XMLStreamException;
	
	protected void SetupVisualMapping() {
		//nodes
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_X, Double.class, false);
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_Y, Double.class, false);
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_Z, Double.class, false);
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_SHAPE, String.class, false);
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_COLOR, String.class, false);
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_TRANSPARENCY, Integer.class, false);
		_cyNetwork.getDefaultNodeTable().createColumn(GEXFViz.ATT_SIZE, Double.class, false);
		
		//edges
		_cyNetwork.getDefaultEdgeTable().createColumn(GEXFViz.ATT_SHAPE, String.class, false);
		_cyNetwork.getDefaultEdgeTable().createColumn(GEXFViz.ATT_COLOR, String.class, false);
		_cyNetwork.getDefaultEdgeTable().createColumn(GEXFViz.ATT_TRANSPARENCY, Integer.class, false);
		_cyNetwork.getDefaultEdgeTable().createColumn(GEXFViz.ATT_THICKNESS, Double.class, false);
	}
	
	protected void ParseMeta() throws InvalidClassException, XMLStreamException {
		CyTable cyTable = _cyNetwork.getDefaultNetworkTable();
		CyRow cyRow = cyTable.getRow(_cyNetwork.getSUID());
				
		List<String> attributes = GetElementAttributes();
		if(attributes.contains(GEXFMeta.LASTMODIFIEDDATE)) {
			if(cyTable.getColumn(GEXFMeta.LASTMODIFIEDDATE)==null) {
				cyTable.createColumn(GEXFMeta.LASTMODIFIEDDATE, String.class, false);
			}
			cyRow.set(GEXFMeta.LASTMODIFIEDDATE, _xmlReader.getAttributeValue(null, GEXFMeta.LASTMODIFIEDDATE).trim());
		}
		
		String tagContent = null;
		
		while(_xmlReader.hasNext()) {
			int event = _xmlReader.next();

			switch(event) {
			case XMLStreamConstants.END_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFMeta.META)) {
					return;
				}
				else if(_xmlReader.getLocalName().trim().equalsIgnoreCase(GEXFMeta.CREATOR) || 
						_xmlReader.getLocalName().equalsIgnoreCase(GEXFMeta.DESCRIPTION) || 
						_xmlReader.getLocalName().equalsIgnoreCase(GEXFMeta.KEYWORDS)) {
					
					if(cyTable.getColumn(_xmlReader.getLocalName().trim().toLowerCase())==null) {
						cyTable.createColumn(_xmlReader.getLocalName().trim().toLowerCase(), String.class, false);
					}
					if(tagContent!=null) {cyRow.set(_xmlReader.getLocalName().trim().toLowerCase(), tagContent.trim());}
					
					tagContent = null;
					
					break;
				}
				else {
					throw new InvalidClassException(_xmlReader.getLocalName().trim());
				}
			case XMLStreamConstants.START_ELEMENT :
				if(_xmlReader.getLocalName().trim().equalsIgnoreCase(GEXFMeta.CREATOR) || 
						_xmlReader.getLocalName().equalsIgnoreCase(GEXFMeta.DESCRIPTION) || 
						_xmlReader.getLocalName().equalsIgnoreCase(GEXFMeta.KEYWORDS)) {
					//this will be null in cases where the value is contained in the character stream
					tagContent = _xmlReader.getAttributeValue(null, "text");
				}
				break;
			case XMLStreamConstants.CHARACTERS :
				if(_xmlReader.getText().trim().length() > 0) {
					tagContent = _xmlReader.getText();
				}
				break;
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected AttributeMapping ParseAttributeHeader(String attributeClass) throws IOException, XMLStreamException {
		AttributeMapping attMapping = new AttributeMapping();

		CyTable cyTable;
		if(attributeClass.equalsIgnoreCase(GEXFAttribute.NODE)) {
			cyTable = _cyNetwork.getDefaultNodeTable();
		} else if(attributeClass.equalsIgnoreCase(GEXFAttribute.EDGE)) {
			cyTable = _cyNetwork.getDefaultEdgeTable();
		} else {
			throw new InvalidClassException(attributeClass);
		}
		
		String xId = null;
		String xTitle = null;
		String xType = null;
		
		String xDefault = null;
		Boolean hasDefault = false;
		
		while(_xmlReader.hasNext()) {
			int event = _xmlReader.next();

			switch(event) {
			case XMLStreamConstants.START_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTRIBUTE)) {
					xId = _xmlReader.getAttributeValue(null, GEXFAttribute.ID).trim();
					xTitle = _xmlReader.getAttributeValue(null, GEXFAttribute.TITLE).trim();
					xType = _xmlReader.getAttributeValue(null, GEXFAttribute.TYPE).trim();
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.DEFAULT)) {
					hasDefault = true;
					xDefault = _xmlReader.getAttributeValue(null, "text");
				}
				break;
			case XMLStreamConstants.CHARACTERS :
				if(hasDefault && xDefault == null) {
					xDefault = _xmlReader.getText();
				}
				break;
			case XMLStreamConstants.END_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTRIBUTES)) {
					return attMapping;
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTRIBUTE)) {
					Class type = GetClass(xType);
					
					if(cyTable.getColumn(xTitle)==null) {
						if(!hasDefault) {
							if(!type.isArray()) {
								cyTable.createColumn(xTitle, type, false);
							}
							else {
								cyTable.createListColumn(xTitle, type.getComponentType(), false);
							}
						}
						else {
							if(!type.isArray()) {
								cyTable.createColumn(xTitle, type, false, GenericParse(xDefault.trim(), type));
							}
							else {
								cyTable.createListColumn(xTitle, type.getComponentType(), false, ParseArray(xDefault.trim(), type.getComponentType()));
							}
						}
					}
					
					attMapping.Id.put(xId, xTitle);
					attMapping.Type.put(xId, xType);
					
					
					
					//reset the storage
					xId = null;
					xTitle = null;
					xType = null;
					
					hasDefault = false;
					xDefault = null;
				}
				break;
			}
		}
		
		throw new InvalidClassException("Missing AttributeHeader tags");
	}
	
	protected ArrayList<CyNode> ParseNodes(CyNode cyNodeParent) throws IOException, XMLStreamException {
		
		ArrayList<CyNode> cyNodes = new ArrayList<CyNode>();
		CyNode cyNode = null;
		Hashtable<String, ArrayList<CyNode>> parentIdToChildrenLookup = new Hashtable<String, ArrayList<CyNode>>();
		
		while(_xmlReader.hasNext()) {
			int event = _xmlReader.next();

			switch(event) {
			case XMLStreamConstants.END_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFNode.NODES)) {
					if (cyNodeParent == null) {
						Enumeration<String> pidEnumeration = parentIdToChildrenLookup.keys();
						while(pidEnumeration.hasMoreElements()) {
							String pid = pidEnumeration.nextElement();
							CyNode parentNode = _cyNetwork.getNode(_idMapping.get(pid));
							
							_cyGroupFactory.createGroup(_cyNetwork, parentNode, parentIdToChildrenLookup.get(pid), null, true);
						}
					}
					return cyNodes;
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFNode.NODE)) {
					cyNodes.add(cyNode);
					cyNode = null;
				}
				break;
			case XMLStreamConstants.START_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFNode.NODE)) {
					String xId = _xmlReader.getAttributeValue(null, GEXFNode.ID).trim();
					String xLabel = _xmlReader.getAttributeValue(null, GEXFNode.LABEL).trim();
					String xPid = _xmlReader.getAttributeValue(null, GEXFNode.PID);
					if (xPid != null) {
						xPid = xPid.trim();
					}
					
					if(!_idMapping.containsKey(xId)) {
						cyNode = _cyNetwork.addNode();
						_idMapping.put(xId, cyNode.getSUID());
					}
					else {
						cyNode = _cyNetwork.getNode(_idMapping.get(xId));
					}
					
					if(xPid != null) {
						if(!parentIdToChildrenLookup.containsKey(xPid)) {
							parentIdToChildrenLookup.put(xPid, new ArrayList<CyNode>());
						}
						ArrayList<CyNode> childrenForPid = (ArrayList<CyNode>)parentIdToChildrenLookup.get(xPid);
						childrenForPid.add(cyNode);
					}					
					
					_cyNetwork.getRow(cyNode).set(CyNetwork.NAME, xLabel);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFNode.NODES)) { 
					ArrayList<CyNode> nodesToAddToGroup = ParseNodes(cyNode);
					if (cyNode != null) {
						_cyGroupFactory.createGroup(_cyNetwork, cyNode, nodesToAddToGroup, null, true);
					}
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTVALUES)) {
					ParseAttributes(new CyIdentifiable[] {cyNode}, _attNodeMapping);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.COLOR)) {
					int red = Integer.parseInt(_xmlReader.getAttributeValue(null, GEXFViz.RED).trim());
					int green = Integer.parseInt(_xmlReader.getAttributeValue(null, GEXFViz.GREEN).trim());
					int blue = Integer.parseInt(_xmlReader.getAttributeValue(null, GEXFViz.BLUE).trim());
					int alpha = GetElementAttributes().contains(GEXFViz.ALPHA) ? (int)(255 * Float.parseFloat(_xmlReader.getAttributeValue(null, GEXFViz.ALPHA).trim())) : 255;
					Color color = new Color(red, green, blue);
					
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_COLOR, ConvertColorToHex(color));
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_TRANSPARENCY, alpha);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.POSITION)) {
					List<String> elementAttributes = GetElementAttributes();
					
					double x = elementAttributes.contains(GEXFViz.X) ? Double.parseDouble(_xmlReader.getAttributeValue(null, GEXFViz.X).trim()) : 0.0d;
					double y = elementAttributes.contains(GEXFViz.Y) ? -Double.parseDouble(_xmlReader.getAttributeValue(null, GEXFViz.Y).trim()) : 0.0d;
					double z = elementAttributes.contains(GEXFViz.Z) ? Double.parseDouble(_xmlReader.getAttributeValue(null, GEXFViz.Z).trim()) : 0.0d;
					
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_X, x);
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_Y, y);
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_Z, z);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.SIZE)) {
					double value = Double.parseDouble(_xmlReader.getAttributeValue(null, GEXFViz.VALUE).trim());
					
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_SIZE, value);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.SHAPE)) {
					String value = _xmlReader.getAttributeValue(null, GEXFViz.VALUE).trim();
					
					_cyNetwork.getRow(cyNode).set(GEXFViz.ATT_SHAPE, ConvertNodeShape(value));
				}
				
				break;
			}
		}
		
		throw new InvalidClassException("Missing Node tags");
	}
	
	protected void ParseEdges(String defaultEdgeType) throws IOException, XMLStreamException {
		
		CyEdge cyEdge = null;
		CyEdge cyEdgeReverse = null;
		
		while(_xmlReader.hasNext()) {
			int event = _xmlReader.next();

			switch(event) {
			case XMLStreamConstants.END_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFEdge.EDGES)) {
					return;
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFEdge.EDGE)) {
					cyEdge = null;
					cyEdgeReverse = null;
				}
				
				break;
			case XMLStreamConstants.START_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFEdge.EDGE)) {
					List<String> edgeElementAttributes = GetElementAttributes();
					
					//String xId = _xmlReader.getAttributeValue(null, GEXFEdge.ID).trim();
					String xSource = _xmlReader.getAttributeValue(null, GEXFEdge.SOURCE).trim();
					String xTarget = _xmlReader.getAttributeValue(null, GEXFEdge.TARGET).trim();
					String xEdgeType = edgeElementAttributes.contains(GEXFEdge.EDGETYPE) ? _xmlReader.getAttributeValue(null, GEXFEdge.EDGETYPE).trim() : defaultEdgeType;
					String xEdgeWeight = edgeElementAttributes.contains(GEXFEdge.WEIGHT) ? _xmlReader.getAttributeValue(null, GEXFEdge.WEIGHT).trim() : "";
					
					if(!_idMapping.containsKey(xSource)) {
						CyNode cyNode = _cyNetwork.addNode();
						_idMapping.put(xSource, cyNode.getSUID());
					}
					
					if(!_idMapping.containsKey(xTarget)) {
						CyNode cyNode = _cyNetwork.addNode();
						_idMapping.put(xTarget, cyNode.getSUID());
					}
					
					cyEdge = _cyNetwork.addEdge(_cyNetwork.getNode(_idMapping.get(xSource)), _cyNetwork.getNode(_idMapping.get(xTarget)), IsDirected(xEdgeType));
					cyEdgeReverse = IsBiDirectional(xEdgeType) ? _cyNetwork.addEdge(_cyNetwork.getNode(_idMapping.get(xTarget)), _cyNetwork.getNode(_idMapping.get(xSource)), IsDirected(xEdgeType)) : null;
					
					_cyNetwork.getRow(cyEdge).set(GEXFEdge.EDGETYPE, xEdgeType);
					if(cyEdgeReverse!=null) _cyNetwork.getRow(cyEdgeReverse).set(GEXFEdge.EDGETYPE, xEdgeType);
					
					if(edgeElementAttributes.contains(GEXFEdge.WEIGHT)) {
						_cyNetwork.getRow(cyEdge).set(GEXFEdge.WEIGHT, Double.parseDouble(xEdgeWeight));
						if(cyEdgeReverse!=null) _cyNetwork.getRow(cyEdgeReverse).set(GEXFEdge.WEIGHT, Double.parseDouble(xEdgeWeight));
					}
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTVALUES)) {
					ParseAttributes(new CyIdentifiable[] {cyEdge, cyEdgeReverse}, _attEdgeMapping);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.COLOR)) {
					int red = Integer.parseInt(_xmlReader.getAttributeValue(null, GEXFViz.RED).trim());
					int green = Integer.parseInt(_xmlReader.getAttributeValue(null, GEXFViz.GREEN).trim());
					int blue = Integer.parseInt(_xmlReader.getAttributeValue(null, GEXFViz.BLUE).trim());
					int alpha = GetElementAttributes().contains(GEXFViz.ALPHA) ? (int)(255 * Float.parseFloat(_xmlReader.getAttributeValue(null, GEXFViz.ALPHA).trim())) : 255;
					Color color = new Color(red, green, blue);
					
					_cyNetwork.getRow(cyEdge).set(GEXFViz.ATT_COLOR, ConvertColorToHex(color));
					_cyNetwork.getRow(cyEdge).set(GEXFViz.ATT_TRANSPARENCY, alpha);
					
					if(cyEdgeReverse!=null) _cyNetwork.getRow(cyEdgeReverse).set(GEXFViz.ATT_SHAPE, ConvertColorToHex(color));
					if(cyEdgeReverse!=null) _cyNetwork.getRow(cyEdgeReverse).set(GEXFViz.ATT_TRANSPARENCY, alpha);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.THICKNESS)) {
					double value = Double.parseDouble(_xmlReader.getAttributeValue(null, GEXFViz.VALUE).trim());
					
					_cyNetwork.getRow(cyEdge).set(GEXFViz.ATT_THICKNESS, value);
					if(cyEdgeReverse!=null) _cyNetwork.getRow(cyEdgeReverse).set(GEXFViz.ATT_THICKNESS, value);
				}
				else if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFViz.SHAPE)) {
					String value = _xmlReader.getAttributeValue(null, GEXFViz.VALUE).trim();

					_cyNetwork.getRow(cyEdge).set(GEXFViz.ATT_SHAPE, ConvertEdgeShape(value));
					if(cyEdgeReverse!=null) _cyNetwork.getRow(cyEdgeReverse).set(GEXFViz.ATT_SHAPE, ConvertEdgeShape(value));
				}
				
				break;
			}
		}
		
		throw new InvalidClassException("Missing Edge tags");
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void ParseAttributes(CyIdentifiable[] cyIdentifiables, AttributeMapping attMapping) throws IOException, XMLStreamException {
		
		while(_xmlReader.hasNext()) {
			int event = _xmlReader.next();

			switch(event) {
			case XMLStreamConstants.END_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTVALUES)) {
					return;
				}
				break;
			case XMLStreamConstants.START_ELEMENT :
				if(_xmlReader.getLocalName().equalsIgnoreCase(GEXFAttribute.ATTVALUE)) {
					String xFor = _xmlReader.getAttributeValue(null, GEXFAttribute.FOR);
					if(xFor==null) {xFor = _xmlReader.getAttributeValue(null, GEXFAttribute.ID);}
					xFor = xFor.trim();					
					String xValue = _xmlReader.getAttributeValue(null, GEXFAttribute.VALUE).trim();
					
					Class type = GetClass(attMapping.Type.get(xFor));
					if(!type.isArray()) {
						for(CyIdentifiable cyIdentifiable : cyIdentifiables) {
							if(cyIdentifiable!=null) {_cyNetwork.getRow(cyIdentifiable).set(attMapping.Id.get(xFor), GenericParse(xValue, type));}
						}
					}
					else {
						for(CyIdentifiable cyIdentifiable : cyIdentifiables) {
							if(cyIdentifiable!=null) {_cyNetwork.getRow(cyIdentifiable).set(attMapping.Id.get(xFor), ParseArray(xValue, type));}
						}
					}
				}
				break;
			}
		}
		
		
		throw new InvalidClassException("Missing Attribute Value tags");
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T GenericParse(String value, Class<T> type) throws InvalidClassException {
		if(type.equals(Integer.class)) {
			return (T)(Integer)Integer.parseInt(value);
		}
		else if(type.equals(Long.class)) {
			return (T)(Long)Long.parseLong(value);
		}
		else if(type.equals(Double.class)) {
			return (T)(Double)Double.parseDouble(value);
		}
		else if(type.equals(Boolean.class)) {
			return (T)(Boolean)Boolean.parseBoolean(value);
		}
		else if(type.equals(String.class)) {
			return (T)value;
		}
		else {
			throw new InvalidClassException(type.getName());
		}
	}
	
	protected abstract <T> List<T> ParseArray(String array, Class<T> type) throws IOException;
	
	@SuppressWarnings("rawtypes")
	protected Class GetClass(String type) throws InvalidClassException {
		if(type.equalsIgnoreCase(DataTypes.INTEGER)) {
			return Integer.class;
		}
		if(type.equalsIgnoreCase(DataTypes.LONG)) {
			return Long.class;
		}
		else if(type.equalsIgnoreCase(DataTypes.FLOAT)) {
			//float not supported
			return Double.class;
		}
		else if(type.equalsIgnoreCase(DataTypes.DOUBLE)) {
			return Double.class;
		}
		else if(type.equalsIgnoreCase(DataTypes.BOOLEAN)) {
			return Boolean.class;
		}
		else if(type.equalsIgnoreCase(DataTypes.STRING)) {
			return String.class;
		}
		else if(type.equalsIgnoreCase(DataTypes.LISTINTEGER)) {
			return Integer[].class;
		}
		else if(type.equalsIgnoreCase(DataTypes.LISTLONG)) {
			return Long[].class;
		}
		else if(type.equalsIgnoreCase(DataTypes.LISTFLOAT)) {
			return Double[].class;
		}
		else if(type.equalsIgnoreCase(DataTypes.LISTDOUBLE)) {
			return Double[].class;
		}
		else if(type.equalsIgnoreCase(DataTypes.LISTBOOLEAN)) {
			return Boolean[].class;
		}
		else if(type.equalsIgnoreCase(DataTypes.LISTSTRING)) {
			return String[].class;
		}
		else {
			throw new InvalidClassException(type);
		}
	}
	
	protected List<String> GetElementAttributes() {
		List<String> attributes = new ArrayList<String>();
		
		int count = _xmlReader.getAttributeCount();
		for(int i=0; i<count; i++) {
			attributes.add(_xmlReader.getAttributeLocalName(i));
		}
		
		return attributes;
	}
	
	protected String ConvertNodeShape(String shape) {
		if(shape.equalsIgnoreCase(GEXFViz.DISC)) {
			return NodeShapeVisualProperty.ELLIPSE.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.SQUARE)) {
			return NodeShapeVisualProperty.RECTANGLE.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.TRIANGLE)) {
			return NodeShapeVisualProperty.TRIANGLE.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.DIAMOND)) {
			return NodeShapeVisualProperty.DIAMOND.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.IMAGE)) {
			return NodeShapeVisualProperty.OCTAGON.getSerializableString();
		}
		else {
			return NodeShapeVisualProperty.ELLIPSE.getSerializableString();
		}
	}
	
	protected String ConvertEdgeShape(String shape) {
		if(shape.equalsIgnoreCase(GEXFViz.SOLID)) {
			return LineTypeVisualProperty.SOLID.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.DOTTED)) {
			return LineTypeVisualProperty.DOT.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.DASHED)) {
			return LineTypeVisualProperty.EQUAL_DASH.getSerializableString();
		}
		else if(shape.equalsIgnoreCase(GEXFViz.DOUBLE)) {
			return "PARALLEL_LINES";
		}
		else {
			return LineTypeVisualProperty.SOLID.getSerializableString();
		}
	}
	
	protected abstract Boolean IsDirected(String direction);
	
	protected abstract Boolean IsBiDirectional(String direction);
	
	private String ConvertColorToHex(Color color) {
		String hexColor = Integer.toHexString(color.getRGB() & 0xffffff);
		  if (hexColor.length() < 6) {
		    hexColor = "000000".substring(0, 6 - hexColor.length()) + hexColor;
		  }
		  return "#" + hexColor;
	}
}