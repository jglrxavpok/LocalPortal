package org.jglrxavpok.localportal.extensions

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import org.lwjgl.util.vector.Vector4f
import java.lang.Math

fun Float.toRadians() = this / 180f * Math.PI.toFloat()
fun Double.toRadians() = this / 360.0 * Math.PI * 2.0
fun Double.toDegrees() = this * 360.0 / Math.PI / 2.0
fun Float.toDegrees() = this * 180.0f / Math.PI.toFloat()

/**
 * Requires both this EnumFacing and 'other' to be NORTH, SOUTH, WEST or EAST
 * @return The angle between the two facings (in degrees)
 */
fun EnumFacing.angleTo(other: EnumFacing): Double {
    if(this.axis == EnumFacing.Axis.Y || other.axis == EnumFacing.Axis.Y)
        throw IllegalArgumentException("Cannot compute angle with Facings with vertical direction!")
    var tmp = this
    var angle = 0
    while (tmp != other) {
        angle += 90
        tmp = tmp.rotateY()
    }
    return angle.toDouble()
}

operator fun Matrix4f.get(i: Int, j: Int): Float {
    return when(j) { // for some reason it's column major order with LWJGL
        0 -> when(i) {
            0 -> this.m00
            1 -> this.m01
            2 -> this.m02
            3 -> this.m03
            else -> error("Invalid matrix position $i, $j")
        }

        1 -> when(i) {
            0 -> this.m10
            1 -> this.m11
            2 -> this.m12
            3 -> this.m13
            else -> error("Invalid matrix position $i, $j")
        }

        2 -> when(i) {
            0 -> this.m20
            1 -> this.m21
            2 -> this.m22
            3 -> this.m23
            else -> error("Invalid matrix position $i, $j")
        }

        3 -> when(i) {
            0 -> this.m30
            1 -> this.m31
            2 -> this.m32
            3 -> this.m33
            else -> error("Invalid matrix position $i, $j")
        }

        else -> error("Invalid matrix position $i, $j")
    }
}

operator fun Matrix4f.times(other: Matrix4f): Matrix4f {
    val result = Matrix4f()
    other.load(this)
    for(i in 0..3) {
        for(j in 0..3) {
            var coeff = 0f
            for(k in 0..3) {
                coeff += this[i, k] * other[k, j]
            }
        }
    }
    return result
}

fun Matrix4f.transform(x: Float, y: Float, z: Float, w: Float): Vector4f {
    val result = Vector4f()
    result.x = x*this.m00+y*this.m01+z*this.m02+w*this.m03
    result.y = x*this.m10+y*this.m11+z*this.m12+w*this.m13
    result.z = x*this.m20+y*this.m21+z*this.m22+w*this.m23
    result.w = x*this.m30+y*this.m31+z*this.m32+w*this.m33
    return result
}

fun Vec3d.dot(x: Double, y: Double, z: Double) = this.x*x+this.y*y+this.z*z