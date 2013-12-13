/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.ArrayList;
import skyproc.*;

/**
 *
 * @author David
 */
public class RecordData {

    private final String EDID;
    private final String masterMod;
    private final GRUP_TYPE type;
    private ArrayList<String> theOutfits;
    private ArrayList<setInfo> theSets;
    private ArrayList<matchInfo> theMatches;

    private class matchInfo {

        boolean isBase;
        String matchName;
    }

    private class setInfo {

        private boolean isBase;
        private String setName;
        private int tier;

        setInfo(boolean b, String s, int i) {
            isBase = b;
            setName = s;
            tier = i;
        }
    }

    RecordData(String id, String mod, GRUP_TYPE g) {
        EDID = id;
        masterMod = mod;
        type = g;
    }

    public String getMod() {
        return masterMod;
    }

    public GRUP_TYPE getType() {
        return type;
    }

    public String getEDID() {
        return EDID;
    }

    public void addMatch(boolean b, String s) {
        if (theMatches == null) {
            theMatches = new ArrayList<>();
        }
        matchInfo thisMatch = new matchInfo();
        thisMatch.isBase = b;
        thisMatch.matchName = s;
        theMatches.add(thisMatch);
    }

    public void removeMatch(boolean b, String s) {
        if (theMatches != null) {
            for (matchInfo m : theMatches) {
                if ((m.isBase == b) && (m.matchName.equalsIgnoreCase(s))) {
                    theMatches.remove(m);
                }
            }
        }
    }

    public ArrayList<Pair<Boolean, String>> getMatches() {
        ArrayList<Pair<Boolean, String>> ret = null;
        if (theMatches != null) {
            ret = new ArrayList<>();
            for (matchInfo m : theMatches) {
                ret.add(new Pair<>(m.isBase, m.matchName));
            }
        }
        return ret;
    }

    public void addOutfit(String s) {
        if (theOutfits == null) {
            theOutfits = new ArrayList<>();
        }
        theOutfits.add(s);
    }

    public void removeOutfit(String s) {
        if (theOutfits != null){
            theOutfits.remove(s);
        }
    }
    
    public ArrayList<String> getOutfits(){
        return theOutfits;
    }

    public void addSet(boolean b, String s, int i) {
        if (theSets == null) {
            theSets = new ArrayList<>();
        }
        setInfo thisSet = new setInfo(b, s, i);
        theSets.add(thisSet);
    }
    
    public void removeSet(boolean b, String s, int i) {
        if (theSets != null) {
            for (setInfo set : theSets) {
                if ( (set.isBase == b) && (set.setName.equalsIgnoreCase(s) && (set.tier == i)) ){
                    theSets.remove(set);
                }
            }
        }
    }
    
    public ArrayList<Pair<String, Boolean>> getSets(){
        ArrayList<Pair<String, Boolean>> ret = null;
        if(theSets != null) {
            ret = new ArrayList<>();
            for (setInfo set : theSets) {
                String s = set.setName;
                if (!(set.isBase) ) {
                    s = s + "_" + set.tier;
                }
                Pair<String, Boolean> p = new Pair<>(s, set.isBase);
                ret.add(p);
            }
        }
        
        return ret;
    }
    
    
}
