package dev.implario.kensuke.impl.packet;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;

@Data
public class PacketDataSnapshot {

    private final String id;

    // Name 'stats' is used for backwards compatibility
    @SerializedName("stats")
    private final Map<String, JsonElement> data;

}