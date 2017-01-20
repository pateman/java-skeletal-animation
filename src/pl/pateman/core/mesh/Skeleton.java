package pl.pateman.core.mesh;

import java.util.*;

/**
 * Created by pateman.
 */
public class Skeleton {
    private final List<Bone> bones;

    public Skeleton() {
        this.bones = new ArrayList<>();
    }

    public Bone getBoneByIndex(int index) {
        for (int i = 0; i < this.bones.size(); i++) {
            final Bone bone = this.bones.get(i);

            if (bone.getIndex() == index) {
                return bone;
            }
        }
        return null;
    }

    public Bone getBone(int index) {
        return this.bones.get(index);
    }

    public Bone getBoneByName(final String boneName) {
        for (int i = 0; i < this.bones.size(); i++) {
            final Bone bone = this.bones.get(i);

            if (bone.getName().equals(boneName)) {
                return bone;
            }
        }
        return null;
    }

    public Bone getRootBone() {
        for (int i = 0; i < this.bones.size(); i++) {
            final Bone bone = this.bones.get(i);
            if (bone.getParent() == null) {
                return bone;
            }
        }
        return null;
    }

    private void calculateBindMatrices(final Bone bone) {
        bone.calculateBindMatrices();

        for (int i = 0; i < bone.getChildren().size(); i++) {
            this.calculateBindMatrices(bone.getChildren().get(i));
        }
    }

    public void calculateBindMatrices() {
        //  Start from the root.
        final Bone rootBone = this.getRootBone();

        if (rootBone != null) {
            this.calculateBindMatrices(rootBone);
        }
    }

    public void arrangeBones() {
        this.bones.sort(Comparator.comparingInt(Bone::getIndex));
    }

    public List<Bone> getBones() {
        return bones;
    }

    @Override
    public String toString() {
        return this.bones.isEmpty() ? "" : this.getRootBone().toString();
    }
}
