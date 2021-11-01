package com.tongtongbigboy.lexer;

//转换函数，包括转换的开始状态，驱动字符，结束状态
public class TransformFunction {
    private String startState;
    private String driverChar;
    private String endState;
    public TransformFunction(String startState, String driverChar, String endState) {
        super();
        this.startState = startState;
        this.driverChar = driverChar;
        this.endState = endState;
    }

    public String getStartState() {
        return startState;
    }

    public void setStartState(String startState) {
        this.startState = startState;
    }

    public String getDriverChar() {
        return driverChar;
    }

    public void setDriverChar(String driverChar) {
        this.driverChar = driverChar;
    }

    public String getEndState() {
        return endState;
    }

    public void setEndState(String endState) {
        this.endState = endState;
    }
}
