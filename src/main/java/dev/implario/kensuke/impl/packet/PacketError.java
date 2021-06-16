package dev.implario.kensuke.impl.packet;

import dev.implario.nettier.RemoteException;
import lombok.Data;

@Data
public class PacketError {

    private final RemoteException.ErrorLevel errorLevel;
    private final String errorMessage;

}
