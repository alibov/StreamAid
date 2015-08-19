package experiment;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

public class XMLutils {
  public static String getAttribute(final Node n, final String attrName, final String defaultValue) {
    if (n.getAttributes().getNamedItem(attrName) != null) {
      return ((Attr) n.getAttributes().getNamedItem(attrName)).getValue();
    }
    return defaultValue;
  }
  
  public static String nodeToString(final Node node) {
    final TransformerFactory transFactory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = transFactory.newTransformer();
      final StringWriter buffer = new StringWriter();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(node), new StreamResult(buffer));
      buffer.append("\n");
      return buffer.toString();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Node stringToNode(final String confXmlString) {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(confXmlString.getBytes()))
          .getFirstChild();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Node findFirstChild(final Node n, final String name) {
    for (int i = 0; i < n.getChildNodes().getLength(); i++) {
      if (n.getChildNodes().item(i).getNodeName().equals(name)) {
        return n.getChildNodes().item(i);
      }
    }
    throw new IllegalStateException("no " + name + " children found in: " + n.getNodeName());
  }
  
  public static ArrayList<Node> findAllChildren(final Node n, final String name) {
    final ArrayList<Node> retVal = new ArrayList<Node>();
    for (int i = 0; i < n.getChildNodes().getLength(); i++) {
      if (n.getChildNodes().item(i).getNodeName().equals(name)) {
        retVal.add(n.getChildNodes().item(i));
      }
    }
    if (retVal.isEmpty()) {
      throw new IllegalStateException("no " + name + " children found in: " + n.getNodeName());
    }
    return retVal;
  }
}
