package org.janelia.jacs2.model.page;

import java.util.List;

public class PageResult<T> {
    private long pageOffset;
    private long pageNumber;
    private int pageSize;
    private List<SortCriteria> sortCriteria;
    private List<T> resultList;

    public PageResult() {
    }

    public PageResult(PageRequest pageRequest, List<T> resultList) {
        pageOffset = pageRequest.getOffset();
        pageNumber = pageRequest.getPageNumber();
        pageSize = pageRequest.getPageSize();
        sortCriteria = pageRequest.getSortCriteria();
        this.resultList = resultList;
    }

    public long getPageOffset() {
        return pageOffset;
    }

    public void setPageOffset(long pageOffset) {
        this.pageOffset = pageOffset;
    }

    public long getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(long pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<SortCriteria> getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(List<SortCriteria> sortCriteria) {
        this.sortCriteria = sortCriteria;
    }

    public List<T> getResultList() {
        return resultList;
    }

    public void setResultList(List<T> resultList) {
        this.resultList = resultList;
    }
}
