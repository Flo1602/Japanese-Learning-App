package at.primetshofer.logic.provider.polygon;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.logic.parser.ISVGParser;
import at.primetshofer.logic.provider.file.IFileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SVGPolygonProvider implements IPolygonProvider {


    private final IFileProvider fileProvider;
    private final ISVGParser svgParser;

    public SVGPolygonProvider(IFileProvider fileProvider, ISVGParser svgParser, Options options) {
        this.fileProvider = fileProvider;
        this.svgParser = svgParser;
    }

    @Override
    public List<Polygon> getAllPolygons() {
        File svgFile = this.fileProvider.provideFile();
        return null;
    }

    public List<Polygon> svgPathToPolygons(String svgPathString, Options opts) {
        List<Polygon> polys = new ArrayList<>();
        double tolerance2 = opts.tolerance * opts.tolerance;
        Polygon poly = null;
        Command prev = null;

        List<Command> commands = makeAbsolute(parseSVG(svgPathString));
        for (Command cmd : commands) {
            switch (cmd.code) {
                case "M":
                    poly = new Polygon();
                    polys.add(poly);
                    // intentional fall-through
                case "L":
                case "H":
                case "V":
                case "Z":
                    add(poly, cmd.x, cmd.y, opts);
                    break;
                case "C":
                    sampleCubicBezier(cmd.x0, cmd.y0, cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y, poly, tolerance2, opts);
                    add(poly, cmd.x, cmd.y, opts);
                    break;
                case "S":
                    double x1 = 0, y1 = 0;
                    if (prev != null) {
                        if (prev.code.equals("C")) {
                            x1 = prev.x * 2 - prev.x2;
                            y1 = prev.y * 2 - prev.y2;
                        } else {
                            x1 = prev.x;
                            y1 = prev.y;
                        }
                    }
                    sampleCubicBezier(cmd.x0, cmd.y0, x1, y1, cmd.x2, cmd.y2, cmd.x, cmd.y, poly, tolerance2, opts);
                    add(poly, cmd.x, cmd.y, opts);
                    break;
                default:
                    System.err.println("Our deepest apologies, but " + cmd.command + " commands (" + cmd.code + ") are not yet supported.");
                    System.exit(2);
            }
            prev = cmd;
        }
        return polys;
    }

    // Function to approximate a cubic Bézier curve recursively
    private void sampleCubicBezier(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, Polygon poly, double tolerance2, Options opts) {
        // Calculate all the mid-points of the line segments
        double x01 = (x0 + x1) / 2.0;
        double y01 = (y0 + y1) / 2.0;
        double x12 = (x1 + x2) / 2.0;
        double y12 = (y1 + y2) / 2.0;
        double x23 = (x2 + x3) / 2.0;
        double y23 = (y2 + y3) / 2.0;
        double x012 = (x01 + x12) / 2.0;
        double y012 = (y01 + y12) / 2.0;
        double x123 = (x12 + x23) / 2.0;
        double y123 = (y12 + y23) / 2.0;
        double x0123 = (x012 + x123) / 2.0;
        double y0123 = (y012 + y123) / 2.0;

        // Try to approximate the full cubic curve by a single straight line
        double dx = x3 - x0;
        double dy = y3 - y0;

        double d1 = Math.abs(((x1 - x3) * dy - (y1 - y3) * dx));
        double d2 = Math.abs(((x2 - x3) * dy - (y2 - y3) * dx));

        if (((d1 + d2) * (d1 + d2)) < (tolerance2 * (dx * dx + dy * dy))) {
            add(poly, x0123, y0123, opts);
        } else {
            // Continue subdivision
            sampleCubicBezier(x0, y0, x01, y01, x012, y012, x0123, y0123, poly, tolerance2, opts);
            sampleCubicBezier(x0123, y0123, x123, y123, x23, y23, x3, y3, poly, tolerance2, opts);
        }
    }

    // Function to add a point to the current polygon
    private void add(Polygon poly, double x, double y, Options opts) {
        if (opts.decimals != null && opts.decimals >= 0) {
            x = roundToDecimals(x, opts.decimals);
            y = roundToDecimals(y, opts.decimals);
        }

        poly.getVertices().add(new Point((int) x, (int) y));
    }

    // Helper function to round a value to a specified number of decimals
    private double roundToDecimals(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    // Implementation for parseSVG function
    private List<Command> parseSVG(String svgPathString) {
        List<Command> commands = new ArrayList<>();
        String svgPath = svgPathString.replaceAll("[\\n\\r]", " ").trim();

        // Regular expression to match SVG path commands and their parameters
        Pattern pattern = Pattern.compile("([MmZzLlHhVvCcSsQqTtAa])|([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)");
        Matcher matcher = pattern.matcher(svgPath);

        String currentCommand = null;
        List<Double> params = new ArrayList<>();

        while (matcher.find()) {
            String match = matcher.group();
            if (match.matches("[MmZzLlHhVvCcSsQqTtAa]")) {
                // Process the previous command
                if (currentCommand != null) {
                    extractCommands(currentCommand, params, commands);
                    params.clear();
                }
                currentCommand = match;
            } else {
                params.add(Double.parseDouble(match));
            }
        }
        // Process the last command
        if (currentCommand != null) {
            extractCommands(currentCommand, params, commands);
        }
        return commands;
    }

    // Helper method to extract commands and their parameters
    private void extractCommands(String commandChar, List<Double> params, List<Command> commands) {
        int paramCount = getParamCount(commandChar);
        String code = commandChar.toUpperCase();

        for (int i = 0; i < params.size(); i += paramCount) {
            Command cmd = new Command(code);
            cmd.command = commandChar;

            int remaining = params.size() - i;
            if (remaining < paramCount) {
                System.err.println("Invalid parameter count for command: " + commandChar);
                break;
            }

            switch (code) {
                case "M":
                case "L":
                    cmd.x = params.get(i);
                    cmd.y = params.get(i + 1);
                    break;
                case "H":
                    cmd.x = params.get(i);
                    break;
                case "V":
                    cmd.y = params.get(i);
                    break;
                case "C":
                    cmd.x1 = params.get(i);
                    cmd.y1 = params.get(i + 1);
                    cmd.x2 = params.get(i + 2);
                    cmd.y2 = params.get(i + 3);
                    cmd.x = params.get(i + 4);
                    cmd.y = params.get(i + 5);
                    break;
                case "S":
                    cmd.x2 = params.get(i);
                    cmd.y2 = params.get(i + 1);
                    cmd.x = params.get(i + 2);
                    cmd.y = params.get(i + 3);
                    break;
                case "Z":
                    // No parameters
                    break;
                default:
                    System.err.println("Command not supported: " + code);
            }
            cmd.code = code;
            cmd.command = commandChar;
            cmd.x0 = 0;
            cmd.y0 = 0;
            commands.add(cmd);
        }
    }

    // Helper method to get the number of parameters for each command
    private int getParamCount(String command) {
        switch (command.toUpperCase()) {
            case "M":
            case "L":
                return 2;
            case "H":
            case "V":
                return 1;
            case "C":
                return 6;
            case "S":
                return 4;
            default:
                return 0;
        }
    }

    // Implementation for makeAbsolute function
    private List<Command> makeAbsolute(List<Command> commands) {
        double currentX = 0, currentY = 0;
        double subPathStartX = 0, subPathStartY = 0;
        for (Command cmd : commands) {
            boolean isRelative = Character.isLowerCase(cmd.command.charAt(0));

            switch (cmd.code) {
                case "M":
                case "L":
                    if (isRelative) {
                        cmd.x += currentX;
                        cmd.y += currentY;
                    }
                    currentX = cmd.x;
                    currentY = cmd.y;
                    if (cmd.code.equals("M")) {
                        subPathStartX = currentX;
                        subPathStartY = currentY;
                    }
                    cmd.x0 = currentX;
                    cmd.y0 = currentY;
                    break;
                case "H":
                    if (isRelative) {
                        cmd.x += currentX;
                    }
                    currentX = cmd.x;
                    cmd.x0 = currentX;
                    cmd.y0 = currentY;
                    cmd.y = currentY;
                    break;
                case "V":
                    if (isRelative) {
                        cmd.y += currentY;
                    }
                    currentY = cmd.y;
                    cmd.x0 = currentX;
                    cmd.y0 = currentY;
                    cmd.x = currentX;
                    break;
                case "C":
                    if (isRelative) {
                        cmd.x1 += currentX;
                        cmd.y1 += currentY;
                        cmd.x2 += currentX;
                        cmd.y2 += currentY;
                        cmd.x += currentX;
                        cmd.y += currentY;
                    }
                    cmd.x0 = currentX;
                    cmd.y0 = currentY;
                    currentX = cmd.x;
                    currentY = cmd.y;
                    break;
                case "S":
                    if (isRelative) {
                        cmd.x2 += currentX;
                        cmd.y2 += currentY;
                        cmd.x += currentX;
                        cmd.y += currentY;
                    }
                    cmd.x0 = currentX;
                    cmd.y0 = currentY;
                    currentX = cmd.x;
                    currentY = cmd.y;
                    break;
                case "Z":
                    currentX = subPathStartX;
                    currentY = subPathStartY;
                    cmd.x0 = currentX;
                    cmd.y0 = currentY;
                    cmd.x = currentX;
                    cmd.y = currentY;
                    break;
                default:
                    System.err.println("Command not supported in makeAbsolute: " + cmd.code);
            }
            cmd.code = cmd.code.toUpperCase();
        }
        return commands;
    }

    public static class Options {

        private final double tolerance;
        private final Integer decimals;
        private final File svgFile;

        public Options(double tolerance, Integer decimals, File svgFile) {
            this.tolerance = tolerance;
            this.decimals = decimals;
            this.svgFile = svgFile;
        }
    }

    private static class Command {
        public String code;
        public String command;
        public double x0, y0; // Starting point
        public double x1, y1; // Control point 1
        public double x2, y2; // Control point 2
        public double x, y;   // End point

        // Constructor and other necessary methods
        public Command(String code) {
            this.code = code;
            this.command = code; // For simplicity, we can assign code to command
        }
    }
}
