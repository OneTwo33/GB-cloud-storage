package ru.onetwo33.streamApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Lambda { // ->
    public static void main(String[] args) {
        Callback c = new Callback() {
            @Override
            public void call(String value) {
                System.out.println("value - " + value);
            }
        };

        c.call("test line");

        Callback c1 = v ->  System.out.println("value-lambda - " + v);

        Callback c2 = v -> {
            System.out.println("start lambda");
            System.out.println("value-lambda - " + v);
            System.out.println("finish lambda");
        };

        Callback c3 = System.out::println;

        Callback c4 = Lambda::signal;

        c1.call("test lambda line");
        c2.call("test lambda 2 line");
        c3.call("test lambda 3 line");
        c4.call("test lambda 4 line");

        System.out.println("-------------------------");

        CallbackDouble cd = new CallbackDouble() {
            @Override
            public String callDouble(String a, String b) {
                return String.format("Input-1: %s\tInput-2 %s", a, b);
            }
        };

        CallbackDouble cd1 = (a, b) -> String.format("InputLambda-1: %s\tInputLambda-2 %s", a, b);

        System.out.println(cd.callDouble("test1", "test2"));
        System.out.println(cd1.callDouble("test1-lambda", "test2-lambda"));

        System.out.println("-------------------------");

        Consumer<Integer> consumer = a -> {
            a++;
            System.out.println(a);
        };

        consumer = consumer.andThen(arg -> {
            arg *= 2;
            System.out.println(arg);
        });

        consumer.accept(10);

        Predicate<Integer> predicate = value -> value % 2 == 0;
        predicate = predicate.and(value -> value > 6).or(val -> val == 5);
        System.out.println(predicate.test(7));

        Function<Integer, String> converter = a -> "test".repeat(a);
        System.out.println(converter.apply(3));

        Function<String, Integer> convertStringToInteger = a -> a.length();
        System.out.println(convertStringToInteger.apply("test"));

        Supplier<List<Integer>> getList = ArrayList::new;
        System.out.println(getList.get());

        Supplier<Set<String>> getSet = HashSet::new;
    }

    private static void signal(String text) {
        System.out.println("!!! " + text + " !!!");
    }
}
