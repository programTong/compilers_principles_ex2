package com.tongtongbigboy.lexer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Ui {

    private JFrame jFrame;
    private JButton[] buttons;
    private JTextArea output;
    private JTextField input;
    private JTextArea notice;

    public Ui() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        jFrame = new JFrame("词法分析");
        notice = new JTextArea("下方的输入框，用于输入正则表达式，正则表达式仅支持选择、连接、闭包、括号。\n"+
                    "右边的文本区域为dfa转c语言代码输出区。\n"+
                "正则转nfa、nfa转dfa、dfa最下化。三个会输出图片，输出前会要求输入文件路径。\n"
                ,20,3);
        notice.setEnabled(false);
        buttons = new JButton[4];
        buttons[0] = new JButton("正则转nfa");
        buttons[1] = new JButton("nfa转dfa");
        buttons[2] = new JButton("dfa最小化");
        buttons[3] = new JButton("dfa转c语言代码");
        output = new JTextArea("",30,40);
        output.setLineWrap(true);
        JScrollPane jsp = new JScrollPane(output);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        input = new JTextField();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10,10,10,10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gridBagLayout.setConstraints(notice,gbc);


        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gridBagLayout.setConstraints(input,gbc);

        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        for (int i = 0; i < 4; i++) {
            gbc.gridy = 2+i;
            gridBagLayout.setConstraints(buttons[i],gbc);
        }


        JPanel leftPanel = new JPanel(gridBagLayout);

        jFrame.setBounds(300,300,1000,600);
        jFrame.setVisible(true);
        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        leftPanel.add(notice);
        leftPanel.add(input);
        for (int i = 0; i < 4; i++) {
            leftPanel.add(buttons[i]);
        }
        jFrame.add(leftPanel,BorderLayout.WEST);
        jFrame.add(jsp,BorderLayout.CENTER);

        buttons[0].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                regexToNfa();
            }
        });
        buttons[1].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nfaToDfa();
            }
        });
        buttons[2].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                minDfa();
            }
        });
        buttons[3].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toCCode();
            }
        });

    }

    /**
     * 调用相关方法，将输入框中的正则表达式转为c语言代码
     */
    public void toCCode(){
        //判读input是否有效
        String pattern = input.getText();
        if (pattern==null||"".equals(pattern)){
            //无效弹窗提醒
            JOptionPane.showMessageDialog(null,"正则表达式为空");
            return;
        }

        //设置起始状态id
        NFAState.idGenerate = 1;
        //正则转nfa
        NFAGraph nfaGraph = NFARegexUtil.createNFAGraph(pattern);
        //设置终态、开始状态
        nfaGraph.getEndState().setEnd(true);
        nfaGraph.getStartState().setStart(true);
        // nfa转dfa
        DFAGraph dfaGraph = DFARegexUtil.NFAToDFA(nfaGraph);
        // dfa最小化
        DFAGraph minDFA = dfaGraph.translateMinDFA();
        // dfa转c语言代码
        String s = DFARegexUtil.toCCode(minDFA);
        //设置输出文本区内容
        output.setText(s);
    }

    /**
     * 弹窗显示图片
     * @param photoPath
     */
    public static void showPhotoDialog(String photoPath){
        JFrame jFrame = new JFrame();
        ImageIcon image = new ImageIcon(photoPath);
        Image img = image.getImage();
        //设置图片大小
        img = img.getScaledInstance(700,800, Image.SCALE_DEFAULT);
        image.setImage(img);
        JLabel jLabel = new JLabel();
        jLabel.setIcon(image);
        //添加滑轮
        JScrollPane jsp = new JScrollPane(jLabel);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jFrame.add(jsp);
        jFrame.setVisible(true);
        jFrame.setBounds(100,100,800,900);
    }

    /**
     * 调用方法将正则表达式转化为nfa
     */
    public void regexToNfa(){
        //获取用户输入路径
        String path = JOptionPane.showInputDialog("请输入图片保存路径");
        //设置默认路径
        if (path==null||"".equals(path)){
            path = System.getProperty("user.dir")+ File.separator +"ex1";
        }
        String pattern = input.getText();
        if (pattern==null||"".equals(pattern)){
            JOptionPane.showMessageDialog(null,"正则表达式为空");
            return;
        }
        NFAState.idGenerate = 1;
        NFAGraph graph = NFARegexUtil.createNFAGraph(pattern);
        graph.getEndState().setEnd(true);
        graph.getStartState().setStart(true);
        Map<String, List<String>> mapList = NFARegexUtil.toMapList(graph);
        try {
            //生成图片
            GraphvizUtil.createStateGraph(mapList,path);
            JOptionPane.showMessageDialog(null,"程序输出保存于"+ new File(path+".png").getAbsolutePath());
            //弹窗显示图片
            showPhotoDialog(path+".png");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 调用方法将nfa转化为dfa
     */
    public void nfaToDfa(){
        String path = JOptionPane.showInputDialog("请输入图片保存路径");
        if (path==null||"".equals(path)){
            path = System.getProperty("user.dir")+ File.separator +"ex2";
        }
        String pattern = input.getText();
        if (pattern==null||"".equals(pattern)){
            JOptionPane.showMessageDialog(null,"正则表达式为空");
            return;
        }
        NFAState.idGenerate = 1;
        NFAGraph nfaGraph = NFARegexUtil.createNFAGraph(pattern);
        nfaGraph.getEndState().setEnd(true);
        nfaGraph.getStartState().setStart(true);

        DFAGraph dfaGraph = DFARegexUtil.NFAToDFA(nfaGraph);
        Map<String, List<String>> mapList = DFARegexUtil.toMapList(dfaGraph);
        try {
            GraphvizUtil.createStateGraph(mapList,path);
            JOptionPane.showMessageDialog(null,"程序输出保存于"+ new File(path+".png").getAbsolutePath());
            showPhotoDialog(path+".png");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 调用方法最小化nfa
     */
    public void minDfa(){
        String path = JOptionPane.showInputDialog("请输入图片保存路径");
        if (path==null||"".equals(path)){
            path = System.getProperty("user.dir")+ File.separator +"ex3";
        }
        String pattern = input.getText();
        if (pattern==null||"".equals(pattern)){
            JOptionPane.showMessageDialog(null,"正则表达式为空");
            return;
        }
        NFAState.idGenerate = 1;
        NFAGraph nfaGraph = NFARegexUtil.createNFAGraph(pattern);
        nfaGraph.getEndState().setEnd(true);
        nfaGraph.getStartState().setStart(true);

        DFAGraph dfaGraph = DFARegexUtil.NFAToDFA(nfaGraph);
        DFAGraph minDFA = dfaGraph.translateMinDFA();
        Map<String, List<String>> mapList = DFARegexUtil.toMapList(minDFA);
        try {
            GraphvizUtil.createStateGraph(mapList,path);
            JOptionPane.showMessageDialog(null,"程序输出保存于"+ new File(path+".png").getAbsolutePath());
            showPhotoDialog(path+".png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动程序
     * @param args
     */
    public static void main(String[] args) {
        Ui ui = new Ui();
    }



}
