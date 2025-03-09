package at.primetshofer.logic.parser;

import at.primetshofer.view.catalog.CreateEditWordWindow;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SVGPathParser implements ISVGPathParser {

    private static final Logger logger = Logger.getLogger(SVGPathParser.class);

    @Override
    public List<String> parse(File file) {
        String regex = "d=\"([^\"]+)\"\\/>";
        List<String> parsedSVGPaths = new ArrayList<>();

        try {
            String content = new String(Files.readAllBytes(file.getAbsoluteFile().toPath()));
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String svgPath = matcher.group(1);
                parsedSVGPaths.add(svgPath);
            }
        } catch (IOException ex) {
            logger.error("Failed to parse SVG paths", ex);
            throw new RuntimeException(ex);
        }

        return parsedSVGPaths;
    }
}
