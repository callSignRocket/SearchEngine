package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "lemma")
@NoArgsConstructor
public class LemmaEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private SiteEntity site;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL, index(lemma)")
    private String lemma;

    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<IndexEntity> index = new ArrayList<>();

    public LemmaEntity(String lemma, int frequency, SiteEntity site) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
