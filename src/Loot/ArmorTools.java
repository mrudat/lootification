/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Loot;

import Loot.RecordData.MatchInfo;
import Loot.RecordData.TieredSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import lev.LFlags;
import skyproc.*;
import skyproc.exceptions.BadParameter;

/**
 *
 * @author David Tynan
 */
public class ArmorTools {

    private static final HashMap<FormID, ArrayList<ARMO>> armorMatches = new HashMap<>();
    private static final HashMap<FormID, ArrayList<ARMO>> armorVariants = new HashMap<>();
    private static final HashMap<String, ArrayList<ARMO>> matchingOutfits = new HashMap<>(0);
    private static final HashMap<String, ArrayList<ARMO>> matchingSets = new HashMap<>(0);
    private static Mod merger;
    private static Mod patch;
    private static final String lli_prefix = LLI.lli_prefix;

    public static void setMergeAndPatch(Mod m, Mod p) {
        merger = m;
        patch = p;
    }

    static void buildOutfitsArmors() {
        boolean use = LLI.save.getBool(YourSaveFile.Settings.USE_MATCHING_OUTFITS);

        for (OTFT lotft : merger.getOutfits()) {
            String lotftName = lotft.getEDID();
            RecordDataOTFT rec = LLI.parsedOTFT.get(lotftName);

            if (use) {
                boolean tiered = false;
                if (rec != null) {
                    Set<RecordData.TieredSet> sets = rec.getTieredSets();
                    if (sets != null) {
                        tiered = true;
                    }
                }
                if (tiered) {
                    LFlags flags = getFlags(lotft.getInventoryList());
                    String subName = lli_prefix + lotftName + flags.toString();
                    LVLI subList = (LVLI) patch.getMajor(subName, GRUP_TYPE.LVLI);
                    if (subList == null) {
                        subList = new LVLI(subName);
                        subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                        subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                        subList.set(LeveledRecord.LVLFlag.UseAll, false);
                        try {
                            subList.setChanceNone(0);
                        } catch (BadParameter ex) {
                        }
                    }

                    insertTieredArmors(subList, lotft, flags);

                    patch.addRecord(subList);
                    patch.addRecord(lotft);
                } // outfit not tiered but are using matched sets
                // find matching sets in outfit
                else {
                    String biggestSetName = getBiggestSetName(lotft.getInventoryList());
                    ArrayList<FormID> biggestSet = getSet(lotft.getInventoryList(), biggestSetName);
                    while (biggestSet.size() > 1) {
                        //remove set from outfit
                        for (FormID f : biggestSet) {
                            lotft.removeInventoryItem(f);
                        }

                        //link set lvli
                        LFlags flags = getFlags(biggestSet);
                        String listName = lli_prefix + biggestSetName + flags.toString();
                        LVLI subList = (LVLI) patch.getMajor(listName, GRUP_TYPE.LVLI);
                        if (subList == null) {
                            subList = new LVLI(listName);
                            subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                            subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                            subList.set(LeveledRecord.LVLFlag.UseAll, true);
                            try {
                                subList.setChanceNone(0);
                            } catch (BadParameter ex) {
                            }

                            for (FormID f : biggestSet) {
                                subList.addEntry(f, 1, 1);
                            }

                        }
                        lotft.addInventoryItem(subList.getForm());
                        patch.addRecord(subList);
                        patch.addRecord(lotft);

                        // refresh
                        biggestSetName = getBiggestSetName(lotft.getInventoryList());
                        biggestSet = getSet(lotft.getInventoryList(), biggestSetName);
                    }
                }
            } // not using matching sets
            // Or using matched sets but non-matched armors remain
            else {
                boolean changed = false;

                ArrayList<FormID> a = lotft.getInventoryList();
                for (int i = 0; i < a.size(); i++) {
                    FormID form = a.get(i);
                    ARMO arm = (ARMO) merger.getMajor(form, GRUP_TYPE.ARMO);
                    if (arm != null) {
                        LVLI subList = (LVLI) patch.getMajor(lli_prefix + arm.getEDID(), GRUP_TYPE.LVLI);
                        if (subList != null) {
                            lotft.removeInventoryItem(form);
                            lotft.addInventoryItem(subList.getForm());
                            changed = true;
                            // hack because otft lists are weird
                            a = lotft.getInventoryList();
                            i = -1;
                        }
                    }
                }
                if (changed) {
                    patch.addRecord(lotft);
                }
            }
        }
    }

    /*public static ArrayList<FormID> containsArmorSet(ArrayList<FormID> inventory, Mod merger) {
     * ArrayList<FormID> set = new ArrayList<>(0);
     * ArrayList<String> suffixes = new ArrayList<>(Arrays.asList("Boots", "Cuirass", "Gauntlets", "Helmet", "Shield"));
     * boolean matchFound = false;
     * for (int count = 0; count < inventory.size() && matchFound == false; count++) {
     * ARMO obj = (ARMO) merger.getMajor(inventory.get(count), GRUP_TYPE.ARMO);
     * if (obj != null) {
     * String armorType;
     * String name = obj.getEDID();
     * if (name.startsWith("Ench")) {
     * name = name.substring(4);
     * }
     * int i;
     * for (String s : suffixes) {
     * i = name.indexOf(s);
     * if (i > 0) {
     * name = name.substring(0, i);
     * }
     * }
     * armorType = name;
     * for (int rest = count; rest < inventory.size(); rest++) {
     * ARMO other = (ARMO) merger.getMajor(inventory.get(count), GRUP_TYPE.ARMO);
     * if (other != null) {
     * String compare = other.getEDID();
     * if (compare.contains(armorType)) {
     * set.add(other.getForm());
     * matchFound = true;
     * }
     * }
     * }
     * if (matchFound) {
     * set.add(obj.getForm());
     * }
     * }
     * }
     * return set;
     * }*/

    /*private static ArrayList<FormID> lvliContainsArmorSet(LVLI llist, Mod merger) {
     * ArrayList<FormID> contentForms = new ArrayList<>(0);
     * ArrayList<LeveledEntry> levContents = llist.getEntries();
     * 
     * for (LeveledEntry levEntry : levContents) {
     * contentForms.add(levEntry.getForm());
     * }
     * ArrayList<FormID> ret = containsArmorSet(contentForms, merger);
     * 
     * return ret;
     * }*/
    static void linkLVLIArmors() {
        for (LVLI llist : merger.getLeveledItems()) {
            if (!llist.getEDID().startsWith(lli_prefix)) {
                if (!llist.isEmpty()) {
                    boolean changed = false;
                    for (int i = 0; i < llist.numEntries(); i++) {
                        LeveledEntry entry = llist.getEntry(i);
                        ARMO obj = (ARMO) merger.getMajor(entry.getForm(), GRUP_TYPE.ARMO);
                        if (obj != null) {

                            boolean hasVar = armorVariants.containsKey(obj.getForm());
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

    /*static void buildArmorBases(Mod merger, FLST baseKeys) {
     * for (ARMO armor : merger.getArmors()) {
     * KYWD baseKey = armorHasAnyKeyword(armor, baseKeys, merger);
     * if (baseKey != null) {
     * //SPGlobal.log(armor.getEDID(), "is base armor");
     * ArrayList<FormID> alts = new ArrayList<>(0);
     * alts.add(0, armor.getForm());
     * armorVariants.add(alts);
     * }
     * }
     * }*/
    static void buildArmorVariants() throws Exception {
        for (ARMO theArmor : merger.getArmors()) {
            FormID lookup = theArmor.getForm();
            if (!FormID.NULL.equals(theArmor.getTemplate())) {
                //add bad template check
                lookup = theArmor.getTemplate();
            }

            ArrayList<ARMO> matches = armorMatches.get(lookup);

            if (matches != null) {
                // theWeap is a base weapon and has matches
                ArrayList<ARMO> vars = armorVariants.get(theArmor.getForm());
                if (vars != null) {
                    throw new Exception("Variants already defined");
                }

                vars = new ArrayList<>();
                armorVariants.put(theArmor.getForm(), vars);

                FormID enchantment = theArmor.getEnchantment();
                if (enchantment.isNull()) {
                    for (ARMO w : matches) {
                        vars.add(w);
                    }
                } else {
                    for (ARMO w : matches) {
                        String name = generateArmorName(w, theArmor);
                        String newEdid = generateArmorEDID(w, theArmor);
                        ARMO armorDupe = (ARMO) patch.makeCopy(w, lli_prefix + newEdid);

                        armorDupe.setEnchantment(enchantment);
                        armorDupe.setTemplate(w.getForm());
                        armorDupe.setName(name);

                        vars.add(armorDupe);
                        patch.addRecord(armorDupe);
                    }
                }
            }
        }

    }

    /*static String getSetName(ArrayList<FormID> set) {
     * String name = String.valueOf(set.hashCode());
     * return name;
     * }
     * 
     * static KYWD getBaseArmor(KYWD k) {
     * KYWD ret = null;
     * for (Pair p : armorMatches) {
     * KYWD var = (KYWD) p.getVar();
     * //SPGlobal.log("getBaseArmor", k.getEDID() + " " + var.getEDID() + " " + var.equals(k));
     * if (var.equals(k)) {
     * ret = (KYWD) p.getBase();
     * }
     * }
     * return ret;
     * }*/
    static void setupArmorMatches() {

        HashMap<String, Pair<ArrayList<ARMO>, ArrayList<ARMO>>> setup = new HashMap<>();
        // add all weapons to setup hashmap
        for (ARMO theArmo : merger.getArmors()) {
            String fullID = theArmo.getEDID();
            RecordDataARMO rec = LLI.parsedARMO.get(fullID);
            if (rec != null) {
                Set<MatchInfo> matches = rec.getMatches();
                //if match is defined in xml
                if (matches != null) {
                    for (MatchInfo match : matches) {
                        Pair<ArrayList<ARMO>, ArrayList<ARMO>> vars = setup.get(match.getMatchName());
                        // if MatchName is not yet entered in setup
                        if (vars == null) {
                            ArrayList<ARMO> bases = new ArrayList<>();
                            ArrayList<ARMO> alts = new ArrayList<>();
                            if (match.getIsBase()) {
                                bases.add(theArmo);
                            } else {
                                alts.add(theArmo);
                            }
                            Pair<ArrayList<ARMO>, ArrayList<ARMO>> newVar = new Pair<>(bases, alts);
                            setup.put(match.getMatchName(), newVar);
                        } // if MatchName is in setup already
                        else {
                            // theWeap declared as base
                            if (match.getIsBase()) {
                                vars.getBase().add(theArmo);
                            } // if theWeap declared as var
                            else {
                                vars.getVar().add(theArmo);
                            }
                        }
                    }
                }
            }
        }
        // match base armors to variant armors in setup andd add them to armorMatches
        for (Pair<ArrayList<ARMO>, ArrayList<ARMO>> p : setup.values()) {
            //compare each base armor to each var armor
            for (ARMO theBase : p.getBase()) {
                ArrayList<ARMO> matches = armorMatches.get(theBase.getForm());
                for (ARMO theVar : p.getVar()) {
                    if (compareArmorSlots(theBase, theVar)) {

                        if (matches == null) {
                            matches = new ArrayList<>();
                            armorMatches.put(theBase.getForm(), matches);
                        }
                        matches.add(theVar);
                    }

                }
            }
        }
    }

    static String generateArmorEDID(ARMO newArmor, ARMO armor) {
        String name = newArmor.getEDID();
        String baseName = armor.getEDID();
        String templateName;
        String ret = "";
        ARMO template = (ARMO) merger.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
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

    static String generateArmorName(ARMO newArmor, ARMO armor) {
        String name = newArmor.getName();
        String baseName = armor.getName();
        String templateName;
        String ret = "";
        ARMO template = (ARMO) merger.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
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

    /*static KYWD armorHasAnyKeyword(ARMO rec, FLST f, Mod m) {
     * ArrayList<FormID> a = f.getFormIDEntries();
     * KYWD hasKey = null;
     * for (int i = 0; i < a.size(); i++) {
     * FormID temp = (FormID) a.get(i);
     * KYWD armorKey = (KYWD) m.getMajor(temp, GRUP_TYPE.KYWD);
     * if (armorHasKeyword(rec, armorKey, m)) {
     * hasKey = armorKey;
     * continue;
     * }
     * }
     * //SPGlobal.log("HasAnyKeyword", rec.toString() + " " + hasKey);
     * return hasKey;
     * }*/
    static boolean armorHasKeyword(ARMO rec, KYWD varKey, Mod m) {
        ArrayList<FormID> a;
        boolean hasKey = false;
        ARMO replace = rec;
        FormID tmp = replace.getTemplate();
        //SPGlobal.log("hasKeyword", varKey.getEDID() + " " + replace.getEDID() + " " + tmp.getFormStr());
        if (!tmp.isNull()) {
            replace = (ARMO) m.getMajor(tmp, GRUP_TYPE.ARMO);
        }
        //SPGlobal.log(replace.getEDID(), varKey.getEDID());
        KeywordSet k = replace.getKeywordSet();
        a = k.getKeywordRefs();
        for (FormID temp : a) {
            KYWD refKey = (KYWD) m.getMajor(temp, GRUP_TYPE.KYWD);
            //SPGlobal.log("formid", temp.toString());
            //SPGlobal.log("KYWD compare", refKey.getEDID() + " " + varKey.getEDID() + " " + (varKey.equals(refKey)));
            if (varKey.equals(refKey)) {
                hasKey = true;
            }
        }
        return hasKey;
    }

    /*static private void linkArmorSet(LVLI llist, ArrayList<FormID> set, Mod merger, Mod patch) {
     * String eid = "DienesLVLI" + getSetName(set) + "level1";
     * LVLI r = (LVLI) merger.getMajor(eid, GRUP_TYPE.LVLI);
     * FormID f = new FormID("107347", "Skyrim.esm");
     * MajorRecord glist = merger.getMajor(f, GRUP_TYPE.LVLI);
     * 
     * if (r == null) {
     * LVLI setList = (LVLI) patch.makeCopy(glist, eid);
     * for (int index = 0; index < set.size(); index++) {
     * FormID item = set.get(index);
     * ARMO temp = (ARMO) merger.getMajor(item, GRUP_TYPE.ARMO);
     * if (temp != null) {
     * setList.addEntry(item, 1, 1);
     * for (int i = 0; i < llist.numEntries(); i++) {
     * FormID tempForm = llist.getEntry(i).getForm();
     * if (item.equals(tempForm)) {
     * llist.removeEntry(i);
     * continue;
     * }
     * }
     * 
     * index = index - 1;
     * }
     * }
     * merger.addRecord(setList);
     * llist.addEntry(setList.getForm(), 1, 1);
     * //matchingSetVariants.add(setList.getForm());
     * 
     * } else {
     * for (int index = 0; index < set.size(); index++) {
     * FormID item = set.get(index);
     * for (int i = 0; i < llist.numEntries(); i++) {
     * FormID tempForm = llist.getEntry(i).getForm();
     * if (item.equals(tempForm)) {
     * llist.removeEntry(i);
     * continue;
     * }
     * }
     * 
     * index = index - 1;
     * }
     * llist.addEntry(r.getForm(), 1, 1);
     * 
     * }
     * }*/
    public static void modLVLIArmors() {
        for (ARMO theArmor : merger.getArmors()) {
            ArrayList<ARMO> vars = armorVariants.get(theArmor.getForm());
            if (vars != null) {
                LVLI varsList = new LVLI(lli_prefix + theArmor.getEDID());
                try {
                    varsList.setChanceNone(0);
                } catch (BadParameter e) {
                }
                varsList.set(LeveledRecord.LVLFlag.UseAll, false);
                varsList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, true);
                varsList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                varsList.addEntry(theArmor.getForm(), 1, 1);

                for (ARMO w : vars) {
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

    static void setupMatchingOutfitsSets() {
        for (ARMO armor : merger.getArmors()) {
            for (ARMO theArmo : merger.getArmors()) {
                String fullID = theArmo.getEDID();
                RecordDataARMO rec = LLI.parsedARMO.get(fullID);
                if (rec != null) {
                    for (String name : rec.getOutfits()) {
                        ArrayList<ARMO> outfit = matchingOutfits.get(name);
                        if (outfit == null) {
                            outfit = new ArrayList<>();
                            matchingOutfits.put(name, outfit);
                        }
                        outfit.add(armor);
                    }
                    for (RecordData.TieredSet p : rec.getTieredSets()) {
                        ArrayList<ARMO> a = matchingSets.get(p.getName());
                        if (a == null) {
                            a = new ArrayList<>();
                            matchingSets.put(fullID, a);
                        }
                        a.add(armor);
                    }
                }
            }
        }
    }

    static KYWD hasKeyStartsWith(ARMO armor, String start, Mod merger) {
        KYWD ret = null;

        ArrayList<FormID> a;

        ARMO replace = armor;
        FormID tmp = replace.getTemplate();
        //SPGlobal.log("hasKeyword", varKey.getEDID() + " " + replace.getEDID() + " " + tmp.getFormStr());
        if (!tmp.isNull()) {
            replace = (ARMO) merger.getMajor(tmp, GRUP_TYPE.ARMO);
        }
        //SPGlobal.log(replace.getEDID(), varKey.getEDID());
        KeywordSet k = replace.getKeywordSet();
        a = k.getKeywordRefs();
        for (FormID temp : a) {
            KYWD refKey = (KYWD) merger.getMajor(temp, GRUP_TYPE.KYWD);
            //SPGlobal.log("formid", temp.toString());
            //SPGlobal.log("KYWD compare", refKey.getEDID() + " " + varKey.getEDID() + " " + (varKey.equals(refKey)));

            if (refKey.getEDID().startsWith(start)) {
                ret = refKey;
            }
        }

        return ret;
    }

    /*static ArrayList<ARMO> getAllWithKey(KYWD key, ArrayList<FormID> a, Mod merger) {
     * ArrayList<ARMO> ret = new ArrayList<>(0);
     * for (FormID f : a) {
     * ARMO arm = (ARMO) merger.getMajor(f, GRUP_TYPE.ARMO);
     * if (arm != null) {
     * if (armorHasKeyword(arm, key, merger)) {
     * ret.add(arm);
     * }
     * }
     * }
     * return ret;
     * }*/

    /*static ArrayList<ARMO> getAllWithKeyARMO(KYWD key, ArrayList<ARMO> a, Mod merger) {
     * ArrayList<ARMO> ret = new ArrayList<>(0);
     * for (ARMO arm : a) {
     * 
     * if (armorHasKeyword(arm, key, merger)) {
     * ret.add(arm);
     * }
     * 
     * }
     * return ret;
     * }*/

    /*static String getNameFromArrayWithKey(ArrayList<ARMO> a, KYWD k, Mod merger) {
     * String ret = null;
     * if (k.getEDID().contains("dienes_outfit")) {
     * ret = "DienesLVLIOutfit" + k.getEDID().substring(13);
     * } else {
     * ret = "DienesLVLIOutfit" + k.getEDID();
     * }
     * boolean h = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
     * || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
     * || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
     * h = true;
     * break;
     * }
     * }
     * boolean c = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
     * c = true;
     * break;
     * }
     * }
     * boolean g = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
     * g = true;
     * break;
     * }
     * }
     * boolean b = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
     * b = true;
     * break;
     * }
     * }
     * KYWD shield = (KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD);
     * boolean s = false;
     * for (ARMO arm : a) {
     * if (armorHasKeyword(arm, shield, merger)) {
     * s = true;
     * break;
     * }
     * }
     * if (h) {
     * ret = ret + "H";
     * }
     * if (c) {
     * ret = ret + "C";
     * }
     * if (g) {
     * ret = ret + "G";
     * }
     * if (b) {
     * ret = ret + "B";
     * }
     * if (s) {
     * ret = ret + "S";
     * }
     * 
     * return ret;
     * }*/

    /*static void addArmorFromArray(LVLI list, ArrayList<ARMO> a, Mod merger, Mod patch) {
     * FormID f = new FormID("107347", "Skyrim.esm");
     * LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
     * 
     * ArrayList<ARMO> h = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD), a, merger);
     * ArrayList<ARMO> c = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorCuirass", GRUP_TYPE.KYWD), a, merger);
     * ArrayList<ARMO> g = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorGauntlets", GRUP_TYPE.KYWD), a, merger);
     * ArrayList<ARMO> b = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorBoots", GRUP_TYPE.KYWD), a, merger);
     * ArrayList<ARMO> s = getAllWithKeyARMO((KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD), a, merger);
     * 
     * if (h.size() > 1) {
     * String name = "DienesLVLI_" + hasKeyStartsWith(h.get(0), "dienes_outfit", merger).getEDID() + "HelmetsSublist";
     * LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
     * if (subList == null) {
     * subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
     * }
     * if (subList != null) {
     * list.addEntry(subList.getForm(), 1, 1);
     * } else {
     * subList = (LVLI) patch.makeCopy(glist, name);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * for (ARMO arm : h) {
     * subList.addEntry(arm.getForm(), 1, 1);
     * }
     * patch.addRecord(subList);
     * }
     * } else if (h.size() == 1) {
     * list.addEntry(h.get(0).getForm(), 1, 1);
     * }
     * if (c.size() > 1) {
     * String name = "DienesLVLI_" + hasKeyStartsWith(c.get(0), "dienes_outfit", merger).getEDID() + "CuirassesSublist";
     * LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
     * if (subList == null) {
     * subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
     * }
     * if (subList != null) {
     * list.addEntry(subList.getForm(), 1, 1);
     * } else {
     * subList = (LVLI) patch.makeCopy(glist, name);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * for (ARMO arm : c) {
     * subList.addEntry(arm.getForm(), 1, 1);
     * }
     * patch.addRecord(subList);
     * }
     * } else if (c.size() == 1) {
     * list.addEntry(c.get(0).getForm(), 1, 1);
     * }
     * if (g.size() > 1) {
     * String name = "DienesLVLI_" + hasKeyStartsWith(g.get(0), "dienes_outfit", merger).getEDID() + "GauntletsSublist";
     * LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
     * if (subList == null) {
     * subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
     * }
     * if (subList != null) {
     * list.addEntry(subList.getForm(), 1, 1);
     * } else {
     * subList = (LVLI) patch.makeCopy(glist, name);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * for (ARMO arm : g) {
     * subList.addEntry(arm.getForm(), 1, 1);
     * }
     * patch.addRecord(subList);
     * }
     * } else if (g.size() == 1) {
     * list.addEntry(g.get(0).getForm(), 1, 1);
     * }
     * if (b.size() > 1) {
     * String name = "DienesLVLI_" + hasKeyStartsWith(b.get(0), "dienes_outfit", merger).getEDID() + "BootsSublist";
     * LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
     * if (subList == null) {
     * subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
     * }
     * if (subList != null) {
     * list.addEntry(subList.getForm(), 1, 1);
     * } else {
     * subList = (LVLI) patch.makeCopy(glist, name);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * for (ARMO arm : b) {
     * subList.addEntry(arm.getForm(), 1, 1);
     * }
     * patch.addRecord(subList);
     * }
     * } else if (b.size() == 1) {
     * list.addEntry(b.get(0).getForm(), 1, 1);
     * }
     * if (s.size() > 1) {
     * String name = "DienesLVLI_" + hasKeyStartsWith(s.get(0), "dienes_outfit", merger).getEDID() + "ShieldsSublist";
     * LVLI subList = (LVLI) merger.getMajor(name, GRUP_TYPE.LVLI);
     * if (subList == null) {
     * subList = (LVLI) patch.getMajor(name, GRUP_TYPE.LVLI);
     * }
     * if (subList != null) {
     * list.addEntry(subList.getForm(), 1, 1);
     * } else {
     * subList = (LVLI) patch.makeCopy(glist, name);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * for (ARMO arm : s) {
     * subList.addEntry(arm.getForm(), 1, 1);
     * }
     * patch.addRecord(subList);
     * }
     * } else if (s.size() == 1) {
     * list.addEntry(s.get(0).getForm(), 1, 1);
     * }
     * 
     * }*/

    /*static void addAlternateSets(LVLI list, ArrayList<ARMO> a, Mod merger, Mod patch) {
     * FormID f = new FormID("107347", "Skyrim.esm");
     * //SPGlobal.log("outfits glist", f.toString());
     * LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
     * 
     * KYWD k = null;
     * ArrayList<Pair<KYWD, ArrayList<ARMO>>> varSets = new ArrayList<>(0);
     * for (ARMO arm : a) {
     * k = hasKeyStartsWith(arm, "dienes_outfit", merger);
     * for (Pair<KYWD, ArrayList<ARMO>> p1 : matchingOutfits) {
     * 
     * boolean key = k.equals(p1.getBase());
     * 
     * if (key) {
     * for (ARMO armor : p1.getVar()) {
     * boolean passed = true;
     * for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
     * boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
     * boolean formFlag = arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
     * 
     * boolean flagMatch = (armorFlag == formFlag);
     * 
     * if (flagMatch == false) {
     * passed = false;
     * }
     * }
     * if (!passed) {
     * KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
     * if (armorHasKeyword(arm, helm, merger) && armorHasKeyword(armor, helm, merger)) {
     * passed = true;
     * }
     * }
     * if (passed) {
     * boolean found = false;
     * KYWD slotKey = getSlotKYWD(armor, merger);
     * if (slotKey == null) {
     * int test = 1;
     * } else {
     * for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {
     * if (p.getBase().equals(slotKey)) {
     * ArrayList<ARMO> q = p.getVar();
     * if (!q.contains(armor)) {
     * q.add(armor);
     * }
     * found = true;
     * break;
     * }
     * }
     * if (found == false) {
     * Pair<KYWD, ArrayList<ARMO>> p = new Pair(slotKey, new ArrayList<ARMO>(0));
     * p.getVar().add(armor);
     * varSets.add(p);
     * }
     * }
     * }
     * }
     * }
     * }
     * }
     * 
     * String bits = getBitsFromArray(a, merger);
     * for (char c : bits.toCharArray()) {
     * for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {
     * 
     * if (arrayHasBits(p.getVar(), String.valueOf(c), merger)) {
     * if (p.getVar().size() > 1) {
     * String lvliName = getNameFromArrayWithKey(p.getVar(), k, merger) + "variants";
     * LVLI list2 = (LVLI) patch.getMajor(lvliName, GRUP_TYPE.LVLI);
     * if (list2 != null) {
     * list.addEntry(list2.getForm(), 1, 1);
     * patch.addRecord(list);
     * } else {
     * LVLI subList = (LVLI) patch.makeCopy(glist, lvliName);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * addArmorByBit(subList, p.getVar(), String.valueOf(c), merger);
     * patch.addRecord(subList);
     * list.addEntry(subList.getForm(), 1, 1);
     * patch.addRecord(list);
     * }
     * } else {
     * boolean found = false;
     * for (LeveledEntry entry : list) {
     * if (entry.getForm().equals(p.getVar().get(0).getForm())) {
     * found = true;
     * }
     * }
     * if (!found) {
     * list.addEntry(p.getVar().get(0).getForm(), 1, 1);
     * }
     * }
     * }
     * }
     * }
     * }*/

    /*static void addAlternateOutfits(LVLI list, ArrayList<ARMO> a, Mod merger, Mod patch) {
     * FormID f = new FormID("107347", "Skyrim.esm");
     * //SPGlobal.log("outfits glist", f.toString());
     * LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
     * 
     * KYWD k = null;
     * ArrayList<Pair<KYWD, ArrayList<ARMO>>> varSets = new ArrayList<>(0);
     * for (ARMO arm : a) {
     * k = hasKeyStartsWith(arm, "LLI_BASE", merger);
     * boolean notBase = false;
     * if (k == null) {
     * k = hasKeyStartsWith(arm, "dienes_outfit", merger);
     * notBase = true;
     * }
     * 
     * for (Pair<KYWD, ArrayList<ARMO>> p1 : matchingOutfits) {
     * 
     * boolean key = false;
     * if (notBase) {
     * key = p1.getBase().equals(k);
     * } else {
     * KYWD ret = null;
     * for (Pair p : armorMatches) {
     * KYWD var = (KYWD) p.getBase();
     * //SPGlobal.log("getBaseArmor", k.getEDID() + " " + var.getEDID() + " " + var.equals(k));
     * if (var.equals(k)) {
     * ret = (KYWD) p.getVar();
     * }
     * }
     * key = armorHasKeyword(p1.getVar().get(0), ret, merger) || armorHasKeyword(p1.getVar().get(0), k, merger);
     * }
     * 
     * if (key) {
     * for (ARMO armor : p1.getVar()) {
     * boolean passed = true;
     * for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
     * boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
     * boolean formFlag = arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
     * 
     * boolean flagMatch = (armorFlag == formFlag);
     * 
     * if (flagMatch == false) {
     * passed = false;
     * }
     * }
     * if (!passed) {
     * KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
     * if (armorHasKeyword(arm, helm, merger) && armorHasKeyword(armor, helm, merger)) {
     * passed = true;
     * }
     * }
     * if (passed) {
     * boolean found = false;
     * KYWD slotKey = getSlotKYWD(armor, merger);
     * if (slotKey == null) {
     * int test = 1;
     * } else {
     * for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {
     * if (p.getBase().equals(slotKey)) {
     * ArrayList<ARMO> q = p.getVar();
     * if (!q.contains(armor)) {
     * q.add(armor);
     * }
     * found = true;
     * break;
     * }
     * }
     * if (found == false) {
     * Pair<KYWD, ArrayList<ARMO>> p = new Pair(slotKey, new ArrayList<ARMO>(0));
     * p.getVar().add(armor);
     * varSets.add(p);
     * }
     * }
     * }
     * }
     * }
     * }
     * }
     * 
     * String bits = getBitsFromArray(a, merger);
     * for (char c : bits.toCharArray()) {
     * for (Pair<KYWD, ArrayList<ARMO>> p : varSets) {
     * 
     * if (arrayHasBits(p.getVar(), String.valueOf(c), merger)) {
     * if (p.getVar().size() > 1) {
     * String lvliName = getNameFromArrayWithKey(p.getVar(), k, merger) + "variants";
     * LVLI list2 = (LVLI) patch.getMajor(lvliName, GRUP_TYPE.LVLI);
     * if (list2 != null) {
     * list.addEntry(list2.getForm(), 1, 1);
     * patch.addRecord(list);
     * } else {
     * LVLI subList = (LVLI) patch.makeCopy(glist, lvliName);
     * addArmorByBit(subList, p.getVar(), String.valueOf(c), merger);
     * patch.addRecord(subList);
     * list.addEntry(subList.getForm(), 1, 1);
     * patch.addRecord(list);
     * }
     * } else {
     * boolean found = false;
     * for (LeveledEntry entry : list) {
     * if (entry.getForm().equals(p.getVar().get(0).getForm())) {
     * found = true;
     * }
     * }
     * if (!found) {
     * list.addEntry(p.getVar().get(0).getForm(), 1, 1);
     * }
     * }
     * }
     * }
     * }
     * }*/

    /*static void insertTieredArmors(LVLI list, String keyPrefix, String bits, Mod merger, Mod patch) {
     * FormID f = new FormID("107347", "Skyrim.esm");
     * LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
     * boolean changed = false;
     * 
     * if (keyPrefix.contains("Boss") || keyPrefix.contains("Thalmor")) {
     * list.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
     * }
     * 
     * for (int lev = 1; lev < 100; lev++) {
     * int tier = lev / 3;
     * String tierName = keyPrefix + String.valueOf(tier);
     * KYWD key = (KYWD) merger.getMajor(tierName, GRUP_TYPE.KYWD);
     * if (key != null) {
     * ArrayList<ArrayList<ARMO>> array = getArrayOfTieredArmorSetsByKeyword(key, merger);
     * String edid = "DienesLVLI_" + keyPrefix + String.valueOf(tier);
     * for (ArrayList<ARMO> ar : array) {
     * if (arrayHasBits(ar, bits, merger)) {
     * 
     * LVLI subList = (LVLI) patch.getMajor(edid, GRUP_TYPE.LVLI);
     * if (subList == null) {
     * //SPGlobal.logError("LLI Error:", "Could not find LVLI " + edid);
     * subList = (LVLI) patch.makeCopy(glist, edid);
     * subList.set(LeveledRecord.LVLFlag.UseAll, false);
     * patch.addRecord(subList);
     * }
     * boolean change = addListIfNotLevel(list, subList, lev);
     * if (change) {
     * changed = true;
     * }
     * String setListName = "DienesLVLI_" + hasKeyStartsWith(ar.get(0), "dienes_outfit", merger).getEDID().substring(14) + bits;
     * LVLI setList = (LVLI) merger.getMajor(setListName, GRUP_TYPE.LVLI);
     * LVLI setList2 = (LVLI) patch.getMajor(setListName, GRUP_TYPE.LVLI);
     * if (setList == null) {
     * setList = setList2;
     * }
     * if (setList != null) {
     * change = addListIfNotLevel(subList, setList, 1);
     * if (change) {
     * changed = true;
     * }
     * } else {
     * LVLI set = (LVLI) patch.makeCopy(glist, setListName);
     * set.set(LeveledRecord.LVLFlag.UseAll, true);
     * set.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
     * set.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
     * ArrayList<ArrayList<ARMO>> abits = new ArrayList<>(0);
     * for (char c : bits.toCharArray()) {
     * abits.add(addArmorByBitToArray(ar, String.valueOf(c), merger));
     * }
     * for (ArrayList<ARMO> a : abits) {
     * addAlternateSets(set, a, merger, patch);
     * }
     * patch.addRecord(set);
     * subList.addEntry(set.getForm(), 1, 1);
     * patch.addRecord(subList);
     * changed = true;
     * }
     * 
     * }
     * }
     * if ((array.isEmpty()) && (edid.contentEquals("DienesLVLI_Thalmor_Tier_9"))) {
     * LVLI subList = (LVLI) patch.makeCopy(glist, edid);
     * subList.set(LeveledRecord.LVLFlag.UseAll, true);
     * subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
     * subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
     * FormID boots = new FormID("01391a", "Skyrim.esm");
     * FormID helm = new FormID("01391d", "Skyrim.esm");
     * FormID cuirass = new FormID("01392a", "Skyrim.esm");
     * FormID gloves = new FormID("01391c", "Skyrim.esm");
     * subList.addEntry(boots, 1, 1);
     * subList.addEntry(helm, 1, 1);
     * subList.addEntry(cuirass, 1, 1);
     * subList.addEntry(gloves, 1, 1);
     * 
     * addListIfNotLevel(list, subList, lev);
     * patch.addRecord(subList);
     * changed = true;
     * }
     * if ((array.isEmpty()) && (edid.contentEquals("DienesLVLI_Necromancer_Tier_0"))) {
     * LVLI subList = (LVLI) patch.makeCopy(glist, edid);
     * subList.set(LeveledRecord.LVLFlag.UseAll, true);
     * subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
     * subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
     * FormID boots = new FormID("0c36e8", "Skyrim.esm");
     * FormID robesList = new FormID("105251", "Skyrim.esm");
     * subList.addEntry(boots, 1, 1);
     * subList.addEntry(robesList, 1, 1);
     * 
     * addListIfNotLevel(list, subList, lev);
     * patch.addRecord(subList);
     * changed = true;
     * }
     * if ((array.isEmpty()) && (edid.contentEquals("DienesLVLI_Warlock_Tier_0"))) {
     * LVLI subList = (LVLI) patch.makeCopy(glist, edid);
     * subList.set(LeveledRecord.LVLFlag.UseAll, true);
     * subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
     * subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
     * FormID boots = new FormID("0c5d12", "Skyrim.esm");
     * FormID robesList = new FormID("105ef9", "Skyrim.esm");
     * subList.addEntry(boots, 1, 1);
     * subList.addEntry(robesList, 1, 1);
     * 
     * addListIfNotLevel(list, subList, lev);
     * patch.addRecord(subList);
     * changed = true;
     * }
     * }
     * }
     * if (changed) {
     * patch.addRecord(list);
     * }
     * }*/

    /*static ArrayList getArrayOfTieredArmorSetsByKeyword(KYWD key, Mod merger) {
     * ArrayList<ArrayList<ARMO>> ret = new ArrayList<>(0);
     * for (Pair<KYWD, ArrayList<ARMO>> p : matchingOutfits) {
     * if (armorHasKeyword(p.getVar().get(0), key, merger)) {
     * ret.add(p.getVar());
     * }
     * }
     * 
     * return ret;
     * }*/

    /*static boolean arrayHasBits(ArrayList<ARMO> ar, String bits, Mod merger) {
     * boolean ret = true;
     * if (bits.contains("H")) {
     * boolean passed = false;
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
     * || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
     * || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
     * passed = true;
     * }
     * }
     * if (passed == false) {
     * ret = false;
     * }
     * }
     * if (bits.contains("C")) {
     * boolean passed = false;
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
     * passed = true;
     * }
     * }
     * if (passed == false) {
     * ret = false;
     * }
     * }
     * if (bits.contains("G")) {
     * boolean passed = false;
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
     * passed = true;
     * }
     * }
     * if (passed == false) {
     * ret = false;
     * }
     * }
     * if (bits.contains("B")) {
     * boolean passed = false;
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
     * passed = true;
     * }
     * }
     * if (passed == false) {
     * ret = false;
     * }
     * }
     * if (bits.contains("S")) {
     * boolean passed = false;
     * for (ARMO a : ar) {
     * KYWD k = hasKeyStartsWith(a, "ArmorShield", merger);
     * if (k != null) {
     * passed = true;
     * }
     * }
     * if (passed == false) {
     * ret = false;
     * }
     * }
     * 
     * return ret;
     * }*/

    /*static String getBitsFromArray(ArrayList<ARMO> a, Mod merger) {
     * String ret = "";
     * KYWD helm = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
     * boolean h = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
     * || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
     * || arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
     * h = true;
     * break;
     * }
     * }
     * KYWD cuirass = (KYWD) merger.getMajor("ArmorCuirass", GRUP_TYPE.KYWD);
     * boolean c = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
     * c = true;
     * break;
     * }
     * }
     * KYWD gauntlets = (KYWD) merger.getMajor("ArmorGauntlets", GRUP_TYPE.KYWD);
     * boolean g = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
     * g = true;
     * break;
     * }
     * }
     * KYWD boots = (KYWD) merger.getMajor("ArmorBoots", GRUP_TYPE.KYWD);
     * boolean b = false;
     * for (ARMO arm : a) {
     * if (arm.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
     * b = true;
     * break;
     * }
     * }
     * KYWD shield = (KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD);
     * boolean s = false;
     * for (ARMO arm : a) {
     * if (armorHasKeyword(arm, shield, merger)) {
     * s = true;
     * break;
     * }
     * }
     * if (h) {
     * ret = ret + "H";
     * }
     * if (c) {
     * ret = ret + "C";
     * }
     * if (g) {
     * ret = ret + "G";
     * }
     * if (b) {
     * ret = ret + "B";
     * }
     * if (s) {
     * ret = ret + "S";
     * }
     * 
     * return ret;
     * }*/

    /*static boolean addListIfNotLevel(LVLI list, LVLI subList, int level) {
     * boolean added = false;
     * boolean found = false;
     * ArrayList<LeveledEntry> ar = list.getEntries();
     * for (LeveledEntry l : ar) {
     * if (l.getLevel() == level) {
     * if (l.getForm().equals(subList.getForm())) {
     * found = true;
     * }
     * }
     * }
     * if (!found) {
     * added = true;
     * list.addEntry(subList.getForm(), level, 1);
     * }
     * return added;
     * }*/

    /*static void addArmorByBit(LVLI set, ArrayList<ARMO> ar, String bits, Mod merger) {
     * if (bits.contains("H")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
     * || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
     * || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
     * set.addEntry(a.getForm(), 1, 1);
     * }
     * }
     * }
     * if (bits.contains("C")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
     * set.addEntry(a.getForm(), 1, 1);
     * }
     * }
     * }
     * if (bits.contains("G")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
     * set.addEntry(a.getForm(), 1, 1);
     * }
     * }
     * }
     * if (bits.contains("B")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
     * set.addEntry(a.getForm(), 1, 1);
     * }
     * }
     * }
     * if (bits.contains("S")) {
     * for (ARMO a : ar) {
     * KYWD k = hasKeyStartsWith(a, "ArmorShield", merger);
     * if (k != null) {
     * set.addEntry(a.getForm(), 1, 1);
     * }
     * }
     * }
     * 
     * }*/

    /*static ArrayList<ARMO> addArmorByBitToArray(ArrayList<ARMO> ar, String bits, Mod merger) {
     * ArrayList<ARMO> ret = new ArrayList<>(0);
     * 
     * if (bits.contains("H")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
     * || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
     * || a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
     * ret.add(a);
     * }
     * }
     * }
     * if (bits.contains("C")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
     * ret.add(a);
     * }
     * }
     * }
     * if (bits.contains("G")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
     * ret.add(a);
     * }
     * }
     * }
     * if (bits.contains("B")) {
     * for (ARMO a : ar) {
     * if (a.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
     * ret.add(a);
     * }
     * }
     * }
     * if (bits.contains("S")) {
     * for (ARMO a : ar) {
     * KYWD k = hasKeyStartsWith(a, "ArmorShield", merger);
     * if (k != null) {
     * ret.add(a);
     * }
     * }
     * }
     * 
     * return ret;
     * }*/

    /*static boolean isTiered(String name) {
     * boolean ret = false;
     * ArrayList<String> names = new ArrayList<>();
     * names.add("BanditArmorMeleeHeavyOutfit");
     * names.add("BanditArmorMeleeHeavyNoShieldOutfit");
     * names.add("BanditArmorHeavyBossOutfit");
     * names.add("BanditArmorHeavyBossNoShieldOutfit");
     * names.add("BanditArmorMeleeShield20Outfit");
     * names.add("BanditArmorMeleeNoShieldOutfit");
     * names.add("ThalmorArmorWithHelmetOutfit");
     * names.add("WarlockOutfitLeveled");
     * names.add("NecromancerOutfit");
     * names.add("NecromancerOutfitHood50");
     * 
     * for (String s : names) {
     * if (name.contentEquals(s)) {
     * ret = true;
     * }
     * }
     * 
     * return ret;
     * }*/

    /*static String getTierKey(String name) {
     * String ret = null;
     * 
     * if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
     * ret = "BanditHeavy_Tier_";
     * }
     * if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
     * ret = "BanditLight_Tier_";
     * }
     * if (name.startsWith("BanditArmorMeleeHeavyNoShieldOutfit")) {
     * ret = "BanditHeavy_Tier_";
     * }
     * if (name.startsWith("BanditArmorHeavyBossOutfit")) {
     * ret = "BanditBoss_Tier_";
     * }
     * if (name.startsWith("BanditArmorHeavyBossNoShieldOutfit")) {
     * ret = "BanditBoss_Tier_";
     * }
     * if (name.startsWith("BanditArmorMeleeNoShieldOutfit")) {
     * ret = "BanditLight_Tier_";
     * }
     * if (name.startsWith("ThalmorArmorWithHelmetOutfit")) {
     * ret = "Thalmor_Tier_";
     * }
     * if (name.startsWith("WarlockOutfitLeveled")) {
     * ret = "Warlock_Tier_";
     * }
     * if (name.contentEquals("NecromancerOutfit")) {
     * ret = "Necromancer_Tier_";
     * }
     * if (name.contentEquals("NecromancerOutfitHood50")) {
     * ret = "Necromancer_Tier_";
     * }
     * 
     * return ret;
     * }*/

    /*static String getBits(String name) {
     * String ret = null;
     * if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
     * ret = "HCGB";
     * }
     * if (name.startsWith("BanditArmorMeleeHeavyNoShieldOutfit")) {
     * ret = "HCGB";
     * }
     * if (name.startsWith("BanditArmorHeavyBossOutfit")) {
     * ret = "HCGB";
     * }
     * if (name.startsWith("BanditArmorHeavyBossNoShieldOutfit")) {
     * ret = "HCGB";
     * }
     * if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
     * ret = "HCBG";
     * }
     * if (name.startsWith("BanditArmorMeleeNoShieldOutfit")) {
     * ret = "CBG";
     * }
     * if (name.startsWith("ThalmorArmorWithHelmetOutfit")) {
     * ret = "HCBG";
     * }
     * if (name.startsWith("WarlockOutfitLeveled")) {
     * ret = "HCBG";
     * }
     * if (name.contentEquals("NecromancerOutfit")) {
     * ret = "HCGB";
     * }
     * if (name.contentEquals("NecromancerOutfitHood50")) {
     * ret = "HCGB";
     * }
     * 
     * 
     * return ret;
     * }*/

    /*static boolean needsShield(String name) {
     * boolean ret = false;
     * if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
     * ret = true;
     * }
     * if (name.startsWith("BanditArmorHeavyBossOutfit")) {
     * ret = true;
     * }
     * if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
     * ret = true;
     * }
     * 
     * return ret;
     * }*/

    /*static FormID shieldForm(String name) {
     * FormID ret = FormID.NULL;
     * if (name.startsWith("BanditArmorMeleeHeavyOutfit")) {
     * ret = new FormID("039d2d", "Skyrim.esm");
     * }
     * if (name.startsWith("BanditArmorHeavyBossOutfit")) {
     * ret = new FormID("03df22", "Skyrim.esm");
     * }
     * if (name.startsWith("BanditArmorMeleeShield20Outfit")) {
     * ret = new FormID("0c0196", "Skyrim.esm");
     * }
     * 
     * return ret;
     * }*/

    /*static KYWD getSlotKYWD(ARMO armor, Mod merger) {
     * KYWD ret = null;
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.CIRCLET)
     * || armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HEAD)
     * || armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HAIR)) {
     * ret = (KYWD) merger.getMajor("ArmorHelmet", GRUP_TYPE.KYWD);
     * }
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.BODY)) {
     * ret = (KYWD) merger.getMajor("ArmorCuirass", GRUP_TYPE.KYWD);
     * }
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.HANDS)) {
     * ret = (KYWD) merger.getMajor("ArmorGauntlets", GRUP_TYPE.KYWD);
     * }
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.FEET)) {
     * ret = (KYWD) merger.getMajor("ArmorBoots", GRUP_TYPE.KYWD);
     * }
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.SHIELD)) {
     * ret = (KYWD) merger.getMajor("ArmorShield", GRUP_TYPE.KYWD);
     * }
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.RING)) {
     * ret = (KYWD) merger.getMajor("ClothingRing", GRUP_TYPE.KYWD);
     * }
     * if (armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, skyproc.genenums.FirstPersonFlags.AMULET)) {
     * ret = (KYWD) merger.getMajor("ClothingNecklace", GRUP_TYPE.KYWD);
     * }
     * 
     * return ret;
     * }*/
    /**
     *
     * @param list
     * @param lotft
     * @param flags Inserts all sets for lotft with flags into list Does not add
     * list to patch
     */
    private static void insertTieredArmors(LVLI list, OTFT lotft, LFlags flags) {
        RecordDataOTFT otftRec = LLI.parsedOTFT.get(lotft.getEDID());
        Set<TieredSet> sets = otftRec.getTieredSets();
        for (TieredSet set : sets) {
            String s = lli_prefix + set.getName() + flags.toString();
            LVLI subList = (LVLI) patch.getMajor(s, GRUP_TYPE.LVLI);
            if (subList == null) {
                subList = new LVLI(s);
                subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, false);
                subList.set(LeveledRecord.LVLFlag.UseAll, true);
                try {
                    subList.setChanceNone(0);
                } catch (BadParameter ex) {
                }
                makeTiered(subList, set.getName(), flags);
            }
            list.addEntry(subList.getForm(), 1, 1);
        }
    }

    private static boolean compareArmorSlots(ARMO theBase, ARMO theVar) {
        // if either is hat
        // do hat stuff

        //else do normal
        for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
            boolean armorFlag = theBase.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
            boolean formFlag = theVar.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);

            boolean flagMatch = (armorFlag == formFlag);

            if (flagMatch == false) {
                return false;
            }
        }
        return true;
    }

    private static LFlags getFlags(ArrayList<FormID> inventoryList) {
        LFlags ret = new LFlags(4);
        for (FormID f : inventoryList) {
            ARMO armor = (ARMO) merger.getMajor(f, GRUP_TYPE.ARMO);
            if (armor != null) {
                FormID template = armor.getTemplate();
                boolean templated = FormID.NULL.equals(template);
                if (templated) {
                    armor = (ARMO) merger.getMajor(template, GRUP_TYPE.ARMO);
                }
                for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
                    boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
                    if (armorFlag) {
                        ret.set(c.ordinal(), true);
                    }
                }
            }
        }

        return ret;
    }

    private static String getBiggestSetName(ArrayList<FormID> list) {

        HashMap<String, Integer> names = new HashMap<>();
        for (FormID f : list) {
            ARMO a = (ARMO) merger.getMajor(f, GRUP_TYPE.ARMO);
            FormID temp = a.getTemplate();
            if (!FormID.NULL.equals(temp)) {
                a = (ARMO) merger.getMajor(temp, GRUP_TYPE.ARMO);
            }
            RecordDataARMO rec = LLI.parsedARMO.get(a.getEDID());
            if (rec != null) {
                Set<String> sets = rec.getOutfits();
                for (String s : sets) {
                    Integer count = names.get(s);
                    if (count == null) {
                        count = Integer.valueOf(0);
                        names.put(s, count);
                    }
                    count++;
                }
            }
        }
        String ret = "";
        Integer high = 0;
        for (Entry<String, Integer> s : names.entrySet()) {
            Integer c = s.getValue();
            if (c > high) {
                ret = s.getKey();
                high = c;
            }
        }

        return ret;
    }

    private static ArrayList<FormID> getSet(ArrayList<FormID> list, String biggestSetName) {
        ArrayList<FormID> ret = new ArrayList<>();
        for (FormID f : list) {
            ARMO a = (ARMO) merger.getMajor(f, GRUP_TYPE.ARMO);
            FormID temp = a.getTemplate();
            if (!FormID.NULL.equals(temp)) {
                a = (ARMO) merger.getMajor(temp, GRUP_TYPE.ARMO);
            }
            RecordDataARMO rec = LLI.parsedARMO.get(a.getEDID());
            if (rec != null) {
                Set<String> sets = rec.getOutfits();
                for (String s : sets) {
                    if (biggestSetName.equalsIgnoreCase(s)) {
                        ret.add(f);
                    }
                }
            }
        }

        return ret;
    }

    private static void makeTiered(LVLI list, String name, LFlags flags) {
        ArrayList<ARMO> armors = matchingSets.get(name);
        HashMap<Integer, ArrayList<ARMO>> tiers = new HashMap();

        // put all armors into tiers
        for (int lev = 1; lev < 34; lev++) {
            for (ARMO a : armors) {
                RecordDataARMO aRec = LLI.parsedARMO.get(a.getEDID());
                Set<TieredSet> sets = aRec.getTieredSets();
                for (RecordData.TieredSet s : sets) {
                    if (s.getTier() == lev) {
                        ArrayList<ARMO> arr = tiers.get(lev);
                        if (arr == null) {
                            arr = new ArrayList<>();
                            tiers.put(lev, arr);
                        }
                        arr.add(a);
                    }
                }
            }
        }
        //seperate tiers into sets
        HashMap<Integer, ArrayList<Entry<String, ArrayList<ARMO>>>> tieredOutfits = new HashMap();
        for (int i : tiers.keySet()) {
            ArrayList<ARMO> tierArmors = tiers.get(i);
            ArrayList<Entry<String, ArrayList<ARMO>>> thisTier = new ArrayList<>();
            tieredOutfits.put(i, thisTier);

            HashMap<String, ArrayList<ARMO>> outfits = new HashMap<>();
            for (ARMO a : tierArmors) {
                RecordDataARMO rec = LLI.parsedARMO.get(a.getEDID());
                for (String s : rec.getOutfits()) {
                    ArrayList<ARMO> out = outfits.get(s);
                    if (out == null) {
                        out = new ArrayList<>();
                        outfits.put(s, out);
                    }
                    out.add(a);
                }
            }
            for (Entry<String, ArrayList<ARMO>> a : outfits.entrySet()) {
                thisTier.add(a);
            }
        }

        // put matching sets into sublists
        for (int lev = 1; lev < 100; lev++) {
            int tier = lev / 3;
            String tierName = name + "_" + String.valueOf(tier);
            LVLI subList = (LVLI) patch.getMajor(tierName, GRUP_TYPE.LVLI);
            if (subList == null) {

                //
                ArrayList<FormID> subLists = new ArrayList<>();
                for (Entry<String, ArrayList<ARMO>> a : tieredOutfits.get(tier)) {
                    subLists.add(sublistFromArrayIfBipedsMatchFlags(a, flags, tierName));
                }
                if (!subLists.isEmpty()) {
                    subList = new LVLI(tierName);
                    subList.set(LeveledRecord.LVLFlag.CalcAllLevelsEqualOrBelowPC, false);
                    subList.set(LeveledRecord.LVLFlag.CalcForEachItemInCount, true);
                    subList.set(LeveledRecord.LVLFlag.UseAll, false);
                    try {
                        subList.setChanceNone(0);
                    } catch (BadParameter ex) {
                    }
                    for (FormID sub : subLists) {
                        subList.addEntry(sub, lev, 1);
                    }
                }
            }
            if (subList != null) {
                list.addEntry(subList.getForm(), lev, 1);
            }
        }
    }

    private static FormID sublistFromArrayIfBipedsMatchFlags(Entry<String, ArrayList<ARMO>> armorEntrys, LFlags flags, String tierName) {
        int flagsInt = Integer.parseInt(flags.toString(), 2);
        int notFlags = ~flagsInt;
        // hashmap of flags, armors
        HashMap<Integer, ArrayList<ARMO>> flagsMap = new HashMap<>();
        // fill map
        for (ARMO a : armorEntrys.getValue()) {
            LFlags f = new LFlags(4);
            ARMO armor = a;
            FormID template = a.getTemplate();
            boolean templated = FormID.NULL.equals(template);
            if (templated) {
                armor = (ARMO) merger.getMajor(template, GRUP_TYPE.ARMO);
            }
            for (skyproc.genenums.FirstPersonFlags c : skyproc.genenums.FirstPersonFlags.values()) {
                boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
                if (armorFlag) {
                    f.set(c.ordinal(), true);
                }
            }
            Integer intFlag = Integer.parseInt(f.toString(), 2);
            if ((intFlag & notFlags) == 0) {
                ArrayList<ARMO> ar = flagsMap.get(intFlag);
                if (ar == null) {
                    ar = new ArrayList<>();
                    flagsMap.put(intFlag, ar);
                }
                ar.add(a);
            }
        }

        // permutation of keys of hashmap
        Set<Set<Integer>> flagSets;
        Set<Integer> temp = new HashSet<>(flagsMap.keySet());

        // guava powerset 
        flagSets = Sets.powerSet(temp);

        // if a permutation == flags
        Set<Set<Integer>> passedSets = new HashSet<>();
        for (Set<Integer> theSet : flagSets) {
            int setInt = 0;
            boolean conflict = false;
            for (Integer theFlag : theSet) {

                if ((setInt & theFlag) == 0) {
                    setInt += theFlag;
                } else {
                    conflict = true;
                    break;
                }
            }
            if ((!conflict) && ((setInt ^ flagsInt) == 0)) {
                passedSets.add(theSet);
            }
        }

        FormID retID = null;
        // make sublists of hashmap values (if needed)
        // add subLists / armors to list
        if (!passedSets.isEmpty()) {
            String retEDID = armorEntrys.getKey() + flagsInt;
            //test if ret exists
            LVLI ret = new LVLI(retEDID);
            retID = ret.getForm();
            // set ret flags
            for (Set<Integer> s : passedSets) {
                for (Integer i : s) {
                    ArrayList<ARMO> flagMatchArmors = flagsMap.get(i);
                    if (flagMatchArmors != null) {
                        if (flagMatchArmors.size() == 1) {
                            ret.addEntry(flagMatchArmors.get(0).getForm(), 1, 1);
                        } else {
                            String subEDID = armorEntrys.getKey() + i.toString(); // overlap?
                            // test if matchSublist exists 
                            LVLI matchSublist = new LVLI(subEDID);
                            // set lvli flags
                            for (ARMO a : flagMatchArmors) {
                                matchSublist.addEntry(a.getForm(), 1, 1);
                            }
                            ret.addEntry(matchSublist.getForm(), 1, 1);
                        }
                    }
                }
            }
        }

        return retID;
    }
}
