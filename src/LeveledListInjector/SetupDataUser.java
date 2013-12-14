/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
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
 * @author David
 */
public class SetupDataUser extends SetupData {

    private HashMap<String, Node> modsMap;
    private Document theDoc;
    private Document newDoc;

    SetupDataUser(HashMap d, HashMap a, HashMap w, Set t, Enum e) {
        super(d, a, w, t, e);
    }

    @Override
    public void setupStart(String logName) throws Exception {
        SPGlobal.newSpecialLog(logKey, logName);
        errorHeader = "Parse xml data master";
        parseMaster("lli_data_master.xml");
        errorHeader = "Parse xml data custom";
        parseMaster("lli_data_custom.xml");
    }

    public void panelChangesToXML() throws Exception {
        modsMap = new HashMap<>();

        getModNodes();
        panelRecordsToNodes();
        writeXML();
    }

    private void getModNodes() throws Exception {
        String fileName = "lli_data_custom.xml";
        try {
            File master_File = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document master_doc = dBuilder.parse(master_File);
            master_doc.getDocumentElement().normalize();
            theDoc = master_doc;
            newDoc = dBuilder.newDocument();

            Element rootElement = newDoc.createElement("lli_user");
            newDoc.appendChild(rootElement);

            NodeList mList = master_doc.getElementsByTagName("mod");
            for (int i = 0; i < mList.getLength(); i++) {
                Node nMod = mList.item(i);
                Element mod = (Element) nMod;
                String modName = mod.getAttribute("ModName");
                if (modName.contentEquals("")) {
                    String logError = fileName + " Mod Entry missing ModName";
                    SPGlobal.logSpecial(logKey, errorHeader, logError);
                    throw new ParseException(logError);
                }
                modsMap.put(fileName, nMod);

            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            String logError = fileName + ", " + e.getMessage();
            SPGlobal.logSpecial(logKey, errorHeader, logError);
            throw e;
        }
    }

    private void panelRecordsToNodes() throws Exception {
        for (RecordData rec : LeveledListInjector.modPanelData.values()) {
            Node modNode = modsMap.get(rec.getMod());
            if (modNode == null) {
                Element modElement = theDoc.createElement("mod");
                modElement.setAttribute("ModName", rec.getMod());
                modNode = modElement;
            }
            Node modDupe = modNode.cloneNode(false);
            newDoc.adoptNode(modDupe);

            NodeList recNodes = ((Element) modNode).getElementsByTagName("record");
            Node theNode = null;
            for (int i = 0; i < recNodes.getLength(); i++) {
                Element recEl = (Element) recNodes.item(i);
                String EDID = recEl.getAttribute("EDID");
                if (EDID.equalsIgnoreCase(rec.getEDID())) {
                    String elType = recEl.getAttribute("type");
                    if (!elType.contentEquals(rec.getTypeFriendly())) {
                        String error = "lli_data_custom.xml already contains " + rec.getEDID()
                                + " in mod " + ((Element) modNode).getAttribute("ModName") + " but has it declared as type "
                                + elType + " instead of " + rec.getTypeFriendly();
                        throw new ParseException(error);
                    }
                    String elMaster = recEl.getAttribute("master");
                    if (!elMaster.contentEquals(rec.getMod())) {
                        String error = "lli_data_custom.xml already contains " + rec.getEDID()
                                + " in mod " + ((Element) modNode).getAttribute("ModName") + " but has it declared as having master "
                                + elMaster + " instead of " + rec.getMod();
                        throw new ParseException(error);
                    }
                    theNode = recEl;
                }
            }
            if (theNode == null) {
                Element recElement = theDoc.createElement("mod");
                recElement.setAttribute("EDID", rec.getEDID());
                recElement.setAttribute("type", rec.getTypeFriendly());
                modNode.appendChild(recElement);
                theNode = recElement;
            }

            rec.toXML(theNode);

            Node nodeDupe = theNode.cloneNode(true);
            newDoc.adoptNode(nodeDupe);
        }
    }

    private void writeXML() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");

        DOMSource source = new DOMSource(newDoc);
        StreamResult result = new StreamResult(new File("new.xml"));
        transformer.transform(source, result);

        source = new DOMSource(theDoc);
        result = new StreamResult(new File("lli_data_custom.xml"));
        transformer.transform(source, result);
    }
}
