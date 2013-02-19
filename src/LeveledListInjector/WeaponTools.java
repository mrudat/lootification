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
public class WeaponTools {

    private static ArrayList<ArrayList<FormID>> weaponVariants = new ArrayList<>(0);
    public static ArrayList<Pair<KYWD, KYWD>> weaponMatches;
    private static Mod merger;
    private static Mod patch;

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

    public static void setMergeAndPatch(Mod m, Mod p) {
        merger = m;
        patch = p;
    }

    static String generateWeaponEDID(WEAP newWeapon, WEAP weapon) {
        String name = newWeapon.getEDID();
        String baseName = weapon.getEDID();
        String prefix = "";
        String suffix = "";
        WEAP template = (WEAP) merger.getMajor(weapon.getTemplate(), GRUP_TYPE.WEAP);
        if (template != null) {
            int prefixLen = baseName.indexOf(template.getEDID());
            if (prefixLen > 0) {
                prefix = baseName.substring(0, prefixLen);
            }
            int suffixLen = baseName.length() - template.getEDID().length() + prefixLen;
            if (suffixLen > 0) {
                suffix = baseName.substring(template.getEDID().length() + prefixLen);
            }
        }
        String ret = prefix + name + suffix;
        return ret;
    }

    static KYWD getBaseWeapon(KYWD k) {
        KYWD ret = null;
        for (Pair<KYWD, KYWD> p : weaponMatches) {
            KYWD var = p.getVar();
            if (var.equals(k)) {
                ret = p.getBase();
            }
        }
        return ret;
    }

    static boolean weaponHasKeyword(WEAP rec, KYWD varKey, Mod merger) {
        ArrayList<FormID> a;
        boolean hasKey = false;
        WEAP replace = rec;
        FormID tmp = replace.getTemplate();
        if (!tmp.isNull()) {
            replace = (WEAP) merger.getMajor(tmp, GRUP_TYPE.WEAP);
        }
        KeywordSet k = replace.getKeywordSet();
        a = k.getKeywordRefs();
        for (FormID temp : a) {
            KYWD refKey = (KYWD) merger.getMajor(temp, GRUP_TYPE.KYWD);
            //SPGlobal.log("formid", temp.toString());
            //SPGlobal.log("KYWD compare", refKey.getEDID() + " " + varKey.getEDID() + " " + (refKey.equals(varKey)));
            if (varKey.equals(refKey)) {
                hasKey = true;
            }
        }
        return hasKey;
    }

    static KYWD weaponHasAnyKeyword(WEAP rec, FLST f, Mod merger) {
        ArrayList<FormID> a = f.getFormIDEntries();
        KYWD hasKey = null;
        //SPGlobal.log("Any keyword size", a.size() + "");
        for (FormID temp : a) {
            //SPGlobal.log("Any keyword", temp.getFormStr());
            KYWD weaponKey = (KYWD) merger.getMajor(temp, GRUP_TYPE.KYWD);
            if (weaponHasKeyword(rec, weaponKey, merger)) {
                hasKey = weaponKey;
                continue;
            }
        }
        return hasKey;
    }

    static void buildWeaponVariants(FLST baseKeys, FLST varKeys) {
        FormID axeForm = new FormID("06D932", "Skyrim.esm");
        KYWD axe = (KYWD) merger.getMajor(axeForm, GRUP_TYPE.KYWD);

        FormID hammerForm = new FormID("06D930", "Skyrim.esm");
        KYWD hammer = (KYWD) merger.getMajor(hammerForm, GRUP_TYPE.KYWD);

        //SPGlobal.log("Build Variants", "Building Base Weapons");
        for (WEAP weapon : merger.getWeapons()) {
            KYWD isBase = weaponHasAnyKeyword(weapon, baseKeys, merger);
            if (isBase != null) {
                //SPGlobal.log("Found", "is base weapon");
                ArrayList<FormID> alts = new ArrayList<>(0);
                alts.add(0, weapon.getForm());
                weaponVariants.add(alts);
            }
        }
        //SPGlobal.log("Build Variants", "Building Variant Weapons");
        ArrayList<WEAP> mWeapons = merger.getWeapons().getRecords();
        for (int weaponNum = 0; weaponNum < mWeapons.size(); weaponNum++) {
            WEAP weapon = mWeapons.get(weaponNum);
            KYWD isVariant = weaponHasAnyKeyword(weapon, varKeys, merger);
            if (isVariant != null) {
                //SPGlobal.log(weapon.getEDID(), "is variant");
                for (int j = 0; j < weaponVariants.size(); j++) {
                    ArrayList<FormID> a2 = weaponVariants.get(j);
                    WEAP form = (WEAP) merger.getMajor((FormID) a2.get(0), GRUP_TYPE.WEAP);
                    boolean passed = false;
                    //SPGlobal.log("trying", form.getEDID());

                    if (weaponHasKeyword(form, getBaseWeapon(isVariant), merger)) {

                        WEAP comp = form;
                        FormID formBase = form.getTemplate();
                        if (!formBase.isNull()) {
                            comp = (WEAP) merger.getMajor(formBase, GRUP_TYPE.WEAP);
                        }
                        if (comp.getWeaponType() == weapon.getWeaponType()) {
                            //SPGlobal.log("weapon type", weapon.getWeaponType() + " " + comp.getWeaponType());

                            //hack to split warhammers and battleaxes
                            if (weapon.getWeaponType() == WEAP.WeaponType.TwoHBluntAxe) {
                                if (weaponHasKeyword(weapon, axe, merger) && weaponHasKeyword(comp, axe, merger)) {
                                    passed = true;
                                } else if (weaponHasKeyword(weapon, hammer, merger) && (weaponHasKeyword(comp, hammer, merger))) {
                                    passed = true;
                                } else{
                                    SPGlobal.log("Error building weapon variants", weapon.getEDID()+
                                            " cannot tell if axe or hammer");
                                }
                                    
                            } else {
                                passed = true;
                            }
                        }
                        if (passed) {
                            //SPGlobal.log("variant found", weapon.getEDID() + " is variant of " + form.getEDID());
                            FormID template = form.getEnchantment();
                            //SPGlobal.log("template", template.getFormStr());
                            if (template.isNull()) {
                                a2.add(weapon.getForm());
                            } else {
                                //SPGlobal.log("Enchant found", weapon.getEDID() + "  " + form.getEDID());
                                String name = generateWeaponName(weapon, form);
                                String newEdid = generateWeaponEDID(weapon, form);
                                WEAP weaponDupe = (WEAP) patch.makeCopy(weapon, "DienesWEAP" + newEdid);
                                //SPGlobal.log("armor copied", weaponDupe.getEDID());
                                weaponDupe.setEnchantment(form.getEnchantment());
                                weaponDupe.setEnchantmentCharge(form.getEnchantmentCharge());
                                weaponDupe.setTemplate(weapon.getForm());
                                weaponDupe.setName(name);
                                a2.add(weaponDupe.getForm());
                                patch.addRecord(weaponDupe);
                            }
                        }
                    }
                }
            }
        }
    }

    static String generateWeaponName(WEAP newWeapon, WEAP weapon) {
        String name = newWeapon.getName();
        String baseName = weapon.getName();
        String prefix = "";
        String suffix = "";
        WEAP template = (WEAP) merger.getMajor(weapon.getTemplate(), GRUP_TYPE.WEAP);
       // SPGlobal.log(weapon.getName(), template.getName());
        int prefixLen = baseName.indexOf(template.getName());
        //SPGlobal.log(name, "" + prefixLen);
        if (prefixLen > 0) {
            prefix = baseName.substring(0, prefixLen);
            //SPGlobal.log(name, "prefix " + prefix);
        }
        int suffixLen = baseName.length() - template.getName().length() + prefixLen;
        //SPGlobal.log(name, "" + suffixLen);
        if (suffixLen > 0) {
            suffix = baseName.substring(template.getName().length() + prefixLen);
            //SPGlobal.log(name, "suffix " + suffix);
        }
        String ret = prefix + name + suffix;
        return ret;
    }

    static void linkLVLIWeapons(FLST baseWeaponKeysFLST) {
        FormID f = new FormID("107347", "Skyrim.esm");
        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
        glist.set(LeveledRecord.LVLFlag.UseAll, false);
        
        for (LVLI llist : merger.getLeveledItems()) {
            if (!llist.isEmpty()) {
                boolean changed = false;
                for (int i = 0; i < llist.numEntries(); i++) {
                    LeveledEntry entry = llist.getEntry(i);
                    WEAP obj = (WEAP) merger.getMajor(entry.getForm(), GRUP_TYPE.WEAP);
                    if (obj != null) {

                        KYWD isBase = weaponHasAnyKeyword(obj, baseWeaponKeysFLST, merger);
                        boolean hasVar = hasVariant(obj);
                        if (hasVar && (isBase != null)) {
                            String eid = "DienesLVLI" + obj.getEDID();
                            MajorRecord r = merger.getMajor(eid, GRUP_TYPE.LVLI);
                            if (r == null) {
                                LVLI subList = (LVLI) patch.makeCopy(glist, eid);
                                InsertWeaponVariants(subList, entry.getForm());
                                patch.addRecord(subList);
                                llist.removeEntry(i);
                                llist.addEntry(new LeveledEntry(subList.getForm(), entry.getLevel(), entry.getCount()));
                                i = -1;
                                changed = true;
                            } else {
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

    static void InsertWeaponVariants(LVLI list, FormID base) {
        for (ArrayList a : weaponVariants) {
            if (a.contains(base)) {
                for (int i = 0; i < a.size(); i++) {
                    FormID f = (FormID) a.get(i);
                    list.addEntry(new LeveledEntry(f, 1, 1));
                }
            }
        }
    }

    static void setupWeaponMatches(FLST base, FLST var, Mod m) {
        weaponMatches = new ArrayList<>(0);
        ArrayList<FormID> bases = base.getFormIDEntries();
        ArrayList<FormID> vars = var.getFormIDEntries();
        for (int i = 0; i < bases.size(); i++) {
            //SPGlobal.log("Weapon pair", i+" out of "+bases.size());
            KYWD newBase = (KYWD) m.getMajor(bases.get(i), GRUP_TYPE.KYWD);
            KYWD newVar = (KYWD) m.getMajor(vars.get(i), GRUP_TYPE.KYWD);
            SPGlobal.log("Weapon pair", newBase.getEDID() + " " + newVar.getEDID());
            Pair<KYWD, KYWD> p = new Pair(newBase, newVar);
            weaponMatches.add(p);
        }
    }

    static void buildOutfitWeapons(FLST baseWeaponKeysFLST) {
        FormID f = new FormID("107347", "Skyrim.esm");
        LVLI glist = (LVLI) merger.getMajor(f, GRUP_TYPE.LVLI);
        glist.set(LeveledRecord.LVLFlag.UseAll, false);

        for (OTFT lotft : merger.getOutfits()) {
            ArrayList<FormID> a = lotft.getInventoryList();
            boolean changed = false;
            for (FormID form : a) {

                WEAP weapon = (WEAP) merger.getMajor(form, GRUP_TYPE.WEAP);
                if (weapon != null) {
                    KYWD baseKey = weaponHasAnyKeyword(weapon, baseWeaponKeysFLST, merger);

                    if (hasVariant(weapon) && (baseKey != null)) {
                        String eid = "DienesLVLI" + weapon.getEDID();
                        MajorRecord r = merger.getMajor(eid, GRUP_TYPE.LVLI);
                        if (r == null) {
                            LVLI subList = (LVLI) patch.makeCopy(glist, eid);
                            InsertWeaponVariants(subList, form);
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

    private static boolean hasVariant(WEAP base) {
        boolean ret = false;
        for (ArrayList<FormID> vars : weaponVariants) {
            if (vars.contains(base.getForm()) && (vars.size() > 1)) {
                ret = true;
            }
        }

        return ret;
    }
}
