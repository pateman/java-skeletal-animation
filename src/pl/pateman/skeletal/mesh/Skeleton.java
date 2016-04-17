package pl.pateman.skeletal.mesh;

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
        for (Bone bone : this.bones) {
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
        for (Bone bone : bones) {
            if (bone.getName().equals(boneName)) {
                return bone;
            }
        }
        return null;
    }

    public Bone getRootBone() {
        for (Bone bone : this.bones) {
            if (bone.getParent() == null) {
                return bone;
            }
        }
        return null;
    }

    private void calculateBindMatrices(final Bone bone) {
        bone.calculateBindMatrices();

        for (Bone child : bone.getChildren()) {
            this.calculateBindMatrices(child);
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
        Collections.sort(this.bones, new Comparator<Bone>() {
            @Override
            public int compare(Bone o1, Bone o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
    }

    public List<Bone> getBones() {
        return bones;
    }

    @Override
    public String toString() {
        return this.bones.isEmpty() ? "" : this.getRootBone().toString();
    }
}
