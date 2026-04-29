package model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RecommendationGroup {
    private long groupId;
    private long createdBy;
    private String groupName;
    private Timestamp createdAt;
    private List<Long> members = new ArrayList<>();

    public RecommendationGroup() {}

    public RecommendationGroup(long createdBy, String groupName) {
        this.createdBy = createdBy;
        this.groupName = groupName;
    }

    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }

    public long getCreatedBy() { return createdBy; }
    public void setCreatedBy(long createdBy) { this.createdBy = createdBy; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<Long> getMembers() { return members; }
    public void setMembers(List<Long> members) { this.members = members; }

    public void addMember(long userId) {
        if (!members.contains(userId)) members.add(userId);
    }

    public void removeMember(long userId) {
        members.remove(userId);
    }
}
