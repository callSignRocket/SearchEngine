package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "index_words", indexes = {@Index(name = "page_id_list", columnList = "page_id"),
                                        @Index(name = "lemma_id_list", columnList = "lemma_id")})
@NoArgsConstructor
public class IndexEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private PageEntity page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private LemmaEntity lemma;

    @Column(name = "index_rank", nullable = false)
    private float rank;

    public IndexEntity(PageEntity page, LemmaEntity lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
}
