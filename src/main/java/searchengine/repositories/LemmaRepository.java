package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    long countBySite(SiteEntity site);
    List<LemmaEntity> findBySite(SiteEntity site);
    List<LemmaEntity> findByLemma(String lemma);
    List<LemmaEntity> findByLemmaAndSite(String lemma, SiteEntity site);
}
