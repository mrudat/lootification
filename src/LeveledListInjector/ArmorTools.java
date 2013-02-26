/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.ArrayList;
import java.util.Arrays;
import skyproc.*;

/**
 *
 * @author David Tynan
 */
public class ArmorTools {

    public static ArrayList<Pair<KYWD, KYWD>> armorMatches;
    private static ArrayList<ArrayList<FormID>> armorVariants = new ArrayList<>(0);
    private static ArrayList<FormID> matchingSetVariants = new ArrayList<>(0);
    //private static Mod merger;
    //private static Mod patch;

    public static class Pair<L, R> {

        private L l;
        private R r;

        public Pair(L l, R r) {
            this.l = l;
            this.r = r;
        }

        public L getBase() {
            return l;
        }

        public R getVar() {
            return r;
        }

        public void setBase(L l) {
            this.l = l;
        }

        public void setVar(R r) {
            this.r = r;
        }
    }

//    public static void setMergeAndPatch(Mod m, Mod p) {
//        merger = m;
//        patch = p;
//    }
    static void buildOutfitsArmors(FLST baseArmorKeysFLST, Mod merger, Mod patch) {
        FormID f = new FormID("107347", "Skyrim.esm");
        //SPGlobal.log("outfits glist", f.toString());
        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
        //SPGlobal.log("outfits glist", glist + "");
        glist.set(LeveledRecord.LVLFlag.UseAll, false);
        for (OTFT lotft : merger.getOutfits()) {
            ArrayList<FormID> a = lotft.getInventoryList();
            boolean changed = false;
            /*            ArrayList<FormID> set = containsArmorSet(a, merger);
             while (set.size() > 0) {
             //check if outfitLVLI exists
             String eid = "DienesLVLI" + getSetName(set) + "level1";
             LVLI r = (LVLI) merger.getMajor(eid, GRUP_TYPE.LVLI);
             if (r == null) {
             //outfitLVLI not exist create it and add matching
             LVLI setList = (LVLI) patch.makeCopy(glist, eid);
             for (int index = 0; index < set.size(); index++) {
             FormID item = set.get(index);
             ARMO temp = (ARMO) merger.getMajor(item, GRUP_TYPE.ARMO);
             if (temp != null) {
             setList.addEntry(new LeveledEntry(item, 1, 1));
             lotft.removeInventoryItem(item);
             }
             set.remove(item);
             index = index - 1;
             }
             merger.addRecord(setList);
             lotft.addInventoryItem(setList.getForm());
             matchingSetVariants.add(setList.getForm());
             changed = true;
             } else {
             for (int index = 0; index < set.size(); index++) {
             FormID item = set.get(index);
             //ARMO temp = (ARMO) merger.getMajor(item, GRUP_TYPE.ARMO);
             //if(temp != null) {
             lotft.removeInventoryItem(item);
             set.remove(item);
             index = index - 1;
             //}
             }
             lotft.addInventoryItem(r.getForm());
             changed = true;
             }
             //get next set
             set = containsArmorSet(a, merger); 
             } */
            //matching set armor moved to sublist, link any remaining weapons or armor
            //first refresh whats in the outfit
            a = lotft.getInventoryList();
            for (FormID form : a) {
                ARMO obj = (ARMO) merger.getMajor(form, GRUP_TYPE.ARMO);
                if (obj != null) {
                    KYWD baseKey = armorHasAnyKeyword(obj, baseArmorKeysFLST, merger);

                    if ((baseKey != null) && (hasVariant(obj))) {
                        String eid = "DienesLVLI" + obj.getEDID();
                        MajorRecord r = merger.getMajor(eid, GRUP_TYPE.LVLI);
                        if (r == null) {
                            LVLI subList = (LVLI) patch.makeCopy(glist, eid);
                            InsertArmorVariants(subList, form);
                            patch.addRecord(subList);
                            lotft.removeInventoryItem(form);
                            lotft.addInventoryItem(subList.getForm());
                            changed = true;
                        } else {
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

    public static ArrayList<FormID> containsArmorSet(ArrayList<FormID> inventory, Mod merger) {
        ArrayList<FormID> set = new ArrayList<>(0);
        ArrayList<String> suffixes = new ArrayList<>(Arrays.asList("Boots", "Cuirass", "Gauntlets", "Helmet", "Shield"));
        boolean matchFound = false;
        for (int count = 0; count < inventory.size() && matchFound == false; count++) {
            ARMO obj = (ARMO) merger.getMajor(inventory.get(count), GRUP_TYPE.ARMO);
            if (obj != null) {
                String armorType;
                String name = obj.getEDID();
                if (name.startsWith("Ench")) {
                    name = name.substring(4);
                }
                int i;
                for (String s : suffixes) {
                    i = name.indexOf(s);
                    if (i > 0) {
                        name = name.substring(0, i);
                    }
                }
                armorType = name;
                for (int rest = count; rest < inventory.size(); rest++) {
                    ARMO other = (ARMO) merger.getMajor(inventory.get(count), GRUP_TYPE.ARMO);
                    if (other != null) {
                        String compare = other.getEDID();
                        if (compare.contains(armorType)) {
                            set.add(other.getForm());
                            matchFound = true;
                        }
                    }
                }
                if (matchFound) {
                    set.add(obj.getForm());
                }
            }
        }
        return set;
    }

    private static ArrayList<FormID> lvliContainsArmorSet(LVLI llist, Mod merger) {
        ArrayList<FormID> contentForms = new ArrayList<>(0);
        ArrayList<LeveledEntry> levContents = llist.getEntries();

        for (LeveledEntry levEntry : levContents) {
            contentForms.add(levEntry.getForm());
        }
        ArrayList<FormID> ret = containsArmorSet(contentForms, merger);

        return ret;
    }

    static void linkLVLIArmors(FLST baseArmorKeysFLST, Mod merger, Mod patch) {
        FormID f = new FormID("107347", "Skyrim.esm");
        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
        glist.set(LeveledRecord.LVLFlag.UseAll, false);

        for (LVLI llist : merger.getLeveledItems()) {
            SPGlobal.log("Link Armor List", llist.getEDID());

//            //check if LVLI is one we made
            boolean found = false;
//            for (FormID set : matchingSetVariants) {
//                if (llist.getForm().equals(set)) {
//                    found = true;
//                }
//            }


            if (found == false) {

                boolean changed = false;
//                if (llist.get(LeveledRecord.LVLFlag.UseAll)) {
//                    //remove any matching outfits
//                    //ArrayList<FormID> set = lvliContainsArmorSet(llist, merger);
//                    while (set.size() > 0) {
//                        linkArmorSet(llist, set, merger, patch);
//                        set = lvliContainsArmorSet(llist, merger);
//                        changed = true;
//                    }
//                }
                //SPGlobal.log(llist.getEDID(), "num entries" + llist.numEntries());
                for (int i = 0; i < llist.numEntries(); i++) {
                    LeveledEntry entry = llist.getEntry(i);
                    FormID test = entry.getForm();
                    //SPGlobal.log("list entry " + i, entry.getForm() + "");
                    ARMO obj = (ARMO) merger.getMajor(test, GRUP_TYPE.ARMO);
                    if (obj != null) {
                        //SPGlobal.log("list entry " + i, obj.getEDID());
                        KYWD base = armorHasAnyKeyword(obj, baseArmorKeysFLST, merger);

                        boolean hasVar = hasVariant(obj);
                        if ((base != null) && (hasVar)) {
                            //SPGlobal.log(obj.getEDID(), "has keyword" + base);

                            String eid = "DienesLVLI" + obj.getEDID();
                            MajorRecord r = patch.getMajor(eid, GRUP_TYPE.LVLI);
                            if (r == null) {
                                //SPGlobal.log(obj.getEDID(), "new sublist needed");
                                LVLI subList = (LVLI) patch.makeCopy(glist, eid);
                                InsertArmorVariants(subList, entry.getForm());
                                patch.addRecord(subList);
                                llist.removeEntry(i);
                                llist.addEntry(new LeveledEntry(subList.getForm(), entry.getLevel(), entry.getCount()));
                                i = -1;
                                changed = true;
                            } else {
                                //SPGlobal.log(obj.getEDID(), "sublist found " + r.getEDID());
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

    static void buildArmorVariants(Mod merger, Mod patch, FLST baseKeys, FLST varKeys) {
        SPGlobal.log("Build Variants", "Building Base Armors");
        for (ARMO armor : merger.getArmors()) {
            KYWD baseKey = armorHasAnyKeyword(armor, baseKeys, merger);
            if (baseKey != null) {
                SPGlobal.log(armor.getEDID(), "is base armor");
                ArrayList<FormID> alts = new ArrayList<>(0);
                alts.add(0, armor.getForm());
                armorVariants.add(alts);
            }
        }
        SPGlobal.log("Build Variants", "Building Variant Armors");

        for (ARMO armor : merger.getArmors()) {
            //SPGlobal.log("armor", armor.getEDID());
            String debugname = armor.getEDID();
            if(debugname.contains("WV")){
                int qwerty = 1;
            }
            KYWD variantKey = armorHasAnyKeyword(armor, varKeys, merger);
            if (variantKey != null) {
                //SPGlobal.log(armor.getEDID(), "is variant");
                for (int j = 0; j < armorVariants.size(); j++) {
                    ArrayList<FormID> a2 = armorVariants.get(j);
                    ARMO form = (ARMO) merger.getMajor((FormID) a2.get(0), GRUP_TYPE.ARMO);

                    boolean passed = true;
                    //SPGlobal.log("comparing to", form.getEDID());


                    if (armorHasKeyword(form, getBaseArmor(variantKey), merger)) {

                        //SPGlobal.log(form.getEDID(), "has base keyword");

                        ARMO replace = form;
                        FormID tmp = replace.getTemplate();
                        if (!tmp.isNull()) {
                            replace = (ARMO) merger.getMajor(tmp, GRUP_TYPE.ARMO);
                        }
                        for (BodyTemplate.FirstPersonFlags c : BodyTemplate.FirstPersonFlags.values()) {
                            boolean armorFlag = armor.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);
                            boolean formFlag = replace.getBodyTemplate().get(BodyTemplate.BodyTemplateType.Biped, c);

                            boolean flagMatch = (armorFlag == formFlag);
                            //SPGlobal.log("flag match" + c, armorFlag + " " + formFlag + " " + flagMatch);
                            if (flagMatch == false) {
                                passed = false;
                            }
                        }
                        if (passed) {
                            //SPGlobal.log("variant found", armor.getEDID() + " is variant of " + form.getEDID());
                            FormID template = form.getTemplate();
                            //SPGlobal.log("template", template.getFormStr());
                            if (template.isNull()) {
                                a2.add(armor.getForm());
                                //SPGlobal.log("variant added", a2.contains(armor.getForm()) + " " + a2.size());
                            } else {
                                //SPGlobal.log("Enchant found", armor.getEDID() + "  " + form.getEDID());
                                String name = generateArmorName(armor, form, merger);
                                String newEdid = generateArmorEDID(armor, form, merger);
                                ARMO armorDupe = (ARMO) patch.makeCopy(armor, "DienesARMO" + newEdid);
                                //SPGlobal.log("armor copied", armorDupe.getEDID());
                                armorDupe.setEnchantment(form.getEnchantment());
                                armorDupe.setName(name);
                                armorDupe.setTemplate(armor.getForm());
                                a2.add(armorDupe.getForm());
                                patch.addRecord(armorDupe);
                            }
                        }
                    }
                }
            }
        }
    }

    static String getSetName(ArrayList<FormID> set) {
        String name = String.valueOf(set.hashCode());
        return name;
    }

    static KYWD getBaseArmor(KYWD k) {
        KYWD ret = null;
        for (Pair p : armorMatches) {
            KYWD var = (KYWD) p.getVar();
            //SPGlobal.log("getBaseArmor", k.getEDID() + " " + var.getEDID() + " " + var.equals(k));
            if (var.equals(k)) {
                ret = (KYWD) p.getBase();
            }
        }
        return ret;
    }

    static void setupArmorMatches(FLST base, FLST var, Mod merger) {
        armorMatches = new ArrayList<>();
        ArrayList<FormID> bases = base.getFormIDEntries();
        ArrayList<FormID> vars = var.getFormIDEntries();
        for (int i = 0; i < bases.size(); i++) {
            KYWD newBase = (KYWD) merger.getMajor(bases.get(i), GRUP_TYPE.KYWD);
            KYWD newVar = (KYWD) merger.getMajor(vars.get(i), GRUP_TYPE.KYWD);
            //SPGlobal.log("Armor pair", newBase.getEDID() + " " + newVar.getEDID());
            Pair<KYWD, KYWD> p = new Pair(newBase, newVar);
            armorMatches.add(p);
            //SPGlobal.log("Armor pair", p.getBase().getEDID() + " " + p.getVar().getEDID());
        }
    }

    static String generateArmorEDID(ARMO newArmor, ARMO armor, Mod m) {
//        String name = newArmor.getEDID();
//        String baseName = armor.getEDID();
//        String prefix = "";
//        String suffix = "";
//        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
//        int prefixLen = baseName.indexOf(template.getEDID());
//        if (prefixLen > 0) {
//            prefix = baseName.substring(0, prefixLen);
//        }
//        int suffixLen = baseName.length() - template.getEDID().length() + prefixLen;
//        if (suffixLen > 0) {
//            suffix = baseName.substring(template.getEDID().length() + prefixLen);
//        }
//        String ret = prefix + name + suffix;
//        return ret;

        String name = newArmor.getEDID();
        String baseName = armor.getEDID();
        String templateName;
        String ret = "";
        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
        if (template != null) {
            templateName = template.getEDID();
            if (baseName.contains(templateName)) {
                ret = baseName.replace(templateName, name);
            } else {
                String gcs = longestCommonSubstring(baseName, templateName);
                ret = baseName.replace(gcs, name);
            }
        }

        return ret;
    }

    static String generateArmorName(ARMO newArmor, ARMO armor, Mod m) {
//        String name = newArmor.getName();
//        String baseName = armor.getName();
//        String prefix = "";
//        String suffix = "";
//        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
//        SPGlobal.log(armor.getName(), template.getName());
//        int prefixLen = baseName.indexOf(template.getName());
//        SPGlobal.log(name, "" + prefixLen);
//        if (prefixLen > 0) {
//            prefix = baseName.substring(0, prefixLen);
//        }
//        int suffixLen = baseName.length() - template.getName().length() + prefixLen;
//        if (suffixLen > 0) {
//            suffix = baseName.substring(template.getName().length() + prefixLen);
//        }
//        String ret = prefix + name + suffix;
//        return ret;

        String name = newArmor.getName();
        String baseName = armor.getName();
        String templateName;
        String ret = "";
        ARMO template = (ARMO) m.getMajor(armor.getTemplate(), GRUP_TYPE.ARMO);
        if (template != null) {
            templateName = template.getName();
            if (baseName.contains(templateName)) {
                ret = baseName.replace(templateName, name);
            } else {
                String gcs = longestCommonSubstring(baseName, templateName);
                ret = baseName.replace(gcs, name);
            }
        }

        return ret;
    }

    static KYWD armorHasAnyKeyword(ARMO rec, FLST f, Mod m) {
        ArrayList<FormID> a = f.getFormIDEntries();
        KYWD hasKey = null;
        for (int i = 0; i < a.size(); i++) {
            FormID temp = (FormID) a.get(i);
            KYWD armorKey = (KYWD) m.getMajor(temp, GRUP_TYPE.KYWD);
            if (armorHasKeyword(rec, armorKey, m)) {
                hasKey = armorKey;
                continue;
            }
        }
        //SPGlobal.log("HasAnyKeyword", rec.toString() + " " + hasKey);
        return hasKey;
    }

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

    static private void InsertArmorVariants(LVLI list, FormID base) {
        ArrayList<LeveledEntry> listEntries = list.getEntries();
        ArrayList<FormID> forms = new ArrayList<>(0);
        for (LeveledEntry e : listEntries) {
            FormID f = e.getForm();
            forms.add(f);
        }
        for (ArrayList a : armorVariants) {
            if (a.contains(base)) {
                for (int i = 0; i < a.size(); i++) {
                    FormID f = (FormID) a.get(i);
                    if (!forms.contains(f)) {
                        list.addEntry(new LeveledEntry(f, 1, 1));
                    }
                }
            }
        }
    }

    static private void linkArmorSet(LVLI llist, ArrayList<FormID> set, Mod merger, Mod patch) {
        String eid = "DienesLVLI" + getSetName(set) + "level1";
        LVLI r = (LVLI) merger.getMajor(eid, GRUP_TYPE.LVLI);
        FormID f = new FormID("107347", "Skyrim.esm");
        MajorRecord glist = merger.getMajor(f, GRUP_TYPE.LVLI);

        if (r == null) {
            LVLI setList = (LVLI) patch.makeCopy(glist, eid);
            for (int index = 0; index < set.size(); index++) {
                FormID item = set.get(index);
                ARMO temp = (ARMO) merger.getMajor(item, GRUP_TYPE.ARMO);
                if (temp != null) {
                    setList.addEntry(item, 1, 1);
                    for (int i = 0; i < llist.numEntries(); i++) {
                        FormID tempForm = llist.getEntry(i).getForm();
                        if (item.equals(tempForm)) {
                            llist.removeEntry(i);
                            continue;
                        }
                    }

                    index = index - 1;
                }
            }
            merger.addRecord(setList);
            llist.addEntry(setList.getForm(), 1, 1);
            matchingSetVariants.add(setList.getForm());

        } else {
            for (int index = 0; index < set.size(); index++) {
                FormID item = set.get(index);
                for (int i = 0; i < llist.numEntries(); i++) {
                    FormID tempForm = llist.getEntry(i).getForm();
                    if (item.equals(tempForm)) {
                        llist.removeEntry(i);
                        continue;
                    }
                }

                index = index - 1;
            }
            llist.addEntry(r.getForm(), 1, 1);

        }
    }

    private static boolean hasVariant(ARMO base) {
        boolean ret = false;
        for (ArrayList<FormID> vars : armorVariants) {
            //SPGlobal.log("hasVariant", base.getForm() + " " + vars.size());
//            if (vars.contains(base.getForm()) && (vars.size() > 1)) {
            if(vars.contains(base.getForm())) {
                ret = true;
            }
        }

        return ret;
    }

    public static void modLVLIArmors(Mod merger, Mod patch) {
        for (LVLI llist : merger.getLeveledItems()) {
            String lname = llist.getEDID();
            if (lname.contains("DienesLVLI")) {
                ARMO armor = (ARMO) merger.getMajor(llist.getEntry(0).getForm(), GRUP_TYPE.ARMO);
                if (armor != null) {
                    if (hasVariant(armor)) {
                        InsertArmorVariants(llist, armor.getForm());
                        patch.addRecord(llist);
                    }
                }
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
}
