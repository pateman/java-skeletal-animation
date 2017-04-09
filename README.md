# OpenGL 3 Java Skeletal Animation
A working example of 3D skeletal animation system made in Java and OpenGL. **The code is in development, so expect changes**. Also, certain things could have been done better. If you find an issue with the code or have a suggestion, please get in touch.

Features included:
+ Smooth blending between different animations
+ Manual bone controlling (e.g. you can play your 'idle' animation and have the character's head follow the player)
+ Possibility to apply an animation only to a subset of bones
+ Custom 3D model format with a 3dsmax exporter written in C++
+ Dynamic ragdoll generation

Planned features:
+ Support for a custom motion-capture system based on Arduino
+ Bone attachments
+ Smooth transition from ragdoll to animation
+ IK foot placement
+ Facial animation

### Third-party code and libraries
+ [LWJGL3](https://github.com/LWJGL/lwjgl3) - OpenGL wrapper for Java
+ [JOML](https://github.com/JOML-CI/JOML) - 3D algebra library for Java
+ TempVars.java from [jMonkeyEngine](https://github.com/jMonkeyEngine/jmonkeyengine)
+ [GSON](https://github.com/google/gson) - for importing/exporting custom 3D models
+ [jBullet](http://jbullet.advel.cz/) - Java port of the Bullet physics library
+ [xStream](http://x-stream.github.io/) - for importing OgreXML models
