package org.gof.demo.gatewaysrv.crossserver;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.core.support.Param;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨服组队服务
 * 实现跨服务器玩家组队功能
 */
@DistrClass
public class TeamService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

    /** 队伍ID生成器 */
    private final AtomicLong teamIdGenerator;

    /** 队伍映射：teamId -> Team */
    private final Map<Long, Team> teams;

    /** 玩家队伍映射：humanId -> teamId */
    private final Map<Long, Long> playerTeams;

    /** 邀请映射：inviteId -> TeamInvite */
    private final Map<Long, TeamInvite> invites;

    /**
     * 队伍
     */
    public static class Team {
        /** 队伍ID */
        private final long teamId;
        /** 队长ID */
        private long leaderId;
        /** 成员列表 */
        private final List<TeamMember> members;
        /** 队伍类型 */
        private final int teamType;
        /** 最大人数 */
        private final int maxSize;
        /** 创建时间 */
        private final long createTime;
        /** 目标服务器（跨服时使用） */
        private volatile String targetServer;

        public Team(long teamId, long leaderId, int teamType, int maxSize) {
            this.teamId = teamId;
            this.leaderId = leaderId;
            this.teamType = teamType;
            this.maxSize = maxSize;
            this.members = new ArrayList<>();
            this.createTime = System.currentTimeMillis();

            // 队长加入
            addMember(leaderId, TeamRole.LEADER);
        }

        /**
         * 添加成员
         */
        public boolean addMember(long humanId, TeamRole role) {
            if (members.size() >= maxSize) {
                return false;
            }

            TeamMember member = new TeamMember(humanId, role);
            members.add(member);
            return true;
        }

        /**
         * 移除成员
         */
        public boolean removeMember(long humanId) {
            return members.removeIf(m -> m.humanId == humanId);
        }

        /**
         * 获取成员
         */
        public TeamMember getMember(long humanId) {
            return members.stream()
                    .filter(m -> m.humanId == humanId)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 是否已满
         */
        public boolean isFull() {
            return members.size() >= maxSize;
        }

        /**
         * 是否有成员
         */
        public boolean hasMember(long humanId) {
            return getMember(humanId) != null;
        }

        /**
         * 转让队长
         */
        public void transferLeader(long newLeaderId) {
            TeamMember oldLeader = getMember(leaderId);
            TeamMember newLeader = getMember(newLeaderId);

            if (oldLeader != null) {
                oldLeader.role = TeamRole.MEMBER;
            }
            if (newLeader != null) {
                newLeader.role = TeamRole.LEADER;
            }

            leaderId = newLeaderId;
        }

        /**
         * 获取成员数量
         */
        public int getMemberCount() {
            return members.size();
        }
    }

    /**
     * 队伍成员
     */
    public static class TeamMember {
        /** 玩家ID */
        private final long humanId;
        /** 角色 */
        private TeamRole role;
        /** 服务器ID */
        private String serverId;
        /** 加入时间 */
        private final long joinTime;

        public TeamMember(long humanId, TeamRole role) {
            this.humanId = humanId;
            this.role = role;
            this.joinTime = System.currentTimeMillis();
        }
    }

    /**
     * 队伍角色
     */
    public enum TeamRole {
        /** 队长 */
        LEADER,
        /** 副队长 */
        VICE_LEADER,
        /** 普通成员 */
        MEMBER
    }

    /**
     * 队伍邀请
     */
    public static class TeamInvite {
        /** 邀请ID */
        private final long inviteId;
        /** 队伍ID */
        private final long teamId;
        /** 邀请人 */
        private final long inviterId;
        /** 被邀请人 */
        private final long inviteeId;
        /** 邀请时间 */
        private final long inviteTime;

        public TeamInvite(long inviteId, long teamId, long inviterId, long inviteeId) {
            this.inviteId = inviteId;
            this.teamId = teamId;
            this.inviterId = inviterId;
            this.inviteeId = inviteeId;
            this.inviteTime = System.currentTimeMillis();
        }

        /** 是否过期（30秒有效期） */
        public boolean isExpired() {
            return System.currentTimeMillis() - inviteTime > 30000;
        }
    }

    /**
     * 构造函数
     */
    public TeamService(Port port) {
        super(port);
        this.teamIdGenerator = new AtomicLong(0);
        this.teams = new ConcurrentHashMap<>();
        this.playerTeams = new ConcurrentHashMap<>();
        this.invites = new ConcurrentHashMap<>();
    }

    @Override
    public Object getId() {
        return GatewayConfig.SERV_GATEWAY_TEAM;
    }

    /**
     * 创建队伍
     */
    @DistrMethod
    public long createTeam(long leaderId, int teamType) {
        if (!GatewayConfig.ENABLE_CROSS_SERVER_TEAM()) {
            logger.warn("跨服组队未启用");
            return -1;
        }

        // 检查玩家是否已在队伍中
        if (playerTeams.containsKey(leaderId)) {
            logger.warn("玩家已在队伍中：humanId={}", leaderId);
            return -1;
        }

        long teamId = teamIdGenerator.incrementAndGet();
        Team team = new Team(teamId, leaderId, teamType, GatewayConfig.getInstance().getTeamMaxSize());
        teams.put(teamId, team);
        playerTeams.put(leaderId, teamId);

        logger.info("创建队伍：teamId={}, leaderId={}, teamType={}", teamId, leaderId, teamType);

        // TODO: 通知玩家创建成功

        return teamId;
    }

    /**
     * 解散队伍
     */
    @DistrMethod
    public boolean disbandTeam(long teamId, long humanId) {
        Team team = teams.get(teamId);
        if (team == null) {
            logger.warn("队伍不存在：teamId={}", teamId);
            return false;
        }

        // 只有队长可以解散
        if (team.leaderId != humanId) {
            logger.warn("只有队长可以解散队伍：teamId={}, humanId={}", teamId, humanId);
            return false;
        }

        // 移除所有成员
        for (TeamMember member : team.members) {
            playerTeams.remove(member.humanId);
        }

        teams.remove(teamId);

        logger.info("解散队伍：teamId={}", teamId);

        // TODO: 通知所有队员队伍解散

        return true;
    }

    /**
     * 邀请玩家
     */
    @DistrMethod
    public long invitePlayer(long teamId, long inviterId, long inviteeId) {
        Team team = teams.get(teamId);
        if (team == null) {
            logger.warn("队伍不存在：teamId={}", teamId);
            return -1;
        }

        // 检查邀请权限
        TeamMember inviter = team.getMember(inviterId);
        if (inviter == null || inviter.role == TeamRole.MEMBER) {
            logger.warn("无邀请权限：teamId={}, inviterId={}", teamId, inviterId);
            return -1;
        }

        // 检查队伍是否已满
        if (team.isFull()) {
            logger.warn("队伍已满：teamId={}", teamId);
            return -1;
        }

        // 检查被邀请人是否已在队伍中
        if (playerTeams.containsKey(inviteeId)) {
            logger.warn("被邀请人已在队伍中：inviteeId={}", inviteeId);
            return -1;
        }

        long inviteId = System.currentTimeMillis();
        TeamInvite invite = new TeamInvite(inviteId, teamId, inviterId, inviteeId);
        invites.put(inviteId, invite);

        logger.info("邀请玩家：teamId={}, inviterId={}, inviteeId={}, inviteId={}",
                teamId, inviterId, inviteeId, inviteId);

        // TODO: 通知被邀请人

        return inviteId;
    }

    /**
     * 接受邀请
     */
    @DistrMethod
    public boolean acceptInvite(long inviteId, long humanId) {
        TeamInvite invite = invites.get(inviteId);
        if (invite == null) {
            logger.warn("邀请不存在：inviteId={}", inviteId);
            return false;
        }

        if (invite.inviteeId != humanId) {
            logger.warn("无权接受此邀请：inviteId={}, humanId={}", inviteId, humanId);
            return false;
        }

        if (invite.isExpired()) {
            logger.warn("邀请已过期：inviteId={}", inviteId);
            invites.remove(inviteId);
            return false;
        }

        Team team = teams.get(invite.teamId);
        if (team == null) {
            logger.warn("队伍不存在：teamId={}", invite.teamId);
            return false;
        }

        if (team.isFull()) {
            logger.warn("队伍已满：teamId={}", invite.teamId);
            return false;
        }

        // 加入队伍
        team.addMember(humanId, TeamRole.MEMBER);
        playerTeams.put(humanId, invite.teamId);
        invites.remove(inviteId);

        logger.info("接受邀请：teamId={}, humanId={}", invite.teamId, humanId);

        // TODO: 通知所有队员

        return true;
    }

    /**
     * 拒绝邀请
     */
    @DistrMethod
    public boolean declineInvite(long inviteId, long humanId) {
        TeamInvite invite = invites.remove(inviteId);
        if (invite == null) {
            return false;
        }

        logger.info("拒绝邀请：inviteId={}, humanId={}", inviteId, humanId);

        // TODO: 通知邀请人

        return true;
    }

    /**
     * 离开队伍
     */
    @DistrMethod
    public boolean leaveTeam(long teamId, long humanId) {
        Team team = teams.get(teamId);
        if (team == null) {
            return false;
        }

        // 队长不能直接离开，需要先转让或解散
        if (team.leaderId == humanId) {
            logger.warn("队长不能直接离开：teamId={}", teamId);
            return false;
        }

        team.removeMember(humanId);
        playerTeams.remove(humanId);

        logger.info("离开队伍：teamId={}, humanId={}", teamId, humanId);

        // TODO: 通知其他队员

        return true;
    }

    /**
     * 踢出队员
     */
    @DistrMethod
    public boolean kickMember(long teamId, long leaderId, long memberId) {
        Team team = teams.get(teamId);
        if (team == null) {
            return false;
        }

        // 只有队长可以踢人
        if (team.leaderId != leaderId) {
            logger.warn("只有队长可以踢人：teamId={}, leaderId={}", teamId, leaderId);
            return false;
        }

        // 不能踢自己
        if (leaderId == memberId) {
            return false;
        }

        if (!team.hasMember(memberId)) {
            return false;
        }

        team.removeMember(memberId);
        playerTeams.remove(memberId);

        logger.info("踢出队员：teamId={}, memberId={}", teamId, memberId);

        // TODO: 通知被踢玩家和其他队员

        return true;
    }

    /**
     * 转让队长
     */
    @DistrMethod
    public boolean transferLeader(long teamId, long leaderId, long newLeaderId) {
        Team team = teams.get(teamId);
        if (team == null) {
            return false;
        }

        if (team.leaderId != leaderId) {
            logger.warn("只有队长可以转让：teamId={}, leaderId={}", teamId, leaderId);
            return false;
        }

        if (!team.hasMember(newLeaderId)) {
            return false;
        }

        team.transferLeader(newLeaderId);

        logger.info("转让队长：teamId={}, oldLeader={}, newLeader={}", teamId, leaderId, newLeaderId);

        // TODO: 通知所有队员

        return true;
    }

    /**
     * 获取队伍信息
     */
    @DistrMethod
    public Param getTeamInfo(long teamId) {
        Team team = teams.get(teamId);
        if (team == null) {
            return null;
        }

        Param param = new Param();
        param.put("teamId", team.teamId);
        param.put("leaderId", team.leaderId);
        param.put("teamType", team.teamType);
        param.put("maxSize", team.maxSize);
        param.put("memberCount", team.getMemberCount());
        param.put("targetServer", team.targetServer);

        // 成员列表
        List<Param> members = new ArrayList<>();
        for (TeamMember member : team.members) {
            Param memberInfo = new Param();
            memberInfo.put("humanId", member.humanId);
            memberInfo.put("role", member.role.name());
            memberInfo.put("serverId", member.serverId);
            members.add(memberInfo);
        }
        param.put("members", members);

        return param;
    }

    /**
     * 心跳处理（清理过期邀请）
     */
    @Override
    public void pulseOverride() {
        // 清理过期邀请
        invites.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                logger.info("清理过期邀请：inviteId={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}
