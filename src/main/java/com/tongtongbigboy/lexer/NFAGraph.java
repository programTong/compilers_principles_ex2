package com.tongtongbigboy.lexer;

import java.util.List;

/**
 * 表示 NFA 对应的转换图
 */
public class NFAGraph {
    // 开始状态节点
    private NFAState startState;
    // 结束状态节点，注意它不一定是终止状态，
    // 一般过程图的结束状态节点就不是终止状态，一般最后大转换图的结束状态才是终止状态。
    private NFAState endState;

    private NFAGraph() {
    }

    private NFAGraph(NFAState startState, NFAState endState) {
        this.startState = startState;
        this.endState = endState;
    }

    // 对应 Thompson 算法基础规则中的，遇到字符 a
    public static NFAGraph createByPath(String path) {
        // 创建开始和终止状态节点
        NFAState newStart = NFAState.create();
        NFAState newEnd = NFAState.create();
        // 添加一条开始到终止状态节点的有向边
        newStart.addEdge(path, newEnd);
        return new NFAGraph(newStart, newEnd);
    }


    /**
     * 对应运算符[], [abA-C]
     * @param charList 例子[abA-Z]对应charList{'a','b','A','B','C'}
     * @return 返回字符范围对应的nfa
     */
    public static NFAGraph createRange(List<Character> charList){
        NFAState newStart = NFAState.create();
        NFAState newEnd = NFAState.create();
        for (Character ch : charList) {
            NFAGraph nfaGraph = NFAGraph.createByPath(ch + "");
            newStart.addEdge(NFAState.EPSILON,nfaGraph.getStartState());
            nfaGraph.getEndState().addEdge(NFAState.EPSILON,newEnd);
        }
        return new NFAGraph(newStart,newEnd);
    }

    // 对应操作符 &; 对应 Thompson 算法归纳规则中的连接操作
    public void addSerial(NFAGraph nextGraph) {
        // 将本转换图的结束状态节点，添加一个 ε有向边 连接到下一个本转换图开始节点
        this.endState.addEdge(NFAState.EPSILON, nextGraph.startState);
        // 更新一个本转换图的结束状态节点，就得到一个新的转换图了。
        this.endState = nextGraph.endState;
    }

    // 对应操作符 |; 对应 Thompson 算法归纳规则中的并操作
    public void addParallel(NFAGraph nextGraph) {
        // 创建新的开始和终止状态节点
        NFAState newStart = NFAState.create();
        NFAState newEnd = NFAState.create();
        // 根据 Thompson 算法，我们要添加四条 ε有向边
        newStart.addEdge(NFAState.EPSILON, this.startState);
        newStart.addEdge(NFAState.EPSILON, nextGraph.startState);
        this.endState.addEdge(NFAState.EPSILON, newEnd);
        nextGraph.endState.addEdge(NFAState.EPSILON, newEnd);

        // 更新本转换图的开始和结束状态节点，得到一个新的转换图了。
        this.startState = newStart;
        this.endState = newEnd;
    }

    // 对应操作符 * 即0次以上
    public void repeatStar() {
        // 将 * 分为1次以上和0次
        repeatPlus();
        zero();
    }

    // 对应操作符 + 即一次以上
    public void repeatPlus() {
        // 创建新的开始和终止状态节点
        NFAState newStart = NFAState.create();
        NFAState newEnd = NFAState.create();
        // 根据 Thompson 算法，我们要添加三条 ε有向边
        newStart.addEdge(NFAState.EPSILON, this.startState);
        this.endState.addEdge(NFAState.EPSILON, newEnd);
        this.endState.addEdge(NFAState.EPSILON, this.startState);

        // 更新本转换图的开始和结束状态节点，得到一个新的转换图了。
        this.startState = newStart;
        this.endState = newEnd;
    }

    // 对应0次
    public void zero() {
        // 添加 ε有向边
        this.startState.addEdge(NFAState.EPSILON, this.endState);
    }

    //对应1次
    public void one(){
        //空操作，无须变动
    }

    //对应运算符?，0次或1次
    public void addOptional(){
        zero();
        one();
    }


    public NFAState getStartState() {
        return startState;
    }

    public NFAState getEndState() {
        return endState;
    }

}
