package LeveledListInjector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import skyproc.*;

/**
 *
 * @author David Tynan
 */
public class SetupMasterData {

    private static String errorHeader;
    private static String curFile;
    private static String curMod;
    private static String curRecord;

    public static enum logKey {

        xmlMaster, xmlData;
    };

    private enum types {

        armor(GRUP_TYPE.ARMO) {
        },
        weapon(GRUP_TYPE.WEAP) {
        },
        leveledList(GRUP_TYPE.LVLI) {
        };
        GRUP_TYPE type;

        types(GRUP_TYPE g) {
            type = g;
        }

        public GRUP_TYPE getType() {
            return type;
        }
    ;

    };

    private enum tags {

        match(new GRUP_TYPE[]{GRUP_TYPE.ARMO, GRUP_TYPE.WEAP}) {
            @Override
            public void parse(Element e, RecordData r) throws ParseException {
                boolean base = e.getAttribute("type").equalsIgnoreCase("base");
                String s = e.getTextContent();
                if (s.contentEquals("")) {
                    //no match content
                    throw new ParseException("Match tag empty");
                }
                r.addMatch(base, s);
            }
        ;
        },
        outfit(new GRUP_TYPE[]{GRUP_TYPE.ARMO}) {
            @Override
            public void parse(Element e, RecordData r) throws ParseException {
                String s = e.getTextContent();
                if (s.contentEquals("")) {
                    //no outfit content
                    throw new ParseException("Outfit tag empty");
                }
                r.addOutfit(s);
            }
        ;
        },
        
        set(new GRUP_TYPE[]{GRUP_TYPE.ARMO, GRUP_TYPE.LVLI}) {
            @Override
            public void parse(Element e, RecordData r) throws ParseException {
                if (r.getType() == GRUP_TYPE.LVLI) {
                    String name = e.getAttribute("set_name");
                    if (name.contentEquals("")) {
                        //no set name
                        throw new ParseException("Set name empty");
                    }
                    r.addSet(true, name, -1);
                } else {
                    String name = e.getAttribute("set_name");
                    if (name.contentEquals("")) {
                        //no set name
                        throw new ParseException("Set name empty");
                    }
                    String s = e.getTextContent();
                    if (s.contentEquals("")) {
                        //no set content
                        throw new ParseException("Set tag empty");
                    }
                    int i = Integer.parseInt(s);
                    r.addSet(false, name, i);
                }
            }
        ;

        };

        tags(GRUP_TYPE[] g) {
            allowed = g;
        }

        public boolean allowedTag(GRUP_TYPE g) {
            if (Arrays.asList(allowed).contains(g)) {
                return true;
            } else {
                return false;
            }
        }
        GRUP_TYPE[] allowed;

        public abstract void parse(Element e, RecordData r) throws ParseException;
    };

    static class ParseException extends Exception {

        ParseException(String s) {
            super(s);
        }
    }

    static void setupStart() throws Exception {
        SPGlobal.newSpecialLog(logKey.xmlMaster, "xmlMasterImport");
        errorHeader = "Parse xml master";
        parseMaster("lli_defines_master.xml");
        errorHeader = "Parse xml master custom";
        parseMaster("lli_defines_custom.xml");
    }

    static void parseMaster(String fileName) throws Exception {
        curFile = fileName;
        try {
            File master_File = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document master_doc = dBuilder.parse(master_File);
            master_doc.getDocumentElement().normalize();

            NodeList mList = master_doc.getElementsByTagName("mod");
            for (int i = 0; i < mList.getLength(); i++) {
                Node nMod = mList.item(i);
                if (!parseMod(nMod)) {
                    //xml bad mod
                }


            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            String logError = curFile + ", " + e.getMessage();
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            throw e;
        }

    }

    private static boolean parseMod(Node nMod) {
        Element mod = (Element) nMod;
        String modName = mod.getAttribute("ModName");
        if (modName.contentEquals("")) {
            String logError = curFile + " Mod Entry missing ModName";
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            return false;
        }
        curMod = modName;
        NodeList items = ((Document) nMod).getElementsByTagName("record");
        for (int j = 0; j < items.getLength(); j++) {
            Node rec = items.item(j);
            if (rec.getNodeType() == Node.ELEMENT_NODE) {

                if (!parseRecord(rec)) {
                    //xml bad Record
                }

            }
        }
        return true;
    }

    private static boolean parseRecord(Node recNode) {
        Element eRecord = (Element) recNode;
        String recordEDID = eRecord.getAttribute("EDID");
        if (recordEDID.contentEquals("")) {
            String logError = curFile + ", " + curMod + ", " + "Record missing EDID";
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            return false;
        }
        String modMaster = eRecord.getAttribute("master");
        if (modMaster.contentEquals("")) {
            modMaster = curMod;
        }
        String fullID = modMaster + "_" + recordEDID;
        curRecord = fullID;
        RecordData theData = LeveledListInjector.parsedData.get(fullID);

        String recordType = eRecord.getAttribute("type");
        if (recordType.contentEquals("")) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Record missing type";
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            return false;
        }
        try {
            types t = types.valueOf(recordType);
            if (theData == null) {
                theData = new RecordData(recordEDID, modMaster, t.getType());
                LeveledListInjector.parsedData.put(fullID, theData);
            }

            NodeList recSubNodesList = recNode.getChildNodes();
            for (int k = 0; k < recSubNodesList.getLength(); k++) {
                Element eRecSub = (Element) recSubNodesList.item(k);
                if (eRecSub.getNodeType() == Node.ELEMENT_NODE) {
                    if (parseTag(eRecSub, theData) == false) {
                        //xml bad tag
                    }
                }
            }
        } catch (EnumConstantNotPresentException e) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Record has invalid type: " + recordType;
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            return false;
        }

        return true;
    }

    private static boolean parseTag(Element eRecSub, RecordData theData) {
        String tagName = eRecSub.getTagName();
        try {
            tags t = tags.valueOf(tagName);
            if (!(t.allowedTag(theData.getType()))) {
                String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Tag not allowed for this element type. Tag: " + tagName;
                SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
                return false;
            }
            t.parse(eRecSub, theData);
        } catch (EnumConstantNotPresentException e) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Record Has invalid tag: " + tagName;
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            return false;
        } catch (ParseException e) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + tagName + ", " + e.getMessage();
            SPGlobal.logSpecial(logKey.xmlMaster, errorHeader, logError);
            return false;
        }

        return true;
    }
}
