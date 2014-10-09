/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Loot;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import skyproc.*;

/**
 *
 * @author David Tynan
 */
public class PluginData {
    
    private final String pluginName;
    private final Map<GRUP_TYPE, Set<RecordData>> GRUPMap = new HashMap<>();

    public PluginData(String pluginName) {
        this.pluginName = pluginName;
    }

    public PluginData(Mod theMod) {
        this.pluginName = theMod.getName();
    }
    
    public PluginData(ModListing theMod) {
        this.pluginName = theMod.print();
    }
    
    public Map<GRUP_TYPE, Set<RecordData>> getGRUPMap() {
        return GRUPMap;
    }

    public String getPluginName() {
        return pluginName;
    }
    
    public void addRecord(RecordData r){
        Set GRUPSet = GRUPMap.get(r.getType());
        if (GRUPSet == null) {
            GRUPSet = new LinkedHashSet<>();
            GRUPMap.put(r.getType(), GRUPSet);
        }
        GRUPSet.add(r);
    }
    
    public void removeRecord(RecordData r){
        Set GRUPSet = GRUPMap.get(r.getType());
        if (GRUPSet != null) {
            GRUPSet.remove(r);
        }
    }
    
    public RecordData getRecord(String edid, GRUP_TYPE g){
        Set<RecordData> GRUPSet = GRUPMap.get(g);
        if (GRUPSet != null) {
            for (RecordData rec : GRUPSet){
                if (rec.getEDID().equalsIgnoreCase(edid)) {
                    return rec;
                }
            }
        }
        return null;
    }
    
    public RecordData getRecord(String edid){
        for (GRUP_TYPE g : GRUPMap.keySet()){
            RecordData rec = getRecord(edid, g);
            if (rec != null){
                return rec;
            }
        }
        return null;
    }
    
}
