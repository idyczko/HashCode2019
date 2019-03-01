
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    private static final String TAG = "///////////////////////////////////// %s /////////////////////////////////////";
    private static final String RESULT_FILENAME = "result.out";
    private static boolean log = false;
    private static boolean saveResult = false;
    private static boolean concurrent = false;
    private static int GRAN;
    private static String stats = "";
    private static int size;

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "-l";
        log |= option(args, "-l");
        concurrent |= option(args, "-c");
        saveResult |= option(args, "-s");
        GRAN = checkValue(args, "-gran", 1);

        Map<Set<String>, List<Integer>> horizontal = new HashMap<>();
        Map<Set<String>, List<Integer>> vertical = new HashMap<>();

        Scanner in = new Scanner(System.in);

        size = in.nextInt();

        for (int i = 0; i < size; i++) {
            String orientation = in.next();
            int sizeOfSlide = in.nextInt();

            Set<String> tags = new HashSet<>();
            for (int j = 0; j < sizeOfSlide; j++) {
                tags.add(in.next());
            }

            if (orientation.equals("H")) {
                if (horizontal.containsKey(tags)) {
                    horizontal.get(tags).add(i);
                } else {
                    List<Integer> list = new ArrayList<>();
                    list.add(i);
                    horizontal.put(tags, list);
                }
            } else {
                if (vertical.containsKey(tags)) {
                    vertical.get(tags).add(i);
                } else {
                    List<Integer> list = new ArrayList<>();
                    list.add(i);
                    vertical.put(tags, list);
                }
            }
            in.nextLine();
        }

        //horizontal.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));

        //vertical.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));

        List<Slide> slideSolution = greedy(horizontal, vertical);
        List<String> results = prepareResultToPrint(slideSolution);

        if (log) {
            //log(String.format(TAG, "Result"));
            printResult(results);
            //System.out.println("Score: " + score(slideSolution));
        }

        if (saveResult) {
            saveResultToFile(results);
        }
    }

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
                    return (i + 1) < args.length && !args[i + 1].startsWith("-") ? Integer.valueOf(args[i + 1]) : defaultVal;
        }

        return defaultVal;
    }

    private static int score(List<Slide> solution) {
        int score = 0;
        for (int i = 0; i < solution.size() - 1; i++) {
            score += solution.get(i).scoreTransition(solution.get(i + 1));
        }
        return score;
    }

    public static List<Slide> greedy(Map<Set<String>, List<Integer>> horizontal, Map<Set<String>, List<Integer>> vertical) {

        Slide firstSlide = null;

        Optional<Map.Entry<Set<String>, List<Integer>>> first = horizontal.entrySet().stream().findAny();
        if (first.isPresent()) {
            Integer id = first.get().getValue().remove(0);
            if (first.get().getValue().isEmpty()) {
                horizontal.remove(first.get().getKey());
            }
            firstSlide = new Slide(id, first.get().getKey());
        } else {
            first = vertical.entrySet().stream().findAny();
            final Optional<Map.Entry<Set<String>, List<Integer>>> temp = first;
            Optional<Map.Entry<Set<String>, List<Integer>>> second = vertical.entrySet().stream().filter(s -> !s.equals(temp.get())).findAny();
            Integer idi = first.get().getValue().remove(0);
            Integer idj = second.get().getValue().remove(0);

            if (first.get().getValue().isEmpty()) {
                vertical.remove(first.get().getKey());
            }

            if (second.get().getValue().isEmpty()) {
                vertical.remove(second.get().getKey());
            }

            firstSlide = new Slide(idi, idj, first.get().getKey(), second.get().getKey());
        }

        List<Slide> solution = new ArrayList<>(horizontal.size() + vertical.size());
        solution.add(firstSlide);


        while (!horizontal.isEmpty() || vertical.size() >= 2){
            /*System.out.println("Horizontals: ");
            horizontal.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));
            System.out.println("Verticals: ");
            vertical.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));*/
            System.out.println("Horizontals: " + horizontal.size() + " vertical: " + vertical.size());
            int horMaxScore = -1;
            Slide hor = null;
            int verMaxScore = 0;
            Slide ver = null;
            Slide last = solution.get(solution.size() - 1);
            Set<String> foundVerTags1 = null;
            Set<String> foundVerTags2 = null;

            List<Map.Entry<Set<String>, List<Integer>>> horList = new ArrayList<>(horizontal.entrySet());
            for (int i = 0; i < horList.size(); i += GRAN) {
                Integer index = horList.get(i).getValue().get(0);
                Slide next = new Slide(index, horList.get(i).getKey());
                int score = last.scoreTransition(next);
                if (score > horMaxScore) {
                    horMaxScore = score;
                    hor = next;
                }
            }

            int bestVerticalScore = -1;
            List<Map.Entry<Set<String>, List<Integer>>> verList = new ArrayList<>(vertical.entrySet());
           for (int i = 0; i < verList.size() - 1; i+=GRAN) {
               int id1 = verList.get(i).getValue().get(0);
               int id2 = verList.get(i + 1).getValue().get(0);
               Set<String> verTags1 = verList.get(i).getKey();
               Set<String> verTags2 = verList.get(i + 1).getKey();
               Slide next = new Slide(id1, id2, verTags1, verTags2);
               if(last.scoreTransition(next) > bestVerticalScore) {
                   foundVerTags1 = verTags1;
                   foundVerTags2 = verTags2;
                   ver = next;
                   bestVerticalScore = last.scoreTransition(next);
               }
           }

            int verScore = ver == null ? -1 :last.scoreTransition(ver);
            int horScore = hor == null ? -1 :last.scoreTransition(hor);

            if (horScore > verScore && (hor != null)) {
                List<Integer> photos = horizontal.get(hor.tags);

                if (photos.size() == 1) {
                    horizontal.remove(hor.tags);
                } else {
                    photos.remove(0);
                }

                solution.add(hor);
            } else if (foundVerTags1 != null && foundVerTags2 != null) {

                List<Integer> photos1 = vertical.get(foundVerTags1);
                List<Integer> photos2 = vertical.get(foundVerTags2);

                if (photos1.size() == 1) {
                    vertical.remove(foundVerTags1);
                } else {
                    photos1.remove(0);
                }

                if (photos2.size() == 1) {
                    vertical.remove(foundVerTags2);
                } else {
                    photos2.remove(0);
                }

                solution.add(ver);
            }
        }

        return solution;
    }


    private static void printResult(List<String> results) {
        for (String line : results) {
            System.out.println(line);
        }
    }

    private static void saveResultToFile(List<String> slidesolution) {
        Path path = Paths.get(".", RESULT_FILENAME);
        try {
            Files.write(path, slidesolution);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static List<String> prepareResultToPrint(List<Slide> slidesolution) {
        List<String> results = slidesolution.stream().map(Slide::toSaveString).collect(Collectors.toList());
        results.add(0, String.valueOf(slidesolution.size()));
        return results;
    }

    public static class Slide {
        boolean horizontal;
        int i;
        int j;
        Set<String> tags;

        public Slide(int i, int j, Set<String> tags1, Set<String> tags2) {
            this.horizontal = false;
            this.i = i;
            this.j = j;
            Set<String> copy = new HashSet<>(tags1);
            copy.addAll(tags2);
            this.tags = copy;
        }

        public Slide(int i, Set<String> tags) {
            this.horizontal = true;
            this.i = i;
            this.tags = tags;
        }

        public String toSaveString() {
            if (!horizontal) {
                return i + " " + j;
            } else {
                return String.valueOf(i);
            }
        }

        int scoreTransition(Slide slide) {
            return Math.min(Math.min(hereButNotThere(slide), thereButNotHere(slide)), commonTags(slide));
        }

        int commonTags(Slide slide) {
            Set<String> copy = new HashSet<>(tags);
            copy.retainAll(slide.tags);
            return copy.size();
        }

        int hereButNotThere(Slide slide) {
            Set<String> copy = new HashSet<>(this.tags);
            copy.removeAll(slide.tags);
            return copy.size();
        }

        int thereButNotHere(Slide slide) {
            Set<String> copy = new HashSet<>(slide.tags);
            copy.removeAll(tags);
            return copy.size();
        }
    }
}
