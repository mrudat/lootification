/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import lev.gui.*;
import skyproc.*;
import skyproc.gui.*;
import java.util.ArrayList;
import java.awt.event.*;
import LeveledListInjector.ini.INI;
import java.awt.BorderLayout;
import java.io.*;

/**
 *
 * @author David
 */
public class ModPanel extends SPSettingPanel {

    private Mod myMod;
    private ArrayList<LComboBox> armorBoxes;

    private class ArmorListener implements ActionListener {

        private ARMO armor;
        private KYWD newKey;
        private LComboBox box;
        private KeywordSet keys;
        private LLabel label;

        ArmorListener(ARMO a, LComboBox b, LLabel l) {
            armor = a;
            keys = armor.getKeywordSet();
            box = b;
            newKey = null;
            label = l;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String pressed = (String) box.getSelectedItem();
            if ((pressed.compareTo("None") == 0) && (newKey != null)) {
                keys.removeKeywordRef(newKey.getForm());
                newKey = null;
                LeveledListInjector.global.addRecord(armor);
                box.clearHighlight();
                label.setText(armor.getName());
            } else if (pressed.compareTo("None") != 0) {
                newKey = (KYWD) LeveledListInjector.gearVariants.getMajor(pressed, GRUP_TYPE.KYWD);
                keys.addKeywordRef(newKey.getForm());
                LeveledListInjector.global.addRecord(armor);
                box.highlightChanged();
                label.setText(armor.getName() + " set " + newKey.getEDID());
            }
        }
    }

    private class WeaponListener implements ActionListener {

        private WEAP weapon;
        private KYWD newKey;
        private LComboBox box;
        private KeywordSet keys;
        private LLabel title;

        WeaponListener(WEAP a, LComboBox b, LLabel l) {
            weapon = a;
            keys = weapon.getKeywordSet();
            box = b;
            newKey = null;
            title = l;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String pressed = (String) box.getSelectedItem();
            if ((pressed.compareTo("None") == 0) && (newKey != null)) {
                keys.removeKeywordRef(newKey.getForm());
                newKey = null;
                LeveledListInjector.global.addRecord(weapon);
                box.clearHighlight();
                title.setText(weapon.getName());
            } else if (pressed.compareTo("None") != 0) {
                newKey = (KYWD)LeveledListInjector.gearVariants.getMajor(pressed, GRUP_TYPE.KYWD);
                keys.addKeywordRef(newKey.getForm());
                LeveledListInjector.global.addRecord(weapon);
                box.highlightChanged();
                title.setText(weapon.getName()+" set as "+newKey.getEDID());
            }
        }
    }

    public ModPanel(SPMainMenuPanel parent_, Mod m, Mod g) {
        super(parent_, m.toString(), LeveledListInjector.headerColor);
        myMod = m;
    }

    @Override
    protected void initialize() {
        super.initialize();
        FLST variantArmorKeysFLST = (FLST) LeveledListInjector.gearVariants.getMajor("LLI_VAR_ARMOR_KEYS", GRUP_TYPE.FLST);
        FLST variantWeaponKeysFLST = (FLST) LeveledListInjector.gearVariants.getMajor("LLI_VAR_WEAPON_KEYS", GRUP_TYPE.FLST);
        FLST armorMatTypes = (FLST) LeveledListInjector.gearVariants.getMajor("LLI_ARMOR_MAT_TYPES", GRUP_TYPE.FLST);
        ArrayList<FormID> armorMaterialTypes = armorMatTypes.getFormIDEntries();
        FLST weaponMatTypes = (FLST) LeveledListInjector.gearVariants.getMajor("LLI_WEAPON_MAT_TYPES", GRUP_TYPE.FLST);
        ArrayList<FormID> weaponMaterialTypes = weaponMatTypes.getFormIDEntries();

        //setupIni();
        ArrayList<FormID> variantArmorKeys = variantArmorKeysFLST.getFormIDEntries();
        ArrayList<String> armorVariantNames = new ArrayList<>(0);
        armorVariantNames.add("None");
        
//        Mod masters = new Mod("mastersTemp", true);
//        masters.addAsOverrides(LeveledListInjector.gearVariants, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
        LeveledListInjector.gearVariants.addAsOverrides(myMod, GRUP_TYPE.FLST, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);

        for (FormID f : variantArmorKeys) {
            MajorRecord maj = LeveledListInjector.gearVariants.getMajor(f, GRUP_TYPE.KYWD);
            armorVariantNames.add(maj.getEDID());
        }

        for (ARMO armor : myMod.getArmors()) {
            LPanel panel = new LPanel(275, 200);
            panel.setSize(300, 60);
            LLabel armorName = new LLabel(armor.getName(), LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);

            LComboBox box = new LComboBox("", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
            for (String s : armorVariantNames) {
                box.addItem(s);
            }

            KYWD k = ArmorTools.armorHasAnyKeyword(armor, armorMatTypes, LeveledListInjector.gearVariants);
            if (k != null) {
                int index = armorMaterialTypes.indexOf(k.getForm()) + 1; //offset None entry
                box.setSelectedIndex(index);
            }
            box.addEnterButton("Set", new ArmorListener(armor, box, armorName));

            box.setSize(250, 30);
            panel.add(armorName, BorderLayout.WEST);
            panel.add(box);
            panel.setPlacement(box);

            setPlacement(panel);
            Add(panel);

        }

        ArrayList<FormID> variantWeaponKeys = variantWeaponKeysFLST.getFormIDEntries();
        ArrayList<String> weaponVariantNames = new ArrayList<>(0);
        weaponVariantNames.add("None");
        for (FormID f : variantWeaponKeys) {
            MajorRecord maj = LeveledListInjector.gearVariants.getMajor(f, GRUP_TYPE.KYWD);
            weaponVariantNames.add(maj.getEDID());
        }

        for (WEAP weapon : myMod.getWeapons()) {
           LPanel panel = new LPanel(275, 200);
            panel.setSize(300, 60);
            LLabel weaponName = new LLabel(weapon.getName(), LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);

            
            LComboBox box = new LComboBox("", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
            for (String s : weaponVariantNames) {
                box.addItem(s);
            }
            KYWD k = WeaponTools.weaponHasAnyKeyword(weapon, weaponMatTypes, LeveledListInjector.gearVariants);
            if (k != null) {
                int index = weaponMaterialTypes.indexOf(k.getForm()) + 1; //offset None entry
                box.setSelectedIndex(index);
            }
            
            String set = "set";
            box.addEnterButton(set, new WeaponListener(weapon, box, weaponName));
            box.setSize(250, 30);
            panel.add(weaponName, BorderLayout.WEST);
            panel.add(box);
            panel.setPlacement(box);

            setPlacement(panel);
            Add(panel);
        }

    }

    public void setupIni() {
        File f;
        f = new File("\\Mod inis\\" + myMod.getName() + ".ini");
        if (!f.exists()) {
            {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    SPGlobal.logException(e);
                }
            }
        }

    }
}
