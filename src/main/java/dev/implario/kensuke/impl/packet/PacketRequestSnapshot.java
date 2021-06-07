package dev.implario.kensuke.impl.packet;

import dev.implario.kensuke.Scope;
import lombok.Data;

import java.util.List;

@Data
public class PacketRequestSnapshot {

    private final String id;
    private final List<Scope<?>> scopes;

}
