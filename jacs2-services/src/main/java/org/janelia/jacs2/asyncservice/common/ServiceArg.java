package org.janelia.jacs2.asyncservice.common;

import org.apache.commons.lang3.StringUtils;

public class ServiceArg {
    private final String flag;
    private final int arity;
    private final String[] values;

    public ServiceArg(String flag, int arity, String... values) {
        this.flag = flag;
        this.arity = arity;
        this.values = values;
    }

    public ServiceArg(String flag, String value) {
        if (StringUtils.isBlank(value)) {
            this.flag = null;
            this.arity = 0;
            this.values = null;
        } else {
            this.flag = flag;
            this.arity = 1;
            this.values = new String[] {value};
        }
    }

    public ServiceArg(String flag) {
        this(flag, 0);
    }

    public String[] toStringArray() {
        if (flag == null) {
            return new String[]{};
        } else {
            int nargs = arity > values.length ? values.length : arity;
            String[] args;
            int valuesStartIndex = 0;
            if (StringUtils.isBlank(flag)) {
                args = new String[nargs];
                valuesStartIndex = 0;
            } else {
                args = new String[nargs + 1];
                args[0] = flag;
                valuesStartIndex = 1;
            }
            for (int i = 0; i < arity; i++) {
                args[i + valuesStartIndex] = values[i];
            }
            return args;
        }
    }
}
