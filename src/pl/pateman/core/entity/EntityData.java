package pl.pateman.core.entity;

/**
 * Class which holds information about an entity that a rigid body is attached to.
 *
 * Created by pateman.
 */
public final class EntityData {
    private final Long entityID;
    private final String entityName;
    private final AbstractEntity entity;

    public EntityData(Long entityID, String entityName, AbstractEntity entity) {
        this.entityID = entityID;
        this.entityName = entityName;
        this.entity = entity;
    }

    public Long getEntityID() {
        return entityID;
    }

    public String getEntityName() {
        return entityName;
    }

    public AbstractEntity getEntity() {
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityData that = (EntityData) o;

        if (entityID != null ? !entityID.equals(that.entityID) : that.entityID != null) return false;
        return entityName != null ? entityName.equals(that.entityName) : that.entityName == null;

    }

    @Override
    public int hashCode() {
        int result = entityID != null ? entityID.hashCode() : 0;
        result = 31 * result + (entityName != null ? entityName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EntityData{" +
                "entityID=" + entityID +
                ", entityName='" + entityName + '\'' +
                '}';
    }
}
