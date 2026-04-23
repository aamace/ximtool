package ximtool.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Matrix4f {

    /*
        0 4 8  12
        1 5 9  13
        2 6 10 14
        3 7 11 15
     */
    val m = FloatArray(16) { 0f }
    init { identity() }

    companion object {

        fun extend(m: Matrix3f) : Matrix4f {
            val out = Matrix4f()
            out.m[0] = m.m[0]
            out.m[1] = m.m[1]
            out.m[2] = m.m[2]

            out.m[4] = m.m[3]
            out.m[5] = m.m[4]
            out.m[6] = m.m[5]

            out.m[8] = m.m[6]
            out.m[9] = m.m[7]
            out.m[10] = m.m[8]

            return out
        }

    }

    fun identity() {
        for (i in m.indices) {
            m[i] = 0f
        }

        m[0] = 1f
        m[5] = 1f
        m[10] = 1f
        m[15] = 1f
    }

    fun translateInPlace(vector3f: Vector3f) : Matrix4f {
        return translateInPlace(vector3f.x, vector3f.y, vector3f.z)
    }

    fun translateInPlace(x: Float, y: Float, z: Float): Matrix4f {
        val translate = Matrix4f()
        translate.m[12] += x
        translate.m[13] += y
        translate.m[14] += z
        return multiplyInPlace(translate)
    }

    fun scaleInPlace(vector3f: Vector3f): Matrix4f {
        return scaleInPlace(vector3f.x, vector3f.y, vector3f.z)
    }

    fun scaleInPlace(x: Float, y: Float, z: Float): Matrix4f {
        val scale = Matrix4f()
        scale.m[0] *= x
        scale.m[5] *= y
        scale.m[10] *= z
        return multiplyInPlace(scale)
    }

    fun rotateXInPlace(radians: Float) : Matrix4f {
        val sin = sin(radians)
        val cos = cos(radians)

        val rotation = Matrix4f()
        rotation.m[5] = cos
        rotation.m[6] = sin
        rotation.m[9] = -sin
        rotation.m[10] = cos

        return multiplyInPlace(rotation)
    }

    fun rotateYInPlace(radians: Float): Matrix4f {
        val sin = sin(radians)
        val cos = cos(radians)

        val rotation = Matrix4f()
        rotation.m[0] = cos
        rotation.m[2] = -sin
        rotation.m[8] = sin
        rotation.m[10] = cos

        return multiplyInPlace(rotation)
    }

    fun rotateZInPlace(radians: Float): Matrix4f {
        val sin = sin(radians)
        val cos = cos(radians)

        val rotation = Matrix4f()
        rotation.m[0] = cos
        rotation.m[1] = sin
        rotation.m[4] = -sin
        rotation.m[5] = cos

        return multiplyInPlace(rotation)
    }

    fun rotateXYZInPlace(vector3f: Vector3f) {
        rotateXYZInPlace(vector3f.x, vector3f.y, vector3f.z)
    }

    fun rotateXYZInPlace(xRad: Float, yRad: Float, zRad: Float): Matrix4f {
        val rotation = Matrix4f()
        rotation.m[0] = cos(yRad) * cos(zRad)
        rotation.m[1] = cos(yRad) * sin(zRad)
        rotation.m[2] = -sin(yRad)
        rotation.m[3] = 0f

        rotation.m[4] = sin(xRad) * sin(yRad) * cos(zRad) - cos(xRad) * sin(zRad)
        rotation.m[5] = sin(xRad) * sin(yRad) * sin(zRad) + cos(xRad) * cos(zRad)
        rotation.m[6] = sin(xRad) * cos(yRad)
        rotation.m[7] = 0f

        rotation.m[8] = cos(xRad) * sin(yRad) * cos(zRad) + sin(xRad) * sin(zRad)
        rotation.m[9] = cos(xRad) * sin(yRad) * sin(zRad) - sin(xRad) * cos(zRad)
        rotation.m[10] = cos(xRad) * cos(yRad)
        rotation.m[11] = 0f

        rotation.m[12] = 0f
        rotation.m[13] = 0f
        rotation.m[14] = 0f
        rotation.m[15] = 1f

        multiplyInPlace(rotation)
        return this
    }


    fun multiplyInPlace(o: Matrix4f) : Matrix4f {
        multiply(o, this)
        return this
    }

    fun multiply(o: Matrix4f, store: Matrix4f) {
        val data = FloatArray(16)

        for (row in 0 until 4) {
            for (col in 0 until 4) {
                data[col*4 + row] = dot(row=row, col=col, o=o)
            }
        }

        for (i in 0 until 16) {
            store.m[i] = data[i]
        }
    }


    private fun dot(row: Int, col: Int, o: Matrix4f): Float {
        val a = m[row + 0*4] * o.m[4*col + 0]
        val b = m[row + 1*4] * o.m[4*col + 1]
        val c = m[row + 2*4] * o.m[4*col + 2]
        val d = m[row + 3*4] * o.m[4*col + 3]
        return a+b+c+d
    }


    fun perspective(fov: Float, aspectRatio: Float, near: Float, far: Float) {
        val h = tan(fov * 0.5f)

        m[0] = 1f / (h * aspectRatio)
        m[5] = 1f / h

        m[10] = (far + near) / (near - far)
        m[14] = 2 * far * near / (near - far)

        m[11] = -1f
    }

    fun ortho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) {
        // scaling factor
        m[0] = 2f / (right - left);
        m[5] = 2f / (top - bottom);
        m[10] = 2f / (near - far);

        // translation factor
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        m[15] = 1f;
    }

    fun lookAt(eye: Vector3f, target: Vector3f, worldUp: Vector3f = Vector3f.UP) {
        // https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/gluLookAt.xml
        val dir = eye.subtract(target).normalize()
        val left = worldUp.cross(dir).normalize()
        val up = dir.cross(left).normalize()

        m[0] = left.x
        m[4] = left.y
        m[8] = left.z
        m[12] = -eye.dot(left)

        m[1] = up.x
        m[5] = up.y
        m[9] = up.z
        m[13] = -eye.dot(up)

        m[2] = dir.x
        m[6] = dir.y
        m[10] = dir.z
        m[14] = -eye.dot(dir)

        m[3] = 0f
        m[7] = 0f
        m[11] = 0f
        m[15] = 1f
    }

    fun axisAngleRotationInPlace(u: Vector3f, angle: Float): Matrix4f {
        val c = cos(angle)
        val s = sin(angle)

        val t = Matrix4f()
        t.m[0] = c + u.x * u.x * (1f - c)
        t.m[1] = u.y * u.x * (1f - c) + u.z * s
        t.m[2] = u.z * u.x * (1f - c) - u.y * s

        t.m[4] = u.x * u.y * (1f - c) - u.z * s
        t.m[5] = c + u.y * u.y * (1f - c)
        t.m[6] = u.z * u.y * (1f - c) + u.x * s

        t.m[8] = u.x * u.z * (1f - c) + u.y * s
        t.m[9] = u.y * u.z * (1f - c) - u.x * s
        t.m[10] = c + u.z * u.z * (1f - c)

        t.m[15] = 1f

        return multiplyInPlace(t)
    }

    fun copyFrom(data: Matrix4f) {
        for (i in 0 until  data.m.size) {
            m[i] = data.m[i]
        }
    }

    fun copyFrom(data: FloatArray) {
        for (i in data.indices) {
            m[i] = data[i]
        }
    }

    fun transform(v: Vector3f, w: Float = 1.0f): Vector3f {
        val result = Vector3f()
        result.x = (m[0] * v.x + m[4] * v.y + m[8] * v.z + w * m[12])
        result.y = (m[1] * v.x + m[5] * v.y + m[9] * v.z + w * m[13])
        result.z = (m[2] * v.x + m[6] * v.y + m[10] * v.z + w *  m[14])
        return result
    }

    fun transformInPlace(v: Vector3f): Vector3f {
        val x = v.x
        val y = v.y
        val z = v.z

        v.x = (m[0] * x + m[4] * y + m[8] * z + m[12])
        v.y = (m[1] * x + m[5] * y + m[9] * z + m[13])
        v.z = (m[2] * x + m[6] * y + m[10] * z + m[14])

        return v
    }

    fun transformTo4d(v: Vector3f): Vector4f {
        val result = Vector4f()
        result.x = (m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12])
        result.y = (m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13])
        result.z = (m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14])
        result.w = (m[3] + m[7] + m[11] + m[15])
        return result
    }

    fun transformDirectionVector(v: Vector3f): Vector3f {
        val v4 = Vector4f(v.x, v.y, v.z, 0f)
        this.transformInPlace(v4)
        return Vector3f(v4.x, v4.y, v4.z)
    }

    fun transformInPlace(v: Vector4f): Vector4f {
        val x = (m[0] * v.x + m[4] * v.y + m[8]  * v.z + m[12] * v.w)
        val y = (m[1] * v.x + m[5] * v.y + m[9]  * v.z + m[13] * v.w)
        val z = (m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w)
        val w = (m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w)

        v.copyFrom(x,y,z,w)
        return v
    }

    fun toColumns(): Array<Vector4f> {
        val columns = Array(4) { Vector4f() }
        columns[0].x =  m[0]; columns[0].y =  m[1]; columns[0].z =  m[2]; columns[0].w =  m[3]
        columns[1].x =  m[4]; columns[1].y =  m[5]; columns[1].z =  m[6]; columns[1].w =  m[7]
        columns[2].x =  m[8]; columns[2].y =  m[9]; columns[2].z = m[10]; columns[2].w = m[11]
        columns[3].x = m[12]; columns[3].y = m[13]; columns[3].z = m[14]; columns[3].w = m[15]
        return columns
    }

    fun identityUpperLeft() {
        m[0] = 1f
        m[1] = 0f
        m[2] = 0f

        m[4] = 0f
        m[5] = 1f
        m[6] = 0f

        m[8] = 0f
        m[9] = 0f
        m[10] = 1f
    }

    fun copyUpperLeft(other: Matrix4f) {
        m[0] = other.m[0]
        m[1] = other.m[1]
        m[2] = other.m[2]

        m[4] = other.m[4]
        m[5] = other.m[5]
        m[6] = other.m[6]

        m[8] = other.m[8]
        m[9] = other.m[9]
        m[10] = other.m[10]
    }

    fun copyUpperLeftXZ(other: Matrix4f) {
        m[0] = other.m[0]
        m[1] = other.m[1]
        m[2] = other.m[2]

        m[8] = other.m[8]
        m[9] = other.m[9]
        m[10] = other.m[10]
    }

    fun isIdentity() : Boolean {
        for (i in 0 until 16) {
            val mainline = (i % 5) == 0
            if (mainline) {
                val diff = abs(1f - m[i])
                if (diff > 0.001f) return false
            } else {
                if (abs(m[i]) > 0.001f) return false
            }
        }
        return true
    }

    fun getTranslationVector(): Vector3f {
        return Vector3f(m[12], m[13], m[14])
    }

    override fun toString(): String {
        return """
            ${m[0]}     ${m[4]}     ${m[8]}     ${m[12]}
            ${m[1]}     ${m[5]}     ${m[9]}     ${m[13]}
            ${m[2]}     ${m[6]}     ${m[10]}    ${m[14]}
            ${m[3]}     ${m[7]}     ${m[11]}    ${m[15]}            
        """.trimIndent()
    }

    fun rotateZYXInPlace(vector3f: Vector3f) : Matrix4f {
        rotateZYXInPlace(vector3f.x, vector3f.y, vector3f.z)
        return this
    }

    fun rotateZYXInPlace(xRad: Float, yRad: Float, zRad: Float): Matrix4f {
        if (xRad == 0f && yRad == 0f && zRad == 0f) { return this }

        val sinX = sin(xRad); val sinY = sin(yRad); val sinZ = sin(zRad)
        val cosX = cos(xRad); val cosY = cos(yRad); val cosZ = cos(zRad)

        val rotation = Matrix4f()
        rotation.m[0] = cosY * cosZ
        rotation.m[1] = cosY * sinZ
        rotation.m[2] = -sinY
        rotation.m[3] = 0f

        rotation.m[4] = sinX * sinY * cosZ - cosX * sinZ
        rotation.m[5] = sinX * sinY * sinZ + cosX * cosZ
        rotation.m[6] = sinX * cosY
        rotation.m[7] = 0f

        rotation.m[8] = cosX * sinY * cosZ + sinX * sinZ
        rotation.m[9] = cosX * sinY * sinZ - sinX * cosZ
        rotation.m[10] = cosX * cosY
        rotation.m[11] = 0f

        rotation.m[12] = 0f
        rotation.m[13] = 0f
        rotation.m[14] = 0f
        rotation.m[15] = 1f

        multiplyInPlace(rotation)
        return this
    }

}