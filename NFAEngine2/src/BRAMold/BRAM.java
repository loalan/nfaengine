/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package BRAMold;

import engineRe.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import PCRE.Refer;

/**
 *
 * @author Richard Le
 */
public class BRAM {

    public LinkedList<ReEngine> engineList; // list of engine
   // public char[] entries;                // string present a single entry
    public char[][] BRam;         // Bram structure
    public LinkedList<BlockChar> blockCharList; // need for routing from BRAM to State block
    public int ID;                         // ID of this BRAM
    int row = -1;
    int col = -1;
    public int width = -1;
    public  String _outputFolder = "GeneratedFiles";
    //use for output result
    public int noNFA;
    public int noPCRE;
    public int noBlockState;
    public int noCRB; //number of  Constraint repetition Block;



    public BRAM(int id) {
        this.engineList = new LinkedList<ReEngine>();
        this.blockCharList = new LinkedList();
        this.ID = id;
        this.BRam = new char[256][1]; // deep = 256, width = 1
        File file = new File(this._outputFolder);
        file.mkdir();
    }

    public void addEngine(ReEngine engine, int id) {
        this.engineList.add(engine);
        this.engineList.get(this.engineList.size() - 1).id_num = id;
    }

    /**************************************
     * This function to share charBlock
     *
     ***************************************/
    public void unionCharBlocks() {
         for (int i = 0; i < this.engineList.size(); i++) {
            engineList.get(i).reduceBlockChar();
            for (int j = 0; j < engineList.get(i).listBlockChar.size(); j++) {
                this.blockCharList.add(engineList.get(i).listBlockChar.get(j));
            }
        }
        for (int i = 0; i < this.blockCharList.size(); i++) {
            BlockChar temp = this.blockCharList.get(i);
            temp.array_id[0] = temp.engine.id_num;
            temp.listToState.add(temp.toState);
            for (int j = i + 1; j < this.blockCharList.size(); j++) {
                BlockChar walk = this.blockCharList.get(j);
                if (this.compareBlockChar(temp, walk)) {
                    temp.listToState.add(walk.toState);
                    temp.array_id[temp.listToState.size() - 1] = walk.engine.id_num;
                    this.blockCharList.remove(walk);
                }
            }
        }


        /*for (int i = 0; i < engineList.size(); i++) {
        ReEngine temp = engineList.get(i);
        for (int j = 0; j < temp.listBlockChar.size(); j++) {
        BlockChar walk = temp.listBlockChar.get(j);
        for (int k = i + 1; k < engineList.size(); k++) {
        for (int h = 0; h < engineList.get(k).listBlockChar.size(); h++) {
        }
        }
        }
        }*/
        this.width = this.blockCharList.size();
        //update ID;
        for (int i = 0; i < this.blockCharList.size(); i++) {
            //System.out.print(this.blockCharList.get(i).value + " ");
            this.blockCharList.get(i).id = i;
            System.out.print(this.blockCharList.get(i).value + "[" + this.blockCharList.get(i).id +"] ");
        }

        for(int i = 0; i < this.blockCharList.size(); i++) {
            BlockChar bChar = this.blockCharList.get(i);
            for(int j = 0; j < this.engineList.size(); j++){
                ReEngine engine = this.engineList.get(j);
                for(int k = 0; k < engine.listBlockChar.size(); k++) {
                    BlockChar bc = engine.listBlockChar.get(k);
                    if(bc.engine.rule.getModifier().contains("i") && bChar.engine.rule.getModifier().contains("i") && bc.value.compareToIgnoreCase(bChar.value) == 0) {
                        bc.id = bChar.id;
                    }
                    else if ((!bc.engine.rule.getModifier().contains("i") || !bChar.engine.rule.getModifier().contains("i")) && bc.value.compareTo(bChar.value) == 0) {
                        bc.id = bChar.id;
                    }
                }
            }
        }

        //need sort list
         for(int i = 0; i < this.engineList.size(); i++){
                ReEngine engine = this.engineList.get(i);
                for(int j = 0; j < engine.listBlockChar.size(); j++) {
                    BlockChar bc = engine.listBlockChar.get(j);
                    for(int k = j + 1; k < engine.listBlockChar.size(); k++) {
                        BlockChar bc_1 = engine.listBlockChar.get(k);
                        BlockChar temp;
                        if(bc.id > bc_1.id) {
                            temp = bc;
                            engine.listBlockChar.add(k+1, temp);
                            engine.listBlockChar.remove(j);
                            j--;
                            break;
                    }
                }
            }
         }


        System.out.println();
        for(int i = 0; i < this.engineList.size(); i++) {
            ReEngine eng = this.engineList.get(i);
            System.out.println(" Eng" + eng.id_num +": ");
            for(int j = 0; j < eng.listBlockChar.size(); j++) {
                BlockChar bc = eng.listBlockChar.get(j);
                System.out.print(bc.value + "[" + bc.id +"] ");
            }
            System.out.println();
        }
        System.out.println();


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
                if (walk.engine.rule.getModifier().indexOf("i") != -1 && temp.engine.rule.getModifier().indexOf("i") != -1 && temp.value.compareToIgnoreCase(walk.value) == 0) {// neu la case insensitive
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


    public void fillEntryValue() {

        this.BRam = new char[256][this.blockCharList.size()];

        int hexValue = -1;
        for (col = 0; col < this.blockCharList.size(); col++) {
            BlockChar temp = this.blockCharList.get(col);
            switch (temp.code_id) {
            case Refer._char:
                //just single char
                this.fillSingle(temp.value, temp.engine.rule.getModifier());
                break;
            case Refer._class:
                //this is class char
                this.fillCharClass(temp.value, temp.engine.rule.getModifier());
                break;
            case Refer._neg_class:
                this.fillNegCharClass(temp.value, temp.engine.rule.getModifier());
                break;
            case Refer._ascii_hex:
                // \xFF
                hexValue = Integer.parseInt(temp.value.substring(2), 16);
                this.fillHex(hexValue);
                break;
            case Refer._class_digit:
                // \d
                this.fillDigitRange();
                break;
            case Refer._class_dot:
                // .
                this.fillDotClass(temp.engine.rule.getModifier());
                break;

            /*case Refer._char_start:
            case Refer._char_end:
                //FIXME
                hexValue = 10;
                this.buildHex(hexValue);
                break;*/

            case Refer._class_word:
                // \w
                this.fillCharRange();
                break;
            case Refer._op_between:
            case Refer._op_atleast:
            case Refer._op_exactly:
                break;
            default:
                this.fillSingle(temp.value.substring(0), temp.engine.rule.getModifier());
                break;
        }
    }



}

    private void fillSingle(String value, String modifier) {
        //throw new UnsupportedOperationException("Not yet implemented");

        int ascii = (int) value.charAt(0);
        //iniitialize
        for(int i = 0; i < 256; i++)
            BRam[i][col] = '0';

        if (modifier.contains("i")) {
            if (ascii >= 65 && ascii <= 90) {
                BRam[ascii][col] = '1';
                BRam[ascii + 32][col] = '1';
            }
            else if (ascii >= 97 && ascii <= 122) {
                BRam[ascii][col] = '1';
                BRam[ascii - 32][col] = '1';
            }
            else // not letter
                BRam[ascii][col] = '1';
        } else { // case sensitive
            BRam[ascii][col] = '1';
        }
    }

    private void fillCharClass(String value, String modifier) {
        //initialize
        for (int i = 0; i < 256; i++)
            BRam[i][col] = '0';
         // value = [abc]
            int from = 0, to = 0;
            value = value.substring(1, value.length() - 1);
            for (int i = 0; i < value.length(); i++) {
                int hex;
                if (value.charAt(i) == '\\') {
                    switch (value.charAt(i + 1)) {
                        case 'x':
                        case 'X':
                            hex = Integer.parseInt(value.substring(i + 2, i + 4), 16);
                            BRam[hex][col] = '1';
                            i = i + 3;
                            break;
                        case 'd':
                            for (int j = 48; j <= 57; j++)
                                BRam[j][col] = '1';
                            i++;
                            break;
                        case 'w':
                            //TODO
                            i++;
                            break;
                        case 's': // white space \x20 = 32
                            BRam[32][col] = '1';
                            i++;
                            break;
                        case 'n': // LF \x0A
                            BRam[10][col] = '1';
                            i++;
                            break;
                        case 'r': // CR \x0D
                            BRam[14][col] = '1';
                            i++;
                            break;
                        case 't': // tab \x09
                            BRam[9][col] = '1';
                            i++;
                            break;
                        case 'z': // range
                            from = (int) Integer.valueOf(value.substring(i + 2, i + 4), 16);
                            to = (int) Integer.valueOf(value.substring(i + 4, i + 6), 16);
                            if (modifier.contains("i")) {
                                int from1 = 0, to1 = 0;
                                if (from >= 65 && from <= 90) {
                                    from1 = from + 32;
                                } else if (from >= 97 && from <= 122) {
                                    from1 = from - 32;
                                }
                                if (to >= 65 && to <= 90) {
                                    to1 = to + 32;
                                } else if (from >= 97 && from <= 122) {
                                    to1 = to - 32;
                                }
                                if ((from >= 65 && from <= 90 && to >= 65 && to <= 90)
                                     || (from >= 97 && from <= 122 && to >= 97 && to <= 122)) {
                                     for (int j = from; j <= to; j++)
                                         BRam[j][col] = '1';
                                     for (int j = from1; j <= to1; j++)
                                         BRam[j][col] = '1';
                                } else  {// not letter
                                    for (int j = from; j <= to; j++)
                                        BRam[j][col] = '1';
                                }
                            } else { // case sensitive
                                 for (int j = from; j <= to; j++)
                                        BRam[j][col] = '1';
                            }
                            i = i + 3;
                            break;
                        default: // \?
                            hex = (int) value.charAt(i + 1);
                            BRam[hex][col] = '1';
                            i++;
                            break;

                    }
                } else {
                    //value.charAt(i) != '\\' ex: a
                    BRam[(int) value.charAt(i)][col] = '1';

                }
            }
    }

   /* this finction to fill \w*/
    private void fillCharRange() {
        //throw new UnsupportedOperationException("Not yet implemented");
        // 48 - 57 [0 - 9]
        // 65 - 90 [A - Z]
        // 97 - 122 [a - z]
        for(int i = 0; i <= 47; i++)
            BRam[i][col] = '0';
        for(int i = 48; i <= 57; i++)
            BRam[i][col] = '1';
        for(int i = 58; i <= 64; i++)
            BRam[i][col] = '0';
        for(int i = 65; i <= 90; i++)
            BRam[i][col] = '1';
        for(int i = 91; i <= 96; i++)
            BRam[i][col] = '0';
        for(int i = 97; i <= 122; i++)
            BRam[i][col] = '1';
        for(int i = 123; i <= 255; i++)
            BRam[i][col] = '0';
    }


    private void fillDigitRange() {
        //throw new UnsupportedOperationException("Not yet implemented");
        // 48 - 57 [0 - 9]
         for(int i = 0; i <= 47; i++)
            BRam[i][col] = '0';
        for(int i = 48; i <= 57; i++)
            BRam[i][col] = '1';
        for(int i = 58; i <= 255; i++)
            BRam[i][col] = '0';

    }

    private void fillNegCharClass(String value, String modifier) {
        //initialize
        for (int i = 0; i < 256; i++)
            BRam[i][col] = '1';
         // value = [^abc]
            int from = 0, to = 0;
            System.out.println("value: " + value);
            value = value.substring(2, value.length() - 1);
             System.out.println("value after: " + value);
            for (int i = 0; i < value.length(); i++) {
                int hex;
                if (value.charAt(i) == '\\') {
                    switch (value.charAt(i + 1)) {
                        case 'x':
                        case 'X':
                            hex = Integer.parseInt(value.substring(i + 2, i + 4), 16);
                            BRam[hex][col] = '0';
                            i = i + 3;
                            break;
                        case 'd':
                            for (int j = 48; j <= 57; j++)
                                BRam[j][col] = '0';
                            i++;
                            break;
                        case 'w':
                            //TODO
                            i++;
                            break;
                        case 's': // white space \x20 = 32
                            BRam[32][col] = '0';
                            i++;
                            break;
                        case 'n': // LF \x0A
                            BRam[10][col] = '0';
                            i++;
                            break;
                        case 'r': // CR \x0D
                            BRam[14][col] = '0';
                            i++;
                            break;
                        case 't': // tab \x09
                            BRam[9][col] = '0';
                            i++;
                            break;
                        case 'z': // range
                            from = (int) Integer.valueOf(value.substring(i + 2, i + 4), 16);
                            to = (int) Integer.valueOf(value.substring(i + 4, i + 6), 16);
                            if (modifier.contains("i")) {
                                int from1 = 0, to1 = 0;
                                if (from >= 65 && from <= 90) {
                                    from1 = from + 32;
                                } else if (from >= 97 && from <= 122) {
                                    from1 = from - 32;
                                }
                                if (to >= 65 && to <= 90) {
                                    to1 = to + 32;
                                } else if (from >= 97 && from <= 122) {
                                    to1 = to - 32;
                                }
                                if ((from >= 65 && from <= 90 && to >= 65 && to <= 90)
                                     || (from >= 97 && from <= 122 && to >= 97 && to <= 122)) {
                                     for (int j = from; j <= to; j++)
                                         BRam[j][col] = '0';
                                     for (int j = from1; j <= to1; j++)
                                         BRam[j][col] = '0';
                                } else  {// not letter
                                    for (int j = from; j <= to; j++)
                                        BRam[j][col] = '0';
                                }
                            } else { // case sensitive
                                 for (int j = from; j <= to; j++)
                                        BRam[j][col] = '0';
                            }
                            i = i + 3;
                            break;
                        default: // \?
                            BRam[(int) value.charAt(i + 1)][col] = '0';
                            i++;
                            break;

                    }
                } else {
                    //value.charAt(i) != '\\' ex: a
                    BRam[(int) value.charAt(i)][col] = '0';

                }
            }
    }


    private void fillHex(int hexValue) {
        //throw new UnsupportedOperationException("Not yet implemented");
         for(int i = 0; i < 256; i++) {
            if (i != hexValue)
                BRam[i][col] = '0';
            else
                BRam[i][col] = '1';
        }

    }

    private void fillDotClass(String modifier) {
        for(int i = 0; i < 256; i++) {
            if (!modifier.contains("s")) {
                //match all except '\n'
                if (i == 10)
                    BRam[i][col] = '0';
                else
                    BRam[i][col] = '1';
            } else
                BRam[i][col] = '1';

        }
    }

    public void printBRam() {
        System.out.println("Width: " + width);
        for(int i = 0; i < this.blockCharList.size(); i ++) {
            System.out.print(this.blockCharList.get(i).value + " ");
        }
        System.out.println();
        for (int i = 0 ; i < 256; i++) {
            if (i >= 32 && i <= 127)
                System.out.print((char) i + ": ");
            else
                System.out.print(i + ": ");
            for(int j = 0; j < width; j++ )
                System.out.print(BRam[i][j] + " ");
            System.out.println();
        }
    }

    public void buildCOE() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this._outputFolder + File.separator + "BRAM_" + ID + ".coe" ));
            bw.write(";");
            for(int i = this.blockCharList.size() - 1; i >= 0; i--) {
                bw.write(this.blockCharList.get(i).value + " ");
            }
            bw.write("\n");
            bw.write(";Block memory of depth=256, and width=" + width + "\n"
                    + "MEMORY_INITIALIZATION_RADIX=2;\n"
                    + "MEMORY_INITIALIZATION_VECTOR=\n");
            for (int i = 0; i < 256; i++) {
                for(int j = width - 1; j >= 0; j--) {
                    bw.write(BRam[i][j]);
                }
                if (i != 255)
                    bw.write(",\n");
                else if (i == 255)
                    bw.write(";\n");
            }

            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*Example:
    module BRAM_0(out,clk,sod,en,char);
	input clk, sod, en;
	input [7:0] char;
	output [2:0] out;
	wire [12:0] q_out;

////BRAM declare

bram_entity_0 ram (
	.addr(char),
	.clk(clk),
	.dout(q_out),

	engine_0 en_0(out[0], clk, sod, en, q_out[0], q_out[1], q_out[2], q_out[3], q_out[4], q_out[5]);
	engine_1 en_1(out[1], clk, sod, en, q_out[1], q_out[2], q_out[3], q_out[6], q_out[7], q_out[8], q_out[9], q_out[10]);
	engine_2 en_2(out[2], clk, sod, en, q_out[1], q_out[6], q_out[11], q_out[12]);

endmodule

     */

    public void buildHDL() {
        try {

            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this._outputFolder + File.separator + "BRAM_" + this.ID + ".v")));
            bw.write("module BRAM_" + this.ID + "(out,clk,sod,en,char);\n");
            bw.write("\tinput clk, sod, en;\n");
            bw.write("\tinput [7:0] char;\n");
            bw.write("\toutput [" + (this.engineList.size() - 1) + ":0] out;\n" );
            bw.write("\twire [" + (this.width - 1) + ":0] q_out;\n");

            //BRAM declare
            // assume that output of single port ram is q_out[width - 1 : 0]
            bw.write("\n//BRAM declare \n");

            bw.write("\tbram_entity_" + this.ID + " ram (.addr(char),.clk(clk),.dout(q_out),.en(en));\n");
            //end of bram

            //declare engine
            for(int i = 0; i < this.engineList.size(); i++) {
                bw.write("\tengine_" + this.ID + "_" + this.engineList.get(i).id_num + " en_" + this.ID + "_" + this.engineList.get(i).id_num +  "(out[" + i + "], clk, sod, en"); // thieu char

                //routing to each engine
                ReEngine te = this.engineList.get(i);
                //routing to each engine
                for(int j =0; j<te.listBlockChar.size();j++) {
                    bw.write(", q_out[" + te.listBlockChar.get(j).id + "]");
                }

                bw.write(");\n");
            }
            for(int i = 0; i < this.engineList.size(); i++)
                this.engineList.get(i).buildHDL();

           bw.write("\n");
           bw.write("endmodule\n");
           bw.flush();
           bw.close();



        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void buildXCO(){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this._outputFolder + File.separator + "bram_entity_" + this.ID + ".xco")));
            bw.write("# BEGIN Project Options\n" +
            "SET flowvendor = Foundation_iSE\n" +
            "SET vhdlsim = True\n" +
            "SET verilogsim = True\n" +
            "SET workingdirectory = " + "." + "\n" + //working dir
            "SET speedgrade = -7\n" +
            "SET simulationfiles = Behavioral\n" +
            "SET asysymbol = True\n" +
            "SET addpads = False\n" +
            "SET device = xc2vp50\n" +
            "SET implementationfiletype = Edif\n" +
            "SET busformat = BusFormatAngleBracketNotRipped\n" +
            "SET foundationsym = False\n" +
            "SET package = ff1148\n" +
            "SET createndf = False\n" +
            "SET designentry = VHDL\n" +
            "SET devicefamily = virtex2p\n" +
            "SET formalverification = False\n" +
            "SET removerpms = False\n" +
            "# END Project Options\n" +
            "# BEGIN Select\n" +
            "SELECT Single_Port_Block_Memory family Xilinx,_Inc. 6.2\n" +
            "# END Select\n" +
            "# BEGIN Parameters\n" +
            "CSET handshaking_pins=false\n" +
            "CSET init_value=0\n" +
            "CSET coefficient_file=" + "BRAM_" + this.ID + ".coe\n" +
            "CSET select_primitive=512x36\n" +
            "CSET initialization_pin_polarity=Active_High\n" +
            "CSET global_init_value=0\n" +
            "CSET depth=256\n" +
            "CSET write_enable_polarity=Active_High\n" +
            "CSET port_configuration=Read_Only\n" +
            "CSET enable_pin_polarity=Active_High\n" +
            "CSET component_name=" + "bram_entity_" + this.ID + "\n" +
            "CSET active_clock_edge=Rising_Edge_Triggered\n" +
            "CSET additional_output_pipe_stages=0\n" +
            "CSET disable_warning_messages=true\n" +
            "CSET limit_data_pitch=18\n" +
            "CSET primitive_selection=Select_Primitive\n" +
            "CSET enable_pin=true\n" +
            "CSET init_pin=false\n" +
            "CSET write_mode=Read_After_Write\n" +
            "CSET has_limit_data_pitch=false\n" +
            "CSET load_init_file=true\n" +
            "CSET width=" + this.width + "\n" +
            "CSET register_inputs=false\n" +
            "# END Parameters\n" +
            "GENERATE");
            
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void buildCORE_RAM_HDL() {
        try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this._outputFolder + File.separator + "bram_entity_" + this.ID + ".v")));
        bw.write("`timescale 1ns/1ps\n" +
                    "module bram_entity_" + this.ID + "(\n" +
                    "addr,\n" + "clk,\n" + "dout,\n"  + "en);\n" +
                    "input [7 : 0] addr;\n" +
                    "input clk;\n" +
                    "output [" + (this.width-1) + " : 0] dout;\n" +
                    "input en;\n" +
                    "// synopsys translate_off\n" +
                    "\tBLKMEMSP_V6_2 #(\n" +
                    "\t8,	// c_addr_width\n" +
                    "\t\"0\",	// c_default_data\n" +
                    "\t256,	// c_depth\n" +
                    "\t0,	// c_enable_rlocs\n" +
                    "\t0,	// c_has_default_data\n" +
                    "\t0,	// c_has_din\n" +
                    "\t1,	// c_has_en\n" +
                    "\t0,	// c_has_limit_data_pitch\n" +
                    "\t0,	// c_has_nd\n" +
                    "\t0,	// c_has_rdy\n" +
                    "\t0,	// c_has_rfd\n" +
                    "\t0,	// c_has_sinit\n" +
                    "\t0,	// c_has_we\n" +
                    "\t18,	// c_limit_data_pitch\n" +
                    "\t\"bram_entity_0.mif\",	// c_mem_init_file\n" +
                    "\t0,	// c_pipe_stages\n" +
                    "\t0,	// c_reg_inputs\n" +
                    "\t\"0\",	// c_sinit_value\n" +
                    "\t" + this.width + ",	// c_width\n" +
                    "\t0,	// c_write_mode\n" +
                    "\t\"0\",	// c_ybottom_addr" +
                    "\t1,	// c_yclk_is_rising\n" +
                    "\t1,	// c_yen_is_high\n" +
                    "\t\"hierarchy1\",	// c_yhierarchy\n" +
                    "\t0,	// c_ymake_bmm\n" +
                    "\t\"512x36\",	// c_yprimitive_type\n" +
                    "\t1,	// c_ysinit_is_high\n" +
                    "\t\"1024\",	// c_ytop_addr\n" +
                    "\t1,	// c_yuse_single_primitive\n" +
                    "\t1,	// c_ywe_is_high\n" +
                    "\t1)	// c_yydisable_warnings\n" +
                    "\t\tinst (\n" +
                            "\t\t.ADDR(addr),\n" +
                            "\t\t.CLK(clk),\n" +
                            "\t\t.DOUT(dout),\n" +
                            "\t\t.EN(en),\n" +
                            "\t\t.DIN(),\n" +
                            "\t\t.ND(),\n" +
                            "\t\t.RFD(),\n" +
                            "\t\t.RDY(),\n" +
                            "\t\t.SINIT(),\n" +
                            "\t\t.WE());\n" +
                            "endmodule\n");

// synopsys translate_on

// FPGA Express black box declaration
// synopsys attribute fpga_dont_touch "true"
// synthesis attribute fpga_dont_touch of bram_entity_0 is "true"

// XST black box declaration
// box_type "black_box"
// synthesis attribute box_type of bram_entity_0 is "black_box"
        bw.flush();
        bw.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void buildTestBench() {
        try {
             BufferedWriter bw = new BufferedWriter(new FileWriter(new File(this._outputFolder + File.separator + "BRAM_" + this.ID + "_tb.v")));
             bw.write("`timescale 1ns/1ps\n" +
                       "module bram_test_tb_v;\n" +
                        "// Inputs\n" +
                       "\treg clk;\n" +
                        "\treg sod;\n" +
                        "\treg en;\n" +
                        "\treg [7:0] char;\n" +
                        "// Outputs\n" +
                        "\twire [" + (this.engineList.size() - 1) +":0] out;\n" +

                        "// Instantiate the Unit Under Test (UUT)\n" +
                        "\tBRAM_" + this.ID + " uut (\n" +
                                "\t\t.out(out),\n" +
                                "\t\t.clk(clk),\n" +
                                "\t\t.sod(sod),\n" +
                                "\t\t.en(en),\n" +
                                "\t\t.char(char)\n" +
                                "\t\t);\n" +

                                "\tinitial begin\n" +
                                 "       // Initialize Inputs\n" +
                                        "\t\tclk = 0;\n" +
                                        "\t\tsod = 1;\n" +
                                        "\t\ten = 1;\n" +
                                        "\t\tchar = 0;" +

                                        "\t// Wait 100 ns for global reset to finish\n" +
                                        "\t\t#100;\n" +

                                        "\t// Add stimulus here\n" +
                                "\t\tend\n" +
                                "\tinitial begin\n");
                         for (int i = 0; i < this.engineList.size();i++) {
                             ReEngine temp = this.engineList.get(i);
                             bw.write("//" + temp.rule.getPattern()+ "..." + temp.rule.getModifier() + ";\n");
                         }

                        bw.write("\t\t#20 sod = 0;\n");
                         for (int i = 0; i < this.engineList.size();i++) {
                             ReEngine temp = this.engineList.get(i);
                             for (int j = 0; j < temp.rule.testPartten.length(); j++)
                                bw.write ("\t\t#20 char = " + ((int) temp.rule.testPartten.charAt(j)) + ";\n");

                        }

                    bw.write("\tend\n" +
                    "\tinitial begin\n" +
                            "\t\t#10 clk = ~clk;\n" +
                            "\t\tforever #10 clk = ~clk;\n" +
                    "\tend\n" +

                    "\tinitial #100000 $finish;\n" +
                    "endmodule\n" );

             bw.flush();
             bw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void buildNecessaryFiles(){
        this.buildHDL();
        this.buildCOE();
        //this.buildCORE_RAM_HDL();
        this.buildXCO();
        this.buildTestBench();
    }
}


