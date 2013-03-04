package LeveledListInjector;

import java.awt.Color;
import java.awt.Font;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lev.gui.LSaveFile;
import skyproc.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SUM;
import skyproc.gui.SUMGUI;
import LeveledListInjector.YourSaveFile.Settings;

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

    /*
     * The types of records you want your patcher to import. Change this to
     * customize the import to what you need.
     */
    GRUP_TYPE[] importRequests = new GRUP_TYPE[]{
        GRUP_TYPE.LVLI, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.OTFT
    };
    public static String myPatchName = "LLI";
    public static String authorName = "Dienes";
    public static String version = "0.1.2";
    public static String welcomeText = "Lootifies weapons and armors";
    public static String descriptionToShowInSUM = "Lootify weapons and armor.";
    public static Color headerColor = new Color(66, 181, 184);  // Teal
    public static Color settingsColor = new Color(72, 179, 58);  // Green
    public static Font settingsFont = new Font("Serif", Font.BOLD, 15);
    public static SkyProcSave save = new YourSaveFile();
    public static ArrayList<Mod> activeMods = new ArrayList<>(0);
    public static Mod gearVariants;
    public static Mod global;

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
        ModListing variants = new ModListing("GearVariants", true);
        ModListing dawnguard = new ModListing("Dawnguard", true);
        ModListing hearthfires = new ModListing("Hearthfires", true);
        ModListing dragonborn = new ModListing("Dragonborn", true);
        gearVariants = importer.importMod(skyrim, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        Mod var = importer.importMod(variants, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        Mod dawn = importer.importMod(dawnguard, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        Mod hearth = importer.importMod(hearthfires, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        Mod born = importer.importMod(dragonborn, SPGlobal.pathToData, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        if (dawn != null){
            gearVariants.addAsOverrides(dawn, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        }
        if (hearth != null){
            gearVariants.addAsOverrides(hearth, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        }
        if (born != null){
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
        ModListing gearVariants = new ModListing("GearVariants", true);
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
        merger.addAsOverrides(global, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);


        FLST baseArmorKeysFLST = (FLST) merger.getMajor("LLI_BASE_ARMOR_KEYS", GRUP_TYPE.FLST);
        FLST variantArmorKeysFLST = (FLST) merger.getMajor("LLI_VAR_ARMOR_KEYS", GRUP_TYPE.FLST);
//        SPGlobal.log("base armor key formlist", baseArmorKeysFLST.getEDID());
//        SPGlobal.log("variant armor keywords", variantArmorKeysFLST.getEDID());
        FLST baseWeaponKeysFLST = (FLST) merger.getMajor("LLI_BASE_WEAPON_KEYS", GRUP_TYPE.FLST);
        FLST variantWeaponKeysFLST = (FLST) merger.getMajor("LLI_VAR_WEAPON_KEYS", GRUP_TYPE.FLST);


        //ArmorTools.setMergeAndPatch(merger, patch);

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

        boolean lootify = false; //save.getBool(Settings.LOOTIFY_MOD);
        if (lootify) {
            ArmorTools.setupArmorMatches(baseArmorKeysFLST, variantArmorKeysFLST, merger);
            ArmorTools.buildArmorVariants(merger, patch, baseArmorKeysFLST, variantArmorKeysFLST);
            ArmorTools.modLVLIArmors(merger, patch);
            
            WeaponTools.setMergeAndPatch(merger, patch);
            WeaponTools.setupWeaponMatches(baseWeaponKeysFLST, variantWeaponKeysFLST, merger);
            WeaponTools.buildWeaponVariants(baseWeaponKeysFLST, variantWeaponKeysFLST);
            WeaponTools.modLVLIWeapons();
        }



    }
}
