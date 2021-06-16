package dev.implario.kensuke.impl.packet;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PacketAuth {

    private final String login;
    private final String password;
    private final String nodeName;
    private final int version = 1;
    private final List<UUID> activeSessions;

}
