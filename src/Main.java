import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {
    static ArrayList<String> IM = new ArrayList<>();
    static int pc = 0;
    static HashMap<String, Integer> var_map = new HashMap<>();
    static int stack[] = new int[100000];
    static int sp = 0;
    static int memory4int[] = new int[100000];
    static int gp = 0;
    static HashMap<String, Integer> label2pc = new HashMap<>();
    static int ret = 0;
    static int ra = 0;
    static final int zero = 0;
    static int para = 0;
    static String console = "";

    public static void main(String[] args) {
        copyIRFile();
        readOrders();
//        printOrders();
        loadLabel();
        parseOrders();
        console_log();
    }

    public static void parseOrders() {
        String arrSplit = "\\[|]";
        String ConstVarDecl = "const int .+";
        String ConstArrDecl = "const arr .+";
        String ArrDecl = "arr int .+";
        String VarDeclInited = "int [^=]+=.+";
        String VarDecl = "int [^=]+";
        String FuncDecl = "^\\^func .*";
        String FuncCall = "^\\^call func .*";
        String StackPush = "^\\^push .*";
        String StackPop = "^\\^pop .*";
        String ArrAssign = "[^\\[\\]]+\\[[^]]+] = .+";
        String NormalVarAssign = "[^=\\[\\]]+=.+";
        String tempVar = "%[0-9]+";
        String hasArr = "[^\\[\\]]+\\[[^]]+]";
        String label = "label.+";
        String JAL = "^jal .*";
        String JR = "^jr .*";
        String J = "^j label.*";
        String BEQ = "^beq .*";
        String ParaInt = "^para int .*";
        String PrintCall = "^\\^call print .*";
        String GetIntCall = "^\\^call GETINT.*";

        Scanner scanner = new Scanner(System.in);

        String order;
        System.out.println(">>> parsing orders...");

        while (pc < IM.size() && pc >= 0) {
            order = IM.get(pc);
            String[] buf = order.split(" ");
            if (order.equals("exit")) {
                System.out.println(">>> run finished!");
                return;
            } else if (Pattern.matches(BEQ, order)) {
//                System.out.println(">>> BEQ");
                String arg1 = buf[1];
                String arg2 = buf[2];
                String labelToGo = buf[3];
                int arg1Value, arg2Value;
                if (arg1.equals("$0")) {
                    arg1Value = zero;
                } else if (isInteger(arg1)) {
                    arg1Value = Integer.parseInt(arg1);
                } else {
                    arg1Value = var_map.get(arg1);
                }
                if (arg2.equals("$0")) {
                    arg2Value = zero;
                } else if (isInteger(arg2)) {
                    arg2Value = Integer.parseInt(arg2);
                } else {
                    arg2Value = var_map.get(arg2);
                }
                int label_pc = label2pc.get(labelToGo);
                if (arg1Value == arg2Value) {
                    pc = label_pc;
                    $pcAssign(pc);
                }
            } else if (Pattern.matches(JAL, order)) {
//                System.out.println("> JAL");
                String labelToGo = buf[1];
                int label_pc = label2pc.get(labelToGo);
                ra = pc;
                $raAssign(ra);
                pc = label_pc;
                $pcAssign(pc);
            } else if(Pattern.matches(JR, order)) {
//                System.out.println("> JR");
                String reg = buf[1];
                if (reg.equals("ra")) {
                    pc = ra;
                    $pcAssign(pc);
                }
            } else if(Pattern.matches(J, order)) {
//                System.out.println("> J");
                String labelToGo = buf[1];
                pc = label2pc.get(labelToGo);
                $pcAssign(pc);
            } else if (Pattern.matches(ConstVarDecl, order)) {
//                System.out.println("> ConstVarDecl");
                var_map.put(buf[2], Integer.getInteger(buf[4]));
            } else if (Pattern.matches(ConstArrDecl, order)) {
//                System.out.println("> ConstArrDecl");
                String[] buf2 = buf[2].split(arrSplit);
                String arr = buf2[0];
                String sizeStr = buf2[1];
                int size = Integer.parseInt(sizeStr);
                var_map.put(arr, gp);
                gp += size;
                $gpAssign(gp);
            } else if (Pattern.matches(VarDeclInited, order)) {
//                System.out.println("> VarDeclInited");
                String var = buf[1];
                String rVal = buf[3];
                if (isInteger(rVal)) {
                    var_map.put(var, Integer.parseInt(rVal));
                } else {
                    int rValValue = var_map.get(rVal);
                    var_map.put(var, rValValue);
                }
            } else if (Pattern.matches(VarDecl, order)) {
//                System.out.println("> VarDeclUninited");
                String var = buf[1];
                var_map.put(var, zero);
            } else if (Pattern.matches(ArrDecl, order)) {
//                System.out.println("> ArrDecl");
                String[] buf2 = buf[2].split(arrSplit);
                String arr = buf2[0];
                String sizeStr = buf2[1];
                int size = Integer.parseInt(sizeStr);
                var_map.put(arr, gp);
                gp += size;
                $gpAssign(gp);
            } else if (Pattern.matches(FuncDecl, order)) {
//                System.out.println("> FuncDecl");
                para = Integer.parseInt(buf[3]);
                $paraAssign(para);
            } else if(Pattern.matches(ParaInt, order)) {
//                System.out.println("> ParaInt");
                String paraName = buf[2];
                int paraValue = stack[sp - para];
                var_map.put(paraName, paraValue);
                para--;
                $paraAssign(para);
            } else if (Pattern.matches(FuncCall, order)) {
                int a = 10;
                // do nothing
//                System.out.println("> FuncCall");
            } else if (Pattern.matches(StackPush, order)) {
//                System.out.println("> StackPush");
                String arg = buf[1];
                if (arg.equals("ra")) {
                    stack[sp++] = ra;
                } else if (isInteger(arg)) {
                    int value = Integer.parseInt(arg);
                    stack[sp++] = value;
                } else {
                    int value;
                    if (var_map.get(arg) == null) {
                        value = 0;
                    } else {
                        value = var_map.get(arg);
                    }
                    stack[sp++] = value;
                }
                $spAssign(sp);
            } else if (Pattern.matches(StackPop, order)) {
//                System.out.println("> StackPop");
                sp--;
                if (buf[1].equals("ra")) {
                    ra = stack[sp];
                    $raAssign(ra);
                }else if (!buf[1].equals("$0")) {
                    String tmp = buf[1];
                    var_map.put(tmp, stack[sp]);
                }
                $spAssign(sp);
            } else if (Pattern.matches(label, order)) {
//                System.out.println("> label");
//                label2pc.put(buf[0], pc);
            } else if(Pattern.matches(GetIntCall, order)) {
//                System.out.println("> GetIntCall");
                ret = scanner.nextInt();
                $retAssign(ret);
            } else if (Pattern.matches(PrintCall, order)) {
                String[] buf2 = order.split("\\$");
                if (buf2.length > 1) {
//                    System.out.println("> PrintFormatString");
                    String processed = buf2[1];
                    System.out.println(processed);
                    console += processed;
                } else {
                    int value = var_map.get(buf[2]);
                    System.out.println(value);
                    console += value;
                }
            } else if (Pattern.matches(ArrAssign, order)) {
//                System.out.println("> ArrAssign");
                String rVal = buf[2];
                String[] buf2 = buf[0].split(arrSplit);
                String arr = buf2[0];
                String offset = buf2[1];
                int off;
                if (isInteger(offset)) {
                    off = Integer.parseInt(offset);
                } else {
                    off = var_map.get(offset);
                }
                int arrGp = var_map.get(arr);
                if (isInteger(rVal)) {
                    memory4int[arrGp + off] = Integer.parseInt(rVal);
                } else if (rVal.equals("^GETINT_RET")) {
                    memory4int[arrGp + off] = ret;
                } else {
                    int rValValue = var_map.get(rVal);
                    memory4int[arrGp + off] = rValValue;
                }
            } else if (Pattern.matches(NormalVarAssign, order)) {
                if (buf.length == 3) {
                    if (buf[2].equals("^GETINT_RET")) {
//                        System.out.println("> GETINT_RET");
                        String lVal = buf[0];
                        var_map.put(lVal, ret);
                    } else if (Pattern.matches(hasArr, buf[2])) {
//                        System.out.println("> arrGet");
                        String[] buf2 = buf[2].split(arrSplit);
                        String lVal = buf[0];
                        String arr = buf2[0];
                        String offset = buf2[1];
                        int offValue;
                        int arrGp = var_map.get(arr);
                        if (isInteger(offset)) {
                            offValue = Integer.parseInt(offset);
                        } else {
                            offValue = var_map.get(offset);
                        }
                        int rValValue = memory4int[arrGp + offValue];
                        var_map.put(lVal, rValValue);
                    } else {
//                        System.out.println("> NormalVarAssign");
                        String lVal = buf[0];
                        String rVal = buf[2];
                        if (lVal.equals("^ret")) {
                            if (isInteger(rVal)) {
                                ret = Integer.parseInt(rVal);
                                $retAssign(ret);
                            } else {
                                ret = var_map.get(rVal);
                                $retAssign(ret);
                            }
                        } else {
                            if (isInteger(rVal)) {
                                int rValValue = Integer.parseInt(rVal);
                                var_map.put(lVal, rValValue);
                            } else {
                                int rValValue;
                                if (rVal.equals("^ret")) {
                                    rValValue = ret;
                                } else {
                                    rValValue = var_map.get(rVal);
                                }
                                var_map.put(lVal, rValValue);
                            }
                        }
                    }
                } else if (buf.length == 4) {
//                    System.out.println("> OneOpRelVarAssign");
                    String lVal = buf[0];
                    String op = buf[2];
                    String rVal = buf[3];
                    int rValValue;
                    if (isInteger(rVal)) {
                        rValValue = Integer.parseInt(rVal);
                    } else {
                        rValValue = var_map.get(rVal);
                    }
                    if (op.equals("-")) {
                        var_map.put(lVal, -rValValue);
                    } else if (op.equals("!")) {
                        if (rValValue != 0) {
                            var_map.put(lVal, 0);
                        } else {
                            var_map.put(lVal, 1);
                        }
                    } else {
                        var_map.put(lVal, rValValue);
                    }
                } else if (buf.length == 5) {
//                    System.out.println("> TwoOpRelVarAssign");
                    String lVal = buf[0];
                    String arg1 = buf[2];
                    String arg2 = buf[4];
                    String op = buf[3];
                    int arg1Value, arg2Value;
                    if (isInteger(arg1)) {
                        arg1Value = Integer.parseInt(arg1);
                    } else {
                        arg1Value = var_map.get(arg1);
                    }
                    if (isInteger(arg2)) {
                        arg2Value = Integer.parseInt(arg2);
                    } else {
                        arg2Value = var_map.get(arg2);
                    }
                    if (op.equals("+")) {
                        var_map.put(lVal, arg1Value + arg2Value);
                    } else if (op.equals("-")) {
                        var_map.put(lVal, arg1Value - arg2Value);
                    } else if (op.equals("*")) {
                        var_map.put(lVal, arg1Value * arg2Value);
                    } else if (op.equals("/")) {
                        var_map.put(lVal, arg1Value / arg2Value);
                    } else if (op.equals("%")) {
                        var_map.put(lVal, arg1Value % arg2Value);
                    } else if (op.equals(">")) {
                        int value = arg1Value > arg2Value ? 1 : 0;
                        var_map.put(lVal, value);
                    } else if (op.equals("<")) {
                        int value = arg1Value < arg2Value ? 1 : 0;
                        var_map.put(lVal, value);
                    } else if (op.equals(">=")) {
                        int value = arg1Value >= arg2Value ? 1 : 0;
                        var_map.put(lVal, value);
                    } else if (op.equals("<=")) {
                        int value = arg1Value <= arg2Value ? 1 : 0;
                        var_map.put(lVal, value);
                    } else if (op.equals("==")) {
                        int value = arg1Value == arg2Value ? 1 : 0;
                        var_map.put(lVal, value);
                    } else if (op.equals("!=")) {
                        int value = arg1Value != arg2Value ? 1 : 0;
                        var_map.put(lVal, value);
                    }
                }
            } else {
                System.out.println("> undefined");
            }
            pc++;
        }
    }

    public static void console_log() {
        try {
            File file =new File("./console.txt");
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getName());
            console = console.replaceAll("\\\\n", "\n");
            fileWriter.write(console);
            fileWriter.close();
            System.out.println(">>> console:");
            System.out.format(console);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void $paraAssign(int value) {
        System.out.println(">> $para <- " + value);
    }

    public static void $pcAssign(int value) {
        System.out.println(">> $pc <- " + value);
    }

    public static void $raAssign(int value) {
        System.out.println(">> $ra <- " + value);
    }

    public static void $gpAssign(int value) {
        System.out.println(">> $gp <- " + value);
    }

    public static void $spAssign(int value) {
        System.out.println(">> $sp <- " + value);
    }

    public static void $retAssign(int value) {
        System.out.println(">> $ret <- " + value);
    }

    public static void printOrders() {
        System.out.println(">>> print orders...");
        for (String s : IM) {
            System.out.println(s);
        }
        System.out.println(">>> print orders success!");
    }

    public static void loadLabel() {
        String label = "label.+";
        int pc_tmp = 0;
        for (String order : IM) {
            if (Pattern.matches(label, order)) {
                String buf[] = order.split(" ");
                label2pc.put(buf[0], pc_tmp);
            }
            pc_tmp++;
        }
    }

    public static void readOrders() {
        File IRFile = new File("IRRes.txt");
        InputStreamReader inputReader = null;
        try {
            inputReader = new InputStreamReader(new FileInputStream(IRFile), "UTF-8");
            BufferedReader bf = new BufferedReader(inputReader);
            String order = bf.readLine();
            while (order != null) {
                if (order.isEmpty()) continue;
                IM.add(order);
                order = bf.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyIRFile() {
        Runtime run = Runtime.getRuntime();
        try {
            System.out.println(">>> getting IR file...");
            String[] cmd = new String[]{"sh", "-c", "cp ~/CLionProjects/Compiler/cmake-build-debug/IRRes.txt ~/IdeaProjects/VirtualMachineForIR/IRRes.txt"};
            Process process = run.exec(cmd);
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }
}
