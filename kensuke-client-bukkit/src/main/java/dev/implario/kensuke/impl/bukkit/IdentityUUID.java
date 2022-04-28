package dev.implario.kensuke.impl.bukkit;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Due to lack of a better option to distinguish players during pre-login,
 * Kensuke uses pointers to UUID objects, which are both unique and constant throughout any session.
 */
@RequiredArgsConstructor
public class IdentityUUID {

    public static final boolean DISABLE_IDENTITY_CHECK = System.getenv("KENSUKE_DISABLE_IDENTITY") != null;

    @NonNull
    private final UUID uuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityUUID that = (IdentityUUID) o;

        // Identity comparison
        return DISABLE_IDENTITY_CHECK ? uuid.equals(that.uuid) : uuid == that.uuid;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
