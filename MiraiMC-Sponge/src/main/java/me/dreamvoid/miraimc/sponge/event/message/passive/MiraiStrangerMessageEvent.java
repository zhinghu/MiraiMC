package me.dreamvoid.miraimc.sponge.event.message.passive;

import me.dreamvoid.miraimc.httpapi.response.FetchMessage;
import org.spongepowered.api.event.Cause;
import net.mamoe.mirai.event.events.StrangerMessageEvent;

/**
 * (Sponge) 消息 - 被动收到消息 - 陌生人消息
 */
@SuppressWarnings("unused")
public class MiraiStrangerMessageEvent extends AbstractMessageEvent {
    public MiraiStrangerMessageEvent(StrangerMessageEvent event, Cause cause) {
        super(event, cause);
    }

    public MiraiStrangerMessageEvent(long BotID, FetchMessage.Data data, Cause cause) {
        super(BotID, data, cause);
    }
}
