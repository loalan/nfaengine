/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Regex;

/**
 *
 * @author heckarim
 */
public class ElementPrefix extends Element {

    public int prefixId = 0000;

    ElementPrefix(Element e) {
        this.modifier = e.modifier;
        this.id = e.id;
        this.value = e.value;
        this.isAtom = true;
        this.isOptional = e.isOptional;
        this.isParentheis = e.isParentheis;
        this.isRepetition = e.isRepetition;
        //
        this.isInfix = e.isInfix;
        this.isPrefix = true;
        this.parsePrefix();
    }

    public ElementPrefix(int id) {
        this.prefixId = id;
        this.isPrefix = true;
        this.isAtom = true;
    }

    private void parsePrefix() {
        this.prefixId = Integer.parseInt(value);
    }

    public String toString() {
        int a4, a3, a2, a1;
        a4 = prefixId / 1000;
        a3 = (prefixId % 1000) / 100;
        a2 = (prefixId % 100) / 10;
        a1 = (prefixId % 10);
        String ret = "(!p" + a4 + "" + a3 + "" + a2 + "" + a1 + ")";
        return ret;
    }
}
