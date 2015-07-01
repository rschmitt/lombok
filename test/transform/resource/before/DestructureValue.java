import lombok.destructure;
import lombok.Value;
import java.awt.Point;
import java.io.*;
import java.util.*;

public class DestructureValue {
    public static void main(String[] args) {
        destructure left, right, isBool = new Greeting("Hello", "World", true);
        System.out.printf("%s %s %s%n", left, right, isBool);
    }

    @Value
    static class Greeting {
        String left;
        String right;
        boolean isBool;
    }
}

