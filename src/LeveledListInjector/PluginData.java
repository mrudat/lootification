/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package LeveledListInjector;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import skyproc.*;

/**
 *
 * @author David Tynan
 * @param <T>
 */
public class PluginData<T extends RecordData> {
    
    private final String pluginName;
    private final Map<GRUP_TYPE, Set<T>> GRUPMap = new HashMap<>();

    public PluginData(String pluginName) {
        this.pluginName = pluginName;
    }

    public PluginData(Mod theMod) {
        this.pluginName = theMod.getName();
    }
    
    public PluginData(ModListing theMod) {
        this.pluginName = theMod.print();
    }
    
    public Map<GRUP_TYPE, Set<T>> getGRUPMap() {
        return GRUPMap;
    }

    public String getPluginName() {
        return pluginName;
    }
    
    public void addRecord(T r){
        Set<T> GRUPSet = GRUPMap.get(r.getType());
        if (GRUPSet == null) {
            GRUPSet = new LinkedHashSet<>();
            GRUPMap.put(r.getType(), GRUPSet);
        }
        GRUPSet.add(r);
    }
    
    public void removeRecord(T r){
        Set<T> GRUPSet = GRUPMap.get(r.getType());
        if (GRUPSet != null) {
            GRUPSet.remove(r);
        }
    }
    
    public T getRecord(String edid, GRUP_TYPE g){
        Set<T> GRUPSet = GRUPMap.get(g);
        if (GRUPSet != null) {
            for (T rec : GRUPSet){
                if (rec.getEDID().equalsIgnoreCase(edid)) {
                    return rec;
                }
            }
        }
        return null;
    }
    
    public T getRecord(String edid){
        for (GRUP_TYPE g : GRUPMap.keySet()){
            T rec = getRecord(edid, g);
            if (rec != null){
                return rec;
            }
        }
        return null;
    }
    
}
