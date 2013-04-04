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
    public static String version = "0.5";
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
                if (m.numRecords() != 0) {
                    activeMods.add(m);
                }
            }
        }
//        FLST baseArmorKeysFLST = (FLST) gearVariants.getMajor("LLI_BASE_ARMOR_KEYS", GRUP_TYPE.FLST);
//        FLST variantArmorKeysFLST = (FLST) gearVariants.getMajor("LLI_VAR_ARMOR_KEYS", GRUP_TYPE.FLST);
//        ArmorTools.setupArmorMatches(baseArmorKeysFLST, variantArmorKeysFLST, gearVariants);
//        FLST baseWeaponKeysFLST = (FLST) gearVariants.getMajor("LLI_BASE_WEAPON_KEYS", GRUP_TYPE.FLST);
//        FLST variantWeaponKeysFLST = (FLST) gearVariants.getMajor("LLI_VAR_WEAPON_KEYS", GRUP_TYPE.FLST);
//        WeaponTools.setupWeaponMatches(baseWeaponKeysFLST, variantWeaponKeysFLST, gearVariants);

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
//        if (!save.getBool(Settings.LOOTIFY_MOD)) {
//            listify = true;
//        }

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
            ArmorTools.setupArmorMatches(baseArmorKeysFLST, variantArmorKeysFLST, merger);
            ArmorTools.buildArmorBases(merger, baseArmorKeysFLST);
            ArmorTools.setupSets(merger, patch);
            ArmorTools.buildArmorVariants(merger, patch, baseArmorKeysFLST, variantArmorKeysFLST);
            ArmorTools.modLVLIArmors(merger, patch);
            ArmorTools.buildOutfitsArmors(baseArmorKeysFLST, merger, patch);
            ArmorTools.linkLVLIArmors(baseArmorKeysFLST, merger, patch);

            WeaponTools.setMergeAndPatch(merger, patch);
            WeaponTools.setupWeaponMatches(baseWeaponKeysFLST, variantWeaponKeysFLST, merger);
            WeaponTools.buildWeaponBases(baseWeaponKeysFLST);
            WeaponTools.buildWeaponVariants(baseWeaponKeysFLST, variantWeaponKeysFLST);
            WeaponTools.modLVLIWeapons();
            WeaponTools.buildOutfitWeapons(baseWeaponKeysFLST);
            WeaponTools.linkLVLIWeapons(baseWeaponKeysFLST);
        }

    }

    public void addModsToXML(Mod merger) {
        try {
            File fXmlFile = new File("Lootification.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            ArrayList<Pair<String, Node>> modNodes = new ArrayList<>(0);
            Element rootElement = doc.getDocumentElement();

            NodeList modList = doc.getElementsByTagName("mod");
            for (int i = 0; i < modList.getLength(); i++) {
                Node mod = modList.item(i);
                Element eElement = (Element) mod;
                Pair<String, Node> p = new Pair<>(eElement.getAttribute("modName"), mod);
                modNodes.add(p);
            }

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
                    Element newElement = doc.createElement("mod");
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
                        Element newElement = doc.createElement("item");
                        newElement.setAttribute("type", "armor");
                        newElement.setAttribute("EDID", akPair.getBase().getEDID());
                        theMod.appendChild(newElement);
                        theArmor = newElement;
                    }
                    Element key = doc.createElement("keyword");
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
                    Element newElement = doc.createElement("mod");
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
                        Element newElement = doc.createElement("item");
                        newElement.setAttribute("type", "weapon");
                        newElement.setAttribute("EDID", akPair.getBase().getEDID());
                        theMod.appendChild(newElement);
                        theArmor = newElement;
                    }
                    Element key = doc.createElement("keyword");
                    key.setTextContent(akPair.getVar().getEDID());
                    theArmor.appendChild(key);
                }
            }

            for (Pair<String, ArrayList<ARMO>> p : outfits) {
                for (ARMO arm : p.getVar()) {
                    NodeList items = doc.getElementsByTagName("item");
                    for (int i = 0; i < items.getLength(); i++) {
                        Element eItem = (Element) items.item(i);
                        if (eItem.getAttribute("EDID").contentEquals(arm.getEDID())) {
                            Element newKey = doc.createElement("keyword");
                            newKey.setTextContent("dienes_outfit_" + p.getBase());
                            eItem.appendChild(newKey);
                            for (Pair<String, ArrayList<String>> q : tiers) {
                                if (q.getBase().contentEquals(p.getBase())) {
                                    for (String s : q.getVar()) {
                                        Element newTier = doc.createElement("keyword");
                                        newTier.setTextContent(s);
                                        eItem.appendChild(newTier);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            doc.getDocumentElement().normalize();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("Lootification.xml"));
            StreamResult result2 = new StreamResult(new File("out.xml"));
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");

            transformer.transform(source, result);
            transformer.transform(source, result2);

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
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("mod");

            for (int i = 0; i < nList.getLength(); i++) {
                Node theMod = nList.item(i);
                Element eElement = (Element) theMod;
                if (lines.contains(eElement.getAttribute("modName"))) {
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

        } catch (Exception e) {
            SPGlobal.logException(e);
            JOptionPane.showMessageDialog(null, "There was an exception thrown during program execution: '" + e + "'  Check the debug logs or contact the author.");
            SPGlobal.closeDebug();
        }

    }
}
