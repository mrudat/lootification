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
    private String EDID;
    private String masterMod;
    private GRUP_TYPE type;
    
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
    
    RecordData(String id, String mod){
        
    }
    
    public String getMod(){
        return masterMod;
    }
    
    public void setType(GRUP_TYPE g) {
        type = g;
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
    
    public void addOutfit(String s){
        if (theOutfits == null) {
            theOutfits = new ArrayList<>();
        }
        theOutfits.add(s);
    }
    
    public void addSet(boolean b, String s, int i) {
        if (theSets == null){
            theSets = new ArrayList<>();
        }
        setInfo thisSet = new setInfo(b, s, i);
        theSets.add(thisSet);
    }
    
    public GRUP_TYPE getType() {
        return type;
    }
}
