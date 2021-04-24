package network.palace.core.messagequeue.packets;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class PacketID {

    @AllArgsConstructor
    enum Global {
        BROADCAST(1), MESSAGEBYRANK(2), PROXYRELOAD(3), DM(4), MESSAGE(5), COMPONENTMESSAGE(6),
        CLEARCHAT(7), CREATESERVER(8), DELETESERVER(9), MENTION(10), IGNORE_LIST(11), CHAT(12),
        CHAT_ANALYSIS(13), CHAT_ANALYSIS_RESPONSE(14), SEND_PLAYER(15), CHANGE_CHANNEL(16), CHAT_MUTED(17),
        MENTIONBYRANK(18), KICK_PLAYER(19), KICK_IP(20), MUTE_PLAYER(21), BAN_PROVIDER(22), FRIEND_JOIN(23),
        PARK_STORAGE_LOCK(24), REFRESH_WARPS(25), MULTI_SHOW_START(26), MULTI_SHOW_STOP(27), CREATE_QUEUE(28),
        REMOVE_QUEUE(29), UPDATE_QUEUE(30), PLAYER_QUEUE(31), BROADCAST_COMPONENT(32), EMPTY_SERVER(33),
        RANK_CHANGE(34), LOG_STATISTIC(35), DISCORD_BOT_RANKS(1);

        @Getter private final int id;
    }
}
