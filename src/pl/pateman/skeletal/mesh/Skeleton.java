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
        for (Bone bone : bones) {
            if (bone.getIndex() == index) {
                return bone;
            }
        }
        return null;
    }

    public Bone getBoneByName(final String boneName) {
        for (Bone bone : bones) {
            if (bone.getName().equals(boneName)) {
                return bone;
            }
        }
        return null;
    }

    public List<Bone> getBones() {
        return bones;
    }
}
