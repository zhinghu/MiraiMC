package me.dreamvoid.miraimc.velocity.event;

import net.mamoe.mirai.event.events.BotGroupPermissionChangeEvent;

/**
 * 机器人在群里的权限被改变
 */
public class MiraiGroupBotPermissionChangeEvent {
    // 主动退群
    public MiraiGroupBotPermissionChangeEvent(BotGroupPermissionChangeEvent event) {
        this.event = event;
    }


    private final BotGroupPermissionChangeEvent event;

    /**
     * 获取机器人账号
     * @return 机器人账号
     */
    public long getBotID() { return event.getBot().getId(); }

    /**
     * 返回目标群的群号
     * @return 群号
     */
    public long getGroupID() { return event.getGroupId(); }

    /**
     * 返回机器人的原有权限。
     * @return 0 - 普通成员 | 1 - 管理员 | 2 - 群主
     */
    public int getOriginPermission() {
        return event.getOrigin().getLevel();
    }

    /**
     * 返回机器人的新权限。
     * @return 0 - 普通成员 | 1 - 管理员 | 2 - 群主
     */
    public int getNewPermission() {
        return event.getNew().getLevel();
    }

    /**
     * 获取原始事件内容<br>
     * [!] 不推荐使用
     * @return 原始事件内容
     */
    public String eventToString() {
        return event.toString();
    }
}