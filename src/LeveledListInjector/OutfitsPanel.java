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
public class OutfitsPanel extends SPSettingPanel {

    private class TierListener implements ActionListener {

        private String set;
        private String newKey;
        private LComboBox box;
        private ArrayList<ARMO> armors;

        TierListener(String a, LComboBox b, ArrayList<ARMO> ar) {
            set = a;
            box = b;
            newKey = null;
            armors = ar;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String pressed = (String) box.getSelectedItem();
            if ((pressed.compareTo("None") == 0) && (newKey != null)) {
                for (ARMO a : armors) {
                    LeveledListInjector.modPanelData.get(a.getEDID()).removeSet(false, set, Integer.parseInt(newKey));
                }
                newKey = null;
                box.clearHighlight();
            } else if (pressed.compareTo("None") != 0) {
                if (newKey != null) {
                    for (ARMO a : armors) {
                        LeveledListInjector.modPanelData.get(a.getEDID()).removeSet(false, set, Integer.parseInt(newKey));
                    }
                }

                for (ARMO a : armors) {
                    LeveledListInjector.modPanelData.get(a.getEDID()).addSet(false, set, Integer.parseInt(pressed));
                }
                newKey = pressed;
                box.highlightChanged();

            }
        }
    }

    public OutfitsPanel(SPMainMenuPanel parent_) {
        super(parent_, "Outfits", LeveledListInjector.headerColor);
    }

    @Override
    protected void initialize() {
        super.initialize();

    }

    @Override
    public void onOpen(SPMainMenuPanel parent) {
        for (String key : LeveledListInjector.modOutfits.keySet()) {
            ArrayList<ARMO> a = LeveledListInjector.modOutfits.get(key);
            if (!a.isEmpty()){
                LPanel panel = new LPanel(275, 200);
                panel.setSize(300, 500);

                LLabel name = new LLabel(key, LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                panel.add(name, BorderLayout.WEST);
                panel.setPlacement(name);
                for (String s : LeveledListInjector.tieredSets) {
                    LComboBox tierBox = new LComboBox(s + " Tier:");
                    tierBox.addItem("None");
                    for (int i = 0; i < 30; i++) {
                        tierBox.addItem(String.valueOf(i));
                    }
                    tierBox.addEnterButton("set", new TierListener(key, tierBox, a));
                    tierBox.setSize(100, 25);
                    LLabel tierBoxLabel = new LLabel(s + " Tier:", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
                    panel.add(tierBoxLabel);
                    panel.setPlacement(tierBoxLabel);
                }
                setPlacement(panel);
                Add(panel);
            }
            
        }
    }
}
