package engineRe;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import NFA.*;
import java.io.File;
import javax.swing.text.Document;
import pcre.PcreRule;
import pcre.Refer;

/**
 *
 * @author heckarim
 *	This class is in charge of managing RegEx engine Structure and generating Verilog HDL code.
 */
public class ReEngine {
    //Declare constant

    public static final int _start = 0;   //BlockState Start
    public static final int _end = 1;     //Block State Exit
    public static final int _normal = 2;  //Block normal :D
    public String _outputfolder = "GeneratedFiles";                    //HDL output folder.
    public LinkedList<BlockChar> listBlockChar;    // All block char in RegEx engine.
    public BlockState start;                        // fist blockState in RegEx engine
    public BlockState end;                          // last blockState in RegEx engine
    public LinkedList<BlockState> listBlockState;  // All block State in RegEx engine.
    public PcreRule rule;                           // Need infomation about pcre Rule
    public int id_num;                              // many pcre rule, many engine? its responsibility
    public Document document = null;
    public int id_ram;

    //public boolean isInsideConRep = false; // 'cause reengine can exist inside Constraint Block so..
    //public BlockState ConRepBlock = null;  // isIn.. and ConRep.. is compatible with Bram ...

    public LinkedList<BlockContraint> listConRep = new LinkedList<BlockContraint>();
    // ^ use for ConRep Package
   

    public ReEngine() {
        this.id_num = 0;
        start = new BlockState(ReEngine._start, this);
        end = new BlockState(ReEngine._end, this);
        this.listBlockChar = new LinkedList<BlockChar>();
        this.listBlockState = new LinkedList<BlockState>();
        
    }

    public void print() {
        this.print(null);
    }

    /**
     * Print RegEx Engine Structure on Documnent object.
     * if doc = null, it just print on console.
     * @param doc
     */
    public void print(Document doc) {
        pcre.Refer.println("List of blockState", doc);
        for (int i = 0; i < this.listBlockState.size(); i++) {
            BlockState temp = this.listBlockState.get(i);
            pcre.Refer.println(temp.id + ": ", doc);

            if (temp.acceptChar != null) {
                switch (temp.acceptChar.code_id) {
                    case Refer._class:
                        pcre.Refer.println("\tAcceptchar: [" + temp.acceptChar.value + "]", doc);
                        break;
                    case Refer._neg_class:
                        pcre.Refer.println("\tAcceptchar: [^" + temp.acceptChar.value + "]", doc);
                        break;
                    default:
                        pcre.Refer.println("\tAcceptchar: " + temp.acceptChar.value, doc);
                }
            }
            if (temp.isStart) {
                pcre.Refer.println("\tIS START", doc);
            }
            if (temp.isEnd) {
                pcre.Refer.println("\tIS END", doc);
            }
            pcre.Refer.print("\tComming: ", doc);
            if (temp.comming != null) {
                for (int j = 0; j < temp.comming.size(); j++) {
                    pcre.Refer.print(temp.comming.get(j).id + " - ", doc);
                }
            }

            pcre.Refer.print("\n\tGoing: ", doc);
            if (temp.going != null) {
                for (int j = 0; j < temp.going.size(); j++) {
                    pcre.Refer.print(temp.going.get(j).id + " - ", doc);
                }
            }
            pcre.Refer.print("\n", doc);
        }
        pcre.Refer.print("\nList BlockChar: ", doc);
        for (int i = 0; i < this.listBlockChar.size(); i++) {
            pcre.Refer.print(this.listBlockChar.get(i).value + " - ", doc);
        }
        pcre.Refer.println("\nTotal: " + this.listBlockState.size() + " block State " + this.listBlockChar.size() + " blockChar", doc);
    }

    /**
     *  BlockState and blockChar need arrange in order, avoid name conflict.
     */
    public void updateId() {
        for (int i = 0; i < this.listBlockState.size(); i++) {
            this.listBlockState.get(i).id = i;
        }
        for (int i = 0; i < this.listBlockChar.size(); i++) {
            this.listBlockChar.get(i).id = i;
        }
    }

    /**
     * Create RegEx engine from NFA, use Compact Architecture for High-Throughput model...
     *  of yeyang, weirongj, prasanna
     * @param nfa
     */
    public void createEngine(NFA nfa) {
        NFAEdge eWalk = nfa.start.edge;
        this.rule = nfa.rule;
        while (eWalk != null) {
            if (!eWalk.isEpsilon) {
                pcre.Refer.println("Error: createEngine, nfa don't in formated form", this.document);
                return;
            }
            BlockState bTemp = buildBlockRecusive(eWalk.dest);
            if (bTemp == null) {
                pcre.Refer.println("Error: createEngine, What happen??", this.document);
                return;
            }
            this.start.going.add(bTemp);
            bTemp.comming.add(this.start);
            eWalk = eWalk.nextEdge;
        }
        // Finish building engine, here.
        this.listBlockState.addFirst(this.start);
        this.listBlockState.addLast(this.end);
        this.updateId();

        // process BlockkContraint
        for (int i = 0; i < this.listBlockState.size(); i++) {
            BlockState walk = this.listBlockState.get(i);
            if (walk.isStart || walk.isEnd) {
                continue;
            }
            if (walk.acceptChar.isConsRep()) {
                BlockState newBlock = new BlockContraint(walk);
                this.listBlockState.remove(i);
                this.listBlockState.add(i, newBlock);
            }
        }
    }

    public BlockState buildBlockRecusive(NFAState state) {
        if (state.isVisited) {
            BlockState ret = state.returnBlock;
            return ret;
        }

        state.isVisited = true;
        NFAEdge eTemp = state.getCharEdge();

        if (eTemp != null) {
            BlockChar bChar = new BlockChar(eTemp, this);
            this.listBlockChar.add(bChar);
            BlockState bState = new BlockState(ReEngine._normal, this);
            this.listBlockState.add(bState);
            bState.acceptChar = bChar;
            bChar.toState.add(bState);
            state.returnBlock = bState;
            NFAEdge eWalk = eTemp.dest.edge;        // don't expect edgeChar here
            while (eWalk != null) {
                if (!eWalk.isEpsilon) {
                    pcre.Refer.println("Error: Recusive, why is there edgeChar here??", this.document);
                    return null;
                }
                BlockState bTemp = buildBlockRecusive(eWalk.dest);
                if (bTemp == null) {
                    pcre.Refer.println("Error: Recusive, What happen =.=??", this.document);
                    return null;
                }
                bState.going.add(bTemp);
                bTemp.comming.add(bState);
                eWalk = eWalk.nextEdge;
            }
            return bState;
        } else if (state.isFinal) {
            state.isVisited = true;
            state.returnBlock = this.end;
            return this.end;
        } else {
            pcre.Refer.println("You will never come here =_=", this.document);
        }

        return null;
    }

    /**
     * This function will reduce similar blockChar.
     * note:
     *      - depend on code_id and value.
     *     //TODO
     */
    public void reduceBlockChar() {
        for (int i = 0; i < this.listBlockChar.size(); i++) {
            BlockChar temp = this.listBlockChar.get(i);
            for (int j = i + 1; j < this.listBlockChar.size(); j++) {
                BlockChar walk = this.listBlockChar.get(j);
                if(this.compareBlockChar(temp, walk)){
                    for (int k = 0; k < walk.toState.size(); k++) {
                        temp.toState.add(walk.toState.get(k));
                        walk.toState.get(k).acceptChar = temp;
                    }
                    this.listBlockChar.remove(j);
                        j--; //just remove one char so ...
                }

            }
        }
        this.synchonousBlockCharConRep();
        pcre.Refer.print("\n engine: " + this.id_num + " blockChar after reduce: ", this.document);
        for (int i = 0; i < this.listBlockChar.size(); i++) {
            pcre.Refer.print(" " + this.listBlockChar.get(i).value, this.document);
        }
        pcre.Refer.print("\n", this.document);


    }

    public void synchonousBlockCharConRep(){
        //if there is contraint block -> synchonous listchar of Constraint block with this list char
        if(!this.listConRep.isEmpty()){
            for(int i = 0; i<this.listConRep.size(); i++){
                ReEngine te = this.listConRep.get(i).ContEngine;
                //fist need ensure no rep in listchar of ContEngine;
                te.reduceBlockChar();
                // now replace all charblock in contengine with of this.
                for(int j =0; j<this.listBlockChar.size(); j++){
                    BlockChar temp = this.listBlockChar.get(j);
                    for(int k =0; k<te.listBlockChar.size();k++){
                        BlockChar walk = te.listBlockChar.get(k);
                        if(this.compareBlockChar(temp, walk)){
                            te.listBlockChar.remove(k);
                            te.listBlockChar.add(k,temp);
                        }
                    }
                }
            }
        }
    }

    public boolean compareBlockChar(BlockChar temp, BlockChar walk) {
        boolean res = false;
        if (temp.code_id == walk.code_id) {
            if (temp.value.compareTo(walk.value) == 0) {
                /*for (int k = 0; k < walk.toState.size(); k++) {
                    temp.toState.add(walk.toState.get(k));
                    walk.toState.get(k).acceptChar = temp;
                }*/
                res = true;
            } else {
                if (this.rule.getModifier().indexOf("i") != -1 && temp.value.compareToIgnoreCase(walk.value) == 0) {// neu la case insensitive
                    //chep toState tu walk vo temp;
                    /*for (int k = 0; k < walk.toState.size(); k++) {
                        temp.toState.add(walk.toState.get(k));
                        walk.toState.get(k).acceptChar = temp;
                    }*/
                    res = true;
                }
            }
        }
        return res;
    }

    /**
     * module myDff (q_o,d_i, clk,en,rst);
    input d_i,clk,en,rst;
    output reg q_o;
    always @ (posedge clk)
    begin
     *      if(rst == 1'b1)
     *          q_o <= 1'b0;
     *      else if(!en)
     *          q_o <= q_o;
     *      else
    q_o <= d_i;
    end
    endmodule

     */
    public void buildDflipflop() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this._outputfolder + File.separator + "myDff.v"));
            bw.write("module myDff (q_o,d_i, clk,en,rst);\n "
                    + "\t input d_i,clk,en,rst;\n"
                    + "\t output reg q_o;\n"
                    + "\t always @ (posedge clk)\n"
                    + "\t\t begin\n"
                    + "\t\tif (rst == 1'b1)\n"
                    + "\t\t\tq_o <= 1'b0;\n"
                    + "\t\telse if (en)\n"
                    + "\t\t\tq_o <= d_i;\n"
                    + "\t\telse\n"
                    + "\t\t\tq_o <= q_o;\n"
                    + "\t\t end\n"
                    + "endmodule\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void buildHDL(String folder) {
        this._outputfolder = folder;
        this.buildHDL();
    }

    /**
     * Example:
     *  module engine_0(out,clk,sod,en,data,stop,eod);
    input [7:0] char;
    input clk,sod,en,eod;
    output out,stop;

    charBlock_0_0_97 (char_0_0_97,char);
    charBlock_0_1_98 (char_0_1_98,char);
    charBlock_0_2_99 (char_0_2_99,char);
    state_0_1 (w1,char_0_0_97,clk,w0);
    state_0_2 (w2,char_0_1_98,clk,w1);
    state_0_3 (w3,char_0_2_99,clk,w2);
    state_0_4 (out,clk,w3);
    endmodule
     */
  /*  public void buildHDL() {	//Build myDff;
        this.buildDflipflop();
        //Create top module HDL code.
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this._outputfolder + "engine_" + this.id_num + ".v"));
            bw.write("module engine_" + this.id_num + "(out,clk,sod,en,char);\n");
            bw.write("//pcre: " + this.rule.toString());
            // declare parameter, variable verilog
            bw.write("\n\tinput [7:0] char;\n"
                    + "\tinput clk,sod,en;\n");
            bw.write("\toutput out;\n\n");

            // net connect Block Char.
            int size = this.listBlockChar.size();
            for (int i = 0; i < size; i++) {
                BlockChar bc = this.listBlockChar.get(i);
                if (bc.isConsRep()) {
                    continue;
                }
                bw.write("\tcharBlock_" + this.id_num + "_" + bc.id + " BC_" + this.id_num + "_" + bc.id + " (char_" + this.id_num + "_" + bc.id + ",char);\n");
            }

            //net connect block State
            //Start State
            boolean start_n = false;
            if (this.rule.getModifier().contains("t")) {
                if (this.rule.getModifier().contains("m")) {
                    bw.write("\n\tstate_" + this.id_num + "_" + "0 St_0 (y1,~y3,clk,en,sod);\n");
                    start_n = true;
                    bw.write("\t charBlock_" + this.id_num + "_" + "100000" + " cB (y3,char);\n");
                } else {
                    //TODO: need to replace it to new structure
                    // bw.write("\tassign w0 = ~y1;");
                    // bw.write ("\tmyDff D_" + this.id_num + " (y,1'b1,clk,en,sod);\n");
                    // bw.write("\tassign y1=~y;\n");
                    bw.write("\n\tstate_" + this.id_num + "_" + "0 St_0 (y1,1'b1,clk,en,sod);\n");
                }
            } else {
                bw.write("\n\tstate_" + this.id_num + "_" + "0 St_0 (y1,1'b0,clk,en,sod);\n");
            }
            bw.write("\tassign w0 = ~y1;\n");

            //route other blockState
            size = this.listBlockState.size();
            for (int i = 1; i < size; i++) {
                BlockState bt = this.listBlockState.get(i);
                if (bt.isEnd) {
                    bw.write("\tstate_" + this.id_num + "_" + bt.id + " BS_" + this.id_num + "_" + bt.id + " (out" + ",clk,en,sod");
                    for (int j = 0; j < bt.comming.size(); j++) {
                        bw.write(",w" + bt.comming.get(j).id);
                    }
                    bw.write(");\n");
                } else if (bt.isContraint) {
                    //TODO
                    //module blockContraint_0_id (out, in, clk);
                    bw.write("\tBlockContraint_" + this.id_num + "_" + bt.id + " BS_" + this.id_num + "_" + bt.id + "(w" + bt.id + ",char");
                    for (int j = 0; j < bt.comming.size(); j++) {
                        bw.write(",w" + bt.comming.get(j).id);
                    }
                    bw.write(",clk,en,sod);\n");
                } else {
                    bw.write("\tstate_" + this.id_num + "_" + bt.id + " BS_" + this.id_num + "_" + bt.id + " (w" + bt.id + ",char_" + this.id_num + "_" + bt.acceptChar.id + ",clk,en,sod");
                    for (int j = 0; j < bt.comming.size(); j++) {
                        bw.write(",w" + bt.comming.get(j).id);
                    }
                    bw.write(");\n");
                }
            }
            bw.write("endmodule\n\n");

            //Create verilog code for Block Char.
            for (int i = 0; i < this.listBlockChar.size(); i++) {
                this.listBlockChar.get(i).buildHDL(bw);
                bw.write("\n");
            }

            if (start_n)// There is modifier m and ^ operator, create new blockChar of '\n'
            {
                BlockChar temp = new BlockChar('n', this);
                temp.bw = bw;
                temp.id = 100000;
                temp.buildHex(10);
            }
            bw.write("\n");
            //Build hdl block State
            for (int i = 0; i < this.listBlockState.size(); i++) {
                if (this.listBlockState.get(i).isContraint) {
                    this.listBlockState.get(i).buildHDL();
                } else {
                    this.listBlockState.get(i).buildHDL(bw);
                }
            }
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
   * */
    public void buildHDL() {
        System.out.println("Here");
        this.buildDflipflop(); // build DFF
        
        //this.reduceBlockChar(); // need here

        System.out.println(" Engine " + this.id_num + ": " + this.listBlockChar.size());
        for(int i = 0; i < this.listBlockChar.size(); i++){
            BlockChar bc = this.listBlockChar.get(i);
            System.out.print(bc.value + "[" + bc.id +"] ");
        }
         System.out.println();
        //Create top module HDL code.
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this._outputfolder + File.separator + "engine_" + this.id_ram + "_" + this.id_num + ".v"));
            bw.write("module engine_" + this.id_ram + "_" + this.id_num + "(out,clk,sod,en");
            for(int i = 0; i < this.listBlockChar.size(); i++) {
                //this.listBlockChar.get(i).id = i; //update id for block char
                bw.write(", in_" + this.id_num + "_" + this.listBlockChar.get(i).id);
            }
            bw.write(");\n");
            bw.write("//pcre: " + this.rule.toString() +"\n");
            bw.write("//block char: ");
            for(int i = 0; i < this.listBlockChar.size(); i++) {
                bw.write(this.listBlockChar.get(i).value + "[" + this.listBlockChar.get(i).id +"], ");
            }
            bw.write("\n");

            // declare parameter, variable verilog
            bw.write("\n\tinput clk,sod,en;\n");
            bw.write("\n\tinput ");
            for(int i = 0; i < this.listBlockChar.size(); i++) {
                if (i == 0)
                    bw.write("in_" + this.id_num + "_" + this.listBlockChar.get(i).id);
                else
                    bw.write(", in_" + this.id_num + "_" + this.listBlockChar.get(i).id);
            }
            bw.write(";\n");
            bw.write("\toutput out;\n\n");

            //routing here
            //net connect block State
            //Start State
            boolean start_n = false;
            if (this.rule.getModifier().contains("t")) { //???? this pcre contain ^
                if (this.rule.getModifier().contains("m")) {
                    bw.write("\n\tstate_" + this.id_ram + "_"+ this.id_num + "_" + "0 St_0 (y1,~y3,clk,en,sod);\n");
                    start_n = true;
                    bw.write("\t charBlock_" + this.id_num + "_" + "100000" + " cB (y3,char);\n");
                } else {
                    //TODO: need to replace it to new structure
                    // bw.write("\tassign w0 = ~y1;");
                    // bw.write ("\tmyDff D_" + this.id_num + " (y,1'b1,clk,en,sod);\n");
                    // bw.write("\tassign y1=~y;\n");
                    bw.write("\n\tstate_" + this.id_ram + "_" + this.id_num + "_" + "0 St_0 (y1,1'b1,clk,en,sod);\n");
                }
            } else {
                bw.write("\n\tstate_" + this.id_ram + "_" + this.id_num + "_" + "0 St_0 (y1,1'b0,clk,en,sod);\n");
            }
            bw.write("\tassign w0 = ~y1;\n");

             //route other blockState
            int size = this.listBlockState.size();
            for (int i = 1; i < size; i++) {
                BlockState bt = this.listBlockState.get(i);
                if (bt.isEnd) {
                    bw.write("\tstate_" + this.id_ram + "_" + this.id_num + "_" + bt.id + " BS_"  + this.id_ram + "_" + this.id_num + "_" + bt.id + " (out" + ",clk,en,sod");
                    for (int j = 0; j < bt.comming.size(); j++) {
                        bw.write(",w" + bt.comming.get(j).id);
                    }
                    bw.write(");\n");
                } else if (bt.isContraint) {
                    //TODO
                    //module blockContraint_0_id (out, in, clk);
                    BlockContraint btc = (BlockContraint) bt;
                    bw.write("\tBlockConRep_" + this.id_ram + "_" + this.id_num + "_" + bt.id +  " BS_ConRep_" + this.id_ram + "_" + this.id_num +  "_" + bt.id + "(w" + bt.id );
                    // need insert character
                    for(int j =0; j<btc.ContEngine.listBlockChar.size(); j++){
                        bw.write(",in_" + this.id_num + "_" + btc.ContEngine.listBlockChar.get(j).id);
                    }
                    
                    bw.write(",clk,en,sod");
                    for (int j = 0; j < bt.comming.size(); j++) {
                        bw.write(",w" + bt.comming.get(j).id);
                    }
                    bw.write(");\n");
                } else {
                    bw.write("\tstate_" + this.id_ram + "_" + this.id_num + "_" + bt.id + " BS_" + this.id_ram + "_" + this.id_num + "_" + bt.id + " (w" + bt.id + ",in_" + this.id_num + "_" + bt.acceptChar.id + ",clk,en,sod");
                    for (int j = 0; j < bt.comming.size(); j++) {
                        bw.write(",w" + bt.comming.get(j).id);
                    }
                    bw.write(");\n");
                }
            }
            
            bw.write("endmodule\n\n");

            //Build hdl block State
            for (int i = 0; i < this.listBlockState.size(); i++) {
                if (this.listBlockState.get(i).isContraint) {
                    this.listBlockState.get(i).buildHDL();
                } else {
                    this.listBlockState.get(i).buildHDL(bw);
                }
            }
            bw.flush();
            bw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
