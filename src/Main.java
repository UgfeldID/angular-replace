import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InputParams {

    String tagName;

    String newTagName;

    List<String> inputParams;

    List<String> outputParams;
}

public class Main {

    private static long HANDLED_TAGS_COUNT = 0;

    public static void main(String[] args) {

        InputParams params = new InputParams();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter files directory or file path: ");
        String path = scanner.nextLine();

        System.out.println("Enter current tag name: ");
        params.tagName = scanner.nextLine();

        System.out.println("Enter renamed tag name (if needed): ");
        params.newTagName = scanner.nextLine();

        System.out.println("Enter input params names: ");
        params.inputParams = parseToList(scanner.nextLine());

        System.out.println("Enter output params names: ");
        params.outputParams = parseToList(scanner.nextLine());

        handleFiles(params, path);
    }

    private static void handleFiles(InputParams params, String path) {

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths.filter(Files::isRegularFile).forEach(file -> {

                if (file.getFileName().toString().endsWith(".html")) {
                    try {
                        List<String> content = Files.readAllLines(file, StandardCharsets.UTF_8);
                        List<String> result = replace(content, params);
                        if (!content.equals(result))
                            Files.write(file, result);
                    } catch (IOException e) {
                        System.out.println("Error while handling file: " + file);
                        e.printStackTrace();
                    }
                }
            });

            System.out.println("Handled tags count: " + HANDLED_TAGS_COUNT);
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> parseToList(String source) {

        return source != null && !source.isEmpty() ? Arrays.asList(source.split(" ")) :
                Collections.emptyList();
    }

    private static List<String> replace(List<String> source, InputParams params) {

        Pattern tagPattern =
                Pattern.compile(String.format("<%s\\b|</%s>", params.tagName, params.tagName));
        Pattern startPattern = Pattern.compile(String.format("<%s\\b", params.tagName));
        Pattern endPattern = Pattern.compile(">");
        AtomicBoolean isReplacementMode = new AtomicBoolean(false);

        return source.stream().map(line -> {
            String result = line;

            Matcher startMatcher = startPattern.matcher(line);
            if (startMatcher.find()) {
                isReplacementMode.set(true);
            }

            if (isReplacementMode.get()) {

                Matcher endMatcher = endPattern.matcher(line);
                String tail = "";

                if (endMatcher.find()) {
                    result = line.substring(0, endMatcher.start() + 1);
                    tail = line.substring(endMatcher.end());
                    isReplacementMode.set(false);
                    HANDLED_TAGS_COUNT++;
                }

                result = replaceInputParams(result, params.inputParams);
                result = replaceOutputParams(result, params.outputParams);
                result += tail;

            }

            return replaceTagIfNeeded(params, tagPattern, line, result);
        }).collect(Collectors.toList());
    }

    private static String replaceTagIfNeeded(InputParams params, Pattern tagPattern, String line,
            String result) {

        if (params.newTagName != null && !params.newTagName.isEmpty()) {
            Matcher tagMatcher = tagPattern.matcher(line);
            if (tagMatcher.find())
                result = result.replaceAll(params.tagName, params.newTagName);
        }
        return result;
    }

    private static String replaceInputParams(String source, List<String> params) {

        AtomicReference<String> result = new AtomicReference<>(source);
        params.forEach(par -> result.set(result.get().replace(par, "[" + par + "]")));

        return result.get();
    }

    private static String replaceOutputParams(String source, List<String> params) {

        AtomicReference<String> result = new AtomicReference<>(source);
        params.forEach(par -> result.set(result.get().replace(par, "(" + par + ")")));

        return result.get();
    }
}
