package ximtool.datresource.particle

sealed interface DynamicParticleData {
    val allocationSize: Int
}

object DynamicParticleDataTypes {

    object PositionData: DynamicParticleData {
        override val allocationSize = 16
    }

    object RotationData: DynamicParticleData {
        override val allocationSize = 16
    }

    object ScaleData: DynamicParticleData {
        override val allocationSize = 16
    }

    object ColorTransform: DynamicParticleData {
        override val allocationSize = 8
    }

    data class KeyFrameData(val opCode: Int): DynamicParticleData {
        override val allocationSize = 12

        override fun equals(other: Any?): Boolean {
            if (other !is KeyFrameData) return false
            return opCode == other.opCode
        }

        override fun hashCode(): Int {
            return opCode
        }

    }

}