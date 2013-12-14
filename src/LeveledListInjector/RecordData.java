/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    public String getTypeFriendly() {
        String ret;
        switch (getType()) {
            case ARMO:
                ret = "armor";
                break;
            case WEAP:
                ret = "weapon";
                break;
            case LVLI:
                ret = "leveledList";
                break;
            case OTFT:
                ret = "outfit";
                break;
            default:
                ret = null;
        }
        return ret;
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

    ArrayList<Pair<Boolean, String>> getMatches() {
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
        if (theOutfits != null) {
            theOutfits.remove(s);
        }
    }

    public ArrayList<String> getOutfits() {
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
                if ((set.isBase == b) && (set.setName.equalsIgnoreCase(s) && (set.tier == i))) {
                    theSets.remove(set);
                }
            }
        }
    }

    ArrayList<Pair<String, Boolean>> getSets() {
        ArrayList<Pair<String, Boolean>> ret = null;
        if (theSets != null) {
            ret = new ArrayList<>();
            for (setInfo set : theSets) {
                String s = set.setName;
                if (!(set.isBase)) {
                    s = s + "_" + set.tier;
                }
                Pair<String, Boolean> p = new Pair<>(s, set.isBase);
                ret.add(p);
            }
        }

        return ret;
    }

    void toXML(Node recNode) {
        NodeList matchNodes = ((Element) recNode).getElementsByTagName("match");
        NodeList outfitNodes = ((Element) recNode).getElementsByTagName("outfit");
        NodeList setNodes = ((Element) recNode).getElementsByTagName("set");

        if (SetupData.tags.match.allowedTag(getType())) {
            for (matchInfo match : theMatches) {
                boolean found = false;
                for (int i = 0; i < matchNodes.getLength(); i++) {
                    Node matchNode = matchNodes.item(i);
                    String matchName = matchNode.getTextContent();
                    boolean isBase = ((Element) matchNode).getAttribute("type").equalsIgnoreCase("base");
                    if (matchName.equalsIgnoreCase(match.matchName) && (isBase == match.isBase)) {
                        found = true;
                    }
                }
                if (!found) {
                    Element matchEl = recNode.getOwnerDocument().createElement("match");
                    if (match.isBase) {
                        matchEl.setAttribute("type", "base");
                    }
                    matchEl.setTextContent(match.matchName);
                    recNode.appendChild(matchEl);
                }
            }
        }
        
        if (SetupData.tags.outfit.allowedTag(getType())) {
            for (String outfit : theOutfits) {
                boolean found = false;
                for (int i = 0; i < outfitNodes.getLength(); i++) {
                    Node outfitNode = outfitNodes.item(i);
                    String outfitName = outfitNode.getTextContent();
                    if (outfitName.equalsIgnoreCase(outfit) ) {
                        found = true;
                    }
                }
                if (!found) {
                    Element outfitEl = recNode.getOwnerDocument().createElement("outfit");
                    outfitEl.setTextContent(outfit);
                    recNode.appendChild(outfitEl);
                }
            }
        }
        
        if (SetupData.tags.set.allowedTag(getType())) {
            for (setInfo set : theSets) {
                boolean found = false;
                for (int i = 0; i < setNodes.getLength(); i++) {
                    Node setNode = setNodes.item(i);
                    boolean setTier;
                    if(set.isBase) {
                        setTier = setNode.getTextContent().equalsIgnoreCase("");
                    }
                    else {
                        setTier = setNode.getTextContent().equalsIgnoreCase(Integer.toString(set.tier));
                    }
                    boolean setName = ((Element) setNode).getAttribute("set_name").equalsIgnoreCase(set.setName);
                    
                    if (setName && setTier) {
                        found = true;
                    }
                }
                if (!found) {
                    Element setEl = recNode.getOwnerDocument().createElement("set");
                    if(set.isBase) {
                    setEl.setTextContent("");
                    }
                    else {
                        setEl.setTextContent("" + set.tier);
                    }
                    setEl.setAttribute("set_name", set.setName);
                    recNode.appendChild(setEl);
                }
            }
        }
    }
}
