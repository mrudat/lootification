/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.LinkedHashSet;
import java.util.Set;
import skyproc.*;

public class RecordDataOTFT extends RecordData {

    private final LinkedHashSet<TieredSet> theSets = new LinkedHashSet<>();

    public RecordDataOTFT(String id, String mod) {
        super(id, mod, GRUP_TYPE.OTFT);
    }
    
    public RecordDataOTFT(MajorRecordNamed rec) {
        super(rec);
    }


    public void addSet(String s, int i) {
        TieredSet t = new TieredSet(s, i);
        theSets.add(t);
    }

    public void removeSet(String s, int i) {
        TieredSet t = new TieredSet(s, i);
        theSets.remove(t);
    }
    
    public void addSet(TieredSet t) {
        TieredSet s = new TieredSet(t.getName(), t.getTier());
        theSets.add(s);
    }

    public void removeSet(TieredSet t) {
        TieredSet s = new TieredSet(t.getName(), t.getTier());
        theSets.remove(s);
    }

    Set<TieredSet> getTieredSets() {
        return theSets;
    }
}
