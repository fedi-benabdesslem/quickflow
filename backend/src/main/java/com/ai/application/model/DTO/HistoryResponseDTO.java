package com.ai.application.model.DTO;

import java.util.List;

public class HistoryResponseDTO {
    private List<HistoryItemDTO> items;
    private boolean hasMore;
    private long total;

    public HistoryResponseDTO() {
    }

    public HistoryResponseDTO(List<HistoryItemDTO> items, boolean hasMore, long total) {
        this.items = items;
        this.hasMore = hasMore;
        this.total = total;
    }

    public List<HistoryItemDTO> getItems() {
        return items;
    }

    public void setItems(List<HistoryItemDTO> items) {
        this.items = items;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
