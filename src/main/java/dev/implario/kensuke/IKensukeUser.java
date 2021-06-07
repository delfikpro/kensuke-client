package dev.implario.kensuke;

import java.util.UUID;

public interface IKensukeUser {

    Session getSession();

    default UUID getId() {
        return getSession().getUserId();
    }

}
