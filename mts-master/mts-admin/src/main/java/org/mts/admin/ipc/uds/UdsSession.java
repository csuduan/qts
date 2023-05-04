package org.mts.admin.ipc.uds;

import io.netty.channel.Channel;
import lombok.Data;

@Data
public class UdsSession {
    private String name;
    private String id;
    private Channel channel;
}
