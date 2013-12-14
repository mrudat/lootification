package LeveledListInjector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
public abstract class SetupData {

    protected String errorHeader;
    protected String curFile;
    protected String curMod;
    protected String curRecord;
    
    private final HashMap<String, RecordData> dataMap;
    private final HashMap armorMap;
    private final HashMap weaponMap;
    private final Set tieredSet;
    protected final Enum logKey; 

    private enum types {

        armor(GRUP_TYPE.ARMO) {
        },
        weapon(GRUP_TYPE.WEAP) {
        },
        leveledList(GRUP_TYPE.LVLI) {
        },
        outfit(GRUP_TYPE.OTFT) {
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

    enum tags {

        match(new GRUP_TYPE[]{GRUP_TYPE.ARMO, GRUP_TYPE.WEAP}) {
            @Override
            public void parse(SetupData setup, Element e, RecordData r) throws ParseException {
                boolean base = e.getAttribute("type").equalsIgnoreCase("base");
                String s = e.getTextContent();
                if (s.contentEquals("")) {
                    //no match content
                    throw new ParseException("Match tag empty");
                }
                r.addMatch(base, s);
                if (base) {
                    String keyID = e.getAttribute("key");
                    if (!keyID.contentEquals("")) {
                        GRUP_TYPE g = r.getType();
                        switch (g) {
                            case ARMO:
                                setup.armorMap.put(s, keyID);
                                break;
                            case WEAP:
                                setup.weaponMap.put(s, keyID);
                                break;
                            default:
                        }
                    }

                }

            }
        ;
        },
        outfit(new GRUP_TYPE[]{GRUP_TYPE.ARMO}) {
            @Override
            public void parse(SetupData setup, Element e, RecordData r) throws ParseException {
                String s = e.getTextContent();
                if (s.contentEquals("")) {
                    //no outfit content
                    throw new ParseException("Outfit tag empty");
                }
                r.addOutfit(s);
            }
        ;
        },
        
        set(new GRUP_TYPE[]{GRUP_TYPE.ARMO, GRUP_TYPE.LVLI, GRUP_TYPE.OTFT}) {
            @Override
            public void parse(SetupData setup, Element e, RecordData r) throws ParseException {
                String name = e.getAttribute("set_name");
                if (name.contentEquals("")) {
                    //no set name
                    throw new ParseException("Set name empty");
                }
                switch (r.getType()) {
                    case LVLI:
                    case OTFT:
                        r.addSet(true, name, -1);
                        setup.tieredSet.add(name);
                        break;
                    case ARMO:
                        String s = e.getTextContent();
                        if (s.contentEquals("")) {
                            //no set content
                            throw new ParseException("Set content empty");
                        }
                        int i = Integer.parseInt(s);
                        r.addSet(false, name, i);
                        break;
                    default:
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

        public abstract void parse(SetupData setup, Element e, RecordData r) throws ParseException;
    };

    public static class ParseException extends Exception {

        ParseException(String s) {
            super(s);
        }
    }
    
    public SetupData(HashMap d, HashMap a, HashMap w, Set t, Enum e){
        dataMap = d;
        armorMap = a;
        weaponMap = w;
        tieredSet = t;
        logKey = e;
    }

    abstract void setupStart(String logName) throws Exception;

    protected void parseMaster(String fileName) throws Exception {
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
            SPGlobal.logSpecial(logKey, errorHeader, logError);
            throw e;
        }

    }

    private boolean parseMod(Node nMod) {
        Element mod = (Element) nMod;
        String modName = mod.getAttribute("ModName");
        if (modName.contentEquals("")) {
            String logError = curFile + " Mod Entry missing ModName";
            SPGlobal.logSpecial(logKey, errorHeader, logError);
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

    private boolean parseRecord(Node recNode) {
        Element eRecord = (Element) recNode;
        String recordEDID = eRecord.getAttribute("EDID");
        if (recordEDID.contentEquals("")) {
            String logError = curFile + ", " + curMod + ", " + "Record missing EDID";
            SPGlobal.logSpecial(logKey, errorHeader, logError);
            return false;
        }
        String modMaster = eRecord.getAttribute("master");
        if (modMaster.contentEquals("")) {
            modMaster = curMod;
        }
        String fullID = /*modMaster + "_" +*/ recordEDID;
        curRecord = fullID;
        RecordData theData = dataMap.get(fullID);

        String recordType = eRecord.getAttribute("type");
        if (recordType.contentEquals("")) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Record missing type";
            SPGlobal.logSpecial(logKey, errorHeader, logError);
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
            SPGlobal.logSpecial(logKey, errorHeader, logError);
            return false;
        }

        return true;
    }

    private boolean parseTag(Element eRecSub, RecordData theData) {
        String tagName = eRecSub.getTagName();
        try {
            tags t = tags.valueOf(tagName);
            if (!(t.allowedTag(theData.getType()))) {
                String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Tag not allowed for this element type. Tag: " + tagName;
                SPGlobal.logSpecial(logKey, errorHeader, logError);
                return false;
            }
            t.parse(this, eRecSub, theData);
        } catch (EnumConstantNotPresentException e) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + "Record Has invalid tag: " + tagName;
            SPGlobal.logSpecial(logKey, errorHeader, logError);
            return false;
        } catch (ParseException e) {
            String logError = curFile + ", " + curMod + ", " + ", " + curRecord + ", " + tagName + ", " + e.getMessage();
            SPGlobal.logSpecial(logKey, errorHeader, logError);
            return false;
        }

        return true;
    }
}
