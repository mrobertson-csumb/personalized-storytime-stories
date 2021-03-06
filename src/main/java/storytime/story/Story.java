package storytime.story;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "STORY")
public class Story {
    @Id
    @GeneratedValue
    private long id;

    @Lob
    @Column(name = "NAME", columnDefinition = "CLOB")
    @NotNull
    @Size(min = 1, max = 2056)
    private String name;

    @Lob
    @Column(name = "CONTENT", columnDefinition = "CLOB")
    private String content;

    public Story(long id, String name, String content) {
        this.id = id;
        this.name = name;
        this.content = content;
    }

    public Story() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
