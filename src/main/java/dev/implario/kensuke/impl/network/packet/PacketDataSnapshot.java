package dev.implario.kensuke.impl.network.packet;

import com.google.gson.JsonElement;
import dev.implario.kensuke.impl.network.KensukePacket;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class PacketDataSnapshot implements KensukePacket {

    private final UUID id;
    private final String name;
    private final Map<String, JsonElement> stats; //TODO: rename to data

}
