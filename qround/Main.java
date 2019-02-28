package qround;

import java.util.Arrays;
import java.util.Scanner;

public class Main {

	private static final String TAG = "///////////////////////////////////// %s /////////////////////////////////////";
	private static final String RESULT_FILENAME = "result.out";
	private static boolean log = false;
  private static boolean saveResult = false;
  private static boolean concurrent = false;
	private static int GRAN;
	private static String stats = "";

	public static void main(String[] args) {
		log |= option(args, "-l");
    concurrent |= option(args, "-c");
    saveResult |= option(args, "-s");
		GRAN = checkValue(args, "-gran", 1);

		Scanner in = new Scanner(System.in);

	}

	/*
	private static void saveResultToFile(Set<Slice> expansionSolution) {
    Path path = Paths.get(".", RESULT_FILENAME);
    List<String> results = expansionSolution.stream().map(Slice::toSaveString).collect(Collectors.toList());
    results.add(0, String.valueOf(expansionSolution.size()));
    try {
      Files.write(path, results);
    } catch (IOException e){
      System.out.println(e.getMessage());
    }
  }*/

	private static void log(String str) {
    if (log)
      System.out.println(str);
  }

  private static boolean option(String[] args, String op) {
    return Arrays.stream(args).anyMatch(a -> a.equals(op));
  }

  private static int checkValue(String[] args, String arg, int defaultVal) {
    if (option(args, arg)) {
      for (int i = 0; i < args.length; i++)
        if (args[i].equals(arg))
          return (i + 1) < args.length && !args[i + 1].startsWith("-") ? Integer.valueOf(args[i+1]) : defaultVal;
    }

    return defaultVal;
  }

}
