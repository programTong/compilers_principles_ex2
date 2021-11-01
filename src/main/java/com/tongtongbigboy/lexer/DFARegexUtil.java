package com.tongtongbigboy.lexer;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * DFA工具类，有许多方法。方法包括：子集构造、nfa转dfa...
 */
public class DFARegexUtil {

    public static final Set<NFAState> EMPTY = new HashSet<>();

    /**
     * 得到 DFA 状态节点 经过 path 后得到的所有 NFA 状态节点集合。
     * @param tState
     * @param path
     * @return
     */
    public static Set<NFAState> edge(DFAState tState, String path) {
        Set<NFAState> resultSet = new HashSet<>();
        // 当前 DFA 状态对应的 NFA 状态集合
        for (NFAState state : tState.getNFAStateSet()) {
            if (state.getEdges().containsKey(path)) {
                resultSet.addAll(state.getEdges().get(path));
            }
        }
        return resultSet;
    }

    /**
     * 得到 ε-closure 的 NFA 状态集合
     * @param state
     * @return
     */
    public static Set<NFAState> closure(NFAState state) {
        Set<NFAState> states = new HashSet<>();
        states.add(state);
        return closure(states);
    }

    /**
     * 得到 ε-closure 的 NFA 状态集合
     * @param states
     * @return
     */
    public static Set<NFAState> closure(Set<NFAState> states) {
        Stack<NFAState> stack = new Stack<>();
        stack.addAll(states);
        Set<NFAState> closureSet = new HashSet<>(states);
        while (!stack.isEmpty()) {
            NFAState state = stack.pop();
            // 得到状态 state 对应的空边 状态集合
            Set<NFAState> epsilonSet = state.getEdges().getOrDefault(NFAState.EPSILON, EMPTY);
            for (NFAState epsilonState : epsilonSet) {
                // 如果不存在，就是新发现的状态，添加到 closureSet 和 stack 中
                if (!closureSet.contains(epsilonState)) {
                    closureSet.add(epsilonState);
                    stack.push(epsilonState);
                }
            }
        }
        return closureSet;
    }

    /**
     * 从 dfaStates 集合中寻找一个未标记的 DFA 状态节点
     * @param dfaStates
     * @return
     */
    public static DFAState getNoTagState(Set<DFAState> dfaStates) {
        for (DFAState state : dfaStates) {
            if (!state.isTag()) {
                return state;
            }
        }
        return null;
    }

    /**
     * NFA 转换成 DFA
     * @param nfaGraph
     * @return
     */
    public static DFAGraph NFAToDFA(NFAGraph nfaGraph) {
        //构造paths
        List<String> paths = new ArrayList<>();
        NFARegexUtil.generateStateListAndPathListFromNFAGraph(new ArrayList<>(),paths,nfaGraph);
        //移除epsilon
        paths.remove("epsilon");

        // 创建开始的 DFA 状态
        DFAState startDFAState = DFAState.create(closure(nfaGraph.getStartState()));
        // 创建 DFAGraph 图
        DFAGraph dfaGraph = DFAGraph.create(startDFAState);
        // 这个集合记录所有生成的 DFA 状态节点
        Set<DFAState> dfaStates = new HashSet<>();
        // 将开始状态节点添加到 dfaStates 中
        dfaStates.add(startDFAState);

        DFAState TState;
        // 从 dfaStates 集合中寻找一个未标记的 DFA 状态节点
        while ((TState = getNoTagState(dfaStates)) != null) {
            // 进行标记，防止重复遍历
            TState.setTag(true);
            dfaGraph.addAcceptOrNonStates(TState);
            // 遍历输入字符
            for (String path : paths) {
                // 创建新的 DFA 状态节点
                DFAState UState = DFAState.create(closure(edge(TState, path)));
                // 不包含就添加
                if (!dfaStates.contains(UState)) {
                    dfaStates.add(UState);
                }
                // 添加转换表
                dfaGraph.addStateTable(TState, path, UState);
            }

        }
        dfaGraph.setPaths(paths);
        return dfaGraph;
    }

    /**
     * DFAGraph转Map<String, List<String>>，目的是给画图工具类GraphvizUtil.createStateGraph(..)传递数据。
     * 数据格式为: statesId@是否为接受态 => [statesId@路径@是否为接受态, statesId@路径@是否为接受态]
     * 例如： 1,2,@false => ['3,4,@a@true' , '4,5,@b@false']
     * @param dfaGraph
     * @return
     */
    public static Map<String, List<String>> toMapList(DFAGraph dfaGraph){
        Map<String, List<String>> mapList = new HashMap<>();
        Map<DFAState, Map<String, DFAState>> stateTable = dfaGraph.getStateTable();
        //遍历stateTable
        stateTable.forEach(new BiConsumer<DFAState, Map<String, DFAState>>() {
            @Override
            public void accept(DFAState dfaState, Map<String, DFAState> stringDFAStateMap) {
                List<String> subList = new ArrayList<>();
                stringDFAStateMap.forEach(new BiConsumer<String, DFAState>() {
                    @Override
                    public void accept(String s, DFAState dfaState) {
                        //如果目的状态存在且路劲不为epsilon
                        if (!dfaState.getNFAStatesId().equals("")&&!"epsilon".equals(s)){
                            subList.add(dfaState.getNFAStatesId()+"@"+s+"@"+dfaState.isEnd());
                        }

                    }
                });
                mapList.put(dfaState.getNFAStatesId()+"@"+dfaState.isEnd(),subList);
            }
        });

        return mapList;
    }

    /**
     * nfa转为c语言代码, 双层case嵌套
     * @param dfaGraph
     * @return
     */
    public static String toCCode(DFAGraph dfaGraph){
        //获取状态转换表，数据源
        Map<DFAState, Map<String, DFAState>> stateTable = dfaGraph.getStateTable();
        //需要用数字代表一个状态。构造intToState、stateToInt
        Map<Integer,DFAState> intToState = new HashMap<>();
        Map<DFAState,Integer> stateToInt = new HashMap<>();
        int id = 1;
        for (DFAState state : stateTable.keySet()) {
            int a = id;
            intToState.put(a,state);
            stateToInt.put(state,a);
            id++;
        }

        //确定开始状态的数字id
        int startId = 0;
        for (DFAState state : stateTable.keySet()) {
            if (state.isStart()) {
                startId = stateToInt.get(state);
                break;
            }
        }
        //代码简单，不用描述。
        StringBuilder sum = new StringBuilder();
        sum.append("int state = "+startId+";\n");
        sum.append("char input;\n");
        sum.append("switch(state){\n");
        sum.append("\tcase -1:\n");
        sum.append("\t\thandleError();\n");
        sum.append("\t\tbreak;\n");
        stateTable.forEach(new BiConsumer<DFAState, Map<String, DFAState>>() {
            @Override
            public void accept(DFAState dfaState, Map<String, DFAState> stringDFAStateMap) {
                StringBuilder sub = new StringBuilder();
                sub.append("\tcase "+stateToInt.get(dfaState)+":\n");
                sub.append("\t\tinput = next();\n");
                sub.append("\t\tswitch(input){\n");
                stringDFAStateMap.forEach(new BiConsumer<String, DFAState>() {
                    @Override
                    public void accept(String s, DFAState dfaState) {
                        StringBuilder subsub = new StringBuilder();
                        subsub.append("\t\t\tcase "+s+":\n");
                        subsub.append("\t\t\t\tstate = "+stateToInt.get(dfaState)+";\n");
                        subsub.append("\t\t\t\tbreak;\n");
                        sub.append(subsub.toString());
                    }
                });
                sub.append("\t\t\tdefault:\n");
                sub.append("\t\t\t\tstate = -1;\n");
                sub.append("\t\t\t\tbreak;\n");
                sub.append("\t\t}\n");
                sub.append("\t\tbreak;\n");
                sum.append(sub.toString());
            }
        });
        sum.append("}\n");
        return sum.toString();
    }

    //测试
    public static void main(String[] args) throws IOException {
//        String pattern = "a((b|c)*)(d*e)";
//        String pattern = "a((b)|(c))(d)*";
//        String pattern = "l(l|d)*";
//        String pattern = "b*";
//        String pattern = "l(l|d)*ab?cd+";
        String pattern = "a[bcA-CdeD-E]";
//        String pattern = "((.-.)|.)*";


        NFAGraph nfaGraph = NFARegexUtil.createNFAGraph(pattern);
        nfaGraph.getEndState().setEnd(true);
        nfaGraph.getStartState().setStart(true);

        DFAGraph dfaGraph = NFAToDFA(nfaGraph);
        DFAGraph minDFA = dfaGraph.translateMinDFA();
        Map<String, List<String>> stringListMap = toMapList(minDFA);
        GraphvizUtil.createStateGraph(stringListMap,"ex6");
//        System.out.println(toCCode(minDFA));

    }
}
