package dev.implario.kensuke;

import lombok.Data;

@Data
public class LeaderboardEntry<T> {

    private final int position;
    private final T data;

}
