package com.tongtongbigboy.lexer;

import java.util.Objects;
import java.util.Set;

/**
 * 表示 DFA 转换图中的状态
 */
public class DFAState{

    // 对应的 NFA 转换图中的状态集合
    private Set<NFAState> stateSet;
    // NFA 转换图中的状态集合对应的唯一标志，用来两个 DFA 是否相等
    private String statesId;

    // 表示当前状态是不是终止状态
    private boolean isEnd;
    private boolean isStart;
    // 这个 tag 只是辅助作用，
    private boolean isTag;

    private DFAState(Set<NFAState> stateSet, String statesId, boolean isEnd,boolean isStart) {
        this.stateSet = stateSet;
        this.statesId = statesId;
        this.isEnd = isEnd;
        this.isStart = isStart;
    }

    public DFAState(String statesId, boolean isEnd,boolean isStart) {
        this.statesId = statesId;
        this.isEnd = isEnd;
        this.isStart = isStart;
    }

    /**
     * 通过 NFA 转换图中的状态集合生成对应的 DFA 状态
     * @param stateSet
     * @return
     */
    public static DFAState create(Set<NFAState> stateSet) {
        StringBuilder idBuilder = new StringBuilder();
        // 生成对应 DFA 状态的 id 标志
        stateSet.stream().sorted().forEach(state -> idBuilder.append(state.getId()+","));
        boolean isEnd = false;
        for (NFAState state : stateSet) {
            // 如果 stateSet 集合中有一个状态节点是终止状态节点，
            // 那么这个新生成的 DFA 状态节点也是终止状态节点
            if (state.isEnd()) {
                isEnd = true;
                break;
            }
        }

        boolean isStart = false;
        for (NFAState state : stateSet) {
            // 如果 stateSet 集合中有一个状态节点是终止状态节点，
            // 那么这个新生成的 DFA 状态节点也是终止状态节点
            if (state.isStart()) {
                isStart = true;
                break;
            }
        }

        return new DFAState(stateSet, idBuilder.toString(), isEnd,isStart);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DFAState dfaNFAState = (DFAState) o;
        return statesId.equals(dfaNFAState.statesId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statesId);
    }

    public void setTag(boolean tag) {
        isTag = tag;
    }

    public Set<NFAState> getNFAStateSet() {
        return stateSet;
    }

    public String getNFAStatesId() {
        return statesId;
    }

    public boolean isTag() {
        return isTag;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DFANFAState{");
        sb.append("statesId='").append(statesId).append('\'');
        sb.append(", isEnd=").append(isEnd);
        sb.append('}');
        return sb.toString();
    }
}
