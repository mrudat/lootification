/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import lev.gui.LCheckBox;
import lev.gui.LComboBox;
import skyproc.SPGlobal;
import skyproc.gui.SPMainMenuPanel;
import skyproc.gui.SPSettingPanel;
import skyproc.gui.SUMGUI;

/**
 *
 * @author Justin Swanson
 */
public class OtherSettingsPanel extends SPSettingPanel {

    LCheckBox importOnStartup;
    LCheckBox lootifyMod;
    

    public OtherSettingsPanel(SPMainMenuPanel parent_) {
	super(parent_, "Other Settings", LeveledListInjector.headerColor);
    }

    @Override
    protected void initialize() {
	super.initialize();

	importOnStartup = new LCheckBox("Import Mods on Startup", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
	importOnStartup.tie(YourSaveFile.Settings.IMPORT_AT_START, LeveledListInjector.save, SUMGUI.helpPanel, true);
	importOnStartup.setOffset(2);
	importOnStartup.addShadow();
	setPlacement(importOnStartup);
	AddSetting(importOnStartup);
       
        lootifyMod = new LCheckBox("Lootify mod(s) instead of making leveled lists", LeveledListInjector.settingsFont, LeveledListInjector.settingsColor);
        lootifyMod.tie(YourSaveFile.Settings.LOOTIFY_MOD, LeveledListInjector.save, SUMGUI.helpPanel, true);
        setPlacement(lootifyMod);
        AddSetting(lootifyMod);

//	alignRight();
        
        

    }
}
