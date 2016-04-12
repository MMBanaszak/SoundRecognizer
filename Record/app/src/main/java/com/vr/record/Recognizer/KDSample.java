package com.vr.record.Recognizer;

import com.savarese.spatial.GenericPoint;
import com.savarese.spatial.KDTree;
import com.savarese.spatial.RangeSearchTree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by Marek on 2016-04-09.
 */
public class KDSample {

    //KD-LIB
    //https://www.savarese.com/software/libssrckdtree-j/
    public static void fillMap(Map map, Set points) {
        for(Object point : points)
            map.put(point, point.toString());
    }

    public static HashSet generatePoints() {
        HashSet points = new HashSet();
        Random random = new Random();

        //GenericPoint<Float> samplePoint = new GenericPoint<Float>(12);
        //samplePoint.setCoord(0,1.244f);
        for(int i = 0; i < 100; ++i) {
            int x = random.nextInt(100);
            int y = random.nextInt(100);
            points.add(new GenericPoint<Integer>(new Integer(x), new Integer(y)));
        }

        return points;
    }

    public static final void main(String argv[]) {
        HashSet points = generatePoints();
        RangeSearchTree<Integer, GenericPoint<Integer>, String>
                tree = new KDTree<Integer, GenericPoint<Integer>, String>();

        fillMap(tree, points);

        Iterator<Map.Entry<GenericPoint<Integer>,String>> range =
                tree.iterator(new GenericPoint<Integer>(25, 25),
                        new GenericPoint<Integer>(75, 75));

        while(range.hasNext()) {
            Map.Entry<GenericPoint<Integer>,String> e = range.next();
            System.out.println(e.getKey() + " : " + e.getValue());
        }
    }
}
