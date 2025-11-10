package com.tarzan.maxkb4j.core.tool;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Divination {

    // 八宫名称
    private static final String[] GONG_NAMES = {
            "乾宫", "兑宫", "离宫", "震宫", "巽宫", "坎宫", "艮宫", "坤宫"
    };

    private static final String[][] BA_GONG_GUA_NAMES = {
            // 乾宫（金）
            {"乾为天", "天风姤", "天山遁", "天地否", "风地观", "山地剥", "火地晋", "火天大有"},
            // 兑宫（金）
            {"兑为泽", "泽水困", "泽地萃", "泽山咸", "水山蹇", "地山谦", "雷山小过", "雷泽归妹"},
            // 离宫（火）
            {"离为火", "火山旅", "火风鼎", "火水未济", "山水蒙", "艮山谦", "风山渐", "风水涣"},
            // 震宫（木）
            {"震为雷", "雷地豫", "雷水解", "雷风恒", "地风升", "水风井", "泽风大过", "泽雷随"},
            // 巽宫（木）
            {"巽为风", "风天小畜", "风火家人", "风雷益", "天雷无妄", "火雷噬嗑", "山雷颐", "山风蛊"},
            // 坎宫（水）
            {"坎为水", "水泽节", "水雷屯", "水火既济", "泽火革", "雷火丰", "地火明夷", "地水师"},
            // 艮宫（土）
            {"艮为山", "山火贲", "山天大畜", "山泽损", "火泽睽", "天泽履", "风泽中孚", "风山渐"},
            // 坤宫（土）
            {"坤为地", "地雷复", "地泽临", "地天泰", "雷天大壮", "泽天夬", "水天需", "水地比"}
    };

    private static final String[][] BA_GONG_GUA = {
            // 乾宫（金）
            {"111111", "011111", "001111", "000111", "000011", "000001", "000101", "111101"},
            // 兑宫（金）
            {"110110", "010110", "000110", "001010", "001010", "001000", "001100", "100100"},
            // 离宫（火）
            {"101101", "001101", "011101", "010101", "010001", "010011", "010111", "101111"},
            // 震宫（木）
            {"100100", "000100", "010100", "011100", "011000", "011010", "011110", "100110"},
            // 巽宫（木）
            {"011011", "111011", "101011", "100011", "100111", "100101", "100001", "011001"},
            // 坎宫（水）
            {"010010", "110010", "100010", "101010", "101110", "101100", "101000", "010000"},
            // 艮宫（土）
            {"001001", "101001", "111001", "110001", "110101", "110111", "110011", "001011"},
            // 坤宫（土）
            {"000000", "100000", "110000", "111000", "111100", "111110", "111010", "000010"}
    };



    private static final String[] GONG_FIVE_ELEMENTS = {
            "乾宫:金", "兑宫:金", "离宫:火", "震宫:木", "巽宫:木", "坎宫:水", "艮宫:土", "坤宫:土"
    };

    private static final String[] GONG_DI_ZHI = {
            "乾宫:子寅辰午申戌", "坎宫:寅辰午申戌子", "艮宫:辰午申戌子寅", "震宫:子寅辰午申戌", "巽宫:丑亥酉未巳卯", "离宫:卯丑亥酉未巳", "坤宫:未巳卯丑亥酉", "兑宫:酉未巳卯丑亥"
    };

    private static final String[] DI_ZHI_FIVE_ELEMENTS = {
            "寅卯:木", "巳午:火", "申酉:金", "亥子:水", "辰戌丑未:土"
    };

    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    // 六神顺序：从初爻开始的默认顺序
    private static final String[] LIU_SHEN = {"青龙", "朱雀", "勾陈", "螣蛇", "白虎", "玄武"};
    private static final Map<String, Integer> TIAN_GAN_LIU_SHEN = Map.of(
            "甲",0,
            "乙",0,
            "丙",1,
            "丁",1,
            "戊",2,
            "己",3,
            "庚",4,
            "辛",4,
            "壬",5,
            "癸",5
    );


    public static void main(String[] args) {
        int[] randomNumbers = getRandomNumbers();
        System.out.println(Arrays.toString(randomNumbers));
        System.out.println(toBinary(randomNumbers));
        int[] sixthLines = toSixthLine(randomNumbers);
        System.out.println(Arrays.toString(sixthLines));
        int[] currentHexagram = currentHexagram(sixthLines);
        String benGua = convert(currentHexagram);
        String guaName = getGuaName(benGua);
        String guaGong = getGuaGong(benGua);
        String guaWx = getGuaFiveElement(guaGong);
        String guaDiZhi = getGuaDiZhi(guaGong);
        assert guaDiZhi != null;
        String yaoWxs = getYaoFiveElement(guaDiZhi);
        String guaLiuQin = getGuaLiuQin(guaWx,yaoWxs);
        String tianGan = getCurrentTianGan();
        String guaLiuShen = getGuaLiuShen(tianGan);
        int shiYaoIndex=getShiYaoIndex(benGua);
        System.out.println("本卦：" + benGua
                + " 卦名：" + guaName
                + " 世爻：" + shiYaoIndex
                + " 应爻：" + getYingYaoIndex(shiYaoIndex)
                + " 卦宫：" + guaGong + "(" + guaWx + ")"
                + " 地支：" + guaDiZhi
                + " 五行：" + yaoWxs
                + " 六亲：" + guaLiuQin
                + " 天干：" + tianGan
                + " 六神：" + guaLiuShen);
        //  int[] changeHexagram = changeHexagram(sixthLines);
        //  System.out.println("变卦：" + Arrays.toString(changeHexagram)+"  卦名："+getGuaName(changeHexagram));
    }

    public static int getShiYaoIndex(String gua) {
        Map<String, Integer> map = new HashMap<>();
        for (String[] gong_gua : BA_GONG_GUA) {
            for (int j = 0; j < gong_gua.length; j++) {
                if (j > 6) {
                    map.put(gong_gua[j], 3);
                }else {
                    map.put(gong_gua[j], (j+1));
                }

            }
        }
        return map.get(gua);
    }

    public static String convert(int[] gua) {
        List<String> list = Arrays.stream(gua)
                .boxed()
                .map(String::valueOf)
                .collect(Collectors.toList());
       return String.join("", list);
    }

    public static int getYingYaoIndex(int shiYaoIndex) {
        if (shiYaoIndex >3) {
            return shiYaoIndex -3;
        }
        return shiYaoIndex+3;
    }

    public static String getCurrentTianGan() {
        int currentYear = LocalDate.now().getYear();
        // 1984 年是甲子年，天干索引为 0
        int offset = (currentYear - 1984) % 10;
        if (offset < 0) offset += 10; // 处理负数
        return TIAN_GAN[offset];
    }

    public static String getGuaLiuShen(String tianGan) {
        List<String> list = new ArrayList<>();
        int startIndex = TIAN_GAN_LIU_SHEN.get(tianGan);
        for (int i = 0; i < 6; i++) {
            int shenIndex = (startIndex + i) % 6;
            list.add(LIU_SHEN[shenIndex]);
        }
        return String.join("", list);
    }


    public static String getGuaLiuQin(String guaWx, String yaoWxs) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < yaoWxs.length(); i++) {
            char yaoWx = yaoWxs.charAt(i);
            String liuQin = getYaoLiuQin(guaWx, String.valueOf(yaoWx));
            list.add(liuQin);
        }
        return String.join("", list);
    }
    public static String getYaoLiuQin(String guaWx, String yaoWx) {
        if (Objects.equals(guaWx, yaoWx)) {
            return LiuQin.BROTHERS; // 同我者为兄弟
        }
        switch (guaWx) {
            case "木":
                if (Objects.equals(yaoWx, WuXing.WATER)) return LiuQin.PARENTS;      // 水生木
                if (Objects.equals(yaoWx, WuXing.FIRE)) return LiuQin.CHILDREN;      // 木生火
                if (Objects.equals(yaoWx, WuXing.METAL)) return LiuQin.OFFICIAL_GHOST; // 金克木
                if (Objects.equals(yaoWx, WuXing.EARTH)) return LiuQin.SPOUSE_WEALTH; // 木克土
                break;
            case "火":
                if (Objects.equals(yaoWx, WuXing.WOOD)) return LiuQin.PARENTS;        // 木生火
                if (Objects.equals(yaoWx, WuXing.EARTH)) return LiuQin.CHILDREN;      // 火生土
                if (Objects.equals(yaoWx, WuXing.WATER)) return LiuQin.OFFICIAL_GHOST; // 水克火
                if (Objects.equals(yaoWx, WuXing.METAL)) return LiuQin.SPOUSE_WEALTH; // 火克金
                break;
            case "土":
                if (Objects.equals(yaoWx, WuXing.FIRE)) return LiuQin.PARENTS;        // 火生土
                if (Objects.equals(yaoWx, WuXing.METAL)) return LiuQin.CHILDREN;      // 土生金
                if (Objects.equals(yaoWx, WuXing.WOOD)) return LiuQin.OFFICIAL_GHOST; // 木克土
                if (Objects.equals(yaoWx, WuXing.WATER)) return LiuQin.SPOUSE_WEALTH; // 土克水
                break;
            case "金":
                if (Objects.equals(yaoWx, WuXing.EARTH)) return LiuQin.PARENTS;       // 土生金
                if (Objects.equals(yaoWx, WuXing.WATER)) return LiuQin.CHILDREN;      // 金生水
                if (Objects.equals(yaoWx, WuXing.FIRE)) return LiuQin.OFFICIAL_GHOST; // 火克金
                if (Objects.equals(yaoWx, WuXing.WOOD)) return LiuQin.SPOUSE_WEALTH;  // 金克木
                break;
            case "水":
                if (Objects.equals(yaoWx, WuXing.METAL)) return LiuQin.PARENTS;       // 金生水
                if (Objects.equals(yaoWx, WuXing.WOOD)) return LiuQin.CHILDREN;       // 水生木
                if (Objects.equals(yaoWx, WuXing.EARTH)) return LiuQin.OFFICIAL_GHOST; // 土克水
                if (Objects.equals(yaoWx, WuXing.FIRE)) return LiuQin.SPOUSE_WEALTH;  // 水克火
                break;
        }

        throw new IllegalArgumentException("无效的五行组合: " + guaWx + ", " + yaoWx);
    }


    public static String getGuaFiveElement(String gong) {
        for (String element : GONG_FIVE_ELEMENTS) {
            if (element.contains(gong)) {
                return element.split(":")[1];
            }
        }
        return null;
    }

    public static String getYaoFiveElement(String guaDiZhi) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < guaDiZhi.length(); i++) {
            char diZhi = guaDiZhi.charAt(i);
            for (String element : DI_ZHI_FIVE_ELEMENTS) {
                if (element.contains(Character.toString(diZhi))) {
                    list.add(element.split(":")[1]);
                }
            }
        }
        return String.join("", list);
    }

    public static String getGuaDiZhi(String gong) {
        for (String element : GONG_DI_ZHI) {
            if (element.contains(gong)) {
                return element.split(":")[1];
            }
        }
        return null;
    }

    public static int[] getRandomNumbers() {
        Random random = new Random();
        int[] numbers = new int[6];
        for (int i = 0; i < 6; i++) {
            int randomNumber = random.nextInt(8);
            numbers[i] = randomNumber;
        }
        return numbers;
    }

    public static List<String> toBinary(int[] numbers) {
        List<String> list = new ArrayList<>();
        for (Integer i : numbers) {
            String binary = String.format("%3s", Integer.toBinaryString(i)).replace(' ', '0');
            list.add(binary);
        }
        return list;
    }

    public static int[] toSixthLine(int[] numbers) {
        int[] gua = new int[6];
        for (int i = 0; i < numbers.length; i++) {
            int yao = numbers[i];
            if (yao == 0) {
                gua[i] = 0;
            } else if (List.of(1, 2, 4).contains(yao)) {
                gua[i] = 1;
            } else if (List.of(3, 5, 6).contains(yao)) {
                gua[i] = 2;
            } else if (yao == 7) {
                gua[i] = 3;
            }
        }
        return gua;
    }

    public static int[] currentHexagram(int[] sixthLines) {
        int[] gua = new int[6];
        for (int i = 0; i < sixthLines.length; i++) {
            int yao = sixthLines[i];
            if (yao < 2) {
                gua[i] = 0;
            } else {
                gua[i] = 1;
            }
        }
        return gua;
    }

    public static int[] changeHexagram(int[] sixthLines) {
        int[] gua = new int[6];
        for (int i = 0; i < sixthLines.length; i++) {
            int yao = sixthLines[i];
            if (yao == 3) {
                gua[i] = 0;
            } else if (yao == 2) {
                gua[i] = 1;
            } else if (yao == 1) {
                gua[i] = 0;
            } else if (yao == 0) {
                gua[i] = 1;
            }
        }
        return gua;
    }

    public static String getGuaGong(String gua) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < BA_GONG_GUA.length; i++) {
            String[] gong_gua = BA_GONG_GUA[i];
            for (String _gua : gong_gua) {
                map.put(_gua, i);
            }
        }
        int index = map.get(gua);
        return GONG_NAMES[index];
    }

    public static String getGuaName(String gua) {
        // 定义八卦与二进制表示的映射
        String[] TRIGRAMS = {"坤", "艮", "坎", "巽", "震", "离", "兑", "乾"};
        // 确定内卦和外卦
        int innerGua = getTrigram(gua.substring(0, 3));
        int outerGua = getTrigram(gua.substring( 3));
        // 打印结果
        System.out.println("内卦: " + TRIGRAMS[innerGua] + " 外卦: " + TRIGRAMS[outerGua]);
        for (int i = 0; i < BA_GONG_GUA.length; i++) {
            String[] gong_gua = BA_GONG_GUA[i];
            for (int j = 0; j < gong_gua.length; j++) {
                if (gua.equals(gong_gua[j])){
                    return BA_GONG_GUA_NAMES[i][j];
                }
            }
        }
        return null;
    }

    // 根据三个爻的状态确定是哪个三爻卦
    private static int getTrigram(String binaryStr) {
        return Integer.parseInt(binaryStr, 2);
    }

    private static class WuXing {
        static String WOOD = "木";
        static String FIRE = "火";
        static String EARTH = "土";
        static String METAL = "金";
        static String WATER = "水";
    }

    private static class LiuQin {
        static String PARENTS = "父母";
        static String CHILDREN = "子孙";
        static String OFFICIAL_GHOST = "官鬼";
        static String SPOUSE_WEALTH = "妻财";
        static String BROTHERS = "兄弟";
    }
}
