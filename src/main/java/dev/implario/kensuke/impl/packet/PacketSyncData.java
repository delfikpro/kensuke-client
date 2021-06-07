package dev.implario.kensuke.impl.packet;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class PacketSyncData {

	private final UUID session;

	// Name 'stats' is used for backwards compatibility
	@SerializedName("stats")
	private final Map<String, JsonElement> data;

}
