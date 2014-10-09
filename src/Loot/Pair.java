/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Loot;

/**
 *
 * @author David
 */
class Pair<L, R> {

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
