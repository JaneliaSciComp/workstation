package org.janelia.jacs2.asyncservice.utils;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptWriter {

    private static final String DEFAULT_INDENT = "    ";

    private final Writer w;
    private String indent = "";

    public ScriptWriter(Writer w) {
        this.w = w;
    }

    public ScriptWriter openFunction(String fname) throws IOException {
        w.append(indent).append("function ").append(fname).append(" {").append('\n');
        addIndent();
        return this;
    }

    public ScriptWriter closeFunction(String fname) throws IOException {
        removeIndent();
        w.append(indent).append("}").append(" # end ").append(fname).append('\n');
        return this;
    }

    public ScriptWriter addIndent() {
        indent = indent + DEFAULT_INDENT;
        return this;
    }

    public ScriptWriter removeIndent() {
        if (indent.length() >= DEFAULT_INDENT.length()) {
            indent = indent.substring(DEFAULT_INDENT.length());
        }
        return this;
    }

    public ScriptWriter addWithArgs(String line) {
        try {
            w.append(indent).append(line);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addArg(String arg) {
        try {
            w.append(' ').append(arg);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addArgFlag(String argFlag, String argValue) {
        try {
            if (StringUtils.isNotBlank(argValue)) {
                w.append(' ').append(argFlag).append(' ').append(argValue);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addArgFlag(String argFlag, int argValue) {
        try {
            if (argValue != 0) {
                w.append(' ').append(argFlag).append(' ').append(String.valueOf(argValue));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addArgFlag(String argFlag, Boolean argValue) {
        try {
            if (argValue) {
                w.append(' ').append(argFlag);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addArgFlag(String argFlag, List<Integer> argValue, String separator) {
        try {
            if (CollectionUtils.isNotEmpty(argValue)) {
                w.append(' ').append(argFlag).append(' ').append(argValue.stream().map(String::valueOf).collect(Collectors.joining(separator)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addArgs(String... args) {
        addArgs(Arrays.asList(args));
        return this;
    }

    public ScriptWriter addArgs(Iterable<String> args) {
        args.forEach(this::addArg);
        return this;
    }

    public ScriptWriter endArgs(String arg) {
        try {
            w.append(' ').append(arg).append('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter add(String line) {
        try {
            w.append(indent).append(line).append('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ScriptWriter addBackground(String line) throws IOException {
        w.append(indent).append(line).append('&').append('\n');
        return this;
    }

    public ScriptWriter setVar(String var, String value) throws IOException {
        w.append(indent).append(var).append('=').append(value).append('\n');
        return this;
    }

    public ScriptWriter exportVar(String var, String value) throws IOException {
        w.append(indent).append("export ").append(var).append('=').append(value).append('\n');
        return this;
    }

    public ScriptWriter echo(String line) throws IOException {
        w.append(indent).append("echo ").append('"').append(line).append('"').append('\n');
        return this;
    }

    public void close() {
        try {
            w.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
