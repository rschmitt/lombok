import lombok.*;
import java.awt.Point;
import java.io.*;
import java.util.*;

public class Hello {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("Hello", "World");

        {
            destructure key, value = map.entrySet().iterator().next();
            System.out.printf("%s %s%n", key, value);
        }

        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        {
            destructure key = entry;
            destructure value = entry;
            System.out.printf("%s %s%n", key, value);
        }

        destructure left, right, isBool = new Greeting("Hello", "World", true);
        System.out.printf("%s %s %s%n", left, right, isBool);

        destructure x, y = createPoint();
        System.out.printf("Point: (%f, %f)%n", x, y);
    }

    @Value
    static class Greeting {
        String left;
        String right;
        boolean isBool;
    }

    private static Point createPoint() {
        return new Point(3, 4);
    }
}
