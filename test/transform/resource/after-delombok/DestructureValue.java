import java.awt.Point;
import java.io.*;
import java.util.*;

public class DestructureValue {


    public static void main(String[] args) {
        final DestructureValue.Greeting $$lombok$destructure$temp0 = new Greeting("Hello", "World", true);
        final java.lang.String left = $$lombok$destructure$temp0.getLeft();
        final java.lang.String right = $$lombok$destructure$temp0.getRight();
        final boolean isBool = $$lombok$destructure$temp0.isBool();
        System.out.printf("%s %s %s%n", left, right, isBool);
    }

    static final class Greeting {
        private final String left;
        private final String right;
        private final boolean isBool;

        @java.beans.ConstructorProperties({"left", "right", "isBool"})
        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public Greeting(final String left, final String right, final boolean isBool) {
            this.left = left;
            this.right = right;
            this.isBool = isBool;
        }

        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public String getLeft() {
            return this.left;
        }

        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public String getRight() {
            return this.right;
        }

        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public boolean isBool() {
            return this.isBool;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof DestructureValue.Greeting)) return false;
            final Greeting other = (Greeting)o;
            final java.lang.Object this$left = this.getLeft();
            final java.lang.Object other$left = other.getLeft();
            if (this$left == null ? other$left != null : !this$left.equals(other$left)) return false;
            final java.lang.Object this$right = this.getRight();
            final java.lang.Object other$right = other.getRight();
            if (this$right == null ? other$right != null : !this$right.equals(other$right)) return false;
            if (this.isBool() != other.isBool()) return false;
            return true;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $left = this.getLeft();
            result = result * PRIME + ($left == null ? 43 : $left.hashCode());
            final java.lang.Object $right = this.getRight();
            result = result * PRIME + ($right == null ? 43 : $right.hashCode());
            result = result * PRIME + (this.isBool() ? 79 : 97);
            return result;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        @javax.annotation.Generated("lombok")
        public java.lang.String toString() {
            return "DestructureValue.Greeting(left=" + this.getLeft() + ", right=" + this.getRight() + ", isBool=" + this.isBool() + ")";
        }
    }
}
