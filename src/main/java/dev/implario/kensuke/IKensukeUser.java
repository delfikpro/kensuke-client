package dev.implario.kensuke;

public interface IKensukeUser {

    Session getSession();

    default String getId() {
        return getSession().getUserId();
    }

}
