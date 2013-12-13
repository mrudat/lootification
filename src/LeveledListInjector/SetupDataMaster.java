/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package LeveledListInjector;

import java.util.HashMap;
import java.util.Set;
import skyproc.SPGlobal;

/**
 *
 * @author David
 */
public class SetupDataMaster extends SetupData {
    

    SetupDataMaster(HashMap d, HashMap a, HashMap w, Set t, Enum e){
        super( d,  a,  w,  t,  e);
    }
    
    @Override
    void setupStart(String logName) throws Exception{
        SPGlobal.newSpecialLog(logKey, logName);
        errorHeader = "Parse xml defines master";
        parseMaster("lli_defines_master.xml");
        errorHeader = "Parse xml defines custom";
        parseMaster("lli_defines_custom.xml");
    }
    
}
