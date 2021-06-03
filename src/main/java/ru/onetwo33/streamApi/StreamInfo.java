package ru.onetwo33.streamApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StreamInfo {
    public static void main(String[] args) throws IOException {
//		List<Developer> developers = new ArrayList<>();
//		developers.add(new Developer("Alex", "Java", 4000));
//		developers.add(new Developer("Mike", "Python", 4000));
//		developers.add(new Developer("John", "C++", 3500));
//		developers.add(new Developer("Bill", "PHP", 5000));
//		developers.add(new Developer("Tom", "Python", 4000));
//		developers.add(new Developer("Curt", "C++", 4000));
//		developers.add(new Developer("Piter", "Java", 3500));
//		developers.add(new Developer("Alex", "Python", 5000));
//		developers.add(new Developer("Artem", "Java", 4000));
//		developers.add(new Developer("Vlad", "C++", 4000));
//		developers.add(new Developer("Michail", "Java", 3400));
//		developers.add(new Developer("Kristin", "Python", 4000));
//		developers.add(new Developer("Marta", "Java", 4000));
//		developers.add(new Developer("Robin", "PHP", 5000));
//
//		List<Developer> filteredDevelopers = new ArrayList<>();
//		for (Developer d : developers) {
//			if ("Java".equals(d.getLanguage()) && d.getSalary() >= 4000) {
//				filteredDevelopers.add(d);
//				System.out.println(d);
//			}
//		}
//
//		List<Developer> javaDev = developers.stream()
//				.filter(p -> "Java".equals(p.getLanguage()))
//				.filter(d -> d.getSalary() >= 4000)
//				.collect(Collectors.toList());
//		javaDev.forEach(System.out::println);
//
//		developers.stream()
//				.filter(k -> "Python".equals(k.getLanguage()))
//				.sorted((k1, k2) -> Integer.compare(k1.getSalary(), k2.getSalary()))
//				.forEach(System.out::println);
//
//		double averageSalary = developers.stream()
//				.filter(d -> "C++".equals(d.getLanguage()))
//				.mapToInt(Developer::getSalary)
//				.average()
//				.getAsDouble();
//		System.out.println(averageSalary);

		/*
		Конвейерные:
		filter(Predicate)
		map(Function): map, mapToInt, mapToDouble, mapToLong
		sorted()
		skip(int N)
		distinct()
		peek()
		limit(int N)
		average(Stream<Integer>)
		flat, flatMapToInt, ..ToLong, ..ToDouble
		Терминальные:
		collect
		count
		max(Comparator), min(Comparator)
		reduce
		findFirst - Optional
		findAny - Optional
		anyMatch (condition)
		noneMatch (condition)
		allMatch (condition)
		forEach(Consumer)
		forEachOrder
		// TODO: 24.05.2021
		// в своем репозитории написать примеры для каждого метода (выше)
		// и 2 метода которые содержат в себе минимум 4 конвейерных  метода
		 */

        // создание стримов
//		Collection<String> collection = Arrays.asList("one", "two", "three");
//		collection.stream();
//
//		Stream.of("one", "two", "three");
//
//		Integer[] array = {1, 2, 3};
//		Arrays.stream(array);
//
//		Files.lines(Path.of("client", "1.txt"));
//
//		IntStream streamFromString = "123".chars();
//
//		Stream.builder()
//				.add("a1")
//				.add("a2")
//				.add("a3")
//				.add("a4")
//				.build();
//
//		collection.parallelStream();

        // Stream.iterate(1, n -> n + 1);
//		Stream.generate(() -> "a1");

        Map<String, Integer> map = Files.lines(Path.of("client", "1.txt"))
//				.forEach(System.out::println);
                .flatMap(line -> Arrays.stream(line.split(" +")))
                .map(v -> v.replaceAll("[?!;:.,—]", "").toLowerCase(Locale.ROOT))
                .filter(line -> !line.isBlank())
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toMap(Function.identity(), value -> 1, Integer::sum));

//		System.out.println(map);

        map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((o1, o2) -> o2 - o1))
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
    }
}
