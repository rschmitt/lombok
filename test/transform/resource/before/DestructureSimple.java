import lombok.destructure;
import java.awt.Point;
import java.io.*;
import java.util.*;

public class DestructureSimple {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("Hello", "World");

        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        {
            destructure key = entry;
            destructure value = entry;
            System.out.printf("%s %s%n", key, value);
        }

        destructure x, y = createPoint();
        System.out.printf("Point: (%f, %f)%n", x, y);
    }

    private static Point createPoint() {
        return new Point(3, 4);
    }
}
