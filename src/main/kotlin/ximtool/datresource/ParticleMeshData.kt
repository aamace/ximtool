package ximtool.datresource

import ximtool.dat.ByteColor
import ximtool.dat.TextureName
import ximtool.math.Vector2f
import ximtool.math.Vector3f

data class ParticleMeshVertex(
    var position: Vector3f,
    var normal: Vector3f,
    var color: ByteColor,
    var uv: Vector2f,
)

data class ParticleMeshEntry(
    var textureName: TextureName?,
    var vertices: MutableList<ParticleMeshVertex>
)