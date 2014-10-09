/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Loot;

import java.util.LinkedHashSet;
import java.util.Set;
import skyproc.*;

public class RecordDataWEAP extends RecordData {

    private final LinkedHashSet<String> theOutfits = new LinkedHashSet<>();
    private final LinkedHashSet<TieredSet> theSets = new LinkedHashSet<>();
    private final LinkedHashSet<MatchInfo> theMatches = new LinkedHashSet<>();

    public RecordDataWEAP(String id, String mod) {
        super(id, mod, GRUP_TYPE.WEAP);
    }

    public RecordDataWEAP(MajorRecordNamed rec) {
        super(rec);
    }

    public void addMatch(MatchInfo toAdd) {
        theMatches.add(toAdd);
    }

    public void removeMatch(MatchInfo toRemove) {
        theMatches.remove(toRemove);
    }

    public Set<MatchInfo> getMatches() {
        return theMatches;
    }
    
    public boolean hasMatchWithBase(MatchInfo theMatch){
        String s = theMatch.getMatchName();
        for (MatchInfo m : theMatches){
            if(m.getIsBase() && m.getMatchName().equalsIgnoreCase(s) ){
                return true;
            }
        }
        return false;
    }

    public void addOutfit(String s) {
        theOutfits.add(s);
    }

    public void removeOutfit(String s) {
        if (theOutfits != null) {
            theOutfits.remove(s);
        }
    }

    public Set<String> getOutfits() {
        return theOutfits;
    }

    public void addSet(TieredSet toAdd) {
        theSets.add(toAdd);
    }

    public void removeSet(TieredSet toRemove) {
        theSets.remove(toRemove);
    }

    Set<TieredSet> getTieredSets() {
        return theSets;
    }
}
