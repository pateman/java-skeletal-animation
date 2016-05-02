package pl.pateman.importer.ogrexml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.util.List;

/**
 * Created by pateman on 2016-03-20.
 */
@XStreamAlias("skeleton")
class OgreXMLSkeleton {
    @XStreamAlias("bones")
    List<Bone> boneList;
    @XStreamAlias("bonehierarchy")
    List<BoneHierarchyInfo> hierarchyInfo;
    List<Animation> animations;

    public static class Vector3 {
        @XStreamAsAttribute
        Float x;
        @XStreamAsAttribute
        Float y;
        @XStreamAsAttribute
        Float z;
    }

    public static class Rotation {
        @XStreamAsAttribute
        Float angle;
        Vector3 axis;
    }

    @XStreamAlias("bone")
    public static class Bone {
        @XStreamAsAttribute
        String name;
        @XStreamAsAttribute
        Integer id;

        Vector3 position;
        Rotation rotation;
        Vector3 scale;
    }

    @XStreamAlias("boneparent")
    public static class BoneHierarchyInfo {
        @XStreamAlias("bone")
        @XStreamAsAttribute
        String boneName;
        @XStreamAlias("parent")
        @XStreamAsAttribute
        String parentBoneName;
    }

    @XStreamAlias("animation")
    public static class Animation {
        @XStreamAsAttribute
        String name;
        @XStreamAsAttribute
        Float length;
        List<Track> tracks;
    }

    @XStreamAlias("track")
    public static class Track {
        @XStreamAlias("bone")
        @XStreamAsAttribute
        String boneName;
        List<Keyframe> keyframes;
    }

    @XStreamAlias("keyframe")
    public static class Keyframe {
        @XStreamAsAttribute
        Float time;
        @XStreamAlias("translate")
        Vector3 translation;
        @XStreamAlias("rotate")
        Rotation rotation;
    }
}
