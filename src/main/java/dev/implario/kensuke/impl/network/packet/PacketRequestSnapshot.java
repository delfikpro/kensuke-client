package dev.implario.kensuke.impl.network.packet;

import dev.implario.kensuke.impl.network.KensukePacket;
import dev.implario.kensuke.scope.Scope;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PacketRequestSnapshot implements KensukePacket {

    private final UUID id;
    private final List<Scope<?>> scopes;

}
