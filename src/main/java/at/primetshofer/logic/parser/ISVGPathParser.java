package at.primetshofer.logic.parser;

import java.io.File;
import java.util.List;

public interface ISVGPathParser {
    List<String> parse(File file);
}
