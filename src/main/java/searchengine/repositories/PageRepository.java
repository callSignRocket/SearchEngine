package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Collection;
import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {

    long countBySite(SiteEntity site);
    Iterable<PageEntity> findBySite(SiteEntity site);

    @Query(value = "SELECT DISTINCT p.* FROM page p JOIN index_words i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas", nativeQuery = true)
    Set<PageEntity> findByLemmaList(@Param("lemmas") Collection<LemmaEntity> lemmaList);
}
