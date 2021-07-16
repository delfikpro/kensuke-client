package dev.implario.kensuke;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class KensukeUser implements IKensukeUser {

    protected transient final KensukeSession session;

}
