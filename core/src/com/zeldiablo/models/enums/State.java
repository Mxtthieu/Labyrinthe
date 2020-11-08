package com.zeldiablo.models.enums;

public enum State {

    IN_PROGRESS('I'),
    WINNED('W'),
    LOST('L'),
    PAUSED('P'),
    RESET('R'),
    STOPPED('S');

    private char state;

    State(char c) { this.state = c; }

}
