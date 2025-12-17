package com.example.dynamic.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marshaller för att konvertera objekt till XML
 */
public class XmlMarshaller {
    private static final Logger logger = LoggerFactory.getLogger(XmlMarshaller.class);

    private static final XmlMapper xmlMapper = new XmlMapper();

    /**
     * Marshaller ett objekt till XML-sträng
     * @param obj Objektet att marshalla
     * @return XML-sträng
     * @throws XmlMarshallingException om marshalling misslyckas
     */
    public String marshallToXml(Object obj) throws XmlMarshallingException {
        if (obj == null) {
            throw new XmlMarshallingException("Objekt kan inte vara null");
        }

        try {
            logger.debug("Marshalling objekt av typ {} till XML", obj.getClass().getName());
            String xmlString = xmlMapper.writeValueAsString(obj);
            logger.debug("XML-marshalling framgångsrikt");
            return xmlString;
        } catch (Exception e) {
            logger.error("Fel vid XML-marshalling", e);
            throw new XmlMarshallingException("Misslyckades att marshalla till XML", e);
        }
    }

    /**
     * Marshaller ett objekt till XML med formattering (för läsbarhet)
     */
    public String marshallToXmlPretty(Object obj) throws XmlMarshallingException {
        if (obj == null) {
            throw new XmlMarshallingException("Objekt kan inte vara null");
        }

        try {
            logger.debug("Marshalling objekt av typ {} till formaterad XML", obj.getClass().getName());
            String xmlString = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            logger.debug("Formaterad XML-marshalling framgångsrikt");
            return xmlString;
        } catch (Exception e) {
            logger.error("Fel vid formaterad XML-marshalling", e);
            throw new XmlMarshallingException("Misslyckades att marshalla till formaterad XML", e);
        }
    }

    /**
     * Custom exception för XML-marshalling-fel
     */
    public static class XmlMarshallingException extends Exception {
        public XmlMarshallingException(String message) {
            super(message);
        }

        public XmlMarshallingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
