package com.tarzan.maxkb4j.core.tool;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class Divination {

    // 定义八卦与二进制表示的映射
    static String[] TRIGRAMS = {"坤", "艮", "坎", "巽", "震", "离", "兑", "乾"};

    static String[] TRIGRAMS_TIAN_GAN = {"坤:乙癸", "艮:丙", "坎:戊", "巽:辛", "震:庚", "离:己", "兑:丁", "乾:甲壬"};

    static final String[] INNER_GUA_DI_ZHI = {
            "坤:丑亥酉", "艮:辰午申", "坎:寅辰午", "巽:丑亥酉", "震:子寅辰", "离:卯丑亥", "兑:巳卯丑", "乾:子寅辰"
    };

    static final String[] OUTER_GUA_DI_ZHI = {
            "坤:丑亥酉", "艮:戌子寅", "坎:申戌子", "巽:未巳卯", "震:午申戌", "离:酉未巳", "兑:亥酉未", "乾:午申戌",
    };

    static final String[] GONG_FIVE_ELEMENTS = {
            "坤宫:土", "艮宫:土", "坎宫:水", "巽宫:木", "震宫:木", "离宫:火", "兑宫:金", "乾宫:金"
    };


    static final int GUA_KUN = 0;
    static final int GUA_QIAN = 7;
    // 八宫名称
    static String[] GONG_NAMES = {
            "坤宫", "艮宫", "坎宫", "巽宫", "震宫", "离宫", "兑宫", "乾宫"
    };

    //六十四卦名称
    static String[][] SIXTY_FOUR_GUA_NAMES = {
            // 坤宫（土）
            {"坤为地", "地雷复", "地泽临", "地天泰", "雷天大壮", "泽天夬", "水天需", "水地比"},
            // 艮宫（土）
            {"艮为山", "山火贲", "山天大畜", "山泽损", "火泽睽", "天泽履", "风泽中孚", "风山渐"},
            // 坎宫（水）
            {"坎为水", "水泽节", "水雷屯", "水火既济", "泽火革", "雷火丰", "地火明夷", "地水师"},
            // 巽宫（木）
            {"巽为风", "风天小畜", "风火家人", "风雷益", "天雷无妄", "火雷噬嗑", "山雷颐", "山风蛊"},
            // 震宫（木）
            {"震为雷", "雷地豫", "雷水解", "雷风恒", "地风升", "水风井", "泽风大过", "泽雷随"},
            // 离宫（火）
            {"离为火", "火山旅", "火风鼎", "火水未济", "山水蒙", "风水涣", "天水讼", "天火同人"},
            // 兑宫（金）
            {"兑为泽", "泽水困", "泽地萃", "泽山咸", "水山蹇", "地山谦", "雷山小过", "雷泽归妹"},
            // 乾宫（金）
            {"乾为天", "天风姤", "天山遁", "天地否", "风地观", "山地剥", "火地晋", "火天大有"},

    };

    //六十四卦，每行第一个是卦宫
    static String[][] SIXTY_FOUR_GUA = {
            // 坤宫（土）
            {"000000", "100000", "110000", "111000", "111100", "111110", "111010", "000010"},
            // 艮宫（土）
            {"001001", "101001", "111001", "110001", "110101", "110111", "110011", "001011"},
            // 坎宫（水）
            {"010010", "110010", "100010", "101010", "101110", "101100", "101000", "010000"},
            // 巽宫（木）
            {"011011", "111011", "101011", "100011", "100111", "100101", "100001", "011001"},
            // 震宫（木）
            {"100100", "000100", "010100", "011100", "011000", "011010", "011110", "100110"},
            // 离宫（火）
            {"101101", "001101", "011101", "010101", "010001", "010011", "010111", "101111"},
            // 兑宫（金）
            {"110110", "010110", "000110", "001110", "001010", "001000", "001100", "110100"},
            // 乾宫（金）
            {"111111", "011111", "001111", "000111", "000011", "000001", "000101", "111101"},
    };

    static final List<String> LIU_HE_GUA = List.of("111000", "000111", "000100", "100000", "101001", "001101", "010110", "110010");

    static final List<String> LIU_CHONG_GUA = List.of("000000", "001001", "010010", "011011", "100100", "101101", "110110", "111111", "100111", "111100");

    static final String[] DI_ZHI_FIVE_ELEMENTS = {
            "寅卯:木", "巳午:火", "申酉:金", "亥子:水", "辰戌丑未:土"
    };
    // 六神顺序：从初爻开始的默认顺序
    static final String[] LIU_SHEN = {"青龙", "朱雀", "勾陈", "螣蛇", "白虎", "玄武"};
    //天干对应的六神的下标
    private static final Map<String, Integer> TIAN_GAN_LIU_SHEN = Map.of(
            "甲", 0,
            "乙", 0,
            "丙", 1,
            "丁", 1,
            "戊", 2,
            "己", 3,
            "庚", 4,
            "辛", 4,
            "壬", 5,
            "癸", 5
    );
    private static final String[] CHINESE_NUMBERS = {
            "初", "二", "三", "四", "五", "上"
    };

    public static void main(String[] args) {
        System.out.println("=== 六爻排盘 ===");
        LocalDateTime now = LocalDateTime.now();
        String dayGan = GanZhiCalendar.toDayGan(now);
        System.out.println("农历：" + GanZhiCalendar.toLunarDate(now));
        System.out.println("干支：" + GanZhiCalendar.toGanZhi(now));
        System.out.println("日空：" + GanZhiCalendar.getKongWang(now));
        int[] randomNumbers = getRandomNumbers();
        int[] fourQuadrant = toFourQuadrant(randomNumbers);
     //   System.out.println("六爻（四象）：" + Arrays.toString(fourQuadrant));
        String benGua = toSixYao(fourQuadrant);
        System.out.println("=== 本卦：" + benGua
                + " 卦名：" + getGuaName(benGua) + "(" + (LIU_HE_GUA.contains(benGua) ? "六合" : "") + (LIU_CHONG_GUA.contains(benGua) ? "六冲" : "") + ") ===");
        getGuaInfo(benGua,dayGan);
        String changeGua = changeSixYao(fourQuadrant);
        System.out.println("=== 变卦：" + changeGua
                + " 卦名：" + getGuaName(changeGua) + "(" + (LIU_HE_GUA.contains(changeGua) ? "六合" : "") + (LIU_CHONG_GUA.contains(changeGua) ? "六冲" : "") + ") ===");
        getGuaInfo(changeGua,dayGan);

    }


    public static void getGuaInfo(String gua,String dayGan) {
        String guaGong = getGuaGong(gua);
        String guaDiZhi = getGuaDiZhi(gua);
        String gongWx = getGongFiveElement(guaGong);
        String yaoWxs = getYaoFiveElement(guaDiZhi);
        String guaLiuQin = getGuaLiuQin(gongWx, yaoWxs);
        String guaTG = getGuaDayGan(gua);
        String guaLiuShen = getGuaLiuShen(dayGan);
        int shiYaoIndex = getShiYaoIndex(gua);
        int yingYaoIndex = getYingYaoIndex(shiYaoIndex);
        for (int i = gua.length() - 1; i >= 0; i--) {
            String c = gua.substring(i, i + 1);
            String yaoName = (c.equals("0") ? "六" : "九") + CHINESE_NUMBERS[i];
            if (i == 0 || i == 5) {
                yaoName = CHINESE_NUMBERS[i] + (c.equals("0") ? "六" : "九");
            }
            String shiYao = (shiYaoIndex - 1) == i ? "世" : "";
            String yingYao = (yingYaoIndex - 1) == i ? "应" : "";
            String yaoTG = guaTG.substring(i, i + 1);
            String dz = guaDiZhi.substring(i, i + 1);
            String wx = yaoWxs.substring(i, i + 1);
            String lq = guaLiuQin.substring(i * 2, (i + 1) * 2);
            String ls = guaLiuShen.substring(i * 2, (i + 1) * 2);
            System.out.println(ls + " " + yaoName + " " + lq + yaoTG + dz + wx + " " + shiYao + yingYao);
        }
    }


    public static int getShiYaoIndex(String gua) {
        Map<String, Integer> map = new HashMap<>();
        for (String[] gong_gua : SIXTY_FOUR_GUA) {
            for (int j = 0; j < gong_gua.length; j++) {
                if (j == 0) {
                    map.put(gong_gua[j], 6);
                } else if (j == 6 || j == 7) {
                    map.put(gong_gua[j], 3);
                } else {
                    map.put(gong_gua[j], j);
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
        if (shiYaoIndex > 3) {
            return shiYaoIndex - 3;
        }
        return shiYaoIndex + 3;
    }

    public static String getGuaDayGan(String gua) {
        // 确定内卦和外卦
        int innerIdx = getTrigram(gua.substring(0, 3));
        int outerIdx = getTrigram(gua.substring(3));
        // 获取内卦天干
        String innerTG = resolveInnerTianGan(innerIdx);
        String outerTG = resolveOuterTianGan(outerIdx);
        String innerGuaTG = innerTG + innerTG + innerTG;
        String outerGuaTG = outerTG + outerTG + outerTG;
        return innerGuaTG + outerGuaTG;
    }

    private static String resolveInnerTianGan(int idx) {
        if (idx == GUA_QIAN) return "甲";
        if (idx == GUA_KUN) return "乙";
        return TRIGRAMS_TIAN_GAN[idx].split(":")[1];
    }

    private static String resolveOuterTianGan(int idx) {
        if (idx == GUA_QIAN) return "壬";
        if (idx == GUA_KUN) return "癸";
        return TRIGRAMS_TIAN_GAN[idx].split(":")[1];
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


    public static String getGongFiveElement(String gong) {
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
            String diZhi = Character.toString(guaDiZhi.charAt(i));
            for (String element : DI_ZHI_FIVE_ELEMENTS) {
                if (element.contains(diZhi)) {
                    list.add(element.split(":")[1]);
                }
            }
        }
        return String.join("", list);
    }

    public static String getGuaDiZhi(String gua) {
        // 确定内卦和外卦
        int innerGuaIdx = getTrigram(gua.substring(0, 3));
        int outerGuaIdx = getTrigram(gua.substring(3));
        return INNER_GUA_DI_ZHI[innerGuaIdx].split(":")[1] + OUTER_GUA_DI_ZHI[outerGuaIdx].split(":")[1];
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

    public static int[] toFourQuadrant(int[] numbers) {
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

    public static String toSixYao(int[] fourQuadrant) {
        int[] gua = new int[6];
        for (int i = 0; i < fourQuadrant.length; i++) {
            int yao = fourQuadrant[i];
            if (yao < 2) {
                gua[i] = 0;
            } else {
                gua[i] = 1;
            }
        }
        return convert(gua);
    }

    public static String changeSixYao(int[] fourQuadrant) {
        int[] gua = new int[6];
        for (int i = 0; i < fourQuadrant.length; i++) {
            int yao = fourQuadrant[i];
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
        return convert(gua);
    }

    public static String getGuaGong(String gua) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < SIXTY_FOUR_GUA.length; i++) {
            String[] guas = SIXTY_FOUR_GUA[i];
            for (String _gua : guas) {
                map.put(_gua, i);
            }
        }
        int index = map.get(gua);
        return GONG_NAMES[index];
    }

    public static String getGuaName(String gua) {
        // 确定内卦和外卦
        int innerGuaIdx = getTrigram(gua.substring(0, 3));
        int outerGuaIdx = getTrigram(gua.substring(3));
        // 打印结果
      //  System.out.println("内卦: " + TRIGRAMS[innerGuaIdx] + " 外卦: " + TRIGRAMS[outerGuaIdx]);
        for (int i = 0; i < SIXTY_FOUR_GUA.length; i++) {
            String[] gong_gua = SIXTY_FOUR_GUA[i];
            for (int j = 0; j < gong_gua.length; j++) {
                if (gua.equals(gong_gua[j])) {
                    return SIXTY_FOUR_GUA_NAMES[i][j];
                }
            }
        }
        return null;
    }

    // 根据三个爻的状态确定是哪个三爻卦
    private static int getTrigram(String binaryStr) {
        return Integer.parseInt(binaryStr, 2);
    }

    static class WuXing {
        final static String WOOD = "木";
        final static String FIRE = "火";
        final static String EARTH = "土";
        final static String METAL = "金";
        final static String WATER = "水";
    }

    static class LiuQin {
        final static String PARENTS = "父母";
        final static String CHILDREN = "子孙";
        final static String OFFICIAL_GHOST = "官鬼";
        final static String SPOUSE_WEALTH = "妻财";
        final static String BROTHERS = "兄弟";
    }
}
