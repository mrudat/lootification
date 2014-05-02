/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.Objects;
import skyproc.*;

/**
 *
 * @author David
 */
public abstract class RecordData {

    private final String EDID;
    private final String masterMod;
    private final GRUP_TYPE type;
    

    public static class MatchInfo {

        public MatchInfo(String matchName) {
            this.matchName = matchName;
            this.isBase = false;
            this.keywordEDID = null;
        }

        public boolean getIsBase() {
            return isBase;
        }

        public String getMatchName() {
            return matchName;
        }

        public String getKeywordEDID() {
            return keywordEDID;
        }

        public MatchInfo(boolean isBase, String matchName, String keywordEDID) {
            this.isBase = isBase;
            this.matchName = matchName;
            this.keywordEDID = keywordEDID;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + (this.isBase ? 1 : 0);
            hash = 23 * hash + Objects.hashCode(this.matchName);
            hash = 23 * hash + Objects.hashCode(this.keywordEDID);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MatchInfo other = (MatchInfo) obj;
            if (this.isBase != other.isBase) {
                return false;
            }
            if (!Objects.equals(this.matchName, other.matchName)) {
                return false;
            }
            return Objects.equals(this.keywordEDID, other.keywordEDID);
        }
        private final boolean isBase;
        private final String matchName;
        private final String keywordEDID;
    }

    public static class TieredSet {

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.setName);
            hash = 79 * hash + this.tier;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TieredSet other = (TieredSet) obj;
            if (!Objects.equals(this.setName, other.setName)) {
                return false;
            }
            return this.tier == other.tier;
        }

        private final String setName;
        private final int tier;

        TieredSet(String s, int i) {
            setName = s;
            tier = i;
        }
        
        public String getName(){
            return setName;
        }
        
        public int getTier(){
            return tier;
        }
    }

    RecordData(String id, String mod, GRUP_TYPE g) {
        EDID = id;
        masterMod = mod;
        type = g;
    }
    
    RecordData(MajorRecordNamed rec) {
        EDID = rec.getEDID();
        masterMod = rec.getFormMaster().print();
        type = GRUP_TYPE.valueOf(rec.getType());
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
}
