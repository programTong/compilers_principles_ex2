package com.tongtongbigboy.lexer;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 正则转nfa
 */
public class NFARegexUtil {


    /**
     * 运算符优先级
     */
    private static Map<Character,Integer> rank = new HashMap<Character, Integer>(){{
        put('|',1);
        put('&',2);
        put('*',3);
        put('?',3);
        put('+',3);
    }};

    /**
     * 后置单操作数运算符，例如闭包 (a)*,后置单操作数运算符不用考虑括号，遇到可直接计算
     * 前置单操作数运算符，例如取反 !flag
     */
    private static Set<Character> backSingleOps = new HashSet<Character>(){{
        add('*');
        add('?');
        add('+');
    }};

    /**
     * 通过 pattern 生成对应的 NFAGraph 转换图, 核心代码，代码逻辑类似处理算术表达式
     * @param pattern
     * @return
     */
    public static NFAGraph createNFAGraph(String pattern) {
        char[] cs = pattern.toCharArray();
        int n = pattern.length();
        //运算符栈
        Deque<Character> ops = new LinkedList<>();
        //nfa栈
        Deque<NFAGraph> graphs = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            char ch = cs[i];
            if (ch=='('){
                /*
                    例如(a)(b) => (a)&(b)
                        a*()  => a*&(b)
                        a()   => a&(b)
                    可有可无
                 */
                if (i>0&&(cs[i-1]==')'||cs[i-1]=='*'||isNormalChar(cs[i-1]))){
                    ops.addLast('&');
                }
                ops.addLast('(');
            } else if (ch==')'){
                /*
                如果是)，从运算符栈中取出运算符，如果是(停下，否则进行计算。
                 */
                while (!ops.isEmpty()){
                    char cha = ops.peekLast();
                    if (cha=='('){
                        ops.removeLast();
                        break;
                    } else {
                        calc(graphs,ops);
                    }
                }
            } else if (ch=='['){
                List<Character> charList = new ArrayList<>();
                int state = 0;
                int index = i;
                //一直读，直到]或已读完
                while (++index<n&&(ch=cs[index])!=']'){
                    switch (state){
                        case 0:
                            if (ch!='-'){
                                state = 0;
                                charList.add(ch);
                            } else {//读到'-'，进入状态1
                                state = 1;
                            }
                            break;
                        case 1:
                            //例如A-C，这里会将B、C加入charList
                            char pre = charList.get(charList.size() - 1);
                            for (int j = pre+1; j <= ch; j++) {
                                charList.add((char)j);
                            }
                            state = 0;
                            break;
                    }
                }
                //读到字符串尾仍然未找到], 报错
                if (index>=n){
                    throw new RuntimeException("正则表达式缺少]");
                } else {
                    i=index;
                    graphs.addLast(NFAGraph.createRange(charList));
                }
            } else {
                //是运算符
                if (rank.containsKey(ch)){
                    //后置单操作数运算符，直接运算
                    if (backSingleOps.contains(ch)){
                        ops.addLast(ch);
                        calc(graphs,ops);
                    } else {//双操作数运算符，若栈内运算符优先级>=当前运算符优先级，取出来进行运算
                        while (!ops.isEmpty()&&ops.peekLast()!='('){
                            char pre = ops.peekLast();
                            if (rank.get(pre)>=rank.get(ch)){
                                calc(graphs,ops);
                            } else {
                                break;
                            }
                        }
                        ops.addLast(ch);
                    }
                } else {//是非运算符
                    //如果上一个字符是非运算符，例如当前字符是b, ab -> a&b
                    if (i>=1&&isNormalChar(cs[i-1])){
                        ops.addLast('&');
                    }
                    NFAGraph grapha = NFAGraph.createByPath(ch+"");
                    graphs.addLast(grapha);
                }

            }
        }

        //若运算符栈不空，进行运算
        while (!ops.isEmpty()){
            calc(graphs,ops);
        }
        //若nfa栈大小>1，不断取出进行连接操作
        while (graphs.size()>1){
            NFAGraph b = graphs.removeLast(), a = graphs.removeLast();
            a.addSerial(b);
            graphs.addLast(a);
        }
        return graphs.peekLast();
    }

    /**
     * 根据不同运算符符进行运算
     * @param graphs
     * @param ops
     */
    public static void calc(Deque<NFAGraph> graphs, Deque<Character> ops){
        if (ops.isEmpty() || (ops.peekLast()=='*'&&graphs.size()<1)){
            return;
        }
        //取出运算符
        char ch = ops.removeLast();
        NFAGraph a = null;
        if (ch=='*'){
            a = graphs.removeLast();
            a.repeatStar();
        } else if (ch=='?'){
            a = graphs.removeLast();
            a.addOptional();
        }else if (ch=='+'){
            a = graphs.removeLast();
            a.repeatPlus();
        } else if (ch=='|'){
            NFAGraph b = graphs.removeLast();
            a = graphs.removeLast();
            a.addParallel(b);
        } else if (ch=='&'){
            NFAGraph b = graphs.removeLast();
            a = graphs.removeLast();
            a.addSerial(b);
        }
        graphs.addLast(a);
    }

    /**
     * 普通Char指不是运算符、括号
     * @param ch
     * @return
     */
    public static boolean isNormalChar(char ch){
        return ch!='('&&ch!=')'&&!rank.containsKey(ch);
    }

    /**
     * 根据参数nfaGraph，生成stateList和pathList
     * @param stateList 一个nfaGraph的所有状态的集合
     * @param pathList  一个nfaGraph的所有路径的集合
     * @param nfaGraph  数据来源
     */
    public static void generateStateListAndPathListFromNFAGraph(List<NFAState> stateList, List<String> pathList, NFAGraph nfaGraph){
        //状态集合
        stateList.clear();
        //path集合
        pathList.clear();

        //生成状态集合 & path集合，广度优先遍历，借助队列实现
        Deque<NFAState> deque = new LinkedList<>();
        //已访问标记
        Set<Integer> visited = new HashSet<>();
        deque.addLast(nfaGraph.getStartState());
        visited.add(nfaGraph.getStartState().getId());
        while (!deque.isEmpty()){
            NFAState nfaState = deque.removeFirst();
            Map<String, Set<NFAState>> edges = nfaState.getEdges();
            Iterator<String> iterator = edges.keySet().iterator();
            while (iterator.hasNext()){
                String next = iterator.next();
                //添加路径
                if (!pathList.contains(next)){
                    pathList.add(next);
                }
                //遍历该nfaState通过 next所代表的字符串 能到达的状态，若未访问过，加入队列
                Set<NFAState> nfaStates = edges.get(next);
                for (NFAState state : nfaStates) {
                    if (!visited.contains(state.getId())){
                        deque.addLast(state);
                        visited.add(state.getId());
                    }
                }
            }
            stateList.add(nfaState);
        }

    }

    /**
     * 打印nfa
     * @param nfaGraph
     */
    public static void printNFA(NFAGraph nfaGraph){
        List<NFAState> stateList = new ArrayList<>();
        //path集合
        List<String> pathSet = new ArrayList<>();
        generateStateListAndPathListFromNFAGraph(stateList,pathSet,nfaGraph);

        /*
            对状态集合的内容进行格式化打印
         */
        stateList.sort(new Comparator<NFAState>() {
            @Override
            public int compare(NFAState nfaState, NFAState t1) {
                return nfaState.getId()-t1.getId();
            }
        });
        int metaSize = 15;
        int separateSize = ((4+pathSet.size())*14);
        String separateStr = "";
        for (int i = 0; i < separateSize; i++) {
            separateStr+='-';
        }
        System.out.println();
        //表头
        System.out.print(String.format("%-"+metaSize+"s","state id")+"    ");
        System.out.print(String.format("%-"+metaSize+"s","final state")+"    ");
        for (String s : pathSet) {
            System.out.print(String.format("%-"+metaSize+"s",s)+"    ");
        }
        System.out.println();
        System.out.println(separateStr);
        //表体
        for (NFAState nfaState : stateList) {
            System.out.print(String.format("%-"+metaSize+"s",nfaState.getId())+"    ");
            System.out.print(String.format("%-"+metaSize+"s",nfaState.isEnd())+"    ");
            Map<String, Set<NFAState>> edges = nfaState.getEdges();
            Iterator<String> iterator = edges.keySet().iterator();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < pathSet.size(); i++) {
                stringBuilder.append("                      ");
            }
            while (iterator.hasNext()){
                String next = iterator.next();
                int indexa = pathSet.indexOf(next);
                Set<NFAState> nfaStates = edges.get(next);
                String toStates = "";
                for (NFAState state : nfaStates) {
                    toStates+=state.getId()+""+',';
                }
                stringBuilder.replace(indexa*(metaSize+4),indexa*(metaSize+4)+metaSize,String.format("%-"+metaSize+"s",toStates));
            }
            System.out.print(stringBuilder.toString());
            System.out.println();
            System.out.println(separateStr);
        }
    }

    /**
     * NFA转Map<String, List<String>>，目的是给画图工具类GraphvizUtil.createStateGraph(..)传递数据。
     * 数据格式为: statesId@是否为接受态 => [statesId@路径@是否为接受态, statesId@路径@是否为接受态]
     * 例如： 1@false => ['3@a@true' , '4,@b@false']
     * @param nfaGraph
     * @return
     */
    public static Map<String, List<String>> toMapList(NFAGraph nfaGraph){
        Map<String, List<String>> mapList = new HashMap<>();

        List<NFAState> stateList = new ArrayList<>();
        List<String> pathList = new ArrayList<>();
        generateStateListAndPathListFromNFAGraph(stateList,pathList,nfaGraph);
        for (NFAState nfaState : stateList) {
            List<String> subList = new ArrayList<>();
            nfaState.getEdges().forEach(new BiConsumer<String, Set<NFAState>>() {
                @Override
                public void accept(String s, Set<NFAState> toStates) {
                    for (NFAState toState : toStates) {
                        subList.add(toState.getId()+""+"@"+s+"@"+toState.isEnd());
                    }
                }
            });
            mapList.put(nfaState.getId()+"@"+nfaState.isEnd(),subList);
        }
        return mapList;
    }


    /**
     * 测试正则转nfa
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
//        String pattern = "a((b|c)*)(d*e)";
//        String pattern = "aa*";
        String pattern = "l(l|d)*ab?";
        NFAGraph graph = createNFAGraph(pattern);
        // 设置转换图的结束状态节点 就是终止状态节点
        graph.getEndState().setEnd(true);
        Map<String, List<String>> mapList = toMapList(graph);
        GraphvizUtil.createStateGraph(mapList,"ex11");

    }

}
