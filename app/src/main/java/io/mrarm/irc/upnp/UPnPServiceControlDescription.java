package io.mrarm.irc.upnp;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class UPnPServiceControlDescription {

    private List<Action> mActionList;

    public void loadFromUrl(String url) throws IOException, SAXException {
        URLConnection connection = new URL(url).openConnection();
        XMLReader reader;
        try {
            reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        reader.setContentHandler(new SAXRootParser(reader, this));
        reader.parse(new InputSource(connection.getInputStream()));
    }

    public List<Action> getActionList() {
        return mActionList;
    }

    public Action findAction(String name) {
        for (Action action : mActionList) {
            if (action.mName != null && action.mName.equals(name))
                return action;
        }
        return null;
    }


    public static class Action {

        private String mName;

        public String getName() {
            return mName;
        }

    }



    public static class SAXRootParser extends DefaultHandler {

        private UPnPServiceControlDescription mService;
        private XMLReader mReader;
        private boolean mParsingActionList;

        public SAXRootParser(XMLReader reader, UPnPServiceControlDescription service) {
            mReader = reader;
            mService = service;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (localName.equals("action")) {
                if (!mParsingActionList)
                    throw new SAXException("Invalid tree");
                Action child = new Action();
                mService.mActionList.add(child);
                mReader.setContentHandler(new SAXActionParser(mReader, this, child));
            } else if (localName.equals("actionList")) {
                if (mParsingActionList)
                    throw new SAXException("Invalid tree");
                mParsingActionList = true;
                mService.mActionList = new ArrayList<>();
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName.equals("actionList"))
                mParsingActionList = false;
        }
    }

    public static class SAXActionParser extends DefaultHandler {

        private Action mAction;
        private XMLReader mReader;
        private ContentHandler mParent;
        private StringBuilder mContent = new StringBuilder();
        private boolean mInArgumentList;

        public SAXActionParser(XMLReader reader, ContentHandler parent, Action action) {
            mReader = reader;
            mParent = parent;
            mAction = action;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {
            mContent.setLength(0);
            if (localName.equals("argumentList"))
                mInArgumentList = true;
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            mContent.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (localName.equals("argumentList"))
                mInArgumentList = false;
            else if (localName.equals("name") && !mInArgumentList)
                mAction.mName = mContent.toString();
            else if (localName.equals("action"))
                mReader.setContentHandler(mParent);
        }

    }

}
