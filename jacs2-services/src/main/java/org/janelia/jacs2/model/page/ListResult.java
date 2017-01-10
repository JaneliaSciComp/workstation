package org.janelia.jacs2.model.page;

import java.util.List;

public class ListResult<T> {
    private List<T> resultList;

    public ListResult() {
    }

    public ListResult(List<T> resultList) {
        this.resultList = resultList;
    }

    public List<T> getResultList() {
        return resultList;
    }

    public void setResultList(List<T> resultList) {
        this.resultList = resultList;
    }
}
