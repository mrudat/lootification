/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Loot.gui;

import Loot.PluginData;
import Loot.RecordData.MatchInfo;
import Loot.RecordDataWEAP;
import skyproc.*;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import lev.gui.LPanel;

/**
 *
 * @author David Tynan
 */
public class TempGUI extends SPSettingPanel {
    
    LPanel panel = new LPanel();
    Button testButton;
    Label testLabel;
    
    public TempGUI(SPMainMenuPanel parent_) {
        super(parent_, "Temp GUI", Loot.LLI.headerColor);
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        
        panel.setSize(400,400);
        panel.setLayout(new FlowLayout());
        
        testButton = new Button("Test Skyrim to JSON");
        

        panel.setPlacement(testButton);
        panel.add(testButton);
        
        testLabel = new Label("label");        
        testLabel.setAlignment(Label.CENTER);
        testLabel.setSize(350, 300);
        panel.setPlacement(testLabel);
        panel.add(testLabel);
        
        testButton.addActionListener(new TestListener(testLabel));
        setPlacement(panel);
        add(panel);
    }
}

class TestListener implements ActionListener {
    Label theLabel;
    
    public TestListener(Label theLabel){
        super();
        this.theLabel = theLabel;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Mod skyrim = null;
        PluginData skyrimData;
        for(Mod m : Loot.LLI.activeMods){
            if (m.getName().equalsIgnoreCase("skyrim.esm")){
                skyrim = m;
                break;
            }
        }
        if (skyrim == null){
            theLabel.setText("Could not find skyrim.esm");
            return;
        }
        skyrimData = new PluginData(skyrim);
        KYWD ironKey = (KYWD) skyrim.getMajor("WeapMaterialIron", GRUP_TYPE.KYWD);
        if(ironKey == null){
            theLabel.setText("Could not find WeapMaterialIron");
            return;
        }
        
        for (WEAP weapon : skyrim.getWeapons()){
            if (Loot.WeaponTools.weaponHasKeyword(weapon, ironKey, skyrim) && !weapon.isTemplated()){
                RecordDataWEAP weaponData = new RecordDataWEAP(weapon);
                String matchName = "Iron" + weapon.getWeaponType().name();
                MatchInfo weapMatch = new MatchInfo(true, matchName, "WeapMaterialIron");
                weaponData.addMatch(weapMatch);
                skyrimData.addRecord(weaponData);
            }
        }
        Loot.LLI.jsonHandler.writeFile(skyrimData);
        
        theLabel.setText("Done");
    }
    
}