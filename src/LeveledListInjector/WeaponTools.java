/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import LeveledListInjector.RecordData.MatchInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import skyproc.*;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author David Tynan
 */
public class WeaponTools {

    private static final HashMap<FormID, ArrayList<WEAP>> weaponMatches = new HashMap<>();
    private static final HashMap<FormID, ArrayList<WEAP>> weaponVariants = new HashMap<>();
    private static Mod merger;
    private static Mod patch;
    private static final String lli_prefix = LeveledListInjector.lli_prefix;

    public static void setMergeAndPatch(Mod m, Mod p) {
        merger = m;
        patch = p;
    }

    static String generateWeaponEDID(WEAP newWeapon, WEAP weapon) {
        String name = newWeapon.getEDID();
        String baseName = weapon.getEDID();
        String templateName;
        String ret = "";
        WEAP template = (WEAP) merger.getMajor(weapon.getTemplate(), GRUP_TYPE.WEAP);
        if (template != null) {
            templateName = template.getEDID();
            if (baseName.contains(templateName)) {
                ret = baseName.replace(templateName, name);
            } else {
                String lcseq = lcs(baseName, templateName);
                if (baseName.contains(lcseq)) {
                    ret = baseName.replace(lcseq, name);
                } else {
                    String gcs = longestCommonSubstring(baseName, templateName);
                    ret = baseName.replace(gcs, name);
                }
            }
        }

        return ret;

    }

    static boolean weaponHasKeyword(WEAP rec, KYWD varKey, Mod m) {
        ArrayList<FormID> a;
        boolean hasKey = false;
        WEAP replace = rec;
        FormID tmp = replace.getTemplate();
        if (!tmp.isNull()) {
            replace = (WEAP) m.getMajor(tmp, GRUP_TYPE.WEAP);
        }
        KeywordSet k = replace.getKeywordSet();
        a = k.getKeywordRefs();
        for (FormID temp : a) {
            KYWD refKey = (KYWD) m.getMajor(temp, GRUP_TYPE.KYWD);
            //SPGlobal.log("formid", temp.toString());
            //SPGlobal.log("KYWD compare", refKey.getEDID() + " " + varKey.getEDID() + " " + (refKey.equals(varKey)));
            if (varKey.equals(refKey)) {
                hasKey = true;
            }
        }
        return hasKey;
    }

    /*static KYWD weaponHasAnyKeyword(WEAP rec, FLST f, Mod merger) {
     * ArrayList<FormID> a = f.getFormIDEntries();
     * KYWD hasKey = null;
     * //SPGlobal.log("Any keyword size", a.size() + "");
     * for (FormID temp : a) {
     * //SPGlobal.log("Any keyword", temp.getFormStr());
     * KYWD weaponKey = (KYWD) merger.getMajor(temp, GRUP_TYPE.KYWD);
     * if (weaponHasKeyword(rec, weaponKey)) {
     * hasKey = weaponKey;
     * continue;
     * }
     * }
     * return hasKey;
     * }*/
    static void buildWeaponVariants() throws Exception {

        for (WEAP theWeap : merger.getWeapons()) {
            FormID lookup = theWeap.getForm();
            if (theWeap.isTemplated()) {
                //add bad template check
                lookup = theWeap.getTemplate();
            }

            ArrayList<WEAP> matches = weaponMatches.get(lookup);

            if (matches != null) {
                // theWeap is a base weapon and has matches
                ArrayList<WEAP> vars = weaponVariants.get(theWeap.getForm());
                if (vars != null) {
                    throw new Exception("Variants already defined");
                }

                vars = new ArrayList<>();
                weaponVariants.put(theWeap.getForm(), vars);

                FormID enchantment = theWeap.getEnchantment();
                if (enchantment.isNull()) {
                    for (WEAP w : matches) {
                        vars.add(w);
                    }
                } else {
                    for (WEAP w : matches) {
                        String name = generateWeaponName(w, theWeap);
                        String newEdid = generateWeaponEDID(w, theWeap);
                        WEAP weaponDupe = (WEAP) patch.makeCopy(w, "DienesWEAP" + newEdid);

                        weaponDupe.setEnchantment(enchantment);
                        weaponDupe.setEnchantmentCharge(theWeap.getEnchantmentCharge());
                        weaponDupe.setTemplate(w.getForm());
                        weaponDupe.setName(name);

                        vars.add(weaponDupe);
                        patch.addRecord(weaponDupe);
                    }
                }
            }
        }
    }

    static String generateWeaponName(WEAP newWeapon, WEAP weapon) {
        String name = newWeapon.getName();
        String baseName = weapon.getName();
        String templateName;
        String ret = "";
        WEAP template = (WEAP) merger.getMajor(weapon.getTemplate(), GRUP_TYPE.WEAP);
        if (template != null) {
            templateName = template.getName();
            if (baseName.contains(templateName)) {
                ret = baseName.replace(templateName, name);
            } else {
                String lcseq = lcs(baseName, templateName);
                if (baseName.contains(lcseq)) {
                    ret = baseName.replace(lcseq, name);
                } else {
                    String gcs = longestCommonSubstring(baseName, templateName);
                    ret = baseName.replace(gcs, name);
                }
            }
        }

        return ret;
    }

    static void linkLVLIWeapons() {

        for (LVLI llist : merger.getLeveledItems()) {
            if (!llist.getEDID().startsWith(lli_prefix)) {
                if (!llist.isEmpty()) {
                    boolean changed = false;
                    for (int i = 0; i < llist.numEntries(); i++) {
                        LeveledEntry entry = llist.getEntry(i);
                        WEAP obj = (WEAP) merger.getMajor(entry.getForm(), GRUP_TYPE.WEAP);
                        if (obj != null) {

                            boolean hasVar = weaponVariants.containsKey(obj.getForm());
                            if (hasVar) {
                                String eid = lli_prefix + obj.getEDID();
                                MajorRecord r = patch.getMajor(eid, GRUP_TYPE.LVLI);
                                if (r != null) {
                                    llist.removeEntry(i);
                                    llist.addEntry(new LeveledEntry(r.getForm(), entry.getLevel(), entry.getCount()));
                                    changed = true;
                                    i = -1;
                                }
                            }
                        }
                    }
                    if (changed) {
                        patch.addRecord(llist);
                    }
                }
            }
        }
    }

    static void setupWeaponMatches() throws Exception {
        KYWD axekey = (KYWD) merger.getMajor("WeapTypeBattleaxe", GRUP_TYPE.KYWD);
        KYWD hammerkey = (KYWD) merger.getMajor("WeapTypeWarhammer", GRUP_TYPE.KYWD);

        HashMap<String, Pair<ArrayList<WEAP>, ArrayList<WEAP>>> setup = new HashMap<>();
        // add all weapons to setup hashmap
        for (WEAP theWeap : merger.getWeapons()) {
            String fullID = theWeap.getEDID();
            RecordDataWEAP rec = LeveledListInjector.parsedWEAP.get(fullID);
            if (rec != null) {
                Set<MatchInfo> matches = rec.getMatches();
                //if match is defined in xml
                if (matches != null) {
                    for (MatchInfo match : matches) {
                        Pair<ArrayList<WEAP>, ArrayList<WEAP>> vars = setup.get(match.getMatchName());
                        // if MatchName is not yet entered in setup
                        if (vars == null) {
                            ArrayList<WEAP> bases = new ArrayList<>();
                            ArrayList<WEAP> alts = new ArrayList<>();
                            if (match.getIsBase()) {
                                bases.add(theWeap);
                            } else {
                                alts.add(theWeap);
                            }
                            Pair<ArrayList<WEAP>, ArrayList<WEAP>> newVar = new Pair<>(bases, alts);
                            setup.put(match.getMatchName(), newVar);
                        } // if MatchName is in setup already
                        else {
                            // theWeap declared as base
                            if (match.getIsBase()) {
                                vars.getBase().add(theWeap);
                            } // if theWeap declared as var
                            else {
                                vars.getVar().add(theWeap);
                            }
                        }
                    }
                }
            }
        }
        // match base weapons to variant weapons in setup andd add them to WeaponMatches
        for (Pair<ArrayList<WEAP>, ArrayList<WEAP>> p : setup.values()) {
            //compare each base weapon to each var weapon
            for (WEAP theBase : p.getBase()) {
                ArrayList<WEAP> matches = weaponMatches.get(theBase.getForm());
                for (WEAP theVar : p.getVar()) {
                    boolean ismatch = false;
                    if (theBase.getWeaponType().equals(theVar.getWeaponType())) {
                        // check if 2h axe or hammer
                        if (theBase.getWeaponType().equals(WEAP.WeaponType.TwoHBluntAxe)) {
                            // check if both have same keyword
                            if ((weaponHasKeyword(theBase, axekey, merger) && weaponHasKeyword(theVar, axekey, merger))
                                    || (weaponHasKeyword(theBase, hammerkey, merger) && weaponHasKeyword(theVar, hammerkey, merger))) {
                                ismatch = true;
                            }
                        } else {
                            ismatch = true;
                        }
                    }
                    if (ismatch) {
                        if (matches == null) {
                            matches = new ArrayList<>();
                            weaponMatches.put(theBase.getForm(), matches);
                        }
                        matches.add(theVar);
                    }

                }
            }
        }
    }

    static void buildOutfitWeapons() {

        for (OTFT lotft : merger.getOutfits()) {
            ArrayList<FormID> a = lotft.getInventoryList();
            boolean changed = false;
            for (FormID form : a) {

                WEAP weapon = (WEAP) merger.getMajor(form, GRUP_TYPE.WEAP);
                if (weapon != null) {
                    boolean isBase = weaponMatches.containsKey(weapon.getForm());

                    if (isBase) {
                        String eid = lli_prefix + weapon.getEDID();
                        MajorRecord r = patch.getMajor(eid, GRUP_TYPE.LVLI);
                        if (r != null) {
                            lotft.removeInventoryItem(form);
                            lotft.addInventoryItem(r.getForm());
                            changed = true;
                        }
                    }
                }
            }
            if (changed) {
                patch.addRecord(lotft);
            }
        }
    }

    public static void modLVLIWeapons() {
        for (WEAP theWeap : merger.getWeapons()) {
            ArrayList<WEAP> vars = weaponVariants.get(theWeap.getForm());
            if (vars != null) {
                LVLI varsList = new LVLI(lli_prefix + theWeap.getEDID());
                try {
                    varsList.setChanceNone(0);
                } catch (BadParameter e) {
                }
                varsList.set(LeveledRecord.LVLFlag.UseAll, false);
                varsList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, true);
                varsList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                varsList.addEntry(theWeap.getForm(), 1, 1);

                for (WEAP w : vars) {
                    varsList.addEntry(w.getForm(), 1, 1);
                }
                patch.addRecord(varsList);
            }
        }

    }

    private static String longestCommonSubstring(String S1, String S2) {
        int Start = 0;
        int Max = 0;
        for (int i = 0; i < S1.length(); i++) {
            for (int j = 0; j < S2.length(); j++) {
                int x = 0;
                while (S1.charAt(i + x) == S2.charAt(j + x)) {
                    x++;
                    if (((i + x) >= S1.length()) || ((j + x) >= S2.length())) {
                        break;
                    }
                }
                if (x > Max) {
                    Max = x;
                    Start = i;
                }
            }
        }
        return S1.substring(Start, (Start + Max));
    }

    public static String lcs(String a, String b) {
        int[][] lengths = new int[a.length() + 1][b.length() + 1];

        // row 0 and column 0 are initialized to 0 already

        for (int i = 0; i < a.length(); i++) {
            for (int j = 0; j < b.length(); j++) {
                if (a.charAt(i) == b.charAt(j)) {
                    lengths[i + 1][j + 1] = lengths[i][j] + 1;
                } else {
                    lengths[i + 1][j + 1] = Math.max(lengths[i + 1][j], lengths[i][j + 1]);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int x = a.length(), y = b.length(); x != 0 && y != 0;) {
            if (lengths[x][y] == lengths[x - 1][y]) {
                x--;
            } else if (lengths[x][y] == lengths[x][y - 1]) {
                y--;
            } else {
                assert a.charAt(x - 1) == b.charAt(y - 1);
                sb.append(a.charAt(x - 1));
                x--;
                y--;
            }
        }

        return sb.reverse().toString();
    }
}
