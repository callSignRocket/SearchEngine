package searchengine.dto.statistics;

import searchengine.model.Status;

import java.time.LocalDateTime;

public record DetailedStatisticsItem(String url,
                                     String name,
                                     Status status,
                                     LocalDateTime statusTime,
                                     String error,
                                     long pages,
                                     long lemmas) {

}
