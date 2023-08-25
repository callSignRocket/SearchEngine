package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "`page`", indexes = {@Index(name = "index_site_id", columnList = "site_id")})
@NoArgsConstructor
public class PageEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private SiteEntity site;

    @Column(name = "`path`", length = 2000, columnDefinition = "VARCHAR(515) NOT NULL, index(`path`)")
    private String path;

    private int code;

    @Column(length = 16777215, columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<IndexEntity> index = new ArrayList<>();

    public PageEntity(SiteEntity site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        PageEntity p = (PageEntity) o;
        return site == null || getClass() == o.getClass() && path.equals(p.path) && site == p.site;
    }

    @Override
    public int hashCode() {
        return path != null && site!= null ? path.hashCode() + site.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "id: " + id + ", siteId: " + site.getId() + ", path: " + path;
    }
}
