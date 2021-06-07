package dev.implario.kensuke.impl;

import dev.implario.nettier.RemoteException;
import dev.implario.nettier.RemoteException.ErrorLevel;
import lombok.Getter;

@Getter
public class KensukeException extends RuntimeException {

    private final ErrorLevel errorLevel;

    public KensukeException(ErrorLevel errorLevel, String s) {
        super(s);
        this.errorLevel = errorLevel;
    }

    public KensukeException(ErrorLevel errorLevel, String s, Throwable throwable) {
        super(s, throwable);
        this.errorLevel = errorLevel;
    }

}
