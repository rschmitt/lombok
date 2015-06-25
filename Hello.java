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

        destructure left, right = new Greeting("Hello", "World");
        System.out.printf("%s %s%n", left, right);

        destructure x, y = createPoint();
        System.out.printf("Point: (%f, %f)%n", x, y);
    }

    @Value
    static class Greeting {
        String left;
        String right;
    }

    private static Point createPoint() {
        return new Point(3, 4);
    }
}
