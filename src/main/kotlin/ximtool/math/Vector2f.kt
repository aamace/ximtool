package ximtool.math

data class Vector2f(var x: Float = 0f, var y: Float = 0f) {

    fun copyFrom(other: Vector2f): Vector2f {
        x = other.x
        y = other.y
        return this
    }

    companion object {
        val zero = Vector2f(0f, 0f)
    }

    operator fun plus(other: Vector2f) : Vector2f {
        return Vector2f(x + other.x, y + other.y)
    }

    operator fun minus(other: Vector2f) : Vector2f {
        return Vector2f(x - other.x, y - other.y)
    }

    operator fun times(value: Float) : Vector2f {
        return Vector2f(x*value, y*value)
    }

    operator fun times(other: Vector2f) : Vector2f {
        return Vector2f(x*other.x, y*other.y)
    }

    operator fun get(uv: Axis): Float {
        return when(uv) {
            Axis.X -> x
            Axis.Y -> y
            else -> throw IllegalStateException("illegal axis on 2d vector")
        }
    }

    operator fun set(uv: Axis, value: Float) {
        when(uv) {
            Axis.X -> x = value
            Axis.Y -> y = value
            else -> throw IllegalStateException("illegal axis on 2d vector")
        }
    }

    fun scale(scale: Vector2f): Vector2f {
        return Vector2f(x * scale.x, y * scale.y)
    }

}