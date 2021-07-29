package me.dreamvoid.miraimc.bungee.event;

import net.mamoe.mirai.event.events.FriendRemarkChangeEvent;
import net.md_5.bungee.api.plugin.Event;

/**
 * 好友昵称改变
 */
public class MiraiFriendRemarkChangeEvent extends Event {
    public MiraiFriendRemarkChangeEvent(FriendRemarkChangeEvent event) {
        this.event = event;
    }

    private final FriendRemarkChangeEvent event;

    /**
     * 获取机器人账号
     * @return 机器人账号
     */
    public long getBotID() { return event.getBot().getId(); }

    /**
     * 获取好友更名之前的昵称
     * @return 昵称
     */
    public String getOldRemark() {
        return event.getOldRemark();
    }

    /**
     * 获取好友更名之后的昵称
     * @return 昵称
     */
    public String getNewRemark() {
        return event.getNewRemark();
    }

    /**
     * 获取哈希值
     * @return 哈希值
     */
    public int getHashCode() {
        return event.hashCode();
    }
}