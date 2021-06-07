package dev.implario.kensuke.impl.packet;

import lombok.Data;

@Data
public class PacketAuth {

    private final String login;
    private final String password;
    private final String nodeName;
    private final int version = 1;

}
