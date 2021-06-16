package dev.implario.kensuke.impl.packet;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PacketCreateSession {

	private final UUID session;

	// Name 'playerId' is used for backwards compatibility
	@SerializedName("playerId")
	private final String id;

	private final List<String> scopes;

}
