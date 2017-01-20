package pl.pateman.core.physics.ragdoll;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.joml.Quaternionf;
import pl.pateman.core.Utils;
import pl.pateman.core.mesh.Bone;
import pl.pateman.core.mesh.Mesh;

import java.io.IOException;
import java.util.*;

/**
 * Holds the structure of a ragdoll.
 *
 * Created by pateman.
 */
public final class RagdollStructure {
    private final Map<String, Part> parts;
    private final Map<String, Link> links;

    RagdollStructure() {
        this.parts = new HashMap<>();
        this.links = new HashMap<>();
    }

    public Part getPart(final String partName) {
        if (partName == null || partName.isEmpty()) {
            throw new IllegalArgumentException("A valid part name is required");
        }

        return this.parts.get(partName);
    }

    public Collection<String> getPartNames() {
        return this.parts.keySet();
    }

    public Collection<String> getLinkNames() {
        return this.links.keySet();
    }

    public Link getLink(final String linkName) {
        return this.links.get(linkName);
    }

    /**
     * Imports a ragdoll structure from a JSON file.
     *
     * @param jsonFileResource Path to the JSON file.
     * @param mesh A {@code Mesh} instance that this structure applies to.
     * @return Parsed {@code RagdollStructure} instance.
     * @throws IOException
     */
    public static RagdollStructure importJSON(final String jsonFileResource, final Mesh mesh) throws IOException {
        if (jsonFileResource == null || jsonFileResource.isEmpty() || mesh == null) {
            throw new IllegalArgumentException("Valid JSON file path and mesh need to be provided");
        }
        final Gson gson = new Gson();

        //  Use Gson to parse the file.
        final RagdollStructure ragdollStructure = gson.fromJson(Utils.readResource(jsonFileResource),
                RagdollStructure.class);

        //  Populate missing fields.
        for (final Map.Entry<String, Part> entry : ragdollStructure.parts.entrySet()) {
            entry.getValue().postProcess(entry.getKey(), mesh);
        }

        for (final Map.Entry<String, Link> entry : ragdollStructure.links.entrySet()) {
            final String[] split = entry.getKey().split("->");
            if (split.length < 2) {
                throw new IllegalStateException("Invalid ragdoll link name. The correct format is 'PART A->PART B'");
            }
            entry.getValue().postProcess(split[0], split[1], mesh);
        }

        System.gc();
        return ragdollStructure;
    }

    public final class PartPhysicalProperties {
        private float linearDamping;
        private float angularDamping;
        private float deactivationTime;
        private float linearSleepingThreshold;
        private float angularSleepingThreshold;

        public float getLinearDamping() {
            return linearDamping;
        }

        public float getAngularDamping() {
            return angularDamping;
        }

        public float getDeactivationTime() {
            return deactivationTime;
        }

        public float getLinearSleepingThreshold() {
            return linearSleepingThreshold;
        }

        public float getAngularSleepingThreshold() {
            return angularSleepingThreshold;
        }
    }

    abstract class StructurePart {
        Bone getBoneByName(final String boneName, final Mesh mesh) {
            if (boneName == null || boneName.isEmpty()) {
                throw new IllegalArgumentException("Bone name cannot be empty");
            }

            final Bone boneByName = mesh.getSkeleton().getBoneByName(boneName);
            if (boneByName == null) {
                throw new IllegalStateException("Missing bone '" + boneName + "'");
            }

            return boneByName;
        }
    }

    public final class Part extends StructurePart {
        private String name;
        private BodyPartCollider colliderType;

        @SerializedName("colliderBones")
        private List<String> colliderBoneNames;
        private transient List<Bone> colliderBones;

        @SerializedName("attachedBones")
        private List<String> attachedBoneNames;
        private transient List<Bone> attachedBones;

        @SerializedName("parentBone")
        private String parentBoneName;
        private transient Bone parentBone;

        @SerializedName("bone")
        private String boneName;
        private transient Bone bone;

        @SerializedName("offsetRotation")
        private List<Float> offsetRotationValues;
        private transient Quaternionf offsetRotation;

        private PartPhysicalProperties physicalProperties;

        void postProcess(final String partName, final Mesh mesh) {
            this.name = partName;

            this.parentBone = this.getBoneByName(this.parentBoneName, mesh);
            this.bone = this.getBoneByName(this.boneName, mesh);

            this.colliderBones = new ArrayList<>(this.colliderBoneNames.size());
            for (final String colliderBoneName : this.colliderBoneNames) {
                this.colliderBones.add(this.getBoneByName(colliderBoneName, mesh));
            }

            this.attachedBones = new ArrayList<>(this.attachedBoneNames.size());
            for (final String attachedBoneName : this.attachedBoneNames) {
                this.attachedBones.add(this.getBoneByName(attachedBoneName, mesh));
            }

            this.offsetRotation = Utils.floatsToQuat(this.offsetRotationValues, new Quaternionf());
        }

        public String getName() {
            return name;
        }

        public List<Bone> getColliderBones() {
            return colliderBones;
        }

        public List<Bone> getAttachedBones() {
            return attachedBones;
        }

        public Bone getParentBone() {
            return parentBone;
        }

        public Bone getBone() {
            return bone;
        }

        public Quaternionf getOffsetRotation() {
            return offsetRotation;
        }

        public PartPhysicalProperties getPhysicalProperties() {
            return physicalProperties;
        }

        public BodyPartCollider getColliderType() {
            return colliderType;
        }
    }

    public final class Link extends StructurePart {
        private String partA;
        private String partB;

        @SerializedName("linkBone")
        private String linkBoneName;
        private transient Bone linkBone;

        private RagdollLinkType constraintType;
        private List<Float> limits;
        private List<Float> pivotA;
        private List<Float> pivotB;
        private boolean flipAWithB;

        void postProcess(final String partAName, final String partBName, final Mesh mesh) {
            this.partA = partAName;
            this.partB = partBName;

            this.linkBone = this.getBoneByName(this.linkBoneName, mesh);
        }

        public String getPartA() {
            return partA;
        }

        public String getPartB() {
            return partB;
        }

        public Bone getLinkBone() {
            return linkBone;
        }

        public RagdollLinkType getConstraintType() {
            return constraintType;
        }

        public List<Float> getLimits() {
            return limits;
        }

        public List<Float> getPivotA() {
            return pivotA;
        }

        public List<Float> getPivotB() {
            return pivotB;
        }

        public boolean isFlipAWithB() {
            return flipAWithB;
        }
    }
}
