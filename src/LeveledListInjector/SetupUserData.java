/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
public class SetupUserData extends SetupData {
    
    SetupUserData(HashMap d, HashMap a, HashMap w, Set t, Enum e){
        super( d,  a,  w,  t,  e);
    }
    
    @Override
    public void setupStart(String logName) throws Exception {
        SPGlobal.newSpecialLog(logKey, logName);
        errorHeader = "Parse xml data master";
        parseMaster("lli_data_master.xml");
        errorHeader = "Parse xml data custom";
        parseMaster("lli_data_custom.xml");
    }
        
    /*File fXmlFile = new File("Lootification.xml");
     * DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
     * DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
     * Document doc = dBuilder.parse(fXmlFile);
     * doc.getDocumentElement().normalize();
     * 
     * NodeList mList = doc.getElementsByTagName("mod");
     * for (int i = 0; i < mList.getLength(); i++) {
     * Node nMod = mList.item(i);
     * Element mod = (Element) nMod;
     * LeveledListInjector.lootifiedMods.add(new Pair<>(mod.getAttribute("modName"), nMod));
     * }
     * 
     * File CustomXmlFile = new File("Custom.xml");
     * Document cDoc = dBuilder.parse(CustomXmlFile);
     * cDoc.getDocumentElement().normalize();
     * 
     * mList = cDoc.getElementsByTagName("mod");
     * for (int i = 0; i < mList.getLength(); i++) {
     * Node nMod = mList.item(i);
     * Element mod = (Element) nMod;
     * Pair<String, Node> p = new Pair<>(mod.getAttribute("modName"), nMod);
     * boolean found = false;
     * for (Pair<String, Node> q : LeveledListInjector.lootifiedMods) {
     * if (q.getBase().contentEquals(p.getBase())) {
     * found = true;
     * break;
     * }
     * }
     * if (!found) {
     * LeveledListInjector.lootifiedMods.add(p);
     * }
     * }
     * }*/
    
    
}
