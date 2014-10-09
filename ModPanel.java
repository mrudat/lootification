/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.ArrayList;
import lev.gui.*;
import skyproc.*;
import skyproc.gui.*;

/**
 *
 * @author David
 */
public class ModPanel extends SPSettingPanel {

    public Mod myMod;
    private ArrayList<ArmorListener> armorListeners = new ArrayList<>(0);
    private ArrayList<WeaponListener> weaponListeners = new ArrayList<>(0);

    private class ArmorListener implements ActionListener {

        private ARMO armor;
        private String newKey;
        private LComboBox box;
        private LLabel label;
        private RecordData rec;

        ArmorListener(ARMO a, LComboBox b, LLabel l) {
            armor = a;
            box = b;
            newKey = null;
            label = l;
            rec = LeveledListInjector.modPanelData.get(a.getEDID());
            if (rec == null) {
//                rec = new RecordData(a.getEDID(), myMod.getName(), GRUP_TYPE.ARMO);
//                LeveledListInjector.modPanelData.put(a.getEDID(), rec);
            }

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String pressed = (String) box.getSelectedItem();
            if ((pressed.compareTo("None") == 0) && (newKey != null)) {
//                rec.removeMatch(false, newKey);

                newKey = null;
                box.clearHighlight();
                label.setText(armor.getName());
            } else if ((pressed.compareTo("None") != 0) && (newKey == null)) {
                newKey = pressed;
                rec.addMatch(false, pressed);

                box.highlightChanged();
                label.setText(armor.getName() + " set " + newKey);
            } else if ((pressed.compareTo("None") != 0) && (newKey != null)) {
                rec.removeMatch(false, newKey);
                newKey = pressed;

                box.highlightChanged();
                label.setText(armor.getName() + " set " + newKey);
            }
        }
    }

    private class WeaponListener implements ActionListener {

        private WEAP weapon;
        private String newKey;
        private LComboBox box;
        private LLabel title;
        private RecordData rec;

        WeaponListener(WEAP a, LComboBox b, LLabel l) {
            weapon = a;
            box = b;
            newKey = null;
            title = l;
            rec = LeveledListInjector.modPanelData.get(a.getEDID());
            if (rec == null) {
                rec = new RecordData(a.getEDID(), myMod.getName(), GRUP_TYPE.ARMO);
                LeveledListInjector.modPanelData.put(a.getEDID(), rec);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String pressed = (String) box.getSelectedItem();
            if ((pressed.compareTo("None") == 0) && (newKey != null)) {
                rec.removeMatch(false, newKey);

                newKey = null;
                box.clearHighlight();
                title.setText(weapon.getName());
            } else if ((pressed.compareTo("None") != 0) && (newKey == null)) {
                newKey = pressed;
                rec.addMatch(false, newKey);

                box.highlightChanged();
                title.setText(weapon.getName() + " set as " + newKey);
            } else if ((pressed.compareTo("None") != 0) && (newKey != null)) {
                rec.removeMatch(false, newKey);
                newKey = pressed;
                rec.addMatch(false, newKey);

                box.highlightChanged();
                title.setText(weapon.getName() + " set as " + newKey);
            }
        }
    }

    private class SetAllListener implements ActionListener {

        SetAllListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            for (ArmorListener a : armorListeners) {
                a.actionPerformed(e);
            }
            for (WeaponListener a : weaponListeners) {
                a.actionPerformed(e);
            }
        }
    }

    private class SetNoneListener implements ActionListener {

        SetNoneListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (ArmorListener a : armorListeners) {
                a.box.setSelectedIndex(0);
                a.actionPerformed(e);
            }
            for (WeaponListener a : weaponListeners) {
                a.box.setSelectedIndex(0);
                a.actionPerformed(e);
            }
        }
    }

    private class OutfitListener implements ActionListener {

        LTextField field;
        ARMO armor;
        String setKey;
        RecordData rec;
        ArrayList<ARMO> outfit;

        OutfitListener(LTextField ltf, ARMO a) {
            field = ltf;
            armor = a;
            setKey = null;
            rec = LeveledListInjector.modPanelData.get(a.getEDID());
            if (rec == null) {
                rec = new RecordData(a.getEDID(), myMod.getName(), GRUP_TYPE.ARMO);
                LeveledListInjector.modPanelData.put(a.getEDID(), rec);
            }
            outfit = null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String key = field.getText();
            if ((setKey != null) && (!setKey.contentEquals(""))) {
                rec.removeOutfit(setKey);
                if (outfit != null) {
                    outfit.remove(armor);
                }
                field.clearHighlight();
            }

            setKey = key;
            if (!key.contentEquals("")) {
                rec.addOutfit(setKey);
                outfit = LeveledListInjector.modOutfits.get(key);
                if (outfit == null) {
                    outfit = new ArrayList<>();
                    LeveledListInjector.modOutfits.put(key, outfit);
                }
                outfit.add(armor);
                field.highlightChanged();
            }

        }
    }

    public ModPanel(SPMainMenuPanel parent_, Mod m) {
        super(parent_, m.toString(), LeveledListInjector.headerColor);
        myMod = m;
    }

    @Override
    protected void initialize() {
        super.initialize();


        boolean found = LeveledListInjector.parsedData.containsKey(myMod.getName());

        if (found) {
            LPanel donePanel = new LPanel(100, 100);
            donePanel.setSize(300, 200);
            LLabel allDone = new LLabel(myMod.getNameNoSuffix() + " is already Lootified.", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
            donePanel.Add(allDone);
            //setPlacement(donePanel);
            scroll.add(donePanel);

        } else {
            ArrayList<Pair<String, KYWD>> variantArmorMatches = new ArrayList<>();
            variantArmorMatches.add(new Pair<>("None", (KYWD) null));

            LeveledListInjector.gearVariants.addAsOverrides(myMod, GRUP_TYPE.KYWD, GRUP_TYPE.ARMO, GRUP_TYPE.WEAP);
            for (String matchName : LeveledListInjector.armorMatches.keySet()) {
                String ID = LeveledListInjector.armorMatches.get(matchName);
                KYWD k = (KYWD) LeveledListInjector.gearVariants.getMajor(ID, GRUP_TYPE.KYWD);
                if (k != null) {
                    variantArmorMatches.add(new Pair<>(matchName, k));
                }
            }

            LPanel setReset = new LPanel(300, 60);
            setReset.setSize(300, 50);

            LButton setAll = new LButton("Set All");
            setAll.addActionListener(new SetAllListener());
            LButton setNone = new LButton("set none");
            setNone.addActionListener(new SetNoneListener());

            setReset.add(setAll, BorderLayout.WEST);
            setReset.add(setNone);
            setReset.setPlacement(setNone, 150, 0);
            setPlacement(setReset);
            Add(setReset);

            for (ARMO armor : myMod.getArmors()) {
                boolean non_playable = armor.getBodyTemplate().get(BodyTemplate.GeneralFlags.NonPlayable);
                FormID enchant = armor.getEnchantment();
                boolean newItem = armor.getFormMaster().print().contentEquals(myMod.getName());
                if (!non_playable && (enchant.isNull()) && newItem) {
                    LPanel panel = new LPanel(275, 200);
                    panel.setSize(300, 80);
                    LLabel armorName = new LLabel(armor.getName(), LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);

                    LComboBox box = new LComboBox("", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                    for (Pair<String, KYWD> p : variantArmorMatches) {
                        box.addItem(p.getBase());
                    }

                    for (Pair<String, KYWD> p : variantArmorMatches) {
                        KYWD k = p.getVar();
                        if (k != null) {
                            boolean has = ArmorTools.armorHasKeyword(armor, k, LeveledListInjector.gearVariants);
                            if (has) {
                                int index = variantArmorMatches.indexOf(p);
                                box.setSelectedIndex(index);
                            }
                        }
                    }
                    ArmorListener al = new ArmorListener(armor, box, armorName);
                    armorListeners.add(al);
                    box.addEnterButton("Set", al);

                    box.setSize(250, 30);
                    panel.add(armorName, BorderLayout.WEST);
                    panel.add(box);
                    panel.setPlacement(box);

                    LTextField outfitField = new LTextField("Outfit name");
                    outfitField.addEnterButton("set", new OutfitListener(outfitField, armor));
                    outfitField.setSize(250, 30);
                    outfitField.setText(armor.getEDID());

                    panel.Add(outfitField);
                    panel.setPlacement(outfitField);

                    setPlacement(panel);
                    Add(panel);
                }
            }

            ArrayList<Pair<String, KYWD>> variantWeaponMatches = new ArrayList<>();
            variantWeaponMatches.add(new Pair<>("None", (KYWD) null));

            for (String matchName : LeveledListInjector.weaponMatches.keySet()) {
                String ID = LeveledListInjector.weaponMatches.get(matchName);
                KYWD k = (KYWD) LeveledListInjector.gearVariants.getMajor(ID, GRUP_TYPE.KYWD);
                if (k != null) {
                    variantWeaponMatches.add(new Pair<>(matchName, k));
                }
            }

            for (WEAP weapon : myMod.getWeapons()) {
                boolean non_playable = weapon.get(WEAP.WeaponFlag.NonPlayable);
                boolean bound = weapon.get(WEAP.WeaponFlag.BoundWeapon);
                FormID enchant = weapon.getEnchantment();
                boolean newItem = weapon.getFormMaster().print().contentEquals(myMod.getName());
                if (!non_playable && !bound && (enchant.isNull()) && newItem) {
                    LPanel panel = new LPanel(275, 200);
                    panel.setSize(300, 60);
                    LLabel weaponName = new LLabel(weapon.getName(), LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);


                    LComboBox box = new LComboBox("", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                    for (Pair<String, KYWD> p : variantWeaponMatches) {
                        box.addItem(p.getBase());
                    }

                    for (Pair<String, KYWD> p : variantWeaponMatches) {
                        KYWD k = p.getVar();
                        if (k != null) {
                            boolean has = WeaponTools.weaponHasKeyword(weapon, k, LeveledListInjector.gearVariants);
                            if (has) {
                                int index = variantWeaponMatches.indexOf(p);
                                box.setSelectedIndex(index);
                            }
                        }
                    }

                    String set = "set";
                    WeaponListener wl = new WeaponListener(weapon, box, weaponName);
                    weaponListeners.add(wl);
                    box.addEnterButton(set, wl);
                    box.setSize(250, 30);
                    panel.add(weaponName, BorderLayout.WEST);
                    panel.add(box);
                    panel.setPlacement(box);

                    setPlacement(panel);
                    Add(panel);
                }
            }
        }

    }
}
