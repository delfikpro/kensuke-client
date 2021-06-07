package dev.implario.kensuke;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class KensukeUser implements IKensukeUser {

    private final Session session;

}
