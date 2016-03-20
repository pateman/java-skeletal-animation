package pl.pateman.skeletal.ogrexml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import pl.pateman.skeletal.ogrexml.converters.OgreXMLMeshSkeletonLinkConverter;

import java.util.List;

/**
 * Created by pateman on 2016-03-17.
 */
@XStreamAlias("mesh")
class OgreXMLMesh {
    @XStreamAlias("skeletonlink")
    @XStreamConverter(OgreXMLMeshSkeletonLinkConverter.class)
    String skeletonResource;

    List<OgreXMLSubmesh> submeshes;
}
