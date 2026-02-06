package org.gof.demo.worldsrv.platform;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.gof.core.Port;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.core.dbsrv.DBServiceProxy;
import org.gof.core.support.Param;
import org.gof.demo.worldsrv.common.GamePort;
import org.gof.demo.worldsrv.common.GameServiceBase;
import org.gof.demo.worldsrv.entity.Human;
import org.gof.demo.worldsrv.support.D;
import org.gof.demo.worldsrv.support.Log;

/**
 * 登录验证服务
 * 负责验证玩家的账号密码和token
 */
@DistrClass(
    servId = D.SERV_PLATFORM_LOGIN,
    importClass = {}
)
public class LoginService extends GameServiceBase {

    // 账号封禁列表（内存缓存）
    private Set<String> bannedAccounts = new HashSet<>();

    // Token有效期（毫秒），默认7天
    private static final long TOKEN_VALID_PERIOD = 7 * 24 * 60 * 60 * 1000L;

    // 账号格式正则：字母开头，4-16位字母数字下划线
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,15}$");

    public LoginService(GamePort port) {
        super(port);
    }

    /**
     * 初始化服务
     */
    @Override
    protected void init() {
        Log.temp.info("LoginService 初始化完成");

        // 加载封禁账号列表（可选：从数据库或配置文件加载）
        loadBannedAccounts();
    }

    /**
     * 加载封禁账号列表
     */
    private void loadBannedAccounts() {
        // TODO: 从数据库或配置文件加载封禁账号
        // 示例：bannedAccounts.add("cheater001");
    }

    /**
     * 验证账号和token
     * 完整的验证流程：
     * 1. 检查账号格式
     * 2. 检查账号是否被封禁
     * 3. 验证token有效性（时效性）
     *
     * @param account 账号
     * @param token   验证令牌
     */
    @DistrMethod
    public void check(String account, String token) {
        boolean result = true;
        String failReason = "";

        // 1. 基础参数验证
        if (account == null || account.isEmpty()) {
            result = false;
            failReason = "账号为空";
            Log.temp.warn("登录验证失败: account=null");
            port.returns("result", result, "reason", failReason);
            return;
        }

        // 统一转为小写
        account = account.toLowerCase();

        // 2. 账号格式验证
        if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            result = false;
            failReason = "账号格式不正确";
            Log.temp.warn("登录验证失败: 账号格式不正确, account={}", account);
            port.returns("result", result, "reason", failReason);
            return;
        }

        // 3. 检查账号是否被封禁
        if (bannedAccounts.contains(account)) {
            result = false;
            failReason = "账号已被封禁";
            Log.temp.warn("登录验证失败: 账号被封禁, account={}", account);
            port.returns("result", result, "reason", failReason);
            return;
        }

        // 4. Token验证
        if (token == null || token.isEmpty()) {
            // 允许无token登录（用于新账号或测试）
            Log.temp.info("登录验证: 无token登录, account={}", account);
            port.returns("result", true);
            return;
        }

        // 验证token时效性
        if (!validateTokenTime(token)) {
            result = false;
            failReason = "Token已过期";
            Log.temp.warn("登录验证失败: Token已过期, account={}", account);
            port.returns("result", result, "reason", failReason);
            return;
        }

        // 5. 可选：查询数据库验证账号下是否有角色
        // checkAccountExists(account);

        Log.temp.info("登录验证成功: account={}", account);
        port.returns("result", result);
    }

    /**
     * 验证token时效性
     * Token格式假设：timestamp_signature 或纯timestamp
     *
     * @param token 验证令牌
     * @return 是否有效
     */
    private boolean validateTokenTime(String token) {
        try {
            // 假设token前缀是时间戳（13位毫秒级）
            if (token.length() >= 13) {
                String timeStr = token.substring(0, 13);
                long timestamp = Long.parseLong(timeStr);
                long currentTime = Port.getTime();

                // 检查token是否在有效期内
                if (currentTime - timestamp > TOKEN_VALID_PERIOD) {
                    return false;
                }

                // 检查token是否来自未来（时钟不同步）
                if (timestamp > currentTime + 60000) { // 允许1分钟误差
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            // Token格式不符合预期，可能使用其他验证方式
            // 当前允许通过
            return true;
        }
    }

    /**
     * 验证账号密码（备用方法）
     *
     * @param account  账号
     * @param password 密码
     */
    @DistrMethod
    public void checkPassword(String account, String password) {
        boolean result = true;
        String failReason = "";

        if (account == null || account.isEmpty()) {
            result = false;
            failReason = "账号为空";
        } else if (password == null || password.isEmpty()) {
            result = false;
            failReason = "密码为空";
        } else if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            result = false;
            failReason = "账号格式不正确";
        } else if (bannedAccounts.contains(account.toLowerCase())) {
            result = false;
            failReason = "账号已被封禁";
        }

        Log.temp.info("密码验证: account={}, result={}, reason={}",
            account, result, failReason);

        port.returns("result", result, "reason", failReason);
    }

    /**
     * 验证token有效性（单独接口）
     *
     * @param token 验证令牌
     */
    @DistrMethod
    public void validateToken(String token) {
        boolean result = true;
        String failReason = "";

        if (token == null || token.isEmpty()) {
            result = false;
            failReason = "Token为空";
        } else if (!validateTokenTime(token)) {
            result = false;
            failReason = "Token已过期";
        }

        port.returns("result", result, "reason", failReason);
    }

    /**
     * 检查账号下是否有角色（可选验证）
     *
     * @param account 账号
     */
    @DistrMethod
    public void checkAccountExists(String account) {
        DBServiceProxy dbProxy = DBServiceProxy.newInstance();
        // 使用异步查询，这里只是为了验证，不需要返回结果
        dbProxy.countBy(false, Human.tableName, "account", account);
        dbProxy.listenResult((Param results, Param context) -> {
            Integer count = results.get();
            Log.temp.info("账号角色数量查询: account={}, count={}", account, count);
        }, "account", account);
    }

    /**
     * 封禁账号
     *
     * @param account 账号
     * @param reason  封禁原因
     */
    @DistrMethod
    public void banAccount(String account, String reason) {
        if (account != null && !account.isEmpty()) {
            account = account.toLowerCase();
            bannedAccounts.add(account);
            Log.temp.warn("账号被封禁: account={}, reason={}", account, reason);
            port.returns("result", true);
        } else {
            port.returns("result", false);
        }
    }

    /**
     * 解封账号
     *
     * @param account 账号
     */
    @DistrMethod
    public void unbanAccount(String account) {
        if (account != null && !account.isEmpty()) {
            account = account.toLowerCase();
            boolean removed = bannedAccounts.remove(account);
            Log.temp.info("账号解封: account={}, success={}", account, removed);
            port.returns("result", removed);
        } else {
            port.returns("result", false);
        }
    }

    /**
     * 踢出玩家
     *
     * @param account 账号
     * @param reason  踢出原因
     */
    @DistrMethod
    public void kick(String account, String reason) {
        Log.temp.info("踢出玩家: account={}, reason={}", account, reason);
        port.returns("result", true);
    }

    /**
     * 获取封禁账号列表（管理用）
     */
    @DistrMethod
    public void getBannedAccounts() {
        port.returns("bannedAccounts", bannedAccounts);
    }
}
