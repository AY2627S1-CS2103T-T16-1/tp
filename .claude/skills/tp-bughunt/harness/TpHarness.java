import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Drives an AB3-derived tP JAR's Logic headlessly, one command per stdin line.
 *
 * Run from the sandbox run directory (see sandbox-run.sh):
 *   java -cp target.jar TpHarness.java [--main-app fully.qualified.MainApp] [--timeout-ms 5000]
 *
 * It constructs the team's MainApp, calls init() exactly as the real launcher
 * would (same working directory, data file and preferences), then pulls the
 * Logic field out by reflection. Each command runs on a fresh thread with the
 * default stack size, which matches the JavaFX Application Thread that runs
 * commands in the real GUI, so StackOverflowError reproduces faithfully.
 *
 * Input lines:   any text      -> executed as a command, verbatim
 *                #! <text>     -> executed with Java escapes decoded (\n, \t, \\uXXXX, \0)
 *                #repeat N UNIT <text> -> <text> with its '%s' replaced by UNIT repeated N times
 *                                         (UNIT is escape-decoded and has no spaces; use \\u0020)
 *                # comment     -> ignored
 * Output lines on stdout (logs go to stderr):
 *   CMD <n> <command, escaped, truncated>
 *   OK <ms>ms <feedback, escaped>
 *   REJECT <ms>ms <ExceptionClass>: <message>      checked command/parse exception (normal)
 *   CRASH <ms>ms <ThrowableClass>: <message> @ <top frames>   anything else: a bug candidate
 *   HANG <timeout>ms                                 then the harness exits with code 3
 * Exit codes: 0 done, 2 could not bootstrap, 3 hang.
 */
public class TpHarness {
    private static final int SHOWN_CHARS = 300;
    private static final int SHOWN_FRAMES = 6;

    public static void main(String[] args) throws Exception {
        String mainAppName = null;
        long timeoutMs = 5000;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--main-app")) {
                mainAppName = args[++i];
            } else if (args[i].equals("--timeout-ms")) {
                timeoutMs = Long.parseLong(args[++i]);
            }
        }
        Object logic;
        try {
            logic = bootstrap(mainAppName);
        } catch (Throwable t) {
            System.out.println("BOOTSTRAP-FAILED " + describe(t));
            System.out.println("Hint: pass --main-app <class extending javafx.application.Application>,"
                    + " or fall back to driving the GUI.");
            System.exit(2);
            return;
        }
        Method execute = findMethod(logic.getClass(), "execute", String.class);
        System.out.println("READY logic=" + logic.getClass().getName());

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        int n = 0;
        while ((line = in.readLine()) != null) {
            String command = toCommand(line);
            if (command == null) {
                continue;
            }
            n++;
            System.out.println("CMD " + n + " " + shorten(escape(command)));
            run(logic, execute, command, timeoutMs);
        }
        System.out.flush();
        System.exit(0);
    }

    private static String toCommand(String line) {
        if (line.startsWith("#! ")) {
            return unescape(line.substring(3));
        }
        if (line.startsWith("#repeat ")) {
            String[] parts = line.split(" ", 4);
            return parts[3].replace("%s", unescape(parts[2]).repeat(Integer.parseInt(parts[1])));
        }
        if (line.startsWith("#")) {
            return null;
        }
        return line;
    }

    private static void run(Object logic, Method execute, String command, long timeoutMs) throws Exception {
        Object[] outcome = new Object[1];
        Thread worker = new Thread(() -> {
            try {
                outcome[0] = execute.invoke(logic, command);
            } catch (java.lang.reflect.InvocationTargetException e) {
                outcome[0] = e.getCause();
            } catch (Throwable t) {
                outcome[0] = t;
            }
        }, "tp-harness-command");
        worker.setDaemon(true);
        long start = System.nanoTime();
        worker.start();
        worker.join(timeoutMs);
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (worker.isAlive()) {
            System.out.println("HANG " + timeoutMs + "ms");
            System.out.flush();
            System.exit(3);
        }
        Object result = outcome[0];
        if (result instanceof Throwable t) {
            String kind = isExpectedRejection(t) ? "REJECT " : "CRASH ";
            System.out.println(kind + ms + "ms " + describe(t));
        } else {
            System.out.println("OK " + ms + "ms " + shorten(escape(feedbackOf(result))));
        }
        System.out.flush();
    }

    /** Command and parse exceptions are how AB3 reports user errors; anything else is unexpected. */
    private static boolean isExpectedRejection(Throwable t) {
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String name = c.getSimpleName();
            if (name.equals("CommandException") || name.equals("ParseException")) {
                return true;
            }
        }
        return false;
    }

    private static Object bootstrap(String mainAppName) throws Exception {
        Class<?> appClass = Class.forName(mainAppName != null ? mainAppName : guessMainApp());
        Object app = appClass.getDeclaredConstructor().newInstance();
        registerEmptyParameters(app);
        appClass.getMethod("init").invoke(app);
        for (Class<?> c = appClass; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                Object value = f.get(app);
                if (value != null && findMethodOrNull(value.getClass(), "execute", String.class) != null
                        && f.getName().toLowerCase().contains("logic")) {
                    return value;
                }
            }
        }
        throw new IllegalStateException("no field named *logic* with execute(String) on " + appClass.getName());
    }

    /** Guesses <package of Main-Class>.MainApp, the AB3 layout. */
    private static String guessMainApp() throws Exception {
        String jar = System.getProperty("java.class.path").split(java.io.File.pathSeparator)[0];
        try (JarFile jarFile = new JarFile(jar)) {
            String main = jarFile.getManifest().getMainAttributes().getValue("Main-Class");
            return main.substring(0, main.lastIndexOf('.')) + ".MainApp";
        }
    }

    /** AB3's original init() calls getParameters(), which is null unless the launcher registered some. */
    private static void registerEmptyParameters(Object app) {
        try {
            Class<?> impl = Class.forName("com.sun.javafx.application.ParametersImpl");
            Object params = impl.getConstructor(String[].class).newInstance((Object) new String[0]);
            impl.getMethod("registerParameters", Class.forName("javafx.application.Application"),
                    Class.forName("javafx.application.Application$Parameters")).invoke(null, app, params);
        } catch (Throwable t) {
            System.err.println("[harness] could not register parameters: " + t);
        }
    }

    private static String feedbackOf(Object result) {
        if (result == null) {
            return "null";
        }
        Method m = findMethodOrNull(result.getClass(), "getFeedbackToUser");
        try {
            return m != null ? String.valueOf(m.invoke(result)) : result.toString();
        } catch (Exception e) {
            return result.toString();
        }
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... types) {
        Method m = findMethodOrNull(c, name, types);
        if (m == null) {
            throw new IllegalStateException("no " + name + " on " + c.getName());
        }
        return m;
    }

    private static Method findMethodOrNull(Class<?> c, String name, Class<?>... types) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(name, types);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                // keep walking up
            }
        }
        return null;
    }

    private static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getName());
        String message = t.getMessage();
        if (message != null) {
            sb.append(": ").append(shorten(escape(message)));
        }
        if (!isExpectedRejection(t)) {
            sb.append(" @");
            List<String> frames = new ArrayList<>();
            for (StackTraceElement e : t.getStackTrace()) {
                if (frames.size() == SHOWN_FRAMES) {
                    break;
                }
                frames.add(" " + e.getClassName() + "." + e.getMethodName() + ":" + e.getLineNumber());
            }
            sb.append(String.join(" <", frames));
        }
        return sb.toString();
    }

    private static String shorten(String s) {
        return s.length() <= SHOWN_CHARS ? s : s.substring(0, SHOWN_CHARS) + "...(" + s.length() + " chars)";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\t') {
                sb.append("\\t");
            } else if (ch < 0x20 || (ch >= 0x7f && ch < 0xa0) || Character.getType(ch) == Character.FORMAT) {
                sb.append(String.format("\\u%04x", (int) ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\\' || i + 1 == s.length()) {
                sb.append(ch);
                continue;
            }
            char next = s.charAt(++i);
            switch (next) {
            case 'n' -> sb.append('\n');
            case 't' -> sb.append('\t');
            case '0' -> sb.append('\0');
            case 'u' -> {
                sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                i += 4;
            }
            default -> sb.append(next);
            }
        }
        return sb.toString();
    }
}
