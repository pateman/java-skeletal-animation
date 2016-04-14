package pl.pateman.skeletal.mesh;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Created by pateman.
 */
public class Mesh {
    private final List<Vector3f> vertices;
    private final List<Vector3f> normals;
    private final List<Vector2f> texcoords;
    private final List<Integer> triangles;
    private final Skeleton skeleton;
    private final List<Animation> animations;

    public Mesh() {
        this.vertices = new ArrayList<>();
        this.normals = new ArrayList<>();
        this.texcoords = new ArrayList<>();
        this.triangles = new ArrayList<>();
        this.skeleton = new Skeleton();
        this.animations = new ArrayList<>();
    }

    public MeshSkinningInfo getSkinningInfo() {
        final MeshSkinningInfo skinningInfo = new MeshSkinningInfo();

        final Map<Integer, Set<VertexInfo>> vertexMap = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        });
        for (final Bone bone : this.skeleton.getBones()) {
            final Map<Integer, Float> boneVertexWeights = bone.getVertexWeights();
            for (final Map.Entry<Integer, Float> entry : boneVertexWeights.entrySet()) {
                Set<VertexInfo> set = vertexMap.get(entry.getKey());
                if (set == null) {
                    set = new HashSet<>();
                }

                set.add(new VertexInfo(bone.getIndex(), entry.getValue()));
                vertexMap.put(entry.getKey(), set);
            }
        }

        //  Flatten the map now.
        for (final Map.Entry<Integer, Set<VertexInfo>> entry : vertexMap.entrySet()) {
            final Set<VertexInfo> vertexInfos = entry.getValue();
            if (vertexInfos.size() > MeshSkinningInfo.MAX_BONES_PER_VERTEX) {
                throw new IllegalStateException("Vertex " + entry.getValue() + " has more bones (" + vertexInfos.size() +
                        ") than allowed to (" + MeshSkinningInfo.MAX_BONES_PER_VERTEX + ")");
            }

            final Vector3f boneIndices = new Vector3f();
            final Vector3f boneWeights = new Vector3f();
            final int cap = Math.min(vertexInfos.size(), MeshSkinningInfo.MAX_BONES_PER_VERTEX);
            final Iterator<VertexInfo> iterator = vertexInfos.iterator();
            for (int i = 0; i < cap; i++) {
                final VertexInfo vertexInfo = iterator.next();
                boneIndices.set(i, vertexInfo.bone);
                boneWeights.set(i, vertexInfo.weight);
            }

            skinningInfo.getBoneWeights().add(boneWeights);
            skinningInfo.getBoneIndices().add(boneIndices);
        }

        return skinningInfo;
    }

    public boolean hasSkeleton() {
        return !this.skeleton.getBones().isEmpty();
    }

    public List<Vector3f> getVertices() {
        return vertices;
    }

    public List<Vector3f> getNormals() {
        return normals;
    }

    public List<Vector2f> getTexcoords() {
        return texcoords;
    }

    public List<Integer> getTriangles() {
        return triangles;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    private class VertexInfo {
        final int bone;
        final float weight;

        public VertexInfo(int bone, float weight) {
            this.bone = bone;
            this.weight = weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            VertexInfo that = (VertexInfo) o;

            if (bone != that.bone) return false;
            return Float.compare(that.weight, weight) == 0;

        }

        @Override
        public int hashCode() {
            int result = bone;
            result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
            return result;
        }
    }

}
