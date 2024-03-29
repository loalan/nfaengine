/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import VRTSignature.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author heckarim
 */
/* Script
I,General:
+,Số rule active, inactive, trong từng rule Set và tổng cộng. (trừ tập deleted.rules)
+, Số rule trong tập deleted.rules.

II,Content:
+, Số lượng rule có option content và không có option content.
 *, trên từng rule set và tổng cộng. ghi chung vào một file dạng:
<ruleset> <tab> <rule have content> <tab> <rule no content>
 *, các rule không có option content ghi ra chung 1 file (toàn bộ rule going như luc chưa parse).
+, Số lượng content trước và sau khi bị reduce.
 *,Trên mỗi tập rule, và tổng cộng, ghi trong 1 file:
<ruleset> <tab> <no content before reduce> <tab> < no content after reduce>
+, Chiều dài content, số lượng content ứng với mỗi chiều dài. (thực hiện trên tập content đã reduce), cái này đã làm rồi nhưng thêm phần gộp cả inactive và active lại.

+, Thống kê số lượng các modify option trong bảng 3.5, dựa trên hai thông số:
 *. Số các option này.
 *. Số các rule chứa option này (nếu một rule có nhiều content sẽ chứa nhiều option này, chỉ tính là 1).
 *. Lưu ra file cũng có dạng:
<option> <tab> <số option> <tab> <số rule>

+, thống kê các option trong bảng 3.6.
 *. Số các rule có chứa option này.
 *. Lưu ra file có dạng:
<option> <tab> <số rule>

 *. Trích xuất:
+, non content rule lưu vào mỗi file
+, rule chi duy nhat content
 *
 * content, uricontent, pcre, content and pcre, content nopcre, pcre no content, nopcre no content no uri
 */
public class Statistic0 {

    String currentDir = System.getProperty("user.dir");
    String outputfolder = currentDir + File.separator + "output.2.9" + File.separator;
    RuleDatabase db;


    public static void main(String[] args){
        new Statistic0(null).action();
    }

    private Statistic0(Object object) {
        //...
    }


    private void action() {
        RuleDatabase rd = new RuleDatabase();
        String outFolder = System.getProperty("user.dir")+ File.separator + "output.2.9" + File.separator;
        String ruleFolder = System.getProperty("user.dir")+ File.separator + "rules.2.9" + File.separator;
        this.outputfolder = outFolder;
        rd.setRuleDir(outFolder);
        rd.setRuleDir(ruleFolder);
        rd.buildDatabase();
        //rd.print4Test();
        this.db = rd;
        this.DoScript();
    }

    public void DoScript() {
        this.DoGeneral();
        this.DoContent();
        this.DoPcre();
    }

    public void DoGeneral() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfolder + "General.st0"));
            bw.write("Malicious Rules: " + db.lstSnortRuleSet.size() + "\n");
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                bw.write(db.lstSnortRuleSet.get(i).name + ", ");
            }
            bw.write("\n");
            // bw.write("Deleted Rules: " + db.rsDeleted.lstRuleInactive.size() + "\n");
            bw.write("\n\n");
            bw.write("<RuleSet>\t<Active>\t<inactive>\t<total>\n");
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write((i + 1) + "\t" + rs.name.replaceAll(".rules", "") + "\t" + rs.lstRuleActive.size() + "\t" + rs.lstRuleInactive.size() + "\t" + rs.lstRuleAll.size() + "\n");
            }

            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(Statistic0.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void DoContent() {
        try {
            OptionMask mask;
            int sum;

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule don't have content and uricontent and pcre
            //create mask
            mask = new OptionMask();
            mask.SetForbid("content");
            mask.SetForbid("uricontent");
            mask.SetForbid("pcre");
            String result1 = "Result: rule just don't have";
            sum = 0;
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfolder + "R0con0uri0pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule just have pcre, no content, no uricontet;
            //write to file use Refere References.WritePcreToFile
            //create mask
            mask = new OptionMask();
            mask.SetForbid("content");
            mask.SetForbid("uricontent");
            mask.SetPermit("pcre");
            LinkedList<RuleComponent> lwrite2file = new LinkedList<RuleComponent>();
            result1 = "Result: all rule just have pcre, no content, no uricontet;";
            sum = 0;
            bw = new BufferedWriter(new FileWriter(outputfolder + "R0con0uri1pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                        lwrite2file.add(rs.lstRuleAll.get(j));
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            References.WritePcreToFileRuleComponent(lwrite2file, outputfolder + "onlypcre.ref");
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule have pcre don't care content or uricontent
            //create mask
            mask = new OptionMask();
            mask.SetPermit("pcre");
            result1 = "Result: rule have pcre;";
            sum = 0;
            bw = new BufferedWriter(new FileWriter(outputfolder + "R_con_uri1pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule have Uricontent don't care pcre and contetn
            //create mask
            mask = new OptionMask();
            mask.SetPermit("uricontent");
            result1 = "Result: rule have uricontent;";
            sum = 0;
            bw = new BufferedWriter(new FileWriter(outputfolder + "R_con1uri_pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule have content don't care content or uricontent
            //create mask
            mask = new OptionMask();
            mask.SetPermit("content");
            result1 = "Result: rule have content;\n";
            sum = 0;
            bw = new BufferedWriter(new FileWriter(outputfolder + "R1con_uri_pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule have content and no pcre and no uricontent
            //create mask
            mask = new OptionMask();
            mask.SetForbid("pcre");
            mask.SetForbid("uricontent");
            mask.SetPermit("content");
            result1 = "Result: rule have content , no pcre no uriocntent;\n";
            sum = 0;
            bw = new BufferedWriter(new FileWriter(outputfolder + "R1con0uri0pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //all rule have pcre and content no uricontent
            //create mask
            mask = new OptionMask();
            mask.SetPermit("pcre");
            mask.SetPermit("content");
            mask.SetForbid("uricontent");
            result1 = "Result: rule have pcre and content no uriocntent;\n";
            sum = 0;
            bw = new BufferedWriter(new FileWriter(outputfolder + "R1con0uri1pcre.st0"));
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (!rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") && !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        bw.write(rs.lstRuleAll.get(j).value + "\n");
                        count++;
                    }
                }
                //insert in to result1
                result1 += rs.name + "\t" + count + "\n";
                sum = sum + count;
            }
            result1 += "Sum" + "\t" + sum + "\n";
            bw.write("\n" + result1);
            bw.flush();
            bw.close();
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------

            //print all rule just have one content, no uricontent,
            //set mask
            mask = new OptionMask();
            mask.SetPermit("content");
            mask.SetForbid("uricontent");
            bw = new BufferedWriter(new FileWriter(outputfolder + "Rule1Content.st0"));
            String result2 = "\n Result: Rule just have 1 content and no uricontent\n";
            LinkedList<RuleComponent> l1cNp2file = new LinkedList<RuleComponent>();
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    //if (rs.lstRuleAll.get(j).ruleStatus.isHaveOption("content") &&
                    //        rs.lstRuleAll.get(j).ruleStatus.GetOption("content").count == 1 &&
                    //        !rs.lstRuleAll.get(j).ruleStatus.isHaveOption("uricontent")) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        //have one content
                        if (rs.lstRuleAll.get(j).ruleStatus.GetOptionStatus("content").count == 1) {
                            bw.write(rs.lstRuleAll.get(j).value + "\n");
                            bw.write(rs.lstRuleAll.get(j).GetOpContent().get(0).toString() + "\n");
                            count++;
                            l1cNp2file.add(rs.lstRuleAll.get(j));
                        }
                    }
                }
                References.WritePcreToFileRuleComponent(l1cNp2file, outputfolder + "1ContentNpcre.ref");
                result2 += rs.name + "\t" + count + "\n";
            }
            bw.write("\n" + result2);
            bw.flush();
            bw.close();


//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------

            //print all rule just have one content, no uricontent, no modifier option execept nocase
            //incluse count nocase and count pcre;
            mask = new OptionMask();
            mask.SetForbid(References._opContentModifier);
            mask.SetDontCare("nocase");
            mask.SetPermit("content");
            mask.SetForbid("uricontent");
            mask.SetDontCare("rawbytes");

            bw = new BufferedWriter(new FileWriter(outputfolder + "SimpleRulewithPcre.st0"));
            String result3 = "\n Result: Rule just have 1 content and no uricontent\n";
            result3 += "<ruleset>" + "\t" + "<no Rule>" + "\n";
            sum = 0;
            int countPcre = 0;
            int countNocase = 0;
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        //have one content
                        if (rs.lstRuleAll.get(j).ruleStatus.GetOptionStatus("content").count == 1) {
                            if (rs.lstRuleAll.get(j).ruleStatus.isHaveOption("pcre")) {
                                countPcre++;
                            }
                            if (rs.lstRuleAll.get(j).ruleStatus.isHaveOption("nocase")) {
                                countNocase++;
                            }
                            bw.write(rs.lstRuleAll.get(j).value + "\n");
                            // bw.write(rs.lstRuleAll.get(j).GetOpContent().get(0).toString() + "\n");
                            count++;
                        }
                    }
                }
                result3 += rs.name + "\t" + count + "\n";
                sum += count;
            }
            result3 += "sum" + "\t" + sum + "\n";
            result3 += "nocase" + "\t" + countNocase + "\n";
            result3 += "pcre" + "\t" + countPcre + "\n";

            bw.write("\n" + result3);

            bw.flush();
            bw.close();

//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------

            //print all rule just have one content, no uricontent, no modifier option execept nocase, no pcre
            //include counting nocase;
            mask = new OptionMask();
            mask.SetForbid(References._opContentModifier);
            mask.SetDontCare("nocase");
            mask.SetPermit("content");
            mask.SetForbid("uricontent");
            mask.SetDontCare("rawbytes");
            mask.SetDontCare("fast_pattern");
            mask.SetForbid("pcre");

            bw = new BufferedWriter(new FileWriter(outputfolder + "SimpleRuleNoPcre.st0"));
            result3 = "\n Result: Rule just have 1 content and no uricontent and no pcre \n";
            result3 += "<ruleset>" + "\t" + "<no Rule>" + "\n";
            sum = 0;
            countNocase = 0;
            for (int i = 0; i < db.lstSnortRuleSet.size(); i++) {
                int count = 0;
                RuleSet rs = db.lstSnortRuleSet.get(i);
                bw.write("\n#" + rs.name + "\n");
                for (int j = 0; j < rs.lstRuleAll.size(); j++) {
                    if (rs.lstRuleAll.get(j).ruleStatus.CompareMask(mask)) {
                        //have one content
                        if (rs.lstRuleAll.get(j).ruleStatus.GetOptionStatus("content").count == 1) {
                            if (rs.lstRuleAll.get(j).ruleStatus.isHaveOption("nocase")) {
                                countNocase++;
                            }
                            bw.write(rs.lstRuleAll.get(j).value + "\n");
                            // bw.write(rs.lstRuleAll.get(j).GetOpContent().get(0).toString() + "\n");
                            count++;
                        }
                    }
                }
                result3 += rs.name + "\t" + count + "\n";
                sum += count;
            }
            result3 += "sum" + "\t" + sum + "\n";
            result3 += "nocase" + "\t" + countNocase + "\n";

            bw.write("\n" + result3);

            bw.flush();
            bw.close();


//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
            //count number of rule have modifier option and number of its optiosn
            int[] opModRuleActive = new int[References._opContentModifier.length];
            int[] opModCountActive = new int[References._opContentModifier.length];
            int conruleActive = 0; //number of rule have content;
            int concountActive = 0; // number of options content;

            int[] opModRuleIn = new int[References._opContentModifier.length];
            int[] opModCountIn = new int[References._opContentModifier.length];
            int conruleIn = 0; //number of rule have content;
            int concountIn = 0; // number of options content;

            //first is active rule
            for (int i = 0; i < db.lstRuleActive.size(); i++) {
                if (db.lstRuleActive.get(i).ruleStatus.isHaveOption("content")) {
                    conruleActive++;
                    concountActive += db.lstRuleActive.get(i).ruleStatus.GetOptionStatus("content").count;
                }
                for (int j = 0; j < References._opContentModifier.length; j++) {

                    if (db.lstRuleActive.get(i).ruleStatus.isHaveOption(References._opContentModifier[j])) {
                        opModRuleActive[j]++;
                        opModCountActive[j] += db.lstRuleActive.get(i).ruleStatus.GetOptionStatus(References._opContentModifier[j]).count;
                    }
                }
            }
            //now is inactive
            for (int i = 0; i < db.lstRuleInactive.size(); i++) {
                if (db.lstRuleInactive.get(i).ruleStatus.isHaveOption("content")) {
                    conruleIn++;
                    concountIn += db.lstRuleInactive.get(i).ruleStatus.GetOptionStatus("content").count;
                }
                for (int j = 0; j < References._opContentModifier.length; j++) {

                    if (db.lstRuleInactive.get(i).ruleStatus.isHaveOption(References._opContentModifier[j])) {
                        opModRuleIn[j]++;
                        opModCountIn[j] += db.lstRuleInactive.get(i).ruleStatus.GetOptionStatus(References._opContentModifier[j]).count;
                    }
                }
            }
            bw = new BufferedWriter(new FileWriter(outputfolder + "ConNmodifierCount.st0"));
            bw.write("REsult: \n");
            bw.write("Option\tNoActive\tNoOptActive\tNoInAct\tNoOptInact\tNoAll\tNoOptAll\n");
            bw.write("content" + "\t" + conruleActive + "\t" + concountActive + "\t" + conruleIn + "\t" +
                    concountIn + "\t" + (conruleActive + conruleIn) + "\t" + (concountActive + concountIn) + "\n");
            for (int i = 0; i < opModRuleActive.length; i++) {
                bw.write(References._opContentModifier[i] + "\t" + opModRuleActive[i] + "\t" +
                        opModCountActive[i] + "\t" + opModRuleIn[i] + "\t" + opModCountIn[i] + "\t" +
                        (opModRuleActive[i] + opModRuleIn[i]) + "\t" + (opModCountActive[i] + opModCountIn[i]) + "\n");
            }
            bw.flush();
            bw.close();
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------


            //Count number of rule wich have 1,2, ...n content
            //don't care ruleset;
            int[] noContent = new int[20];
            //set all to 0
            for (int i = 0; i < noContent.length; i++) {
                noContent[i] = 0;
            }
            //set value for noContent[]
            for (int i = 0; i < db.lstRuleAll.size(); i++) {
                if (db.lstRuleAll.get(i).ruleStatus.isHaveOption("content")) {
                    noContent[db.lstRuleAll.get(i).ruleStatus.GetOptionStatus("content").count]++;
                }
            }
            bw = new BufferedWriter(new FileWriter(outputfolder + "ContentRuleCount.st0"));
            bw.write("Result: \n");
            for (int i = 0; i < noContent.length; i++) {
                bw.write(i + "\t" + noContent[i] + "\n");
            }
            bw.flush();
            bw.close();

        //print all rule don't have content and uricontent
        } catch (IOException ex) {
            Logger.getLogger(Statistic0.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void DoPcre() {
    }
}
