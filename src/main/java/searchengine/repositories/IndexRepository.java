package searchengine.repositories;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    @Cacheable("indexesByLemmasAndPages")
    default List<IndexEntity> findByPagesAndLemmas(List<LemmaEntity> lemmaListId, List<PageEntity> pageListId) {
        Set<Long> lemmaIds = lemmaListId.stream().map(LemmaEntity::getId).collect(Collectors.toSet());
        Set<Long> pageIds = pageListId.stream().map(PageEntity::getId).collect(Collectors.toSet());
        return findAllByLemmaIdInAndPageIdIn(lemmaIds, pageIds);
    }

    List<IndexEntity> findAllByLemmaIdInAndPageIdIn(Set<Long> lemmaIds, Set<Long> pageIds);
}

