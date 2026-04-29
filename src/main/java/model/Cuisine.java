package model;

public class Cuisine {
    private long cuisineId;
    private String name;
    private String slug;
    private Long parentId; // nullable

    public Cuisine() {}

    public long getCuisineId()          { return cuisineId; }
    public void setCuisineId(long v)    { this.cuisineId = v; }

    public String getName()             { return name; }
    public void setName(String v)       { this.name = v; }

    public String getSlug()             { return slug; }
    public void setSlug(String v)       { this.slug = v; }

    public Long getParentId()           { return parentId; }
    public void setParentId(Long v)     { this.parentId = v; }
}
