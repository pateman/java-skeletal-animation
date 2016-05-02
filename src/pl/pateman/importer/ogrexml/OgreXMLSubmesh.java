package pl.pateman.importer.ogrexml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import pl.pateman.importer.ogrexml.converters.OgreXMLVertexBufferTextureCoordsAttribConverter;

import java.util.List;

/**
 * Created by pateman on 2016-03-17.
 */
@XStreamAlias("submesh")
class OgreXMLSubmesh {
    List<Face> faces;
    @XStreamAlias("geometry")
    List<VertexBuffer> vertexBuffers;
    @XStreamAlias("boneassignments")
    List<VertexBoneAssignment> boneAssignments;

    @XStreamAlias("face")
    public static class Face {
        @XStreamAsAttribute
        int v1;
        @XStreamAsAttribute
        int v2;
        @XStreamAsAttribute
        int v3;
    }

    @XStreamAlias("vertexbuffer")
    public static class VertexBuffer {
        @XStreamAsAttribute
        boolean positions = false;
        @XStreamAsAttribute
        boolean normals = false;
        @XStreamAlias("texture_coords")
        @XStreamAsAttribute
        @XStreamConverter(OgreXMLVertexBufferTextureCoordsAttribConverter.class)
        boolean textureCoords = false;
        @XStreamImplicit
        List<Vertex> vertexes;
    }

    @XStreamAlias("vertex")
    public static class Vertex {
        Position position;
        Normal normal;
        Texcoord texcoord;
    }

    @XStreamAlias("position")
    public static class Position {
        @XStreamAsAttribute
        float x;
        @XStreamAsAttribute
        float y;
        @XStreamAsAttribute
        float z;
    }

    @XStreamAlias("normal")
    public static class Normal {
        @XStreamAsAttribute
        float x;
        @XStreamAsAttribute
        float y;
        @XStreamAsAttribute
        float z;
    }

    @XStreamAlias("texcoord")
    public static class Texcoord {
        @XStreamAsAttribute
        float u;
        @XStreamAsAttribute
        float v;
    }

    @XStreamAlias("vertexboneassignment")
    public static class VertexBoneAssignment {
        @XStreamAlias("vertexindex")
        @XStreamAsAttribute
        int vertex;
        @XStreamAlias("boneindex")
        @XStreamAsAttribute
        int bone;
        @XStreamAsAttribute
        float weight;
    }
}
