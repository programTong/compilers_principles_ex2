package com.tongtongbigboy.lexer;

import java.util.*;

/**
 * DFAGraph类，有个最小化DFA方法
 */
public class DFAGraph {

    public static final Map<String, DFAState> EMPTY = new HashMap<>();
    // DFA 开始状态节点
    private DFAState start;
    // DFA 对应的转换表。采用 Map<DFAState, Map<String, DFAState>>。  dfaState => (path, dfaState)
    private Map<DFAState, Map<String, DFAState>> stateTable;
    // statesId => (path, statesId)
    private Map<String,Map<String,String>> stateTableB;
    // 接受状态集合
    private List<DFAState> acceptStates;
    // 非接收状态集合
    private List<DFAState> nonAcceptStates;
    // statesId => dfaState
    private Map<String,DFAState> idToState;
    // 路径集合
    private List<String> paths;

    public DFAGraph(DFAState start) {
        this.start = start;
    }

    /**
     * 根据状态集合、转换函数集合构造DFAGraph
     * @param stateList 状态集合
     * @param transList 转换函数集合
     */
    public DFAGraph(List<DFAState> stateList, List<TransformFunction> transList) {
        this.start = stateList.get(0);
        this.stateTableB = new HashMap<>();
        this.stateTable = new HashMap<>();
        this.acceptStates = new ArrayList<>();
        this.nonAcceptStates = new ArrayList<>();
        this.paths = new ArrayList<>();
        this.idToState = new HashMap<>();

        //构造stateTableB、paths
        for (TransformFunction transformFunction : transList) {
            Map<String, String> stringIntegerMap = stateTableB.get(transformFunction.getStartState());
            if (stringIntegerMap==null) {
                stringIntegerMap = new HashMap<>();
                stateTableB.put(transformFunction.getStartState(),stringIntegerMap);
            }
            if (!paths.contains(transformFunction.getDriverChar())){
                paths.add(transformFunction.getDriverChar());
            }
            stringIntegerMap.put(transformFunction.getDriverChar(),transformFunction.getEndState());
        }


        //构造idToState、acceptStates、nonAcceptStates
        for (DFAState state : stateList) {
            this.idToState.put(state.getNFAStatesId(),state);
            if (state.isEnd()){
                this.acceptStates.add(state);
            } else {
                this.nonAcceptStates.add(state);
            }
        }

        // 构造stateTable
        for (TransformFunction transformFunction : transList) {
            Map<String, DFAState> stringDFAStateMap = this.stateTable.get(this.idToState.get(transformFunction.getStartState()));
            if (stringDFAStateMap==null){
                stringDFAStateMap = new HashMap<>();
                this.stateTable.put(this.idToState.get(transformFunction.getStartState()),stringDFAStateMap);
            }
            stringDFAStateMap.put(transformFunction.getDriverChar(),this.idToState.get(transformFunction.getEndState()));
        }

        // 在transList中缺失了一些终态。这些终肽不指向其他状态。
        for (DFAState state : stateList) {
            if (state.isEnd()&&!stateTableB.containsKey(state.getNFAStatesId())){
                stateTableB.put(state.getNFAStatesId(),new HashMap<>());
            }
            if (state.isEnd()&&!stateTable.containsKey(state)){
                stateTable.put(state,new HashMap<>());
            }
        }
    }

    /**
     *
     * @param start
     * @return
     */
    public static DFAGraph create(DFAState start) {
        DFAGraph dfaGraph = new DFAGraph(start);
        dfaGraph.stateTable = new HashMap<>();
        dfaGraph.stateTableB = new HashMap<>();
        dfaGraph.acceptStates = new ArrayList<>();
        dfaGraph.nonAcceptStates = new ArrayList<>();
        dfaGraph.idToState = new HashMap<>();
        dfaGraph.paths = new ArrayList<>();
        return dfaGraph;
    }

    /**
     * 向转换表中添加数据，附带更新idToState
     * @param currentState
     * @param path
     * @param state
     */
    public void addStateTable(DFAState currentState, String path, DFAState state) {
        Map<String, DFAState> pathMap = stateTable.get(currentState);
        if (pathMap == null) {
            pathMap = new HashMap<>();
            stateTable.put(currentState, pathMap);
        }
        pathMap.put(path, state);

        Map<String, String> pathMapB = stateTableB.get(currentState.getNFAStatesId());
        if (pathMapB == null){
            pathMapB = new HashMap<>();
            stateTableB.put(currentState.getNFAStatesId(),pathMapB);
        }
        pathMapB.put(path,state.getNFAStatesId());

        idToState.put(currentState.getNFAStatesId(),currentState);
        idToState.put(state.getNFAStatesId(),state);
    }

    /**
     * 根据参数是否为接受态，加入接受态集合或非接受态集合
     * @param dfaState
     */
    public void addAcceptOrNonStates(DFAState dfaState){
        if (dfaState.isEnd()){
            if (!acceptStates.contains(dfaState)){
                acceptStates.add(dfaState);
            }
        } else {
            if (!nonAcceptStates.contains(dfaState)){
                nonAcceptStates.add(dfaState);
            }
        }
    }

    /**
     * 对本DFA进行HopCraft最小化
     * Hopsroft 算法就是先根据非终结状态与非终结状态将所有的节点分为 N 和 A 两大类。 N 为非终结状态，A 为终结状态，之后再对每一组运用基于等价类实现的切割算法。
     * //基于等价类的思想
     * split(equal)
     *     for(path in paths)
     *         if(path can split equal)
     *             split equal into equal1, ..., equalk
     *
     * hopcroft()
     *     split all nodes into N, A
     *     while(equal_set is still changes)
     *         for(equal in set)
     *      	   split(equal)
     *
     * @return	HopCraft最小化的DFA
     */
    public DFAGraph translateMinDFA() {
        int i;
        //用于容纳状态的等价集合
        List<List<String>> container = new LinkedList<>();

        List<String> acceptL = new LinkedList<>();
        List<String> nacceptL = new LinkedList<>();
        container.add(acceptL);
        container.add(nacceptL);

        //分割成一个接受集，一个非接受集
        for(i = 0; i < acceptStates.size(); i++) {
            acceptL.add(acceptStates.get(i).getNFAStatesId());
        }
        for (int j = 0; j < nonAcceptStates.size(); j++) {
            nacceptL.add(nonAcceptStates.get(j).getNFAStatesId());
        }

        //分割等价类
        boolean hasChange = true;					//表示了等价类修改了
        while( hasChange ) {
            hasChange = splitContainer(container);
        }

        //用于求新的转换函数
        return produceMinDFA(container);			//根据该等价类获取DFA
    }

    /**
     * 对container容器中的等价类进行分割
     * @param container		等价类的容器
     * @return				是否进行了分割（false代表否， true代表真）
     */
    private boolean splitContainer( List<List<String>> container ) {

        for(List<String> list : container) {
            for (String inputCh : paths) {
                if(splitList(inputCh, list, container)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 功能：使用指定驱动字符，分割等价类
     * 返回：是否分割
     * @param input			使用的字符
     * @param list			分割的等价类
     * @param container		等价类的容器
     * @return				是否能进行分割
     */
    private boolean splitList(String input, List<String> list, List<List<String>> container) {

        //一个map集
        //键：每个这个等价类中的状态能够转的到的等价类下标
        //值：一个List，里面保存了该转换到该等价类的状态
        Map<Integer, List<String>> map = new HashMap();
        for(String num : list) {
            //这个等价类中的状态能够转的到状态
            String endstate = stateTableB.get(num).get(input);
            //获取这个状态的等价类下标
            int equalIndex = getListNum(endstate, container);
            if ( !map.containsKey(equalIndex) ) {
                List<String> temp = new LinkedList<>();
                temp.add(num);
                map.put(equalIndex, temp);
            }else {
                List<String> temp = map.get(equalIndex);
                temp.add(num);
                map.put(equalIndex, temp);
            }
        }

        if(map.size() == 1) {
            //表示该字符无法分割该等价类
            return false;
        }else{
            //表示该字符能分割该等价类
            //1.删除该等价类
            container.remove(list);
            //2.添加分割后的等价类
            for(Map.Entry< Integer, List<String>> entry : map.entrySet()) {
                container.add(entry.getValue());
            }
            return true;
        }
    }

    /**
     * 获取一个状态所在的等价类的编号
     * @param state				传入的状态
     * @param container			等价类的容器
     * @return					该状态所对应的等价类的编号
     */
    private int getListNum(String state, List<List<String>> container) {
        for(int i = 0; i < container.size(); i++) {
            if( container.get(i).contains(state) ) {
                return i;
            }
        }
        return -1;						//代表着不能该状态没有转换到的等价类
    }

    /**
     * 根据分割开来的等价类集合生成新的DFA
     * @param container		等价类的容器
     * @return	DFA
     */
    private DFAGraph produceMinDFA( List<List<String>> container) {
        //状态集合
        List<DFAState> stateList = new LinkedList<>();
        //转换函数集合
        List<TransformFunction> transList = new LinkedList<TransformFunction>();
        //开始状态排到container前面。
        container.sort(new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                int a = 0;
                for (String s : o1) {
                    //是开始状态
                    if (s.startsWith("1,")){
                        a = 1;
                        break;
                    }
                }
                int b = 0;
                for (String s : o2) {
                    if (s.startsWith("1,")){
                        b = 1;
                        break;
                    }
                }
                return a-b;
            }
        });

        // 旧指最小化前，新指最小化后。旧的statesId => 新的statesId
        Map<String,String> oldIdToNewId = new HashMap<>();
        //将每个等价类转化为一个状态
        for(int i=0; i < container.size(); i++ ) {
            //获取一个等价类，等价类就是List<String>集合，里面是状态的statesId
            List<String> list = container.get(i);

            //将一个等价类关联的所有nfaState保存到一个set中
            Set<NFAState> set = new HashSet<>();
            for (String id : list) {
                set.addAll(idToState.get(id).getNFAStateSet());
            }
            //根据Set<NFAState>构造DFAState
            DFAState stateA = DFAState.create(set);
            //添加到状态集合
            stateList.add(stateA);
            //更新oldIdToNewId
            for (String s : list) {
                oldIdToNewId.put(s,stateA.getNFAStatesId());
            }
        }

        //构造转换函数集合
        for (int i = 0; i < container.size(); i++) {
            //获取一个等价类
            List<String> list = container.get(i);
            //任意取出一个等价类中的状态。因为该等价类中所有的状态都是等价的
            String state = list.get(0);
            //遍历paths，以求构造转换函数集合
            for (String inputCh : paths) {
                //获取该状态通过路径inputCh能到达的目的状态
                String targetState = stateTableB.get(state).get(inputCh);
                //能获取到目的状态
                if( targetState != null && !"".equals(targetState)) {
                    //目的状态对应的等价类状态体，或者说最小化后的一个状态
                    String target = oldIdToNewId.get(targetState);
                    //new 了一个转换函数。state所在的等价类状态体 通过inputCh 到达 目的状态对应的等价类状态体
                    transList.add(new TransformFunction(oldIdToNewId.get(state) , inputCh, target));
                }
            }

        }

        return new DFAGraph(stateList, transList);
    }




    /**
     * 获取对应的下一个状态节点
     * @param currentState
     * @param path
     * @return
     */
    public DFAState getStateByMove(DFAState currentState, String path) {
        return stateTable.getOrDefault(currentState, EMPTY).get(path);
    }

    public DFAState getStart() {
        return start;
    }

    public Map<DFAState, Map<String, DFAState>> getStateTable() {
        return stateTable;
    }

    public List<DFAState> getAcceptStates() {
        return acceptStates;
    }

    public List<DFAState> getNonAcceptStates() {
        return nonAcceptStates;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DFAGraph{");
        sb.append("start=").append(start);
        sb.append(", stateTable=").append(stateTable);
        sb.append('}');
        return sb.toString();
    }
}
