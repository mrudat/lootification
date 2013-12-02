package LeveledListInjector;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import skyproc.*;

/**
 *
 * @author David
 */
public class SetupData {

    private enum types {

        armor {
            @Override
            public GRUP_TYPE getType() {
                return GRUP_TYPE.ARMO;
            }
        ;
        },
        weapon {
            @Override
            public GRUP_TYPE getType() {
                return GRUP_TYPE.WEAP;
            }
        ;

        };

        public abstract GRUP_TYPE getType();
    };

    private enum tags {

        match {
            @Override
            public void parse(Element e, RecordData r) {
                boolean base = e.getAttribute("type").equalsIgnoreCase("base");
                String s = e.getTextContent();
                r.addMatch(base, s);
            }
        ;
        },
        outfit {
            @Override
            public void parse(Element e, RecordData r) {
                String s = e.getTextContent();
                r.addOutfit(s);
            }
        ;
        },
        
        set {
            @Override
            public void parse(Element e, RecordData r) {
                if (r.getType() == GRUP_TYPE.LVLI) {
                    String name = e.getAttribute("set_name");
                    r.addSet(true, name, -1);
                } else {
                    String name = e.getAttribute("set_name");
                    int i = Integer.parseInt(e.getTextContent());
                    r.addSet(false, name, i);
                }
            }
        ;

        };

        public abstract void parse(Element e, RecordData r);
    };

    static void setupStart() throws Exception {
    }

    static void parseMaster(String fileName) throws Exception {
        File master_File = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document master_doc = dBuilder.parse(master_File);
        master_doc.getDocumentElement().normalize();

        NodeList mList = master_doc.getElementsByTagName("mod");
        for (int i = 0; i < mList.getLength(); i++) {
            Node nMod = mList.item(i);
            Element mod = (Element) nMod;
            String modName = mod.getAttribute("ModName");
            NodeList items = ((Document) nMod).getElementsByTagName("record");
            for (int j = 0; j < items.getLength(); j++) {
                Node rec = items.item(j);
                if (rec.getNodeType() == Node.ELEMENT_NODE) {

                    if (!parseRecord(rec, modName)) {
                        //alert xml bad
                    }

                }
            }

        }

    }

    private static boolean parseRecord(Node recNode, String modName) {
        Element eRecord = (Element) recNode;
        String recordEDID = eRecord.getAttribute("EDID");
        if (recordEDID.contentEquals("")) {
            return false;
        }
        RecordData theData = LeveledListInjector.parsedData.get(recordEDID);
        if (theData == null) {
            theData = new RecordData(recordEDID, modName);
            LeveledListInjector.parsedData.put(recordEDID, theData);
        } else {
            if ((theData.getMod().equalsIgnoreCase(modName)) == false) {
                String logError = recordEDID + " listed in " + modName + " but is already listed in " + theData.getMod();
                SPGlobal.log("Parse xml masters", logError);
                return false;
            }
        }

        String recordType = eRecord.getAttribute("type");
        if (recordType.contentEquals("")) {
            return false;
        }
        try {
            types t = types.valueOf(recordType);
            theData.setType(t.getType());
            NodeList recSubNodesList = recNode.getChildNodes();
            for (int k = 0; k < recSubNodesList.getLength(); k++) {
                Element eRecSub = (Element) recSubNodesList.item(k);
                if (eRecSub.getNodeType() == Node.ELEMENT_NODE) {
                    if (parseTag(eRecSub, theData) == false) {
                        //xml bad tag
                    }
                }
            }
        } catch (Exception e) {
            String logError = recordEDID + " has invalid type " + recordType;
            SPGlobal.log("Parse xml masters", logError);
            return false;
        }

        return true;
    }

    private static boolean parseTag(Element eRecSub, RecordData theData) {
        String tagName = eRecSub.getTagName();
        tags.valueOf(tagName).parse(eRecSub, theData);
        return true;
    }
}
