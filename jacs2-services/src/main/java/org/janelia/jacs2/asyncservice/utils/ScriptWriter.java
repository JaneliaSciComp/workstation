package org.janelia.jacs2.asyncservice.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

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
        w.append(indent).append(var).append('=').append('"').append(value).append('"').append('\n');
        return this;
    }

    public ScriptWriter exportVar(String var, String value) throws IOException {
        w.append(indent).append("EXPORT ").append(var).append('=').append('"').append(value).append('"').append('\n');
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
