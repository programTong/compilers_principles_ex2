package com.tongtongbigboy.lexer;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Link.to;

/**
 * 画图工具类
 */
public class GraphvizUtil {

    /**
     * 数据格式为: statesId@是否为接受态 => [statesId@路径@是否为接受态, statesId@路径@是否为接受态]
     *      * 例如： 1,2,@false => ['3,4,@a@true' , '4,5,@b@false']
     * @param mapList 数据源，格式有要求
     * @param savePath 图片保存路径
     * @throws IOException
     */
    public static void createStateGraph(Map<String, List<String>> mapList, String savePath) throws IOException {
        MutableGraph g = mutGraph("lifeCycle").setDirected(true).use((gr, ctx) -> {
            mapList.forEach(new BiConsumer<String, List<String>>() {
                @Override
                public void accept(String keyStr, List<String> list) {
                    for (String value : list) {
                        String values = null;
                        String lable = null;
                        boolean toStateIsEnd = false;
                        String key = null;
                        boolean fromStateIsEnd = false;
                        if(value!=null&&!value.equals("")){
                            String[] splitValues = value.split("@");
                            values = splitValues[0];
                            lable = splitValues[1];
                            if (splitValues.length>=3&&"true".equals(splitValues[2])){
                                toStateIsEnd = true;
                            }
                        }
                        if(keyStr!=null&&!"".equals(keyStr)){
                            String[] splitKey = keyStr.split("@");
                            key = splitKey[0];
                            if (splitKey.length>=2&&"true".equals(splitKey[1])){
                                fromStateIsEnd = true;
                            }
                        }
                        MutableNode add = null;
                        add = mutNode(key).add(Color.rgb(200, 200, 200)); // 设置颜色
                        //是接受态，双圆圈
                        if (fromStateIsEnd){
                            add = add.add(Shape.DOUBLE_CIRCLE);
                        } else {
                            add = add.add(Shape.ELLIPSE);
                        }

                        if(value!=null&&!value.equals("")){
                            //antd的蓝色 24, 144, 255
                            MutableNode add2 = null;
                            add2 = mutNode(values)
                                    .add(Color.rgb(200, 200, 200)); // 设置颜色
                            if (toStateIsEnd){
                                add2 = add2.add(Shape.DOUBLE_CIRCLE);
                            } else {
                                add2 = add2.add(Shape.ELLIPSE);
                            }
                            add.addLink(to((add2))      // 节点之间对接
                                    .add(Label.of(lable))  // 节点之间跳转时候的标签值
                                    .add(Color.rgb(200, 200, 200))); // 颜色
                        }
                    }
                }
            });
        });
        // 输出到本地
        Graphviz.fromGraph(g).width(1500).render(Format.PNG)
                .toFile(new File(savePath+".png"));

    }

}
