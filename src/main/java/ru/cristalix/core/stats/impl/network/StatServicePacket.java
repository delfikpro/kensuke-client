package ru.cristalix.core.stats.impl.network;

import ru.cristalix.core.stats.impl.network.packet.*;

import java.util.UUID;

public interface StatServicePacket {

	default StatServicePacketFrame toFrame(UUID uuid) {
		return new StatServicePacketFrame(getType(this.getClass()), this, uuid);
	}

	static Class<? extends StatServicePacket> getClass(String type) {
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
			default: throw new IllegalArgumentException("No packet class for type " + type);
		}
	}

	static String getType(Class<? extends StatServicePacket> clazz) {
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
		throw new IllegalArgumentException("No packet type for class " + clazz.getName());
	}

}
