package org.jglrxavpok.localportal.extensions

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.util.vector.Quaternion
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