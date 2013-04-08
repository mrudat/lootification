package LeveledListInjector;

import java.awt.Color;
import java.awt.Font;
import java.net.URL;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Files;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lev.gui.LSaveFile;
import skyproc.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SUM;
import skyproc.gui.SUMGUI;
import LeveledListInjector.YourSaveFile.Settings;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import skyproc.gui.SPProgressBarPlug;

/**
 *
 * @author David Tynan
 */
public class LeveledListInjector implements SUM {

    /*
     * The important functions to change are:
     * - getStandardMenu(), where you set up the GUI
     * - runChangesToPatch(), where you put all the processing code and add records to the output patch.
     */
    public static class Pair<L, R> {

        private L l;
        private R r;

        public Pair(L l, R r) {
            this.l = l;
            this.r = r;
        }

        public L getBase() {
            return l;
        }

        public R getVar() {
            return r;
        }

        public void setBase(L l) {
            this.l = l;
        }

        public void setVar(R r) {
            this.r = r;
        }
    }
    /*
     * The types of records you want your patcher to import. Change this to
     * customize the import to what you need.
     */
    GRUP_TYPE[] importRequests = new GRUP_TYPE[]{
        GRUP_TYPE.LVLI, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.OTFT
    };
    public static String myPatchName = "LLI";
    public static String authorName = "Dienes";
    public static String version = "0.5.5";
    public static String welcomeText = "Lootifies weapons and armors";
    public static String descriptionToShowInSUM = "Lootify weapons and armor.";
    public static Color headerColor = new Color(66, 181, 184);  // Teal
    public static Color settingsColor = new Color(72, 179, 58);  // Green
    public static Font settingsFont = new Font("Serif", Font.BOLD, 15);
    public static SkyProcSave save = new YourSaveFile();
    public static ArrayList<Mod> activeMods = new ArrayList<>(0);
    public static Mod gearVariants;
    public static Mod global;
    public static ArrayList<Pair<String, ArrayList<ARMO>>> outfits = new ArrayList<>(0);
    public static ArrayList<Pair<String, ArrayList<String>>> tiers = new ArrayList<>(0);
    public static ArrayList<Pair<Mod, ArrayList<Pair<ARMO, KYWD>>>> modArmors = new ArrayList<>(0);
    public static ArrayList<Pair<Mod, ArrayList<Pair<WEAP, KYWD>>>> modWeapons = new ArrayList<>(0);
    public static boolean listify = false;
    public static ArrayList<Pair<String, Node>> lootifiedMods = new ArrayList<>(0);

    // Do not write the bulk of your program here
    // Instead, write your patch changes in the "runChangesToPatch" function
    // at the bottom
    public static void main(String[] args) {
        try {
            SPGlobal.createGlobalLog();
            SUMGUI.open(new LeveledListInjector(), args);
        } catch (Exception e) {
            // If a major error happens, print it everywhere and display a message box.
            System.err.println(e.toString());
            SPGlobal.logException(e);
            JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs or contact the author.");
            SPGlobal.closeDebug();
        }
    }

    @Override
    public String getName() {
        return myPatchName;
    }

    // This function labels any record types that you "multiply".
    // For example, if you took all the armors in a mod list and made 3 copies,
    // you would put ARMO here.
    // This is to help monitor/prevent issues where multiple SkyProc patchers
    // multiply the same record type to yeild a huge number of records.
    @Override
    public GRUP_TYPE[] dangerousRecordReport() {
        return new GRUP_TYPE[]{GRUP_TYPE.LVLI};
    }

    @Override
    public GRUP_TYPE[] importRequests() {
        return importRequests;
    }

    @Override
    public boolean importAtStart() {
        return true;
    }

    @Override
    public boolean hasStandardMenu() {
        return true;
    }

    // This is where you add panels to the main menu.
    // First create custom panel classes (as shown by YourFirstSettingsPanel),
    // Then add them here.
    @Override
    public SPMainMenuPanel getStandardMenu() {
        SPMainMenuPanel settingsMenu = new SPMainMenuPanel(getHeaderColor());



        settingsMenu.setWelcomePanel(new WelcomePanel(settingsMenu));
        settingsMenu.addMenu(new OtherSettingsPanel(settingsMenu), false, save, Settings.OTHER_SETTINGS);

        for (Mod m : activeMods) {
            settingsMenu.addMenu(new ModPanel(settingsMenu, m, global));
        }

        settingsMenu.addMenu(new OutfitsPanel(settingsMenu), false, save, Settings.OTHER_SETTINGS);

        return settingsMenu;
    }

    // Usually false unless you want to make your own GUI
    @Override
    public boolean hasCustomMenu() {
        return false;
    }

    @Override
    public JFrame openCustomMenu() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasLogo() {
        return false;
    }

    @Override
    public URL getLogo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasSave() {
        return true;
    }

    @Override
    public LSaveFile getSave() {
        return save;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public ModListing getListing() {
        return new ModListing(getName(), false);
    }

    @Override
    public Mod getExportPatch() {
        Mod out = new Mod(getListing());
        out.setAuthor(authorName);
        return out;
    }

    @Override
    public Color getHeaderColor() {
        return headerColor;
    }

    // Add any custom checks to determine if a patch is needed.
    // On Automatic Variants, this function would check if any new packages were
    // added or removed.
    @Override
    public boolean needsPatching() {
        return false;
    }

    // This function runs when the program opens to "set things up"
    // It runs right after the save file is loaded, and before the GUI is displayed
    @Override
    public void onStart() throws Exception {
        //get active mods with weapons or armor
        SPImporter importer = new SPImporter();
        ArrayList<ModListing> activeModListing = importer.getActiveModList();
        ModListing skyrim = new ModListing("Skyrim", true);
        ModListing update = new ModListing("Update", true);
        ModListing variants = new ModListing("Lootification", true);
        ModListing dawnguard = new ModListing("Dawnguard", true);
        ModListing hearthfires = new ModListing("Hearthfires", true);
        ModListing dragonborn = new ModListing("Dragonborn", true);
        gearVariants = importer.importMod(skyrim, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        Mod up = importer.importMod(update, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        gearVariants.addAsOverrides(up, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        Mod var = importer.importMod(variants, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);

        List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(SPGlobal.getPluginsTxt()), StandardCharsets.UTF_8);

        File dawnf = new File(SPGlobal.pathToData + "Dawnguard.esm");
        if (lines.contains("Dawnguard.esm")) {
            Mod dawn = importer.importMod(dawnguard, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
            gearVariants.addAsOverrides(dawn, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        }
        File hearthf = new File(SPGlobal.pathToData + "Hearthfires.esm");
        if (lines.contains("Hearthfires.esm")) {
            Mod hearth = importer.importMod(hearthfires, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
            gearVariants.addAsOverrides(hearth, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        }
        File bornf = new File(SPGlobal.pathToData + "Dragonborn.esm");
        if (lines.contains("Dragonborn.esm")) {
            Mod born = importer.importMod(dragonborn, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
            gearVariants.addAsOverrides(born, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        }

        gearVariants.addAsOverrides(var, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);


        global = new Mod(getName() + "MergerTemp", false);
//        for (ModListing eachMod : activeModListing) {
//            merger.
//        }
        //merger.addAsOverrides(SPGlobal.getDB());

        for (ModListing eachMod : activeModListing) {
            SPGlobal.log("active Mod", eachMod.toString());
            if (!(eachMod.equals(skyrim) || eachMod.equals(variants) || eachMod.equals(update))) {
                Mod m = importer.importMod(eachMod, SPGlobal.pathToData, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP, GRUP_TYPE.KYWD);
                int numArmors = m.getArmors().size();
                int numWeapons = m.getWeapons().size();
                if ( (numArmors != 0) || (numWeapons != 0) ) {
                    activeMods.add(m);
                }
            }
        }

        File fXmlFile = new File("Lootification.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();

        NodeList mList = doc.getElementsByTagName("mod");
        for (int i = 0; i < mList.getLength(); i++) {
            Node nMod = mList.item(i);
            Element mod = (Element) nMod;
            lootifiedMods.add(new Pair<>(mod.getAttribute("modName"), nMod));
        }
        
        File CustomXmlFile = new File("Custom.xml");
        Document cDoc = dBuilder.parse(CustomXmlFile);
        cDoc.getDocumentElement().normalize();

        mList = cDoc.getElementsByTagName("mod");
        for (int i = 0; i < mList.getLength(); i++){
            Node nMod = mList.item(i);
            Element mod = (Element) nMod;
            Pair<String, Node> p = new Pair<>(mod.getAttribute("modName"), nMod);
            boolean found = false;
            for(Pair<String, Node> q : lootifiedMods){
                if (q.getBase().contentEquals(p.getBase()) ) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                lootifiedMods.add(p);
            }
        }
    }

    // This function runs right as the program is about to close.
    @Override
    public void onExit(boolean patchWasGenerated) throws Exception {
    }

    // Add any mods that you REQUIRE to be present in order to patch.
    @Override
    public ArrayList<ModListing> requiredMods() {
        ArrayList<ModListing> req = new ArrayList<>(0);
        ModListing gearVariants = new ModListing("Lootification", true);
        req.add(gearVariants);
        return req;
    }

    @Override
    public String description() {
        return descriptionToShowInSUM;
    }

    // This is where you should write the bulk of your code.
    // Write the changes you would like to make to the patch,
    // but DO NOT export it.  Exporting is handled internally.
    @Override
    public void runChangesToPatch() throws Exception {

        Mod patch = SPGlobal.getGlobalPatch();

        Mod merger = new Mod(getName() + "Merger", false);
        merger.addAsOverrides(SPGlobal.getDB());


//        for (Pair<String, ArrayList<ARMO>> p : outfits) {
//            KYWD k2 = new KYWD(patch, "dienes_outfit_" + p.getBase());
//            patch.addRecord(k2);
//            merger.addRecord(k2);
//            for (ARMO arm : p.getVar()) {
//                KeywordSet keys = arm.getKeywordSet();
//                keys.addKeywordRef(k2.getForm());
//
//                for (Pair<String, ArrayList<String>> q : tiers) {
//                    if (p.getBase().contentEquals(q.getBase())) {
//                        for (String s : q.getVar()) {
//                            KYWD tierKey = (KYWD) merger.getMajor(s, GRUP_TYPE.KYWD);
//                            keys.addKeywordRef(tierKey.getForm());
//                        }
//                    }
//                }
//
//                merger.addRecord(arm);
//                patch.addRecord(arm);
//            }
//        }
        SPProgressBarPlug.setStatus("Processing XML");
        addModsToXML(merger);
        processXML(merger, patch);


        FLST baseArmorKeysFLST = (FLST) merger.getMajor("LLI_BASE_ARMOR_KEYS", GRUP_TYPE.FLST);
        FLST variantArmorKeysFLST = (FLST) merger.getMajor("LLI_VAR_ARMOR_KEYS", GRUP_TYPE.FLST);
//        SPGlobal.log("base armor key formlist", baseArmorKeysFLST.getEDID());
//        SPGlobal.log("variant armor keywords", variantArmorKeysFLST.getEDID());
        FLST baseWeaponKeysFLST = (FLST) merger.getMajor("LLI_BASE_WEAPON_KEYS", GRUP_TYPE.FLST);
        FLST variantWeaponKeysFLST = (FLST) merger.getMajor("LLI_VAR_WEAPON_KEYS", GRUP_TYPE.FLST);


        //ArmorTools.setMergeAndPatch(merger, patch);
        boolean buildLootification = false;
        if (buildLootification) {
//        if (save.getBool(Settings.PROCESS_ARMORS)) {
            ArmorTools.setupArmorMatches(baseArmorKeysFLST, variantArmorKeysFLST, merger);
            ArmorTools.buildArmorVariants(merger, patch, baseArmorKeysFLST, variantArmorKeysFLST);
//            if (save.getBool(Settings.PROCESS_OUTFITS)) {
            ArmorTools.buildOutfitsArmors(baseArmorKeysFLST, merger, patch);
//            }
            ArmorTools.linkLVLIArmors(baseArmorKeysFLST, merger, patch);

//        }

//        if (save.getBool(Settings.PROCESS_WEAPONS)) {
            WeaponTools.setMergeAndPatch(merger, patch);
            WeaponTools.setupWeaponMatches(baseWeaponKeysFLST, variantWeaponKeysFLST, merger);
            WeaponTools.buildWeaponVariants(baseWeaponKeysFLST, variantWeaponKeysFLST);
//            if (save.getBool(Settings.PROCESS_OUTFITS)) {
            WeaponTools.buildOutfitWeapons(baseWeaponKeysFLST);
//            }
            WeaponTools.linkLVLIWeapons(baseWeaponKeysFLST);
//        }
        }

        boolean lootify = true; //save.getBool(Settings.LOOTIFY_MOD);
        if (lootify) {
            SPProgressBarPlug.setStatus("Setting up armor matches");
            ArmorTools.setupArmorMatches(baseArmorKeysFLST, variantArmorKeysFLST, merger);
            SPProgressBarPlug.setStatus("Building base armors");
            ArmorTools.buildArmorBases(merger, baseArmorKeysFLST);
            SPProgressBarPlug.setStatus("Setting up armor sets");
            ArmorTools.setupSets(merger, patch);
            SPProgressBarPlug.setStatus("Building armor variants");
            ArmorTools.buildArmorVariants(merger, patch, baseArmorKeysFLST, variantArmorKeysFLST);
            SPProgressBarPlug.setStatus("Setting up armor leveled lists");
            ArmorTools.modLVLIArmors(merger, patch);
            SPProgressBarPlug.setStatus("Processing outfit armors");
            ArmorTools.buildOutfitsArmors(baseArmorKeysFLST, merger, patch);
            SPProgressBarPlug.setStatus("Linking armor leveled lists");
            ArmorTools.linkLVLIArmors(baseArmorKeysFLST, merger, patch);

            WeaponTools.setMergeAndPatch(merger, patch);
            SPProgressBarPlug.setStatus("Setting up weapon matches");
            WeaponTools.setupWeaponMatches(baseWeaponKeysFLST, variantWeaponKeysFLST, merger);
            SPProgressBarPlug.setStatus("Building base weapons");
            WeaponTools.buildWeaponBases(baseWeaponKeysFLST);
            SPProgressBarPlug.setStatus("Building weapon variants");
            WeaponTools.buildWeaponVariants(baseWeaponKeysFLST, variantWeaponKeysFLST);
            SPProgressBarPlug.setStatus("Setting up weapon leveled lists");
            WeaponTools.modLVLIWeapons();
            SPProgressBarPlug.setStatus("Processing outfit weapons");
            WeaponTools.buildOutfitWeapons(baseWeaponKeysFLST);
            SPProgressBarPlug.setStatus("Linking weapon leveled lists");
            WeaponTools.linkLVLIWeapons(baseWeaponKeysFLST);
        }

    }

    public void addModsToXML(Mod merger) {
        try {
            File fXmlFile = new File("Custom.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document custom = dBuilder.parse(fXmlFile);


            Document newDoc = dBuilder.newDocument();


            ArrayList<Pair<String, Node>> modNodes = new ArrayList<>(0);
            Element rootElement = newDoc.createElement("lootification");
            newDoc.appendChild(rootElement);


            for (Pair<Mod, ArrayList<Pair<ARMO, KYWD>>> p : modArmors) {
                boolean found = false;
                Node theMod = null;
                for (Pair<String, Node> q : modNodes) {
                    if (p.getBase().getName().contentEquals(q.getBase())) {
                        theMod = q.getVar();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Element newElement = newDoc.createElement("mod");
                    newElement.setAttribute("modName", p.getBase().getName());
                    rootElement.appendChild(newElement);
                    theMod = newElement;
                    Pair<String, Node> q = new Pair<>(newElement.getAttribute("modName"), theMod);
                    modNodes.add(q);
                }
                for (Pair<ARMO, KYWD> akPair : p.getVar()) {
                    boolean armorFound = false;
                    Node theArmor = null;
                    NodeList items = theMod.getChildNodes();
                    for (int i = 0; i < items.getLength(); i++) {
                        Node item = items.item(i);
                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            Element eItem = (Element) item;
                            if (eItem.getAttribute("EDID").contentEquals(akPair.getBase().getEDID())) {
                                theArmor = item;
                                armorFound = true;
                                break;
                            }
                        }
                    }
                    if (!armorFound) {
                        Element newElement = newDoc.createElement("item");
                        newElement.setAttribute("type", "armor");
                        newElement.setAttribute("EDID", akPair.getBase().getEDID());
                        theMod.appendChild(newElement);
                        theArmor = newElement;
                    }
                    Element key = newDoc.createElement("keyword");
                    key.setTextContent(akPair.getVar().getEDID());
                    theArmor.appendChild(key);
                }
            }

            for (Pair<Mod, ArrayList<Pair<WEAP, KYWD>>> p : modWeapons) {
                boolean found = false;
                Node theMod = null;
                for (Pair<String, Node> q : modNodes) {
                    if (p.getBase().getName().contentEquals(q.getBase())) {
                        theMod = q.getVar();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Element newElement = newDoc.createElement("mod");
                    newElement.setAttribute("modName", p.getBase().getName());
                    rootElement.appendChild(newElement);
                    theMod = newElement;
                    Pair<String, Node> q = new Pair<>(newElement.getAttribute("modName"), theMod);
                    modNodes.add(q);
                }
                for (Pair<WEAP, KYWD> akPair : p.getVar()) {
                    boolean armorFound = false;
                    Node theArmor = null;
                    NodeList items = theMod.getChildNodes();
                    for (int i = 0; i < items.getLength(); i++) {
                        Node item = items.item(i);
                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            Element eItem = (Element) item;
                            if (eItem.getAttribute("EDID").contentEquals(akPair.getBase().getEDID())) {
                                theArmor = item;
                                armorFound = true;
                                break;
                            }
                        }
                    }
                    if (!armorFound) {
                        Element newElement = newDoc.createElement("item");
                        newElement.setAttribute("type", "weapon");
                        newElement.setAttribute("EDID", akPair.getBase().getEDID());
                        theMod.appendChild(newElement);
                        theArmor = newElement;
                    }
                    Element key = newDoc.createElement("keyword");
                    key.setTextContent(akPair.getVar().getEDID());
                    theArmor.appendChild(key);
                }
            }

            for (Pair<String, ArrayList<ARMO>> p : outfits) {
                String master = p.getVar().get(0).getFormMaster().print();
                master = master.substring(0, master.length() - 4);
                for (ARMO arm : p.getVar()) {
                    NodeList items = newDoc.getElementsByTagName("item");
                    for (int i = 0; i < items.getLength(); i++) {
                        Element eItem = (Element) items.item(i);
                        if (eItem.getAttribute("EDID").contentEquals(arm.getEDID())) {
                            Element newKey = newDoc.createElement("keyword");
                            newKey.setTextContent("dienes_outfit_" + master + p.getBase());
                            eItem.appendChild(newKey);
                            for (Pair<String, ArrayList<String>> q : tiers) {
                                if (q.getBase().contentEquals(p.getBase())) {
                                    for (String s : q.getVar()) {
                                        Element newTier = newDoc.createElement("keyword");
                                        newTier.setTextContent(s.replace(" ", ""));
                                        eItem.appendChild(newTier);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            newDoc.getDocumentElement().normalize();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(newDoc);

            StreamResult result = new StreamResult(new File("out.xml"));
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
            transformer.transform(source, result);

            Document merged = mergeDocs(custom, newDoc);
            DOMSource sourceMerged = new DOMSource(merged);
            StreamResult resultMerged = new StreamResult(new File("Custom.xml"));
            transformer.transform(sourceMerged, resultMerged);


        } catch (Exception e) {
            SPGlobal.logException(e);
            JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs or contact the author.");
            SPGlobal.closeDebug();
        }
    }

    public void processXML(Mod merger, Mod patch) {
        try {
            List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(SPGlobal.getPluginsTxt()), StandardCharsets.UTF_8);

            File fXmlFile = new File("Lootification.xml");
            File customXmlFile = new File("Custom.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document loot = dBuilder.parse(fXmlFile);
            Document custom = dBuilder.parse(customXmlFile);

            loot.getDocumentElement().normalize();
            custom.getDocumentElement().normalize();

            Document doc = mergeDocs(loot, custom);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("mod");

            for (int i = 0; i < nList.getLength(); i++) {
                Node theMod = nList.item(i);
                Element eElement = (Element) theMod;
                if (lines.contains(eElement.getAttribute("modName")) || !(save.getBool(Settings.SKIP_INACTIVE_MODS))) {
                    if (!eElement.getAttribute("modName").contentEquals("Dragonborn.esm") || (eElement.getAttribute("modName").contentEquals("Dragonborn.esm") && save.getBool(Settings.LOOTIFY_DRAGONBORN))) {
                        NodeList items = theMod.getChildNodes();
                        for (int j = 0; j < items.getLength(); j++) {
                            Node item = items.item(j);
                            if (item.getNodeType() == Node.ELEMENT_NODE) {
                                Element eItem = (Element) item;
                                if (eItem.getAttribute("type").contentEquals("weapon")) {
                                    WEAP weapon = (WEAP) merger.getMajor(eItem.getAttribute("EDID"), GRUP_TYPE.WEAP);
                                    if (weapon != null) {
                                        KeywordSet keys = weapon.getKeywordSet();
                                        NodeList kList = eItem.getElementsByTagName("keyword");
                                        for (int k = 0; k < kList.getLength(); k++) {
                                            Element eKey = (Element) kList.item(k);
                                            KYWD newKey = (KYWD) merger.getMajor(eKey.getTextContent(), GRUP_TYPE.KYWD);
                                            if (newKey != null) {
                                                keys.addKeywordRef(newKey.getForm());
                                                patch.addRecord(weapon);
                                                merger.addRecord(weapon);
                                            }
                                        }
                                    }
                                } else {
                                    if (eItem.getAttribute("type").contentEquals("armor")) {
                                        ARMO armor = (ARMO) merger.getMajor(eItem.getAttribute("EDID"), GRUP_TYPE.ARMO);
                                        if (armor != null) {
                                            KeywordSet keys = armor.getKeywordSet();
                                            NodeList kList = eItem.getElementsByTagName("keyword");
                                            for (int k = 0; k < kList.getLength(); k++) {
                                                Element eKey = (Element) kList.item(k);
                                                KYWD newKey = (KYWD) merger.getMajor(eKey.getTextContent(), GRUP_TYPE.KYWD);
                                                if (newKey == null) {
                                                    newKey = new KYWD(patch, eKey.getTextContent());
                                                    merger.addRecord(newKey);
                                                }
                                                keys.addKeywordRef(newKey.getForm());
                                                patch.addRecord(armor);
                                                merger.addRecord(armor);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

        } catch (Exception e) {
            SPGlobal.logException(e);
            JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs or contact the author.");
            SPGlobal.closeDebug();
        }

    }

    public Document mergeDocs(Document doc1, Document doc2) {
        Document newDoc = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            newDoc = dBuilder.newDocument();

            ArrayList<Pair<String, Node>> modNodes = new ArrayList<>(0);
            Element rootElement = newDoc.createElement("lootification");
            newDoc.appendChild(rootElement);

            NodeList modList = doc1.getElementsByTagName("mod");
            for (int i = 0; i < modList.getLength(); i++) {
                Node mod = modList.item(i);
                Element eElement = (Element) mod;
                Node newMod = newDoc.importNode(mod, true);
                rootElement.appendChild(newMod);
                Pair<String, Node> p = new Pair<>(eElement.getAttribute("modName"), newMod);
                modNodes.add(p);
            }

            modList = doc2.getElementsByTagName("mod");
            for (int i = 0; i < modList.getLength(); i++) {
                Node mod = modList.item(i);
                Element eElement = (Element) mod;
                boolean foundMod = false;
                for (Pair<String, Node> p : modNodes) {
                    if (p.getBase().contentEquals(eElement.getAttribute("modName"))) {
                        foundMod = true;
                        NodeList newItems = mod.getChildNodes();
                        NodeList oldItems = p.getVar().getChildNodes();
                        for (int j = 0; j < newItems.getLength(); j++) {
                            Node newItem = newItems.item(j);
                            boolean foundItem = false;
                            if (newItem.getNodeType() == Node.ELEMENT_NODE) {
                                Element eNewItem = (Element) newItem;
                                for (int k = 0; k < oldItems.getLength(); k++) {
                                    Node oldItem = oldItems.item(j);
                                    if (oldItem.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eOldItem = (Element) oldItem;
                                        if (eNewItem.getAttribute("EDID").contentEquals(eOldItem.getAttribute("EDID"))) {
                                            foundItem = true;
                                            NodeList newKeys = newItem.getChildNodes();
                                            for (int m = 0; m < newKeys.getLength(); m++) {
                                                if (newKeys.item(m).getNodeType() == Node.ELEMENT_NODE) {
                                                    Element newKey = (Element) newKeys.item(m);
                                                    if (newKey.getNodeName().contentEquals("keyword")) {
                                                        boolean foundKey = false;
                                                        NodeList oldKeys = oldItem.getChildNodes();
                                                        for (int l = 0; l < oldKeys.getLength(); l++) {
                                                            if (oldKeys.item(l).getNodeType() == Node.ELEMENT_NODE) {
                                                                Element oldKey = (Element) oldKeys.item(l);
                                                                if (oldKey.getNodeName().contentEquals("keyword")) {
                                                                    if (oldKey.getTextContent().contentEquals(newKey.getTextContent())) {
                                                                        foundKey = true;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (!foundKey) {
                                                            newItem.appendChild(newDoc.importNode(newKey, true));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!foundItem) {
                                    p.getVar().appendChild(newDoc.importNode(newItem, true));
                                }
                            }
                        }
                    }
                }
                if (!foundMod) {
                    rootElement.appendChild(newDoc.importNode(mod, true));
                }
            }
            newDoc.normalize();

        } catch (Exception e) {
            SPGlobal.logException(e);
            JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs or contact the author.");
            SPGlobal.closeDebug();
        }
        return newDoc;
    }
}
