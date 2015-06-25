import lombok.*;
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
    }
}
