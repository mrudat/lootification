/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
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
public class JsonHandler {
    Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    
    public JsonHandler() {
    }

    public void startUp() {

        Set<String> toProcess = new HashSet<>();

        // check if we load json files for only active mods or all files (for merged mods etc)
        if (LLI.save.getBool(YourSaveFile.Settings.SKIP_INACTIVE_MODS)) {
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

        // load json files
        for (String s : toProcess) {
            PluginData p = readFile(s);
            if (p != null){
                LLI.plugins.put(s, p);
            }
        }
        
        // check json object types and put in maps to use during patching
        

    }

    public PluginData readFile(String file) {
        Reader reader;
        try {
            reader = new FileReader("PluginData/" + file);
            PluginData plugin = gson.fromJson(reader, PluginData.class);
            reader.close();
            return plugin;
        } catch (IOException ex) {
            Logger.getLogger(JsonHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public void pluginsToMaps(){
        for (PluginData p : LLI.plugins.values()) {
            Map<GRUP_TYPE, Set<RecordData>> m = p.getGRUPMap();
            Set<RecordData> armorData = m.get(GRUP_TYPE.ARMO);
            for (RecordData rec : armorData) {
                if (rec instanceof RecordDataARMO) {
                    RecordDataARMO armoRec = (RecordDataARMO) rec;
                    if (armoRec.getEDID() != null) {
                        LLI.parsedARMO.put(armoRec.getEDID(), armoRec);
                    }
                }
            }

            Set<RecordData> weaponData = m.get(GRUP_TYPE.WEAP);
            for (RecordData rec : weaponData) {
                if (rec instanceof RecordDataWEAP) {
                    RecordDataWEAP weapRec = (RecordDataWEAP) rec;
                    if (weapRec.getEDID() != null) {
                        LLI.parsedWEAP.put(weapRec.getEDID(), weapRec);
                    }
                }
            }

            Set<RecordData> lvliData = m.get(GRUP_TYPE.LVLI);
            for (RecordData rec : lvliData) {
                if (rec instanceof RecordDataLVLI) {
                    RecordDataLVLI lvliRec = (RecordDataLVLI) rec;
                    if (lvliRec.getEDID() != null) {
                        LLI.parsedLVLI.put(lvliRec.getEDID(), lvliRec);
                    }
                }
            }

            Set<RecordData> otftData = m.get(GRUP_TYPE.OTFT);
            for (RecordData rec : otftData) {
                if (rec instanceof RecordDataOTFT) {
                    RecordDataOTFT otftRec = (RecordDataOTFT) rec;
                    if (otftRec.getEDID() != null) {
                        LLI.parsedOTFT.put(otftRec.getEDID(), otftRec);
                    }
                }
            }
        }
    }
}
