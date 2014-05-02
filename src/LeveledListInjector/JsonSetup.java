/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import skyproc.*;

/**
 *
 * @author David Tynan
 */
public class JsonSetup {

    public void startUp() {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        Set<String> toProcess = new HashSet<>();

        if (LeveledListInjector.save.getBool(YourSaveFile.Settings.SKIP_INACTIVE_MODS)) {
            for (Mod m : SPDatabase.getImportedMods()) {
                String name = m.getName();
                toProcess.add(name.toLowerCase() + ".json");
            }
        } else {
            File dir = new File("PluginData/");

            for (File f : dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.toLowerCase().endsWith(".json");
                }
            })) {
                toProcess.add(f.getName().toLowerCase());
            }
        }

        for (String s : toProcess) {
            Reader reader;
            try {
                reader = new FileReader("PluginData/" + s);
                PluginData plugin = gson.fromJson(reader, PluginData.class);
                LeveledListInjector.plugins.put(s, plugin);
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(JsonSetup.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        
        for (PluginData p : LeveledListInjector.plugins.values()) {
            Map<GRUP_TYPE, Set<RecordData>> m = p.getGRUPMap();
            Set<RecordData> armorData = m.get(GRUP_TYPE.ARMO);
            for (RecordData rec : armorData){
                if (rec instanceof RecordDataARMO) {
                    RecordDataARMO armoRec = (RecordDataARMO) rec;
                    if (armoRec.getEDID() != null) {
                        LeveledListInjector.parsedARMO.put(armoRec.getEDID(), armoRec);
                    }
                }
            }
            
            Set<RecordData> weaponData = m.get(GRUP_TYPE.WEAP);
            for (RecordData rec : weaponData){
                if (rec instanceof RecordDataWEAP) {
                    RecordDataWEAP weapRec = (RecordDataWEAP) rec;
                    if (weapRec.getEDID() != null) {
                        LeveledListInjector.parsedWEAP.put(weapRec.getEDID(), weapRec);
                    }
                }
            }
            
            Set<RecordData> lvliData = m.get(GRUP_TYPE.LVLI);
            for (RecordData rec : lvliData){
                if (rec instanceof RecordDataLVLI) {
                    RecordDataLVLI lvliRec = (RecordDataLVLI) rec;
                    if (lvliRec.getEDID() != null) {
                        LeveledListInjector.parsedLVLI.put(lvliRec.getEDID(), lvliRec);
                    }
                }
            }
            
            Set<RecordData> otftData = m.get(GRUP_TYPE.OTFT);
            for (RecordData rec : otftData){
                if (rec instanceof RecordDataOTFT) {
                    RecordDataOTFT otftRec = (RecordDataOTFT) rec;
                    if (otftRec.getEDID() != null) {
                        LeveledListInjector.parsedOTFT.put(otftRec.getEDID(), otftRec);
                    }
                }
            }
        }

    }
}
