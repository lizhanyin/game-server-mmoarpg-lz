package org.gof.core.support;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import static org.apache.commons.lang3.math.NumberUtils.isCreatable;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.logging.log4j.message.ParameterizedMessage;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class Utils {

    /**
     * MD5加密
     * 
     * @param s 被加密的字符串
     * @return 加密后的字符串
     */
    public static String md5(String s) {
        if (s == null)
            s = "";
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes("UTF-8");
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断两个对象是否相等
     * 
     * @param objA
     * @param objB
     * @return
     */
    public static boolean isEquals(Object objA, Object objB) {
        return new EqualsBuilder().append(objA, objB).isEquals();
    }

    /**
     * 参数1是否为参数2的子类或接口实现
     * 
     * @param parentCls
     * @return
     */
    public static boolean isInstanceof(Class<?> cls, Class<?> parentCls) {
        return parentCls.isAssignableFrom(cls);
    }

    /**
     * 格式化时间戳
     * 
     * @param value
     * @param pattern
     * @return
     */
    public static String formatTime(long timestamp, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);

        return format.format(new Date(timestamp));
    }

    /**
     * 根据时分秒配置 获取今天配置时间点
     * 
     * @param suffix
     * @return
     */
    public static long formatDateStr(String dateStr, String pattern) {
        try {
            SimpleDateFormat bartDateFormat = new SimpleDateFormat(pattern);

            return bartDateFormat.parse(dateStr).getTime();
        } catch (ParseException ex) {
            throw new SysException(ex);
        }
    }

    /**
     * 两个时间戳相差的天数
     * 
     * @param a
     * @param b
     * @return
     */
    public static int getDaysBetween(long ta, long tb) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(ta);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(tb);

        if (a.after(b)) {
            Calendar swap = a;
            a = b;
            b = swap;
        }

        int days = b.get(Calendar.DAY_OF_YEAR) - a.get(Calendar.DAY_OF_YEAR);
        int y2 = b.get(Calendar.YEAR);
        if (a.get(Calendar.YEAR) != y2) {
            a = (Calendar) a.clone();
            do {
                days += a.getActualMaximum(Calendar.DAY_OF_YEAR);
                a.add(Calendar.YEAR, 1);
            } while (a.get(Calendar.YEAR) != y2);
        }
        return days;
    }

    /**
     * 是否是同一天
     * 
     * @param ta
     * @param tb
     * @return
     */
    public static boolean isSameDay(long ta, long tb) {
        return Utils.formatTime(ta, "yyyyMMdd").equals(Utils.formatTime(tb, "yyyyMMdd"));
    }

    /**
     * 获取给定时间当前凌晨的时间对象
     * 
     * @param time 取当天凌晨的话传入 System.currentTimeMillis() 即可
     * @return
     */
    public static long getTimeBeginOfToday(long time) {
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.set(Calendar.MINUTE, 0);
        ca.set(Calendar.SECOND, 0);
        ca.set(Calendar.MILLISECOND, 0);

        return ca.getTimeInMillis();
    }

    /**
     * 获取给定时间本周一的时间对象
     * 
     * @param time 取当天凌晨的话传入 System.currentTimeMillis() 即可
     * @return
     */
    public static long getTimeBeginOfWeek(long time) {
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);

        // 当今天是星期天的时候，需要特殊处理，因为星期天是按照这个星期第一天算的，而我们不是这么需要的
        long timeCheck = 0;
        if (ca.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            timeCheck = Time.DAY * 7;
        }

        ca.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        ca.set(Calendar.HOUR_OF_DAY, 0);
        ca.set(Calendar.MINUTE, 0);
        ca.set(Calendar.SECOND, 0);
        ca.set(Calendar.MILLISECOND, 0);

        return ca.getTimeInMillis() - timeCheck;
    }

    /**
     * 根据当前时间，获取小时数 24小时格式
     * 
     * @param time 当天的时间
     * @return
     */
    public static int getHourOfTime(long time) {
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(time);
        return ca.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 构造List对象
     * 
     * 如果传入的是参数仅仅为一个对象数组(Object[])或原生数组(int[], long[]等)
     * 那么表现结果表现是不同的，Object[]为[obj[0], obj[1], obj[2]]
     * 而原生数组则为[[int[0], int[1]，int[2]]]
     * 多了一层嵌套，需要对原生数组进行特殊处理。
     * 
     * @param <T>
     * @param ts
     * @return
     */
    @SafeVarargs
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> List<T> ofList(T... ts) {
        List result = new ArrayList();

        // 对Null进行特殊处理
        if (ts == null) {
            result.add(null);
            return result;
        }

        // 对单独的原始数组类型进行特殊处理
        if (ts.length == 1 && ts[0] != null && OFLIST_ARRAY_CLASS.contains(ts[0].getClass())) {
            switch (ts[0]) {
                case int[] val -> {
                    for (int v : val) {
                        result.add(v);
                    }
                }
                case long[] val -> {
                    for (long v : val) {
                        result.add(v);
                    }
                }
                case boolean[] val -> {
                    for (boolean v : val) {
                        result.add(v);
                    }
                }
                case byte[] val -> {
                    for (byte v : val) {
                        result.add(v);
                    }
                }
                case double[] val -> {
                    for (double v : val) {
                        result.add(v);
                    }
                }
                default -> {
                }
            }
        } else { // 对象数组
            result.addAll(Arrays.asList(ts));
        }

        return result;
    }

    // 专供ofList类使用 对于数组类型进行特殊处理
    private static final List<?> OFLIST_ARRAY_CLASS = Utils.ofList(int[].class, long[].class, boolean[].class,
            byte[].class, double[].class);

    /**
     * 构造Map对象
     * 
     * @param <T>
     * @param ts
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> ofMap(Object... params) {
        LinkedHashMap<K, V> result = new LinkedHashMap<>();

        // 无参 返回空即可
        if (params == null || params.length == 0) {
            return result;
        }

        // 处理成对参数
        int len = params.length;
        for (int i = 0; i < len; i += 2) {
            K key = (K) params[i];
            V val = (V) params[i + 1];

            result.put(key, val);
        }

        return result;
    }

    /**
     * 基于参数创建字符串
     * #0开始
     * 
     * @param str
     * @param params
     * @return
     */
    public static String createStr(String str, Object... params) {
        return ParameterizedMessage.format(str, params);
    }

    public static void intToBytes(byte[] b, int offset, int v) {
        for (int i = 0; i < 4; ++i) {
            b[offset + i] = (byte) (v >>> (24 - i * 8));
        }
    }

    public static int bytesToInt(byte[] b, int offset) {
        int num = 0;
        for (int i = offset; i < offset + 4; ++i) {
            num <<= 8;
            num |= (b[i] & 0xff);
        }
        return num;
    }

    public static int bytesToInt(Object[] b, int offset) {
        int num = 0;
        for (int i = offset; i < offset + 4; ++i) {
            num <<= 8;
            num |= (((byte) b[i]) & 0xff);
        }
        return num;
    }

    public static int bytesToLittleEndian32(byte[] b, int offset) {
        return (((int) b[offset] & 0xff)) |
                (((int) b[offset + 1] & 0xff) << 8) |
                (((int) b[offset + 2] & 0xff) << 16) |
                (((int) b[offset + 3] & 0xff) << 24);
    }

    public static void LittleEndian32ToBytes(byte[] b, int offset, int value) {
        b[offset + 0] = (byte) ((value) & 0xFF);
        b[offset + 1] = (byte) ((value >> 8) & 0xFF);
        b[offset + 2] = (byte) ((value >> 16) & 0xFF);
        b[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    public static String toHexString(byte[] byteBuffer, int length) {
        StringBuilder outputBuf = new StringBuilder(length * 4);

        for (int i = 0; i < length; i++) {
            String hexVal = Integer.toHexString(byteBuffer[i] & 0xff);

            if (hexVal.length() == 1) {
                hexVal = "0" + hexVal; //$NON-NLS-1$
            }

            outputBuf.append(hexVal); // $NON-NLS-1$
        }
        return outputBuf.toString();
    }

    public static String getClassPath() {
        return Class.class.getResource("/").getPath();
    }

    /**
     * String转为int型
     * 如果出错 则为0
     * 
     * @param value
     * @return
     */
    public static int intValue(String value) {
        if (StringUtils.isNotEmpty(value) && NumberUtils.isCreatable(value))
            return Double.valueOf(value).intValue();
        else
            return 0;
    }

    public static int intValue(Integer value) {
        if (null == value)
            return 0;
        else
            return value;
    }

    /**
     * String转为long型
     * 如果出错 则为0
     * 
     * @param value
     * @return
     */
    public static long longValue(String value) {
        if (StringUtils.isNotEmpty(value) && NumberUtils.isCreatable(value))
            return Long.parseLong(value);
        else
            return 0;
    }

    public static long longValue(Long value) {
        if (null == value)
            return 0;
        else
            return value;
    }

    /**
     * String转为double型
     * 如果出错 则为0.0
     * 
     * @param value
     * @return
     */
    public static double doubleValue(String value) {
        if (StringUtils.isNotEmpty(value) && NumberUtils.isCreatable(value))
            return Double.parseDouble(value);
        else
            return 0.0D;
    }

    public static double doubleValue(Double value) {
        if (null == value)
            return 0.0D;
        else
            return value;
    }

    /**
     * String转为float型
     * 如果出错 则为0.0
     * 
     * @param value
     * @return
     */
    public static float floatValue(String value) {
        if (StringUtils.isNotEmpty(value) && isCreatable(value))
            return Float.parseFloat(value);
        else
            return 0.0f;
    }

    public static float floatValue(Float value) {
        if (null == value)
            return 0.0f;
        else
            return value;
    }

    /**
     * String转为boolean型
     * 如果出错 则为false
     * 
     * @param value
     * @return
     */
    public static boolean booleanValue(String value) {
        return "true".equalsIgnoreCase(value) && value != null;
    }

    /**
     * 从字符串转为JSONArray，主要目的是包装一下空值处理
     * 
     * @param str
     * @return 正常返回对象，否则返回长度为0的JSONArray
     */
    public static JSONArray toJSONArray(String str) {
        if (StringUtils.isEmpty(str)) {
            str = "[]";
        }

        return JSON.parseArray(str);
    }

    /**
     * 从字符串转为JSONObject，主要目的是包装一下空值处理
     * 
     * @param str
     * @return 正常返回对象，否则返回空的JSONObject
     */
    public static JSONObject toJSONObject(String str) {
        if (StringUtils.isEmpty(str)) {
            str = "{}";
        }

        return JSON.parseObject(str);
    }

    /**
     * 获取Param值 提供参数默认值
     * 
     * @param param
     * @param key
     * @param defaultValue
     * @return
     */
    public static <T> T getParamValue(Param param, String key, T defaultValue) {
        T result = param.get(key);
        if (result == null)
            result = defaultValue;

        return result;
    }

    /**
     * 从字符串转为JSONObject，主要目的是包装一下异常处理
     * 
     * @param str
     * @return 正常返回对象，否则返回长度为0的JSONArray
     */
    public static JSONObject str2JSONObject(String str) {
        if (StringUtils.isEmpty(str)) {
            str = "{}";
        }
        return JSON.parseObject(str);
    }

    /**
     * 将对象转化为JSON字符串
     * 
     * @param obj
     * @return
     */
    public static String toJSONString(Object obj) {
        return JSON.toJSONString(obj);
    }

    /**
     * 把两个数组组成一个匹配的Json 前面是属性，后面是数值
     * 
     * @param str1
     * @param str2
     * @return
     */
    public static String toJOSNString(String[] str1, String[] str2) {
        Map<String, String> tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], str2[i]);
            }
        }
        return toJSONString(tempMap);
    }

    /**
     * 把两个数组组成一个匹配的Json 前面是属性，后面是数值
     * 
     * @param str1
     * @param str2
     * @return
     */
    public static String toJOSNString(String[] str1, int[] str2) {
        Map<String, Integer> tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], str2[i]);
            }
        }
        return toJSONString(tempMap);
    }

    public static Map<String, Integer> arrToMap(String[] str1, int[] str2) {
        Map<String, Integer> tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], str2[i]);
            }
        }
        return tempMap;
    }

    public static Map<String, Float> toMapWeight(String[] str1, float[] str2, float weight) {
        Map<String, Float> tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], (str2[i] + weight));
            }
        }
        return tempMap;
    }

    public static String toJOSNStringWeight(String[] str1, int[] str2, double weight) {
        Map<String, Integer> tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], (int) (str2[i] + weight));
            }
        }
        return toJSONString(tempMap);
    }

    public static String toJOSNStringNag(String[] str1, int[] str2) {
        Map<String, Integer> tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], -str2[i]);
            }
        }
        return toJSONString(tempMap);
    }

    /**
     * 把两个数组组成一个匹配的Json 前面是属性，后面是数值
     * 
     * @param str1
     * @param str2
     * @return
     */
    public static String toJOSNString(String[] str1, float[] str2) {
        Map<String, Float> tempMap;
        tempMap = new HashMap<>();
        if (str1 != null && str2 != null && str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                tempMap.put(str1[i], str2[i]);
            }
        }
        return toJSONString(tempMap);
    }

    /**
     * 读取配置文件
     * 
     * @return
     */
    public static Properties readProperties(String name) {
        String filePath = Utils.class.getClassLoader().getResource(name).getPath();
        try (FileInputStream in = new FileInputStream(filePath)) {
            Properties p = new Properties();
            p.load(in);

            return p;
        } catch (Exception e) {
            throw new SysException(e);
        }
    }

    /**
     * 获取对象的属性
     * 会先尝试利用getter方法获取 然后再直接访问字段属性
     * 如果给定的属性不存在 会返回null
     * 
     * @param obj
     * @param fieldName
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T fieldRead(Object obj, String fieldName) {
        try {
            // 返回值
            Object result = null;

            Class<? extends Object> clazz = obj.getClass();

            // 先通过自省来获取字段的值(getter方法)
            boolean hasGetter = false;
            BeanInfo bi = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            for (PropertyDescriptor p : pds) {
                if (!p.getName().equals(fieldName))
                    continue;

                result = p.getReadMethod().invoke(obj);
                hasGetter = true;
            }

            // 如果通过getter方法没找到 那么就尝试直接读取字段
            if (!hasGetter) {
                for (Field f : clazz.getFields()) {
                    if (!f.getName().equals(fieldName))
                        continue;

                    result = f.get(obj);
                }
            }

            return (T) result;
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new SysException(e);
        }
    }

    /**
     * 获取对象的静态属性
     * 
     * @param clazz
     * @param fieldName
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T fieldRead(Class<?> clazz, String fieldName) {
        try {
            Field field = FieldUtils.getDeclaredField(clazz, fieldName);
            return (T) field.get(clazz);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new SysException(e);
        }
    }

    /**
     * 设置对象的属性
     * 会先尝试利用setter方法修改 然后再直接修改字段属性
     * 如果给定的属性不存在 会抛出异常
     * 
     * @param obj
     * @param fieldName
     * @return
     */
    public static void fieldWrite(Object obj, String fieldName, Object valueNew) {
        try {
            Class<? extends Object> clazz = obj.getClass();

            // 先通过自省来设置字段的值(setter方法)
            boolean hasSetter = false;
            BeanInfo bi = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            for (PropertyDescriptor p : pds) {
                if (!p.getName().equals(fieldName))
                    continue;

                // 到这里的话 证明属性能找到（至少有对应的getter）但是没有找到setter
                // 可能是setter方法不符合规范 比如非void有返回值等
                // 这种情况使用反射再次尝试
                Method wm = p.getWriteMethod();
                if (wm == null) {
                    String wmStr = "set" + StringUtils.capitalize(fieldName);
                    for (Method m : clazz.getMethods()) {
                        if (!m.getName().equals(wmStr))
                            continue;
                        m.invoke(obj, valueNew);
                    }
                } else {
                    wm.invoke(obj, valueNew);
                }

                hasSetter = true;
            }

            // 如果通过setter方法没找到 那么就尝试直接操作字段
            if (!hasSetter) {
                Field f = clazz.getField(fieldName);
                f.set(obj, valueNew);
            }
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException | InvocationTargetException e) {
            throw new SysException(e);
        }
    }

    /**
     * 通过反射执行函数
     * 
     * @param obj
     * @param method
     * @param param
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object obj, String method, Object... params) {
        try {
            return (T) MethodUtils.invokeMethod(obj, method, params);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new SysException(e);
        }
    }

    /**
     * 通过反射执行函数
     * 
     * @param obj
     * @param method
     * @param param
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeStaticMethod(Class<?> cls, String method, Object... params) {
        try {
            return (T) MethodUtils.invokeStaticMethod(cls, method, params);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new SysException(e);
        }
    }

    /**
     * 通过反射执行构造函数
     * 
     * @param cls
     * @param params
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeConstructor(Class<?> cls, Object... params) {
        try {
            return (T) ConstructorUtils.invokeConstructor(cls, params);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new SysException(e);
        }
    }

    /**
     * 进行Get请求操作
     *
     * @return
     */
    public static String httpGet(String url, Map<String, String> params) {
        try {
            // 1 拼接地址
            StringBuilder urlSB = new StringBuilder(url);
            // 1.1 有需要拼接的参数
            if (!params.isEmpty()) {
                urlSB.append("?");
            }

            // 1.2 拼接参数
            for (Entry<String, String> entry : params.entrySet()) {
                Object value = entry.getValue();
                String v = (value == null) ? "" : URLEncoder.encode(entry.getValue(), "UTF-8");

                urlSB.append(entry.getKey()).append("=").append(v).append("&");
            }

            // 1.3 最终地址
            String urlStrFinal = urlSB.toString();

            // 1.4 去除末尾的&
            if (urlStrFinal.endsWith("&")) {
                urlStrFinal = urlStrFinal.substring(0, urlStrFinal.length() - 1);
            }

            // 使用 HttpClient 5 推荐的方式：HttpClientResponseHandler 确保自动资源释放
            HttpGet get = new HttpGet(urlStrFinal);

            try (CloseableHttpClient http = HttpClients.createDefault()) {
                return http.execute(get, response -> {
                    HttpEntity entity = response.getEntity();

                    // 主体数据
                    try (InputStream in = entity.getContent();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                        // 读取
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        return sb.toString();
                    }
                });
            }
        } catch (IOException e) {
            throw new SysException(e);
        }
    }

    /**
     * 通过HTTPS请求获取json格式的返回
     *
     * @param urlStr
     * @param data
     * @return
     */
    public static String httpsGet(String urlStr, Map<String, String> params) {
        String html = null;
        try {
            if (params == null)
                params = new HashMap<>();

            StringBuilder sb = new StringBuilder(urlStr);

            if (!params.isEmpty()) {
                sb.append("?");
            }

            for (Entry<String, String> entry : params.entrySet()) {
                Object value = entry.getValue();
                String v = (value == null) ? "" : URLEncoder.encode(entry.getValue(), "UTF-8");

                sb.append(entry.getKey()).append("=").append(v).append("&");
            }

            String urlStrFinal = sb.toString();

            // 去除末尾的&
            if (urlStrFinal.endsWith("&")) {
                urlStrFinal = urlStrFinal.substring(0, urlStrFinal.length() - 1);
            }

            // 使用 HttpClient 5 推荐的方式：HttpClientResponseHandler 确保自动资源释放
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(urlStrFinal);

                // 使用 ResponseHandler 确保自动资源释放（HttpClient 5 推荐方式）
                html = httpClient.execute(httpGet, response -> {
                    HttpEntity httpEntity = response.getEntity();
                    if (httpEntity != null) {
                        return EntityUtils.toString(httpEntity);
                    }
                    return "";
                });
                return html;
            }
        } catch (IOException e) {
            throw new SysException("返回内容为:" + html, e);
        } catch (Exception e) {
            throw new SysException("HTTPS请求失败", e);
        }
    }

    /**
     * 进行Post请求操作
     *
     * @return
     */
    public static String httpPost(String url, Map<String, String> params) {
        try {
            // 参数 - 使用 HttpClient 5 的 API
            List<NameValuePair> nvps = new ArrayList<>();
            for (Entry<String, String> entry : params.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                String valStr = (val == null) ? "" : val.toString();

                nvps.add(new BasicNameValuePair(key.toString(), valStr));
            }

            // 请求地址 - 使用 HttpClient 5 的 API
            HttpPost post = new HttpPost(url);
            // 设置参数
            post.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));

            // 使用 HttpClient 5 推荐的方式：HttpClientResponseHandler 确保自动资源释放
            try (CloseableHttpClient http = HttpClients.createDefault()) {
                return http.execute(post, response -> {
                    HttpEntity entity = response.getEntity();

                    // 主体数据
                    try (InputStream in = entity.getContent();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                        // 读取
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        return sb.toString();
                    }
                });
            }
        } catch (IOException e) {
            throw new SysException(e);
        }
    }

    public static List<Integer> strToIntList(String str) {
        if (str == null || str.isEmpty())
            return new ArrayList<>();

        List<Integer> l = new ArrayList<>();
        String[] o = str.split(",");
        for (String s : o) {
            if (s.isEmpty())
                continue;
            l.add(Integer.valueOf(s));
        }

        return l;
    }

    /**
     * 将String，以“，”分割的字符串，转化为int[]
     * 
     * @param str
     * @return
     */
    public static int[] arrayStrToInt(String str) {
        if (StringUtils.isEmpty(str)) {
            return new int[0];
        }

        String skillLogicArrTemp[] = str.split(","); // 逻辑库的数组

        int[] skillLogicArr = new int[skillLogicArrTemp.length];
        for (int i = 0; i < skillLogicArrTemp.length; i++) {
            skillLogicArr[i] = Utils.intValue(skillLogicArrTemp[i]);
        }

        return skillLogicArr;
    }

    /**
     * 将String[]，转化为String，以“，”分割
     * 
     * @param str
     * @return
     */
    public static String arrayStrToStr(String[] arr) {
        if (arr.length == 0) {
            return "";
        }

        String result = "";
        for (String arr1 : arr) {
            result += (arr1 + ",");
        }

        return result.substring(0, result.length() - 1);
    }

    /**
     * 将int[]，转化为String，以“，”分割
     * 
     * @param str
     * @return
     */
    public static String arrayIntToStr(int[] arr) {
        if (arr.length == 0) {
            return "";
        }

        String result = "";
        for (int i = 0; i < arr.length; i++) {
            result += (arr[i] + ",");
        }

        return result.substring(0, result.length() - 1);
    }

    /**
     * 创建函数特征码
     * 类全路径:函数名(参数类型)
     * 
     * @return
     */
    public static String createMethodKey(Method method) {
        return createMethodKey(method.getDeclaringClass(), method);
    }

    /**
     * 创建函数特征码
     * 类全路径:函数名(参数类型)
     * 
     * @return
     */
    public static String createMethodKey(Class<?> cls, Method method) {
        // 类全路径
        String clazzName = cls.getName();
        // 函数名
        String methodName = method.getName();
        // 参数类型字符串
        StringBuilder methodParam = new StringBuilder();
        methodParam.append("(");
        for (Class<?> clazz : method.getParameterTypes()) {
            if (methodParam.length() > 1)
                methodParam.append(", ");
            methodParam.append(clazz.getSimpleName());
        }
        methodParam.append(")");

        return clazzName + ":" + methodName + methodParam.toString();
    }
}
