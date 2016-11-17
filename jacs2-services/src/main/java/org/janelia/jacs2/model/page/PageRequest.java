package org.janelia.jacs2.model.page;

import java.util.List;

public class PageRequest {
    private long firstPageOffset;
    private long pageNumber;
    private int pageSize;
    private List<SortCriteria> sortCriteria;

    public long getFirstPageOffset() {
        return firstPageOffset;
    }

    public void setFirstPageOffset(long firstPageOffset) {
        this.firstPageOffset = firstPageOffset;
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

    public long getOffset() {
        long offset = 0L;
        if (firstPageOffset > 0) {
            offset = firstPageOffset;
        }
        if (pageNumber > 0 && pageSize > 0) {
            offset += pageNumber * pageSize;
        }
        return offset;
    }
}
