package me.dreamvoid.miraimc.velocity.event;

import net.mamoe.mirai.event.events.BotNickChangedEvent;

/**
 * Bot 昵称改变
 */
public class MiraiBotNickChangedEvent {

    public MiraiBotNickChangedEvent(BotNickChangedEvent event) {
        this.event = event;
    }

    private final BotNickChangedEvent event;

    /**
     * 获取机器人账号
     * @return 机器人账号
     */
    public long getID() { return event.getBot().getId(); }

    /**
     * 获取机器人更换前的昵称
     * @return 机器人更换前的昵称
     */
    public String getOldNick() { return event.getFrom(); }

    /**
     * 获取机器人更换后的昵称
     * @return 机器人更换后的昵称
     */
    public String getNewNick() { return event.getTo(); }

    /**
     * 获取原始事件内容<br>
     * [!] 不推荐使用
     * @return 原始事件内容
     */
    public String eventToString() {
        return event.toString();
    }
}
