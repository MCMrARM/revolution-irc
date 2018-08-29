package io.mrarm.irc.upnp.rpc;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidParameterException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.mrarm.irc.upnp.XMLParseHelper;

public abstract class UPnPRemoteCall {

    public static final String NS_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";


    protected Document doSend(URL serviceEndpoint) throws IOException, SAXException,
            TransformerException, UPnPRPCError {
        if (!validate())
            throw new InvalidParameterException("Validation of the request failed");
        Document doc = buildDocument();

        HttpURLConnection connection = (HttpURLConnection) serviceEndpoint.openConnection();
        connection.setDoOutput(true);
        connection.addRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        connection.addRequestProperty("SOAPAction", "\"" + getSOAPAction() + "\"");
        Log.d("UPnPRemoteCall", "Request action: " + getSOAPAction());
        connection.setRequestProperty("Connection", "close");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        //transformer.transform(new DOMSource(doc), new StreamResult(connection.getOutputStream()));
        StringWriter xmlWriter = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(xmlWriter));
        String xmlBodyString = xmlWriter.toString();
        Log.d("UPnPRemoteCall", "Request body: " + xmlBodyString);
        byte[] xmlBodyBytes = xmlBodyString.getBytes("UTF-8");

        connection.setFixedLengthStreamingMode(xmlBodyBytes.length);
        connection.getOutputStream().write(xmlBodyBytes);
        Log.d("UPnPRemoteCall", "Response status: " + connection.getResponseCode());

        InputStream stream = connection.getErrorStream() != null ? connection.getErrorStream()
                : connection.getInputStream();
        ByteArrayOutputStream responseBytesBuilder = new ByteArrayOutputStream();
        byte[] buf = new byte[1024 * 4];
        int n;
        while ((n = stream.read(buf)) >= 0) {
            responseBytesBuilder.write(buf, 0, n);
        }
        byte[] responseBytes = responseBytesBuilder.toByteArray();
        Log.d("UPnPRemoteCall", "Response: " +
                new String(responseBytes, "UTF-8"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document ret;
        try {
            ret = factory.newDocumentBuilder().parse(new ByteArrayInputStream(responseBytes));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        if (!ret.getDocumentElement().getLocalName().equals("Envelope"))
            throw new IOException("Invalid reply");
        Element body = XMLParseHelper.findChildElement(ret.getDocumentElement(), "Body");
        Element fault = XMLParseHelper.findChildElement(body, "Fault");
        if (fault != null)
            throw new UPnPRPCError(fault);
        return ret;
    }

    protected abstract boolean validate();

    protected abstract String getSOAPAction();


    /* XML building */

    public Document buildDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc;
        try {
            doc = factory.newDocumentBuilder().getDOMImplementation()
                    .createDocument(NS_SOAP, "s:Envelope", null);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        doc.getDocumentElement().setAttribute("s:encodingStyle",
                "http://schemas.xmlsoap.org/soap/encoding/");
        buildEnvelope(doc.getDocumentElement());
        return doc;
    }


    protected void buildEnvelope(Element envelope) {
        Document document = envelope.getOwnerDocument();
        Element body = document.createElementNS(NS_SOAP, "s:Body");
        envelope.appendChild(body);
        buildBody(body);
    }

    protected void buildBody(Element body) {
        Document document = body.getOwnerDocument();
        Element request = createRequest(document);
        body.appendChild(request);
    }

    protected abstract Element createRequest(Document document);


    protected static Element addArgumentNode(Element container, String name, String value) {
        Element ret = container.getOwnerDocument().createElement(name);
        ret.setTextContent(value);
        container.appendChild(ret);
        return ret;
    }

}
