package ximtool.datresource.particle

import ximtool.dat.*
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

object ParticleGeneratorSerializer {

    fun serialize(config: ParticleGenerator): ByteArray {
        return ParticleGeneratorSerializerInstance(config).serialize()
    }

}

private class ParticleGeneratorSerializerInstance(val config: ParticleGenerator) {

    private val allocationPool = HashMap<DynamicParticleData, Int>()

    fun serialize(): ByteArray {
        var size = 0x90 // Size of header

        val generatorUpdaterOffset = size
        size += config.generatorUpdaters.sumOf { it.sizeInBytes() }.padTo16()

        val initializerOffset = size
        size += config.particleInitializers.sumOf { it.sizeInBytes() }.padTo16()

        val updaterOffset = size
        size += config.particleUpdaters.sumOf { it.sizeInBytes() }.padTo16()

        val expirationHandlerOffset = size
        size += config.particleExpirationHandlers.sumOf { it.sizeInBytes() }.padTo16()

        val out = ByteReader(ByteArray(size))
        SectionHeader(config.datId, SectionType.S05_ParticleGenerator, size).write(out)

        writeGeneratorHeader(out)

        out.position = 0x80
        out.write32(generatorUpdaterOffset)
        out.write32(initializerOffset)
        out.write32(updaterOffset)
        out.write32(expirationHandlerOffset)

        writeGeneratorUpdaters(out, generatorUpdaterOffset)
        writeParticleInitializers(out, initializerOffset)
        writeParticleUpdaters(out, updaterOffset)
        writeParticleExpirationHandlers(out, expirationHandlerOffset)

        return out.bytes
    }

    private fun writeGeneratorHeader(out: ByteReader) {
        val start = out.position

        var attachField = config.header.attachType.flag
        attachField += config.header.attachedJoint0 shl 4
        attachField += config.header.attachedJoint1 shl 10
        out.write16(attachField)

        var attachScaling = 0
        if (config.header.attachSourceOriented) { attachScaling += 0x0001 }

        attachScaling += when (config.header.actorScaleParams.scalePosition) {
            ActorScaleTarget.None -> 0x0000
            ActorScaleTarget.Source -> 0x0040
            ActorScaleTarget.Target -> 0x0080
        }
        attachScaling += when (config.header.actorScaleParams.scaleSize) {
            ActorScaleTarget.None -> 0x0000
            ActorScaleTarget.Source -> 0x0400
            ActorScaleTarget.Target -> 0x0800
        }

        out.write16(attachScaling)
        out.align0x10()

        out.writeFloat(config.header.actorScaleParams.scalePositionAmount)
        out.writeFloat(config.header.actorScaleParams.scaleSizeAmount)

        out.position = start + 0x54
        out.write(config.header.environmentId ?: DatId.zero)

        out.position = start + 0x64
        out.write16(config.header.emissionVariance)
        out.write16(config.header.framesPerEmission)
        out.write8((config.header.particlesPerEmission - 1).coerceAtLeast(0))

        var genflags = 0
        if (config.header.continuous) { genflags += 0x04 }
        if (config.header.autoRun) { genflags += 0x10 }
        out.write8(genflags)

        out.write8(0x00)

        var envflags = 0
        if (config.header.batched) { envflags += 0x20 }
        out.write8(envflags)
    }

    private fun writeGeneratorUpdaters(out: ByteReader, generatorUpdaterOffset: Int) {
        out.position = generatorUpdaterOffset
        config.generatorUpdaters.forEach {
            val startPosition = out.position
            it.write(out)
            check(out.position == startPosition + it.sizeInBytes()) { "Wrote wrong number of bytes: $it" }
        }
    }

    private fun writeParticleInitializers(out: ByteReader, initializerOffset: Int) {
        val allocationPoolSize = config.particleInitializers
            .mapNotNull { it.allocationType() }
            .distinct()
            .sumOf { it.allocationSize }

        var currentAllocationOffset = allocationPoolSize

        out.position = initializerOffset
        val context = InitializerContext(allocationPoolSize = allocationPoolSize)

        for (initializer in config.particleInitializers) {
            val allocationType = initializer.allocationType()
            val existingAllocation = allocationPool[allocationType]

            val allocationOffset = if (existingAllocation != null) {
                existingAllocation
            } else if (allocationType == null) {
                0
            } else {
                currentAllocationOffset -= allocationType.allocationSize
                check(currentAllocationOffset >= 0) { "Illegal allocation offset $currentAllocationOffset" }
                allocationPool[allocationType] = currentAllocationOffset
                currentAllocationOffset
            }

            val startPosition = out.position
            initializer.write(out, allocationOffset, context)
            check(out.position == startPosition + initializer.sizeInBytes()) { "Wrote wrong number of bytes: $initializer" }
        }
    }

    private fun writeParticleUpdaters(out: ByteReader, updaterOffset: Int) {
        out.position = updaterOffset

        for (updater in config.particleUpdaters) {
            val linked = updater.linkedDataType()
            val allocationOffset = if (linked != null) {
                allocationPool[linked] ?: throw IllegalStateException("Updater $updater is missing its required initializer: $linked")
            } else {
                0
            }

            val startPosition = out.position
            updater.write(out, allocationOffset)
            check(out.position == startPosition + updater.sizeInBytes()) { "Wrote wrong number of bytes: $updater" }
        }
    }

    private fun writeParticleExpirationHandlers(out: ByteReader, expirationHandlerOffset: Int) {
        out.position = expirationHandlerOffset
        config.particleExpirationHandlers.forEach {
            val startPosition = out.position
            it.write(out)
            check(out.position == startPosition + it.sizeInBytes()) { "Wrote wrong number of bytes: $it" }
        }
    }

}

fun <T: ParticleInitializer> ParticleGenerator.getOptional(type: KClass<T>): T? {
    return particleInitializers.firstNotNullOfOrNull { type.safeCast(it) }
}

operator fun <T: ParticleInitializer> ParticleGenerator.get(type: KClass<T>): T {
    return getOptional(type) ?: throw IllegalStateException("Particle generator does not have initializer ${type.simpleName}")
}

fun ParticleGenerator.deepCopy(): ParticleGenerator {
    val bytes = ParticleGeneratorSerializer.serialize(this)
    return ParticleGeneratorSection.read(bytes)
}