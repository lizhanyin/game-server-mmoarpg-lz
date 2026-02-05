import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从生成的 Protobuf Java 文件中解码 descriptor 数据
 * 用于重建原始的 .proto 文件
 */
public class DecodeProto {

    public static void main(String[] args) {
        // 从 Msg.java 中提取的 descriptorData 字符串
        String descriptor = getStringFromDescriptor();

        // 解码并保存到文件
        byte[] decoded = decodeDescriptor(descriptor);

        // 保存为 msg.proto
        saveToFile("msg.proto", decoded);

        // 同时尝试解析为文本格式输出
        System.out.println("========== 解码后的 msg.proto 内容 ==========");
        System.out.println(new String(decoded));
        System.out.println("=========================================");
    }

    /**
     * 将 descriptor 字符串中的八进制转义序列解码为字节数组
     */
    private static byte[] decodeDescriptor(String descriptor) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 匹配八进制转义序列 \xxx
        Pattern pattern = Pattern.compile("\\\\([0-7]{3})");
        Matcher matcher = pattern.matcher(descriptor);

        int lastEnd = 0;
        while (matcher.find()) {
            // 添加之前的内容（不包括转义序列）
            String before = descriptor.substring(lastEnd, matcher.start());
            for (char c : before.toCharArray()) {
                baos.write(c);
            }

            // 解码八进制转义序列
            String octal = matcher.group(1);
            int value = Integer.parseInt(octal, 8);
            baos.write(value);

            lastEnd = matcher.end();
        }

        // 添加剩余的内容
        if (lastEnd < descriptor.length()) {
            String remaining = descriptor.substring(lastEnd);
            for (char c : remaining.toCharArray()) {
                baos.write(c);
            }
        }

        return baos.toByteArray();
    }

    /**
     * 获取 descriptor 字符串（简化版，直接从 Msg.java 复制）
     * 实际使用时应该从 Msg.java 文件中提取
     */
    private static String getStringFromDescriptor() {
        return "\n\tmsg.proto\022\031org.gof.demo.worldsrv.msg\032\r" +
        "options.proto\" \n\010DVector2\022\t\n\001x\030\001 \001(\002\022\t\n\001" +
        "y\030\002 \001(\002\"+\n\010DVector3\022\t\n\001x\030\001 \001(\002\022\t\n\001y\030\002 \001(" +
        "\002\022\t\n\001z\030\003 \001(\002\"b\n\nDCharacter\022\n\n\002id\030\001 \002(\003\022\n" +
        "\n\002sn\030\002 \001(\t\022\014\n\004name\030\003 \001(\t\022\022\n\nprofession\030\004" +
        " \001(\005\022\r\n\005level\030\005 \001(\005\022\013\n\003sex\030\006 \001(\005\"\224\002\n\005DUn" +
        "it\022\n\n\002id\030\001 \001(\003\22.\n\004prop\030\002 \001(\0132 .org.gof.d" +
        "emo.worldsrv.msg.DProp\022\014\n\004name\030\003 \001(\t\022\022\n\n" +
        "profession\030\004 \001(\005\022\r\n\005level\030\005 \001(\005\022\013\n\003sex\030\006" +
        " \001(\005\022\r\n\005hpCur\030\007 \001(\005\022\r\n\005mpCur\030\010 \001(\005\022\r\n\005hp" +
        "Max\030\t \001(\005\022\r\n\005mpMax\030\n \001(\005\022\017\n\007modelSn\030\013 \001(" +
        "\t\022\n\n\002sn\030\r \001(\t\022\016\n\006expCur\030\016 \001(\003\022\022\n\nexpUpgr" +
        "ade\030\017 \001(\003\022\024\n\014teamBundleID\0304 \001(\003\"\234\005\n\006DHum" +
        "an\022\n\n\002id\030\001 \001(\003\22.\n\004prop\030\002 \001(\0132 .org.gof.d" +
        "emo.worldsrv.msg.DProp\022\014\n\004name\030\003 \001(\t\022\022\n\n" +
        "profession\030\004 \001(\005\022\r\n\005level\030\005 \001(\005\022\013\n\003sex\030\006" +
        " \001(\005\022\r\n\005hpCur\030\007 \001(\005\022\r\n\005mpCur\030\010 \001(\005\022\r\n\005hp" +
        "Max\030\t \001(\005\022\r\n\005mpMax\030\n \001(\005\022\020\n\010actValue\030\013 \001" +
        "(\003\022\023\n\013actValueMax\030\014 \001(\003\022\014\n\004gold\030\r \001(\003\022\014\n" +
        "\004coin\030\016 \001(\003\022\016\n\006combat\030\017 \001(\005\022\016\n\006expCur\030\020 " +
        "\001(\003\022\017\n\007modelSn\030\021 \001(\t\022\n\n\002sn\030\022 \001(\t\022\020\n\010prop" +
        "Json\030\023 \001(\t\022\017\n\007canMove\030\024 \001(\010\022\024\n\014canCastSk" +
        "ill\030\025 \001(\010\022\021\n\tcanAttack\030\026 \001(\010\022\024\n\014teamBund" +
        "leID\030\027 \001(\003\022\023\n\013generalList\030\030 \003(\003\022\020\n\010vipLe" +
        "vel\030\031 \001(\005\022\023\n\013competMoney\030\032 \001(\003\022\017\n\007virtua" +
        "l\030\036 \003(\003\022\022\n\nfightPower\030! \001(\005\022\023\n\013rebirthGo" +
        "ld\030\" \001(\005\0220\n\005parts\030# \003(\0132!.org.gof.demo.w" +
        "orldsrv.msg.DParts\022\033\n\023propShopLimitCount" +
        "s\030$ \003(\005\022\030\n\020otherLimitCounts\030% \003(\005\022\033\n\023nex" +
        "tRefreshShopTime\030& \001(\003\"\355\007\n\005DProp\022\r\n\005hpMa" +
        "x\030\001 \001(\005\022\020\n\010hpMaxPct\030\002 \001(\005\022\r\n\005mpMax\030\003 \001(\005" +
        "\022\020\n\010mpMaxPct\030\004 \001(\005\022\016\n\006atkPhy\030\005 \001(\005\022\021\n\tat" +
        "kPhyPct\030\006 \001(\005\022\016\n\006defPhy\030\007 \001(\005\022\021\n\tdefPhyP" +
        "ct\030\010 \001(\005\022\016\n\006atkMag\030\t \001(\005\022\021\n\tatkMagPct\030\n " +
        "\001(\005\022\016\n\006defMag\030\013 \001(\005\022\021\n\tdefMagPct\030\014 \001(\005\022\013" +
        "\n\003hit\030\r \001(\005\022\016\n\006hitPct\030\016 \001(\005\022\r\n\005dodge\030\017 \001" +
        "(\005\022\020\n\010dodgePct\030\020 \001(\005\022\014\n\004crit\030\021 \001(\005\022\017\n\007cr" +
        "itPct\030\022 \001(\005\022\017\n\007critAdd\030\023 \001(\005\022\022\n\ncritAddP" +
        "ct\030\024 \001(\005\022\r\n\005tough\030\025 \001(\005\022\020\n\010toughPct\030\026 \001(" +
        "\005\022\020\n\010elem1Atk\030( \001(\005\022\023\n\013elem1AtkPct\030) \001(\005" +
        "\022\020\n\010elem1Def\030* \001(\005\022\023\n\013elem1DefPct\030+ \001(\005\022" +
        "\020\n\010elem2Atk\030, \001(\005\022\023\n\013elem2AtkPct\030- \001(\005\022\020" +
        "\n\010elem2Def\030. \001(\005\022\023\n\013elem2DefPct\030/ \001(\005\022\020\n" +
        "\010elem3Atk\0300 \001(\005\022\023\n\013elem3AtkPct\0301 \001(\005\022\020\n\010" +
        "elem3Def\0302 \001(\005\022\023\n\013elem3DefPct\0303 \001(\005\022\020\n\010e" +
        "lem4Atk\0304 \001(\005\022\023\n\013elem4AtkPct\0305 \001(\005\022\020\n\010el" +
        "em4Def\0306 \001(\005\022\023\n\013elem4DefPct\0307 \001(\005\022\014\n\004suc" +
        "k\030P \001(\005\022\017\n\007suckPct\030Q \001(\005\022\021\n\tsuckRatio\030R " +
        "\001(\005\022\024\n\014suckRatioPct\030S \001(\005\022\020\n\010avoidAtk\030T " +
        "\001(\005\022\023\n\013avoidAtkPct\030U \001(\005\022\023\n\013skillHealth\030" +
        "V \001(\005\022\026\n\016skillHealthPct\030W \001(\005\022\r\n\005speed\030X" +
        " \001(\005\022\020\n\010speedPct\030Y \001(\005\022\017\n\007hpRecov\030Z \001(\005\022" +
        "\022\n\nhpRecovPct\030[ \001(\005\022\017\n\007mpRecov\030\\ \001(\005\022\022\n\n" +
        "mpRecovPct\030] \001(\005\022\020\n\010mpReduce\030^ \001(\005\022\023\n\013mp" +
        "ReducePct\030_ \001(\005\022\r\n\005aggro\030n \001(\005\"#\n\010DProdu" +
        "ce\022\n\n\002sn\030\001 \001(\005\022\013\n\003num\030\002 \001(\005\"%\n\010DVirtual\022" +
        "\014\n\004type\030\001 \001(\005\022\013\n\003num\030\002 \001(\003\"u\n\013DAwardGrou" +
        "p\0225\n\010virtuals\030\001 \003(\0132#.org.gof.demo.world" +
        "srv.msg.DVirtual\022/\n\005items\030\002 \003(\0132 .org.go" +
        "f.demo.worldsrv.msg.DItem\"R\n\006DParts\022\n\n\002s," +
        "n\030\001 \001(\005\022\014\n\004qhLv\030\002 \001(\005\022\r\n\005cxLvs\030\003 \003(\005\022\017\n\007" +
        "equipSn\030\004 \001(\005\022\016\n\006gemSns\030\005 \003(\005\"/\n\021DBagCel" +
        "lNumChange\022\r\n\005index\030\001 \001(\005\022\013\n\003num\030\002 \001(\005\"." +
        "\n\014DBagCellMove\022\016\n\006sIndex\030\001 \001(\005\022\016\n\006tIndex" +
        "\030\002 \001(\005\"L\n\013DBagCellAdd\022\r\n\005index\030\001 \001(\005\022.\n\004" +
        "item\030\002 \001(\0132 .org.gof.demo.worldsrv.msg.D" +
        "Item\"[\n\005DItem\022\014\n\004code\030\001 \001(\005\022\013\n\003num\030\002 \001(\005" +
        "\022\014\n\004bind\030\003 \001(\005\022\r\n\005etime\030\004 \001(\003\022\013\n\003pos\030\005 \001" +
        "(\005\022\r\n\005isNew\030\006 \001(\010\"\331\001\n\nDBagUpdate\022\r\n\005pTyp" +
        "e\030\001 \001(\005\022\014\n\004type\030\002 \001(\005\022@\n\nnumChanger\030\003 \001(" +
        "\0132,.org.gof.demo.worldsrv.msg.DBagCellNu" +
        "mChange\0225\n\005adder\030\004 \001(\0132&.org.gof.demo.wo" +
        "rldsrv.msg.DBagCellAdd\0225\n\004move\030\005 \001(\0132\'.o" +
        "rg.gof.demo.worldsrv.msg.DBagCellMove\"I\n" +
        "\004DBag\022\020\n\010capacity\030\001 \001(\005\022/\n\005items\030\002 \003(\0132 " +
        ".org.gof.demo.worldsrv.msg.DItem\"$\n\006DGoo" +
        "ds\022\n\n\002sn\030\001 \001(\005\022\016\n\006status\030\002 \001(\005\"\\\n\005DShop\022" +
        "\n\n\002sn\030\001 \001(\005\022\024\n\014refreshCount\030\002 \001(\005\0221\n\006she" +
        "lfs\030\004 \003(\0132!.org.gof.demo.worldsrv.msg.DG" +
        "oods\"3\n\005DTask\022\014\n\004code\030\001 \001(\005\022\r\n\005state\030\002 \001(" +
        "\005\022\r\n\005value\030\003 \001(\005\"Q\n\006DQuest\022\n\n\002sn\030\001 \001(\005\022" +
        "\026\n\016targetProgress\030\002 \001(\005\022\023\n\013nowProgress\030\003" +
        " \001(\005\022\016\n\006status\030\004 \001(\005\"\335\001\n\005DMail\022\016\n\006sender" +
        "\030\001 \001(\003\022\r\n\005title\030\002 \001(\t\022\017\n\007content\030\003 \001(\t\022\020" +
        "\n\010sendTime\030\004 \001(\003\022\020\n\010spanTime\030\005 \001(\003\022\014\n\004re" +
        "ad\030\006 \001(\005\0225\n\010virtuals\030\007 \003(\0132#.org.gof.dem" +
        "o.worldsrv.msg.DVirtual\022/\n\005items\030\010 \003(\0132 " +
        ".org.gof.demo.worldsrv.msg.DItem\022\n\n\002id\030\t" +
        " \001(\003\"5\n\005DAttr\022\014\n\004type\030\001 \001(\005\022\r\n\005value\030\002 \001" +
        "(\005\022\017\n\007addType\030\003 \001(\005\"\224\001\n\tDTreasure\022\014\n\004typ" +
        "e\030\001 \001(\005\022\021\n\tdailyFree\030\002 \001(\005\022\021\n\tbonusProc\030" +
        "\003 \001(\005\022\017\n\007toHiden\030\004 \001(\005\022\n\n\002cd\030\005 \001(\003\022\020\n\010ho" +
        "tItems\030\006 \003(\005\022\021\n\tbatchCost\030\007 \001(\005\022\021\n\tbatch" +
        "Sale\030\010 \001(\002\"+\n\013DBlackGoods\022\014\n\004code\030\001 \001(\005\022" +
        "\016\n\006canBuy\030\002 \001(\010\"5\n\007DSignIn\022\r\n\005month\030\001 \001(" +
        "\005\022\014\n\004days\030\002 \001(\005\022\r\n\005state\030\003 \001(\005\"D\n\tDLiven" +
        "ess\022\n\n\002sn\030\001 \001(\005\022\n\n\002tp\030\002 \001(\005\022\017\n\007currNum\030\003" +
        " \001(\005\022\016\n\006status\030\004 \001(\005\"\233\001\n\017DTeamMemberItem" +
        "\022\017\n\007humanId\030\001 \001(\003\022\016\n\006headSn\030\002 \001(\t\022\014\n\004nam" +
        "e\030\003 \001(\t\022\r\n\005job\030\004 \001(\005\022\016\n\006hpMax\030\005 \001(\005\022\016\n\006mpMax\030\006 \001(\005";
    }

    /**
     * 保存字节数组到文件
     */
    private static void saveToFile(String filename, byte[] data) {
        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }

            System.out.println("文件已保存: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
