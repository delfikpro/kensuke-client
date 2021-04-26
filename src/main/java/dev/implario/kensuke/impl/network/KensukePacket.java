package dev.implario.kensuke.impl.network;

import dev.implario.kensuke.impl.network.packet.*;

import java.util.UUID;

public interface KensukePacket {

	default KensukePacketFrame toFrame(UUID uuid) {
		return new KensukePacketFrame(getType(this.getClass()), this, uuid);
	}

	static Class<? extends KensukePacket> getClass(String type) {
		switch (type) {
			case "ok": return PacketOk.class;
			case "error": return PacketError.class;
			case "auth": return PacketAuth.class;
			case "createSession": return PacketCreateSession.class;
			case "endSession": return PacketEndSession.class;
			case "syncData": return PacketSyncData.class;
			case "requestSync": return PacketRequestSync.class;
			case "useScopes": return PacketUseScopes.class;
			case "keepAlive": return PacketKeepAlive.class;
			case "requestLeaderboard": return PacketRequestLeaderboard.class;
			case "leaderboardState": return PacketLeaderboardState.class;
			case "requestSnapshot": return PacketRequestSnapshot.class;
			case "dataSnapshot": return PacketDataSnapshot.class;
			default: throw new IllegalArgumentException("No packet class for type " + type);
		}
	}

	static String getType(Class<? extends KensukePacket> clazz) {
		if (clazz == PacketOk.class) return "ok";
		if (clazz == PacketError.class) return "error";
		if (clazz == PacketAuth.class) return "auth";
		if (clazz == PacketCreateSession.class) return "createSession";
		if (clazz == PacketEndSession.class) return "endSession";
		if (clazz == PacketSyncData.class) return "syncData";
		if (clazz == PacketRequestSync.class) return "requestSync";
		if (clazz == PacketUseScopes.class) return "useScopes";
		if (clazz == PacketKeepAlive.class) return "keepAlive";
		if (clazz == PacketRequestLeaderboard.class) return  "requestLeaderboard";
		if (clazz == PacketLeaderboardState.class) return  "leaderboardState";
		if (clazz == PacketRequestSnapshot.class) return  "requestSnapshot";
		if (clazz == PacketDataSnapshot.class) return  "dataSnapshot";
		throw new IllegalArgumentException("No packet type for class " + clazz.getName());
	}

}
