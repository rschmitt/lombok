import java.awt.Point;
import java.io.*;
import java.util.*;

public class DestructureSimple {


    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("Hello", "World");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        {
            final java.util.Map.Entry<java.lang.String, java.lang.String> $$lombok$destructure$temp0 = entry;
            final java.lang.String key = $$lombok$destructure$temp0.getKey();
            final java.util.Map.Entry<java.lang.String, java.lang.String> $$lombok$destructure$temp1 = entry;
            final java.lang.String value = $$lombok$destructure$temp1.getValue();
            System.out.printf("%s %s%n", key, value);
        }
        final java.awt.Point $$lombok$destructure$temp2 = createPoint();
        final double x = $$lombok$destructure$temp2.getX();
        final double y = $$lombok$destructure$temp2.getY();
        System.out.printf("Point: (%f, %f)%n", x, y);
    }

    private static Point createPoint() {
        return new Point(3, 4);
    }
}
