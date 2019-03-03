
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
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;


public class Main {

    private static final String TAG = "///////////////////////////////////// %s /////////////////////////////////////";
    private static final String RESULT_FILENAME = "result.out";
    private static boolean log = false;
    private static boolean concurrent = false;
    private static int THREADS;
    private static int GRAN_H;
    private static int GRAN_V1;
    private static int GRAN_V2;
    private static String stats = "";
    private static boolean s = false;
    private static int size;

    public static void main(String[] args) {
        log |= option(args, "-l");
        concurrent |= option(args, "-c");
        s |= option(args, "-s");
        GRAN_H = checkValue(args, "-gran_h", 1);
        GRAN_V1 = checkValue(args, "-gran_v1", 1);
        GRAN_V2 = checkValue(args, "-gran_v2", 1);
        THREADS = checkValue(args, "-c", 8);
        stats+= "Horizontal gran: " + GRAN_H + "\n";
        stats+= "Vertical 1st level gran: " + GRAN_V1 + "\n";
        stats+= "Vertical 2nd level gran: " + GRAN_V2 + "\n";

        Map<Set<String>, List<Integer>> horizontal = new HashMap<>();
        Map<Set<String>, List<Integer>> vertical = new HashMap<>();

        read(horizontal, vertical);

        if (log) {
          //horizontal.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));
          //vertical.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));
        }

        List<Slide> slideSolution = greedy(horizontal, vertical);

        stats += "Score: " + score(slideSolution) + "\n";

        if(s)
          System.out.println(stats);

        printResult(prepareResultToPrint(slideSolution));
    }

    private static void read(Map<Set<String>, List<Integer>> horizontal, Map<Set<String>, List<Integer>> vertical) {
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
        long starttime = System.currentTimeMillis();
        List<Slide> solution = new ArrayList<>(horizontal.size() + vertical.size());
        Slide firstSlide = getFirstSlide(horizontal, vertical);
        solution.add(firstSlide);

        List<Map.Entry<Set<String>, List<Integer>>> horList = new ArrayList<>(horizontal.entrySet());
        List<Map.Entry<Set<String>, List<Integer>>> verList = new ArrayList<>(vertical.entrySet());
        while (!horList.isEmpty() || verList.size() >= 2){
            /*System.out.println("Horizontals: ");
            horizontal.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));
            System.out.println("Verticals: ");
            vertical.entrySet().stream().forEach(e -> System.out.println("Key: " + e.getKey() + " value: " + Arrays.toString(e.getValue().toArray())));*/
            log("Horizontals: " + horList.size() + " vertical: " + verList.size());
            Slide last = solution.get(solution.size() - 1);

            SlideHolder hor = new SlideHolder();
            AtomicInteger horMaxScore = new AtomicInteger(-1);
            AtomicInteger horMaxScoreIndex = new AtomicInteger(-1);

            Slide ver = null;
            int verMaxScore = -1;
            int verIndex1 = -1;
            int verIndex2 = -1;

            ExecutorService hPool = Executors.newFixedThreadPool(THREADS);
            final int chunkSize = horList.size()/THREADS + 1;
            log("Chunk size: " + chunkSize);
            for (int i = 0; i < THREADS; i++) {
              final int I = i;
              hPool.execute(() -> horizontalUnitOfWork(horList, horMaxScore, horMaxScoreIndex, hor, last, I*chunkSize, (I + 1)*chunkSize));
            }
            hPool.shutdown();
            try {
              hPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
            }

            if (verList.size() >= 2) {

              for (int i = 0; i < verList.size() - 1; i+=GRAN_V1) {
                int id1 = verList.get(i).getValue().get(0);
                Set<String> verTags1 = verList.get(i).getKey();
                for(int j = 0; j < verList.size(); j+=GRAN_V2) {
                  if (i == j)
                    if (j == verList.size() - 1)
                      break;
                    else
                      j++;

                  int id2 = verList.get(j).getValue().get(0);
                  Set<String> verTags2 = verList.get(j).getKey();
                  Slide next = new Slide(id1, id2, verTags1, verTags2);
                  int score = last.scoreTransition(next);

                  if(score > verMaxScore) {
                    verIndex1 = i;
                    verIndex2 = j;
                    verMaxScore = score;
                    ver = next;
                  }
                }
              }
            }

            log("Hor score: " + horMaxScore.get() + " ver score: " + verMaxScore);
            if (horMaxScore.get() > verMaxScore) {
                List<Integer> photos = horList.get(horMaxScoreIndex.get()).getValue();

                if (photos.size() == 1) {
                    horList.remove(horMaxScoreIndex.get());
                } else {
                    photos.remove(0);
                }

                solution.add(hor.slide);
            } else {
                int biggerIndex = verIndex1 > verIndex2 ? verIndex1 : verIndex2;
                int smallerIndex = verIndex1 > verIndex2 ? verIndex2 : verIndex1;

                List<Integer> biggerPhotos = verList.get(biggerIndex).getValue();
                List<Integer> smallerPhotos = verList.get(smallerIndex).getValue();

                if (biggerPhotos.size() == 1) {
                    verList.remove(biggerIndex);
                } else {
                    biggerPhotos.remove(0);
                }

                if (smallerPhotos.size() == 1) {
                    verList.remove(smallerIndex);
                } else {
                    smallerPhotos.remove(0);
                }

                solution.add(ver);
            }
        }
        long endtime = System.currentTimeMillis();
        stats += "The algorithm took: " + (endtime-starttime) + " millis.\n";
        return solution;
    }

    private static void horizontalUnitOfWork(List<Map.Entry<Set<String>, List<Integer>>> horList, AtomicInteger score, AtomicInteger index, SlideHolder slideHolder, Slide last, int start, int end) {
      for (int i = start; (i < end) && (i < horList.size()); i += GRAN_H) {
        Integer photoId = horList.get(i).getValue().get(0);
        Slide next = new Slide(photoId, horList.get(i).getKey());
        int tempScore = last.scoreTransition(next);
        if (tempScore > score.get()) {
          synchronized (score) {
            if (tempScore > score.get()) {
              index.set(i);
              score.set(tempScore);
              slideHolder.slide = next;
            }
          }
        }
      }
    }

    private static Slide getFirstSlide(Map<Set<String>, List<Integer>> horizontal, Map<Set<String>, List<Integer>> vertical) {
      Optional<Map.Entry<Set<String>, List<Integer>>> first = horizontal.entrySet().stream().min((e1, e2) -> e1.getKey().size() - e2.getKey().size());
      if (first.isPresent()) {
          Integer id = first.get().getValue().remove(0);
          if (first.get().getValue().isEmpty()) {
              horizontal.remove(first.get().getKey());
          }
          return new Slide(id, first.get().getKey());
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

          return new Slide(idi, idj, first.get().getKey(), second.get().getKey());
      }
    }

    private static void printResult(List<String> results) {
        for (String line : results) {
            System.out.println(line);
        }
    }

    private static List<String> prepareResultToPrint(List<Slide> slidesolution) {
        List<String> results = slidesolution.stream().map(Slide::toSaveString).collect(Collectors.toList());
        results.add(0, String.valueOf(slidesolution.size()));
        return results;
    }

    public static class SlideHolder {
      Slide slide;

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
