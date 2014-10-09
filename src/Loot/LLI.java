package Loot;

import Loot.YourSaveFile.Settings;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lev.gui.LSaveFile;
import skyproc.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPProgressBarPlug;
import skyproc.gui.SUM;
import skyproc.gui.SUMGUI;

/**
 *
 * @author David Tynan
 */
public class LLI implements SUM {

    /*
     * The important functions to change are:
     * - getStandardMenu(), where you set up the GUI
     * - runChangesToPatch(), where you put all the processing code and add records to the output patch.
     */

    /*
     * The types of records you want your patcher to import. Change this to
     * customize the import to what you need.
     */
    GRUP_TYPE[] importRequests = new GRUP_TYPE[]{
        GRUP_TYPE.LVLI, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.OTFT
    };
    public static final String myPatchName = "LLI";
    public static String authorName = "Dienes";
    public static String version = "1.0.0 alpha 1";
    public static String welcomeText = "Lootifies weapons and armors";
    public static String descriptionToShowInSUM = "Lootify weapons and armor.";
    public static Color headerColor = new Color(66, 181, 184);  // Teal
    public static Color settingsColor = new Color(72, 179, 58);  // Green
    public static Font settingsFont = new Font("Serif", Font.BOLD, 15);
    public static SkyProcSave save = new YourSaveFile();
    public static ArrayList<Mod> activeMods = new ArrayList<>(0);
    // ModPanel setup variables
    public static final HashMap<String, String> armorMatches = new HashMap<>(); // holds matchname and keyword EDID
    public static final HashMap<String, String> weaponMatches = new HashMap<>();
    public static final Set<String> tieredSets = new TreeSet<>();
    public static final HashMap<String, ArrayList<ARMO>> modOutfits = new HashMap<>();
    public static Mod gearVariants;
    
//    public static ArrayList<ModPanel> modPanels = new ArrayList<>(0);
    // Processing info holders
    public static final HashMap<String, RecordData> modPanelData = new HashMap<>();
    public static final String lli_prefix = "LLI_vars_"; // added as prefix to patcher created records

    public static final JsonHandler jsonHandler = new JsonHandler();
    public static final Map<String, PluginData> plugins = new HashMap<>();
    // json hashmaps
    public static final HashMap<String, RecordDataARMO> parsedARMO = new HashMap<>();
    public static final HashMap<String, RecordDataWEAP> parsedWEAP = new HashMap<>();
    public static final HashMap<String, RecordDataLVLI> parsedLVLI = new HashMap<>();
    public static final HashMap<String, RecordDataOTFT> parsedOTFT = new HashMap<>();
    

    // Do not write the bulk of your program here
    // Instead, write your patch changes in the "runChangesToPatch" function
    // at the bottom
    public static void main(String[] args) {
        try {
            SPGlobal.createGlobalLog();
            SUMGUI.open(new LLI(), args);
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
        return false;
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
        final SPMainMenuPanel settingsMenu = new SPMainMenuPanel(getHeaderColor());

        settingsMenu.setWelcomePanel(new WelcomePanel(settingsMenu));
        settingsMenu.addMenu(new OtherSettingsPanel(settingsMenu), false, save, Settings.OTHER_SETTINGS);

//        for (Mod m : activeMods) {
//            ModPanel panel = new ModPanel(settingsMenu, m);
//            modPanels.add(panel);
//            settingsMenu.addMenu(panel);
//        }

//        settingsMenu.addMenu(new OutfitsPanel(settingsMenu), false, save, Settings.OTHER_SETTINGS);

        settingsMenu.updateUI();

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
        //add xml file checks
        return false;
    }

    // This function runs when the program opens to "set things up"
    // It runs right after the save file is loaded, and before the GUI is displayed
    @Override
    public void onStart() throws Exception {

        for(ModListing mListing : SPImporter.getActiveModList() ){
            Mod aMod = SPDatabase.getMod(mListing);
            activeMods.add(aMod);
        }
        
        jsonHandler.startUp();
        
        /*
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    ArrayList<ModListing> activeModListing = SPImporter.getActiveModList();
                    ArrayList<Mod> allMods = new ArrayList<>();
                    for (ModListing listing : activeModListing) {
                        Mod newMod = new Mod(listing);
                        allMods.add(newMod);
                    }

                    gearVariants = new Mod(getName() + "MergerTemp", false);
                    gearVariants.addAsOverrides(SPGlobal.getDB());

                    for (Mod m : allMods) {
                        String modName = m.getName();

                        if (!(modName.contentEquals("Skyrim.esm") || modName.contentEquals("HearthFires.esm")
                                || modName.contentEquals("Update.esm") || modName.contentEquals("Dragonborn.esm")
                                || modName.contentEquals("Dawnguard.esm"))) {
                            int numArmors = m.getArmors().size();
                            int numWeapons = m.getWeapons().size();

                            if (numArmors > 0 || numWeapons > 0) {
                                activeMods.add(m);
                            }

                        }
                    }
                } catch (IOException e) {
                }
            }
        };
        SUMGUI.startImport(r);

        SPProgressBarPlug.setStatus("Processing XML");
*/

        // need to load json files
    }

    // This function runs right as the program is about to close.
    @Override
    public void onExit(boolean patchWasGenerated) throws Exception {
    }

    // Add any mods that you REQUIRE to be present in order to patch.
    @Override
    public ArrayList<ModListing> requiredMods() {
        return new ArrayList<>(0);
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
        ArrayList<ModListing> mods = SPDatabase.getMods();
            ArrayList<Mod> imods = SPDatabase.getImportedMods();
        
        SPProgressBarPlug.setStatus("JSON to internals");
        jsonHandler.pluginsToMaps();

        SPProgressBarPlug.setStatus("Setting up armor matches");
        ArmorTools.setMergeAndPatch(merger, patch);
        ArmorTools.setupArmorMatches();
        SPProgressBarPlug.setStatus("Setting up armor sets");
        ArmorTools.setupMatchingOutfitsSets();
        SPProgressBarPlug.setStatus("Building armor variants");
        ArmorTools.buildArmorVariants();
        SPProgressBarPlug.setStatus("Setting up armor leveled lists");
        ArmorTools.modLVLIArmors();
        SPProgressBarPlug.setStatus("Processing outfit armors");
        ArmorTools.buildOutfitsArmors();
        SPProgressBarPlug.setStatus("Linking armor leveled lists");
        ArmorTools.linkLVLIArmors();

        SPProgressBarPlug.setStatus("Setting up weapon matches");
        WeaponTools.setMergeAndPatch(merger, patch);
        WeaponTools.setupWeaponMatches();
        SPProgressBarPlug.setStatus("Building weapon variants");
        WeaponTools.buildWeaponVariants();
        SPProgressBarPlug.setStatus("Setting up weapon leveled lists");
        WeaponTools.modLVLIWeapons();
        SPProgressBarPlug.setStatus("Processing outfit weapons");
        WeaponTools.buildOutfitWeapons();
        SPProgressBarPlug.setStatus("Linking weapon leveled lists");
        WeaponTools.linkLVLIWeapons();

    }

}