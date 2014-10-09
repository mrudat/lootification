/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Loot;

import java.util.LinkedHashSet;
import java.util.Set;
import skyproc.*;

public class RecordDataLVLI extends RecordData {

    private final LinkedHashSet<String> theSets = new LinkedHashSet<>();

    public RecordDataLVLI(String id, String mod) {
        super(id, mod, GRUP_TYPE.LVLI);
    }
    
    public RecordDataLVLI(MajorRecordNamed rec) {
        super(rec);
    }


    public void addSet(String toAdd) {
        theSets.add(toAdd);
    }

    public void removeSet(String toRemove) {
        theSets.remove(toRemove);
    }

    Set<String> getTieredSets() {
        return theSets;
    }
}
